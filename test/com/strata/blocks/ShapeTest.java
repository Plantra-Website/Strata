package com.strata.blocks;

public class ShapeTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static class Bright implements BlockView {
      final java.util.HashMap<Long, Integer> tiles = new java.util.HashMap<>();
      final java.util.HashMap<Long, Integer> datas = new java.util.HashMap<>();

      static long key(int x, int y, int z) {
         return ((long)x << 42) | ((long)(z & 0x3FFFFF) << 21) | (y & 0x1FFFFF);
      }

      void set(int x, int y, int z, int id) {
         tiles.put(key(x, y, z), id);
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
         if (t == null) {
            return BlockState.of(null);
         }
         return Blocks.stateOf(Blocks.byId(t), datas.getOrDefault(key(x, y, z), 0));
      }
   }

   public static void main(String[] args) {
      {
         Bright v = new Bright();
         v.set(0, 10, 0, Blocks.VINE_ID);
         MeshBuilder b = new MeshBuilder();
         b.init();
         Blocks.byId(Blocks.VINE_ID).render(b, v, 0, 0, 10, 0);
         check(b.count() == 0, "wall-less vine emits nothing");
         v.set(0, 10, -1, Blocks.STONE_ID);
         b.init();
         Blocks.byId(Blocks.VINE_ID).render(b, v, 0, 0, 10, 0);
         check(b.count() == 8, "walled vine emits two quads (got " + b.count() + ")");
      }
      {
         Bright v = new Bright();
         v.set(0, 10, 0, Blocks.LILYPAD_ID);
         MeshBuilder b = new MeshBuilder();
         b.init();
         Blocks.byId(Blocks.LILYPAD_ID).render(b, v, 0, 0, 10, 0);
         check(b.count() == 8, "lily emits two quads (got " + b.count() + ")");
         float[] verts = b.vertices();
         for (int i = 1; i < verts.length; i += 3) {
            check(verts[i] >= 10.0F && verts[i] <= 10.2F, "lily stays flat");
         }
      }
      {
         Bright v = new Bright();
         v.set(0, 10, 0, Blocks.SNOW_LAYER_ID);
         MeshBuilder b = new MeshBuilder();
         b.init();
         Blocks.byId(Blocks.SNOW_LAYER_ID).render(b, v, 0, 0, 10, 0);
         check(b.count() == 40, "snow slab emits five quads (got " + b.count() + ")");
         float[] verts = b.vertices();
         float maxY = -99;
         for (int i = 1; i < verts.length; i += 3) {
            if (verts[i] > maxY) maxY = verts[i];
         }
         check(maxY <= 10.13F, "snow stays thin (top " + maxY + ")");
      }
      check(Blocks.byId(Blocks.VINE_ID).pickBox(0, 10, 0) != null, "vine pickable");
      check(Blocks.byId(Blocks.LILYPAD_ID).pickBox(0, 10, 0).y1 <= 10.3F, "lily pick thin");
      check(Blocks.byId(Blocks.SNOW_LAYER_ID).pickBox(0, 10, 0).y1 <= 10.3F, "snow pick thin");
      {
         Bright v = new Bright();
         v.set(0, 10, 0, Blocks.SNOW_LAYER_ID);
         MeshBuilder b = new MeshBuilder();
         b.init();
         Blocks.byId(Blocks.SNOW_LAYER_ID).render(b, v, 0, 0, 10, 0,
            Blocks.stateOf(Blocks.byId(Blocks.SNOW_LAYER_ID), 3));
         float maxY = -99;
         float[] verts = b.vertices();
         for (int i = 1; i < verts.length; i += 3) {
            if (verts[i] > maxY) maxY = verts[i];
         }
         check(Math.abs(maxY - 10.5F) < 0.01F, "data-3 snow stands 8/16 tall (top " + maxY + ")");
      }
      {
         java.util.HashSet<String> firsts = new java.util.HashSet<>();
         for (int px = 0; px < 6; px++) {
            Bright v = new Bright();
            v.set(px, 10, 0, Blocks.LILYPAD_ID);
            MeshBuilder b = new MeshBuilder();
            b.init();
            Blocks.byId(Blocks.LILYPAD_ID).render(b, v, 0, px, 10, 0);
            float[] verts = b.vertices();
            firsts.add(verts[0] + "," + verts[2]);
         }
         check(firsts.size() > 1, "lily corners rotate by position");
      }
      {
         Bright v = new Bright();
         v.set(0, 10, 0, Blocks.VINE_ID);
         v.set(0, 11, 0, Blocks.STONE_ID);
         MeshBuilder b = new MeshBuilder();
         b.init();
         Blocks.byId(Blocks.VINE_ID).render(b, v, 0, 0, 10, 0);
         check(b.count() == 8, "capped vine emits its top quad (got " + b.count() + ")");
      }
      {
         Bright v = new Bright();
         v.set(5, 10, 5, Blocks.ICE_ID);
         check(CubeBlock.showsFace(Blocks.STONE_ID, v, 5, 10, 5), "stone face shows against ice");
         v.set(5, 10, 5, Blocks.STONE_ID);
         check(!CubeBlock.showsFace(Blocks.STONE_ID, v, 5, 10, 5), "stone face hides against stone");
      }
      {
         Bright v = new Bright();
         v.set(0, 10, 0, Blocks.CACTUS_ID);
         MeshBuilder b = new MeshBuilder();
         b.init();
         Blocks.byId(Blocks.CACTUS_ID).render(b, v, 0, 0, 10, 0);
         check(b.count() > 0, "cactus emits quads");
         float[] verts = b.vertices();
         for (int i = 0; i < verts.length; i += 3) {
            check(verts[i] >= 0.0F && verts[i] <= 1.0F, "cactus x in cell");
            check(verts[i + 2] >= 0.0F && verts[i + 2] <= 1.0F, "cactus z in cell");
         }
         boolean inset = false;
         for (int i = 0; i < verts.length; i += 3) {
            if (Math.abs(verts[i] - 1.0F / 16.0F) < 0.001F) {
               inset = true;
            }
         }
         check(inset, "cactus sides inset 1/16");
      }
      {
         com.strata.core.AABB pick = Blocks.byId(Blocks.REED_ID).pickBox(0, 10, 0);
         check(Math.abs(pick.y1 - 11.0F) < 0.01F, "reed pick full height");
         check(Math.abs(pick.x0 - 0.125F) < 0.01F, "reed pick 12/16 wide");
      }
      check(Math.abs(CubeBlock.sliceV(0.0F, 1.0F, 1.0F) - 0.0F) < 1e-6F, "slice full height");
      check(Math.abs(CubeBlock.sliceV(0.0F, 1.0F, 0.0F) - 1.0F) < 1e-6F, "slice zero height");
      check(Math.abs(CubeBlock.sliceV(0.0F, 1.0F, 0.5F) - 0.5F) < 1e-6F, "slice half height");
      check(Math.abs(CubeBlock.sliceV(0.2F, 0.9F, 2.0F) - 0.2F) < 1e-6F, "slice clamps high");
      check(Math.abs(CubeBlock.sliceV(0.2F, 0.9F, -1.0F) - 0.9F) < 1e-6F, "slice clamps low");
      {
         Bright v = new Bright();
         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               v.set(dx, 10, dz, Blocks.WATER_ID, 7);
            }
         }
         MeshBuilder b = new MeshBuilder();
         b.init();
         Blocks.byId(Blocks.WATER_ID).render(b, v, 2, 1, 10, 0,
            Blocks.stateOf(Blocks.byId(Blocks.WATER_ID), 7));
         check(b.count() > 0, "low water emits sides");
         float[] uv = AtlasStitcher.uv(Blocks.byId(Blocks.WATER_ID).texture);
         float expect = CubeBlock.sliceV(uv[1], uv[3], 1.0F / 9.0F);
         boolean sliced = false;
         float[] tc = b.texCoords();
         for (int i = 1; i < tc.length; i += 2) {
            if (Math.abs(tc[i] - expect) < 1e-4F) {
               sliced = true;
            }
         }
         check(sliced, "side tops carry the height slice");
      }
      {
         Bright v = new Bright();
         v.set(0, 10, 0, Blocks.SNOW_LAYER_ID);
         MeshBuilder b = new MeshBuilder();
         b.init();
         Blocks.byId(Blocks.SNOW_LAYER_ID).render(b, v, 0, 0, 10, 0,
            Blocks.stateOf(Blocks.byId(Blocks.SNOW_LAYER_ID), 3));
         float[] uv = AtlasStitcher.uv(Blocks.byId(Blocks.SNOW_LAYER_ID).texture);
         float expect = CubeBlock.sliceV(uv[1], uv[3], 0.5F);
         boolean sliced = false;
         float[] tc = b.texCoords();
         for (int i = 1; i < tc.length; i += 2) {
            if (Math.abs(tc[i] - expect) < 1e-4F) {
               sliced = true;
            }
         }
         check(sliced, "snow strip tops carry the half slice");
      }

      if (failures == 0) System.out.println("SHAPE PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

