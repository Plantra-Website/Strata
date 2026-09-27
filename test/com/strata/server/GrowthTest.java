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
      s.level().setTile(nx, nh + 1, nz, Blocks.SAPLING_ID);
      s.level().scheduleTick(nx, nh + 1, nz, 5);
      ItemTest.drain(s, conn, 20);
      check(topWood(s, nx, nh + 1, nz) >= nh + 4, "negative-coord sapling grows");

      if (failures == 0) System.out.println("GROWTH PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

