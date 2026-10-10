package com.strata.client;

public class FrameTapTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) {
      FrameTap d = FrameTap.fromSpec(null);
      check(d.tapWidth() == 160 && d.tapHeight() == 120, "default size 160x120");
      check(d.targetFps() == 15, "default fps 15");

      FrameTap s = FrameTap.fromSpec("96x72");
      check(s.tapWidth() == 96 && s.tapHeight() == 72, "size 96x72");
      check(s.targetFps() == 15, "size-only keeps default fps");

      FrameTap f = FrameTap.fromSpec("160x120:30");
      check(f.tapWidth() == 160 && f.tapHeight() == 120, "fps spec keeps size");
      check(f.targetFps() == 30, "fps 30 parsed");

      FrameTap hi = FrameTap.fromSpec("80x60:99");
      check(hi.targetFps() == 60, "fps clamped to 60");

      FrameTap lo = FrameTap.fromSpec("80x60:0");
      check(lo.targetFps() == 1, "fps clamped to 1");

      FrameTap bad = FrameTap.fromSpec("garbage");
      check(bad.tapWidth() == 160 && bad.tapHeight() == 120, "bad size falls back");
      check(bad.targetFps() == 15, "bad size keeps default fps");

      FrameTap badFps = FrameTap.fromSpec("64x48:abc");
      check(badFps.tapWidth() == 64 && badFps.tapHeight() == 48, "bad fps keeps size");
      check(badFps.targetFps() == 15, "bad fps falls back to 15");

      if (failures > 0) { System.out.println(failures + " FAILURES"); System.exit(1); }
      System.out.println("frametap ok");
   }
}

