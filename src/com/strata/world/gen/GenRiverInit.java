package com.strata.world.gen;

public class GenRiverInit extends GenLayer {
   public GenRiverInit(long seed, GenLayer parent) {
      super(seed);
      this.parent = parent;
   }

   @Override
   public int[] generate(int x, int z, int w, int h) {
      int[] p = this.parent.generate(x, z, w, h);
      int[] out = new int[w * h];
      for (int i = 0; i < w * h; i++) {
         this.initCellSeed(x + i % w, z + i / w);
         out[i] = p[i] > 0 ? this.nextInt(2) + 2 : 0;
      }
      return out;
   }
}

