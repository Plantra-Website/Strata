package com.strata.world.gen;

public class GenSnow extends GenLayer {
   public GenSnow(long seed, GenLayer parent) {
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
            int c = p[dx + 1 + (dz + 1) * stride];
            this.initCellSeed(dx + x, dz + z);
            if (c == 0) {
               out[dx + dz * w] = 0;
            } else {
               out[dx + dz * w] = this.nextInt(5) == 0 ? GenBiomes.ICE_PLAINS : 1;
            }
         }
      }
      return out;
   }
}

