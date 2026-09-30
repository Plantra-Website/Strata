package com.strata.world.light;

import java.util.HashSet;

public class LightTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static class StubWorld implements LightWorld {
      final int depth = 16;
      final HashSet<Long> opaque = new HashSet<>();
      int notifications = 0;

      static long key(int x, int y, int z) {
         return ((long)x << 32) | (((long)z & 0xFFFF) << 16) | (y & 0xFFFF);
      }

      @Override public int depth() { return this.depth; }

      @Override public boolean isLightBlocker(int x, int y, int z) {
         return this.opaque.contains(key(x, y, z));
      }

      @Override public void lightColumnChanged(int x, int z, int y0, int y1) {
         this.notifications++;
      }
   }

   public static void main(String[] args) {
      StubWorld w = new StubWorld();
      BlockLightEngine block = new BlockLightEngine(w);
      SkyLightEngine sky = new SkyLightEngine(w);

      block.floodAdd(0, 8, 0, 14);
      check(block.get(0, 8, 0) == 14, "source cell 14");
      check(block.get(1, 8, 0) == 13, "falloff 1");
      check(block.get(2, 8, 0) == 12, "falloff 2");
      check(block.get(0, 9, 0) == 13, "falloff up");
      check(block.get(0, 8, 5) == 9, "falloff 5");
      check(block.get(0, 8, 0) == 14, "source stable");
      check(w.notifications > 0, "flood notifies columns");
      System.out.println("falloff ok");

      w.opaque.add(StubWorld.key(1, 8, 0));
      BlockLightEngine block2 = new BlockLightEngine(w);
      block2.floodAdd(0, 8, 0, 14);
      check(block2.get(1, 8, 0) == 0, "wall cell dark");
      check(block2.get(2, 8, 0) < 13, "shadow behind wall (got " + block2.get(2, 8, 0) + ")");
      System.out.println("shadow ok");

      w.opaque.clear();
      BlockLightEngine block3 = new BlockLightEngine(w);
      block3.floodAdd(0, 8, 0, 14);
      check(block3.get(3, 8, 0) == 11, "lit before remove");
      block3.floodRemove(0, 8, 0, 14);
      check(block3.get(0, 8, 0) == 0, "source dark after remove");
      check(block3.get(3, 8, 0) == 0, "field dark after remove");
      check(block3.get(0, 8, 1) == 0, "neighbor dark after remove");
      System.out.println("remove ok");

      BlockLightEngine block4 = new BlockLightEngine(w);
      block4.floodAdd(0, 8, 0, 14);
      block4.floodAdd(0, 8, 4, 14);
      check(block4.get(-11, 8, 0) == 3, "far cell lit by first source (got " + block4.get(-11, 8, 0) + ")");
      block4.floodRemove(0, 8, 0, 14);
      check(block4.get(0, 8, 4) == 14, "second source intact");
      check(block4.get(0, 8, 2) > 0, "overlap healed by second source (got " + block4.get(0, 8, 2) + ")");
      check(block4.get(-11, 8, 0) == 0, "far cell dark after removal (got " + block4.get(-11, 8, 0) + ")");
      System.out.println("overlap ok");

      check(sky.get(5, 15, 5) == 15, "open sky bright");
      check(sky.get(5, 0, 5) == 15, "shaft keeps level to floor");
      for (int cx = -3; cx <= 3; cx++) {
         for (int cz = -3; cz <= 3; cz++) {
            int cd0 = sky.opaqueHeight(cx, cz);
            w.opaque.add(StubWorld.key(cx, 10, cz));
            sky.invalidate(cx, cz);
            int cd1 = sky.opaqueHeight(cx, cz);
            sky.onEdit(cx, 10, cz, false, true, cd0, cd1);
         }
      }
      check(sky.get(0, 15, 0) == 15, "above canopy bright");
      int under = sky.get(0, 5, 0);
      check(under > 0 && under < 15, "bent light under canopy (got " + under + ")");
      int edge = sky.get(3, 5, 3);
      int center = sky.get(0, 5, 0);
      check(edge >= center, "brighter near the edge (" + edge + " vs " + center + ")");
      check(sky.get(0, 10, 0) == 0, "canopy cells hold no sky");
      System.out.println("overhang ok");

      w.opaque.clear();
      SkyLightEngine sky2 = new SkyLightEngine(w);
      check(sky2.get(0, 5, 0) == 15, "open before cap");
      int pd0 = sky2.opaqueHeight(0, 0);
      w.opaque.add(StubWorld.key(0, 10, 0));
      sky2.invalidate(0, 0);
      int pd1 = sky2.opaqueHeight(0, 0);
      sky2.onEdit(0, 10, 0, false, true, pd0, pd1);
      check(sky2.get(0, 5, 0) == 14, "pillar bends, not black (got " + sky2.get(0, 5, 0) + ")");
      int rd0 = sky2.opaqueHeight(0, 0);
      w.opaque.remove(StubWorld.key(0, 10, 0));
      sky2.invalidate(0, 0);
      int rd1 = sky2.opaqueHeight(0, 0);
      sky2.onEdit(0, 10, 0, true, false, rd0, rd1);
      check(sky2.get(0, 5, 0) == 15, "relit after removal");
      int notes = w.notifications;
      sky2.onEdit(0, 5, 0, false, false, 0, 0);
      check(w.notifications == notes, "translucent edit notifies nothing");
      System.out.println("edit ok");

      w.opaque.clear();
      SkyLightEngine sky3 = new SkyLightEngine(w);
      for (int x = -6; x <= 10; x++) {
         for (int y = 0; y <= 2; y++) {
            w.opaque.add(StubWorld.key(x, y, 0));
         }
      }
      for (int y = 0; y < 16; y++) {
         for (int x = 3; x <= 10; x++) {
            if (y == 5 && x <= 6) {
               continue; 
            }
            w.opaque.add(StubWorld.key(x, y, 0));
         }
      }
      check(sky3.get(2, 5, 0) == 15, "canyon floor full bright");
      check(sky3.get(4, 5, 0) == 0, "sealed tunnel dark before breach");
      int wd0 = sky3.opaqueHeight(3, 0);
      w.opaque.remove(StubWorld.key(3, 5, 0));
      sky3.invalidate(3, 0);
      int wd1 = sky3.opaqueHeight(3, 0);
      sky3.onEdit(3, 5, 0, true, false, wd0, wd1);
      check(sky3.get(3, 5, 0) == 14, "breach floods from canyon (got " + sky3.get(3, 5, 0) + ")");
      check(sky3.get(4, 5, 0) > 0, "tunnel interior lights up");
      System.out.println("breach ok");

      w.opaque.clear();
      SkyLightEngine sky4 = new SkyLightEngine(w);
      for (int cx = 0; cx <= 20; cx++) {
         for (int cz = -2; cz <= 2; cz++) {
            w.opaque.add(StubWorld.key(cx, 10, cz));
         }
      }
      check(sky4.get(0, 5, 0) == 0, "virgin lip underside dark before reconcile");
      sky4.reconcileSkyChunk(0, 0);
      int lip = sky4.get(0, 5, 0);
      check(lip > 0 && lip < 15, "lip underside bent, not black (got " + lip + ")");
      check(sky4.get(-1, 5, 0) == 15, "open ground beside lip full bright");
      int deep = sky4.get(10, 5, 0);
      check(deep >= 0 && deep <= lip, "deep interior no brighter than lip (" + deep + " vs " + lip + ")");
      System.out.println("reconcile ok");

      w.opaque.clear();
      SkyLightEngine sky5 = new SkyLightEngine(w);
      for (int cx = -3; cx <= 20; cx++) {
         for (int cz = -2; cz <= 2; cz++) {
            for (int y = 0; y <= 10; y++) {
               if (cx >= 0 && cx <= 5 && y >= 8 && y <= 9) {
                  continue; 
               }
               if (cx < 0 && y > 0) {
                  continue; 
               }
               w.opaque.add(StubWorld.key(cx, y, cz));
            }
         }
      }
      check(sky5.get(2, 8, 0) == 0, "virgin notch dark before reconcile");
      check(sky5.get(-1, 8, 0) == 15, "open air beside notch full bright");
      sky5.reconcileSkyChunk(0, 0);
      int mouth = sky5.get(0, 8, 0);
      check(mouth == 14, "notch mouth one below open air (got " + mouth + ")");
      int notchDeep = sky5.get(5, 8, 0);
      check(notchDeep > 0, "notch interior lit, not zero (got " + notchDeep + ")");
      System.out.println("notch ok");

      if (failures > 0) { System.out.println(failures + " FAILURES"); System.exit(1); }
      System.out.println("LIGHT PASS");
   }
}

