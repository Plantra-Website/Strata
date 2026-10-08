package com.strata.server;

import com.strata.core.Config;
import com.strata.net.InputState;
import com.strata.net.LocalConnection;
import com.strata.world.WorldMeta;
import java.io.File;

public class AutosaveTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) {
      ItemTest.wipeDir(new File("autosaveworld"));
      File dir = new File("autosaveworld");
      LocalConnection conn = new LocalConnection();
      GameServer s = new GameServer(conn, dir, 555L);
      InputState idle = new InputState();
      for (int i = 0; i < Config.AUTOSAVE_TICKS; i++) {
         conn.sendToServer(idle);
         s.tick();
         while (conn.pollClient() != null) {
         }
      }
      check(WorldMeta.load(dir).time == Config.AUTOSAVE_TICKS,
         "autosaved clock persists (got " + WorldMeta.load(dir).time + ")");
      GameServer re = new GameServer(new LocalConnection(), dir, null);
      check(re.timeOfDay() == Config.AUTOSAVE_TICKS, "reboot resumes autosaved clock");

      if (failures == 0) System.out.println("AUTOSAVE PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

