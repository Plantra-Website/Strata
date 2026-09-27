package com.strata.blocks;

public class BlockCrackTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static void checkF(float got, float want, String msg) {
      if (Math.abs(got - want) > 1e-4) { failures++; System.out.println("FAIL: " + msg + " got=" + got + " want=" + want); }
   }

   static class Dim implements BlockView {
      @Override public boolean isSolidTile(int x, int y, int z) { return false; }
      @Override public float getBrightness(int x, int y, int z) { return 0.25F; }
   }

   public static void main(String[] args) {
      AtlasStitcher.stitch(); 
      float[] p = {0, 0, 0, 10, 0, 0, 10, 10, 0, 0, 10, 0};
      float[] c00 = Block.lerp2(p, 0, 0);
      float[] c10 = Block.lerp2(p, 1, 0);
      float[] c11 = Block.lerp2(p, 1, 1);
      float[] c01 = Block.lerp2(p, 0, 1);
      checkF(c00[0], 0.0F, "lerp origin");
      checkF(c10[0], 10.0F, "lerp u edge");
      checkF(c11[0], 10.0F, "lerp (1,1) x is C");
      checkF(c11[1], 10.0F, "lerp (1,1) y is C (not D)");
      checkF(c01[0], 0.0F, "lerp (0,1) x is D");
      checkF(c01[1], 10.0F, "lerp (0,1) y is D");
      float[] mid = Block.lerp2(p, 0.5F, 0.5F);
      checkF(mid[0], 5.0F, "lerp center x");
      checkF(mid[1], 5.0F, "lerp center y");
      int crack = AtlasStitcher.slot("blocks/destroy_stage_5.png");
      float[] cuv = AtlasStitcher.uv(crack);
      Dim dim = new Dim();

      MeshBuilder b = new MeshBuilder();
      b.init();
      Blocks.byId(Blocks.STONE_ID).renderCrack(b, dim, 10, 40, 20, crack);
      check(b.count() > 0 && b.count() % 8 == 0, "cube crack all faces (" + b.count() + " verts)");
      float[] v = b.vertices();
      float[] t = b.texCoords();
      float[] c = b.colors();
      boolean sawPlain = false, sawSide = false, sawEdge = false;
      float minX = 99, maxX = -99, minY = 99, maxY = -99, minZ = 99, maxZ = -99;
      for (int i = 0; i < b.count(); i++) {
         minX = Math.min(minX, v[i * 3]);
         maxX = Math.max(maxX, v[i * 3]);
         minY = Math.min(minY, v[i * 3 + 1]);
         maxY = Math.max(maxY, v[i * 3 + 1]);
         minZ = Math.min(minZ, v[i * 3 + 2]);
         maxZ = Math.max(maxZ, v[i * 3 + 2]);
         check(t[i * 2] >= cuv[0] && t[i * 2] <= cuv[2]
            && t[i * 2 + 1] >= cuv[1] && t[i * 2 + 1] <= cuv[3], "cube crack uvs in crack tile");
         float cb = c[i * 3];
         check(cb == 0.25F || cb == 0.2F || cb == 0.15F, "cube crack shaded grade (" + cb + ")");
         if (cb == 0.25F) sawPlain = true;
         if (cb == 0.2F) sawSide = true;
         if (cb == 0.15F) sawEdge = true;
      }
      check(minX == 10.0F && maxX == 11.0F, "cube crack spans x faces");
      check(minY == 40.0F && maxY == 41.0F, "cube crack spans y faces");
      check(minZ == 20.0F && maxZ == 21.0F, "cube crack spans z faces");
      check(sawPlain && sawSide && sawEdge, "all three shade grades present");

      MeshBuilder tb = new MeshBuilder();
      tb.init();
      Blocks.byId(Blocks.TORCH_ID).renderCrack(tb, dim, 10, 40, 20, crack);
      check(tb.count() > 0 && tb.count() % 8 == 0, "torch crack all post faces (" + tb.count() + " verts)");
      float[] tv = tb.vertices();
      float[] tc = tb.colors();
      for (int i = 0; i < tb.count(); i++) {
         float px = tv[i * 3] - 10;
         float pz = tv[i * 3 + 2] - 20;
         float py = tv[i * 3 + 1] - 40;
         check(px >= 0.4375F && px <= 0.5625F && pz >= 0.4375F && pz <= 0.5625F, "torch crack on post");
         check(py >= 0.0F && py <= 0.625F, "torch crack within post height");
         checkF(tc[i * 3], 0.25F, "torch crack bakes cell light");
      }

      MeshBuilder fb = new MeshBuilder();
      fb.init();
      Blocks.byId(Blocks.ROSE_ID).renderCrack(fb, dim, 10, 40, 20, crack);
      check(fb.count() > 0 && fb.count() % 8 == 0, "flower crack masked quads (" + fb.count() + " verts)");
      float[] fv = fb.vertices();
      float[] ft = fb.texCoords();
      float[] fc = fb.colors();
      for (int i = 0; i < fb.count(); i++) {
         check(fv[i * 3] >= 10.0F && fv[i * 3] <= 11.0F
            && fv[i * 3 + 1] >= 40.0F && fv[i * 3 + 1] <= 41.0F
            && fv[i * 3 + 2] >= 20.0F && fv[i * 3 + 2] <= 21.0F, "flower crack inside the cell");
         check(ft[i * 2] >= cuv[0] && ft[i * 2] <= cuv[2]
            && ft[i * 2 + 1] >= cuv[1] && ft[i * 2 + 1] <= cuv[3], "flower crack uvs in crack tile");
         checkF(fc[i * 3], 0.25F, "flower crack bakes cell light");
      }

      if (failures == 0) System.out.println("CRACKMASK PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

