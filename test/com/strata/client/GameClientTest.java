package com.strata.client;

public class GameClientTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) {
      int[] p = GameClient.termGuiPos(0, 0, 320, 240, 160, 120, 1, 1);
      check(p != null && p[0] == 1 && p[1] == 238, "top-left cell maps, y flips");
      p = GameClient.termGuiPos(159, 59, 320, 240, 160, 120, 1, 1);
      check(p != null && p[0] == 319 && p[1] == 2, "bottom-right cell maps");
      p = GameClient.termGuiPos(0, 0, 320, 240, 320, 240, 2, 1);
      check(p != null && p[0] == 1 && p[1] == 238, "scale 2 top-left matches");
      p = GameClient.termGuiPos(0, 0, 320, 240, 160, 120, 1, 2);
      check(p != null && p[0] == 0 && p[1] == 119, "gui scale divides");
      check(GameClient.termGuiPos(-1, 0, 320, 240, 160, 120, 1, 1) == null, "unknown x rejected");
      check(GameClient.termGuiPos(0, -1, 320, 240, 160, 120, 1, 1) == null, "unknown y rejected");
      check(GameClient.termGuiPos(0, 0, 320, 240, 160, 120, 0, 1) == null, "bad scale rejected");
      check(GameClient.termGuiPos(0, 0, 320, 240, 160, 120, 1, 0) == null, "bad gui scale rejected");

      if (failures > 0) { System.out.println(failures + " FAILURES"); System.exit(1); }
      System.out.println("gameclient ok");
   }
}

