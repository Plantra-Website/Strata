#!/bin/sh
# Project build script (this file lives at the project root).
#   ./build.sh        compile everything into out/
#   ./build.sh run    compile and launch the game
#   ./build.sh test   compile and run the headless suite (each test isolated)
#   ./build.sh strip  regenerate stripped/ (comment-free duplicate of src+test,
#                     comment-only lines dropped) and verify it still compiles
set -e
cd "$(dirname "$0")"
ROOT="$(pwd)"
PINNED_JDK="/Users/john.patrick/Library/Java/JavaVirtualMachines/temurin-17.0.19/Contents/Home"
# Honor JAVA_HOME only if it is actually 17+ (Path.of and friends need it);
# otherwise fall back to the pinned JDK instead of failing obscurely.
pick_jdk() {
  if [ -n "$JAVA_HOME" ] && "$JAVA_HOME/bin/javac" -version 2>&1 | grep -qE 'javac (1[7-9]|[2-9][0-9])'; then
    echo "$JAVA_HOME"
  else
    echo "$PINNED_JDK"
  fi
}
JDK_HOME="$(pick_jdk)"
JAVAC_BIN="$JDK_HOME/bin/javac"
JAVA_BIN="$JDK_HOME/bin/java"
LIBDIR="$ROOT/lib"
LIB="$LIBDIR/lwjgl.jar:$LIBDIR/lwjgl_util.jar:$LIBDIR/jinput.jar"

compile() {
  # Clean first: renamed/moved classes would otherwise leave stale .class
  # files behind (DepTest scans out/ and would false-positive on ghosts).
  rm -rf out
  mkdir -p out
  cp -r res/. out/
  # shellcheck disable=SC2046
  "$JAVAC_BIN" -encoding UTF-8 -cp "$LIB" -d out $(find src test -name '*.java')
}

run_tests() {
  # Each test runs in a fresh temp dir: Level/region files never touch
  # the project, and tests can't see each other's saves.
  rm -rf /tmp/rbtest
  for t in $(cd out && find . -name '*Test.class' | sed 's|^\./||; s|\.class$||; s|/|.|g'); do
    d="/tmp/rbtest/$(echo "$t" | tr . _)"
    mkdir -p "$d"
    echo "=== $t ==="
    (cd "$d" && "$JAVA_BIN" -cp "$ROOT/out:$ROOT/res:$LIB" "$t") || exit 1
  done
  rm -rf /tmp/rbtest
}

case "${1:-build}" in
  build) compile; echo BUILD_OK ;;
  run) shift; compile; echo BUILD_OK; "$JAVA_BIN" -cp "out:res:$LIB" com.strata.Boot "$@" ;;
  test) compile; echo BUILD_OK; run_tests ;;
  strip)
    rm -rf stripped
    python3 tools/strip.py || exit 1
    # Prove the mirror is real code, not approximate text: it must compile.
    rm -rf /tmp/stripcheck
    mkdir -p /tmp/stripcheck
    # shellcheck disable=SC2046
    "$JAVAC_BIN" -encoding UTF-8 -cp "$LIB" -d /tmp/stripcheck $(find stripped/src stripped/test -name '*.java') || exit 1
    echo STRIP_OK ;;
  *) echo "usage: ./build.sh [build|run|test|strip]"; exit 1 ;;
esac
