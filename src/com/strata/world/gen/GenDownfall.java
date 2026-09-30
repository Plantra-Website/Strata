package com.strata.world.gen;

public class GenDownfall extends GenLayer {
   public GenDownfall(GenLayer parent) {
      super(0L);
      this.parent = parent;
   }

   @Override
   public int[] generate(int x, int z, int w, int h) {
      int[] p = this.parent.generate(x, z, w, h);
      int[] out = new int[w * h];
      for (int i = 0; i < w * h; i++) {
         out[i] = GenBiomes.rainInt(p[i]);
      }
      return out;
   }
}

