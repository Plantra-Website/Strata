package com.strata.client;

public class FontTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static void checkF(float got, float want, String msg) {
      if (Math.abs(got - want) > 1e-6F) { failures++; System.out.println("FAIL: " + msg + " got=" + got); }
   }

   public static void main(String[] args) {
      float[] zero = Gui.asciiUV('0');
      checkF(zero[0], 0.0F, "zero u0");
      checkF(zero[1], 24.0F / 128.0F, "zero v0");
      checkF(zero[2], 8.0F / 128.0F, "zero u1");
      checkF(zero[3], 32.0F / 128.0F, "zero v1");
      float[] a = Gui.asciiUV('A');
      checkF(a[0], 8.0F / 128.0F, "A u0");
      checkF(a[1], 32.0F / 128.0F, "A v0");
      check(Gui.drawStringWidth("fps 60") == 36, "width 6px/char");
      check(Gui.drawStringWidth("") == 0, "empty width");
      check(Gui.drawStringWidth(null) == 0, "null width");

      if (failures == 0) System.out.println("FONT PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

