#!/bin/sh
# Publish the comment-stripped mirror as a standalone repo.
#   ./tools/publish.sh          regenerate + assemble + commit locally
#   ./tools/publish.sh push     ...and push (needs a `github` remote + auth)
#
# Layout of publish/: stripped code (src/, test/) + res/ + lib/ + tools/ +
# build.sh + TODO.md + README.md. Never edit publish/ by hand — it is wiped
# and rebuilt from the live tree every run, so it cannot go stale.
set -e
cd "$(dirname "$0")/.."
ROOT="$(pwd)"

./build.sh strip > /dev/null || exit 1

# Rebuild publish/ but keep its .git (history must persist across updates
# or pushes reject). Everything else is wiped: never edit publish/ by hand.
HAD_GIT=""
if [ -d publish/.git ]; then
  HAD_GIT="yes"
  rm -rf /tmp/publish-git-keep
  mv publish/.git /tmp/publish-git-keep
fi
rm -rf publish
mkdir -p publish
if [ -n "$HAD_GIT" ]; then
  mv /tmp/publish-git-keep publish/.git
fi
cp -r stripped/src stripped/test res lib tools build.sh TODO.md publish/
# strip.py stays private (dev-only generator): the public tree gets code,
# never the tooling that made it.
rm -f publish/tools/strip.py
# No duplicate tree: the stripped mirror is an intermediate — its contents
# now live directly under publish/, so remove it.
rm -rf stripped
cat > publish/README.md <<'EOF'
# Strata

A blocky voxel sandbox in Java (LWJGL2): dig, build, and survive from dawn
through torch-lit midnight.

```sh
./build.sh        # compile everything into out/
./build.sh run    # compile and launch the game
./build.sh test   # compile and run the headless suite
```

Controls and current status: see TODO.md.
EOF
printf 'out/\n' > publish/.gitignore

cd publish
if [ ! -d .git ]; then
  git init -q
  # Local identity only (never touches global config). Replace the email
  # with your own: git config user.email "you@example.com".
  git config user.name "john.patrick" 2>/dev/null || true
  git config user.email >/dev/null 2>&1 || git config user.email "john.patrick101.help@gmail.com"
  git add -A
  git commit -qm "snapshot $(date '+%Y-%m-%d %H:%M')"
  echo "publish repo initialized + committed"
else
  git add -A
  if git diff --cached --quiet; then
    echo "publish/ unchanged, nothing to commit"
  else
    git commit -qm "snapshot $(date '+%Y-%m-%d %H:%M')"
    echo "publish/ recommitted"
  fi
fi
if [ "$1" = "push" ]; then
  git push || exit 1
  echo "pushed"
else
  echo "next: create the empty repo on github.com, then:"
  echo "  cd publish && git remote add origin <url> && git push -u origin HEAD"
fi
