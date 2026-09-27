package com.strata.server;

import com.strata.blocks.Blocks;
import com.strata.net.InputState;
import com.strata.net.LocalConnection;
import java.io.File;

public class HardnessTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static int ticksToBreak(GameServer s, LocalConnection conn, int x, int y, int z) {
      InputState in = new InputState();
      in.breaking = true;
      in.breakX = x;
      in.breakY = y;
      in.breakZ = z;
      in.breakFace = 1;
      for (int i = 1; i <= 150; i++) {
         conn.sendToServer(in);
         s.tick();
         while (conn.pollClient() != null) {
         }
         if (s.level().getTile(x, y, z) == 0) {
            return i;
         }
      }
      return -1;
   }

   public static void main(String[] args) {
      check(Blocks.hardness(Blocks.BEDROCK_ID) <= 0, "bedrock unbreakable");
      check(Blocks.hardness(Blocks.LAVA_ID) <= 0, "lava unbreakable");
      check(Blocks.hardness(Blocks.DIRT_ID) < Blocks.hardness(Blocks.STONE_ID), "dirt faster than stone");
      check(Blocks.hardness(Blocks.STONE_ID) < Blocks.hardness(Blocks.DIAMOND_ID), "stone faster than diamond ore");
      check(Blocks.hardness(Blocks.TORCH_ID) < Blocks.hardness(Blocks.DIRT_ID), "torch instant-ish");

      File dir = new File("hardnessworld");
      LocalConnection conn = new LocalConnection();
      GameServer s = new GameServer(conn, dir, 777L);
      int x = 30, z = 30;
      int h = ItemTest.surface(s.level(), x, z);
      s.level().setTile(x, h + 1, z, Blocks.DIRT_ID);
      s.level().setTile(x + 1, h + 1, z, Blocks.STONE_ID);
      s.level().setTile(x + 2, h + 1, z, Blocks.BEDROCK_ID);
      int dirtTicks = ticksToBreak(s, conn, x, h + 1, z);
      int stoneTicks = ticksToBreak(s, conn, x + 1, h + 1, z);
      int rockTicks = ticksToBreak(s, conn, x + 2, h + 1, z);
      check(dirtTicks > 0 && dirtTicks <= 20, "dirt breaks fast (" + dirtTicks + " ticks)");
      check(stoneTicks > dirtTicks, "stone slower than dirt (" + stoneTicks + " ticks)");
      check(rockTicks < 0, "bedrock survives 150 ticks");
      check(s.level().getTile(x + 2, h + 1, z) == Blocks.BEDROCK_ID, "bedrock untouched");

      if (failures == 0) System.out.println("HARDNESS PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

