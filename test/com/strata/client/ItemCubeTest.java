package com.strata.client;

import com.strata.blocks.AtlasStitcher;
import com.strata.blocks.Blocks;
import com.strata.blocks.CubeBlock;
import com.strata.blocks.MeshBuilder;

public class ItemCubeTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static void checkF(float got, float want, String msg) {
      if (Math.abs(got - want) > 1e-4) { failures++; System.out.println("FAIL: " + msg + " got=" + got + " want=" + want); }
   }

   static float normalDot(float[] v, int q, float ax, float ay, float az) {
      float e1x = v[(q * 4 + 1) * 3] - v[(q * 4) * 3];
      float e1y = v[(q * 4 + 1) * 3 + 1] - v[(q * 4) * 3 + 1];
      float e1z = v[(q * 4 + 1) * 3 + 2] - v[(q * 4) * 3 + 2];
      float e2x = v[(q * 4 + 2) * 3] - v[(q * 4) * 3];
      float e2y = v[(q * 4 + 2) * 3 + 1] - v[(q * 4) * 3 + 1];
      float e2z = v[(q * 4 + 2) * 3 + 2] - v[(q * 4) * 3 + 2];
      float nx = e1y * e2z - e1z * e2y;
      float ny = e1z * e2x - e1x * e2z;
      float nz = e1x * e2y - e1y * e2x;
      return nx * ax + ny * ay + nz * az;
   }

   public static void main(String[] args) {
      CubeBlock grass = (CubeBlock)Blocks.byId(Blocks.GRASS_ID);
      MeshBuilder b = new MeshBuilder();
      b.init();
      int n = ItemRenderer.emitCube(b, grass, 10, 20, 30, 0.125F, 0.0, 1.0F);
      check(n == 24, "six quads (" + n + " verts)");
      check(b.count() == 24, "builder holds six quads");
      float[] v = b.vertices();
      float[] t = b.texCoords();
      float[] c = b.colors();
      float[][] axes = {{0, 1, 0}, {0, -1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
      String[] names = {"top", "bottom", "east", "west", "south", "north"};
      for (int q = 0; q < 6; q++) {
         check(normalDot(v, q, axes[q][0], axes[q][1], axes[q][2]) > 0, names[q] + " winds outward");
      }
      float[] top = AtlasStitcher.uv(grass.topTexture);
      float[] side = AtlasStitcher.uv(grass.sideTexture);
      float[] bot = AtlasStitcher.uv(grass.bottomTexture);
      for (int k = 0; k < 4; k++) {
         check(t[k * 2] >= top[0] && t[k * 2] <= top[2], "top u in top tile");
      }
      for (int k = 8; k < 24; k++) {
         check(t[k * 2] >= side[0] && t[k * 2] <= side[2], "side u in side tile");
      }
      for (int k = 4; k < 8; k++) {
         check(t[k * 2] >= bot[0] && t[k * 2] <= bot[2], "bottom u in dirt tile");
      }
      checkF(c[0 * 3], 1.0F, "top full bright");
      checkF(c[4 * 3], 0.5F, "bottom dim");
      checkF(c[8 * 3], 0.6F, "east edge shade");
      checkF(c[16 * 3], 0.8F, "south side shade");
      float mx = 0, my = 0, mz = 0;
      for (int i = 0; i < 24; i++) {
         mx += v[i * 3];
         my += v[i * 3 + 1];
         mz += v[i * 3 + 2];
      }
      checkF(mx / 24, 10.0F, "centroid x");
      checkF(my / 24, 20.0F, "centroid y");
      checkF(mz / 24, 30.0F, "centroid z");
      MeshBuilder b2 = new MeshBuilder();
      b2.init();
      ItemRenderer.emitCube(b2, grass, 10, 20, 30, 0.125F, Math.PI / 2, 1.0F);
      float[] v2 = b2.vertices();
      boolean moved = false;
      for (int i = 0; i < 24 * 3; i++) {
         if (Math.abs(v[i] - v2[i]) > 1e-4) {
            moved = true;
            break;
         }
      }
      check(moved, "quarter spin moves verts");
      MeshBuilder b3 = new MeshBuilder();
      b3.init();
      ItemRenderer.emitCube(b3, grass, 10, 20, 30, 0.125F, Math.PI * 2, 1.0F);
      float[] v3 = b3.vertices();
      boolean back = true;
      for (int i = 0; i < 24 * 3; i++) {
         if (Math.abs(v[i] - v3[i]) > 1e-3) {
            back = false;
            break;
         }
      }
      check(back, "full turn returns");
      if (failures == 0) System.out.println("ITEMCUBE PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

