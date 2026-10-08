package com.strata.client;

import com.strata.blocks.Blocks;
import com.strata.world.Level;

public class ParticleLightTest {
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

   static int padTop = 0;

   static void pad(Level l, int cx, int cz) {
      int h = surface(l, cx, cz);
      for (int x = cx - 1; x <= cx + 1; x++) {
         for (int z = cz - 1; z <= cz + 1; z++) {
            l.setTile(x, h + 1, z, Blocks.STONE_ID);
            l.setTile(x, h + 2, z, Blocks.STONE_ID);
         }
      }
      l.setTile(cx, h + 1, cz, 0);
      l.setTile(cx, h + 2, cz, 0);
      padTop = h + 1;
   }

   public static void main(String[] args) {
      Level l = new Level(64, false);
      pad(l, -30, -30);
      int nh = padTop;
      pad(l, 30, 30);
      int ph = padTop;

      for (int xx = 0; xx < 4; xx++) {
         float px = -30 + (xx + 0.5F) / 4;
         float pz = -30 + (xx + 0.5F) / 4;
         Particle p = new Particle(l, px, nh + 0.5F, pz, 0, 0, 0, 1);
         check(p.r > 0.9F && p.g > 0.9F && p.b > 0.9F,
            "negative burst bright (got " + p.r + ")");
         Particle q = new Particle(l, 30 + (xx + 0.5F) / 4, ph + 0.5F, 30 + (xx + 0.5F) / 4, 0, 0, 0, 1);
         check(q.r > 0.9F, "positive burst still bright");
      }

      ItemRenderer r = new ItemRenderer(l);
      r.spawn(1, -29.5F, nh + 0.5F, -29.5F, 0, 0, 0, Blocks.DIRT_ID);
      r.spawn(2, 30.5F, ph + 0.5F, 30.5F, 0, 0, 0, Blocks.DIRT_ID);
      check(r.get(1).r > 0.9F, "negative drop bright (got " + r.get(1).r + ")");
      check(r.get(2).r > 0.9F, "positive drop still bright");
      l.setSkylightSub(11);
      r.tick(new com.strata.core.AABB(0, 60, 0, 1, 62, 1));
      check(r.get(1).r < 0.5F, "negative drop dark at night (got " + r.get(1).r + ")");
      check(r.get(2).r < 0.5F, "positive drop dark at night (got " + r.get(2).r + ")");
      l.setSkylightSub(0);
      r.tick(new com.strata.core.AABB(0, 60, 0, 1, 62, 1));
      check(r.get(1).r > 0.9F, "negative drop bright again at dawn");
      if (failures == 0) System.out.println("PARTLIGHT PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

