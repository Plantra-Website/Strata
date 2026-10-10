*> STRATA terminal viewer in COBOL.
*> Reads /tmp/strata-frame.bin (8-char counter + W*H*3 raw RGB top-down)
*> published by FrameTap --terminal, renders to terminal.
*> Modes: TRUECOLOR half-blocks (2 px per cell) and ASCII grayscale.
*> Usage: viewer [W H] [--mode truecolor|ascii] [--fps N] [--once]
*>               [--file PATH] [--help]
*> Defaults: 160 120 truecolor 30fps /tmp/strata-frame.bin loop.
identification division.
program-id. STRATA-VIEWER.
environment division.
input-output section.
file-control.
    select frame-file assign to frame-path
    organization is binary sequential
    file status is fs-code.
data division.
file section.
fd frame-file.
01 frame-rec pic x(921608).
working-storage section.
01 ws-pix pic x(921600).
01 ws-bytes redefines ws-pix pic 9(2) comp-5
   occurs 921600 times.
01 pix-count pic 9(9) comp-5 value 0.
01 raw-top pic 9(9) comp-5 value 0.
01 raw-bot pic 9(9) comp-5 value 0.
01 frame-path pic x(1024) value "/tmp/strata-frame.bin".
01 fs-code pic xx.
01 argcount pic 9(9) comp-5 value 0.
01 argval pic x(1024).
01 i pic 9(9) comp-5 value 0.
01 w-val pic 9(9) comp-5 value 160.
01 h-val pic 9(9) comp-5 value 120.
01 pos-count pic 9(9) comp-5 value 0.
01 mode-val pic x(16) value "TRUECOLOR".
01 mono-flag pic 9 value 0.
01 fps-val pic 9(9) comp-5 value 30.
01 scale-val pic 9(9) comp-5 value 1.
01 step3 pic 9(9) comp-5 value 3.
01 dcols pic 9(9) comp-5 value 160.
01 drows pic 9(9) comp-5 value 60.
01 once-flag pic 9 value 0.
01 need-for pic x(16) value spaces.
01 tmp-trim pic x(1024).
01 tmp-up pic x(1024).
01 expected-size pic 9(18) comp-5 value 0.
01 file-size-num pic 9(18) comp-5 value 0.
01 finf.
   05 fe-size pic x(8) comp-x.
   05 fe-day pic x comp-x.
   05 fe-month pic x comp-x.
   05 fe-year pic x(2) comp-x.
   05 fe-hh pic x comp-x.
   05 fe-mm pic x comp-x.
   05 fe-ss pic x comp-x.
   05 fe-hh2 pic x comp-x.
01 st-code pic s9(9) comp-5 value 0.
01 last-counter pic x(8) value spaces.
01 curr-counter pic x(8) value spaces.
01 frames-done pic 9(18) comp-5 value 0.
01 exit-flag pic 9 value 0.
01 wait-shown pic 9 value 0.
01 nsec pic 9(18) comp-5 value 33333333.
01 esc-char pic x value x"1B".
01 block-char pic x(3) value x"E29680".
01 ramp-str pic x(70) value " .'`^"",:;Il!i><~+_-?][}{1)(|\/tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@$".
01 ramp-max pic 9(9) comp-5 value 69.
01 bright-f pic 9v9(6) value 0.
01 gamma-tab.
   05 gidx occurs 256 times pic 9(2) comp-5 value 0.
01 fgf-tab.
   05 fgf-e occurs 256 times pic x(12).
01 fgfl-tab.
   05 fgfl-e occurs 256 times pic 9(2) comp-5 value 0.
01 bgf-tab.
   05 bgf-e occurs 256 times pic x(12).
01 bgfl-tab.
   05 bgfl-e occurs 256 times pic 9(2) comp-5 value 0.
01 mid-tab.
   05 mid-e occurs 256 times pic x(4).
01 midl-tab.
   05 midl-e occurs 256 times pic 9(2) comp-5 value 0.
01 prev-rt pic 9(9) comp-5 value 0.
01 prev-gt pic 9(9) comp-5 value 0.
01 prev-bt pic 9(9) comp-5 value 0.
01 prev-rb pic 9(9) comp-5 value 0.
01 prev-gb pic 9(9) comp-5 value 0.
01 prev-bb pic 9(9) comp-5 value 0.
01 prev-pair pic 9(9) comp-5 value 0.
01 top-avg pic 9(9) comp-5 value 0.
01 bot-avg pic 9(9) comp-5 value 0.
01 ch-tmp pic 9(9) comp-5 value 0.
01 fg-code pic 9(9) comp-5 value 0.
01 bg-code pic 9(9) comp-5 value 0.
01 pair-idx pic 9(9) comp-5 value 0.
01 mx pic 9(9) comp-5 value 0.
01 mn pic 9(9) comp-5 value 0.
01 spread pic 9(9) comp-5 value 0.
01 f5-tab.
   05 f5-e occurs 256 times pic x(12).
01 f5l-tab.
   05 f5l-e occurs 256 times pic 9(2) comp-5 value 0.
01 b5-tab.
   05 b5-e occurs 256 times pic x(12).
01 b5l-tab.
   05 b5l-e occurs 256 times pic 9(2) comp-5 value 0.
01 row-first pic 9 value 1.
01 rowbase-top pic 9(9) comp-5 value 0.
01 rowbase-bot pic 9(9) comp-5 value 0.
01 line-buf pic x(32768).
01 ptr pic 9(9) comp-5 value 1.
01 line-len pic 9(9) comp-5 value 0.
01 px pic 9(9) comp-5 value 0.
01 py pic 9(9) comp-5 value 0.
01 py2 pic 9(9) comp-5 value 0.
01 rows-val pic 9(9) comp-5 value 0.
01 ty-val pic 9(9) comp-5 value 0.
01 by-val pic 9(9) comp-5 value 0.
01 idx pic 9(9) comp-5 value 0.
01 idx-top pic 9(9) comp-5 value 0.
01 idx-bot pic 9(9) comp-5 value 0.
01 rv pic 9(9) comp-5 value 0.
01 gv pic 9(9) comp-5 value 0.
01 bv pic 9(9) comp-5 value 0.
01 rb pic 9(9) comp-5 value 0.
01 gb pic 9(9) comp-5 value 0.
01 bb pic 9(9) comp-5 value 0.
01 bright pic 9(9) comp-5 value 0.
01 ramp-idx pic 9(9) comp-5 value 0.
01 numtab.
   05 numentry occurs 256 times.
      10 nstr pic x(3) value spaces.
      10 nlen pic 9 comp-5 value 0.
01 tmp-fmt pic zz9.
01 tmp-num pic 9(9) comp-5 value 0.
01 num-arg pic 9(9) comp-5 value 0.
procedure division.
main-section.
    perform init-numtab
    perform parse-args
    compute expected-size = 8 + w-val * h-val * 3
    *> Display size: stride sampling keeps every scale-val-th pixel, so
    *> the frame takes fewer cells than the terminal has (no wrap/scroll
    *> tear). Buffer math stays in whole cells.
    compute dcols = w-val / scale-val
    compute drows = function integer(
        (h-val + 2 * scale-val - 1) / (2 * scale-val))
    compute step3 = scale-val * 3
    compute nsec = 1000000000 / fps-val
    display "STRATA-VIEWER " w-val "x" h-val " "
        function trim(mode-val) " fps=" fps-val
        " scale=" scale-val " (" dcols "x" drows " cells)"
        " file=" function trim(frame-path)
    display esc-char "[?25l" with no advancing
    display esc-char "[2J" with no advancing
    display esc-char "[H" with no advancing
    perform until exit-flag = 1
        call "CBL_CHECK_FILE_EXIST" using frame-path finf
            returning st-code
        end-call
        if st-code not = 0
            if frames-done > 0
                display "FRAME-GONE: game exited, quitting."
                move 1 to exit-flag
            else
                if once-flag = 1
                    display "NO-FRAME: " function trim(frame-path)
                        " missing."
                    display esc-char "[?25h"
                    stop run returning 1
                end-if
                if wait-shown = 0
                    display "WAITING: " function trim(frame-path)
                        " (run game with --terminal "
                        w-val "x" h-val ")..."
                    move 1 to wait-shown
                end-if
                call "CBL_GC_NANOSLEEP" using nsec
                end-call
            end-if
        else
            move fe-size to file-size-num
            if file-size-num not = expected-size
                display "SIZE-MISMATCH: file=" file-size-num
                    " expected=" expected-size
                    " for " w-val "x" h-val
                    " (restart viewer with correct W H)."
                display esc-char "[?25h"
                stop run returning 1
            end-if
            open input frame-file
            if fs-code not = "00"
                call "CBL_GC_NANOSLEEP" using nsec
                end-call
            else
                read frame-file
                    at end
                        close frame-file
                        call "CBL_GC_NANOSLEEP" using nsec
                        end-call
                    not at end
                        close frame-file
                        move frame-rec(1:8) to curr-counter
                        compute pix-count = w-val * h-val * 3
                        move frame-rec(9:pix-count)
                            to ws-pix(1:pix-count)
                        if curr-counter = last-counter
                            call "CBL_GC_NANOSLEEP" using nsec
                            end-call
                        else
                            move curr-counter to last-counter
                            display esc-char "[H" with no advancing
                            if function trim(mode-val) = "ASCII"
                                or function trim(mode-val) = "MONO"
                                perform render-ascii
                            else
                                if function trim(mode-val) = "ANSI"
                                    perform render-ansi16
                                else
                                    perform render-truecolor
                                end-if
                            end-if
                            add 1 to frames-done
                            if once-flag = 1
                                move 1 to exit-flag
                            else
                                call "CBL_GC_NANOSLEEP" using nsec
                                end-call
                            end-if
                        end-if
                end-read
            end-if
        end-if
    end-perform
    display esc-char "[?25h"
    display "DONE frames=" frames-done
    stop run.
init-numtab.
    perform varying i from 0 by 1 until i > 255
        move i to tmp-fmt
        move function trim(tmp-fmt) to nstr(i + 1)
        if i < 10
            move 1 to nlen(i + 1)
        else
            if i < 100
                move 2 to nlen(i + 1)
            else
                move 3 to nlen(i + 1)
            end-if
        end-if
        *> sqrt gamma lift (dark-scene readability), baked once so the
        *> per-cell path stays integer-only.
        compute bright-f = function sqrt(i / 255)
        compute gidx(i + 1) = bright-f * ramp-max
        *> escape fragments for separators only ("\ESC[38;2;N;", "N;").
        *> The FINAL component of a sequence must use the plain number
        *> (nstr) + "m": a trailing ";" before "m" is an empty param =
        *> SGR 0 reset, which greys the whole screen. Don't regress this.
        string esc-char "[38;2;" delimited by size
            function trim(tmp-fmt) delimited by size
            ";" delimited by size
            into fgf-e(i + 1)
        end-string
        compute fgfl-e(i + 1) = function stored-char-length(
            function trim(fgf-e(i + 1)))
        string esc-char "[48;2;" delimited by size
            function trim(tmp-fmt) delimited by size
            ";" delimited by size
            into bgf-e(i + 1)
        end-string
        compute bgfl-e(i + 1) = function stored-char-length(
            function trim(bgf-e(i + 1)))
        string function trim(tmp-fmt) delimited by size
            ";" delimited by size
            into mid-e(i + 1)
        end-string
        compute midl-e(i + 1) = function stored-char-length(
            function trim(mid-e(i + 1)))
        *> 256-color "\ESC[38;5;Nm" / "\ESC[48;5;Nm" fragments: short,
        *> single-param SGR (no empty params, ever).
        move i to tmp-fmt
        string esc-char "[38;5;" delimited by size
            function trim(tmp-fmt) delimited by size
            "m" delimited by size
            into f5-e(i + 1)
        end-string
        compute f5l-e(i + 1) = function stored-char-length(
            function trim(f5-e(i + 1)))
        string esc-char "[48;5;" delimited by size
            function trim(tmp-fmt) delimited by size
            "m" delimited by size
            into b5-e(i + 1)
        end-string
        compute b5l-e(i + 1) = function stored-char-length(
            function trim(b5-e(i + 1)))
    end-perform.
parse-args.
    accept argcount from argument-number
    end-accept
    perform varying i from 1 by 1 until i > argcount
        accept argval from argument-value
        end-accept
        move function trim(argval) to tmp-trim
        move function upper-case(tmp-trim) to tmp-up
        if need-for = "MODE"
            move tmp-up to mode-val
            if mode-val = "ANSI16"
                move "ANSI" to mode-val
            end-if
            move spaces to need-for
        else
            if need-for = "FPS"
                if function trim(tmp-trim) is not numeric
                    display "BAD-FPS: " function trim(argval)
                    stop run returning 1
                end-if
                compute fps-val = function numval(tmp-trim)
                if fps-val < 1 or fps-val > 60
                    display "BAD-FPS 1..60: " function trim(argval)
                    stop run returning 1
                end-if
                move spaces to need-for
            else
                if need-for = "FILE"
                    move tmp-trim to frame-path
                    move spaces to need-for
                else
                if need-for = "SCALE"
                    if function trim(tmp-trim) is not numeric
                        display "BAD-SCALE: " function trim(argval)
                        stop run returning 1
                    end-if
                    compute scale-val = function numval(tmp-trim)
                    if scale-val < 1 or scale-val > 8
                        display "BAD-SCALE 1..8: " function trim(argval)
                        stop run returning 1
                    end-if
                    move spaces to need-for
                else
                    if tmp-trim = "--mode"
                        move "MODE" to need-for
                    else
                        if tmp-trim = "--fps"
                            move "FPS" to need-for
                        else
                            if tmp-trim = "--scale"
                                move "SCALE" to need-for
                            else
                            if tmp-trim = "--file"
                                move "FILE" to need-for
                            else
                                if tmp-trim = "--once"
                                    move 1 to once-flag
                                else
                                    if tmp-trim = "--ascii"
                                        move "ASCII" to mode-val
                                    else
                                        if tmp-trim = "--ansi"
                                            move "ANSI" to mode-val
                                        else
                                        if tmp-trim = "--mono"
                                            or tmp-trim = "--grey"
                                            or tmp-trim = "--gray"
                                            move "MONO" to mode-val
                                        else
                                        if tmp-trim = "--truecolor"
                                            move "TRUECOLOR" to mode-val
                                        else
                                            if tmp-up = "ASCII"
                                                move "ASCII" to mode-val
                                            else
                                                if tmp-up = "ANSI"
                                                    or tmp-up = "ANSI16"
                                                    move "ANSI" to mode-val
                                            else
                                                if tmp-up = "MONO"
                                                    or tmp-up = "GRAY"
                                                    or tmp-up = "GREY"
                                                    move "MONO" to mode-val
                                            else
                                                if tmp-up = "TRUECOLOR"
                                                    or tmp-up = "TRUE"
                                                    or tmp-up = "TC"
                                                    or tmp-up = "COLOR"
                                                    move "TRUECOLOR"
                                                        to mode-val
                                                else
                                                    if tmp-trim = "--help"
                                                        or tmp-trim = "-h"
                                                        perform show-usage
                                                        stop run
                                                    else
                                                        if tmp-trim(1:7)
                                                            = "--mode="
                                                            move tmp-up(8:)
                                                                to mode-val
                                                            if mode-val
                                                                = "ANSI16"
                                                                move "ANSI"
                                                                to mode-val
                                                            end-if
                                                            if mode-val
                                                                not = "ASCII"
                                                                and mode-val
                                                                not = "TRUECOLOR"
                                                                and mode-val
                                                                not = "MONO"
                                                                and mode-val
                                                                not = "ANSI"
                                                                display
                                                                "BAD-MODE: "
                                                                function trim(
                                                                argval)
                                                                stop run
                                                                returning 1
                                                            end-if
                                                        else
                                                            if tmp-trim(1:6)
                                                                = "--fps="
                                                                move tmp-trim(
                                                                7:)
                                                                to tmp-trim
                                                                if function trim(
                                                                tmp-trim)
                                                                is not numeric
                                                                    display
                                                                    "BAD-FPS: "
                                                                    function
                                                                    trim(
                                                                    argval)
                                                                    stop run
                                                                    returning 1
                                                                end-if
                                                                compute fps-val
                                                                = function
                                                                numval(
                                                                tmp-trim)
                                                                if fps-val < 1
                                                                or fps-val
                                                                > 60
                                                                    display
                                                                    "BAD-FPS: "
                                                                    function
                                                                    trim(
                                                                    argval)
                                                                    stop run
                                                                    returning 1
                                                                end-if
                                                            else
                                                                if tmp-trim(
                                                                1:7)
                                                                = "--file="
                                                                    move
                                                                    function
                                                                    trim(
                                                                    argval)
                                                                    to tmp-trim
                                                                    move
                                                                    tmp-trim(
                                                                    8:)
                                                                    to
                                                                    frame-path
                                                                else
                                                                    if tmp-trim(
                                                                    1:2)
                                                                    = "--"
                                                                        display
                                                                        "BAD-ARG: "
                                                                        function
                                                                        trim(
                                                                        argval)
                                                                        perform
                                                                        show-usage
                                                                        stop run
                                                                        returning 1
                                                                    else
                                                                        if function trim(
                                                                        tmp-trim)
                                                                        is numeric
                                                                            compute
                                                                            num-arg
                                                                            = function
                                                                            numval(
                                                                            tmp-trim)
                                                                            if
                                                                            pos-count
                                                                            = 0
                                                                                move
                                                                                num-arg
                                                                                to
                                                                                w-val
                                                                                if
                                                                                w-val
                                                                                < 16
                                                                                or
                                                                                w-val
                                                                                > 640
                                                                                    display
                                                                                    "BAD-W 16..640"
                                                                                    stop run
                                                                                    returning 1
                                                                                end-if
                                                                                add 1
                                                                                to
                                                                                pos-count
                                                                            else
                                                                                if
                                                                                pos-count
                                                                                = 1
                                                                                    move
                                                                                    num-arg
                                                                                    to
                                                                                    h-val
                                                                                    if
                                                                                    h-val
                                                                                    < 16
                                                                                    or
                                                                                    h-val
                                                                                    > 480
                                                                                        display
                                                                                        "BAD-H 16..480"
                                                                                        stop run
                                                                                        returning 1
                                                                                    end-if
                                                                                    add 1
                                                                                    to
                                                                                    pos-count
                                                                                else
                                                                                    display
                                                                                    "EXTRA-NUM: "
                                                                                    function
                                                                                    trim(
                                                                                    argval)
                                                                                    stop run
                                                                                    returning 1
                                                                                end-if
                                                                            end-if
                                                                        else
                                                                            display
                                                                            "BAD-ARG: "
                                                                            function
                                                                            trim(
                                                                            argval)
                                                                            perform
                                                                            show-usage
                                                                            stop run
                                                                            returning 1
                                                                        end-if
                                                                    end-if
                                                                end-if
                                                            end-if
                                                        end-if
                                                    end-if
                                                end-if
                                            end-if
                                        end-if
                                    end-if
                                end-if
                            end-if
                        end-if
                    end-if
                end-if
            end-if
        end-if
    end-if
    end-if
    end-if
    end-if
    end-if
    end-perform
    if need-for not = spaces
        display "MISSING-VALUE for --" function trim(need-for)
        stop run returning 1
    end-if
    if mode-val not = "ASCII" and mode-val not = "TRUECOLOR"
        and mode-val not = "MONO" and mode-val not = "ANSI"
        display "BAD-MODE use truecolor|ascii|mono|ansi, got: "
            function trim(mode-val)
        stop run returning 1
    end-if
    if mode-val = "MONO"
        move 1 to mono-flag
    end-if.
show-usage.
    display "USAGE: viewer [W H] [--mode truecolor|ascii|mono|ansi]"
        " [--fps 1..60] [--scale 1..8] [--once] [--file PATH]".
    display "  W H default 160 120 (16..640 x 16..480), must match"
        " game --terminal WxH.".
    display "  --mode truecolor = half-block 24-bit (default),"
        " ascii = fast colored text, mono = fast greyscale text,"
        " ansi = fast 256-color blocks (cube + grey ramp).".
    display "  --fps N frame cap 1..60 (default 30; game emits ~15).".
    display "  --scale N stride 1..8 (default 1): shows every Nth pixel,"
        " so the frame takes fewer cells than the terminal (no wrap"
        " or scroll tear). E.g. 320x240 tap at --scale 2 -> 160x120.".
    display "  --once renders one frame then quits; default loops"
        " until /tmp/strata-frame.bin is deleted.".
render-ascii.
    *> Fast colored ASCII: one char per 2x1 pixel block (terminal cells
    *> are ~2x as tall as wide, so this keeps proportions right), fg set
    *> to the block's average color, glyph from a 70-level ramp with a
    *> sqrt gamma lift so torch-lit dark scenes stay readable.
    perform varying py2 from 0 by 1 until py2 >= drows
        move 1 to ptr
        move 1 to row-first
        compute ty-val = py2 * 2 * scale-val
        compute by-val = ty-val + scale-val
        compute rowbase-top = ty-val * w-val * 3
        move rowbase-top to raw-top
        if by-val < h-val
            compute rowbase-bot = by-val * w-val * 3
            move rowbase-bot to raw-bot
        end-if
        perform varying px from 0 by 1 until px >= dcols
            move ws-bytes(raw-top + 1) to rv
            move ws-bytes(raw-top + 2) to gv
            move ws-bytes(raw-top + 3) to bv
            if by-val < h-val
                move ws-bytes(raw-bot + 1) to rb
                move ws-bytes(raw-bot + 2) to gb
                move ws-bytes(raw-bot + 3) to bb
            else
                move 0 to rb
                move 0 to gb
                move 0 to bb
            end-if
            compute rv = (rv + rb) / 2
            compute gv = (gv + gb) / 2
            compute bv = (bv + bb) / 2
            compute bright = (rv * 299 + gv * 587 + bv * 114) / 1000
            move gidx(bright + 1) to ramp-idx
            if ramp-idx > ramp-max
                move ramp-max to ramp-idx
            end-if
            if mono-flag = 1
                move ramp-str(ramp-idx + 1:1) to line-buf(px + 1:1)
            else
            if row-first = 1
                or rv not = prev-rt or gv not = prev-gt
                or bv not = prev-bt
                move rv to prev-rt
                move gv to prev-gt
                move bv to prev-bt
                move 0 to row-first
            string fgf-e(rv + 1)(1:fgfl-e(rv + 1)) delimited by size
                mid-e(gv + 1)(1:midl-e(gv + 1)) delimited by size
                nstr(bv + 1)(1:nlen(bv + 1)) delimited by size
                "m" delimited by size
                ramp-str(ramp-idx + 1:1) delimited by size
                into line-buf with pointer ptr
            end-string
            else
                move ramp-str(ramp-idx + 1:1) to line-buf(ptr:1)
                add 1 to ptr
            end-if
            end-if
            add step3 to raw-top
            add step3 to raw-bot
        end-perform
        if mono-flag = 1
            display line-buf(1:dcols)
        else
        string esc-char "[0m" delimited by size
            into line-buf with pointer ptr
        end-string
        compute line-len = ptr - 1
        display line-buf(1:line-len)
        end-if
    end-perform.
render-truecolor.
    perform varying py2 from 0 by 1 until py2 >= drows
        move 1 to ptr
        move 1 to row-first
        compute ty-val = py2 * 2 * scale-val
        compute by-val = ty-val + scale-val
        compute rowbase-top = ty-val * w-val * 3
        move rowbase-top to raw-top
        if by-val < h-val
            compute rowbase-bot = by-val * w-val * 3
            move rowbase-bot to raw-bot
        end-if
        perform varying px from 0 by 1 until px >= dcols
            move ws-bytes(raw-top + 1) to rv
            move ws-bytes(raw-top + 2) to gv
            move ws-bytes(raw-top + 3) to bv
            if by-val < h-val
                move ws-bytes(raw-bot + 1) to rb
                move ws-bytes(raw-bot + 2) to gb
                move ws-bytes(raw-bot + 3) to bb
            else
                move 0 to rb
                move 0 to gb
                move 0 to bb
            end-if
            *> Same colors as the previous cell: the terminal still has
            *> them selected, so emit just the block (flat sky/walls
            *> collapse to ~3 bytes/cell instead of ~40).
            if row-first = 1
                or rv not = prev-rt or gv not = prev-gt
                or bv not = prev-bt or rb not = prev-rb
                or gb not = prev-gb or bb not = prev-bb
                move rv to prev-rt
                move gv to prev-gt
                move bv to prev-bt
                move rb to prev-rb
                move gb to prev-gb
                move bb to prev-bb
                move 0 to row-first
            string fgf-e(rv + 1)(1:fgfl-e(rv + 1)) delimited by size
                mid-e(gv + 1)(1:midl-e(gv + 1)) delimited by size
                nstr(bv + 1)(1:nlen(bv + 1)) delimited by size
                "m" delimited by size
                bgf-e(rb + 1)(1:bgfl-e(rb + 1)) delimited by size
                mid-e(gb + 1)(1:midl-e(gb + 1)) delimited by size
                nstr(bb + 1)(1:nlen(bb + 1)) delimited by size
                "m" delimited by size
                block-char delimited by size
                into line-buf with pointer ptr
            end-string
            else
                move block-char to line-buf(ptr:3)
                add 3 to ptr
            end-if
            add step3 to raw-top
            add step3 to raw-bot
        end-perform
        string esc-char "[0m" delimited by size
            into line-buf with pointer ptr
        end-string
        compute line-len = ptr - 1
        display line-buf(1:line-len)
    end-perform.
render-ansi16.
    *> Fastest mode: half-blocks quantized to the 16 ANSI colors
    *> (threshold 128/channel, bright bit on average). Only 256 fg/bg
    *> combos exist, so flat scenes collapse to ~3 bytes/cell and the
    *> escapes are short absolute SGR (no empty params, ever).
    perform varying py2 from 0 by 1 until py2 >= drows
        move 1 to ptr
        move 1 to row-first
        compute ty-val = py2 * 2 * scale-val
        compute by-val = ty-val + scale-val
        compute rowbase-top = ty-val * w-val * 3
        move rowbase-top to raw-top
        if by-val < h-val
            compute rowbase-bot = by-val * w-val * 3
            move rowbase-bot to raw-bot
        end-if
        perform varying px from 0 by 1 until px >= dcols
            move ws-bytes(raw-top + 1) to rv
            move ws-bytes(raw-top + 2) to gv
            move ws-bytes(raw-top + 3) to bv
            if by-val < h-val
                move ws-bytes(raw-bot + 1) to rb
                move ws-bytes(raw-bot + 2) to gb
                move ws-bytes(raw-bot + 3) to bb
            else
                move 0 to rb
                move 0 to gb
                move 0 to bb
            end-if
            *> 256-color mapping: grey ramp for flat (low-spread)
            *> content, 6x6x6 cube otherwise.
            move rv to mx
            if gv > mx
                move gv to mx
            end-if
            if bv > mx
                move bv to mx
            end-if
            move rv to mn
            if gv < mn
                move gv to mn
            end-if
            if bv < mn
                move bv to mn
            end-if
            compute spread = mx - mn
            if spread < 32
                compute top-avg = (rv + gv + bv) / 3
                if top-avg <= 8
                    move 16 to fg-code
                else
                    compute fg-code = 232 + (top-avg - 8) / 10
                    if fg-code > 255
                        move 255 to fg-code
                    end-if
                end-if
            else
                *> NOTE: one channel per COMPUTE — intermediates stay
                *> decimal, so folding them shares fractions across
                *> channels (wrong cube cell). Truncate each first.
                compute ch-tmp = (rv * 5) / 255
                compute fg-code = 16 + ch-tmp * 36
                compute ch-tmp = (gv * 5) / 255
                compute fg-code = fg-code + ch-tmp * 6
                compute ch-tmp = (bv * 5) / 255
                compute fg-code = fg-code + ch-tmp
            end-if
            move rb to mx
            if gb > mx
                move gb to mx
            end-if
            if bb > mx
                move bb to mx
            end-if
            move rb to mn
            if gb < mn
                move gb to mn
            end-if
            if bb < mn
                move bb to mn
            end-if
            compute spread = mx - mn
            if spread < 32
                compute bot-avg = (rb + gb + bb) / 3
                if bot-avg <= 8
                    move 16 to bg-code
                else
                    compute bg-code = 232 + (bot-avg - 8) / 10
                    if bg-code > 255
                        move 255 to bg-code
                    end-if
                end-if
            else
                compute ch-tmp = (rb * 5) / 255
                compute bg-code = 16 + ch-tmp * 36
                compute ch-tmp = (gb * 5) / 255
                compute bg-code = bg-code + ch-tmp * 6
                compute ch-tmp = (bb * 5) / 255
                compute bg-code = bg-code + ch-tmp
            end-if
            compute pair-idx = fg-code * 256 + bg-code
            if row-first = 1 or pair-idx not = prev-pair
                move pair-idx to prev-pair
                move 0 to row-first
            string f5-e(fg-code + 1)(1:f5l-e(fg-code + 1))
                delimited by size
                b5-e(bg-code + 1)(1:b5l-e(bg-code + 1))
                delimited by size
                block-char delimited by size
                into line-buf with pointer ptr
            end-string
            else
                move block-char to line-buf(ptr:3)
                add 3 to ptr
            end-if
            add step3 to raw-top
            add step3 to raw-bot
        end-perform
        string esc-char "[0m" delimited by size
            into line-buf with pointer ptr
        end-string
        compute line-len = ptr - 1
        display line-buf(1:line-len)
    end-perform.
