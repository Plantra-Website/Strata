package com.strata.client;

public class IsoTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static void checkF(float got, float want, String msg) {
      if (Math.abs(got - want) > 1e-2) { failures++; System.out.println("FAIL: " + msg + " got=" + got + " want=" + want); }
   }

   static float shoelace(float[][] q) {
      float sum = 0;
      for (int i = 0; i < 4; i++) {
         float[] a = q[i];
         float[] b = q[(i + 1) % 4];
         sum += a[0] * b[1] - b[0] * a[1];
      }
      return sum;
   }

   static float[] scr(float x, float y, float z) {
      float[] p = Gui.isoProj(x, y, z);
      return new float[]{8 + 9 * p[0], 8.72F + 9 * p[1]};
   }

   public static void main(String[] args) {
      float minX = 99, maxX = -99, minY = 99, maxY = -99;
      for (int x = 0; x <= 1; x++) {
         for (int y = 0; y <= 1; y++) {
            for (int z = 0; z <= 1; z++) {
               float[] p = Gui.isoProj(x, y, z);
               minX = Math.min(minX, p[0]);
               maxX = Math.max(maxX, p[0]);
               minY = Math.min(minY, p[1]);
               maxY = Math.max(maxY, p[1]);
            }
         }
      }
      checkF((maxX - minX) * 10, 14.142F, "footprint width");
      checkF((maxY - minY) * 10, 15.731F, "footprint height");

      float[] top = scr(0, 1, 0);
      checkF(top[0], 8.0F, "top vertex x centered");
      checkF(top[1], 0.93F, "top vertex clears the slot top");
      float[] bottom = scr(1, 0, 1);
      checkF(bottom[1], 15.08F, "bottom vertex clears the slot bottom");

      float[][] topQ = {scr(1, 1, 1), scr(1, 1, 0), scr(0, 1, 0), scr(0, 1, 1)};
      float[][] eastQ = {scr(1, 0, 1), scr(1, 0, 0), scr(1, 1, 0), scr(1, 1, 1)};
      float[][] southQ = {scr(0, 1, 1), scr(0, 0, 1), scr(1, 0, 1), scr(1, 1, 1)};
      check(shoelace(topQ) < 0, "top winds with the sprite");
      check(shoelace(eastQ) < 0, "east winds with the sprite");
      check(shoelace(southQ) < 0, "south winds with the sprite");

      if (failures == 0) System.out.println("ISO PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

