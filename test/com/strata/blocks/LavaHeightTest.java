package com.strata.blocks;

public class LavaHeightTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static void checkF(float got, float want, String msg) {
      if (Math.abs(got - want) > 1e-2) { failures++; System.out.println("FAIL: " + msg + " got=" + got + " want=" + want); }
   }

   static class Stub implements BlockView {
      final java.util.HashMap<Long, Integer> tiles = new java.util.HashMap<>();
      final java.util.HashMap<Long, Integer> datas = new java.util.HashMap<>();

      static long key(int x, int y, int z) {
         return ((long)x << 42) | ((long)(z & 0x3FFFFF) << 21) | (y & 0x1FFFFF);
      }

      void set(int x, int y, int z, int id, int data) {
         tiles.put(key(x, y, z), id);
         datas.put(key(x, y, z), data);
      }

      @Override public boolean isSolidTile(int x, int y, int z) {
         return Blocks.isSolid(this.getTile(x, y, z));
      }

      @Override public float getBrightness(int x, int y, int z) {
         return 1.0F;
      }

      @Override public int getTile(int x, int y, int z) {
         Integer t = tiles.get(key(x, y, z));
         return t == null ? 0 : t;
      }

      @Override public BlockState getBlockState(int x, int y, int z) {
         Integer t = tiles.get(key(x, y, z));
         if (t == null || t == 0) {
            return BlockState.AIR;
         }
         Integer d = datas.get(key(x, y, z));
         return BlockState.of(Blocks.byId(t), d == null ? 0 : d);
      }
   }

   static float maxY(MeshBuilder b) {
      float[] v = b.vertices();
      float m = -99;
      for (int i = 1; i < v.length; i += 3) {
         if (v[i] > m) {
            m = v[i];
         }
      }
      return m;
   }

   static float minY(MeshBuilder b) {
      float[] v = b.vertices();
      float m = 99;
      for (int i = 1; i < v.length; i += 3) {
         if (v[i] < m) {
            m = v[i];
         }
      }
      return m;
   }

   static void pad(Stub s, int level) {
      for (int x = 0; x < 3; x++) {
         for (int z = 0; z < 3; z++) {
            s.set(x, 10, z, Blocks.LAVA_ID, level);
         }
      }
   }

   public static void main(String[] args) {
      check(Math.abs(FluidBlock.surface(0) - 8.0F / 9.0F) < 1e-4, "source surface 8/9");
      check(Math.abs(FluidBlock.surface(4) - 4.0F / 9.0F) < 1e-4, "range-4 surface 4/9");

      Block lava = Blocks.byId(Blocks.LAVA_ID);
      check(lava instanceof LavaBlock, "lava registers the fluid renderer");

      Stub s = new Stub();
      pad(s, 0);
      MeshBuilder b = new MeshBuilder();
      b.init();
      lava.render(b, s, 0, 1, 10, 1, BlockState.of(lava, 0));
      check(b.count() > 0, "source emits");
      checkF(maxY(b), 10 + 8.0F / 9.0F, "source top lowered");
      checkF(minY(b), 10.0F, "source bottom full");

      Stub f = new Stub();
      pad(f, 4);
      MeshBuilder b2 = new MeshBuilder();
      b2.init();
      lava.render(b2, f, 0, 1, 10, 1, BlockState.of(lava, 4));
      check(b2.count() > 0, "flow emits");
      checkF(maxY(b2), 10 + 4.0F / 9.0F, "range-4 top lower than source");

      Stub tip = new Stub();
      tip.set(1, 10, 1, Blocks.LAVA_ID, 6);
      MeshBuilder b3 = new MeshBuilder();
      b3.init();
      lava.render(b3, tip, 0, 1, 10, 1, BlockState.of(lava, 6));
      check(b3.count() > 0, "tip emits");
      checkF(maxY(b3), 10 + 2.0F / 9.0F, "tip stays tapered");

      Block water = Blocks.byId(Blocks.WATER_ID);
      check(water instanceof WaterBlock, "water registers the fluid renderer");
      Stub w = new Stub();
      for (int x = 0; x < 3; x++) {
         for (int z = 0; z < 3; z++) {
            w.set(x, 10, z, Blocks.WATER_ID, 0);
         }
      }
      MeshBuilder w0 = new MeshBuilder();
      w0.init();
      water.render(w0, w, 0, 1, 10, 1, BlockState.of(water, 0));
      check(w0.count() == 0, "water emits nothing on layer 0");
      MeshBuilder w1 = new MeshBuilder();
      w1.init();
      water.render(w1, w, 1, 1, 10, 1, BlockState.of(water, 0));
      check(w1.count() == 0, "water emits nothing on layer 1");
      MeshBuilder w2 = new MeshBuilder();
      w2.init();
      water.render(w2, w, 2, 1, 10, 1, BlockState.of(water, 0));
      check(w2.count() > 0, "water emits on layer 2");
      checkF(maxY(w2), 10 + 8.0F / 9.0F, "water source lowered like lava");

      if (failures == 0) System.out.println("LAVAHEIGHT PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

