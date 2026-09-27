package com.strata.server;

import com.strata.blocks.Blocks;
import com.strata.net.LocalConnection;
import java.io.File;
import static com.strata.world.Level.LAVA_TICKS;

public class FlowTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) {
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
      check(s.level().getData(x, h + 2, z) == 0, "down fill keeps level");

      int sx = x + 10;
      int sh = ItemTest.surface(s.level(), sx, z);
      s.level().setTile(sx, sh + 1, z, Blocks.DIRT_ID);
      s.level().setTile(sx, sh + 2, z, Blocks.LAVA_ID);
      for (int i = 1; i <= 5; i++) {
         s.level().setTile(sx + i, sh + 1, z, Blocks.DIRT_ID);
         s.level().setTile(sx + i, sh + 2, z, 0);
      }
      s.level().scheduleTick(sx, sh + 2, z, LAVA_TICKS);
      ItemTest.drain(s, conn, LAVA_TICKS + 20);
      check(s.level().getTile(sx + 1, sh + 2, z) == Blocks.LAVA_ID, "side spread lands +x");
      check(s.level().getData(sx + 1, sh + 2, z) == 1, "side spread costs +1");
      check(s.level().getBlockLevel(sx + 1, sh + 2, z) == 15, "flow lights itself");

      ItemTest.drain(s, conn, LAVA_TICKS * 5 + 40);
      check(s.level().getTile(sx + 4, sh + 2, z) == Blocks.LAVA_ID, "range reaches +4");
      check(s.level().getData(sx + 4, sh + 2, z) == 4, "range level caps at 4");
      check(s.level().getTile(sx + 5, sh + 2, z) == 0, "range stops before +5");

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
      check(re.level().getTile(sx + 4, sh + 2, z) == Blocks.LAVA_ID, "flow tile persists");
      check(re.level().getData(sx + 4, sh + 2, z) == 4, "flow level persists");

      if (failures == 0) System.out.println("FLOW PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

