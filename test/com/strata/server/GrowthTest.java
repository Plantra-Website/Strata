package com.strata.server;

import com.strata.blocks.Blocks;
import com.strata.net.LocalConnection;
import java.io.File;

public class GrowthTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static int topWood(GameServer s, int x, int y, int z) {
      int top = -1;
      for (int i = 0; i < 10; i++) {
         if (s.level().getTile(x, y + i, z) == Blocks.WOOD_ID) top = y + i;
      }
      return top;
   }

   static int leafCount(GameServer s, int x, int y, int z) {
      int n = 0;
      for (int dx = -2; dx <= 2; dx++) {
         for (int dz = -2; dz <= 2; dz++) {
            for (int i = -3; i <= 8; i++) {
               if (s.level().getTile(x + dx, y + i, z + dz) == Blocks.LEAF_ID) n++;
            }
         }
      }
      return n;
   }

   public static void main(String[] args) {
      File dir = new File("growthworld");
      LocalConnection conn = new LocalConnection();
      GameServer s = new GameServer(conn, dir, 818L);

      int x = 30, z = 30;
      int h = ItemTest.surface(s.level(), x, z);
      s.level().setTile(x, h + 1, z, Blocks.SAPLING_ID);
      s.level().scheduleTick(x, h + 1, z, 5);
      ItemTest.drain(s, conn, 20);
      int top = topWood(s, x, h + 1, z);
      check(top >= h + 4 && top <= h + 6, "trunk 4-6 high (top=" + top + ", base=" + (h + 1) + ")");
      check(leafCount(s, x, h + 1, z) > 20, "canopy leaves present");

      int bx = x + 6;
      int bh = ItemTest.surface(s.level(), bx, z);
      s.level().setTile(bx, bh + 1, z, Blocks.SAPLING_ID);
      s.level().scheduleTick(bx, bh + 1, z, 5);
      s.level().setTile(bx, bh + 1, z, 0);
      ItemTest.drain(s, conn, 20);
      check(topWood(s, bx, bh + 1, z) < 0, "broken sapling grows nothing");

      int fx = x + 12;
      int fh = ItemTest.surface(s.level(), fx, z);
      s.level().setTile(fx, fh + 1, z, 0);
      s.level().setTile(fx, fh + 2, z, Blocks.SAPLING_ID);
      s.level().scheduleTick(fx, fh + 2, z, 5);
      ItemTest.drain(s, conn, 20);
      check(topWood(s, fx, fh + 2, z) < 0, "floating sapling grows nothing");

      int nx = -30, nz = -30;
      int nh = ItemTest.surface(s.level(), nx, nz);
      s.level().setTile(nx, nh, nz, Blocks.DIRT_ID);
      for (int i = 1; i <= 7; i++) {
         s.level().setTile(nx, nh + i, nz, 0);
      }
      s.level().setTile(nx, nh + 1, nz, Blocks.SAPLING_ID);
      s.level().scheduleTick(nx, nh + 1, nz, 5);
      ItemTest.drain(s, conn, 20);
      check(topWood(s, nx, nh + 1, nz) >= nh + 4, "negative-coord sapling grows");

      int ex = x + 18;
      int eh = ItemTest.surface(s.level(), ex, z);
      s.level().setTile(ex, eh, z, Blocks.DIRT_ID);
      for (int i = 1; i <= 10; i++) {
         s.level().setTile(ex, eh + i, z, 0);
      }
      s.level().setTile(ex, eh + 1, z, Blocks.BIRCH_SAPLING_ID);
      s.level().scheduleTick(ex, eh + 1, z, 5);
      ItemTest.drain(s, conn, 20);
      boolean birchWood = false;
      for (int i = 0; i < 10; i++) {
         if (s.level().getTile(ex, eh + 1 + i, z) == Blocks.BIRCH_LOG_ID) {
            birchWood = true;
         }
      }
      check(birchWood, "birch sapling grows birch wood");
      int gx = x + 24;
      int gh = ItemTest.surface(s.level(), gx, z);
      s.level().setTile(gx, gh, z, Blocks.DIRT_ID);
      for (int i = 1; i <= 10; i++) {
         s.level().setTile(gx, gh + i, z, 0);
      }
      s.level().setTile(gx, gh + 1, z, Blocks.SPRUCE_SAPLING_ID);
      s.level().scheduleTick(gx, gh + 1, z, 5);
      ItemTest.drain(s, conn, 20);
      boolean spruceWood = false;
      for (int i = 0; i < 10; i++) {
         if (s.level().getTile(gx, gh + 1 + i, z) == Blocks.SPRUCE_LOG_ID) {
            spruceWood = true;
         }
      }
      check(spruceWood, "spruce sapling grows spruce wood");

      int px = x + 30;
      int ph = ItemTest.surface(s.level(), px, z);
      s.level().setTile(px, ph, z, Blocks.SAND_ID);
      s.level().setTile(px + 1, ph, z, Blocks.WATER_ID);
      for (int i = 1; i <= 6; i++) {
         s.level().setTile(px, ph + i, z, 0);
      }
      s.level().setTile(px, ph + 1, z, Blocks.REED_ID);
      s.level().scheduleTick(px, ph + 1, z, 5);
      ItemTest.drain(s, conn, 20);
      check(s.level().getTile(px, ph + 1, z) == Blocks.REED_ID, "reed cutting takes");
      check(s.level().getData(px, ph + 1, z) > 0, "reed stages (data counts)");
      s.level().setTile(px, ph + 1, z, Blocks.stateOf(Blocks.byId(Blocks.REED_ID), 15));
      s.level().scheduleTick(px, ph + 1, z, 5);
      ItemTest.drain(s, conn, 20);
      check(s.level().getTile(px, ph + 2, z) == Blocks.REED_ID, "ripe reed grows up");
      check(s.level().getData(px, ph + 1, z) == 0, "grown reed resets its counter");

      int qx = x + 36;
      int qh = ItemTest.surface(s.level(), qx, z);
      s.level().setTile(qx, qh, z, Blocks.SAND_ID);
      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            if (dx == 0 && dz == 0) {
               continue;
            }
            s.level().setTile(qx + dx, qh + 1, z + dz, 0);
         }
      }
      for (int i = 1; i <= 6; i++) {
         s.level().setTile(qx, qh + i, z, 0);
      }
      s.level().setTile(qx, qh + 1, z, Blocks.stateOf(Blocks.byId(Blocks.CACTUS_ID), 15));
      s.level().scheduleTick(qx, qh + 1, z, 5);
      ItemTest.drain(s, conn, 20);
      check(s.level().getTile(qx, qh + 2, z) == Blocks.CACTUS_ID, "ripe cactus grows up");
      s.level().setTile(qx, qh + 3, z, Blocks.CACTUS_ID);
      s.level().setTile(qx, qh + 1, z, Blocks.stateOf(Blocks.byId(Blocks.CACTUS_ID), 15));
      s.level().setTile(qx, qh + 2, z, Blocks.stateOf(Blocks.byId(Blocks.CACTUS_ID), 15));
      s.level().setTile(qx, qh + 3, z, Blocks.stateOf(Blocks.byId(Blocks.CACTUS_ID), 15));
      s.level().scheduleTick(qx, qh + 1, z, 5);
      s.level().scheduleTick(qx, qh + 2, z, 5);
      s.level().scheduleTick(qx, qh + 3, z, 5);
      ItemTest.drain(s, conn, com.strata.world.Level.GROWTH_TICKS + 40);
      check(s.level().getTile(qx, qh + 4, z) == 0, "cactus stops at 3 tall");

      if (failures == 0) System.out.println("GROWTH PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

