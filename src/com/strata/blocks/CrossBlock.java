package com.strata.blocks;

import com.strata.core.AABB;

public class CrossBlock extends Block {
   static final float HALF = 0.45F;
   public CrossBlock(int id, int texture) {
      super(id, texture, false, 0);
   }

   @Override
   public AABB pickBox(int x, int y, int z) {
      return new AABB(x + 0.3F, y, z + 0.3F, x + 0.7F, y + 0.6F, z + 0.7F);
   }

   @Override
   public void renderCrack(MeshBuilder b, BlockView view, int x, int y, int z, int crackTile) {
      float br = view.getBrightness(x, y, z);
      float[] uv = AtlasStitcher.uv(this.texture);
      float u0 = uv[0];
      float u1 = uv[2];
      float v0 = uv[1];
      float v1 = uv[3];
            float[] crack = AtlasStitcher.uv(crackTile);
float y0 = y + 0.0F;
      float y1 = y + 1.0F;
      float ax0 = x + 0.5F - HALF;
      float ax1 = x + 0.5F + HALF;
      float az0 = z + 0.5F - HALF;
      float az1 = z + 0.5F + HALF;
      crackFace(b, br, this.texture, crackTile,
         new float[]{ax0, y0, az0, ax1, y0, az1, ax1, y1, az1, ax0, y1, az0},
         new float[]{u0, v1, u1, v1, u1, v0, u0, v0},
         cuv(crack, 0.05F, 1.0F, 0.95F, 1.0F, 0.95F, 0.0F, 0.05F, 0.0F));
      crackFace(b, br, this.texture, crackTile,
         new float[]{ax0, y0, az1, ax1, y0, az0, ax1, y1, az0, ax0, y1, az1},
         new float[]{u0, v1, u1, v1, u1, v0, u0, v0},
         cuv(crack, 0.05F, 1.0F, 0.95F, 1.0F, 0.95F, 0.0F, 0.05F, 0.0F));
   }

   @Override
   public void render(MeshBuilder t, BlockView level, int layer, int x, int y, int z) {
      float br = level.getBrightness(x, y, z);
      if (br == 1.0F ^ layer == 1) {
      float[] uv = AtlasStitcher.uv(this.texture);
      float u0 = uv[0];
      float u1 = uv[2];
      float v0 = uv[1];
      float v1 = uv[3];
      t.color(br, br, br);
         float y0 = y + 0.0F;
         float y1 = y + 1.0F;
         float ax0 = x + 0.5F - HALF;
         float ax1 = x + 0.5F + HALF;
         float az0 = z + 0.5F - HALF;
         float az1 = z + 0.5F + HALF;
         t.quad(ax0, y0, az0, ax1, y0, az1, ax1, y1, az1, ax0, y1, az0, u0, v1, u1, v1, u1, v0, u0, v0);
         t.quad(ax0, y0, az1, ax1, y0, az0, ax1, y1, az0, ax0, y1, az1, u0, v1, u1, v1, u1, v0, u0, v0);
      }
   }
}

