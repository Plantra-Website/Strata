package com.strata.blocks;

public class CrossQuadTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static void checkF(float got, float want, String msg) {
      if (Math.abs(got - want) > 1e-2) { failures++; System.out.println("FAIL: " + msg + " got=" + got + " want=" + want); }
   }

   static class Air implements BlockView {
      @Override public boolean isSolidTile(int x, int y, int z) { return false; }
      @Override public float getBrightness(int x, int y, int z) { return 1.0F; }
   }

   static float dist(float[] v, int a, int b) {
      float dx = v[a * 3] - v[b * 3];
      float dy = v[a * 3 + 1] - v[b * 3 + 1];
      float dz = v[a * 3 + 2] - v[b * 3 + 2];
      return (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   public static void main(String[] args) {
      MeshBuilder b = new MeshBuilder();
      b.init();
      Blocks.byId(Blocks.ROSE_ID).render(b, new Air(), 0, 0, 40, 0);
      check(b.count() == 16, "two diagonals x two windings (" + b.count() + " verts)");
      float[] v = b.vertices();
      checkF(dist(v, 0, 1), 1.2728F, "diagonal bottom edge (vanilla 0.9 footprint)");
      checkF(dist(v, 1, 2), 1.0F, "vertical edge unit");
      checkF(dist(v, 2, 3), 1.2728F, "diagonal top edge (vanilla 0.9 footprint)");
      checkF(dist(v, 8, 9), 1.2728F, "second diagonal (vanilla 0.9 footprint)");
      if (failures == 0) System.out.println("CROSS PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

