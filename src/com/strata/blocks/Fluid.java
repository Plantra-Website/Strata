package com.strata.blocks;

public final class Fluid {
   private Fluid() {
   }

   public static final class Spec {
      public final int step;
      public final int range;
      public final int ticks;

      Spec(int step, int range, int ticks) {
         this.step = step;
         this.range = range;
         this.ticks = ticks;
      }
   }

   public static final Spec WATER = new Spec(1, 7, 15);
   public static final Spec LAVA = new Spec(2, 6, 90);

   public static Spec of(int id) {
      if (id == Blocks.WATER_ID) {
         return WATER;
      }
      if (id == Blocks.LAVA_ID) {
         return LAVA;
      }
      throw new RuntimeException("not a fluid id: " + id);
   }
}

