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

   static int rootRoll(int ox, int oz, int salt, int bound) {
      long h = (long)ox * 0x8DA6B343L + (long)oz * 0xD8163841L + (long)salt * 0x9E3779B1L;
      h ^= h >>> 13;
      h *= 1274126177L;
      h ^= h >>> 16;
      return (int)((h & 0x7fffffffL) % bound);
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
         if (adx > 1 || adz > 1) {
            return false;
         }
         return !(adx == 1 && adz == 1);
      }
      return false;
   }

   public static boolean swamp(int dx, int dy, int dz, int x, int z) {
      int adx = dx < 0 ? -dx : dx;
      int adz = dz < 0 ? -dz : dz;
      if (dy == 1) {
         if (adx > 2 || adz > 2) {
            return false;
         }
         return !(adx == 2 && adz == 2);
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
      if (dy == -1 || dy == -2) {
         if (adx > 3 || adz > 3) {
            return false;
         }
         if (adx == 3 && adz == 3) {
            return cornerKept(x, z);
         }
         return true;
      }
      return false;
   }

   static int taiga1Radius(int i, int depth, int maxR) {
      int r = 0;
      for (int l = 0; l < i; l++) {
         if (r >= 1 && l == depth - 1) {
            r--;
         } else if (r < maxR) {
            r++;
         }
      }
      return r;
   }

   static int taiga2Radius(int i, int r0, int maxR) {
      int r = r0;
      int lvl = 1;
      int alt = 0;
      for (int l = 0; l < i; l++) {
         if (r >= lvl) {
            r = alt;
            alt = 1;
            if (lvl < maxR) {
               lvl++;
            }
         } else {
            r++;
         }
      }
      return r;
   }

   public static boolean spruce(int dx, int dy, int dz, int th, boolean tall, int x, int z) {
      int adx = dx < 0 ? -dx : dx;
      int adz = dz < 0 ? -dz : dz;
      int ox = x - dx;
      int oz = z - dz;
      if (tall) {
         int depth = 3 + rootRoll(ox, oz, 1, 2);
         if (dy > 1 || dy < 1 - depth) {
            return false;
         }
         int i = 1 - dy;
         int maxR = 1 + rootRoll(ox, oz, 2, depth + 1);
         int r = taiga1Radius(i, depth, maxR);
         if (adx > r || adz > r) {
            return false;
         }
         return !(adx == r && adz == r && r > 0);
      }
      int r0 = rootRoll(ox, oz, 3, 2);
      int maxR = 2 + rootRoll(ox, oz, 4, 2);
      int var7 = 1 + rootRoll(ox, oz, 5, 2);
      int var8 = th - var7;
      if (dy > 1 || dy < 1 - var8) {
         return false;
      }
      int i = 1 - dy;
      int r = taiga2Radius(i, r0, maxR);
      if (adx > r || adz > r) {
         return false;
      }
      return !(adx == r && adz == r && r > 0);
   }
}
