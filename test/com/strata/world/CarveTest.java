package com.strata.world;

import com.strata.blocks.Blocks;

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

      com.strata.world.gen.CaveCarver caves =
         new com.strata.world.gen.CaveCarver(com.strata.world.gen.TerrainGenerator.DEFAULT_SEED, 64);
      com.strata.world.gen.RavineCarver ravines =
         new com.strata.world.gen.RavineCarver(com.strata.world.gen.TerrainGenerator.DEFAULT_SEED, 64);
      int veins = 0, floated = 0;
      for (int x = -48; x < 48; x++) {
         for (int z = -48; z < 48; z++) {
            for (int y = 1; y <= 40 && y < 64; y++) {
               int id = l.getTile(x, y, z);
               if (id == Blocks.GRAVEL_ID || Blocks.isOre(id)) {
                  veins++;
                  if (caves.isCarved(x, y, z) || ravines.isCarved(x, y, z)) {
                     if (floated < 5) {
                        System.out.println("FLOATER @" + x + "," + y + "," + z + " id=" + id);
                     }
                     floated++;
                  }
               }
            }
         }
      }
      System.out.println("veins=" + veins + " floaters=" + floated);
      check(veins > 100, "vein sample exists (" + veins + ")");
      check(floated == 0, "no floating veins (" + floated + ")");

      if (failures == 0) System.out.println("CARVE PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

