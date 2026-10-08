package com.strata.blocks;

import com.strata.core.AABB;

public class VineBlock extends Block {
   static final float OFF = 0.05F;

   public VineBlock(int id, int texture) {
      super(id, texture, false, 0);
   }

   static boolean host(BlockView level, int x, int y, int z) {
      int id = level.getTile(x, y, z);
      return Blocks.isOpaqueCube(id) || Blocks.isLeaves(id);
   }

   @Override
   public boolean canStay(BlockView level, int x, int y, int z) {
      if (host(level, x + 1, y, z) || host(level, x - 1, y, z)
         || host(level, x, y, z + 1) || host(level, x, y, z - 1)
         || host(level, x, y + 1, z)) {
         return true;
      }
      return level.getTile(x, y + 1, z) == Blocks.VINE_ID;
   }

   @Override
   public AABB pickBox(BlockView level, int x, int y, int z) {
      int sides = this.attachSides(level, x, y, z);
      float x0 = (sides & 4) != 0 ? OFF : 0.0F;
      float x1 = (sides & 8) != 0 ? 1.0F - OFF : 1.0F;
      float z0 = (sides & 1) != 0 ? OFF : 0.0F;
      float z1 = (sides & 2) != 0 ? 1.0F - OFF : 1.0F;
      float y1 = 1.0F;
      if (sides == 0 && host(level, x, y + 1, z)) {
         y1 = 1.0F - 1.0F / 16.0F;
      }
      return new AABB(x + x0, y, z + z0, x + x1, y + y1, z + z1);
   }

   private int attachSides(BlockView level, int x, int y, int z) {
      int sides = 0;
      if (host(level, x, y, z - 1)) {
         sides |= 1;
      }
      if (host(level, x, y, z + 1)) {
         sides |= 2;
      }
      if (host(level, x - 1, y, z)) {
         sides |= 4;
      }
      if (host(level, x + 1, y, z)) {
         sides |= 8;
      }
      if (level.getTile(x, y + 1, z) == Blocks.VINE_ID) {
         sides |= this.attachSides(level, x, y + 1, z);
      }
      return sides;
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
          float y0 = y + 0.0F;
          float y1 = y + 1.0F;
          int sides = this.attachSides(level, x, y, z);
          if ((sides & 1) != 0) {
             float z0 = z + OFF;
             t.quad(x, y0, z0, x + 1.0F, y0, z0, x + 1.0F, y1, z0, x, y1, z0,
                u0, v1, u1, v1, u1, v0, u0, v0);
          }
          if ((sides & 2) != 0) {
             float z1 = z + 1.0F - OFF;
             t.quad(x, y0, z1, x, y1, z1, x + 1.0F, y1, z1, x + 1.0F, y0, z1,
                u0, v1, u0, v0, u1, v0, u1, v1);
          }
          if ((sides & 4) != 0) {
             float x0 = x + OFF;
             t.quad(x0, y0, z, x0, y0, z + 1.0F, x0, y1, z + 1.0F, x0, y1, z,
                u0, v1, u1, v1, u1, v0, u0, v0);
          }
          if ((sides & 8) != 0) {
             float x1 = x + 1.0F - OFF;
             t.quad(x1, y0, z, x1, y1, z, x1, y1, z + 1.0F, x1, y0, z + 1.0F,
                u0, v1, u0, v0, u1, v0, u1, v1);
          }
          if (host(level, x, y + 1, z)) {
             float yy = y + 1.0F - OFF;
             t.quad(x, yy, z, x, yy, z + 1.0F, x + 1.0F, yy, z + 1.0F, x + 1.0F, yy, z,
                u0, v1, u0, v0, u1, v0, u1, v1);
          }
      }
   }
}

