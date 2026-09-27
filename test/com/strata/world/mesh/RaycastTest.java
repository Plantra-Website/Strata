package com.strata.world.mesh;

import com.strata.HitResult;
import com.strata.blocks.Blocks;
import com.strata.core.AABB;
import com.strata.world.Level;

public class RaycastTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static void lane(Level l, int x, int z0, int z1) {
      for (int z = z0; z <= z1; z++) {
         l.setTile(x, 40, z, 0);
      }
   }

   public static void main(String[] args) {
      AABB box = new AABB(0, 0, 0, 1, 1, 1);
      double[] hit = Raycaster.slabHit(box, -1, 0.5, 0.5, 1, 0, 0);
      check(hit != null && Math.abs(hit[0] - 1.0) < 1e-9 && (int)hit[1] == 4, "slab entry t + face");
      check(Raycaster.slabHit(box, -1, 5, 0.5, 1, 0, 0) == null, "parallel miss");
      check(Raycaster.slabHit(box, 0.5, 0.5, 0.5, 1, 0, 0)[0] == 0.0, "inside reports t=0");

      Level l = new Level(64, false);
      lane(l, 0, -3, 8);
      l.setTile(0, 40, 0, Blocks.TORCH_ID);
      HitResult center = Raycaster.pick(l, 0.5, 40.3, -2.5, 180, 0, 6.0);
      check(center != null && center.x == 0 && center.y == 40 && center.z == 0, "torch post hit");
      check(center != null && center.f == 2, "torch hit face z- (got " + (center == null ? "null" : center.f) + ")");
      HitResult edge = Raycaster.pick(l, 0.1, 40.3, -2.5, 180, 0, 6.0);
      check(edge == null, "ray past the post misses the cell");
      for (int y = 41; y <= 45; y++) {
         l.setTile(0, y, 0, 0);
      }
      HitResult top = Raycaster.pick(l, 0.5, 45, 0.5, 0, 90, 8.0);
      check(top != null && top.x == 0 && top.y == 40 && top.z == 0 && top.f == 1, "torch crown face top");

      lane(l, 5, -3, 8);
      l.setTile(5, 40, 5, Blocks.ROSE_ID);
      HitResult bloom = Raycaster.pick(l, 5.5, 40.3, 2.5, 180, 0, 6.0);
      check(bloom != null && bloom.x == 5 && bloom.z == 5, "flower hit");
      HitResult stem = Raycaster.pick(l, 5.05, 40.3, 2.5, 180, 0, 6.0);
      check(stem == null, "ray past the bloom misses");

      l.setTile(0, 40, 0, Blocks.STONE_ID);
      HitResult cube = Raycaster.pick(l, 0.5, 40.3, -2.5, 180, 0, 6.0);
      check(cube != null && cube.x == 0 && cube.y == 40 && cube.z == 0 && cube.f == 2, "cube unchanged");

      if (failures == 0) System.out.println("RAYCAST PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

