package com.strata.world.storage;

import com.strata.net.LocalConnection;
import com.strata.server.GameServer;
import com.strata.world.WorldMeta;
import com.strata.world.gen.TerrainGenerator;
import java.io.File;
import java.io.FileOutputStream;

public class WorldSlotTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

    public static void main(String[] args) throws Exception {
      com.strata.server.ItemTest.wipeDir(new File("freshworld"));
      com.strata.server.ItemTest.wipeDir(new File("slotA"));
      com.strata.server.ItemTest.wipeDir(new File("slotB"));
      com.strata.server.ItemTest.wipeDir(new File("slotC"));
      File fresh = new File("freshworld");
      WorldMeta m0 = WorldMeta.load(fresh);
      check(m0.seed == TerrainGenerator.DEFAULT_SEED, "default seed");
      check(m0.time == 0L, "default time");

      WorldMeta.save(fresh, 987654321L, 12345L, true,
         new float[]{1.5F, 64.0F, -2.5F}, "pixelart");
      WorldMeta back = WorldMeta.load(fresh);
      check(back.seed == 987654321L, "seed roundtrip");
      check(back.time == 12345L, "time roundtrip");
      check(back.voidWorld, "void flag roundtrips");
      check(back.hasSpawn && back.spawnX == 1.5F && back.spawnY == 64.0F && back.spawnZ == -2.5F, "spawn roundtrips");
      check("pixelart".equals(back.pack), "pack roundtrips");

      FileOutputStream corrupt = new FileOutputStream(WorldMeta.fileFor(fresh));
      corrupt.write(new byte[]{1, 2, 3, 4, 5});
      corrupt.close();
      WorldMeta mCorrupt = WorldMeta.load(fresh);
      check(mCorrupt.seed == TerrainGenerator.DEFAULT_SEED, "corrupt defaults seed");

      WorldMeta.save(fresh, 555L, 77L, false, null, "");
      File dst = WorldMeta.fileFor(fresh);
      File tmp = new File(fresh, "world.dat.tmp");
      java.nio.file.Files.copy(dst.toPath(), tmp.toPath(),
         java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      check(dst.delete(), "crash staged");
      WorldMeta mCrash = WorldMeta.load(fresh);
      check(mCrash.seed == 555L && mCrash.time == 77L, "tmp recovers seed/time");

      File wa = new File("slotA");
      File wb = new File("slotB");
      GameServer sa = new GameServer(new LocalConnection(), wa, 111L);
      GameServer sb = new GameServer(new LocalConnection(), wb, 222L);
      boolean differ = false;
      for (int x = 0; x < 64 && !differ; x += 4) {
         for (int z = 0; z < 64 && !differ; z += 4) {
            for (int y = 30; y < 55; y++) {
               if (sa.level().getTile(x, y, z) != sb.level().getTile(x, y, z)) {
                  differ = true;
                  break;
               }
            }
         }
      }
      check(differ, "different seeds grow different terrain");

      for (int i = 0; i < 5; i++) sa.tick();
      check(sa.timeOfDay() == 5L, "clock ticks (" + sa.timeOfDay() + ")");
      sa.save();
      GameServer reloaded = new GameServer(new LocalConnection(), wa, null);
      check(reloaded.seed() == 111L, "seed persists (" + reloaded.seed() + ")");
      check(reloaded.timeOfDay() == 5L, "time persists (" + reloaded.timeOfDay() + ")");

      GameServer legacy = new GameServer(new LocalConnection());
      check(legacy.seed() == TerrainGenerator.DEFAULT_SEED, "legacy default seed");

      File slotC = new File("slotC");
      GameServer s1 = new GameServer(new LocalConnection(), slotC, 333L);
      int gx = 12, gz = 12;
      int gy = 63;
      while (gy > 0 && s1.level().getTile(gx, gy, gz) == 0) gy--;
      check(gy > 1, "dig site found");
      s1.level().setTile(gx, gy, gz, 0);
      s1.level().setTile(gx, gy - 1, gz, 0);
      s1.save();
      GameServer s2 = new GameServer(new LocalConnection(), slotC, null);
      check(s2.level().getTile(gx, gy, gz) == 0, "break survives reboot");
      check(s2.level().getTile(gx, gy - 1, gz) == 0, "second break survives");
      check(!s2.snapshot().columns.isEmpty(), "snapshot carries edits after reboot");

      if (failures == 0) System.out.println("WORLDSLOT PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

