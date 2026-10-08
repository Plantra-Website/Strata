package com.strata.server;

import com.strata.blocks.Blocks;
import com.strata.net.BreakEffect;
import com.strata.net.DebugGive;
import com.strata.net.InputState;
import com.strata.net.ItemSpawn;
import com.strata.net.LocalConnection;
import com.strata.net.Packet;
import com.strata.net.PlaceBlock;
import com.strata.world.gen.TerrainGenerator;
import java.io.File;
import java.util.ArrayList;

public class SandTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static boolean sawDrop(ArrayList<Packet> packets, int blockId) {
      for (Packet p : packets) {
         if (p instanceof ItemSpawn && ((ItemSpawn)p).blockId == blockId) return true;
      }
      return false;
   }

   static void breakOnce(GameServer s, LocalConnection conn, int x, int y, int z) {
      InputState in = new InputState();
      in.breaking = true;
      in.breakX = x;
      in.breakY = y;
      in.breakZ = z;
      in.breakFace = 1;
      for (int i = 0; i < 150; i++) {
         conn.sendToServer(in);
         s.tick();
         Packet p;
         while ((p = conn.pollClient()) != null) {
            if (p instanceof BreakEffect) {
               BreakEffect e = (BreakEffect)p;
               if (e.x == x && e.y == y && e.z == z) {
                  return;
               }
            }
         }
      }
   }

   static ArrayList<Packet> settle(GameServer s, LocalConnection conn, int ticks) {
      ArrayList<Packet> out = new ArrayList<>();
      InputState idle = new InputState();
      for (int i = 0; i < ticks; i++) {
         conn.sendToServer(idle);
         s.tick();
         Packet p;
         while ((p = conn.pollClient()) != null) {
            out.add(p);
         }
      }
      return out;
   }

   static void place(GameServer s, LocalConnection conn, int x, int y, int z, int id) {
      PlaceBlock p = new PlaceBlock();
      p.x = x;
      p.y = y;
      p.z = z;
      p.face = 1;
      p.blockId = id;
      conn.sendToServer(p);
      settle(s, conn, 3);
   }

   public static void main(String[] args) {
      File dir = new File("sandworld");
      LocalConnection conn = new LocalConnection();
      GameServer s = new GameServer(conn, dir, 31337L);
      int x = 30, z = 30;
      int h = ItemTest.surface(s.level(), x, z);
      check(h > 0, "dig site found");

      s.level().setTile(x, h + 1, z, Blocks.SAND_ID);
      s.level().setTile(x, h + 2, z, Blocks.SAND_ID);
      s.level().setTile(x, h + 3, z, Blocks.SAND_ID);
      breakOnce(s, conn, x, h, z);
      check(s.fallingCount() == 3, "stack spawns 3 entities (" + s.fallingCount() + ")");
      for (int i = 0; i < s.fallingCount(); i++) {
         FallingBlock e = s.falling(i);
         boolean centered = false;
         for (int c = h + 1; c <= h + 3; c++) {
            if (Math.abs(e.y - (c + 0.5F)) < 0.15F) {
               centered = true;
            }
         }
         check(centered, "entity rides its cell center (y=" + e.y + ")");
      }
      settle(s, conn, 400);
      check(s.level().getTile(x, h, z) == Blocks.SAND_ID, "stack base lands on the break");
      check(s.level().getTile(x, h + 1, z) == Blocks.SAND_ID, "stack middle follows");
      check(s.level().getTile(x, h + 2, z) == Blocks.SAND_ID, "stack top follows");
      check(s.level().getTile(x, h + 3, z) == 0, "stack vacates the top");

      conn.sendToServer(new DebugGive());
      ItemTest.drain(s, conn, 3);

      int vx = x + 6;
      for (int sx = x + 6; sx < x + 120; sx += 4) {
         if (s.level().generator().heightAt(sx, z) > TerrainGenerator.SEA_LEVEL + 1) {
            vx = sx;
            break;
         }
      }
      int vh = ItemTest.surface(s.level(), vx, z);
      for (int y = 0; y <= vh; y++) {
         s.level().setTile(vx, y, z, 0);
      }
      place(s, conn, vx, vh, z, Blocks.SAND_ID);
      ArrayList<Packet> packets = settle(s, conn, 500);
      check(s.level().getTile(vx, vh + 1, z) == 0, "void sand vacates");
      check(!sawDrop(packets, Blocks.SAND_ID), "void sand is lost, not dropped");

      int tx = x + 9;
      int th = ItemTest.surface(s.level(), tx, z);
      s.level().setTile(tx, th + 1, z, Blocks.TORCH_ID);
      for (int y = th + 2; y <= th + 5; y++) {
         s.level().setTile(tx, y, z, 0);
      }
      place(s, conn, tx, th + 4, z, Blocks.SAND_ID);
      ArrayList<Packet> packets3 = settle(s, conn, 400);
      check(s.level().getTile(tx, th + 1, z) == Blocks.SAND_ID, "sand takes the torch cell");
      check(sawDrop(packets3, Blocks.TORCH_ID), "landed-on torch pops as a drop");

      int gx = x + 12;
      int gh = ItemTest.surface(s.level(), gx, z);
      s.level().setTile(gx, gh + 1, z, Blocks.GRAVEL_ID);
      s.level().setTile(gx, gh + 2, z, Blocks.GRAVEL_ID);
      breakOnce(s, conn, gx, gh, z);
      settle(s, conn, 400);
      check(s.level().getTile(gx, gh, z) == Blocks.GRAVEL_ID, "gravel falls to the break");
      check(s.level().getTile(gx, gh + 1, z) == Blocks.GRAVEL_ID, "gravel stack follows");
      check(s.level().getTile(gx, gh + 2, z) == 0, "gravel vacates the top");

      int wx = x + 18;
      int wh = ItemTest.surface(s.level(), wx, z);
      for (int dx = -6; dx <= 6; dx++) {
         for (int dz = -6; dz <= 6; dz++) {
            s.level().setTile(wx + dx, wh + 1, z + dz, Blocks.DIRT_ID);
            s.level().setTile(wx + dx, wh + 2, z + dz, Blocks.DIRT_ID);
            s.level().setTile(wx + dx, wh + 3, z + dz, Blocks.DIRT_ID);
         }
      }
      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            s.level().setTile(wx + dx, wh + 2, z + dz, Blocks.WATER_ID);
            s.level().setTile(wx + dx, wh + 3, z + dz, Blocks.WATER_ID);
         }
      }
      conn.sendToServer(new DebugGive());
      settle(s, conn, 5);
      place(s, conn, wx, wh + 2, z, Blocks.SAND_ID);
      settle(s, conn, 400);
      check(s.level().getTile(wx, wh + 2, z) == Blocks.SAND_ID, "grain settles on the seabed");
      check(s.level().getTile(wx, wh + 3, z) == Blocks.WATER_ID, "surface refills behind it");

      int bx = x - 24;
      int bh = ItemTest.surface(s.level(), bx, z);
      for (int dx = -2; dx <= 2; dx++) {
         s.level().setTile(bx + dx, bh, z, Blocks.DIRT_ID);
         for (int y = bh + 1; y <= bh + 4; y++) {
            s.level().setTile(bx + dx, y, z, 0);
         }
      }
      s.level().setTile(bx - 1, bh + 2, z, Blocks.GRAVEL_ID);
      s.level().setTile(bx, bh + 2, z, Blocks.GRAVEL_ID);
      s.level().setTile(bx + 1, bh + 2, z, Blocks.GRAVEL_ID);
      breakOnce(s, conn, bx, bh + 2, z);
      check(s.fallingCount() == 2, "neighbors cascade off one break (" + s.fallingCount() + ")");
      settle(s, conn, 400);
      check(s.level().getTile(bx, bh + 2, z) == 0, "mined middle stays out");
      check(s.level().getTile(bx - 1, bh + 1, z) == Blocks.GRAVEL_ID, "left neighbor lands one lower");
      check(s.level().getTile(bx + 1, bh + 1, z) == Blocks.GRAVEL_ID, "right neighbor lands one lower");

      int cx = bx + 10;
      int ch = ItemTest.surface(s.level(), cx, z);
      for (int dx = -2; dx <= 2; dx++) {
         s.level().setTile(cx + dx, ch, z, Blocks.DIRT_ID);
         for (int y = ch + 1; y <= ch + 4; y++) {
            s.level().setTile(cx + dx, y, z, 0);
         }
      }
      s.level().setTile(cx - 1, ch + 2, z, Blocks.GRAVEL_ID);
      s.level().setTile(cx, ch + 2, z, Blocks.GRAVEL_ID);
      s.level().setTile(cx + 1, ch + 2, z, Blocks.GRAVEL_ID);
      for (int y = bh + 3; y <= bh + 11; y++) {
         s.level().setTile(bx, y, z, 0);
      }
      s.level().setTile(bx, bh + 10, z, Blocks.GRAVEL_ID);
      breakOnce(s, conn, bx, bh, z);
      check(s.fallingCount() == 0, "settled rig breaks clean (" + s.fallingCount() + ")");
      settle(s, conn, 100);
      check(s.level().getTile(cx - 1, ch + 2, z) == Blocks.GRAVEL_ID, "far beam left untouched");
      check(s.level().getTile(cx, ch + 2, z) == Blocks.GRAVEL_ID, "far beam middle untouched");
      check(s.level().getTile(cx + 1, ch + 2, z) == Blocks.GRAVEL_ID, "far beam right untouched");
      check(s.level().getTile(bx, bh + 10, z) == Blocks.GRAVEL_ID, "high floater hangs on");

      if (failures == 0) System.out.println("SAND PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

