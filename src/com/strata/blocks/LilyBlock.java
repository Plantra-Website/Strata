package com.strata.blocks;

import com.strata.core.AABB;

public class LilyBlock extends Block {
   static final float LIFT = 0.015625F;

   public LilyBlock(int id, int texture) {
      super(id, texture, false, 0);
   }

   @Override
   public boolean canStay(BlockView level, int x, int y, int z) {
      return level.getTile(x, y - 1, z) == Blocks.WATER_ID;
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
         float yy = y + LIFT;
         int h = x * 3129871 ^ z * 116129781 ^ y;
         h = h * h * 42317861 + h * 11;
         int rot = (h >> 16) & 3;
         float[] cx = {x, x, x + 1.0F, x + 1.0F};
         float[] cz = {z, z + 1.0F, z + 1.0F, z};
         float[] cu = {u0, u0, u1, u1};
         float[] cv = {v1, v0, v0, v1};
         t.quad(cx[rot & 3], yy, cz[rot & 3],
            cx[(rot + 1) & 3], yy, cz[(rot + 1) & 3],
            cx[(rot + 2) & 3], yy, cz[(rot + 2) & 3],
            cx[(rot + 3) & 3], yy, cz[(rot + 3) & 3],
            cu[rot & 3], cv[rot & 3],
            cu[(rot + 1) & 3], cv[(rot + 1) & 3],
            cu[(rot + 2) & 3], cv[(rot + 2) & 3],
            cu[(rot + 3) & 3], cv[(rot + 3) & 3]);
      }
   }
}

