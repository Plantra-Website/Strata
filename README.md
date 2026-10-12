# Strata

/ˈstrɑːtɑː/
STRAH-tah

A blocky voxel sandbox in Java (LWJGL2): dig, build, and survive from dawn
through torch-lit midnight.

```sh
./build.sh        # compile everything into out/
./build.sh run    # compile and launch the game
./build.sh test   # compile and run the headless suite
```

Windows uses the native mirror instead (`build.bat` with the same
`build`/`run`/`test`/`jar` commands; needs a JDK 17+ via `JAVA_HOME`,
`PATH`, or a Temurin-style install — with none found it downloads
Temurin 17 once to `%LOCALAPPDATA%\Strata\jdk17`, so a fresh clone
builds with zero setup):

```cmd
build.bat test
```

Needs a JDK 17+ (`JAVA_HOME`, on `PATH`, or a standard Temurin-style
install — the script finds it).

## Play (no build needed)

Download your platform's jar from the Releases page and double-click it
(or `java -jar <file>`):

- `Strata-<version>-mac-arm64.jar` — Apple Silicon
- `Strata-<version>-mac-x64.jar` — Intel Macs
- `Strata-<version>-windows-x64.jar` — Windows 64-bit
- `Strata-<version>-linux-x64.jar` — Linux 64-bit

Needs a Java 17+ runtime, nothing else to install (natives ride inside
each jar).

## Boot flags

```sh
./build.sh run --seed N --world DIR --import IMG --pack NAME
```

- `--seed N` — new world / reseed (default: stored seed).
- `--world DIR` — world slot dir owning `region/` + `world.dat` +
  `player.dat` (default `saves/testing`).
- `--import IMG` — paste a grayscale picture (PNG/JPG) as blocks into a
  FRESH void world, then play it (needs an unused `--world` dir);
  luminance picks the nearest block on a 9-step dark→light ramp.
- `--pack NAME` — texture-pack zip file name under `pack/` (missing file
  reverts to built-ins).
- `--terminal [WxH[:FPS]] [viewer flags]` — mirror each frame to a
  raw-RGB file and auto-launch the COBOL terminal
  viewer in the same terminal (default tap `160x120@15`; the game loop
  caps itself to FPS while the tap is active). Viewer sub-flags ride on
  the same flag and are forwarded: `--mode truecolor|ascii|mono|ansi`,
  `--fps N`, `--scale N`, `--once`
  (e.g. `--terminal 320x240:15 --mode ansi --fps 10 --scale 2`).
  Needs a viewer binary: `./viewer*` next to the game (dev aliases),
  the `STRATA_VIEWER` env path, or one embedded in the fat jar (all
  flavors carry their native viewer: mac-arm64, mac-x64, linux-x64,
  windows-x64); without one the game runs viewerless. `STRATA_VIEWER_LIB`
  overrides the viewer library path when `libcob` is not installed.
  The frame file, the input-bridge snapshot and the relocated log all
  live in one private per-launch temp dir (0700, removed on exit) — not
  the old fixed `/tmp` names, which any local user could read (screen)
  or write (keystroke injection) and which two game instances clobbered.
  The launcher hands the real paths to the viewer (`--file`) and the
  input bridge (its file args), so no path juggling is needed on any
  platform. While the tap is active the game log moves to `game.log`
  beside the frame file so log lines can't tear the fullscreen
  render; errors additionally stay on stderr.
  The tap also auto-launches the Pascal input bridge (`terminput`,
  same resolution rules as the viewer): terminal keys/mouse merge with
  window input, so the game stays playable with the window in the
  background.   Hotbar keys, look, buttons, wheel, quit, and inventory clicks all
  work from the terminal (clicks map cells back onto window pixels via
  the tap size + forwarded `--scale`, so auto-launched viewers always
  agree; a manual `--scale` the game doesn't know about misplaces them).

## Controls

Move/look: WASD + mouse · Jump: Space · Fly: `'` spectator, Space up /
Shift down · Fullbright x-ray: `\` · Render distance: `[`/`]` · Dig: hold LMB · Place: RMB ·
Hotbar: 1–9/wheel ·
Inventory: E · Save: Return · Respawn: R · Debug kit: +/= key · Self-hurt: `/` (1 half-heart)
· Time cycle: `;` · Tick rate: `,`/`.`
(0.125x–8x debug) · F3 overlay (fps/pos/time/mesh/hp) · Mouse release:
ESC · Quit: Delete (Fn+⌫ on laptops).

You start with 8 torches (no recipe yet). All keys rebindable in
`options.txt` (written on exit); view distance + sensitivity + gui scale
live there too.

Strata is an original project, not affiliated with or endorsed by Mojang.
