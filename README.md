# Strata

*Official pronunciation: /ˈstrɑːtɑː/*

A blocky voxel sandbox in Java (LWJGL2): dig, build, and survive from dawn
through torch-lit midnight.

```sh
./build.sh        # compile everything into out/
./build.sh run    # compile and launch the game
./build.sh test   # compile and run the headless suite
```

## Play (no build needed)

Download `Strata-0.0.0.0.jar` from the Releases page and double-click it
(or `java -jar Strata-0.0.0.0.jar`). Needs a Java 17+ runtime and macOS on
Apple Silicon (the bundled natives are arm64; Intel/Windows need theirs
swapped into `lib/native` and a rebuild).

Needs a JDK 17+ (`JAVA_HOME`, on `PATH`, or a standard Temurin-style
install — the script finds it).

## Boot flags

```sh
./build.sh run --seed N --world DIR --import IMG --pack NAME
```

- `--seed N` — new world / reseed (default: stored seed).
- `--world DIR` — world slot dir owning `region/` + `world.dat` +
  `player.dat` (default `saves/testing`).
- `--import IMG` — paste a two-tone picture as blocks into a FRESH void
  world, then play it (needs an unused `--world` dir).
- `--pack NAME` — texture-pack zip file name under `pack/` (missing file
  reverts to built-ins).

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
