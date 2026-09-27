package com.strata.server;

import com.strata.blocks.Blocks;
import com.strata.core.AABB;
import com.strata.net.InputState;
import com.strata.net.InventorySync;
import com.strata.net.ItemRemove;
import com.strata.net.ItemSpawn;
import com.strata.net.LocalConnection;
import com.strata.net.Packet;
import com.strata.net.PlaceBlock;
import com.strata.net.TileUpdate;
import com.strata.world.Level;
import java.io.File;
import java.util.ArrayList;

public class ItemTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static int surface(Level l, int x, int z) {
      for (int y = 63; y >= 0; y--) {
         int t = l.getTile(x, y, z);
         if (t > 0 && t != 13 && t != 14 && t != 15 && t != 16 && t != 17 && t != 20) return y;
      }
      return -1;
   }

   static ArrayList<Packet> breakBlock(GameServer server, LocalConnection conn, int x, int y, int z) {
      ArrayList<Packet> out = new ArrayList<>();
      InputState in = new InputState();
      in.breaking = true;
      in.breakX = x;
      in.breakY = y;
      in.breakZ = z;
      in.breakFace = 1;
      for (int i = 0; i < 30; i++) {
         conn.sendToServer(in);
         server.tick();
         Packet p;
         while ((p = conn.pollClient()) != null) {
            out.add(p);
         }
         if (server.level().getTile(x, y, z) == 0) {
            break;
         }
      }
      return out;
   }

   static void drain(GameServer server, LocalConnection conn, int ticks) {
      InputState idle = new InputState();
      for (int i = 0; i < ticks; i++) {
         conn.sendToServer(idle);
         server.tick();
         while (conn.pollClient() != null) {
         }
      }
   }

   public static void main(String[] args) {
      Inventory inv = new Inventory();
      check(inv.add(Blocks.DIRT_ID, 70), "70 fits two slots");
      check(inv.slots[0].blockId == Blocks.DIRT_ID && inv.slots[0].count == 64, "first stack full");
      check(inv.slots[1].blockId == Blocks.DIRT_ID && inv.slots[1].count == 6, "spill stacks");
      check(inv.consume(0, 1) == 1 && inv.slots[0].count == 63, "consume one");
      check(inv.consume(1, 6) == 6 && inv.slots[1].isEmpty(), "consume clears slot");
      for (int i = 0; i < Inventory.SLOTS; i++) inv.add(Blocks.STONE_ID + (i % 3), 64);
      check(!inv.add(Blocks.GOLD_ID, 1), "full stock refuses");
      Inventory copy = new Inventory();
      int[] b = new int[Inventory.SLOTS];
      int[] c = new int[Inventory.SLOTS];
      for (int i = 0; i < Inventory.SLOTS; i++) { b[i] = inv.slots[i].blockId; c[i] = inv.slots[i].count; }
      copy.applySync(b, c);
      check(copy.sameAs(inv), "sync roundtrips stock");

      Inventory box = new Inventory();
      ItemStack hand = new ItemStack();
      box.slots[0] = new ItemStack(Blocks.DIRT_ID, 10);
      box.applyClick(0, hand);
      check(hand.blockId == Blocks.DIRT_ID && hand.count == 10 && box.slots[0].isEmpty(), "click picks up");
      box.applyClick(1, hand);
      check(hand.isEmpty() && box.slots[1].blockId == Blocks.DIRT_ID
         && box.slots[1].count == 10, "click places into empty");
      box.slots[2] = new ItemStack(Blocks.DIRT_ID, 60);
      hand.blockId = Blocks.DIRT_ID;
      hand.count = 10;
      box.applyClick(2, hand);
      check(box.slots[2].count == 64 && hand.count == 6, "merge caps, leftover held");
      box.slots[3] = new ItemStack(Blocks.STONE_ID, 5);
      box.applyClick(3, hand);
      check(box.slots[3].blockId == Blocks.DIRT_ID && box.slots[3].count == 6
         && hand.blockId == Blocks.STONE_ID && hand.count == 5, "mismatch swaps");
      box.applyClick(99, hand);
      check(hand.blockId == Blocks.STONE_ID && hand.count == 5, "wild slot ignored");

      check(Blocks.dropId(Blocks.GRASS_ID) == Blocks.DIRT_ID, "grass drops dirt");
      check(Blocks.dropId(Blocks.STONE_ID) == Blocks.COBBLE_ID, "stone drops cobble");
      check(Blocks.dropId(Blocks.DIRT_ID) == Blocks.DIRT_ID, "dirt drops self");
      check(Blocks.dropId(0) == 0, "air drops nothing");
      check(Blocks.dropId(200) == 0, "unknown drops nothing");
      for (int i = 0; i < 40; i++) {
         int d = Blocks.dropId(Blocks.LEAF_ID);
         if (d != 0 && d != Blocks.SAPLING_ID) {
            check(false, "leaves drop sapling or nothing");
            break;
         }
      }

      File dir = new File("itemworld");
      LocalConnection conn = new LocalConnection();
      GameServer s = new GameServer(conn, dir, 444L);
      int bx = 30, bz = 30;
      int bh = surface(s.level(), bx, bz);
      check(bh > 0, "dig site found");
      int broken = s.level().getTile(bx, bh, bz);
      int wantDrop = Blocks.dropId(broken);
      ArrayList<Packet> packets = breakBlock(s, conn, bx, bh, bz);
      ItemSpawn spawn = null;
      for (Packet p : packets) {
         if (p instanceof ItemSpawn) spawn = (ItemSpawn)p;
      }
      if (wantDrop > 0) {
         check(spawn != null, "break spawns a drop");
         check(spawn != null && spawn.blockId == wantDrop, "drop is the table yield");
      }

      if (spawn != null) {
         Player pl = s.player();
         pl.x = spawn.x;
         pl.y = spawn.y;
         pl.z = spawn.z;
         pl.bb = new AABB(spawn.x - 0.3F, spawn.y - 0.9F, spawn.z - 0.3F,
            spawn.x + 0.3F, spawn.y + 0.9F, spawn.z + 0.3F);
         boolean removed = false;
         InventorySync lastSync = null;
         InputState idle = new InputState();
         for (int i = 0; i < 300 && !removed; i++) {
            conn.sendToServer(idle);
            s.tick();
            Packet p;
            while ((p = conn.pollClient()) != null) {
               if (p instanceof ItemRemove && ((ItemRemove)p).entityId == spawn.entityId) removed = true;
               if (p instanceof InventorySync) lastSync = (InventorySync)p;
            }
         }
         check(removed, "drop collected on touch");
         check(lastSync != null, "collect syncs stock");
         boolean stocked = false;
         if (lastSync != null) {
            for (int i = 0; i < Inventory.SLOTS; i++) {
               if (lastSync.blocks[i] == wantDrop && lastSync.counts[i] > 0) stocked = true;
            }
         }
         check(stocked, "drop lands in stock");

         int px = bx + 2, py = bh, pz = bz;
         s.level().setTile(px, py, pz, 0);
         s.level().setTile(px, py + 1, pz, 0);
         PlaceBlock place = new PlaceBlock();
         place.x = px;
         place.y = py;
         place.z = pz;
         place.face = 1;
         place.blockId = wantDrop;
         conn.sendToServer(place);
         drain(s, conn, 3);
         check(s.level().getTile(px, py + 1, pz) == wantDrop
            || s.level().getTile(px, py, pz) == wantDrop, "paid placement lands");
         PlaceBlock ghost = new PlaceBlock();
         ghost.x = px + 4;
         ghost.y = py;
         ghost.z = pz;
         ghost.face = 1;
         ghost.blockId = Blocks.DIAMOND_ID;
         conn.sendToServer(ghost);
         drain(s, conn, 3);
         check(s.level().getTile(px + 4, py + 1, pz) != Blocks.DIAMOND_ID, "ghost placement rejected");
      }

      s.save();
      GameServer re = new GameServer(new LocalConnection(), dir, null);
      InventorySync a = s.inventoryState();
      InventorySync after = re.inventoryState();
      boolean sameStock = true;
      for (int i = 0; i < Inventory.SLOTS; i++) {
         if (a.blocks[i] != after.blocks[i] || a.counts[i] != after.counts[i]) sameStock = false;
      }
      check(sameStock, "stock survives reboot");
      check(re.level().getTile(bx, bh, bz) == 0, "break survives reboot");

      File dir2 = new File("giveworld");
      LocalConnection conn2 = new LocalConnection();
      GameServer g = new GameServer(conn2, dir2, 777L);
      conn2.sendToServer(new com.strata.net.DebugGive());
      InventorySync kit = null;
      InputState idle2 = new InputState();
      for (int i = 0; i < 5 && kit == null; i++) {
         conn2.sendToServer(idle2);
         g.tick();
         Packet p;
         while ((p = conn2.pollClient()) != null) {
            if (p instanceof InventorySync) kit = (InventorySync)p;
         }
      }
      check(kit != null, "give syncs stock");
      if (kit != null) {
         int[] want = {Blocks.DIRT_ID, Blocks.STONE_ID, Blocks.COBBLE_ID,
            Blocks.TORCH_ID, Blocks.LEAF_ID, Blocks.SAND_ID, Blocks.PLANKS_ID};
         for (int id : want) {
            boolean found = false;
            for (int i = 0; i < Inventory.SLOTS; i++) {
               if (kit.blocks[i] == id && kit.counts[i] == 64) found = true;
            }
            check(found, "kit stocks " + id + " x64");
         }
         int filled = 0;
         for (int i = 0; i < Inventory.SLOTS; i++) {
            if (kit.blocks[i] != 0 && kit.counts[i] > 0) filled++;
         }
         check(filled == 7, "give replaces stock (" + filled + " filled)");
      }

      if (failures == 0) System.out.println("ITEM PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

