package com.strata.blocks;

public class IceBlock extends CubeBlock {
   public IceBlock(int id, int texture) {
      super(id, texture, texture, texture);
   }

   @Override
   public void render(MeshBuilder t, BlockView level, int layer, int x, int y, int z) {
      if (layer != 2) {
         return;
      }
      float x0 = x + 0.0F;
      float x1 = x + 1.0F;
      float y0 = y + 0.0F;
      float y1 = y + 1.0F;
      float z0 = z + 0.0F;
      float z1 = z + 1.0F;
      this.face(t, level, x, y, z, x, y - 1, z, FULL, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
      this.face(t, level, x, y, z, x, y + 1, z, FULL, 1, x1, y1, z1, x1, y1, z0, x0, y1, z0, x0, y1, z1);
      this.face(t, level, x, y, z, x, y, z - 1, SIDE, 2, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0);
      this.face(t, level, x, y, z, x, y, z + 1, SIDE, 3, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
      this.face(t, level, x, y, z, x - 1, y, z, EDGE, 4, x0, y1, z1, x0, y1, z0, x0, y0, z0, x0, y0, z1);
      this.face(t, level, x, y, z, x + 1, y, z, EDGE, 5, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
   }

   private void face(MeshBuilder t, BlockView level, int x, int y, int z, int nx, int ny, int nz, float shade, int face,
         float ax, float ay, float az, float bx, float by, float bz,
         float cx, float cy, float cz, float dx, float dy, float dz) {
      if (!showsFace(this.id, level, nx, ny, nz)) {
         return;
      }
      float br = level.getBrightness(nx, ny, nz) * shade;
      float[] uv = AtlasStitcher.uv(variantTile(this.sideTexture, nx, ny, nz, face));
      lightColor(t, level, nx, ny, nz, shade);
      tintFor(t, level, this.sideTexture, x, z);
      t.quad(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz,
         uv[0], uv[1], uv[2], uv[1], uv[2], uv[3], uv[0], uv[3]);
   }
}

