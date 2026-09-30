package com.strata.world.gen;

import java.util.Random;

public class NoiseParityTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) {
      VanillaOctaves g = new VanillaOctaves(new Random(7777L), 8);
      double[] r = g.generate(null, 12, 5, -30, 5, 17, 5, 8.55515, 4.277575, 8.55515);
      check(r.length == 5 * 17 * 5, "3D shape");
      check(Double.toString(r[0]).equals("-14.445925508654266"), "3D r[0] (got " + r[0] + ")");
      check(Double.toString(r[100]).equals("51.56741539640078"), "3D r[100] (got " + r[100] + ")");
      check(Double.toString(r[424]).equals("94.62454726223554"), "3D r[424] (got " + r[424] + ")");
      VanillaOctaves h = new VanillaOctaves(new Random(1234L), 10);
      double[] s = h.generate2D(null, -45, 67, 5, 5, 1.121D, 1.121D);
      check(s.length == 25, "2D shape");
      check(Double.toString(s[0]).equals("60.19342653390665"), "2D s[0] (got " + s[0] + ")");
      check(Double.toString(s[24]).equals("60.16485670361819"), "2D s[24] (got " + s[24] + ")");
      VanillaOctaves g2 = new VanillaOctaves(new Random(7777L), 8);
      double[] r2 = g2.generate(null, 12, 5, -30, 5, 17, 5, 8.55515, 4.277575, 8.55515);
      check(r2[100] == r[100], "deterministic");

      if (failures == 0) System.out.println("NOISEPARITY PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

