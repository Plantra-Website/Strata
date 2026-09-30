package com.strata.blocks;

import com.strata.core.AABB;


public class TorchBlock extends Block {
   static final float POST_MIN = 7.0F / 16.0F;
   static final float POST_MAX = 9.0F / 16.0F;
   static final float POST_TOP = 10.0F / 16.0F;

   static float[] strip(int tile) {
      float[] origin = AtlasStitcher.tileRect(tile);
      float tw = origin[2];
      float th = origin[3];
      float su0 = origin[0] + tw * 7.05F / 16.0F;
      float su1 = origin[0] + tw * 8.95F / 16.0F;
      float sv0 = origin[1] + th * 6.05F / 16.0F;
      float sv1 = origin[1] + th * 15.95F / 16.0F;
      float capTop0 = origin[1] + th * 6.05F / 16.0F;
      float capTop1 = origin[1] + th * 7.95F / 16.0F;
      float capBot0 = origin[1] + th * 14.05F / 16.0F;
      float capBot1 = origin[1] + th * 15.95F / 16.0F;
      return new float[]{su0, su1, sv0, sv1, capTop0, capTop1, capBot0, capBot1};
   }

   public TorchBlock(int id, int texture, int light) {
      super(id, texture, false, light);
   }

   @Override
   public AABB pickBox(int x, int y, int z) {
      return new AABB(x + 0.35F, y, z + 0.35F, x + 0.65F, y + 0.6F, z + 0.65F);
   }

   @Override
   public boolean needsSupport() {
      return true;
   }

   @Override
   public boolean canStay(BlockView level, int x, int y, int z) {
      int facing = level.getBlockState(x, y, z).data;
      if (facing < 2 || facing > 5) {
         int below = level.getTile(x, y - 1, z);
         return below != Blocks.SNOW_LAYER_ID && level.isSolidTile(x, y - 1, z);
      }
      return wallSolid(level, x, y, z, facing);
   }

   public static boolean wallSolid(BlockView level, int x, int y, int z, int face) {
      if (face == 2) {
         return level.isSolidTile(x, y, z + 1);
      }
      if (face == 3) {
         return level.isSolidTile(x, y, z - 1);
      }
      if (face == 4) {
         return level.isSolidTile(x + 1, y, z);
      }
      if (face == 5) {
         return level.isSolidTile(x - 1, y, z);
      }
      return false;
   }

   @Override
   public void render(MeshBuilder t, BlockView level, int layer, int x, int y, int z) {
      this.render(t, level, layer, x, y, z, BlockState.of(this));
   }

   @Override
   public void render(MeshBuilder t, BlockView level, int layer, int x, int y, int z, BlockState state) {
      if (layer != 0) {
         return;
      }
      float[] s = strip(this.texture);
      float su0 = s[0], su1 = s[1], sv0 = s[2], sv1 = s[3];
      float br = level.getBrightness(x, y, z);
      t.color(br, br, br);
      int facing = state.data;
      if (facing >= 2 && facing <= 5) {
         float w = (POST_MAX - POST_MIN) / 2.0F;
         float xc = x + 0.5F;
         float zc = z + 0.5F;
         float y0 = y + 0.2F;
         float y1 = y + 0.8F;
         float wall, lean;
         boolean xAxis = facing == 4 || facing == 5;
         if (facing == 2) {
            wall = z + 1.0F;
            lean = -1.0F;
         } else if (facing == 3) {
            wall = z + 0.0F;
            lean = 1.0F;
         } else if (facing == 4) {
            wall = x + 1.0F;
            lean = -1.0F;
         } else {
            wall = x + 0.0F;
            lean = 1.0F;
         }
         float zb0 = wall + lean * -0.1F;
         float zb1 = wall + lean * -0.35F;
         if (!xAxis) {
            t.quad(xc - w, y0, zb0, xc + w, y0, zb0, xc + w, y1, zb1, xc - w, y1, zb1,
               su0, sv1, su1, sv1, su1, sv0, su0, sv0);
            t.quad(xc, y0, zb0 - w, xc, y0, zb0 + w, xc, y1, zb1 + w, xc, y1, zb1 - w,
               su0, sv1, su1, sv1, su1, sv0, su0, sv0);
         } else {
            t.quad(zb0, y0, zc - w, zb0, y0, zc + w, zb1, y1, zc + w, zb1, y1, zc - w,
               su0, sv1, su1, sv1, su1, sv0, su0, sv0);
            t.quad(zb0, y0, zc, zb0, y1, zc, zb1, y1, zc, zb1, y0, zc,
               su0, sv1, su1, sv1, su1, sv0, su0, sv0);
         }
         float capTop0 = s[4];
         float capTop1 = s[5];
         float hx0 = xc - w, hx1 = xc + w, hz = zb1;
         if (xAxis) {
            hx0 = zb1;
            hx1 = zb1;
            hz = zc;
            t.quad(hx0 - w, y1, hz - w, hx0 + w, y1, hz - w, hx0 + w, y1, hz + w, hx0 - w, y1, hz + w,
               su0, capTop1, su1, capTop1, su1, capTop0, su0, capTop0);
         } else {
            t.quad(hx0, y1, hz - w, hx1, y1, hz - w, hx1, y1, hz + w, hx0, y1, hz + w,
               su0, capTop1, su1, capTop1, su1, capTop0, su0, capTop0);
         }
         return;
      }
      this.renderFloor(t, su0, su1, sv0, sv1, s, br, x, y, z);
   }

   private void renderFloor(MeshBuilder t, float su0, float su1, float sv0, float sv1,
         float[] s, float br, int x, int y, int z) {
      float capTop0 = s[4];
      float capTop1 = s[5];
      float capBot0 = s[6];
      float capBot1 = s[7];
      float xa = x + POST_MIN;
      float xb = x + POST_MAX;
      float ya = y + 0.0F;
      float yb = y + POST_TOP;
      float za = z + POST_MIN;
      float zb = z + POST_MAX;
      t.quad(xa, ya, za, xa, ya, zb, xa, yb, zb, xa, yb, za, su0, sv1, su1, sv1, su1, sv0, su0, sv0);
      t.quad(xb, ya, zb, xb, ya, za, xb, yb, za, xb, yb, zb, su0, sv1, su1, sv1, su1, sv0, su0, sv0);
      t.quad(xa, ya, zb, xb, ya, zb, xb, yb, zb, xa, yb, zb, su0, sv1, su1, sv1, su1, sv0, su0, sv0);
      t.quad(xb, ya, za, xa, ya, za, xa, yb, za, xb, yb, za, su0, sv1, su1, sv1, su1, sv0, su0, sv0);
      t.quad(xa, yb, zb, xb, yb, zb, xb, yb, za, xa, yb, za, su0, capTop1, su1, capTop1, su1, capTop0, su0, capTop0);
      t.quad(xa, ya, za, xb, ya, za, xb, ya, zb, xa, ya, zb, su0, capBot1, su1, capBot1, su1, capBot1, su0, capBot1);
   }

   @Override
   public void renderCrack(MeshBuilder t, BlockView level, int x, int y, int z, int crackTile) {
            float[] s = strip(this.texture);
      float su0 = s[0], su1 = s[1], sv0 = s[2], sv1 = s[3];
      float capTop0 = s[4];
      float capTop1 = s[5];
      float capBot0 = s[6];
      float capBot1 = s[7];
      float br = level.getBrightness(x, y, z);
      float[] crack = AtlasStitcher.uv(crackTile);
      float xa = x + POST_MIN;
      float xb = x + POST_MAX;
      float ya = y + 0.0F;
      float yb = y + POST_TOP;
      float za = z + POST_MIN;
      float zb = z + POST_MAX;
      float[] sideQ = new float[]{su0, sv1, su1, sv1, su1, sv0, su0, sv0};
      crackFace(t, br, this.texture, crackTile,
         new float[]{xa, ya, za, xa, ya, zb, xa, yb, zb, xa, yb, za}, sideQ,
         cuv(crack, 0.4375F, 1.0F, 0.5625F, 1.0F, 0.5625F, 0.375F, 0.4375F, 0.375F));
      crackFace(t, br, this.texture, crackTile,
         new float[]{xb, ya, zb, xb, ya, za, xb, yb, za, xb, yb, zb}, sideQ,
         cuv(crack, 0.4375F, 1.0F, 0.5625F, 1.0F, 0.5625F, 0.375F, 0.4375F, 0.375F));
      crackFace(t, br, this.texture, crackTile,
         new float[]{xa, ya, zb, xb, ya, zb, xb, yb, zb, xa, yb, zb}, sideQ,
         cuv(crack, 0.4375F, 1.0F, 0.5625F, 1.0F, 0.5625F, 0.375F, 0.4375F, 0.375F));
      crackFace(t, br, this.texture, crackTile,
         new float[]{xb, ya, za, xa, ya, za, xa, yb, za, xb, yb, za}, sideQ,
         cuv(crack, 0.5625F, 1.0F, 0.4375F, 1.0F, 0.4375F, 0.375F, 0.5625F, 0.375F));
      crackFace(t, br, this.texture, crackTile,
         new float[]{xa, yb, zb, xb, yb, zb, xb, yb, za, xa, yb, za},
         new float[]{su0, capTop1, su1, capTop1, su1, capTop0, su0, capTop0},
         cuv(crack, 0.4375F, 0.5625F, 0.5625F, 0.5625F, 0.5625F, 0.4375F, 0.4375F, 0.4375F));
      crackFace(t, br, this.texture, crackTile,
         new float[]{xa, ya, za, xb, ya, za, xb, ya, zb, xa, ya, zb},
         new float[]{su0, capBot0, su1, capBot0, su1, capBot1, su0, capBot1},
         cuv(crack, 0.4375F, 0.4375F, 0.5625F, 0.4375F, 0.5625F, 0.5625F, 0.4375F, 0.5625F));
   }
}

