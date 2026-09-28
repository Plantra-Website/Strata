package com.strata.world;

public class CarveTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) throws Exception {
      Level l = new Level(64);

      int[] before = new int[64];
      for (int y = 0; y < 64; y++) {
         before[y] = l.getTile(40, y, 40);
      }
      l.warmCarves(-4, -4, 4, 4);
      for (int y = 0; y < 64; y++) {
         check(l.getTile(40, y, 40) == before[y], "warm is read-only (y=" + y + ")");
      }

      final int[][] queries = new int[200][3];
      java.util.Random rng = new java.util.Random(7L);
      for (int i = 0; i < queries.length; i++) {
         queries[i][0] = rng.nextInt(512) - 256;
         queries[i][1] = 1 + rng.nextInt(62);
         queries[i][2] = rng.nextInt(512) - 256;
      }
      final int[][] got = new int[8][queries.length];
      Thread[] workers = new Thread[got.length];
      for (int w = 0; w < workers.length; w++) {
         final int ww = w;
         workers[w] = new Thread(() -> {
            if (ww % 2 == 0) {
               l.warmCarves(-8, -8, 8, 8);
            }
            for (int i = 0; i < queries.length; i++) {
               got[ww][i] = l.getTile(queries[i][0], queries[i][1], queries[i][2]);
            }
         });
         workers[w].start();
      }
      for (Thread t : workers) {
         t.join();
      }
      for (int i = 0; i < queries.length; i++) {
         for (int w = 1; w < workers.length; w++) {
            check(got[w][i] == got[0][i], "unanimous carve @" + queries[i][0] + "," + queries[i][1] + "," + queries[i][2]);
         }
      }

      if (failures == 0) System.out.println("CARVE PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

