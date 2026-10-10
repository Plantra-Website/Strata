package com.strata.core;

public class CoreTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) {
      double[] vals = {-257.9, -16.0, -1.5, -1.0, -0.5, -0.0, 0.0, 0.5, 1.0, 15.9, 16.0, 100000.25};
      for (double v : vals) {
         check(MathHelper.floor(v) == (int)Math.floor(v), "floor(" + v + ")");
         check(MathHelper.floor((float)v) == (int)Math.floor(v), "floorf(" + v + ")");
      }
      check(MathHelper.floor(-0.5) == -1, "negative floor");
      check(MathHelper.clamp(5, 0, 3) == 3, "clamp high");
      check(MathHelper.clamp(-1, 0, 3) == 0, "clamp low");
      check(MathHelper.clamp(2, 0, 3) == 2, "clamp mid");
      check(MathHelper.clamp(5.5f, 0f, 1f) == 1f, "clampf");
      check(MathHelper.lerp(0f, 10f, 0.25f) == 2.5f, "lerp");
      check(MathHelper.lerp(0.0, 10.0, 0.0) == 0.0, "lerp t=0");
      System.out.println("math ok");

      check(Config.VIEW_RADIUS == 6, "view radius 6");
      check(Config.PREBUILD_RADIUS == 2, "prebuild 2");
      check(Config.SUBMIT_BUDGET == 8 && Config.UPLOAD_BUDGET == 8, "budgets 8");
      check(Config.MESH_WORKERS == 5, "workers 5");
      check(Config.DAY_LENGTH == 72000L, "day length");
      check(com.strata.client.GameClient.DAY_LENGTH == Config.DAY_LENGTH, "day alias");
      System.out.println("config ok");

      int ringBefore = Debug.ringUsed();
      Debug.slow("test", 5L, 1000000L, "fast op (no log expected)");
      check(Debug.ringUsed() == ringBefore + 1, "slow event recorded");
      Debug.slow("test", 5000L, 10L, "slow op (warn expected above)");
      check(Debug.ringUsed() == ringBefore + 2, "slow warn recorded");
      Debug.worker("test-0", "chunk 1,2");
      check("chunk 1,2".equals(Debug.workerStates().get("test-0")), "worker state tracked");
      Debug.statusLine();
      Debug.dumpRecent();
      check(Config.SLOW_MESH_MS > 0 && Config.SLOW_FRAME_MS > 0 && Config.DBG_STATUS_EVERY > 0, "debug thresholds sane");

      System.out.println("debug ok");

      Log.setQuiet(false);
      Log.info("test", "hello");
      Log.warn("test", "caution");
      Log.error("test", "boom");
      Log.error("test", "boom with cause", new RuntimeException("cause"));
      Log.raw("verbatim");
      Log.setQuiet(true);
      Log.info("test", "silent");
      Log.raw("silent");
      Log.setQuiet(false);
      Log.setLevel(Log.ERROR);
      Log.info("test", "suppressed by level");
      Log.setLevel(Log.INFO);
      System.out.println("log ok");

      checkRngProfilerGl();

      if (failures > 0) { System.out.println(failures + " FAILURES"); System.exit(1); }
      System.out.println("CORE PASS");
   }

   static void checkRngProfilerGl() {
      Rng.reseed(1337L);
      float a1 = Rng.range(-16.0F, 16.0F);
      float b1 = Rng.range(-16.0F, 16.0F);
      Rng.reseed(1337L);
      check(Rng.range(-16.0F, 16.0F) == a1, "rng replays a");
      check(Rng.range(-16.0F, 16.0F) == b1, "rng replays b");
      Rng.reseed(999L);
      check(Rng.range(-16.0F, 16.0F) != a1, "rng reseed changes stream");
      check(Rng.range(0, 5) >= 0 && Rng.range(0, 5) <= 5, "rng int range");
      System.out.println("rng ok");

      Profiler.setEnabled(false);
      Profiler.push("x");
      Profiler.pop();
      Profiler.pop();
      Profiler.endFrame();
      Profiler.setEnabled(true);
      Profiler.push("outer");
      Profiler.push("inner");
      Profiler.pop();
      Profiler.pop();
      for (int i = 0; i < 601; i++) {
         Profiler.endFrame();
      }
      Profiler.setEnabled(false);
      System.out.println("profiler ok");

      GlResources.reset();
      check(GlResources.live() == 0, "census empty");
      GlResources.track(11, "chunk");
      GlResources.track(12, "chunk");
      GlResources.track(0, "ignored");
      check(GlResources.live() == 2, "census tracks");
      check(GlResources.peak() == 2, "census peak");
      GlResources.release(11);
      check(GlResources.live() == 1, "census releases");
      GlResources.release(12);
      check(GlResources.live() == 0, "census empty again");
      check(GlResources.peak() == 2, "peak sticks");
      GlResources.dump();
      GlResources.reset();
      System.out.println("gl ok");
   }
}

