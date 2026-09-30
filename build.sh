#!/bin/sh
# Project build script (this file lives at the project root).
#   ./build.sh        compile everything into out/
#   ./build.sh run    compile and launch the game
#   ./build.sh test   compile and run the headless suite (each test isolated)
#   ./build.sh strip  regenerate stripped/ (comment-free duplicate of src+test,
#                     comment-only lines dropped) and verify it still compiles
#   ./build.sh jar    fat release jar into dist/ (classes + res + libs +
#                     mac-arm64 natives, self-extracting — double-clickable)
set -e
cd "$(dirname "$0")"
ROOT="$(pwd)"
# JDK discovery (no machine-specific paths — must work on a fresh clone):
# JAVA_HOME if 17+, else javac on PATH if 17+, else a Temurin-style install
# under ~/Library, /Library or /usr/lib/jvm. 17.x preferred (the verified
# runtime for this LWJGL2 tree); 18+ accepted as fallback.
# Honor JAVA_HOME only if it is actually 17+ (Path.of and friends need it);
# otherwise fall back to discovery instead of failing obscurely.
pick_jdk() {
  if [ -n "$JAVA_HOME" ] && "$JAVA_HOME/bin/javac" -version 2>&1 | grep -qE 'javac (1[7-9]|[2-9][0-9])'; then
    echo "$JAVA_HOME"
    return
  fi
  if javac -version 2>&1 | grep -qE 'javac (1[7-9]|[2-9][0-9])'; then
    echo ""
    return
  fi
  for want in 'javac 17' 'javac (1[89]|[2-9][0-9])'; do
    for base in "$HOME/Library/Java/JavaVirtualMachines" /Library/Java/JavaVirtualMachines /usr/lib/jvm; do
      for d in "$base"/*/ "$base"; do
        for j in "$d/Contents/Home" "$d"; do
          if [ -x "$j/bin/javac" ] && "$j/bin/javac" -version 2>&1 | grep -qE "$want"; then
            echo "$j"
            return
          fi
        done
      done
    done
  done
  echo "NOJDK"
}
JDK_HOME="$(pick_jdk)"
if [ "$JDK_HOME" = "NOJDK" ]; then
  echo "need a JDK 17+: set JAVA_HOME or put javac 17+ on PATH" >&2
  exit 1
fi
if [ -z "$JDK_HOME" ]; then
  JAVAC_BIN="javac"
  JAVA_BIN="java"
else
  JAVAC_BIN="$JDK_HOME/bin/javac"
  JAVA_BIN="$JDK_HOME/bin/java"
fi
LIBDIR="$ROOT/lib"
LIB="$LIBDIR/lwjgl.jar:$LIBDIR/lwjgl_util.jar:$LIBDIR/jinput.jar"
# Heap floor: streaming regularly parks 300-600MB (meshes, regions,
# columns); starting there skips the early GC churn on every boot and
# every one of the 60 test JVMs. No ceiling change (default is fine).
JAVA_FLAGS="-Xms256m"
# Temp roots honor the platform (macOS sets TMPDIR, Linux usually /tmp).
TMPDIR="${TMPDIR:-/tmp}"

compile() {
  # Clean first: renamed/moved classes would otherwise leave stale .class
  # files behind (DepTest scans out/ and would false-positive on ghosts).
  rm -rf out
  mkdir -p out
  # Alt-discovery index (see AtlasStitcher.hasTexture): probing
  # getResourceAsStream x99 per tile base costs seconds on the first
  # stitch (far worse from a jar). Regenerated every compile so it can
  # never go stale; rides into out/ with the cp below (jars included).
  # Absent index (hand-rolled tree) falls back to probing — slow, correct.
  if [ -d res/textures ]; then
    (cd res/textures && find . -name '*.png' | sed 's|^\./||' | sort > index.txt)
  fi
  cp -r res/. out/
  # test/ is optional (published trees ship code only): compile it when present.
  SOURCES="$(find src -name '*.java')"
  if [ -d test ]; then
    # shellcheck disable=SC2046
    SOURCES="$SOURCES $(find test -name '*.java')"
  fi
  # shellcheck disable=SC2086
  "$JAVAC_BIN" -encoding UTF-8 -cp "$LIB" -d out $SOURCES
}

run_tests() {
  # Each test runs in a fresh temp dir: Level/region files never touch
  # the project, and tests can't see each other's saves.
  rm -rf "$TMPDIR/rbtest"
  found=0
  for t in $(cd out && find . -name '*Test.class' | sed 's|^\./||; s|\.class$||; s|/|.|g'); do
    found=1
    d="$TMPDIR/rbtest/$(echo "$t" | tr . _)"
    mkdir -p "$d"
    echo "=== $t ==="
    # Headless: without this every fresh JVM activates as a GUI app and
    # steals focus (60 Dock bounces per suite run). Tests only need
    # BufferedImage/ImageIO/fonts, all headless-safe — no test opens a
    # window or GL context.
    (cd "$d" && "$JAVA_BIN" $JAVA_FLAGS -Djava.awt.headless=true -cp "$ROOT/out:$ROOT/res:$LIB" "$t") || exit 1
  done
  rm -rf "$TMPDIR/rbtest"
  if [ "$found" = "0" ]; then
    echo "(no tests in this tree)"
  fi
}

case "${1:-build}" in
  build) compile; echo BUILD_OK ;;
  run) shift; compile; echo BUILD_OK; "$JAVA_BIN" $JAVA_FLAGS -cp "out:res:$LIB" com.strata.Boot "$@" ;;
  test) compile; echo BUILD_OK; run_tests ;;
  strip)
    if [ ! -f tools/strip.py ]; then
      echo "strip unavailable in this tree (dev-only tool)"; exit 1
    fi
    rm -rf stripped
    python3 tools/strip.py || exit 1
    # Prove the mirror is real code, not approximate text: it must compile.
    rm -rf "$TMPDIR/stripcheck"
    mkdir -p "$TMPDIR/stripcheck"
    # shellcheck disable=SC2046
    "$JAVAC_BIN" -encoding UTF-8 -cp "$LIB" -d "$TMPDIR/stripcheck" $(find stripped/src stripped/test -name '*.java') || exit 1
    echo STRIP_OK ;;
  jar)
    # Fat release jar per platform flavor: game classes + res + unpacked
    # libs + that platform's natives/ (self-extracting at boot — see
    # Boot.preloadNatives). Usage: ./build.sh jar [mac-arm64|mac-x64|
    # windows-x64|linux-x64] (default: mac-arm64, this machine).
    PLAT="${2:-mac-arm64}"
    case "$PLAT" in
      mac-arm64|mac-x64|windows-x64|linux-x64) ;;
      *) echo "unknown platform '$PLAT' (mac-arm64|mac-x64|windows-x64|linux-x64)"; exit 1 ;;
    esac
    if [ ! -d "natives/$PLAT" ]; then
      echo "no vendored natives for $PLAT"; exit 1
    fi
    compile
    VER="$(sed -n 's/.*VERSION = "\([^"]*\)".*/\1/p' src/com/strata/core/Config.java)"
    JAR_BIN="${JDK_HOME:+$JDK_HOME/bin/}jar"
    rm -rf dist/work "dist/Strata-$VER-$PLAT.jar"
    mkdir -p dist/work
    cp -r out/. dist/work/
    for j in "$LIBDIR"/*.jar; do
      unzip -o -q "$j" -x 'META-INF/*' -d dist/work
    done
    mkdir -p dist/work/native
    cp "natives/$PLAT/"* dist/work/native/
    find dist/work -name '.DS_Store' -delete
    rm -rf dist/work/__MACOSX
    mkdir -p dist/work/META-INF
    printf 'Manifest-Version: 1.0\nMain-Class: com.strata.Boot\n' > dist/work/META-INF/MANIFEST.MF
    (cd dist/work && "$JAR_BIN" cfm "../Strata-$VER-$PLAT.jar" META-INF/MANIFEST.MF .)
    rm -rf dist/work
    ls -la "dist/Strata-$VER-$PLAT.jar" ;;
  *) echo "usage: ./build.sh [build|run|test|strip|jar]"; exit 1 ;;
esac
