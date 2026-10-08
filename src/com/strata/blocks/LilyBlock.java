package com.strata.blocks;

import com.strata.core.AABB;

public class LilyBlock extends Block {
   static final float LIFT = 0.015625F;

   public LilyBlock(int id, int texture) {
      super(id, texture, false, 0);
   }

   @Override
   public boolean canStay(BlockView level, int x, int y, int z) {
      if (level.getTile(x, y - 1, z) != Blocks.WATER_ID) {
         return false;
      }
      return level.getBlockState(x, y - 1, z).data == 0;
   }

   @Override
   public AABB pickBox(int x, int y, int z) {
      return new AABB(x, y, z + 0.0F, x + 1.0F, y + 0.25F, z + 1.0F);
   }

   @Override
   public AABB collisionBox(BlockView level, int x, int y, int z) {
      return new AABB(x, y, z, x + 1.0F, y + LIFT, z + 1.0F);
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
         lightColor(t, level, x, y, z, 1.0F);
         tintFor(t, level, this.texture, x, z);
          float yy = y + LIFT;
          int h = x * 3129871 ^ z * 116129781 ^ y;
          h = h * h * 42317861 + h * 11;
          int rot = (h >> 16) & 3;
          float[] cx = {x, x, x + 1.0F, x + 1.0F};
          float[] cz = {z, z + 1.0F, z + 1.0F, z};
          float[] cu = {u0, u0, u1, u1};
          float[] cv = {v1, v0, v0, v1};
          int[] o = {rot & 3, (rot + 1) & 3, (rot + 2) & 3, (rot + 3) & 3};
          for (int k = 0; k < 4; k++) {
             int i = o[k];
             t.tex(cu[i], cv[i]);
             t.vertex(cx[i], yy, cz[i]);
          }
          t.color(0.5F, level.getSkyLevel(x, y, z) / 15.0F * 0.5F,
             level.getBlockLevel(x, y, z) / 15.0F * 0.5F);
          for (int k = 0; k < 4; k++) {
             int i = o[(4 - k) & 3];
             t.tex(cu[i], cv[i]);
             t.vertex(cx[i], yy, cz[i]);
          }
      }
   }
}

