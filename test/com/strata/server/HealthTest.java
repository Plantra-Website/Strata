package com.strata.server;

import com.strata.blocks.Blocks;
import com.strata.core.MathHelper;
import com.strata.net.HurtSelf;
import com.strata.net.InputState;
import com.strata.net.LocalConnection;
import com.strata.net.Packet;
import com.strata.net.PlayerState;
import java.io.File;

public class HealthTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static PlayerState drain(GameServer s, LocalConnection conn, int ticks) {
      PlayerState last = null;
      InputState idle = new InputState();
      for (int i = 0; i < ticks; i++) {
         conn.sendToServer(idle);
         s.tick();
         Packet p;
         while ((p = conn.pollClient()) != null) {
            if (p instanceof PlayerState) last = (PlayerState)p;
         }
      }
      return last;
   }

   public static void main(String[] args) {
      File dir = new File("healthworld");
      LocalConnection conn = new LocalConnection();
      GameServer s = new GameServer(conn, dir, 4242L);
      Player pl = s.player();

      PlayerState st = drain(s, conn, 400);
      check(pl.onGround, "spawn settles");
      check(pl.hp == 20, "spawn costs nothing (hp=" + pl.hp + ")");
      int fx = MathHelper.floor(pl.x);
      int fy = MathHelper.floor(pl.bb.y0);
      int fz = MathHelper.floor(pl.z);
      check(!s.level().isSolidTile(fx, fy + 1, fz) && !s.level().isSolidTile(fx, fy + 2, fz),
         "spawn headroom (feet=" + fy + ")");
      check(st != null && st.hp == 20, "hp syncs full");

      float gy = pl.y;
      pl.teleport(pl.x, gy + 15.0F, pl.z);
      st = drain(s, conn, 400);
      check(pl.onGround, "fall settles");
      check(pl.hp < 20 && pl.hp > 0, "15-block fall damages (hp=" + pl.hp + ")");
      check(st != null && st.hp == pl.hp, "hp syncs damage");

      int lx = 60, lz = 60;
      int lh = ItemTest.surface(s.level(), lx, lz);
      s.level().setTile(lx, lh + 4, lz, Blocks.DIRT_ID);
      s.level().setTile(lx, lh + 5, lz, Blocks.LAVA_ID);
      pl.hp = 20;
      pl.teleport(lx + 0.5F, lh + 6.5F, lz + 0.5F);
      drain(s, conn, 100);
      check(pl.hp <= 12, "lava burns (hp=" + pl.hp + ")");

      int cx = 70, cz = 70;
      int ch = ItemTest.surface(s.level(), cx, cz);
      s.level().setTile(cx, ch + 1, cz, Blocks.CACTUS_ID);
      pl.hp = 20;
      pl.teleport(cx + 0.5F, ch + 3.0F, cz + 0.5F);
      drain(s, conn, 40);
      check(pl.hp < 20, "cactus pricks (hp=" + pl.hp + ")");
      pl.teleport(cx + 2.5F, ch + 1.6F, cz + 0.5F);
      pl.hp = 20;
      drain(s, conn, 30);
      check(pl.hp == 20, "clear air is safe (hp=" + pl.hp + ")");

      pl.hurt(100);
      drain(s, conn, 3);
      check(pl.hp == 20, "death restores health");
      check(pl.onGround, "death respawns grounded");

      conn.sendToServer(new HurtSelf());
      drain(s, conn, 3);
      check(pl.hp == 19, "slash deals 1 self damage (hp=" + pl.hp + ")");

      if (failures == 0) System.out.println("HEALTH PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

