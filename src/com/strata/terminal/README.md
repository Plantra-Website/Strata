# strata-viewer — COBOL terminal viewer for Strata

Renders the game's `--terminal` tap (`/tmp/strata-frame.bin`: 8-char frame
counter + `W*H*3` raw RGB bytes, top-down, atomic rename, ~15fps default)
as terminal cells. Written in GnuCOBOL (`viewer.cbl`, free format).

## Build

Requires GnuCOBOL 3.x (`cobc`) and a C compiler:

```sh
cd src/com/strata/terminal
cobc -x -F -O2 -o viewer viewer.cbl
./viewer --help
```

No libraries beyond libcob itself. To run the game with the tap, see the
main README (`--terminal [WxH[:FPS]]`) and point the viewer at the same
size:

```sh
./build.sh run --terminal 160x120:15
./viewer 160 120 --mode truecolor --fps 15
```

## Syntax

```sh
viewer [W H] [--mode truecolor|ascii|mono|ansi] [--fps 1..60]
       [--scale 1..8] [--once] [--file PATH] [--help]
```

- `W H` — tap size, must match the game's `--terminal WxH` (both clamp to
  16..640 x 16..480). Default `160 120`. Mismatch exits with
  `SIZE-MISMATCH` showing file vs expected bytes.
- `--mode` (also `--ascii`, `--mono`/`--grey`, `--ansi`, `--truecolor`,
  bare `ascii`/`mono`/`ansi`/`truecolor`, `--mode=X`):
  - `truecolor` (default) — `▀` half-blocks, 24-bit fg+bg, 2 px/cell.
  - `ascii` — 1 char/cell from a 70-level ramp with sqrt gamma lift,
    fg = block average color, 2x1 pixels per char (aspect-correct).
  - `mono` — same as `ascii`, glyphs only, no color (fastest text).
  - `ansi` — half-blocks in the 256-color palette (6x6x6 cube + grey
    ramp for flat content). Short `38;5;Nm` escapes, run-collapsed.
    Close to truecolor quality at ~half the bytes.
- `--fps N` — poll/render cap 1..60 (default 30; game emits ~15).
- `--scale N` — stride 1..8 (default 1): display every Nth pixel, so
  `320x240` tap at `--scale 2` shows `160x120` cells. See ratios below.
- `--once` — render one frame, quit. Default loops until the frame file
  is deleted (game exit), printing `FRAME-GONE`.
- `--file PATH` — frame file (default `/tmp/strata-frame.bin`).
  Useful for frozen-frame testing.

Game + viewer pairs:

```sh
./build.sh run --terminal 160x120:15
viewer 160 120 --mode truecolor --fps 15
viewer 320 240 --mode ascii --fps 10 --scale 2   # 160x120 cells
```

## Sizing: tap vs terminal (avoiding tear)

Tearing = wrap (a row as wide as the terminal wraps the cursor) or scroll
(more rows than the terminal has); both fight the home-cursor redraw.
Rules:

- display cols <= terminal width - 2
- display rows <= terminal height - 3..4 (banner + cursor lines need room)
- display cols = `W / scale`, display rows = `H / (2*scale)`
  (block modes pack 2 vertical px per cell; ascii/mono too)

The banner prints both sizes, e.g. `320x240 ... scale=2 (160x120 cells)`.
Pick tap and scale so the *cells* fit with margin:

| terminal | tap | scale | cells | notes |
|---|---|---|---|---|
| 80x24 | 76x40 | 1 | 76x20 | fits classic terminals, chunky |
| 120x40 | 116x72 | 1 | 116x36 | roomy default |
| 120x40 | 232x144 | 2 | 116x36 | same cells, 2x supersampled |
| 160x60 | 156x104 | 1 | 156x52 | large terminal |
| 200x60 | 392x224 | 2 | 196x56 | near the practical max |
| any | 320x240 | 2 | 160x120 | good detail/speed balance |
| any | 640x480 | 4 | 160x120 | max tap, same cells, slowest tap |

Cells preserve source aspect automatically (1 cell = 2x1 px). Bigger taps
only add detail if the game window is at least that big (nearest-sampled),
and cost cells linearly: 640x480 is 16x the cells of 160x120 (~2fps).

## Modes compared (160x120 gradient frame)

| mode | bytes/frame | notes |
|---|---|---|
| truecolor | ~370KB | best quality |
| ascii | ~181KB | color + readable, gamma-lifted darks |
| ansi | ~208KB gradient, far less when flat | 256-color, short escapes |
| mono | ~10KB | glyphs only |

Flat scenes (sky/walls) collapse via run encoding: truecolor ~31KB,
ascii ~11KB. Mono loop CPU is ~10ms/frame; color modes ~20ms (libcob
per-statement floor). In `--once` timings, ~15ms is one-shot startup.

## Frame format (for other viewers)

`/tmp/strata-frame.bin` = `%08d` ASCII counter + `W*H*3` bytes RGB,
top-down rows, rewritten atomically (~15fps default, configurable via
game `--terminal WxH:FPS`, 1..60). Deleted on game exit. Counter lets a
viewer skip unchanged frames.

## Troubleshooting

- `SIZE-MISMATCH`: viewer `W H` must equal the game's `--terminal WxH`.
  Note the separator is `x` (`640x480:10`, not `640:480:10`); a
  non-matching spec silently falls back to the game default 160x120.
- Grey output: check escapes are strict `38;2;R;G;Bm` (no `;` before
  `m` — an empty SGR param resets the color). If escapes are vivid but
  the scene looks grey, the frame bytes are dark (night cycle: `;` key,
  F3 shows time; `\` toggles fullbright).
- Tearing: shrink cells below terminal size (`--scale`, smaller tap, or
  smaller font). Leave 2 cols / 3-4 rows of margin.
- Game segfault in `glReadPixels` while resizing: fixed — the tap skips
  frames whose dimensions changed (drawable lags the window on macOS).
- Dark scenes: night/midnight frames average <80/255. Use day time,
  fullbright, or a brighter mode.
