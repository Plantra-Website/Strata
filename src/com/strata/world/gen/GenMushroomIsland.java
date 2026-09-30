package com.strata.world.gen;

public class GenMushroomIsland extends GenLayer {
   public GenMushroomIsland(long seed, GenLayer parent) {
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
            if (c == 0 && n == 0 && s == 0 && wv == 0 && e == 0 && this.nextInt(100) == 0) {
               out[dx + dz * w] = GenBiomes.MUSHROOM_ISLAND;
            } else {
               out[dx + dz * w] = c;
            }
         }
      }
      return out;
   }
}

