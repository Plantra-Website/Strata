#!/bin/sh
set -e
cd "$(dirname "$0")/.."
ROOT="$(pwd)"
TMPDIR="${TMPDIR:-/tmp}"
PYTHON="/Applications/Xcode.app/Contents/Developer/Library/Frameworks/Python3.framework/Versions/3.9/bin/python3.9"
if [ ! -x "$PYTHON" ]; then
  echo "need $PYTHON (Xcode command line tools)" >&2
  exit 1
fi

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
mkdir -p publish/src/com/strata/terminal
cp src/com/strata/terminal/viewer.cbl src/com/strata/terminal/README.md \
  publish/src/com/strata/terminal/
cp pack/ATTRIBUTION-Pixel-Perfection-Fidelity.txt publish/ATTRIBUTION.md
rm -rf publish/tools publish/TODO.md
mkdir -p publish/tools
"$PYTHON" - "$ROOT/tools/publish.sh" publish/tools/publish.sh <<'PYEOF'
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
"$PYTHON" -c "
p = 'publish/tools/publish.sh'
s = open(p).read().replace('you@example.com', 'you@example.com')
open(p, 'w').write(s)
"
rm -rf stripped
cp README.md publish/README.md
printf 'out/\nout-test/\n*.DS_Store\n__MACOSX/\n' > publish/.gitignore

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
