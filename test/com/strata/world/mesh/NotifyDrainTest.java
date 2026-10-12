package com.strata.world.mesh;

public class NotifyDrainTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) throws Exception {
      com.strata.world.Level level = new com.strata.world.Level(64, false, null);
      LevelRenderer renderer = new LevelRenderer(level);

      int threads = 8;
      int callsPerThread = 1000;
      java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
      java.util.concurrent.atomic.AtomicInteger errors = new java.util.concurrent.atomic.AtomicInteger();

      Thread[] ts = new Thread[threads];
      for (int t = 0; t < threads; t++) {
         final int tid = t;
         ts[t] = new Thread(() -> {
            try { start.await(); } catch (InterruptedException e) { return; }
            try {
               for (int i = 0; i < callsPerThread; i++) {
                  int x = (tid * 1000 + i) % 64;
                  int z = i % 64;
                  renderer.lightColumnChanged(x, z, 0, 10);
                  renderer.tileChanged(x, 0, z);
               }
            } catch (Throwable e) {
               errors.incrementAndGet();
               System.out.println("exception thread=" + tid + ": " + e);
            }
         }, "notify-drain-" + t);
         ts[t].start();
      }
      start.countDown();
      for (Thread t : ts) {
         t.join();
      }

      check(errors.get() == 0, "no exceptions from concurrent notify (" + errors.get() + ")");

      renderer.setDirty(-1000, 0, -1000, 1000, 64, 1000);

      if (failures == 0) System.out.println("NOTIFYDRAIN PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

