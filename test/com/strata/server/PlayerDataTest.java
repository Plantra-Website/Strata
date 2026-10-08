package com.strata.server;

import com.strata.blocks.Blocks;
import com.strata.net.DebugGive;
import com.strata.net.InventorySync;
import com.strata.net.LocalConnection;
import com.strata.net.SlotClick;
import com.strata.world.PlayerData;
import java.io.File;

public class PlayerDataTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static void checkF(float got, float want, String msg) {
      if (Math.abs(got - want) > 1e-4) { failures++; System.out.println("FAIL: " + msg + " got=" + got); }
   }

   public static void main(String[] args) {
      ItemTest.wipeDir(new File("playerworld"));
      ItemTest.wipeDir(new File("playerworld_legacy"));
      File dir = new File("playerworld");
      LocalConnection conn = new LocalConnection();
      GameServer s = new GameServer(conn, dir, 60606L);
      Player pl = s.player();
      conn.sendToServer(new DebugGive());
      ItemTest.drain(s, conn, 3);
      SlotClick grab = new SlotClick();
      grab.slot = 0;
      conn.sendToServer(grab);
      ItemTest.drain(s, conn, 3);
      pl.teleport(12.5F, 48.0F, -7.5F);
      pl.yRot = 45.0F;
      pl.xRot = -10.0F;
      pl.hurt(7);
      s.save();

      check(PlayerData.fileFor(dir).isFile(), "player.dat bundled in the world dir");
      PlayerData stored = PlayerData.load(dir);
      check(stored.present, "player.dat present after save");
      GameServer re = new GameServer(new LocalConnection(), dir, null);
      Player rp = re.player();
      checkF(rp.x, 12.5F, "pos x resumes");
      checkF(rp.y, 49.62F, "pos y resumes");
      checkF(rp.bb.y0, 48.0F, "feet resume");
      checkF(rp.z, -7.5F, "pos z resumes");
      checkF(rp.yRot, 45.0F, "yaw resumes");
      checkF(rp.xRot, -10.0F, "pitch resumes");
      check(rp.hp == 13, "hp resumes (hp=" + rp.hp + ")");
      InventorySync stock = re.inventoryState();
      check(stock.blocks[0] == 0, "grabbed stack left its slot");
      check(stock.blocks[1] == Blocks.STONE_ID && stock.counts[1] == 64, "rest of stock rides player.dat");
      check(stock.heldBlock == Blocks.DIRT_ID && stock.heldCount == 64, "held stack rides player.dat");

      File legacy = new File("playerworld_legacy");
      GameServer first = new GameServer(new LocalConnection(), legacy, 60606L);
      first.save();
      PlayerData.fileFor(legacy).delete();
      check(!PlayerData.load(legacy).present, "deleted player.dat reads absent");
      GameServer scattered = new GameServer(new LocalConnection(), legacy, null);
      check(scattered.player().hp == 20, "legacy reboot at full health");

      if (failures == 0) System.out.println("PLAYERDATA PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

