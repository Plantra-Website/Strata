package com.strata.world.gen;

import com.strata.blocks.Blocks;

public class RavineTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) {
      TerrainGenerator g = new TerrainGenerator(TerrainGenerator.DEFAULT_SEED, 128);
      RavineCarver r = new RavineCarver(TerrainGenerator.DEFAULT_SEED, 128);
      RavineCarver r2 = new RavineCarver(TerrainGenerator.DEFAULT_SEED, 128);

      int with = 0;
      for (int ccx = -16; ccx < 16; ccx++) {
         for (int ccz = -16; ccz < 16; ccz++) {
            boolean found = false;
            for (int x = ccx * 16; x < ccx * 16 + 16 && !found; x += 2) {
               for (int z = ccz * 16; z < ccz * 16 + 16 && !found; z += 2) {
                  for (int y = 1; y < 128; y += 3) {
                     if (r.isCarved(x, y, z)) {
                        found = true;
                        break;
                     }
                  }
               }
            }
            if (found) {
               with++;
            }
         }
      }
      System.out.println("ravine chunks=" + with + "/1024");
      check(with > 50 && with < 350, "ravine footprint in band (got " + with + ")");

      java.util.Random rng = new java.util.Random(99L);
      for (int i = 0; i < 500; i++) {
         int x = rng.nextInt(512) - 256, y = 1 + rng.nextInt(126), z = rng.nextInt(512) - 256;
         check(r.isCarved(x, y, z) == r2.isCarved(x, y, z), "ravine deterministic @" + x + "," + y + "," + z);
      }
      System.out.println("determinism ok");

      int best = 0;
      for (int x = -256; x < 256; x += 2) {
         for (int z = -256; z < 256; z += 2) {
            int run = 0;
            for (int y = 1; y < 128; y++) {
               if (r.isCarved(x, y, z)) {
                  run++;
               } else {
                  if (run > best) {
                     best = run;
                  }
                  run = 0;
               }
            }
            if (run > best) {
               best = run;
            }
         }
      }
      System.out.println("tallest ravine run=" + best);
      check(best >= 15, "ravines breach tall (got " + best + ")");

      boolean lavaOk = false;
      outer:
      for (int x = -256; x < 256; x += 2) {
         for (int z = -256; z < 256; z += 2) {
            for (int y = 1; y < 10; y++) {
               if (r.isCarved(x, y, z)) {
                  int h = g.heightAt(x, z);
                  check(g.blockAt(x, y, z, h) == Blocks.LAVA_ID, "deep ravine floods lava @" + x + "," + y + "," + z);
                  lavaOk = true;
                  break outer;
               }
            }
         }
      }
      check(lavaOk, "deep ravine carve exists");

      boolean breachOk = false;
      outer2:
      for (int x = -256; x < 256; x += 2) {
         for (int z = -256; z < 256; z += 2) {
            int run = 0;
            for (int y = 10; y < 128; y++) {
               if (r.isCarved(x, y, z)) {
                  run++;
               } else {
                  run = 0;
               }
               if (run >= 15) {
                  int h = g.heightAt(x, z);
                  boolean open = true;
                  for (int yy = y - 14; yy <= y; yy++) {
                     int id = g.blockAt(x, yy, z, h);
                     if (id != 0 && !Blocks.isOre(id)) {
                        open = false;
                        break;
                     }
                  }
                  check(open, "breach daylights @" + x + "," + z);
                  breachOk = true;
                  break outer2;
               }
            }
         }
      }
      check(breachOk, "surface breach exists");
      int evicted = r.evictFar(10000, 10000, 2);
      check(evicted > 0, "ravine regions evict far (" + evicted + ")");
      check(r.isCarved(-18, 40, -146) == new RavineCarver(TerrainGenerator.DEFAULT_SEED, 128).isCarved(-18, 40, -146),
         "evicted ravines recompute identically");

      if (failures == 0) System.out.println("RAVINE PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

