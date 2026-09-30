package com.strata.world.gen;

public class GenShore extends GenLayer {
   public GenShore(long seed, GenLayer parent) {
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
            this.initCellSeed(dx + x, dz + z);
            int c = p[dx + 1 + (dz + 1) * stride];
            if (c == GenBiomes.MUSHROOM_ISLAND) {
               int n = p[dx + 1 + (dz + 1 - 1) * stride];
               int s = p[dx + 1 + 1 + (dz + 1) * stride];
               int wv = p[dx + 1 - 1 + (dz + 1) * stride];
               int e = p[dx + 1 + (dz + 1 + 1) * stride];
               if (n != GenBiomes.OCEAN && s != GenBiomes.OCEAN
                  && wv != GenBiomes.OCEAN && e != GenBiomes.OCEAN) {
                  out[dx + dz * w] = c;
               } else {
                  out[dx + dz * w] = GenBiomes.MUSHROOM_SHORE;
               }
            } else {
               out[dx + dz * w] = c;
            }
         }
      }
      return out;
   }
}

