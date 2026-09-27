#!/usr/bin/env python3
"""Comment-stripped duplicate of the codebase (./build.sh strip).

Reads src/ + test/, writes the same tree under stripped/ with every
// line comment and /* */ block comment removed. Generated, never edited:
re-run any time; it cannot go stale because nothing else writes there.

Lines that held only comments are DROPPED entirely (no gaps), so stripped
line numbers do NOT match the original — stack traces point into stripped/
itself, consistently. Originally-blank lines are kept (intentional
spacing). Code layout, strings, chars and escapes pass through
byte-identical — the stripped tree must still compile (build.sh verifies
this after generating).
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_DIRS = ("src", "test")
OUT_DIR = "stripped"


def strip_java(src):
    lines = []  # kept output lines, newline joined at the end
    buf = []  # code chars of the current line
    line_has_code = False
    line_has_comment = False

    def flush():
        if line_has_code or not line_has_comment:
            lines.append("".join(buf))

    i, n = 0, len(src)
    CODE, LINE, BLOCK, STR, CHR = range(5)
    state = CODE
    while i < n:
        c = src[i]
        nxt = src[i + 1] if i + 1 < n else ""
        if c == "\n":
            if state == LINE:
                state = CODE
            flush()
            buf = []
            line_has_code = False
            line_has_comment = False
            i += 1
            continue
        if state == CODE:
            if c == "/" and nxt == "/":
                state = LINE
                line_has_comment = True
                i += 2
            elif c == "/" and nxt == "*":
                state = BLOCK
                line_has_comment = True
                i += 2
            elif c == '"':
                state = STR
                buf.append(c)
                line_has_code = True
                i += 1
            elif c == "'":
                state = CHR
                buf.append(c)
                line_has_code = True
                i += 1
            else:
                buf.append(c)
                if not c.isspace():
                    line_has_code = True
                i += 1
        elif state == LINE:
            line_has_comment = True
            i += 1
        elif state == BLOCK:
            line_has_comment = True
            if c == "*" and nxt == "/":
                state = CODE
                i += 2
            else:
                i += 1
        else:  # STR or CHR: escapes and the closing quote pass through
            buf.append(c)
            line_has_code = True
            if c == "\\" and i + 1 < n:
                buf.append(src[i + 1])
                i += 2
            elif (state == STR and c == '"') or (state == CHR and c == "'"):
                state = CODE
                i += 1
            else:
                i += 1
    # Trailing content without a final newline.
    if buf or line_has_code or not line_has_comment:
        if line_has_code or not line_has_comment:
            lines.append("".join(buf))
    if not lines:
        return ""
    return "\n".join(lines) + "\n"


def main():
    files, lines_in, lines_out = 0, 0, 0
    for d in SRC_DIRS:
        for root, _, names in os.walk(os.path.join(ROOT, d)):
            for name in sorted(names):
                if not name.endswith(".java"):
                    continue
                src_path = os.path.join(root, name)
                with open(src_path, encoding="utf-8") as f:
                    src = f.read()
                stripped = strip_java(src)
                rel = os.path.relpath(src_path, ROOT)
                dst = os.path.join(ROOT, OUT_DIR, rel)
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                with open(dst, "w", encoding="utf-8", newline="") as f:
                    f.write(stripped)
                files += 1
                lines_in += src.count("\n") + 1
                lines_out += stripped.count("\n")
    print("stripped %d files -> %s/ (%d lines, %.0f%% of %d)" % (
        files, OUT_DIR, lines_out, 100.0 * lines_out / max(1, lines_in), lines_in))
    return 0


if __name__ == "__main__":
    sys.exit(main())
