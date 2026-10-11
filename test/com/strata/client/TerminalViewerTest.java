package com.strata.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TerminalViewerTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static List<String> slurp(String... args) {
      List<String> out = new ArrayList<>();
      int next = TerminalViewer.slurp(args, 0, out);
      check(next >= 0 && next <= args.length, "next in range for " + Arrays.toString(args));
      return out;
   }

   static int slurpNext(String... args) {
      return TerminalViewer.slurp(args, 0, new ArrayList<>());
   }

   public static void main(String[] args) {
      check(slurp().isEmpty(), "empty slurp");
      check(slurp("--mode", "ansi", "--fps", "10").equals(
         Arrays.asList("--mode", "ansi", "--fps", "10")), "flags+values");
      check(slurp("--once").equals(Arrays.asList("--once")), "bare once");
      check(slurpNext("--mode", "ascii", "--seed", "5") == 2, "stops at game flag");
      check(slurp("--mode", "ascii", "--seed", "5").equals(
         Arrays.asList("--mode", "ascii")), "game flag not forwarded");
      check(slurp("--file", "/tmp/x.bin", "--mode", "mono").equals(
         Arrays.asList("--mode", "mono")), "--file stripped, rest kept");
      check(slurp("--bogus").isEmpty(), "unknown stops slurp");
      check(slurpNext("--bogus") == 0, "unknown consumes nothing");
      check(slurp("--mode").equals(Arrays.asList("--mode")), "dangling flag forwards bare");
      check(slurp("--scale").equals(Arrays.asList("--scale")), "dangling scale forwards bare");
      check(slurp("--fps").equals(Arrays.asList("--fps")), "dangling fps forwards bare");

      try {
         TerminalViewer.stop(null);
      } catch (Throwable t) {
         check(false, "stop(null) threw");
      }

      if (failures > 0) { System.out.println(failures + " FAILURES"); System.exit(1); }
      System.out.println("terminalviewer ok");
   }
}

