package com.strata.world.gen;

public class LayerIsland extends GenLayer {
   public LayerIsland(long seed) {
      super(seed);
   }

   @Override
   public int[] generate(int x, int z, int w, int h) {
      int[] out = new int[w * h];
      for (int dz = 0; dz < h; dz++) {
         for (int dx = 0; dx < w; dx++) {
            this.initCellSeed(x + dx, z + dz);
            out[dx + dz * w] = this.nextInt(10) == 0 ? 1 : 0;
         }
      }
      if (x > -w && x <= 0 && z > -h && z <= 0) {
         out[-x + -z * w] = 1;
      }
      return out;
   }
}

