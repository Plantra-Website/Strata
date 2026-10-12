{ Terminal -> game input bridge for Strata --terminal play.
  Owns stdin (raw mode + SGR mouse tracking on Unix; console input mode
  on Windows), publishes snapshots for the game to poll:

    SEQ <n>                  (bumps on every state change)
    KEYS <LWJGL names...>    (empty = none held)
    MOUSE <x> <y> <buttons>  (cell coords; bitmask 1=left 2=right 4=mid)
    WHEEL <acc>              (monotonic notches; game diffs the accumulator)

  Written atomically (<path>.tmp + rename), on change and as a 2 Hz
  heartbeat (lets the game tell a live idle bridge from a dead one).
  Unix keys have no release events, so a press holds that name for
  250 ms after its last repeat byte (terminals autorepeat held keys).
  Ctrl-C exits cleanly (console restored, keys-up snapshot written);
  so does the game deleting the frame file it watches.

  Usage: terminput <inputfile> <framefile> [--selftest] [--help]
  --selftest runs the parser vectors (no tty needed) and exits 0/1. }
program terminput;

{$mode objfpc}{$H+}

uses
  SysUtils, Classes
  {$IFNDEF MSWINDOWS}, termio, BaseUnix {$ENDIF}
  {$IFDEF MSWINDOWS}, Windows {$ENDIF};

const
  HoldMs = 250;
  HeartbeatMs = 500;
  FreshMs = 2000;

type
  TPressed = record
    Name: AnsiString;
    LastSeen: QWord;
  end;

var
  InputPath, FramePath: AnsiString;
  Seq: LongInt = 0;
  Pressed: array of TPressed;
  MouseX: LongInt = -1;
  MouseY: LongInt = -1;
  Buttons: LongInt = 0;
  WheelAcc: LongInt = 0;
  Dirty: Boolean = False;
  LastWriteTick: QWord = 0;
  {$IFNDEF MSWINDOWS}
  SavedTermios: TermIOS;
  HaveTermios: Boolean = False;
  {$ENDIF}

function TickNow: QWord;
begin
  Result := GetTickCount64;
end;

procedure TouchKey(const Nm: AnsiString; NowTick: QWord);
var
  i: Integer;
begin
  for i := 0 to High(Pressed) do
    if Pressed[i].Name = Nm then
    begin
      Pressed[i].LastSeen := NowTick;
      Exit;
    end;
  SetLength(Pressed, Length(Pressed) + 1);
  Pressed[High(Pressed)].Name := Nm;
  Pressed[High(Pressed)].LastSeen := NowTick;
  Inc(Seq);
  Dirty := True;
end;

procedure DropKey(const Nm: AnsiString);
var
  i, j: Integer;
begin
  j := 0;
  for i := 0 to High(Pressed) do
    if Pressed[i].Name <> Nm then
    begin
      Pressed[j] := Pressed[i];
      Inc(j);
    end
    else
    begin
      Inc(Seq);
      Dirty := True;
    end;
  if j <> Length(Pressed) then
    SetLength(Pressed, j);
end;

procedure ExpireKeys(NowTick: QWord);
var
  i, j: Integer;
begin
  j := 0;
  for i := 0 to High(Pressed) do
    if NowTick - Pressed[i].LastSeen <= HoldMs then
    begin
      Pressed[j] := Pressed[i];
      Inc(j);
    end
    else
    begin
      Inc(Seq);
      Dirty := True;
    end;
  if j <> Length(Pressed) then
    SetLength(Pressed, j);
end;

procedure SetMouse(AX, AY, ABtns: LongInt);
begin
  if (AX <> MouseX) or (AY <> MouseY) or (ABtns <> Buttons) then
  begin
    MouseX := AX;
    MouseY := AY;
    Buttons := ABtns;
    Inc(Seq);
    Dirty := True;
  end;
end;

procedure AddWheel(Delta: LongInt);
begin
  if Delta <> 0 then
  begin
    WheelAcc := WheelAcc + Delta;
    Inc(Seq);
    Dirty := True;
  end;
end;

procedure BumpSeq;
begin
  Inc(Seq);
  Dirty := True;
end;

function AsciiName(b: Byte): AnsiString;
begin
  case b of
    32: Result := 'SPACE';
    9: Result := 'TAB';
    13: Result := 'RETURN';
    8, 127: Result := 'BACK';
    45: Result := 'MINUS';
    61: Result := 'EQUALS';
    91: Result := 'LBRACKET';
    93: Result := 'RBRACKET';
    59: Result := 'SEMICOLON';
    39: Result := 'APOSTROPHE';
    96: Result := 'GRAVE';
    92: Result := 'BACKSLASH';
    44: Result := 'COMMA';
    46: Result := 'PERIOD';
    47: Result := 'SLASH';
  else
    if (b >= 65) and (b <= 90) then
      Result := Chr(b)
    else if (b >= 97) and (b <= 122) then
      Result := Chr(b - 32)
    else if (b >= 48) and (b <= 57) then
      Result := Chr(b)
    else if (b >= 1) and (b <= 26) then
      Result := Chr(b + 64)
    else
      Result := '';
  end;
end;

function CsiFinalName(Num: LongInt; Fin: Char): AnsiString;
begin
  if Fin = '~' then
    case Num of
      1: Result := 'HOME';
      2: Result := 'INSERT';
      3: Result := 'DELETE';
      4: Result := 'END';
      5: Result := 'PAGEUP';
      6: Result := 'PAGEDOWN';
      11, 12, 13, 14, 15: Result := 'F' + IntToStr(Num - 10);
      17, 18, 19, 20, 21: Result := 'F' + IntToStr(Num - 11);
      23, 24: Result := 'F' + IntToStr(Num - 12);
    else
      Result := '';
    end
  else if Fin = 'A' then Result := 'UP'
  else if Fin = 'B' then Result := 'DOWN'
  else if Fin = 'C' then Result := 'RIGHT'
  else if Fin = 'D' then Result := 'LEFT'
  else if Fin = 'H' then Result := 'HOME'
  else if Fin = 'F' then Result := 'END'
  else Result := '';
end;

function SsFinalName(Fin: Char): AnsiString;
begin
  case Fin of
    'P': Result := 'F1';
    'Q': Result := 'F2';
    'R': Result := 'F3';
    'S': Result := 'F4';
    'H': Result := 'HOME';
    'F': Result := 'END';
  else
    Result := '';
  end;
end;

{ Feed buffered input bytes; returns True to keep running, False to quit
  (Ctrl-C). Incomplete trailing sequences stay buffered via Used count.
  A trailing lone ESC is held up to 100 ms for a follower; then it is the
  Escape key. }
var
  EscWait: Boolean = False;
  EscWaitTick: QWord = 0;

function FeedUnix(var Buf: array of Byte; var Used: Integer; NowTick: QWord): Boolean;
var
  i, j, k, Num, B, X, Y: LongInt;
  Nm: AnsiString;
  Fin: Char;
begin
  Result := True;
  i := 0;
  while i < Used do
  begin
    if Buf[i] = 3 then
    begin
      Result := False;
      Inc(i);
      Continue;
    end;
    if Buf[i] <> 27 then
    begin
      Nm := AsciiName(Buf[i]);
      if Nm <> '' then
        TouchKey(Nm, NowTick);
      Inc(i);
      Continue;
    end;
    { ESC ... }
    if i + 1 >= Used then
    begin
      { trailing lone ESC: hold briefly, then Escape }
      if not EscWait then
      begin
        EscWait := True;
        EscWaitTick := NowTick;
        Break;
      end;
      if NowTick - EscWaitTick > 100 then
      begin
        TouchKey('ESCAPE', NowTick);
        EscWait := False;
        Inc(i);
      end
      else
        Break;
      Continue;
    end;
    EscWait := False;
    if Buf[i + 1] = 91 then
    begin
      { ESC [ ... : SGR mouse, CSI, or extended }
      if (i + 3 < Used) and (Buf[i + 2] = 60) then
      begin
        { ESC [ < b ; x ; y M|m }
        j := i + 3;
        B := 0;
        while (j < Used) and (Buf[j] >= 48) and (Buf[j] <= 57) do
        begin
          B := B * 10 + (Buf[j] - 48);
          Inc(j);
        end;
        if (j >= Used) or (Buf[j] <> 59) then
          Break;
        Inc(j);
        X := 0;
        while (j < Used) and (Buf[j] >= 48) and (Buf[j] <= 57) do
        begin
          X := X * 10 + (Buf[j] - 48);
          Inc(j);
        end;
        if (j >= Used) or (Buf[j] <> 59) then
          Break;
        Inc(j);
        Y := 0;
        while (j < Used) and (Buf[j] >= 48) and (Buf[j] <= 57) do
        begin
          Y := Y * 10 + (Buf[j] - 48);
          Inc(j);
        end;
        if j >= Used then
          Break;
        Fin := Chr(Buf[j]);
        if (Fin <> 'M') and (Fin <> 'm') then
        begin
          Inc(i);
          Continue;
        end;
        if (B and 64) <> 0 then
        begin
          if (B and 1) = 0 then
            AddWheel(1)
          else
            AddWheel(-1);
          SetMouse(X, Y, Buttons);
        end
        else if Fin = 'm' then
        begin
          case B and 3 of
            0: Buttons := Buttons and not 1;
            1: Buttons := Buttons and not 4;
            2: Buttons := Buttons and not 2;
          else
            Buttons := 0;
          end;
          SetMouse(X, Y, Buttons);
        end
        else if (B and 32) <> 0 then
        begin
          case B and 3 of
            0: k := 1;
            1: k := 4;
            2: k := 2;
          else
            k := 0;
          end;
          SetMouse(X, Y, k);
        end
        else
        begin
          case B and 3 of
            0: k := 1;
            1: k := 4;
            2: k := 2;
          else
            k := Buttons;
          end;
          SetMouse(X, Y, Buttons or k);
        end;
        i := j + 1;
        Continue;
      end;
      { CSI: ESC [ digits [;...] final }
      j := i + 2;
      Num := -1;
      while (j < Used) and (Buf[j] >= 48) and (Buf[j] <= 57) do
      begin
        if Num < 0 then
          Num := 0;
        Num := Num * 10 + (Buf[j] - 48);
        Inc(j);
      end;
      while (j < Used) and ((Buf[j] = 59) or ((Buf[j] >= 48) and (Buf[j] <= 57))) do
        Inc(j);
      if j >= Used then
        Break;
      Fin := Chr(Buf[j]);
      if (Fin >= 'A') and (Fin <= 'Z') or (Fin = '~') then
      begin
        if Fin = '~' then
        begin
          if Num < 0 then
          begin
            i := j + 1;
            Continue;
          end;
          Nm := CsiFinalName(Num, Fin);
        end
        else
          Nm := CsiFinalName(-1, Fin);
        if Nm <> '' then
          TouchKey(Nm, NowTick);
        i := j + 1;
        Continue;
      end;
      Inc(i);
      Continue;
    end;
    if Buf[i + 1] = 79 then
    begin
      { ESC O x : SS3, exactly 3 bytes }
      if i + 2 >= Used then
        Break;
      Nm := SsFinalName(Chr(Buf[i + 2]));
      if Nm <> '' then
        TouchKey(Nm, NowTick);
      i := i + 3;
      Continue;
    end;
    { ESC + printable: Alt-held key, take the plain name }
    if (Buf[i + 1] >= 32) and (Buf[i + 1] <= 126) then
    begin
      Nm := AsciiName(Buf[i + 1]);
      if Nm <> '' then
        TouchKey(Nm, NowTick);
      i := i + 2;
      Continue;
    end;
    Inc(i);
  end;
  { compact leftover }
  if i > 0 then
  begin
    for j := i to Used - 1 do
      Buf[j - i] := Buf[j];
    Used := Used - i;
  end;
end;

procedure DbgLog(const Msg: AnsiString);
var
  F: TextFile;
  P: AnsiString;
begin
  P := ExtractFilePath(InputPath) + 'terminput.log';
  try
    AssignFile(F, P);
    if FileExists(P) then
      Append(F)
    else
      Rewrite(F);
    try
      WriteLn(F, FormatDateTime('hh:nn:ss.zzz', Now), ' ', Msg);
    finally
      CloseFile(F);
    end;
  except
    { logging must never kill the bridge }
  end;
end;

procedure WriteSnapshot;
var
  F: TextFile;
  Tmp: AnsiString;
  i: Integer;
  Line: AnsiString;
begin
  Tmp := InputPath + '.tmp';
  try
    AssignFile(F, Tmp);
    Rewrite(F);
    try
      WriteLn(F, 'SEQ ', Seq);
      Line := 'KEYS';
      for i := 0 to High(Pressed) do
        Line := Line + ' ' + Pressed[i].Name;
      WriteLn(F, Line);
      WriteLn(F, 'MOUSE ', MouseX, ' ', MouseY, ' ', Buttons);
      WriteLn(F, 'WHEEL ', WheelAcc);
    finally
      CloseFile(F);
    end;
    RenameFile(Tmp, InputPath);
  except
    on E: Exception do
      DbgLog('snapshot write failed: ' + E.Message);
  end;
  Dirty := False;
  LastWriteTick := TickNow;
end;

procedure WriteKeysUp;
begin
  SetLength(Pressed, 0);
  Inc(Seq);
  WriteSnapshot;
end;

{$IFNDEF MSWINDOWS}
var
  EscPending: Boolean = False;
  EscTick: QWord = 0;

procedure SetupUnixConsole;
var
  T: TermIOS;
begin
  if TCGetAttr(0, T) <> 0 then
  begin
    DbgLog('stdin is not a tty, running blind (pipe test?)');
    Exit;
  end;
  SavedTermios := T;
  HaveTermios := True;
  T.c_lflag := T.c_lflag and not (ICANON or ECHO or ECHOE or ECHOK or ISIG or IEXTEN);
  T.c_iflag := T.c_iflag and not (IXON or IXOFF or ICRNL or INLCR or IGNCR or ISTRIP or BRKINT or PARMRK);
  T.c_oflag := T.c_oflag and not OPOST;
  T.c_cflag := (T.c_cflag and not PARENB) or CS8;
  T.c_cc[VMIN] := 0;
  T.c_cc[VTIME] := 1;
  TCSetAttr(0, TCSANOW, T);
  Write(#27'[?1000h'#27'[?1006h');
  Flush(Output);
end;

procedure RestoreUnixConsole;
begin
  Write(#27'[?1006l'#27'[?1000l');
  Flush(Output);
  if HaveTermios then
    TCSetAttr(0, TCSANOW, SavedTermios);
end;

procedure RunUnix;
var
  Buf: array[0..255] of Byte;
  Used: LongInt = 0;
  Got: SizeInt;
  NowTick: QWord;
  Alive: Boolean = True;
  Waited: LongInt = 0;
begin
  SetupUnixConsole;
  try
    DbgLog('bridge start, waiting for frame file');
    while (not FileExists(FramePath)) and (Waited < 60000) do
    begin
      Sleep(100);
      Inc(Waited, 100);
    end;
    if not FileExists(FramePath) then
    begin
      DbgLog('no frame file in 60s, giving up');
      Exit;
    end;
    DbgLog('frame seen, entering loop');
    LastWriteTick := TickNow;
    WriteSnapshot;
    while Alive and FileExists(FramePath) do
    begin
      Got := fpRead(0, Buf[Used], SizeOf(Buf) - Used);
      NowTick := TickNow;
      if Got > 0 then
      begin
        Used := Used + Got;
        Alive := FeedUnix(Buf, Used, NowTick);
      end;
      ExpireKeys(NowTick);
      if Dirty or (NowTick - LastWriteTick >= HeartbeatMs) then
        WriteSnapshot;
    end;
    DbgLog('frame file gone, exiting clean');
  finally
    WriteKeysUp;
    RestoreUnixConsole;
  end;
end;
{$ENDIF}

{$IFDEF MSWINDOWS}
function VkName(Vk: DWORD): AnsiString;
begin
  case Vk of
    $41..$5A: Result := Chr(Vk);
    $30..$39: Result := Chr(Vk);
    $25: Result := 'LEFT';
    $26: Result := 'UP';
    $27: Result := 'RIGHT';
    $28: Result := 'DOWN';
    $20: Result := 'SPACE';
    $0D: Result := 'RETURN';
    $1B: Result := 'ESCAPE';
    $09: Result := 'TAB';
    $08: Result := 'BACK';
    $2E: Result := 'DELETE';
    $2D: Result := 'INSERT';
    $24: Result := 'HOME';
    $23: Result := 'END';
    $21: Result := 'PAGEUP';
    $22: Result := 'PAGEDOWN';
    $70..$7B: Result := 'F' + IntToStr(Vk - $6F);
    $A0: Result := 'LSHIFT';
    $A1: Result := 'RSHIFT';
    $A2: Result := 'LCONTROL';
    $A3: Result := 'RCONTROL';
    $A4: Result := 'LMENU';
    $A5: Result := 'RMENU';
    $BA: Result := 'SEMICOLON';
    $BB: Result := 'EQUALS';
    $BC: Result := 'COMMA';
    $BD: Result := 'MINUS';
    $BE: Result := 'PERIOD';
    $BF: Result := 'SLASH';
    $C0: Result := 'GRAVE';
    $DB: Result := 'LBRACKET';
    $DC: Result := 'BACKSLASH';
    $DD: Result := 'RBRACKET';
    $DE: Result := 'APOSTROPHE';
  else
    Result := '';
  end;
end;

function MyReadConsoleInput(h: THandle; var Buf; BufLen: DWORD;
  var Read: DWORD): BOOL; stdcall; external 'kernel32' name 'ReadConsoleInputA';
function MyGetNumberOfConsoleInputEvents(h: THandle;
  var Num: DWORD): BOOL; stdcall; external 'kernel32' name 'GetNumberOfConsoleInputEvents';

type
  TKeyEvent = packed record
    KeyDown: LongBool;
    RepeatCount: Word;
    VirtualKeyCode: Word;
    VirtualScanCode: Word;
    UnicodeChar: WideChar;
    ControlKeyState: DWORD;
  end;
  TMouseEvent = packed record
    dwMousePosition: packed record X, Y: SmallInt end;
    dwButtonState: DWORD;
    dwControlKeyState: DWORD;
    dwEventFlags: DWORD;
  end;
  TInputRecord = packed record
    EventType: Word;
    case Integer of
      0: (KeyEvent: TKeyEvent);
      1: (MouseEvent: TMouseEvent);
  end;

procedure RunWindows;
const
  ENABLE_MOUSE_INPUT = $10;
  ENABLE_WINDOW_INPUT = $8;
  ENABLE_EXTENDED_FLAGS = $80;
  ENABLE_QUICK_EDIT = $40;
var
  HIn: THandle;
  Mode, Num, I: DWORD;
  WaitedMs: LongInt = 0;
  Recs: array[0..63] of TInputRecord;
  Nm: AnsiString;
  Btns: LongInt;
begin
  HIn := GetStdHandle(STD_INPUT_HANDLE);
  if (HIn = 0) or (HIn = INVALID_HANDLE_VALUE) then
    Exit;
  if GetConsoleMode(HIn, Mode) then
    SetConsoleMode(HIn, (Mode or ENABLE_MOUSE_INPUT or ENABLE_WINDOW_INPUT
      or ENABLE_EXTENDED_FLAGS) and not ENABLE_QUICK_EDIT);
  try
    DbgLog('bridge start (windows), waiting for frame file');
    WaitedMs := 0;
    while (not FileExists(FramePath)) and (WaitedMs < 60000) do
    begin
      Sleep(100);
      Inc(WaitedMs, 100);
    end;
    if not FileExists(FramePath) then
    begin
      DbgLog('no frame file in 60s, giving up');
      Exit;
    end;
    DbgLog('frame seen, entering loop');
    LastWriteTick := TickNow;
    WriteSnapshot;
    while FileExists(FramePath) do
    begin
      if MyGetNumberOfConsoleInputEvents(HIn, Num) and (Num > 0) then
      begin
        if Num > 64 then
          Num := 64;
        if MyReadConsoleInput(HIn, Recs[0], Num, Num) then
          for I := 0 to Num - 1 do
          begin
            if Recs[I].EventType = 1 then
            begin
              if Recs[I].KeyEvent.KeyDown then
              begin
                Nm := VkName(Recs[I].KeyEvent.VirtualKeyCode);
                if Nm <> '' then
                  TouchKey(Nm, TickNow);
              end
              else
              begin
                { exact release event beats the hold timer }
                Nm := VkName(Recs[I].KeyEvent.VirtualKeyCode);
                if Nm <> '' then
                  DropKey(Nm);
              end;
            end
            else if Recs[I].EventType = 2 then
            begin
              with Recs[I].MouseEvent do
              begin
                if dwEventFlags = 4 then
                  AddWheel(SmallInt(dwButtonState shr 16))
                else
                begin
                  Btns := 0;
                  if (dwButtonState and 1) <> 0 then
                    Btns := Btns or 1;
                  if (dwButtonState and 2) <> 0 then
                    Btns := Btns or 2;
                  if (dwButtonState and 4) <> 0 then
                    Btns := Btns or 4;
                  SetMouse(dwMousePosition.X, dwMousePosition.Y, Btns);
                end;
              end;
            end;
          end;
      end;
      ExpireKeys(TickNow);
      if Dirty or (TickNow - LastWriteTick >= HeartbeatMs) then
        WriteSnapshot;
      Sleep(5);
    end;
  finally
    DbgLog('exiting, restoring console');
    WriteKeysUp;
    if GetConsoleMode(HIn, Mode) then
      SetConsoleMode(HIn, Mode and not (ENABLE_MOUSE_INPUT or ENABLE_WINDOW_INPUT));
  end;
end;
{$ENDIF}

procedure ShowHelp;
begin
  WriteLn('terminput: terminal -> game input bridge for Strata --terminal play.');
  WriteLn('Usage: terminput <inputfile> <framefile> [--selftest] [--help]');
  WriteLn('  Owns stdin (raw + mouse tracking); exits when <framefile> vanishes.');
  WriteLn('  --selftest runs the parser vectors without a tty.');
end;

{ --selftest: deterministic parser vectors (explicit timestamps). }
function SelfTest: Boolean;
var
  Buf: array[0..255] of Byte;
  Used: Integer;
  LastAlive: Boolean;

  procedure FeedStr(const S: AnsiString; T: QWord);
  var
    k: Integer;
  begin
    for k := 1 to Length(S) do
    begin
      Buf[Used] := Ord(S[k]);
      Inc(Used);
    end;
    LastAlive := FeedUnix(Buf, Used, T);
  end;

  function HasKey(const Nm: AnsiString): Boolean;
  var
    k: Integer;
  begin
    Result := False;
    for k := 0 to High(Pressed) do
      if Pressed[k].Name = Nm then
        Exit(True);
  end;

  procedure Check(Cond: Boolean; const Msg: AnsiString);
  begin
    if Cond then
      WriteLn('PASS: ', Msg)
    else
    begin
      WriteLn('FAIL: ', Msg);
      Result := False;
    end;
  end;

begin
  Result := True;
  Used := 0;
  SetLength(Pressed, 0);
  Dirty := False;
  FeedStr('a', 1000);
  Check(HasKey('A'), 'letter press registers');
  ExpireKeys(1100);
  Check(HasKey('A'), 'held inside window');
  ExpireKeys(1400);
  Check(not HasKey('A'), 'expires after HoldMs');
  FeedStr(#27'[A', 2000);
  Check(HasKey('UP'), 'arrow up');
  FeedStr(#27'[3~', 2100);
  Check(HasKey('DELETE'), 'delete key');
  FeedStr(' ', 2200);
  Check(HasKey('SPACE'), 'space');
  FeedStr(#27'[24~', 2300);
  Check(HasKey('F12'), 'F12');
  FeedStr(#27'OP', 2400);
  Check(HasKey('F1'), 'ss3 F1');
  FeedStr(#27, 5000);
  ExpireKeys(5000);
  Check(Used = 1, 'lone ESC held, not consumed');
  Check(not HasKey('ESCAPE'), 'no premature escape');
  FeedUnix(Buf, Used, 5200);
  Check(HasKey('ESCAPE'), 'lone ESC fires after timeout');
  Check(Used = 0, 'ESC consumed');
  FeedStr(#27'[<0;10;20M', 6000);
  Check((MouseX = 10) and (MouseY = 20) and (Buttons = 1), 'mouse press pos+button');
  FeedStr(#27'[<0;10;20m', 6100);
  Check((MouseX = 10) and (MouseY = 20) and (Buttons = 0), 'mouse release clears');
  FeedStr(#27'[<64;5;5M', 6200);
  Check(WheelAcc = 1, 'wheel up notch');
  FeedStr(#27'[<65;5;5M', 6300);
  Check(WheelAcc = 0, 'wheel down notch');
  FeedStr(#27'[<35;7;9M', 6400);
  Check((MouseX = 7) and (MouseY = 9) and (Buttons = 0), 'motion without buttons');
  Check(LastAlive, 'no quit requested');
  FeedStr(#3, 7100);
  Check(not LastAlive, 'ctrl-C requests quit');
  WriteLn('selftest done');
end;

var
  i: Integer;
  DoTest: Boolean = False;
begin
  for i := 1 to ParamCount do
  begin
    if ParamStr(i) = '--selftest' then
      DoTest := True
    else if (ParamStr(i) = '--help') or (ParamStr(i) = '-h') then
    begin
      ShowHelp;
      Halt(0);
    end;
  end;
  if DoTest then
  begin
    if SelfTest then
      Halt(0)
    else
      Halt(1);
  end;
  if ParamCount < 2 then
  begin
    ShowHelp;
    Halt(1);
  end;
  for i := 3 to ParamCount do
  begin
    ShowHelp;
    Halt(1);
  end;
  InputPath := ParamStr(1);
  FramePath := ParamStr(2);
  {$IFDEF MSWINDOWS}
  RunWindows;
  {$ELSE}
  RunUnix;
  {$ENDIF}
end.
