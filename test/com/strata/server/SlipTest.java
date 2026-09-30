package com.strata.server;

import com.strata.blocks.Blocks;
import com.strata.net.InputState;
import com.strata.net.LocalConnection;
import java.io.File;

public class SlipTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static void runway(GameServer s, int x, int z0, int id) {
      for (int z = z0 - 2; z < z0 + 48; z++) {
         for (int dx = -1; dx <= 1; dx++) {
            s.level().setTile(x + dx, 90, z, id);
            s.level().setTile(x + dx, 91, z, 0);
            s.level().setTile(x + dx, 92, z, 0);
            s.level().setTile(x + dx, 93, z, 0);
            s.level().setTile(x + dx, 94, z, 0);
         }
      }
   }

   static float sprint(GameServer s, LocalConnection conn, int x, int z0) {
      s.player().teleport(x + 0.5F, 95.0F, z0 + 0.5F);
      s.player().yRot = 180.0F;
      ItemTest.drain(s, conn, 40);
      float sx = s.player().x, sz = s.player().z;
      InputState fwd = new InputState();
      fwd.fwd = true;
      fwd.yaw = 180.0F;
      for (int i = 0; i < 80; i++) {
         conn.sendToServer(fwd);
         s.tick();
         while (conn.pollClient() != null) {
         }
      }
      float dz = s.player().z - sz;
      float dx = s.player().x - sx;
      return (float)Math.sqrt(dx * dx + dz * dz);
   }

   public static void main(String[] args) {
      File dir = new File("slipworld");
      LocalConnection conn = new LocalConnection();
      GameServer s = new GameServer(conn, dir, 555L);
      runway(s, 0, 0, Blocks.DIRT_ID);
      runway(s, 3, 0, Blocks.ICE_ID);
      float dirt = sprint(s, conn, 0, 0);
      float ice = sprint(s, conn, 3, 0);
      System.out.println("dirt=" + dirt + " ice=" + ice);
      check(dirt > 2.0F, "runner moves on dirt (" + dirt + ")");
      check(ice > dirt * 1.5F, "ice carries further (" + ice + " vs " + dirt + ")");

      if (failures == 0) System.out.println("SLIP PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

