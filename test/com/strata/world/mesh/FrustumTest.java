package com.strata.world.mesh;

import com.strata.core.AABB;

public class FrustumTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static Frustum box() {
      Frustum f = new Frustum();
      f.m_Frustum[0] = new float[]{-1, 0, 0, 1}; 
      f.m_Frustum[1] = new float[]{1, 0, 0, 1}; 
      f.m_Frustum[2] = new float[]{0, 1, 0, 1}; 
      f.m_Frustum[3] = new float[]{0, -1, 0, 1}; 
      f.m_Frustum[4] = new float[]{0, 0, -1, 1}; 
      f.m_Frustum[5] = new float[]{0, 0, 1, 1}; 
      return f;
   }

   public static void main(String[] args) {
      Frustum f = box();
      check(f.cubeInFrustum(new AABB(0, 0, 0, 0.5F, 0.5F, 0.5F)), "inside box drawn");
      check(!f.cubeInFrustum(new AABB(5, 5, 5, 6, 6, 6)), "far box culled");
      check(!f.cubeInFrustum(new AABB(-6, 0, 0, -5, 1, 1)), "behind-left culled");
      check(f.cubeInFrustum(new AABB(0.5F, 0, 0, 2, 1, 1)), "straddler drawn");
      check(f.cubeInFrustum(new AABB(-10, -10, -10, 10, 10, 10)), "engulfer drawn");

      if (failures == 0) System.out.println("FRUSTUM PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

