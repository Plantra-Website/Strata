package com.strata;

import com.strata.core.AABB;

public class HitResult {
   public int x;
   public int y;
   public int z;
   public int f;
   public int entityId = -1;
   public AABB box = null;
   public double t = -1.0;

   public HitResult(int x, int y, int z, int f) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.f = f;
   }
}

