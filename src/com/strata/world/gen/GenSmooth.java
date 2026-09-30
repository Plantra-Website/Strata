package com.strata.world.gen;

public class GenSmooth extends GenLayer {
   public GenSmooth(long seed, GenLayer parent) {
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
            int n = p[dx + 0 + (dz + 1) * stride];
            int s = p[dx + 2 + (dz + 1) * stride];
            int wv = p[dx + 1 + (dz + 0) * stride];
            int e = p[dx + 1 + (dz + 2) * stride];
            int c = p[dx + 1 + (dz + 1) * stride];
            if (n == s && wv == e) {
               this.initCellSeed(dx + x, dz + z);
               c = this.nextInt(2) == 0 ? n : wv;
            } else {
               if (n == s) {
                  c = n;
               }
               if (wv == e) {
                  c = wv;
               }
            }
            out[dx + dz * w] = c;
         }
      }
      return out;
   }
}

