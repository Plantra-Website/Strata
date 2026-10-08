package com.strata.server;

import com.strata.blocks.Blocks;
import com.strata.net.LocalConnection;
import com.strata.net.PlaceBlock;
import com.strata.world.Level;
import java.io.File;

public class MeltTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
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

   public static void main(String[] args) {
      File dir = new File("meltworld");
      LocalConnection conn = new LocalConnection();
      GameServer s = new GameServer(conn, dir, 31337L);
      int x = 30, z = 30;
      int h = ItemTest.surface(s.level(), x, z);
      check(h > 0, "dig site found");

      s.level().setTile(x, h, z, Blocks.DIRT_ID);
      for (int i = 1; i <= 4; i++) {
         s.level().setTile(x, h + i, z, 0);
      }
      s.level().setTile(x + 1, h + 1, z, Blocks.ICE_ID);
      s.level().setTile(x - 1, h + 1, z, Blocks.SNOW_LAYER_ID);
      s.level().setTile(x + 8, h + 1, z, Blocks.ICE_ID);
      place(s, conn, x, h, z, 1, Blocks.TORCH_ID);
      check(s.level().getTile(x, h + 1, z) == Blocks.TORCH_ID, "torch lands");
      ItemTest.drain(s, conn, Level.MELT_TICKS + 40);
      check(s.level().getTile(x + 1, h + 1, z) == Blocks.WATER_ID, "ice melts to still water");
      check(s.level().getTile(x - 1, h + 1, z) == 0, "snow layer melts away");
      check(s.level().getTile(x + 8, h + 1, z) == Blocks.ICE_ID, "distant ice survives");

      s.level().setTile(x + 2, h + 1, z, Blocks.ICE_ID);
      s.level().setTile(x + 2, h + 2, z, Blocks.SAND_ID);
      s.level().armMeltCheck(x + 2, h + 1, z, 5);
      ItemTest.drain(s, conn, Level.MELT_TICKS + 40);
      check(s.level().getTile(x + 2, h + 1, z) == Blocks.SAND_ID, "sand sinks into the melt");
      check(s.level().getTile(x + 2, h + 2, z) == 0, "sand vacates the melt surface");

      if (failures == 0) System.out.println("MELT PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

