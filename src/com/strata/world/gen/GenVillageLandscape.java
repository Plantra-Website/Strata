package com.strata.world.gen;

public class GenVillageLandscape extends GenLayer {
   private static final int[] ALLOWED = {
      GenBiomes.DESERT, GenBiomes.FOREST, GenBiomes.HILLS,
      GenBiomes.SWAMPLAND, GenBiomes.PLAINS, GenBiomes.TAIGA};

   public GenVillageLandscape(long seed, GenLayer parent) {
      super(seed);
      this.parent = parent;
   }

   @Override
   public int[] generate(int x, int z, int w, int h) {
      int[] p = this.parent.generate(x, z, w, h);
      int[] out = new int[w * h];
      for (int dz = 0; dz < h; dz++) {
         for (int dx = 0; dx < w; dx++) {
            this.initCellSeed(dx + x, dz + z);
            int c = p[dx + dz * w];
            if (c == 0) {
               out[dx + dz * w] = 0;
            } else if (c == GenBiomes.MUSHROOM_ISLAND) {
               out[dx + dz * w] = c;
            } else if (c == 1) {
               out[dx + dz * w] = ALLOWED[this.nextInt(ALLOWED.length)];
            } else {
               out[dx + dz * w] = GenBiomes.ICE_PLAINS;
            }
         }
      }
      return out;
   }
}

