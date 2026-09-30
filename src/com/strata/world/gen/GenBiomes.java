package com.strata.world.gen;

public class GenBiomes {
   public static final int OCEAN = 0;
   public static final int PLAINS = 1;
   public static final int DESERT = 2;
   public static final int HILLS = 3;
   public static final int FOREST = 4;
   public static final int TAIGA = 5;
   public static final int SWAMPLAND = 6;
   public static final int RIVER = 7;
   public static final int HELL = 8;
   public static final int SKY = 9;
   public static final int FROZEN_OCEAN = 10;
   public static final int FROZEN_RIVER = 11;
   public static final int ICE_PLAINS = 12;
   public static final int ICE_MOUNTAINS = 13;
   public static final int MUSHROOM_ISLAND = 14;
   public static final int MUSHROOM_SHORE = 15;

   private static final float[] MIN = new float[16];
   private static final float[] MAX = new float[16];
   private static final int[] TEMP = new int[16];
   private static final int[] RAIN = new int[16];

   static {
      def(OCEAN, -1.0F, 0.4F, 0.5F, 0.5F);
      def(PLAINS, 0.1F, 0.3F, 0.8F, 0.4F);
      def(DESERT, 0.1F, 0.2F, 2.0F, 0.0F);
      def(HILLS, 0.2F, 1.8F, 0.2F, 0.3F);
      def(FOREST, 0.1F, 0.3F, 0.7F, 0.8F);
      def(TAIGA, 0.1F, 0.4F, 0.3F, 0.8F);
      def(SWAMPLAND, -0.2F, 0.1F, 0.8F, 0.9F);
      def(RIVER, -0.5F, 0.0F, 0.5F, 0.5F);
      def(HELL, 0.1F, 0.3F, 2.0F, 0.0F);
      def(SKY, 0.1F, 0.3F, 0.5F, 0.5F);
      def(FROZEN_OCEAN, -1.0F, 0.5F, 0.0F, 0.5F);
      def(FROZEN_RIVER, -0.5F, 0.0F, 0.0F, 0.5F);
      def(ICE_PLAINS, 0.1F, 0.3F, 0.0F, 0.5F);
      def(ICE_MOUNTAINS, 0.2F, 1.8F, 0.0F, 0.5F);
      def(MUSHROOM_ISLAND, 0.2F, 1.0F, 0.9F, 1.0F);
      def(MUSHROOM_SHORE, -1.0F, 0.1F, 0.9F, 1.0F);
   }

   private static void def(int id, float min, float max, float temp, float rain) {
      MIN[id] = min;
      MAX[id] = max;
      TEMP[id] = (int)(temp * 65536.0F);
      RAIN[id] = (int)(rain * 65536.0F);
   }

   public static float minHeight(int id) {
      return (id < 0 || id >= 16) ? 0.1F : MIN[id];
   }

   public static float maxHeight(int id) {
      return (id < 0 || id >= 16) ? 0.3F : MAX[id];
   }

   public static int tempInt(int id) {
      return (id < 0 || id >= 16) ? (int)(0.5F * 65536.0F) : TEMP[id];
   }

   public static int rainInt(int id) {
      return (id < 0 || id >= 16) ? (int)(0.5F * 65536.0F) : RAIN[id];
   }

   public static boolean snowy(int id) {
      return tempInt(id) / 65536.0F <= 0.15F;
   }

   public static boolean noRain(int id) {
      return id == DESERT || id == HELL || id == SKY;
   }
}

