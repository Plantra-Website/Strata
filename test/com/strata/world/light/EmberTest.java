package com.strata.world.light;

import com.strata.blocks.Blocks;
import com.strata.world.Level;

public class EmberTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static int surface(Level l, int x, int z) {
      for (int y = 63; y >= 0; y--) {
         int t = l.getTile(x, y, z);
         if (t > 0 && t != 13 && t != 14 && t != 15 && t != 16 && t != 17 && t != 20) return y;
      }
      return -1;
   }

   public static void main(String[] args) {
      Level src = new Level(64);
      int x = 20, z = 20;
      int h = surface(src, x, z);
      src.setTile(x, h + 1, z, Blocks.TORCH_ID);
      check(src.getBlockLevel(x, h + 1, z) == 14, "live torch floods (got " + src.getBlockLevel(x, h + 1, z) + ")");

      Level dst = new Level(64);
      dst.applyBulk(src.snapshotEdits());
      check(dst.getTile(x, h + 1, z) == Blocks.TORCH_ID, "bulk carries torch tile");
      check(dst.getBlockLevel(x, h + 1, z) == 0, "bulk is dark pre-seed (got " + dst.getBlockLevel(x, h + 1, z) + ")");

      dst.seedEmitters();
      check(dst.getBlockLevel(x, h + 1, z) == 14, "seeded torch 14 (got " + dst.getBlockLevel(x, h + 1, z) + ")");
      check(dst.getBlockLevel(x + 1, h + 1, z) == 13, "seeded falloff 13 (got " + dst.getBlockLevel(x + 1, h + 1, z) + ")");
      dst.seedEmitters();
      check(dst.getBlockLevel(x, h + 1, z) == 14, "reseed stable");

      Level src2 = new Level(64);
      int ax = 30, bx2 = 34, z2 = 30;
      int ha = surface(src2, ax, z2);
      int hb = surface(src2, bx2, z2);
      src2.setTile(ax, ha, z2, 3);
      src2.setTile(ax, ha + 1, z2, Blocks.TORCH_ID);
      src2.setTile(bx2, hb, z2, 3);
      src2.setTile(bx2, hb + 1, z2, Blocks.TORCH_ID);
      Level dst2 = new Level(64);
      dst2.applyBulk(src2.snapshotEdits());
      dst2.seedEmitters();
      check(dst2.getBlockLevel(ax, ha + 1, z2) == 14, "torch A own 14 (got " + dst2.getBlockLevel(ax, ha + 1, z2) + ")");
      check(dst2.getBlockLevel(bx2, hb + 1, z2) == 14, "torch B own 14 (got " + dst2.getBlockLevel(bx2, hb + 1, z2) + ")");

      if (failures == 0) System.out.println("ember ok");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

