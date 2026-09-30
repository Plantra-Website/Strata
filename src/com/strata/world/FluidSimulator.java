package com.strata.world;

import com.strata.blocks.Blocks;
import com.strata.blocks.Fluid;
import com.strata.core.Dirs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

final class FluidSimulator {
   static final int SAPLING_TICKS = 12000;

   static final int GROWTH_TICKS = 300;

   static final int MELT_TICKS = 600;

   private final Level level;
   private final HashMap<Long, Long> scheduled = new HashMap<>();
   private long scheduledClock = 0;

   FluidSimulator(Level level) {
      this.level = level;
   }

   void scheduleTick(int x, int y, int z, int delay) {
      long key = Level.entityKey(x, y, z);
      long due = this.scheduledClock + delay;
      Long cur = this.scheduled.get(key);
      if (cur == null || due < cur) {
         this.scheduled.put(key, due);
      }
   }

   void tickScheduled() {
      this.scheduledClock++;
      if (this.scheduled.isEmpty()) {
         return;
      }
      ArrayList<Long> due = new ArrayList<>();
      for (Map.Entry<Long, Long> e : this.scheduled.entrySet()) {
         if (e.getValue() <= this.scheduledClock) {
            due.add(e.getKey());
         }
      }
      for (int i = 0; i < due.size(); i++) {
         long key = due.get(i);
         this.scheduled.remove(key);
         int ex = (int)(key >> 38);
         if ((ex & 0x2000000) != 0) {
            ex |= ~0x3FFFFFF;
         }
         int ez = (int)((key >> 12) & 0x3FFFFFF);
         if ((ez & 0x2000000) != 0) {
            ez |= ~0x3FFFFFF;
         }
         this.onScheduledTick(ex, (int)(key & 0xFFF), ez);
      }
   }

   private void onScheduledTick(int x, int y, int z) {
      int id = this.level.getTile(x, y, z);
      if (Blocks.isSapling(id)) {
         this.level.growTree(x, y, z);
      } else if (Blocks.isFluid(id)) {
         this.flowLiquid(x, y, z, id);
      } else if (id == Blocks.REED_ID || id == Blocks.CACTUS_ID) {
         this.growStalk(x, y, z, id);
      } else if (id == Blocks.ICE_ID || id == Blocks.SNOW_LAYER_ID
         || id == Blocks.SNOW_BLOCK_ID) {
         this.meltCheck(x, y, z, id);
      }
   }

   private void meltCheck(int x, int y, int z, int id) {
      int glow = this.level.getBlockLevel(x, y, z);
      if (id == Blocks.ICE_ID) {
         if (glow > 8) {
            this.level.setTile(x, y, z, Blocks.WATER_ID);
         }
         return;
      }
      if (glow > 11) {
         this.level.setTile(x, y, z, 0);
      }
   }

   void armMeltCheck(int x, int y, int z, int radius) {
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dy = -radius; dy <= radius; dy++) {
            for (int dz = -radius; dz <= radius; dz++) {
               int id = this.level.getTile(x + dx, y + dy, z + dz);
               if (id == Blocks.ICE_ID || id == Blocks.SNOW_LAYER_ID
                  || id == Blocks.SNOW_BLOCK_ID) {
                  this.scheduleTick(x + dx, y + dy, z + dz, MELT_TICKS);
               }
            }
         }
      }
   }

   private void growStalk(int x, int y, int z, int id) {
      if (this.level.getTile(x, y + 1, z) != 0) {
         return;
      }
      int below = 0;
      while (below < 3 && this.level.getTile(x, y - 1 - below, z) == id) {
         below++;
      }
      if (below + 1 >= 3) {
         return;
      }
      int stage = this.level.getData(x, y, z);
      if (stage >= 15) {
         this.level.setTile(x, y + 1, z, id);
         this.level.setData(x, y, z, 0);
         this.scheduleTick(x, y + 1, z, GROWTH_TICKS);
      } else {
         this.level.setData(x, y, z, stage + 1);
      }
      this.scheduleTick(x, y, z, GROWTH_TICKS);
   }

   private void flowLiquid(int x, int y, int z, int fluid) {
      Fluid.Spec flow = Fluid.of(fluid);
      int step = flow.step;
      int range = flow.range;
      int ticks = flow.ticks;
      int level = this.level.getData(x, y, z);

      if (fluid == Blocks.LAVA_ID && this.tryHarden(x, y, z)) {
         this.wakeFluids(x, y, z);
         return;
      }

      if (y > 0 && this.level.getTile(x, y - 1, z) == 0) {
         this.placeFlow(fluid, x, y - 1, z, level >= 8 ? level : level + 8);
         return;
      }
      if (level > 0) {
         int want;
         if (this.level.getTile(x, y + 1, z) == fluid) {
            int ad = this.level.getData(x, y + 1, z);
            want = ad >= 8 ? ad : ad + 8;
         } else {
            int s = -100;
            s = this.smallestFlowDecay(x + 1, y, z, s, fluid);
            s = this.smallestFlowDecay(x - 1, y, z, s, fluid);
            s = this.smallestFlowDecay(x, y, z + 1, s, fluid);
            s = this.smallestFlowDecay(x, y, z - 1, s, fluid);
            want = s + step;
            if (want >= 8 || s < 0) {
               want = -1;
            }
         }
         if (fluid == Blocks.WATER_ID && want != 0 && this.adjacentSources(x, y, z, fluid) >= 2) {
            int below = y > 0 ? this.level.getTile(x, y - 1, z) : 0;
            if (below != 0 && (below != fluid || this.level.getData(x, y - 1, z) == 0)) {
               want = 0;
            }
         }
         if (want != level) {
            if (want < 0) {
               this.removeFlow(x, y, z);
            } else {
               this.level.setData(x, y, z, want);
               this.scheduleTick(x, y, z, ticks);
            }
            this.wakeFluids(x, y, z);
         }
      }
      int sideLevel = level >= 8 ? 1 : level + step;
      if (sideLevel > range) {
         return;
      }
      if (y > 0) {
         int below = this.level.getTile(x, y - 1, z);
         if (below == 0 || Blocks.isFluid(below)) {
            if (fluid != Blocks.WATER_ID || level != 0) {
               return;
            }
         }
      }
      boolean[] opt = this.optimalFlowDirs(x, y, z, fluid);
      if (opt[0] && this.level.getTile(x + 1, y, z) == 0) {
         this.placeFlow(fluid, x + 1, y, z, sideLevel);
      }
      if (opt[1] && this.level.getTile(x - 1, y, z) == 0) {
         this.placeFlow(fluid, x - 1, y, z, sideLevel);
      }
      if (opt[2] && this.level.getTile(x, y, z + 1) == 0) {
         this.placeFlow(fluid, x, y, z + 1, sideLevel);
      }
      if (opt[3] && this.level.getTile(x, y, z - 1) == 0) {
         this.placeFlow(fluid, x, y, z - 1, sideLevel);
      }
   }

   private int smallestFlowDecay(int x, int y, int z, int best, int fluid) {
      if (this.level.getTile(x, y, z) != fluid) {
         return best;
      }
      int d = effLevel(this.level.getData(x, y, z));
      if (d == 0) {
         return 0;
      }
      return best >= 0 && d >= best ? best : d;
   }

   private int adjacentSources(int x, int y, int z, int fluid) {
      int n = 0;
      if (this.level.getTile(x + 1, y, z) == fluid && effLevel(this.level.getData(x + 1, y, z)) == 0) n++;
      if (this.level.getTile(x - 1, y, z) == fluid && effLevel(this.level.getData(x - 1, y, z)) == 0) n++;
      if (this.level.getTile(x, y, z + 1) == fluid && effLevel(this.level.getData(x, y, z + 1)) == 0) n++;
      if (this.level.getTile(x, y, z - 1) == fluid && effLevel(this.level.getData(x, y, z - 1)) == 0) n++;
      return n;
   }

   private boolean blocksFlow(int x, int y, int z) {
      int id = this.level.getTile(x, y, z);
      return id != 0 && id != Blocks.LAVA_ID;
   }

   private int flowCost(int x, int y, int z, int depth, int fromDir, int fluid) {
      int best = 1000;
      for (int dir = 0; dir < 4; dir++) {
         if ((dir == 0 && fromDir == 1) || (dir == 1 && fromDir == 0)
            || (dir == 2 && fromDir == 3) || (dir == 3 && fromDir == 2)) {
            continue;
         }
         int nx = x + (dir == 0 ? 1 : dir == 1 ? -1 : 0);
         int nz = z + (dir == 2 ? 1 : dir == 3 ? -1 : 0);
         if (!this.blocksFlow(nx, y, nz)
            && (this.level.getTile(nx, y, nz) != fluid
               || effLevel(this.level.getData(nx, y, nz)) != 0)) {
            if (this.level.getTile(nx, y - 1, nz) == 0) {
               return depth;
            }
            if (depth < 4) {
               int c = this.flowCost(nx, y, nz, depth + 1, dir, fluid);
               if (c < best) {
                  best = c;
               }
            }
         }
      }
      return best;
   }

   private boolean[] optimalFlowDirs(int x, int y, int z, int fluid) {
      int[] cost = new int[4];
      int[] nx = {x + 1, x - 1, x, x};
      int[] nz = {z, z, z + 1, z - 1};
      for (int dir = 0; dir < 4; dir++) {
         cost[dir] = 1000;
         if (!this.blocksFlow(nx[dir], y, nz[dir])
            && (this.level.getTile(nx[dir], y, nz[dir]) != fluid
               || effLevel(this.level.getData(nx[dir], y, nz[dir])) != 0)) {
            if (this.level.getTile(nx[dir], y - 1, nz[dir]) == 0) {
               cost[dir] = 0;
            } else {
               cost[dir] = this.flowCost(nx[dir], y, nz[dir], 1, dir, fluid);
            }
         }
      }
      int m = cost[0];
      for (int dir = 1; dir < 4; dir++) {
         if (cost[dir] < m) {
            m = cost[dir];
         }
      }
      boolean[] opt = new boolean[4];
      for (int dir = 0; dir < 4; dir++) {
         opt[dir] = cost[dir] == m;
      }
      return opt;
   }

   private void wakeFluids(int x, int y, int z) {
      this.wakeFluid(x + 1, y, z);
      this.wakeFluid(x - 1, y, z);
      this.wakeFluid(x, y + 1, z);
      this.wakeFluid(x, y - 1, z);
      this.wakeFluid(x, y, z + 1);
      this.wakeFluid(x, y, z - 1);
   }

   private void wakeFluid(int x, int y, int z) {
      int id = this.level.getTile(x, y, z);
      if (Blocks.isFluid(id)) {
         this.scheduleTick(x, y, z, Fluid.of(id).ticks);
      }
   }

   private void removeFlow(int x, int y, int z) {
      this.level.setTile(x, y, z, 0);
      this.level.setData(x, y, z, 0);
   }

   private void placeFlow(int fluid, int x, int y, int z, int level) {
      this.level.setTile(x, y, z, fluid);
      this.level.setData(x, y, z, level);
      this.scheduleTick(x, y, z, Fluid.of(fluid).ticks);
      this.wakeFluids(x, y, z);
      if (fluid == Blocks.LAVA_ID) {
         this.hardenCheck(x, y, z);
         this.armMeltCheck(x, y, z, 7);
      } else {
         this.hardenNeighbors(x, y, z);
      }
   }

   private boolean tryHarden(int x, int y, int z) {
      if (this.level.getTile(x, y, z) != Blocks.LAVA_ID) {
         return false;
      }
      for (int[] d : Dirs.DIRS) {
         if (this.level.getTile(x + d[0], y + d[1], z + d[2]) == Blocks.WATER_ID) {
            this.level.setTile(x, y, z, Blocks.STONE_ID);
            return true;
         }
      }
      return false;
   }

   private void hardenCheck(int x, int y, int z) {
      this.tryHarden(x, y, z);
   }

   private void hardenNeighbors(int x, int y, int z) {
      for (int[] d : Dirs.DIRS) {
         this.hardenCheck(x + d[0], y + d[1], z + d[2]);
      }
   }

   static int effLevel(int data) {
      return data >= 8 ? 0 : data;
   }
}

