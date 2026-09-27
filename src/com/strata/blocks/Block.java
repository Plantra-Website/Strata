package com.strata.blocks;

import com.strata.core.AABB;

public abstract class Block {
   public final int id;
   public final int texture;
   public final boolean solid;
   public final int light;
   public final BlockState state;

   protected Block(int id, int texture, boolean solid, int light) {
      this.id = id;
      this.texture = texture;
      this.solid = solid;
      this.light = light;
      this.state = BlockState.of(this);
   }

   public final BlockState defaultState() {
      return this.state;
   }

   public int particleTile() {
      return this.texture;
   }

   public abstract void render(MeshBuilder builder, BlockView view, int layer, int x, int y, int z);

   public AABB pickBox(int x, int y, int z) {
      return new AABB(x, y, z, x + 1.0F, y + 1.0F, z + 1.0F);
   }

   public boolean needsSupport() {
      return false;
   }
   public void render(MeshBuilder builder, BlockView view, int layer, int x, int y, int z, BlockState state) {
      this.render(builder, view, layer, x, y, z);
   }

   public void renderCrack(MeshBuilder b, BlockView view, int x, int y, int z, int crackTile) {
      float[] tile = AtlasStitcher.uv(this.texture);
      float u0 = tile[0];
      float v0 = tile[1];
      float u1 = tile[2];
      float v1 = tile[3];
      float x0 = x + 0.0F;
      float x1 = x + 1.0F;
      float y0 = y + 0.0F;
      float y1 = y + 1.0F;
      float z0 = z + 0.0F;
      float z1 = z + 1.0F;
      float[] crack = AtlasStitcher.uv(crackTile);
      crackFace(b, view.getBrightness(x, y - 1, z) * 1.0F, this.texture, crackTile,
         new float[]{x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1},
         new float[]{u0, v1, u0, v0, u1, v0, u1, v1},
         cuv(crack, 0, 1, 0, 0, 1, 0, 1, 1));
      crackFace(b, view.getBrightness(x, y + 1, z) * 1.0F, this.texture, crackTile,
         new float[]{x1, y1, z1, x1, y1, z0, x0, y1, z0, x0, y1, z1},
         new float[]{u1, v1, u1, v0, u0, v0, u0, v1},
         cuv(crack, 1, 1, 1, 0, 0, 0, 0, 1));
      crackFace(b, view.getBrightness(x, y, z - 1) * 0.8F, this.texture, crackTile,
         new float[]{x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0},
         new float[]{u0, v0, u1, v0, u1, v1, u0, v1},
         cuv(crack, 0, 0, 1, 0, 1, 1, 0, 1));
      crackFace(b, view.getBrightness(x, y, z + 1) * 0.8F, this.texture, crackTile,
         new float[]{x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1},
         new float[]{u0, v0, u0, v1, u1, v1, u1, v0},
         cuv(crack, 0, 0, 0, 1, 1, 1, 1, 0));
      crackFace(b, view.getBrightness(x - 1, y, z) * 0.6F, this.texture, crackTile,
         new float[]{x0, y1, z1, x0, y1, z0, x0, y0, z0, x0, y0, z1},
         new float[]{u1, v0, u0, v0, u0, v1, u1, v1},
         cuv(crack, 1, 0, 0, 0, 0, 1, 1, 1));
      crackFace(b, view.getBrightness(x + 1, y, z) * 0.6F, this.texture, crackTile,
         new float[]{x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0},
         new float[]{u0, v0, u0, v1, u1, v1, u1, v0},
         cuv(crack, 1, 0, 1, 1, 0, 1, 0, 0));
   }

   static float[] cuv(float[] crack, float fu0, float fv0, float fu1, float fv1,
         float fu2, float fv2, float fu3, float fv3) {
      float u0 = crack[0];
      float v0 = crack[1];
      float du = crack[2] - crack[0];
      float dv = crack[3] - crack[1];
      return new float[]{u0 + du * fu0, v0 + dv * fv0, u0 + du * fu1, v0 + dv * fv1,
         u0 + du * fu2, v0 + dv * fv2, u0 + du * fu3, v0 + dv * fv3};
   }

   protected static void crackFace(MeshBuilder b, float brightness,
         int maskTile, int crackTile, float[] p, float[] q, float[] cq) {
      float[] tileUv = AtlasStitcher.uv(maskTile);
      float tu0 = tileUv[0];
      float tv0 = tileUv[1];
      float tdu = tileUv[2] - tileUv[0];
      float tdv = tileUv[3] - tileUv[1];
      boolean[] mask = AtlasStitcher.alphaMask(maskTile);
      boolean[] cmask = AtlasStitcher.alphaMask(crackTile);
      b.color(brightness, brightness, brightness);
      for (int pass = 0; pass < 2; pass++) {
         for (int iy = 0; iy < 16; iy++) {
            for (int ix = 0; ix < 16; ix++) {
               float[] c00 = lerpUV(cq, ix / 16.0F, iy / 16.0F);
               float[] c10 = lerpUV(cq, (ix + 1) / 16.0F, iy / 16.0F);
               float[] c11 = lerpUV(cq, (ix + 1) / 16.0F, (iy + 1) / 16.0F);
               float[] c01 = lerpUV(cq, ix / 16.0F, (iy + 1) / 16.0F);
               if (!crackOpaque(cmask, crackTile, c00) && !crackOpaque(cmask, crackTile, c10)
                  && !crackOpaque(cmask, crackTile, c11) && !crackOpaque(cmask, crackTile, c01)) {
                  continue;
               }
               float[] muv = lerpUV(q, (ix + 0.5F) / 16.0F, (iy + 0.5F) / 16.0F);
               int tx = (int)((muv[0] - tu0) / tdu * 16.0F);
               int ty = (int)((muv[1] - tv0) / tdv * 16.0F);
               if (tx < 0 || tx > 15 || ty < 0 || ty > 15 || !mask[ty * 16 + tx]) {
                  continue;
               }
               if (pass == 0) {
                  quadUV(b, lerp2(p, ix / 16.0F, iy / 16.0F), c00[0], c00[1],
                     lerp2(p, (ix + 1) / 16.0F, iy / 16.0F), c10[0], c10[1],
                     lerp2(p, (ix + 1) / 16.0F, (iy + 1) / 16.0F), c11[0], c11[1],
                     lerp2(p, ix / 16.0F, (iy + 1) / 16.0F), c01[0], c01[1]);
               } else {
                  quadUV(b, lerp2(p, ix / 16.0F, iy / 16.0F), c00[0], c00[1],
                     lerp2(p, ix / 16.0F, (iy + 1) / 16.0F), c01[0], c01[1],
                     lerp2(p, (ix + 1) / 16.0F, (iy + 1) / 16.0F), c11[0], c11[1],
                     lerp2(p, (ix + 1) / 16.0F, iy / 16.0F), c10[0], c10[1]);
               }
            }
         }
      }
   }

   private static boolean crackOpaque(boolean[] cmask, int crackTile, float[] cuv) {
      float[] r = AtlasStitcher.uv(crackTile);
      int tx = (int)((cuv[0] - r[0]) / (r[2] - r[0]) * 16.0F);
      int ty = (int)((cuv[1] - r[1]) / (r[3] - r[1]) * 16.0F);
      if (tx < 0 || tx > 15 || ty < 0 || ty > 15) {
         return false;
      }
      return cmask[ty * 16 + tx];
   }

   static float[] lerp2(float[] p, float s, float t) {
      float a0 = p[0] + (p[3] - p[0]) * s;
      float a1 = p[1] + (p[4] - p[1]) * s;
      float a2 = p[2] + (p[5] - p[2]) * s;
      float b0 = p[9] + (p[6] - p[9]) * s;
      float b1 = p[10] + (p[7] - p[10]) * s;
      float b2 = p[11] + (p[8] - p[11]) * s;
      return new float[]{a0 + (b0 - a0) * t, a1 + (b1 - a1) * t, a2 + (b2 - a2) * t};
   }

   static float[] lerpUV(float[] q, float s, float t) {
      float au = q[0] + (q[2] - q[0]) * s;
      float av = q[1] + (q[3] - q[1]) * s;
      float bu = q[6] + (q[4] - q[6]) * s;
      float bv = q[7] + (q[5] - q[7]) * s;
      return new float[]{au + (bu - au) * t, av + (bv - av) * t};
   }

   private static void quadUV(MeshBuilder b, float[] a, float au, float av, float[] c,
         float cu, float cv, float[] d, float du, float dv, float[] e, float eu, float ev) {
      b.tex(au, av);
      b.vertex(a[0], a[1], a[2]);
      b.tex(cu, cv);
      b.vertex(c[0], c[1], c[2]);
      b.tex(du, dv);
      b.vertex(d[0], d[1], d[2]);
      b.tex(eu, ev);
      b.vertex(e[0], e[1], e[2]);
   }
}

