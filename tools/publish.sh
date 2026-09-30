#!/bin/sh
set -e
cd "$(dirname "$0")/.."
ROOT="$(pwd)"
TMPDIR="${TMPDIR:-/tmp}"

PUSH=""
if [ "$1" = "push" ]; then
  PUSH="yes"
  shift
fi
MSG="$1"
if [ -z "$MSG" ]; then
  MSG="snapshot $(date '+%Y-%m-%d %H:%M')"
fi

./build.sh strip > /dev/null || exit 1

if [ -d publish/.git ]; then
  DIRTY="$(git -C publish status --porcelain | grep -vE 'DS_Store|__MACOSX' || true)"
  if [ -n "$DIRTY" ]; then
    echo "publish/ has uncommitted edits — rebuild would wipe them." >&2
    echo "Move lasting changes into src/, res/, or this script, then re-run." >&2
    exit 1
  fi
fi
HAD_GIT=""
if [ -d publish/.git ]; then
  HAD_GIT="yes"
  rm -rf "$TMPDIR/publish-git-keep"
  mv publish/.git "$TMPDIR/publish-git-keep"
fi
rm -rf publish
mkdir -p publish
if [ -n "$HAD_GIT" ]; then
  mv "$TMPDIR/publish-git-keep" publish/.git
fi
cp -r stripped/src stripped/test res lib build.sh options.txt publish/
cp pack/ATTRIBUTION-Pixel-Perfection-Fidelity.txt publish/ATTRIBUTION.md
rm -rf publish/tools publish/TODO.md
mkdir -p publish/tools
python3 - "$ROOT/tools/publish.sh" publish/tools/publish.sh <<'PYEOF'
import re, sys
src, dst = sys.argv[1], sys.argv[2]
out, in_doc, marker = [], False, ""
for i, line in enumerate(open(src).read().split("\n")):
    if in_doc:
        out.append(line)
        if line == marker:
            in_doc = False
        continue
    m = re.search(r"<<'(\w+)'", line)
    if m:
        in_doc, marker = True, m.group(1)
        out.append(line)
        continue
    stripped = line.strip()
    if stripped.startswith("#") and not (i == 0 and stripped.startswith("#!")):
        continue
    out.append(line)
open(dst, "w").write("\n".join(out))
PYEOF
python3 -c "
p = 'publish/tools/publish.sh'
s = open(p).read().replace('you@example.com', 'you@example.com')
open(p, 'w').write(s)
"
rm -rf stripped
cat > publish/README.md <<'EOF'
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
EOF
printf 'out/\n*.DS_Store\n__MACOSX/\n' > publish/.gitignore

cd publish
if [ ! -d .git ]; then
  git init -q
  git config user.name "john.patrick" 2>/dev/null || true
  git config user.email >/dev/null 2>&1 || git config user.email "you@example.com"
  git add -A
  git commit -qm "$MSG"
  echo "publish repo initialized + committed"
else
  git add -A
  if git diff --cached --quiet; then
    echo "publish/ unchanged, nothing to commit"
  else
    git commit -qm "$MSG"
    echo "publish/ recommitted"
  fi
fi
if [ -n "$PUSH" ]; then
  git push || exit 1
  echo "pushed"
else
  echo "next: create the empty repo on github.com, then:"
  echo "  cd publish && git remote add origin <url> && git push -u origin HEAD"
fi
