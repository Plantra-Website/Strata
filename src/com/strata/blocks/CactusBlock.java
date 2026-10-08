package com.strata.blocks;

import com.strata.core.AABB;

public class CactusBlock extends Block {
    static final float IN = 1.0F / 16.0F;

    public final int topTexture;
    public final int sideTexture;
    public final int bottomTexture;

    public CactusBlock(int id, int top, int side, int bottom) {
       super(id, side, true, 0);
       this.topTexture = top;
       this.sideTexture = side;
       this.bottomTexture = bottom;
    }

    @Override
    public int particleTile() {
       return this.sideTexture;
    }

    @Override
    public boolean canStay(BlockView level, int x, int y, int z) {
       int below = level.getTile(x, y - 1, z);
       if (below != Blocks.CACTUS_ID && below != Blocks.SAND_ID) {
          return false;
       }
       return !Blocks.isSolid(level.getTile(x + 1, y, z))
          && !Blocks.isSolid(level.getTile(x - 1, y, z))
          && !Blocks.isSolid(level.getTile(x, y, z + 1))
          && !Blocks.isSolid(level.getTile(x, y, z - 1));
    }

    @Override
    public AABB pickBox(int x, int y, int z) {
       return new AABB(x + IN, y, z + IN, x + 1.0F - IN, y + 1.0F, z + 1.0F - IN);
    }

    @Override
    public AABB collisionBox(BlockView level, int x, int y, int z) {
       return new AABB(x + IN, y, z + IN, x + 1.0F - IN, y + 1.0F, z + 1.0F - IN);
    }

    private static boolean show(BlockView level, int x, int y, int z) {
       int n = level.getTile(x, y, z);
       return n != Blocks.CACTUS_ID && !Blocks.blocksLight(n);
    }

    @Override
    public void render(MeshBuilder t, BlockView level, int layer, int x, int y, int z) {
       float br = level.getBrightness(x, y, z);
       if (br == 1.0F ^ layer == 1) {
          float x0 = x + 0.0F;
          float x1 = x + 1.0F;
          float y0 = y + 0.0F;
          float y1 = y + 1.0F;
          float z0 = z + 0.0F;
          float z1 = z + 1.0F;

          float[] su = AtlasStitcher.uv(this.sideTexture);
          float[] tu = AtlasStitcher.uv(this.topTexture);
          float[] bu = AtlasStitcher.uv(this.bottomTexture);

          lightColor(t, level, x, y, z, 1.0F);
          tintFor(t, level, this.sideTexture, x, z);

          if (show(level, x, y, z - 1)) {
             side(t, x1, y0, z0 + IN, x0, y0, z0 + IN, x0, y1, z0 + IN, x1, y1, z0 + IN, su);
          }
          if (show(level, x, y, z + 1)) {
             side(t, x0, y0, z1 - IN, x1, y0, z1 - IN, x1, y1, z1 - IN, x0, y1, z1 - IN, su);
          }
          if (show(level, x - 1, y, z)) {
             side(t, x0 + IN, y0, z0, x0 + IN, y0, z1, x0 + IN, y1, z1, x0 + IN, y1, z0, su);
          }
          if (show(level, x + 1, y, z)) {
             side(t, x1 - IN, y0, z1, x1 - IN, y0, z0, x1 - IN, y1, z0, x1 - IN, y1, z1, su);
          }

          if (show(level, x, y + 1, z)) {
             t.quad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0,
                    tu[0], tu[3], tu[2], tu[3], tu[2], tu[1], tu[0], tu[1]);
          }

          if (show(level, x, y - 1, z)) {
             t.quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1,
                    bu[0], bu[1], bu[2], bu[1], bu[2], bu[3], bu[0], bu[3]);
          }
       }
    }

    private static void side(MeshBuilder t,
          float ax, float ay, float az, float bx, float by, float bz,
          float cx, float cy, float cz, float dx, float dy, float dz, float[] uv) {
       t.quad(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz,
              uv[0], uv[3], uv[2], uv[3], uv[2], uv[1], uv[0], uv[1]);
    }
}
