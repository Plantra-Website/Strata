package com.strata.server;

import com.strata.blocks.Blocks;
import com.strata.net.LocalConnection;
import com.strata.world.gen.TerrainGenerator;
import java.io.File;
import static com.strata.world.Level.LAVA_TICKS;

public class FlowTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static int[] drySpot(GameServer s, int x0, int z0) {
      for (int x = x0; x < x0 + 160; x += 4) {
         for (int z = z0; z < z0 + 160; z += 4) {
            if (s.level().generator().heightAt(x, z) > TerrainGenerator.SEA_LEVEL + 1) {
               return new int[]{x, z};
            }
         }
      }
      throw new RuntimeException("no dry land near " + x0 + "," + z0);
   }

   public static void main(String[] args) {
      ItemTest.wipeDir(new File("flowworld"));
      ItemTest.wipeDir(new File("flowworld2"));
      ItemTest.wipeDir(new File("flowworld3"));
      ItemTest.wipeDir(new File("flowworld4"));
      File dir = new File("flowworld");
      LocalConnection conn = new LocalConnection();
      GameServer s = new GameServer(conn, dir, 7777L);
      int x = 30, z = 30;
      int h = ItemTest.surface(s.level(), x, z);
      check(h > 0, "dig site found");

      s.level().setTile(x, h + 3, z, Blocks.LAVA_ID);
      s.level().setTile(x, h + 1, z, 0);
      s.level().setTile(x, h + 2, z, 0);
      s.level().setTile(x + 1, h + 3, z, Blocks.DIRT_ID);
      SandTest.breakOnce(s, conn, x + 1, h + 3, z);
      ItemTest.drain(s, conn, LAVA_TICKS + 20);
      check(s.level().getTile(x, h + 2, z) == Blocks.LAVA_ID, "down fill lands");
      check(s.level().getData(x, h + 2, z) == 8, "down fill flags falling");

      int sx = x + 10;
      int sh = ItemTest.surface(s.level(), sx, z);
      for (int i = -5; i <= 5; i++) {
         s.level().setTile(sx + i, sh + 1, z, Blocks.DIRT_ID);
         s.level().setTile(sx + i, sh + 2, z - 1, Blocks.DIRT_ID);
         s.level().setTile(sx + i, sh + 2, z + 1, Blocks.DIRT_ID);
         s.level().setTile(sx + i, sh + 2, z, 0);
      }
      s.level().setTile(sx, sh + 2, z, Blocks.LAVA_ID);
      s.level().scheduleTick(sx, sh + 2, z, LAVA_TICKS);
      ItemTest.drain(s, conn, LAVA_TICKS + 20);
      check(s.level().getTile(sx + 1, sh + 2, z) == Blocks.LAVA_ID, "side spread lands +x");
      check(s.level().getData(sx + 1, sh + 2, z) == 2, "side spread costs +2");
      check(s.level().getBlockLevel(sx + 1, sh + 2, z) == 15, "flow lights itself");

      ItemTest.drain(s, conn, LAVA_TICKS * 5 + 40);
      check(s.level().getTile(sx + 3, sh + 2, z) == Blocks.LAVA_ID, "range reaches +3");
      check(s.level().getData(sx + 3, sh + 2, z) == 6, "range level caps at 6");
      check(s.level().getTile(sx + 4, sh + 2, z) == 0, "range stops before +4");

      int tx = x + 16;
      int th = ItemTest.surface(s.level(), tx, z);
      s.level().setTile(tx, th + 1, z, Blocks.DIRT_ID);
      s.level().setTile(tx, th + 2, z, Blocks.LAVA_ID);
      s.level().setTile(tx + 1, th + 2, z, Blocks.TORCH_ID);
      s.level().scheduleTick(tx, th + 2, z, LAVA_TICKS);
      ItemTest.drain(s, conn, LAVA_TICKS + 20);
      check(s.level().getTile(tx + 1, th + 2, z) == Blocks.TORCH_ID, "torch survives the front");

      s.save();
      GameServer re = new GameServer(new LocalConnection(), dir, null);
      check(re.level().getTile(sx + 3, sh + 2, z) == Blocks.LAVA_ID, "flow tile persists");
      check(re.level().getData(sx + 3, sh + 2, z) == 6, "flow level persists");

      int fx = x + 24;
      int fh = ItemTest.surface(s.level(), fx, z);
      for (int dx = -6; dx <= 6; dx++) {
         for (int dz = -6; dz <= 6; dz++) {
            s.level().setTile(fx + dx, fh + 1, z + dz, Blocks.DIRT_ID);
            s.level().setTile(fx + dx, fh + 2, z + dz, 0);
            if (Math.abs(dx) == 6 || Math.abs(dz) == 6) {
               s.level().setTile(fx + dx, fh + 3, z + dz, Blocks.DIRT_ID);
            } else {
               s.level().setTile(fx + dx, fh + 3, z + dz, 0);
            }
         }
      }
      s.level().setTile(fx, fh + 2, z, Blocks.LAVA_ID);
      s.level().scheduleTick(fx, fh + 2, z, LAVA_TICKS);
      ItemTest.drain(s, conn, LAVA_TICKS + 20);
      check(s.level().getTile(fx - 1, fh + 2, z) == Blocks.LAVA_ID, "fan spreads -x too");
      check(s.level().getTile(fx, fh + 2, z + 1) == Blocks.LAVA_ID, "fan spreads +z too");
      check(s.level().getTile(fx, fh + 2, z - 1) == Blocks.LAVA_ID, "fan spreads -z too");

      SandTest.breakOnce(s, conn, fx, fh + 2, z);
      ItemTest.drain(s, conn, LAVA_TICKS * 12 + 60);
      check(s.level().getTile(fx + 1, fh + 2, z) == 0, "orphan +x arm dries");
      check(s.level().getTile(fx - 1, fh + 2, z) == 0, "orphan -x arm dries");

      File dir2 = new File("flowworld2");
      LocalConnection conn2 = new LocalConnection();
      GameServer s2 = new GameServer(conn2, dir2, 7777L);
      int[] spot8 = drySpot(s2, x + 32, z);
      int px = spot8[0];
      int pz8 = spot8[1];
      int ph = ItemTest.surface(s2.level(), px, pz8);
      for (int dx = -4; dx <= 4; dx++) {
         for (int dz = -4; dz <= 4; dz++) {
            s2.level().setTile(px + dx, ph, pz8 + dz, Blocks.DIRT_ID);
            s2.level().setTile(px + dx, ph + 1, pz8 + dz, 0);
            s2.level().setTile(px + dx, ph + 2, pz8 + dz, 0);
         }
      }
      for (int dx = -4; dx <= 4; dx++) {
         for (int dz = -4; dz <= 4; dz++) {
            if (Math.abs(dx) == 4 || Math.abs(dz) == 4) {
               s2.level().setTile(px + dx, ph + 1, pz8 + dz, Blocks.DIRT_ID);
            }
         }
      }
      s2.level().setTile(px, ph + 2, pz8, Blocks.LAVA_ID);
      s2.level().scheduleTick(px, ph + 2, pz8, LAVA_TICKS);
      ItemTest.drain(s2, conn2, LAVA_TICKS * 2 + 40);
      check(s2.level().getData(px, ph + 1, pz8) == 8, "pit flags falling");
      check(s2.level().getData(px + 1, ph + 1, pz8) == 1, "landed fall resets to 1");

      int[] spot9 = drySpot(s2, x + 40, z);
      int qx = spot9[0];
      int qz9 = spot9[1];
      int qh = ItemTest.surface(s2.level(), qx, qz9);
      s2.level().setTile(qx, qh + 1, qz9, Blocks.DIRT_ID);
      for (int i = 2; i <= 9; i++) {
         s2.level().setTile(qx, qh + i, qz9, 0);
      }
      s2.level().setTile(qx, qh + 10, qz9, Blocks.LAVA_ID);
      s2.level().scheduleTick(qx, qh + 10, qz9, LAVA_TICKS);
      ItemTest.drain(s2, conn2, LAVA_TICKS * 10 + 60);
      check(s2.level().getTile(qx, qh + 2, qz9) == Blocks.LAVA_ID, "shaft reaches bottom");
      check(s2.level().getData(qx, qh + 5, qz9) == 8, "shaft stays falling");

      int[] spot10 = drySpot(s2, x + 80, z);
      int rx = spot10[0];
      int rz10 = spot10[1];
      int rh = ItemTest.surface(s2.level(), rx, rz10);
      for (int dx = -5; dx <= 5; dx++) {
         s2.level().setTile(rx + dx, rh, rz10, Blocks.DIRT_ID);
         for (int i = 1; i <= 4; i++) {
            s2.level().setTile(rx + dx, rh + i, rz10, 0);
         }
      }
      s2.level().setTile(rx + 1, rh + 5, rz10, Blocks.DIRT_ID);
      s2.level().setTile(rx - 1, rh + 5, rz10, Blocks.DIRT_ID);
      s2.level().setTile(rx, rh + 5, rz10 + 1, Blocks.DIRT_ID);
      s2.level().setTile(rx, rh + 5, rz10 - 1, Blocks.DIRT_ID);
      s2.level().setTile(rx, rh + 5, rz10, Blocks.LAVA_ID);
      s2.level().scheduleTick(rx, rh + 5, rz10, LAVA_TICKS);
      ItemTest.drain(s2, conn2, LAVA_TICKS * 6 + 40);
      check(s2.level().getTile(rx, rh + 1, rz10) == Blocks.LAVA_ID, "rig shaft fills");
      check(s2.level().getTile(rx + 1, rh + 3, rz10) == 0, "mid-air wall clean high");
      check(s2.level().getTile(rx + 1, rh + 2, rz10) == 0, "mid-air wall clean mid");
      check(s2.level().getTile(rx + 1, rh + 1, rz10) == Blocks.LAVA_ID, "bottom pools outward");

      File dir3 = new File("flowworld3");
      LocalConnection conn3 = new LocalConnection();
      GameServer s3 = new GameServer(conn3, dir3, 7777L);
      int[] spot11 = drySpot(s3, x + 90, z);
      int wx = spot11[0];
      int wz11 = spot11[1];
      int wh = ItemTest.surface(s3.level(), wx, wz11);
      for (int dx = -6; dx <= 6; dx++) {
         for (int dz = -6; dz <= 6; dz++) {
            s3.level().setTile(wx + dx, wh + 1, wz11 + dz, Blocks.DIRT_ID);
            if (Math.abs(dx) == 6 || Math.abs(dz) == 6) {
               s3.level().setTile(wx + dx, wh + 2, wz11 + dz, Blocks.DIRT_ID);
               s3.level().setTile(wx + dx, wh + 3, wz11 + dz, Blocks.DIRT_ID);
            } else {
               s3.level().setTile(wx + dx, wh + 2, wz11 + dz, 0);
               s3.level().setTile(wx + dx, wh + 3, wz11 + dz, 0);
            }
         }
      }
      s3.level().setTile(wx - 3, wh + 4, wz11, Blocks.LAVA_ID);
      s3.player().teleport(wx - 2.5F, wh + 2.9F, wz11 + 0.5F);
      SandTest.breakOnce(s3, conn3, wx - 3, wh + 4, wz11);
      ItemTest.drain(s3, conn3, 60);
      com.strata.net.PlaceBlock pour = new com.strata.net.PlaceBlock();
      pour.x = wx + 2;
      pour.y = wh + 1;
      pour.z = wz11;
      pour.face = 1;
      pour.blockId = Blocks.LAVA_ID;
      conn3.sendToServer(pour);
      ItemTest.drain(s3, conn3, 5);
      check(s3.level().getTile(wx + 2, wh + 2, wz11) == Blocks.LAVA_ID, "pour lands");
      ItemTest.drain(s3, conn3, LAVA_TICKS + 20);
      check(s3.level().getTile(wx + 1, wh + 2, wz11) == Blocks.LAVA_ID, "pour spreads itself");

      File dir4 = new File("flowworld4");
      LocalConnection conn4 = new LocalConnection();
      GameServer s4 = new GameServer(conn4, dir4, 7777L);
      int ux = -504, uz = -498;
      int uy = s4.level().generator().unrestFluidY(ux, uz);
      check(uy > 0, "puncture site restless (got " + uy + ")");
      check(s4.level().getTile(ux, uy, uz) == Blocks.WATER_ID, "surface holds pre-stream");
      check(s4.level().getTile(ux, uy - 1, uz) == 0, "hole hovers pre-stream");
      s4.player().teleport(ux + 0.5F, 90.0F, uz + 0.5F);
      ItemTest.drain(s4, conn4, 300);
      check(s4.level().getTile(ux, uy - 1, uz) == Blocks.WATER_ID, "hole streams full");
      check(s4.level().getTile(ux, uy, uz) == Blocks.WATER_ID, "surface persists (source)");

      if (failures == 0) System.out.println("FLOW PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

