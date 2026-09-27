package com.strata.blocks;

import com.strata.core.Config;

public class CubeBlock extends Block {
   private static final float FULL = 1.0F;
   private static final float SIDE = 0.8F;
   private static final float EDGE = 0.6F;

   public final int topTexture;
   public final int sideTexture;
   public final int bottomTexture;
   public final int overlayTexture;

   public CubeBlock(int id, int texture) {
      this(id, texture, texture, texture, 0);
   }

   public CubeBlock(int id, int texture, int light) {
      this(id, texture, texture, texture, light);
   }

   public CubeBlock(int id, int top, int side, int bottom) {
      this(id, top, side, bottom, -1, 0);
   }

   public CubeBlock(int id, int top, int side, int bottom, int light) {
      this(id, top, side, bottom, -1, light);
   }

   public CubeBlock(int id, int top, int side, int bottom, int overlay, int light) {
      super(id, side, true, light);
      this.topTexture = top;
      this.sideTexture = side;
      this.bottomTexture = bottom;
      this.overlayTexture = overlay;
   }

   @Override
   public int particleTile() {
      return this.sideTexture;
   }

   private static boolean showsFace(BlockView level, int x, int y, int z) {
      int id = level.getTile(x, y, z);
      return id <= 0 || !Blocks.isSolid(id) || id == Blocks.LEAF_ID;
   }

   private static int hash(int x, int y, int z, int face) {
      int h = x * 374761393 + y * 668265263 + z * 1440662683 + face * 97234489;
      h ^= h >>> 13;
      h *= 1274126177;
      h ^= h >>> 16;
      return h & 0x7fffffff;
   }

   private static int variantTile(int baseSlot, int x, int y, int z, int face) {
      int[] alts = AtlasStitcher.altsFor(baseSlot);
      if (alts.length == 0) {
         return baseSlot;
      }
      int pick = hash(x, y, z, face) % (alts.length + 1);
      return pick == 0 ? baseSlot : alts[pick - 1];
   }

   @Override
   public void render(MeshBuilder t, BlockView level, int layer, int x, int y, int z) {      float x0 = x + 0.0F;
      float x1 = x + 1.0F;
      float y0 = y + 0.0F;
      float y1 = y + 1.0F;
      float z0 = z + 0.0F;
      float z1 = z + 1.0F;
      if (showsFace(level, x, y - 1, z)) {
         float br = level.getBrightness(x, y - 1, z) * FULL;
         if (br == FULL ^ layer == 1) {
            float[] uv = AtlasStitcher.uv(variantTile(this.bottomTexture, x, y, z, 0));
            t.color(br, br, br);
            float[] cx = {x0, x0, x1, x1};
            float[] cz = {z1, z0, z0, z1};
            float[] cu = {uv[0], uv[0], uv[2], uv[2]};
            float[] cv = {uv[3], uv[1], uv[1], uv[3]};
            int r = Config.BLOCK_ROTATION ? hash(x, y, z, 0) & 3 : 0;
            for (int i = 0; i < 4; i++) {
               t.tex(cu[(i + r) & 3], cv[(i + r) & 3]);
               t.vertex(cx[i], y0, cz[i]);
            }
         }
      }

      if (showsFace(level, x, y + 1, z)) {
         float br = level.getBrightness(x, y + 1, z) * FULL;
         if (br == FULL ^ layer == 1) {
            float[] uv = AtlasStitcher.uv(variantTile(this.topTexture, x, y, z, 1));
            t.color(br, br, br);
            float[] cx = {x1, x1, x0, x0};
            float[] cz = {z1, z0, z0, z1};
            float[] cu = {uv[2], uv[2], uv[0], uv[0]};
            float[] cv = {uv[3], uv[1], uv[1], uv[3]};
            int r = Config.BLOCK_ROTATION ? hash(x, y, z, 1) & 3 : 0;
            for (int i = 0; i < 4; i++) {
               t.tex(cu[(i + r) & 3], cv[(i + r) & 3]);
               t.vertex(cx[i], y1, cz[i]);
            }
         }
      }

      if (showsFace(level, x, y, z - 1)) {
         float br = level.getBrightness(x, y, z - 1) * SIDE;
         if (br == SIDE ^ layer == 1) {
            float[] uv = AtlasStitcher.uv(variantTile(this.sideTexture, x, y, z, 2));
            t.color(br, br, br);
            this.side(t, br, new float[]{x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0},
               new float[]{uv[2], uv[1], uv[0], uv[1], uv[0], uv[3], uv[2], uv[3]}, 0.0F, -1.0F);
         }
      }

      if (showsFace(level, x, y, z + 1)) {
         float br = level.getBrightness(x, y, z + 1) * SIDE;
         if (br == SIDE ^ layer == 1) {
            float[] uv = AtlasStitcher.uv(variantTile(this.sideTexture, x, y, z, 3));
            t.color(br, br, br);
            this.side(t, br, new float[]{x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1},
               new float[]{uv[0], uv[1], uv[0], uv[3], uv[2], uv[3], uv[2], uv[1]}, 0.0F, 1.0F);
         }
      }

      if (showsFace(level, x - 1, y, z)) {
         float br = level.getBrightness(x - 1, y, z) * EDGE;
         if (br == EDGE ^ layer == 1) {
            float[] uv = AtlasStitcher.uv(variantTile(this.sideTexture, x, y, z, 4));
            t.color(br, br, br);
            this.side(t, br, new float[]{x0, y1, z1, x0, y1, z0, x0, y0, z0, x0, y0, z1},
               new float[]{uv[2], uv[1], uv[0], uv[1], uv[0], uv[3], uv[2], uv[3]}, -1.0F, 0.0F);
         }
      }

      if (showsFace(level, x + 1, y, z)) {
         float br = level.getBrightness(x + 1, y, z) * EDGE;
         if (br == EDGE ^ layer == 1) {
            float[] uv = AtlasStitcher.uv(variantTile(this.sideTexture, x, y, z, 5));
            t.color(br, br, br);
            this.side(t, br, new float[]{x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1},
               new float[]{uv[0], uv[3], uv[2], uv[3], uv[2], uv[1], uv[0], uv[1]}, 1.0F, 0.0F);
         }
      }
   }

   private void side(MeshBuilder t, float br, float[] p, float[] q, float onx, float onz) {
      for (int i = 0; i < 4; i++) {
         t.tex(q[i * 2], q[i * 2 + 1]);
         t.vertex(p[i * 3], p[i * 3 + 1], p[i * 3 + 2]);
      }
      if (this.overlayTexture < 0) {
         return;
      }
      float[] ov = AtlasStitcher.uv(this.overlayTexture);
      float ou0 = ov[0], ov0 = ov[1], ou1 = ov[2], ov1 = ov[3];
      float bu0 = Math.min(Math.min(q[0], q[2]), Math.min(q[4], q[6]));
      float bu1 = Math.max(Math.max(q[0], q[2]), Math.max(q[4], q[6]));
      float bv0 = Math.min(Math.min(q[1], q[3]), Math.min(q[5], q[7]));
      float bv1 = Math.max(Math.max(q[1], q[3]), Math.max(q[5], q[7]));
      float ox = onx * 0.002F;
      float oz = onz * 0.002F;
      for (int i = 0; i < 4; i++) {
         float ou = q[i * 2] <= bu0 ? ou0 : ou1;
         float ovv = q[i * 2 + 1] <= bv0 ? ov0 : ov1;
         t.tex(ou, ovv);
         t.vertex(p[i * 3] + ox, p[i * 3 + 1], p[i * 3 + 2] + oz);
      }
   }
}

