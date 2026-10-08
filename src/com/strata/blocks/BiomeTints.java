package com.strata.blocks;

public class BiomeTints {
   public static final int NONE = 0;
   public static final int GRASS = 1;
   public static final int FOLIAGE = 2;
   public static final int BIRCH = 3;
   public static final int PINE = 4;

   public static final int DEFAULT_BIOME = 1;

   private static final int[][] GRASS_TABLE = {
      {145, 189, 89},   
      {145, 189, 89},   
      {191, 183, 85},   
      {138, 182, 90},   
      {121, 194, 90},   
      {125, 176, 105},  
      {106, 112, 57},   
      {145, 189, 89},   
      {145, 189, 89},   
      {145, 189, 89},   
      {160, 200, 144},  
      {160, 200, 144},  
      {160, 200, 144},  
      {150, 190, 130},  
      {145, 189, 89},   
      {145, 189, 89},   
   };

   private static final int[][] FOLIAGE_TABLE = {
      {119, 171, 47},   
      {119, 171, 47},   
      {170, 170, 80},   
      {110, 165, 55},   
      {77, 186, 32},    
      {89, 150, 60},    
      {73, 102, 35},    
      {119, 171, 47},   
      {119, 171, 47},   
      {119, 171, 47},   
      {140, 180, 120},  
      {140, 180, 120},  
      {140, 180, 120},  
      {130, 170, 110},  
      {119, 171, 47},   
      {119, 171, 47},   
   };

   private static final float[][] FIXED = new float[5][];

   static {
      FIXED[NONE] = new float[]{1.0F, 1.0F, 1.0F};
      FIXED[BIRCH] = new float[]{128 / 255.0F, 167 / 255.0F, 85 / 255.0F};
      FIXED[PINE] = new float[]{97 / 255.0F, 153 / 255.0F, 97 / 255.0F};
   }

   private static final float[][][] CACHE = new float[5][16][];

   static {
      for (int b = 0; b < 16; b++) {
         CACHE[GRASS][b] = new float[]{
            GRASS_TABLE[b][0] / 255.0F, GRASS_TABLE[b][1] / 255.0F, GRASS_TABLE[b][2] / 255.0F};
         CACHE[FOLIAGE][b] = new float[]{
            FOLIAGE_TABLE[b][0] / 255.0F, FOLIAGE_TABLE[b][1] / 255.0F, FOLIAGE_TABLE[b][2] / 255.0F};
      }
   }

   public static float[] colorFor(int kind, int biome) {
      if (kind == GRASS || kind == FOLIAGE) {
         if (biome < 0 || biome >= 16) {
            biome = DEFAULT_BIOME;
         }
         return CACHE[kind][biome];
      }
      if (kind < 0 || kind >= FIXED.length || FIXED[kind] == null) {
         return FIXED[NONE];
      }
      return FIXED[kind];
   }
}

