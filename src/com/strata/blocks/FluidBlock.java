package com.strata.blocks;

import com.strata.core.Config;

public class FluidBlock extends CubeBlock {
   protected final int fluidId;

   public FluidBlock(int id, int texture, int light, boolean solid) {
      super(id, texture, light, solid);
      this.fluidId = id;
   }

   static float surface(int level) {
      if (level >= 8) {
         level = 0;
      }
      if (level < 0) {
         level = 0;
      }
      if (level > 7) {
         level = 7;
      }
      return 1.0F - (level + 1) / 9.0F;
   }

   float cornerHeight(BlockView level, int x, int y, int z) {
      float sum = 0.0F;
      int n = 0;
      for (int dx = -1; dx <= 0; dx++) {
         for (int dz = -1; dz <= 0; dz++) {
            int cx = x + dx;
            int cz = z + dz;
            if (level.getTile(cx, y + 1, cz) == this.fluidId) {
               return 1.0F;
            }
            if (level.getTile(cx, y, cz) == this.fluidId) {
               int raw = level.getBlockState(cx, y, cz).data;
               int lv = raw >= 8 ? 0 : raw;
               int w = lv == 0 ? 11 : 1;
               sum += surface(lv) * w;
               n += w;
            }
         }
      }
      return n == 0 ? 1.0F : sum / n;
   }

   @Override
   public void render(MeshBuilder t, BlockView level, int layer, int x, int y, int z, BlockState state) {
      int meta = state == null ? 0 : state.data;
      if (meta < 0 || meta > 7) {
         meta = 0;
      }
      boolean legacy = this.fluidId == Blocks.LAVA_ID;
      if (!legacy && layer != 2) {
         return;
      }
      float x0 = x + 0.0F;
      float x1 = x + 1.0F;
      float y0 = y + 0.0F;
      float z0 = z + 0.0F;
      float z1 = z + 1.0F;
      float h00 = y + cornerHeight(level, x, y, z);
      float h10 = y + cornerHeight(level, x + 1, y, z);
      float h01 = y + cornerHeight(level, x, y, z + 1);
      float h11 = y + cornerHeight(level, x + 1, y, z + 1);

      if (showsFace(this.id, level, x, y - 1, z)) {
         float br = level.getBrightness(x, y - 1, z) * FULL;
         if (!legacy || br == FULL ^ layer == 1) {
            int tile = variantTile(this.bottomTexture, x, y, z, 0);
            float[] uv = AtlasStitcher.uv(tile);
            lightColor(t, level, x, y - 1, z, FULL);
            tintFor(t, level, tile, x, z);
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

      if (showsFace(this.id, level, x, y + 1, z)) {
         float br = level.getBrightness(x, y + 1, z) * FULL;
         if (!legacy || br == FULL ^ layer == 1) {
            int tile = variantTile(this.topTexture, x, y, z, 1);
            float[] uv = AtlasStitcher.uv(tile);
            lightColor(t, level, x, y + 1, z, FULL);
            tintFor(t, level, tile, x, z);
            float[] cy = {h11, h10, h00, h01};
            float[] cx = {x1, x1, x0, x0};
            float[] cz = {z1, z0, z0, z1};
            float[] cu = {uv[2], uv[2], uv[0], uv[0]};
            float[] cv = {uv[3], uv[1], uv[1], uv[3]};
            int r = Config.BLOCK_ROTATION ? hash(x, y, z, 1) & 3 : 0;
            for (int i = 0; i < 4; i++) {
               t.tex(cu[(i + r) & 3], cv[(i + r) & 3]);
               t.vertex(cx[i], cy[i], cz[i]);
            }
         }
      }

      if (showsFace(this.id, level, x, y, z - 1)) {
         float br = level.getBrightness(x, y, z - 1) * SIDE;
         if (!legacy || br == SIDE ^ layer == 1) {
            int tile = variantTile(this.sideTexture, x, y, z, 2);
            float[] uv = AtlasStitcher.uv(tile);
            lightColor(t, level, x, y, z - 1, SIDE);
            float vt00 = CubeBlock.sliceV(uv[1], uv[3], h00 - y);
            float vt10 = CubeBlock.sliceV(uv[1], uv[3], h10 - y);
            this.side(t, level, x, z, tile, new float[]{x0, h00, z0, x1, h10, z0, x1, y0, z0, x0, y0, z0},
               new float[]{uv[2], vt00, uv[0], vt10, uv[0], uv[3], uv[2], uv[3]}, 0.0F, -1.0F, this.overlayTexture);
         }
      }

      if (showsFace(this.id, level, x, y, z + 1)) {
         float br = level.getBrightness(x, y, z + 1) * SIDE;
         if (!legacy || br == SIDE ^ layer == 1) {
            int tile = variantTile(this.sideTexture, x, y, z, 3);
            float[] uv = AtlasStitcher.uv(tile);
            lightColor(t, level, x, y, z + 1, SIDE);
            float vt01 = CubeBlock.sliceV(uv[1], uv[3], h01 - y);
            float vt11 = CubeBlock.sliceV(uv[1], uv[3], h11 - y);
            this.side(t, level, x, z, tile, new float[]{x0, h01, z1, x0, y0, z1, x1, y0, z1, x1, h11, z1},
               new float[]{uv[0], vt01, uv[0], uv[3], uv[2], uv[3], uv[2], vt11}, 0.0F, 1.0F, this.overlayTexture);
         }
      }

      if (showsFace(this.id, level, x - 1, y, z)) {
         float br = level.getBrightness(x - 1, y, z) * EDGE;
         if (!legacy || br == EDGE ^ layer == 1) {
            int tile = variantTile(this.sideTexture, x, y, z, 4);
            float[] uv = AtlasStitcher.uv(tile);
            lightColor(t, level, x - 1, y, z, EDGE);
            float vt01 = CubeBlock.sliceV(uv[1], uv[3], h01 - y);
            float vt00 = CubeBlock.sliceV(uv[1], uv[3], h00 - y);
            this.side(t, level, x, z, tile, new float[]{x0, h01, z1, x0, h00, z0, x0, y0, z0, x0, y0, z1},
               new float[]{uv[2], vt01, uv[0], vt00, uv[0], uv[3], uv[2], uv[3]}, -1.0F, 0.0F, this.overlayTexture);
         }
      }

      if (showsFace(this.id, level, x + 1, y, z)) {
         float br = level.getBrightness(x + 1, y, z) * EDGE;
         if (!legacy || br == EDGE ^ layer == 1) {
            int tile = variantTile(this.sideTexture, x, y, z, 5);
            float[] uv = AtlasStitcher.uv(tile);
            lightColor(t, level, x + 1, y, z, EDGE);
            float vt10 = CubeBlock.sliceV(uv[1], uv[3], h10 - y);
            float vt11 = CubeBlock.sliceV(uv[1], uv[3], h11 - y);
            this.side(t, level, x, z, tile, new float[]{x1, y0, z1, x1, y0, z0, x1, h10, z0, x1, h11, z1},
               new float[]{uv[0], uv[3], uv[2], uv[3], uv[2], vt10, uv[0], vt11}, 1.0F, 0.0F, this.overlayTexture);
         }
      }
   }
}

