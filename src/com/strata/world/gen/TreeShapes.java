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
      if (dy == -2 || dy == -1) {
         if (adx > 2 || adz > 2) {
            return false;
         }
         if (adx == 2 && adz == 2) {
            return cornerKept(x, z);
         }
         return true;
      }
      if (dy == 0) {
         if (adx > 1 || adz > 1) {
            return false;
         }
         if (adx == 1 && adz == 1) {
            return cornerKept(x, z);
         }
         return true;
      }
      if (dy == 1) {
         return adx <= 1 && adz <= 1;
      }
      return false;
   }

   public static boolean swamp(int dx, int dy, int dz, int x, int z) {
      int adx = dx < 0 ? -dx : dx;
      int adz = dz < 0 ? -dz : dz;
      if (dy == -2 || dy == -1) {
         if (adx > 3 || adz > 3) {
            return false;
         }
         if (adx == 3 && adz == 3) {
            return cornerKept(x, z);
         }
         return true;
      }
      if (dy == 0) {
         if (adx > 2 || adz > 2) {
            return false;
         }
         if (adx == 2 && adz == 2) {
            return cornerKept(x, z);
         }
         return true;
      }
      if (dy == 1) {
         return adx <= 2 && adz <= 2;
      }
      return false;
   }

   public static boolean spruce(int dx, int dy, int dz, int th, boolean tall, int x, int z) {
      int adx = dx < 0 ? -dx : dx;
      int adz = dz < 0 ? -dz : dz;
      if (tall) {
         if (dy == 1) {
            return adx == 0 && adz == 0;
         }
         if (dy == 0 || dy == -3) {
            return adx + adz <= 1;
         }
         if (dy == -1 || dy == -2) {
            if (adx > 2 || adz > 2) {
               return false;
            }
            if (adx == 2 && adz == 2) {
               return cornerKept(x, z);
            }
            return true;
         }
         return false;
      }
      boolean phase = cornerKept(x, z);
      if (dy == 1) {
         return phase ? adx + adz <= 1 : adx == 0 && adz == 0;
      }
      if (dy == 0) {
         return phase ? adx == 0 && adz == 0 : adx + adz <= 1;
      }
      if (dy == -1) {
         return phase ? adx + adz <= 1 : adx == 0 && adz == 0;
      }
      if (dy <= -2 && dy >= 2 - th) {
         boolean wide = ((dy & 1) == 0) == phase;
         if (wide) {
            if (adx > 2 || adz > 2) {
               return false;
            }
            if (adx == 2 && adz == 2) {
               return cornerKept(x, z);
            }
            return true;
         }
         return adx + adz <= 1;
      }
      return false;
   }
}

