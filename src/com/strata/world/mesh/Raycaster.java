package com.strata.world.mesh;

import com.strata.HitResult;
import com.strata.blocks.Block;
import com.strata.blocks.Blocks;
import com.strata.core.AABB;
import com.strata.core.MathHelper;
import com.strata.world.Level;

public class Raycaster {
   public static HitResult pick(Level level, double ex, double ey, double ez, float yawDeg, float pitchDeg, double maxDist) {
      double yaw = Math.toRadians(yawDeg);
      double pitch = Math.toRadians(pitchDeg);
      double dx = Math.sin(yaw) * Math.cos(pitch);
      double dy = -Math.sin(pitch);
      double dz = -Math.cos(yaw) * Math.cos(pitch);
      int x = MathHelper.floor(ex);
      int y = MathHelper.floor(ey);
      int z = MathHelper.floor(ez);
      int stepX = dx > 0.0 ? 1 : -1;
      int stepY = dy > 0.0 ? 1 : -1;
      int stepZ = dz > 0.0 ? 1 : -1;
      double tMaxX = (stepX > 0 ? (x + 1 - ex) : (ex - x)) / Math.abs(dx);
      double tMaxY = (stepY > 0 ? (y + 1 - ey) : (ey - y)) / Math.abs(dy);
      double tMaxZ = (stepZ > 0 ? (z + 1 - ez) : (ez - z)) / Math.abs(dz);
      double tDeltaX = 1.0 / Math.abs(dx);
      double tDeltaY = 1.0 / Math.abs(dy);
      double tDeltaZ = 1.0 / Math.abs(dz);
      boolean inside = level.getTile(x, y, z) > 0;
      double bestT = maxDist + 1.0;
      HitResult best = null;
      double t = 0.0;
      while (t <= maxDist && t <= bestT) {
         int tile = level.getTile(x, y, z);
         if (tile > 0 && !Blocks.isFluid(tile) && !inside) {
            Block block = Blocks.byId(tile);
            if (block != null) {
               AABB box = block.pickBox(x, y, z);
               double[] hit = slabHit(box, ex, ey, ez, dx, dy, dz);
               if (hit != null && hit[0] < bestT) {
                  bestT = hit[0];
                  best = new HitResult(x, y, z, (int)hit[1]);
               }
            }
         }
         if (tile <= 0 || Blocks.isFluid(tile)) {
            inside = false;
         }
         if (tMaxX < tMaxY && tMaxX < tMaxZ) {
            x += stepX;
            t = tMaxX;
            tMaxX += tDeltaX;
         } else if (tMaxY < tMaxZ) {
            y += stepY;
            t = tMaxY;
            tMaxY += tDeltaY;
         } else {
            z += stepZ;
            t = tMaxZ;
            tMaxZ += tDeltaZ;
         }
      }
      return best;
   }

   static double[] slabHit(AABB box, double ex, double ey, double ez, double dx, double dy, double dz) {
      double[] xr = axisRange(box.x0, box.x1, ex, dx);
      double[] yr = axisRange(box.y0, box.y1, ey, dy);
      double[] zr = axisRange(box.z0, box.z1, ez, dz);
      double enter = xr[0];
      int face;
      if (yr[0] > enter) {
         enter = yr[0];
         face = dy > 0.0 ? 0 : 1;
      } else {
         face = dx > 0.0 ? 4 : 5;
      }
      if (zr[0] > enter) {
         enter = zr[0];
         face = dz > 0.0 ? 2 : 3;
      }
      double exit = Math.min(xr[1], Math.min(yr[1], zr[1]));
      if (enter > exit) {
         return null;
      }
      return new double[]{enter < 0.0 ? 0.0 : enter, face};
   }

   private static double[] axisRange(float min, float max, double e, double d) {
      if (d == 0.0) {
         if (e < min || e > max) {
            return new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
         }
         return new double[]{Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
      }
      double a = (min - e) / d;
      double b = (max - e) / d;
      return a < b ? new double[]{a, b} : new double[]{b, a};
   }
}

