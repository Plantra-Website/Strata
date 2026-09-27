package com.strata.world;

public class EvictTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) {
      Level l = new Level(64, false);
      int nearX = 0, nearZ = 0;
      int farX = 2048, farZ = -2048;
      int nearTile = l.getTile(nearX, 40, nearZ);
      int farTile = l.getTile(farX, 40, farZ);

      int sx = 8, sz = 8;
      int sy = 60;
      while (sy > 0 && l.getTile(sx, sy, sz) == 0) sy--;
      l.setTile(sx, sy, sz, com.strata.blocks.Blocks.STONE_ID);
      check(l.getTile(sx, sy, sz) == com.strata.blocks.Blocks.STONE_ID, "edit applied");

      int computedBefore = l.computedSize();
      int heightBefore = l.heightCacheSize();
      check(computedBefore > 0, "computed filled (" + computedBefore + ")");
      check(heightBefore > 0, "heightCache filled (" + heightBefore + ")");

      int evicted = l.evictFar(0, 0, 7);
      check(evicted > 0, "evicted far entries (" + evicted + ")");
      check(l.computedSize() < computedBefore || l.heightCacheSize() < heightBefore,
         "caches shrank (computed " + computedBefore + "->" + l.computedSize()
         + " height " + heightBefore + "->" + l.heightCacheSize() + ")");

      check(l.getTile(nearX, 40, nearZ) == nearTile, "near tile stable");
      check(l.getTile(farX, 40, farZ) == farTile, "far tile regenerates");
      check(l.getTile(sx, sy, sz) == com.strata.blocks.Blocks.STONE_ID, "edit survives eviction");

      if (failures == 0) System.out.println("EVICT PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

