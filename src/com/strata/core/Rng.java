package com.strata.core;

import java.util.Random;

public final class Rng {
   private static Random world = new Random();

   private Rng() {
   }

   public static void reseed(long seed) {
      world = new Random(seed);
   }

   public static Random world() {
      return world;
   }

   public static float range(float min, float max) {
      return min + world.nextFloat() * (max - min);
   }

   public static int range(int min, int max) {
      return min + world.nextInt(max - min + 1);
   }
}

