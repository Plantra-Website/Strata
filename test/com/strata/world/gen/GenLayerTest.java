package com.strata.world.gen;

public class GenLayerTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) {
      LayerIsland base = new LayerIsland(1L);
      base.initSeed(7777L);
      int[] cells = base.generate(0, 0, 4, 4);
      int[] want = {1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0};
      check(java.util.Arrays.equals(cells, want), "island seed values");

      GenLayer[] stack = GenLayer.buildStack(7777L);
      int[] coarse = stack[0].generate(0, 0, 16, 16);
      for (int v : coarse) {
         check(v >= -1 && v <= 15, "coarse id in range (got " + v + ")");
      }
      GenLayer[] again = GenLayer.buildStack(7777L);
      check(java.util.Arrays.equals(coarse, again[0].generate(0, 0, 16, 16)), "stack deterministic");
      GenLayer[] other = GenLayer.buildStack(1234L);
      check(!java.util.Arrays.equals(coarse, other[0].generate(0, 0, 16, 16)), "stack seed-sensitive");

      int[] fine = stack[1].generate(0, 0, 16, 16);
      for (int v : fine) {
         check(v >= -1 && v <= 15, "voronoi id in range (got " + v + ")");
      }

      if (failures == 0) System.out.println("GENLAYER PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

