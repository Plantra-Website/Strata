package com.strata.world.gen;

public abstract class GenLayer {
   private long worldSeed;
   protected GenLayer parent;
   private long chunkSeed;
   private long baseSeed;

   public static GenLayer[] buildStack(long seed) {
      LayerIsland island0 = new LayerIsland(1L);
      GenZoomFuzzy fz = new GenZoomFuzzy(2000L, island0);
      GenIsland isl1 = new GenIsland(1L, fz);
      GenZoom z1 = new GenZoom(2001L, isl1);
      isl1 = new GenIsland(2L, z1);
      GenSnow snow = new GenSnow(2L, isl1);
      z1 = new GenZoom(2002L, snow);
      isl1 = new GenIsland(3L, z1);
      z1 = new GenZoom(2003L, isl1);
      isl1 = new GenIsland(4L, z1);
      GenMushroomIsland mush = new GenMushroomIsland(5L, isl1);
      GenLayer riverBase = GenZoom.stack(1000L, mush, 0);
      GenRiverInit riverInit = new GenRiverInit(100L, riverBase);
      riverBase = GenZoom.stack(1000L, riverInit, 6);
      GenRiver river = new GenRiver(1L, riverBase);
      GenSmooth riverSmooth = new GenSmooth(1000L, river);
      GenLayer biomeBase = GenZoom.stack(1000L, mush, 0);
      GenVillageLandscape assigned = new GenVillageLandscape(200L, biomeBase);
      GenLayer biomes = GenZoom.stack(1000L, assigned, 2);
      GenLayer temp = new GenTemperature(biomes);
      GenLayer rain = new GenDownfall(biomes);
      for (int i = 0; i < 4; i++) {
         biomes = new GenZoom(1000L + i, biomes);
         if (i == 0) {
            biomes = new GenIsland(3L, biomes);
         }
         if (i == 0) {
            biomes = new GenShore(1000L, biomes);
         }
         GenSmoothZoom tempZoom = new GenSmoothZoom(1000L + i, temp);
         temp = new GenTemperatureMix(tempZoom, biomes, i);
         GenSmoothZoom rainZoom = new GenSmoothZoom(1000L + i, rain);
         rain = new GenDownfallMix(rainZoom, biomes, i);
      }
      GenSmooth biomeSmooth = new GenSmooth(1000L, biomes);
      GenRiverMix mixed = new GenRiverMix(100L, biomeSmooth, riverSmooth);
      GenLayer tempOut = GenSmoothZoom.stack(1000L, temp, 2);
      GenLayer rainOut = GenSmoothZoom.stack(1000L, rain, 2);
      GenZoomVoronoi voronoi = new GenZoomVoronoi(10L, mixed);
      mixed.initSeed(seed);
      voronoi.initSeed(seed);
      tempOut.initSeed(seed);
      rainOut.initSeed(seed);
      return new GenLayer[]{mixed, voronoi, tempOut, rainOut, mixed};
   }

   public GenLayer(long seed) {
      this.baseSeed = seed;
      this.baseSeed *= this.baseSeed * 6364136223846793005L + 1442695040888963407L;
      this.baseSeed += seed;
      this.baseSeed *= this.baseSeed * 6364136223846793005L + 1442695040888963407L;
      this.baseSeed += seed;
      this.baseSeed *= this.baseSeed * 6364136223846793005L + 1442695040888963407L;
      this.baseSeed += seed;
   }

   public void initSeed(long seed) {
      this.worldSeed = seed;
      if (this.parent != null) {
         this.parent.initSeed(seed);
      }
      this.worldSeed *= this.worldSeed * 6364136223846793005L + 1442695040888963407L;
      this.worldSeed += this.baseSeed;
      this.worldSeed *= this.worldSeed * 6364136223846793005L + 1442695040888963407L;
      this.worldSeed += this.baseSeed;
      this.worldSeed *= this.worldSeed * 6364136223846793005L + 1442695040888963407L;
      this.worldSeed += this.baseSeed;
   }

   public void initCellSeed(long x, long z) {
      this.chunkSeed = this.worldSeed;
      this.chunkSeed *= this.chunkSeed * 6364136223846793005L + 1442695040888963407L;
      this.chunkSeed += x;
      this.chunkSeed *= this.chunkSeed * 6364136223846793005L + 1442695040888963407L;
      this.chunkSeed += z;
      this.chunkSeed *= this.chunkSeed * 6364136223846793005L + 1442695040888963407L;
      this.chunkSeed += x;
      this.chunkSeed *= this.chunkSeed * 6364136223846793005L + 1442695040888963407L;
      this.chunkSeed += z;
   }

   protected int nextInt(int n) {
      int r = (int)((this.chunkSeed >> 24) % (long)n);
      if (r < 0) {
         r += n;
      }
      this.chunkSeed *= this.chunkSeed * 6364136223846793005L + 1442695040888963407L;
      this.chunkSeed += this.worldSeed;
      return r;
   }

   public abstract int[] generate(int x, int z, int w, int h);
}

