package com.strata.world.gen;

public class GenRaceTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static int[] expected(GenLayer layer, int x, int z, int w, int h) {
      return layer.generate(x, z, w, h);
   }

   public static void main(String[] args) throws Exception {
      long[] seeds = {7777L, 123456789L, -9L, 42424242L};
      int[][] calls = {
         {0, 0, 16, 16},
         {16, 16, 16, 16},
         {-16, -16, 16, 16},
         {100, -200, 8, 8},
         {0, 0, 32, 16},
      };

      java.util.Map<String, int[]> baseline = new java.util.LinkedHashMap<>();
      for (long seed : seeds) {
         GenLayer[] stack = GenLayer.buildStack(seed);
         for (int li = 0; li < 4; li++) {
            GenLayer layer = stack[li];
            for (int[] c : calls) {
               int[] exp = expected(layer, c[0], c[1], c[2], c[3]);
               baseline.put(seed + ":" + li + ":" + java.util.Arrays.toString(c), exp);
            }
         }
      }

      GenLayer[] stack = GenLayer.buildStack(7777L);
      GenLayer coarse = stack[0];
      GenLayer voronoi = stack[1];
      GenLayer temp = stack[2];
      GenLayer rain = stack[3];

      int threads = 8;
      int rounds = 200;
      java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
      java.util.concurrent.atomic.AtomicInteger threadFailures = new java.util.concurrent.atomic.AtomicInteger();

      Thread[] ts = new Thread[threads];
      for (int t = 0; t < threads; t++) {
         final int tid = t;
         ts[t] = new Thread(() -> {
            try {
               start.await();
            } catch (InterruptedException e) {
               return;
            }
            try {
               for (int r = 0; r < rounds; r++) {
                  int key = (tid * 37 + r * 59) % calls.length;
                  int[] c = calls[key];
                  GenLayer layer = new GenLayer[]{coarse, voronoi, temp, rain}[r % 4];
                  int[] got = layer.generate(c[0], c[1], c[2], c[3]);
                  int[] want = baseline.get(7777L + ":" + (r % 4) + ":" + java.util.Arrays.toString(c));
                  if (!java.util.Arrays.equals(got, want)) {
                     threadFailures.incrementAndGet();
                     System.out.println("mismatch thread=" + tid + " round=" + r + " layer=" + (r % 4)
                        + " call=" + java.util.Arrays.toString(c));
                  }
               }
            } catch (Throwable e) {
               threadFailures.incrementAndGet();
               System.out.println("exception thread=" + tid + ": " + e);
            }
         }, "gen-race-" + t);
         ts[t].start();
      }
      start.countDown();
      for (Thread t : ts) {
         t.join();
      }

      check(threadFailures.get() == 0, "no cross-thread mismatches (" + threadFailures.get() + ")");

      GenLayer[] again = GenLayer.buildStack(7777L);
      check(java.util.Arrays.equals(again[0].generate(0, 0, 16, 16),
            baseline.get(7777L + ":0:" + java.util.Arrays.toString(calls[0]))),
         "rebuilt stack matches baseline");

      if (failures == 0) System.out.println("GENRACE PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

