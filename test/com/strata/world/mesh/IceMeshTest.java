package com.strata.world.mesh;

import com.strata.blocks.Blocks;
import com.strata.world.Level;

public class IceMeshTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) {
      Level level = new Level(64);
      int x = -1, z = -1, h = -1;
      outer:
      for (int cx = 0; cx < 16; cx++) {
         for (int cz = 0; cz < 16; cz++) {
            for (int y = 60; y >= 10; y--) {
               int t = level.getTile(cx, y, cz);
               if (t > 0 && Blocks.isSolid(t)
                  && level.getTile(cx, y + 1, cz) == 0
                  && level.getTile(cx, y + 2, cz) == 0
                  && level.getTile(cx, y + 3, cz) == 0) {
                  x = cx;
                  z = cz;
                  h = y;
                  break outer;
               }
            }
         }
      }
      check(x >= 0, "clear test column found");
      if (x < 0) { System.out.println("ICEMESH ABORT"); System.exit(1); }
      Chunk chunk = new Chunk(level, 0, 0);
      int before = chunk.mesh().counts[2];
      level.setTile(x, h + 1, z, Blocks.ICE_ID);
      check(level.getTile(x, h + 1, z) == Blocks.ICE_ID, "ice placed");
      int after = chunk.mesh().counts[2];
      check(after > before, "ice reaches the blended layer (delta " + (after - before) + " verts)");
      if (failures == 0) System.out.println("ICEMESH PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

