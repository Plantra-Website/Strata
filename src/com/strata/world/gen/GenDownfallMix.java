package com.strata.world.gen;

public class GenDownfallMix extends GenLayer {
   private final GenLayer smoothParent;
   private final int loop;

   public GenDownfallMix(GenLayer smoothParent, GenLayer biomeParent, int loop) {
      super(0L);
      this.smoothParent = smoothParent;
      this.parent = biomeParent;
      this.loop = loop;
   }

   @Override
   public void initSeed(long seed) {
      this.smoothParent.initSeed(seed);
      super.initSeed(seed);
   }

   @Override
   public int[] generate(int x, int z, int w, int h) {
      int[] biomes = this.parent.generate(x, z, w, h);
      int[] smooth = this.smoothParent.generate(x, z, w, h);
      int[] out = new int[w * h];
      for (int i = 0; i < w * h; i++) {
         out[i] = smooth[i] + (GenBiomes.rainInt(biomes[i]) - smooth[i]) / (this.loop + 1);
      }
      return out;
   }
}

