package com.strata.world.gen;

public class GenIsland extends GenLayer {
   public GenIsland(long seed, GenLayer parent) {
      super(seed);
      this.parent = parent;
   }

   @Override
   public int[] generate(int x, int z, int w, int h) {
      int[] p = this.parent.generate(x - 1, z - 1, w + 2, h + 2);
      int[] out = new int[w * h];
      int stride = w + 2;
      for (int dz = 0; dz < h; dz++) {
         for (int dx = 0; dx < w; dx++) {
            int n = p[dx + (dz + 0) * stride];
            int s = p[dx + 2 + (dz + 0) * stride];
            int wv = p[dx + 0 + (dz + 2) * stride];
            int e = p[dx + 2 + (dz + 2) * stride];
            int c = p[dx + 1 + (dz + 1) * stride];
            this.initCellSeed(dx + x, dz + z);
            if (c != 0 || n == 0 && s == 0 && wv == 0 && e == 0) {
               if (c > 0 && (n == 0 || s == 0 || wv == 0 || e == 0)) {
                  if (this.nextInt(5) == 0) {
                     if (c == GenBiomes.ICE_PLAINS) {
                        out[dx + dz * w] = GenBiomes.FROZEN_OCEAN;
                     } else {
                        out[dx + dz * w] = 0;
                     }
                  } else {
                     out[dx + dz * w] = c;
                  }
               } else {
                  out[dx + dz * w] = c;
               }
            } else {
               int k = 1;
               int v = 1;
               if (n != 0 && this.nextInt(k++) == 0) {
                  v = n;
               }
               if (s != 0 && this.nextInt(k++) == 0) {
                  v = s;
               }
               if (wv != 0 && this.nextInt(k++) == 0) {
                  v = wv;
               }
               if (e != 0 && this.nextInt(k++) == 0) {
                  v = e;
               }
               if (this.nextInt(3) == 0) {
                  out[dx + dz * w] = v;
               } else if (v == GenBiomes.ICE_PLAINS) {
                  out[dx + dz * w] = GenBiomes.FROZEN_OCEAN;
               } else {
                  out[dx + dz * w] = 0;
               }
            }
         }
      }
      return out;
   }
}

