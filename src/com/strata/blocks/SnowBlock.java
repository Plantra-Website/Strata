package com.strata.blocks;

import com.strata.core.AABB;

public class SnowBlock extends Block {
   static final float UNIT = 2.0F / 16.0F;

   public SnowBlock(int id, int texture) {
      super(id, texture, false, 0);
   }

   @Override
   public boolean canStay(BlockView level, int x, int y, int z) {
      int below = level.getTile(x, y - 1, z);
      return below != 0 && below != Blocks.ICE_ID && !Blocks.isLeaves(below)
         && Blocks.isSolid(below);
   }

   @Override
   public AABB pickBox(int x, int y, int z) {
      return new AABB(x, y, z, x + 1.0F, y + 0.25F, z + 1.0F);
   }

   @Override
   public AABB pickBox(BlockView level, int x, int y, int z) {
      float h = UNIT * (1 + (level.getBlockState(x, y, z).data & 7));
      return new AABB(x, y, z, x + 1.0F, y + h, z + 1.0F);
   }

   @Override
   public AABB collisionBox(BlockView level, int x, int y, int z) {
      if ((level.getBlockState(x, y, z).data & 7) >= 3) {
         return new AABB(x, y, z, x + 1.0F, y + 0.5F, z + 1.0F);
      }
      return null;
   }

   @Override
   public void render(MeshBuilder t, BlockView level, int layer, int x, int y, int z) {
      this.render(t, level, layer, x, y, z, BlockState.of(this));
   }

   @Override
   public void render(MeshBuilder t, BlockView level, int layer, int x, int y, int z, BlockState state) {
      float br = level.getBrightness(x, y, z);
      if (br == 1.0F ^ layer == 1) {
         float[] uv = AtlasStitcher.uv(this.texture);
         float u0 = uv[0];
         float u1 = uv[2];
         float v0 = uv[1];
         float v1 = uv[3];
         lightColor(t, level, x, y, z, 1.0F);
         tintFor(t, level, this.texture, x, z);
         float y0 = y + 0.0F;
         float y1 = y + UNIT * (1 + (state.data & 7));
         float vSliceTop = CubeBlock.sliceV(v0, v1, (y1 - y0));
         t.quad(x, y1, z, x, y1, z + 1.0F, x + 1.0F, y1, z + 1.0F, x + 1.0F, y1, z,
            u0, v1, u0, v0, u1, v0, u1, v1);
         t.quad(x, y0, z, x, y1, z, x + 1.0F, y1, z, x + 1.0F, y0, z,
            u0, v1, u0, vSliceTop, u1, vSliceTop, u1, v1);
         t.quad(x, y0, z + 1.0F, x + 1.0F, y0, z + 1.0F, x + 1.0F, y1, z + 1.0F, x, y1, z + 1.0F,
            u0, v1, u1, v1, u1, vSliceTop, u0, vSliceTop);
         t.quad(x, y0, z, x, y0, z + 1.0F, x, y1, z + 1.0F, x, y1, z,
            u0, v1, u0, vSliceTop, u1, vSliceTop, u1, v1);
         t.quad(x + 1.0F, y0, z, x + 1.0F, y1, z, x + 1.0F, y1, z + 1.0F, x + 1.0F, y0, z + 1.0F,
            u0, v1, u0, vSliceTop, u1, vSliceTop, u1, v1);
      }
   }
}

