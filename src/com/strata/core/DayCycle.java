package com.strata.core;

public final class DayCycle {
   private DayCycle() {
   }

   public static float amount(long time, long dayLength) {
      double sun = Math.sin(time * Math.PI * 2.0 / dayLength);
      double t = (sun + 0.08) / 0.28;
      if (t < 0.0) {
         t = 0.0;
      }
      if (t > 1.0) {
         t = 1.0;
      }
      return (float)(t * t * (3.0 - 2.0 * t));
   }

   public static int subFor(float dayAmount) {
      float d = dayAmount < 0.0F ? 0.0F : (dayAmount > 1.0F ? 1.0F : dayAmount);
      return Math.round(11.0F * (1.0F - d));
   }

   public static final String[] STOP_NAMES = {"dawn", "noon", "dusk", "midnight"};
   public static final double[] STOP_FRACS = {0.0, 0.25, 0.5, 0.75};

   public static long nextStop(long time, long dayLength) {
      long t = ((time % dayLength) + dayLength) % dayLength;
      for (double f : STOP_FRACS) {
         long s = (long)(f * dayLength);
         if (s > t) {
            return time - t + s;
         }
      }
      return time - t + dayLength;
   }

   public static String stopName(long stopTime, long dayLength) {
      long t = ((stopTime % dayLength) + dayLength) % dayLength;
      for (int i = 0; i < STOP_FRACS.length; i++) {
         if ((long)(STOP_FRACS[i] * dayLength) == t) {
            return STOP_NAMES[i];
         }
      }
      return "day+" + t;
   }
}

