package com.strata.server;

import com.strata.blocks.Blocks;
import com.strata.net.InventorySync;
import com.strata.net.ItemSpawn;
import com.strata.net.LocalConnection;
import com.strata.net.Packet;
import com.strata.net.PlaceBlock;
import java.io.File;
import java.util.ArrayList;

public class TorchRuleTest {
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

   static int stockTorch(GameServer s) {
      InventorySync inv = s.inventoryState();
      int n = 0;
      for (int i = 0; i < Inventory.SLOTS; i++) {
         if (inv.blocks[i] == Blocks.TORCH_ID) n += inv.counts[i];
      }
      return n;
   }

   static void place(GameServer s, LocalConnection conn, int x, int y, int z, int id) {
      PlaceBlock p = new PlaceBlock();
      p.x = x;
      p.y = y;
      p.z = z;
      p.face = 1;
      p.blockId = id;
      conn.sendToServer(p);
      ItemTest.drain(s, conn, 3);
   }

   public static void main(String[] args) {
      File dir = new File("torchworld");
      LocalConnection conn = new LocalConnection();
      GameServer s = new GameServer(conn, dir, 999L);
      int x = 30, z = 30;
      int h = ItemTest.surface(s.level(), x, z);
      check(h > 0, "dig site found");

      s.level().setTile(x, h + 1, z, Blocks.TORCH_ID);
      ArrayList<Packet> packets = ItemTest.breakBlock(s, conn, x, h, z);
      check(s.level().getTile(x, h + 1, z) == 0, "torch pops when support mined");
      check(sawDrop(packets, Blocks.TORCH_ID), "popped torch drops itself");

      int x2 = x + 3;
      int h2 = ItemTest.surface(s.level(), x2, z);
      s.level().setTile(x2, h2 + 1, z, Blocks.TORCH_ID);
      ArrayList<Packet> packets2 = ItemTest.breakBlock(s, conn, x2, h2 + 1, z);
      check(s.level().getTile(x2, h2 + 1, z) == 0, "torch mines directly");
      check(sawDrop(packets2, Blocks.TORCH_ID), "mined torch drops itself");

      int fx = x + 6;
      int fh = ItemTest.surface(s.level(), fx, z);
      s.level().setTile(fx, fh, z, 0);
      s.level().setTile(fx, fh + 1, z, 0);
      int before = stockTorch(s);
      check(before > 0, "starter torches in stock (" + before + ")");
      place(s, conn, fx, fh, z, Blocks.TORCH_ID);
      check(s.level().getTile(fx, fh + 1, z) != Blocks.TORCH_ID, "floating torch rejected");
      check(stockTorch(s) == before, "rejected placement spends nothing");

      s.level().setTile(fx, fh, z, Blocks.DIRT_ID);
      place(s, conn, fx, fh, z, Blocks.TORCH_ID);
      check(s.level().getTile(fx, fh + 1, z) == Blocks.TORCH_ID, "supported torch lands");
      check(stockTorch(s) == before - 1, "accepted placement spends one");

      if (failures == 0) System.out.println("TORCHRULE PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

