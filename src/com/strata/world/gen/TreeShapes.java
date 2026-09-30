package com.strata.world.gen;

public final class TreeShapes {
   private TreeShapes() {
   }

   static boolean cornerKept(int x, int z) {
      long h = (long)x * 0x8DA6B343L + (long)z * 0xD8163841L;
      h ^= h >>> 13;
      h *= 1274126177L;
      h ^= h >>> 16;
      return (h & 1L) == 0L;
   }

   public static boolean round(int dx, int dy, int dz, int x, int z) {
      int adx = dx < 0 ? -dx : dx;
      int adz = dz < 0 ? -dz : dz;
      if (dy == -3 || dy == -2) {
         if (adx > 2 || adz > 2) {
            return false;
         }
         if (adx == 2 && adz == 2) {
            return cornerKept(x, z);
         }
         return true;
      }
      if (dy == -1) {
         if (adx > 1 || adz > 1) {
            return false;
         }
         if (adx == 1 && adz == 1) {
            return cornerKept(x, z);
         }
         return true;
      }
      if (dy == 0) {
         return adx <= 1 && adz <= 1;
      }
      return false;
   }

   public static boolean swamp(int dx, int dy, int dz) {
      int adx = dx < 0 ? -dx : dx;
      int adz = dz < 0 ? -dz : dz;
      if (dy == -3 || dy == -2) {
         return adx <= 3 && adz <= 3;
      }
      if (dy == -1 || dy == 0) {
         return adx <= 2 && adz <= 2;
      }
      return false;
   }

   public static boolean spruce(int dx, int dy, int dz) {
      int adx = dx < 0 ? -dx : dx;
      int adz = dz < 0 ? -dz : dz;
      if (dy == -3 || dy == -2) {
         return adx <= 2 && adz <= 2 && !(adx == 2 && adz == 2);
      }
      if (dy == -1 || dy == 0) {
         return adx <= 1 && adz <= 1;
      }
      if (dy == 1) {
         return adx + adz <= 1;
      }
      return false;
   }
}

