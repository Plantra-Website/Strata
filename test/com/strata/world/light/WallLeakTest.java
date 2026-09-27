package com.strata.world.light;

import com.strata.blocks.Blocks;
import com.strata.world.Level;

public class WallLeakTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static void buildRoom(Level l) {
      for (int x = 48; x <= 52; x++) {
         for (int y = 58; y <= 62; y++) {
            for (int z = 48; z <= 52; z++) {
               boolean interior = x >= 49 && x <= 51 && y >= 59 && y <= 61 && z >= 49 && z <= 51;
               if (!interior) {
                  l.setTile(x, y, z, Blocks.STONE_ID);
               }
            }
         }
      }
      l.setTile(50, 60, 46, Blocks.TORCH_ID);
   }

   static int glow(Level l) {
      return l.getBlockLevel(50, 60, 50);
   }

   public static void main(String[] args) {
      Level l = new Level(64);
      buildRoom(l);
      check(glow(l) == 0, "sealed room dark (got " + glow(l) + ")");

      l.setTile(50, 60, 48, 0);
      check(glow(l) > 0, "open wall lights room (got " + glow(l) + ")");

      l.setTile(50, 60, 48, Blocks.STONE_ID);
      check(glow(l) == 0, "rebuilt wall darkens room (got " + glow(l) + ")");

      l.setTile(50, 60, 48, 0);
      check(glow(l) > 0, "reopen relights (got " + glow(l) + ")");
      l.setTile(50, 60, 46, 0);
      check(glow(l) == 0, "torch removal darkens room (got " + glow(l) + ")");

      if (failures == 0) System.out.println("WALLLEAK PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

