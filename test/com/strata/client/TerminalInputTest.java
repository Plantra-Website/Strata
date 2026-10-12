package com.strata.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TerminalInputTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static void write(String name, String... lines) throws Exception {
      java.nio.file.Files.write(java.nio.file.Paths.get(name),
         Arrays.asList(lines), java.nio.charset.StandardCharsets.UTF_8,
         java.nio.file.StandardOpenOption.CREATE,
         java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
   }

   public static void main(String[] args) throws Exception {
      TerminalInput.setPathForTest("tinput.bin");
      TerminalInput in = new TerminalInput();
      try {
         check(!in.poll(), "missing file is stale");
         check(!in.isFresh(), "stale flag");

         write("tinput.bin", "SEQ 1", "KEYS W SPACE", "MOUSE 10 20 1", "WHEEL 0");
         check(in.poll(), "first snapshot applies");
         check(in.isDown(org.lwjgl.input.Keyboard.KEY_W), "W held");
         check(in.isDown(org.lwjgl.input.Keyboard.KEY_SPACE), "space held");
         check(!in.isDown(org.lwjgl.input.Keyboard.KEY_S), "S not held");
         check(in.isButtonDown(0), "left held");
         check(!in.isButtonDown(1), "right up");
         check(in.drainDX() == 0 && in.drainDY() == 0, "first sample has no delta");
         check(in.drainWheel() == 0, "no wheel yet");

         check(in.poll(), "same seq stays fresh");
         check(in.isDown(org.lwjgl.input.Keyboard.KEY_W), "keys persist across polls");

         write("tinput.bin", "SEQ 2", "KEYS A", "MOUSE 13 18 1", "WHEEL 2");
         check(in.poll(), "next seq applies");
         check(!in.isDown(org.lwjgl.input.Keyboard.KEY_W), "W released");
         check(in.isDown(org.lwjgl.input.Keyboard.KEY_A), "A held");
         check(in.drainDX() == 3 && in.drainDY() == -2, "mouse delta drained once");
         check(in.drainDX() == 0, "delta does not repeat");
         check(in.drainWheel() == 2, "wheel diff applied");
         check(in.drainWheel() == 0, "wheel does not repeat");

         write("tinput.bin", "SEQ 3", "KEYS", "MOUSE 13 18 0", "WHEEL 2");
         check(in.poll(), "release snapshot applies");
         check(!in.isDown(org.lwjgl.input.Keyboard.KEY_A), "all up");
         check(!in.isButtonDown(0), "button cleared");

         write("tinput.bin", "SEQ 4", "KEYS BOGUSKEY", "MOUSE x y", "WHEEL z");
         check(!in.poll(), "malformed snapshot is stale, keeps last state");

         write("tinput.bin", "SEQ 5", "KEYS NOTAKEY", "MOUSE 0 0 0", "WHEEL 2");
         check(in.poll(), "unknown names do not fail the poll");
         check(!in.isDown(9999), "unknown code never down");

         write("tinput.bin", "SEQ 6", "MOUSE 0 0 0", "WHEEL 0");
         check(in.poll(), "accumulator reset resyncs without scrolling back");
         check(in.drainWheel() == 0, "no phantom scroll on bridge restart");

         java.io.File f = new java.io.File("tinput.bin");
         f.setLastModified(System.currentTimeMillis() - 60000L);
         check(!in.poll(), "old file goes stale");
      } finally {
         TerminalInput.setPathForTest(null);
         new java.io.File("tinput.bin").delete();
      }

      if (failures > 0) { System.out.println(failures + " FAILURES"); System.exit(1); }
      System.out.println("terminalinput ok");
   }
}

