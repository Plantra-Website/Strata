package com.strata.server;

import com.strata.net.BreakEffect;
import com.strata.net.InputState;
import com.strata.net.LocalConnection;
import com.strata.net.Packet;
import com.strata.net.TileUpdate;
import com.strata.world.Level;
import java.util.ArrayList;

public class MirrorTest {
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

   public static void main(String[] args) {
      LocalConnection conn = new LocalConnection();
      GameServer server = new GameServer(conn);
      Level client = new Level(64, false);
      Level sl = server.level();

      int bx = 30, bz = 30;
      int bh = surface(sl, bx, bz);
      check(bh > 0, "dig site found");

      int[][] targets = {{bx, bh, bz}, {bx, bh - 1, bz}, {bx, bh - 2, bz}, {bx + 1, bh - 2, bz}};
      boolean sawTileBeforeBurst = false;
      for (int[] t : targets) {
         ArrayList<Packet> packets = breakBlock(server, conn, t[0], t[1], t[2]);
         int tileIdx = -1, burstIdx = -1;
         for (int i = 0; i < packets.size(); i++) {
            Packet p = packets.get(i);
            if (p instanceof TileUpdate) {
               TileUpdate u = (TileUpdate)p;
               if (u.x == t[0] && u.y == t[1] && u.z == t[2] && u.type == 0 && tileIdx < 0) {
                  tileIdx = i;
               }
               client.setTile(u.x, u.y, u.z, u.type);
            }
         }
         for (int i = 0; i < packets.size(); i++) {
            if (packets.get(i) instanceof BreakEffect) {
               burstIdx = i;
               break;
            }
         }
         check(tileIdx >= 0, "break at " + t[0] + "," + t[1] + "," + t[2] + " produced TileUpdate");
         check(burstIdx >= 0, "break produced burst");
         if (tileIdx >= 0 && burstIdx >= 0) {
            check(tileIdx < burstIdx, "TileUpdate before burst (got tile@" + tileIdx + " burst@" + burstIdx + ")");
            sawTileBeforeBurst = sawTileBeforeBurst || tileIdx < burstIdx;
         }
      }
      check(sawTileBeforeBurst, "packet order holds across breaks");

      check(client.getBrightness(bx, bh - 2, bz) > 0.9f, "burst cell bright on client");

      int mism = 0;
      for (int x = bx - 4; x <= bx + 5; x++) {
         for (int z = bz - 4; z <= bz + 5; z++) {
            for (int y = 0; y < 64; y++) {
               if (sl.getTile(x, y, z) != client.getTile(x, y, z)
                  || sl.getSkyLevel(x, y, z) != client.getSkyLevel(x, y, z)
                  || sl.getBlockLevel(x, y, z) != client.getBlockLevel(x, y, z)) {
                  if (mism < 5) {
                     System.out.println("MISMATCH @" + x + "," + y + "," + z
                        + " server=" + sl.getTile(x, y, z) + "/" + sl.getSkyLevel(x, y, z) + "/" + sl.getBlockLevel(x, y, z)
                        + " client=" + client.getTile(x, y, z) + "/" + client.getSkyLevel(x, y, z) + "/" + client.getBlockLevel(x, y, z));
                  }
                  mism++;
               }
            }
         }
      }
      check(mism == 0, "mirror converges (" + mism + " mismatched cells)");

      if (failures > 0) { System.out.println(failures + " FAILURES"); System.exit(1); }
      System.out.println("MIRROR PASS");
   }
}

