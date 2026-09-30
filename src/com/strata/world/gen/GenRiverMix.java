package com.strata.world.gen;

public class GenRiverMix extends GenLayer {
   private final GenLayer riverParent;

   public GenRiverMix(long seed, GenLayer biomeParent, GenLayer riverParent) {
      super(seed);
      this.parent = biomeParent;
      this.riverParent = riverParent;
   }

   @Override
   public void initSeed(long seed) {
      this.riverParent.initSeed(seed);
      super.initSeed(seed);
   }

   @Override
   public int[] generate(int x, int z, int w, int h) {
      int[] biomes = this.parent.generate(x, z, w, h);
      int[] rivers = this.riverParent.generate(x, z, w, h);
      int[] out = new int[w * h];
      for (int i = 0; i < w * h; i++) {
         if (biomes[i] == GenBiomes.OCEAN) {
            out[i] = biomes[i];
         } else if (rivers[i] >= 0) {
            if (biomes[i] == GenBiomes.ICE_PLAINS) {
               out[i] = GenBiomes.FROZEN_RIVER;
            } else if (biomes[i] != GenBiomes.MUSHROOM_ISLAND
               && biomes[i] != GenBiomes.MUSHROOM_SHORE) {
               out[i] = rivers[i];
            } else {
               out[i] = GenBiomes.MUSHROOM_SHORE;
            }
         } else {
            out[i] = biomes[i];
         }
      }
      return out;
   }
}

