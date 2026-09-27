package com.strata.core;

public final class MathHelper {
   private MathHelper() {
   }

   public static int floor(double v) {
      int i = (int)v;
      return v < i ? i - 1 : i;
   }

   public static int floor(float v) {
      int i = (int)v;
      return v < i ? i - 1 : i;
   }

   public static int clamp(int v, int min, int max) {
      return v < min ? min : (v > max ? max : v);
   }

   public static float clamp(float v, float min, float max) {
      return v < min ? min : (v > max ? max : v);
   }

   public static double clamp(double v, double min, double max) {
      return v < min ? min : (v > max ? max : v);
   }

   public static float lerp(float a, float b, float t) {
      return a + (b - a) * t;
   }

   public static double lerp(double a, double b, double t) {
      return a + (b - a) * t;
   }
}

