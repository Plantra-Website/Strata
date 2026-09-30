package com.strata.blocks;

import com.strata.core.AABB;

public class VineBlock extends Block {
   static final float OFF = 0.05F;

   public VineBlock(int id, int texture) {
      super(id, texture, false, 0);
   }

   @Override
   public boolean canStay(BlockView level, int x, int y, int z) {
      return Blocks.isSolid(level.getTile(x + 1, y, z))
         || Blocks.isSolid(level.getTile(x - 1, y, z))
         || Blocks.isSolid(level.getTile(x, y, z + 1))
         || Blocks.isSolid(level.getTile(x, y, z - 1))
         || Blocks.isSolid(level.getTile(x, y + 1, z));
   }

   @Override
   public AABB pickBox(int x, int y, int z) {
      return new AABB(x, y, z, x + 1.0F, y + 1.0F, z + 1.0F);
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
         if (Blocks.isSolid(level.getTile(x, y, z - 1))) {
            float z0 = z + OFF;
            t.quad(x, y0, z0, x + 1.0F, y0, z0, x + 1.0F, y1, z0, x, y1, z0,
               u0, v1, u1, v1, u1, v0, u0, v0);
         }
         if (Blocks.isSolid(level.getTile(x, y, z + 1))) {
            float z1 = z + 1.0F - OFF;
            t.quad(x, y0, z1, x, y1, z1, x + 1.0F, y1, z1, x + 1.0F, y0, z1,
               u0, v1, u0, v0, u1, v0, u1, v1);
         }
         if (Blocks.isSolid(level.getTile(x - 1, y, z))) {
            float x0 = x + OFF;
            t.quad(x0, y0, z, x0, y0, z + 1.0F, x0, y1, z + 1.0F, x0, y1, z,
               u0, v1, u1, v1, u1, v0, u0, v0);
         }
         if (Blocks.isSolid(level.getTile(x + 1, y, z))) {
            float x1 = x + 1.0F - OFF;
            t.quad(x1, y0, z, x1, y1, z, x1, y1, z + 1.0F, x1, y0, z + 1.0F,
               u0, v1, u0, v0, u1, v0, u1, v1);
         }
         if (Blocks.isSolid(level.getTile(x, y + 1, z))) {
            float yy = y + 1.0F - OFF;
            t.quad(x, yy, z, x, yy, z + 1.0F, x + 1.0F, yy, z + 1.0F, x + 1.0F, yy, z,
               u0, v1, u0, v0, u1, v0, u1, v1);
         }
      }
   }
}

