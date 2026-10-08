package com.strata.server;

import com.strata.blocks.Blocks;
import com.strata.net.DebugGive;
import com.strata.net.ItemSpawn;
import com.strata.net.LocalConnection;
import com.strata.net.Packet;
import com.strata.net.PlaceBlock;
import java.io.File;
import java.util.ArrayList;

public class PlaceRuleTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static int stock(GameServer s, int id) {
      com.strata.net.InventorySync inv = s.inventoryState();
      int n = 0;
      for (int i = 0; i < Inventory.SLOTS; i++) {
         if (inv.blocks[i] == id) n += inv.counts[i];
      }
      return n;
   }

   static void place(GameServer s, LocalConnection conn, int x, int y, int z, int face, int id) {
      PlaceBlock p = new PlaceBlock();
      p.x = x;
      p.y = y;
      p.z = z;
      p.face = face;
      p.blockId = id;
      conn.sendToServer(p);
      ItemTest.drain(s, conn, 3);
   }

   static boolean sawDrop(ArrayList<Packet> packets, int blockId) {
      for (Packet p : packets) {
         if (p instanceof ItemSpawn && ((ItemSpawn)p).blockId == blockId) return true;
      }
      return false;
   }

   static int pad(GameServer s, int x, int z, int ground) {
      int h = ItemTest.surface(s.level(), x, z);
      s.level().setTile(x, h, z, ground);
      for (int i = 1; i <= 4; i++) {
         s.level().setTile(x, h + i, z, 0);
      }
      return h;
   }

   public static void main(String[] args) {
      File dir = new File("placeworld");
      LocalConnection conn = new LocalConnection();
      GameServer s = new GameServer(conn, dir, 4242L);
      conn.sendToServer(new DebugGive());
      ItemTest.drain(s, conn, 3);

      int x = 30, z = 30;
      int h = pad(s, x, z, Blocks.DIRT_ID);
      int before = stock(s, Blocks.LILYPAD_ID);
      check(before > 0, "lilies in stock");
      place(s, conn, x, h, z, 1, Blocks.LILYPAD_ID);
      check(s.level().getTile(x, h + 1, z) != Blocks.LILYPAD_ID, "lily on dirt rejected");
      check(stock(s, Blocks.LILYPAD_ID) == before, "rejected lily spends nothing");
      s.level().setTile(x, h, z, Blocks.WATER_ID);
      place(s, conn, x, h, z, 1, Blocks.LILYPAD_ID);
      check(s.level().getTile(x, h + 1, z) == Blocks.LILYPAD_ID, "lily on water lands");
      check(stock(s, Blocks.LILYPAD_ID) == before - 1, "accepted lily spends one");
      ArrayList<Packet> packets = ItemTest.breakBlock(s, conn, x, h, z);
      check(s.level().getTile(x, h + 1, z) == 0, "lily pops when water goes");
      check(sawDrop(packets, Blocks.LILYPAD_ID), "popped lily drops itself");

      int fx = x + 6, fz = z;
      int fh = pad(s, fx, fz, Blocks.DIRT_ID);
      place(s, conn, fx, fh, fz, 1, Blocks.ROSE_ID);
      check(s.level().getTile(fx, fh + 1, fz) == Blocks.ROSE_ID, "rose on dirt in daylight lands");
      int dx = x + 12;
      int dh = pad(s, dx, fz, Blocks.DIRT_ID);
      s.level().setTile(dx + 1, dh + 1, fz, Blocks.DIRT_ID);
      s.level().setTile(dx - 1, dh + 1, fz, Blocks.DIRT_ID);
      s.level().setTile(dx, dh + 1, fz + 1, Blocks.DIRT_ID);
      s.level().setTile(dx, dh + 1, fz - 1, Blocks.DIRT_ID);
      s.level().setTile(dx, dh + 2, fz, Blocks.DIRT_ID);
      before = stock(s, Blocks.DANDELION_ID);
      place(s, conn, dx, dh, fz, 1, Blocks.DANDELION_ID);
      check(s.level().getTile(dx, dh + 1, fz) != Blocks.DANDELION_ID, "flower in darkness rejected");
      check(stock(s, Blocks.DANDELION_ID) == before, "dark rejection spends nothing");
      s.level().setTile(fx, fh + 1, fz, 0);
      s.level().setTile(fx, fh, fz, Blocks.STONE_ID);
      place(s, conn, fx, fh, fz, 1, Blocks.ROSE_ID);
      check(s.level().getTile(fx, fh + 1, fz) != Blocks.ROSE_ID, "rose on stone rejected");

      int rx = x + 18;
      int rh = pad(s, rx, fz, Blocks.SAND_ID);
      s.level().setTile(rx + 1, rh, fz, Blocks.DIRT_ID);
      s.level().setTile(rx - 1, rh, fz, Blocks.DIRT_ID);
      s.level().setTile(rx, rh, fz + 1, Blocks.DIRT_ID);
      s.level().setTile(rx, rh, fz - 1, Blocks.DIRT_ID);
      before = stock(s, Blocks.REED_ID);
      place(s, conn, rx, rh, fz, 1, Blocks.REED_ID);
      check(s.level().getTile(rx, rh + 1, fz) != Blocks.REED_ID, "reed without water rejected");
      check(stock(s, Blocks.REED_ID) == before, "dry reed spends nothing");
      s.level().setTile(rx + 1, rh, fz, Blocks.WATER_ID);
      place(s, conn, rx, rh, fz, 1, Blocks.REED_ID);
      check(s.level().getTile(rx, rh + 1, fz) == Blocks.REED_ID, "reed at water edge lands");
      s.level().setTile(rx, rh + 2, fz, Blocks.REED_ID);
      ArrayList<Packet> rpack = ItemTest.breakBlock(s, conn, rx + 1, rh, fz);
      check(s.level().getTile(rx, rh + 1, fz) == 0, "reed pops when water goes");
      check(s.level().getTile(rx, rh + 2, fz) == 0, "reed column cascades");
      check(sawDrop(rpack, Blocks.REED_ID), "popped reed drops");

      int cx = x + 24;
      int ch = pad(s, cx, fz, Blocks.GRASS_ID);
      s.level().setTile(cx + 1, ch + 1, fz, 0);
      s.level().setTile(cx - 1, ch + 1, fz, 0);
      s.level().setTile(cx, ch + 1, fz + 1, 0);
      s.level().setTile(cx, ch + 1, fz - 1, 0);
      before = stock(s, Blocks.CACTUS_ID);
      place(s, conn, cx, ch, fz, 1, Blocks.CACTUS_ID);
      check(s.level().getTile(cx, ch + 1, fz) != Blocks.CACTUS_ID, "cactus on grass rejected");
      s.level().setTile(cx, ch, fz, Blocks.SAND_ID);
      place(s, conn, cx, ch, fz, 1, Blocks.CACTUS_ID);
      check(s.level().getTile(cx, ch + 1, fz) == Blocks.CACTUS_ID, "cactus on sand lands");
      s.level().setTile(cx + 1, ch + 1, fz, Blocks.STONE_ID);
      ArrayList<Packet> cpack = ItemTest.breakBlock(s, conn, cx, ch + 1, fz);
      check(s.level().getTile(cx, ch + 1, fz) == 0, "re-placing broken cactus clears it");
      place(s, conn, cx, ch, fz, 1, Blocks.CACTUS_ID);
      check(s.level().getTile(cx, ch + 1, fz) != Blocks.CACTUS_ID, "cactus hugging stone rejected");
      s.level().setTile(cx + 1, ch + 1, fz, 0);

      int sx = x + 30;
      int sh = pad(s, sx, fz, Blocks.DIRT_ID);
      s.level().setTile(sx, sh + 1, fz, 0);
      before = stock(s, Blocks.SNOW_LAYER_ID);
      for (int i = 0; i <= 4; i++) {
         s.level().setTile(sx + 7, sh + i, fz, 0);
      }
      place(s, conn, sx + 7, sh + 5, fz, 0, Blocks.SNOW_LAYER_ID);
      check(s.level().getTile(sx + 7, sh + 4, fz) != Blocks.SNOW_LAYER_ID, "floating snow rejected");
      place(s, conn, sx, sh, fz, 1, Blocks.SNOW_LAYER_ID);
      check(s.level().getTile(sx, sh + 1, fz) == Blocks.SNOW_LAYER_ID, "snow on dirt lands");
      check(s.level().getData(sx, sh + 1, fz) == 0, "fresh snow is thin");
      place(s, conn, sx, sh + 1, fz, 1, Blocks.SNOW_LAYER_ID);
      check(s.level().getTile(sx, sh + 1, fz) == Blocks.SNOW_LAYER_ID, "snow stacks on snow");
      check(s.level().getData(sx, sh + 1, fz) == 1, "stacked snow thickens");
      check(stock(s, Blocks.SNOW_LAYER_ID) == before - 2, "two snows spent");
      check(!s.level().isSolidTile(sx, sh + 1, fz), "thin snow walk-through");
      s.level().setTile(sx, sh + 1, fz, Blocks.stateOf(Blocks.byId(Blocks.SNOW_LAYER_ID), 3));
      check(s.level().isSolidTile(sx, sh + 1, fz), "stacked snow collides");

      int bx = x + 36;
      int bh = pad(s, bx, fz, Blocks.GRASS_ID);
      place(s, conn, bx, bh, fz, 1, Blocks.DEADBUSH_ID);
      check(s.level().getTile(bx, bh + 1, fz) != Blocks.DEADBUSH_ID, "deadbush on grass rejected");
      s.level().setTile(bx, bh, fz, Blocks.SAND_ID);
      place(s, conn, bx, bh, fz, 1, Blocks.DEADBUSH_ID);
      check(s.level().getTile(bx, bh + 1, fz) == Blocks.DEADBUSH_ID, "deadbush on sand lands");
      int mx = x + 42;
      int mh = pad(s, mx, fz, Blocks.DIRT_ID);
      place(s, conn, mx, mh, fz, 1, Blocks.MUSHROOM_BROWN_ID);
      check(s.level().getTile(mx, mh + 1, fz) != Blocks.MUSHROOM_BROWN_ID, "mushroom in daylight rejected");
      s.level().setTile(mx, mh, fz, Blocks.MYCELIUM_ID);
      place(s, conn, mx, mh, fz, 1, Blocks.MUSHROOM_BROWN_ID);
      check(s.level().getTile(mx, mh + 1, fz) == Blocks.MUSHROOM_BROWN_ID, "mushroom on mycelium lands");

      int vx = x + 48;
      int vh = pad(s, vx, fz, Blocks.DIRT_ID);
      for (int ox = -1; ox <= 1; ox++) {
         for (int oz = -1; oz <= 1; oz++) {
            s.level().setTile(vx + ox, vh + 1, fz + oz, 0);
            s.level().setTile(vx + ox, vh + 2, fz + oz, 0);
         }
      }
      place(s, conn, vx, vh, fz, 1, Blocks.VINE_ID);
      check(s.level().getTile(vx, vh + 1, fz) != Blocks.VINE_ID, "vine mid-air rejected");
      s.level().setTile(vx + 1, vh + 1, fz, Blocks.DIRT_ID);
      place(s, conn, vx + 1, vh + 1, fz, 4, Blocks.VINE_ID);
      check(s.level().getTile(vx, vh + 1, fz) == Blocks.VINE_ID, "vine on wall lands");
       int wx = x + 54;
       int wh = pad(s, wx, fz, Blocks.DIRT_ID);
       for (int ox = -1; ox <= 2; ox++) {
          for (int oz = -1; oz <= 1; oz++) {
             for (int oy = 1; oy <= 6; oy++) {
                s.level().setTile(wx + ox, wh + oy, fz + oz, 0);
             }
          }
       }
       s.level().setTile(wx + 1, wh + 1, fz, Blocks.DIRT_ID);
      place(s, conn, wx + 1, wh + 1, fz, 4, Blocks.TORCH_ID);
      check(s.level().getTile(wx, wh + 1, fz) == Blocks.TORCH_ID, "wall torch mounts");
      check(s.level().getData(wx, wh + 1, fz) == 4, "wall torch faces its wall");
      ArrayList<Packet> wpack = ItemTest.breakBlock(s, conn, wx + 1, wh + 1, fz);
      check(s.level().getTile(wx, wh + 1, fz) == 0, "wall torch pops with its wall");
      check(sawDrop(wpack, Blocks.TORCH_ID), "popped wall torch drops");
      place(s, conn, wx, wh + 1, fz, 0, Blocks.TORCH_ID);
      check(s.level().getTile(wx, wh, fz) == Blocks.DIRT_ID, "ceiling torch rejected (ground untouched)");

      if (failures == 0) System.out.println("PLACERULE PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

