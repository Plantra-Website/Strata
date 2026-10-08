package com.strata.server;

import com.strata.blocks.Blocks;
import com.strata.net.LocalConnection;
import java.io.File;
import static com.strata.world.Level.WATER_TICKS;

public class WaterTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) {
      ItemTest.wipeDir(new File("waterworld"));
      File dir = new File("waterworld");
      LocalConnection conn = new LocalConnection();
      GameServer s = new GameServer(conn, dir, 7777L);
      int x = 30, z = 30;
      int h = ItemTest.surface(s.level(), x, z);
      check(h > 0, "dig site found");

      s.level().setTile(x, h + 3, z, Blocks.WATER_ID);
      s.level().setTile(x, h + 1, z, 0);
      s.level().setTile(x, h + 2, z, 0);
      s.level().setTile(x + 1, h + 3, z, Blocks.DIRT_ID);
      SandTest.breakOnce(s, conn, x + 1, h + 3, z);
      ItemTest.drain(s, conn, WATER_TICKS + 20);
      check(s.level().getTile(x, h + 2, z) == Blocks.WATER_ID, "down fill lands");
      check(s.level().getData(x, h + 2, z) == 8, "down fill flags falling");

      int sx = x + 10;
      int sh = ItemTest.surface(s.level(), sx, z);
      for (int i = -9; i <= 9; i++) {
         s.level().setTile(sx + i, sh + 1, z, Blocks.DIRT_ID);
         s.level().setTile(sx + i, sh + 2, z - 1, Blocks.DIRT_ID);
         s.level().setTile(sx + i, sh + 2, z + 1, Blocks.DIRT_ID);
         s.level().setTile(sx + i, sh + 2, z, 0);
      }
      s.level().setTile(sx, sh + 2, z, Blocks.WATER_ID);
      s.level().scheduleTick(sx, sh + 2, z, WATER_TICKS);
      ItemTest.drain(s, conn, WATER_TICKS + 20);
      check(s.level().getTile(sx + 1, sh + 2, z) == Blocks.WATER_ID, "side spread lands +x");
      check(s.level().getData(sx + 1, sh + 2, z) == 1, "side spread costs +1");
      ItemTest.drain(s, conn, WATER_TICKS * 8 + 40);
      check(s.level().getTile(sx + 7, sh + 2, z) == Blocks.WATER_ID, "range reaches +7");
      check(s.level().getData(sx + 7, sh + 2, z) == 7, "range level caps at 7");
      check(s.level().getTile(sx + 8, sh + 2, z) == 0, "range stops before +8");

      int tx = x + 24;
      int th = ItemTest.surface(s.level(), tx, z);
      s.level().setTile(tx, th + 1, z, Blocks.DIRT_ID);
      s.level().setTile(tx, th + 2, z, Blocks.WATER_ID);
      s.level().setTile(tx + 1, th + 2, z, Blocks.TORCH_ID);
      s.level().scheduleTick(tx, th + 2, z, WATER_TICKS);
      ItemTest.drain(s, conn, WATER_TICKS + 20);
      check(s.level().getTile(tx + 1, th + 2, z) == Blocks.WATER_ID, "torch washes out");

      int ix = x + 32;
      int ih = ItemTest.surface(s.level(), ix, z);
      for (int dx = -4; dx <= 4; dx++) {
         for (int dz = -4; dz <= 4; dz++) {
            s.level().setTile(ix + dx, ih + 1, z + dz, Blocks.DIRT_ID);
            s.level().setTile(ix + dx, ih + 2, z + dz, 0);
         }
      }
      s.level().setTile(ix - 1, ih + 2, z, Blocks.WATER_ID);
      s.level().setTile(ix + 1, ih + 2, z, Blocks.WATER_ID);
      s.level().setTile(ix, ih + 3, z, Blocks.WATER_ID);
      s.level().scheduleTick(ix, ih + 3, z, WATER_TICKS);
      ItemTest.drain(s, conn, WATER_TICKS + 20);
      check(s.level().getTile(ix, ih + 2, z) == Blocks.WATER_ID, "middle dives in");
      s.level().setTile(ix, ih + 3, z, 0);
      s.level().scheduleTick(ix, ih + 2, z, WATER_TICKS);
      ItemTest.drain(s, conn, WATER_TICKS + 20);
      check(s.level().getData(ix, ih + 2, z) == 0, "middle becomes source");

      int mx = x + 44;
      int mh = ItemTest.surface(s.level(), mx, z);
      s.level().setTile(mx, mh + 1, z, Blocks.DIRT_ID);
      s.level().setTile(mx + 1, mh + 1, z, Blocks.DIRT_ID);
      s.level().setTile(mx, mh + 2, z, Blocks.LAVA_ID);
      s.level().setTile(mx + 1, mh + 2, z, Blocks.WATER_ID);
      s.level().scheduleTick(mx, mh + 2, z, com.strata.world.Level.LAVA_TICKS);
      ItemTest.drain(s, conn, com.strata.world.Level.LAVA_TICKS + 20);
      check(s.level().getTile(mx, mh + 2, z) == Blocks.OBSIDIAN_ID, "lava source hardens to obsidian");
      check(s.level().getTile(mx + 1, mh + 2, z) == Blocks.WATER_ID, "water survives mixing");
      int fx = x + 48;
      int fh = ItemTest.surface(s.level(), fx, z);
      s.level().setTile(fx, fh + 1, z, Blocks.DIRT_ID);
      s.level().setTile(fx + 1, fh + 1, z, Blocks.DIRT_ID);
      s.level().setTile(fx, fh + 2, z, Blocks.stateOf(Blocks.byId(Blocks.LAVA_ID), 1));
      s.level().setTile(fx + 1, fh + 2, z, Blocks.WATER_ID);
      s.level().scheduleTick(fx, fh + 2, z, com.strata.world.Level.LAVA_TICKS);
      ItemTest.drain(s, conn, com.strata.world.Level.LAVA_TICKS + 20);
      check(s.level().getTile(fx, fh + 2, z) == Blocks.COBBLE_ID, "flowing lava hardens to cobble");

      s.save();
      GameServer re = new GameServer(new LocalConnection(), dir, null);
      check(re.level().getTile(sx + 7, sh + 2, z) == Blocks.WATER_ID, "flow tile persists");
      check(re.level().getData(sx + 7, sh + 2, z) == 7, "flow level persists");

       int ox = -1, oz = -1;
       for (int cx = 0; cx < 600 && ox < 0; cx += 4) {
          for (int cz = 0; cz < 600 && ox < 0; cz += 4) {
             if (s.level().generator().heightAt(cx, cz) < com.strata.world.gen.TerrainGenerator.SEA_LEVEL - 3
                && s.level().getTile(cx, com.strata.world.gen.TerrainGenerator.SEA_LEVEL - 1, cz) == Blocks.WATER_ID
                && s.level().getTile(cx, com.strata.world.gen.TerrainGenerator.SEA_LEVEL - 2, cz) == Blocks.WATER_ID) {
                ox = cx;
                oz = cz;
             }
          }
       }
      check(ox >= 0, "sea found");
      s.player().teleport(ox + 0.5F, com.strata.world.gen.TerrainGenerator.SEA_LEVEL - 1, oz + 0.5F);
      float y0 = s.player().y;
      ItemTest.drain(s, conn, 60);
      float y1 = s.player().y;
      check(y1 < y0 && y1 > y0 - 5, "idle sinks slowly (" + y0 + " -> " + y1 + ")");
      com.strata.net.InputState swim = new com.strata.net.InputState();
      swim.jump = true;
      for (int i = 0; i < 60; i++) {
         conn.sendToServer(swim);
         s.tick();
         while (conn.pollClient() != null) {
         }
      }
      check(s.player().y > y1, "held jump rises (" + y1 + " -> " + s.player().y + ")");

      if (failures == 0) System.out.println("WATER PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

