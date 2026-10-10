package com.strata.server;

import com.strata.blocks.Blocks;
import com.strata.blocks.MeshBuilder;
import com.strata.client.MobRenderer;
import com.strata.core.MathHelper;
import com.strata.net.AttackMob;
import com.strata.net.ItemRemove;
import com.strata.net.LocalConnection;
import com.strata.net.MobHurt;
import com.strata.net.MobSpawn;
import com.strata.net.Packet;
import com.strata.net.TimeCycle;
import java.io.File;
import java.util.ArrayList;

public class MobTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static void checkF(float got, float want, String msg) {
      if (Math.abs(got - want) > 1e-3) {
         failures++; System.out.println("FAIL: " + msg + " got=" + got + " want=" + want);
      }
   }

   static float[] partCenter(float[] v, int part) {
      float x = 0, y = 0, z = 0;
      for (int i = part * 24; i < part * 24 + 24; i++) {
         x += v[i * 3];
         y += v[i * 3 + 1];
         z += v[i * 3 + 2];
      }
      return new float[]{x / 24.0F, y / 24.0F, z / 24.0F};
   }

   static ArrayList<Packet> drain(GameServer s, LocalConnection conn, int ticks) {
      ArrayList<Packet> out = new ArrayList<>();
      com.strata.net.InputState idle = new com.strata.net.InputState();
      for (int i = 0; i < ticks; i++) {
         conn.sendToServer(idle);
         s.tick();
         Packet p;
         while ((p = conn.pollClient()) != null) {
            out.add(p);
         }
      }
      return out;
   }

   static void toMidnight(GameServer s, LocalConnection conn) {
      for (int i = 0; i < 3; i++) {
         conn.sendToServer(new TimeCycle());
         drain(s, conn, 3);
      }
   }

   static int pad(GameServer s, int x, int z) {
      int h = ItemTest.surface(s.level(), x, z);
      for (int dx = -3; dx <= 3; dx++) {
         for (int dz = -3; dz <= 3; dz++) {
            s.level().setTile(x + dx, h, z + dz, Blocks.DIRT_ID);
            for (int i = 1; i <= 6; i++) {
               s.level().setTile(x + dx, h + i, z + dz, 0);
            }
         }
      }
      return h;
   }

   static void park(Zombie z, float x, float y, float zz) {
      float w = 0.3F;
      z.bb.x0 = x - w;
      z.bb.y0 = y;
      z.bb.z0 = zz - w;
      z.bb.x1 = x + w;
      z.bb.y1 = y + 1.8F;
      z.bb.z1 = zz + w;
      z.x = x;
      z.y = y + 1.62F;
      z.z = zz;
      z.xd = 0.0F;
      z.yd = 0.0F;
      z.zd = 0.0F;
   }

   static MobSpawn findSpawn(ArrayList<Packet> packets) {
      for (Packet p : packets) {
         if (p instanceof MobSpawn) {
            return (MobSpawn)p;
         }
      }
      return null;
   }

   static boolean present(GameServer s, int id) {

      for (int i = 0; i < s.mobCount(); i++) {
         if (s.mob(i).id == id) {
            return true;
         }
      }
      return false;
   }

   static float[] armCenter(float fx, float fy, float fz, float px, float rz) {
      float cosX = (float)Math.cos(MobRenderer.ARM_RAISE);
      float sinX = (float)Math.sin(MobRenderer.ARM_RAISE);
      float y1 = -0.25F * cosX;
      float z1 = -0.25F * sinX;
      float cosZ = (float)Math.cos(rz);
      float sinZ = (float)Math.sin(rz);
      return new float[]{fx + px - y1 * sinZ, fy + 1.375F + y1 * cosZ, fz + z1};
   }

   public static void main(String[] args) {
      {
         File dir = new File("mobworld-spawn");
         ItemTest.wipeDir(dir);
         LocalConnection conn = new LocalConnection();
         GameServer s = new GameServer(conn, dir, 444L);
         drain(s, conn, 150);
         check(s.mobCount() == 0, "day spawns nothing (" + s.mobCount() + ")");
         toMidnight(s, conn);
         ArrayList<Packet> spawned = drain(s, conn, 1500);
         check(s.mobCount() > 0, "night spawns mobs (" + s.mobCount() + ")");
         check(s.mobCount() <= 6, "spawn cap holds (" + s.mobCount() + ")");
         check(findSpawn(spawned) != null, "spawns ride MobSpawn packets");
         s.player().teleport(s.player().x + 200.0F, s.player().bb.y0, s.player().z);
         drain(s, conn, 5);
         check(s.mobCount() == 0, "far mobs despawn (" + s.mobCount() + ")");
      }

      {
         File dir = new File("mobworld-melee");
         ItemTest.wipeDir(dir);
         LocalConnection conn = new LocalConnection();
         GameServer s = new GameServer(conn, dir, 444L);
         int x = 30, z = 30;
         int h = pad(s, x, z);
         s.player().teleport(x + 0.5F, h + 1.0F, z + 0.5F);
         drain(s, conn, 10);
         float px = s.player().x, pz = s.player().z;
         s.spawnZombie(px + 10.0F, h + 1.0F, pz);
         float d0 = Math.abs(s.mob(0).x - px) + Math.abs(s.mob(0).z - pz);
         drain(s, conn, 120);
         float d1 = Math.abs(s.mob(0).x - px) + Math.abs(s.mob(0).z - pz);
         check(d1 < d0, "walker closes distance (" + d0 + " -> " + d1 + ")");
         check(d0 - d1 < 4.5F, "walker at vanilla pace, not player speed (closed " + (d0 - d1) + ")");
         s.spawnZombie(px + 0.5F, h + 1.0F, pz);
         Zombie biter = s.mob(1);
         int hp0 = s.player().hp;
         drain(s, conn, 30);
         check(s.player().hp == hp0 - 4, "contact bites once (" + hp0 + " -> " + s.player().hp + ")");
         s.player().teleport(px, h + 1.0F, pz);
         park(biter, px + 0.5F, h + 1.0F, pz);
         drain(s, conn, 70);
         check(s.player().hp == hp0 - 8, "cooldown paces bites (" + s.player().hp + ")");
      }

      {
         File dir = new File("mobworld-sight");
         ItemTest.wipeDir(dir);
         LocalConnection conn = new LocalConnection();
         GameServer s = new GameServer(conn, dir, 444L);
         int x = 60, z = 60;
         int h = pad(s, x, z);
         s.player().teleport(x + 0.5F, h + 1.0F, z + 0.5F);
         drain(s, conn, 10);
         float px = s.player().x, pz = s.player().z;
         s.level().setTile(MathHelper.floor(px + 1.0F), h + 1, MathHelper.floor(pz), Blocks.DIRT_ID);
         s.level().setTile(MathHelper.floor(px + 1.0F), h + 2, MathHelper.floor(pz), Blocks.DIRT_ID);
         s.spawnZombie(px + 2.0F, h + 1.0F, pz);
         int hp0 = s.player().hp;
         drain(s, conn, 70);
         check(s.player().hp == hp0, "wall blocks the bite (" + hp0 + " -> " + s.player().hp + ")");
         s.level().setTile(MathHelper.floor(px + 1.0F), h + 1, MathHelper.floor(pz), 0);
         s.level().setTile(MathHelper.floor(px + 1.0F), h + 2, MathHelper.floor(pz), 0);
         drain(s, conn, 70);
         check(s.player().hp == hp0 - 4, "open sight bites (" + hp0 + " -> " + s.player().hp + ")");
      }

      {
         File dir = new File("mobworld-notice");
         ItemTest.wipeDir(dir);
         LocalConnection conn = new LocalConnection();
         GameServer s = new GameServer(conn, dir, 444L);
         toMidnight(s, conn);
         int hz = pad(s, 80, 80);
         int hmid = pad(s, 80, 92);
         int hfar = pad(s, 80, 98);
         s.player().teleport(80.5F, hfar + 1.0F, 98.5F);
         s.spawnZombie(80.5F, hz + 1.0F, 80.5F);
         Zombie zid = s.mob(s.mobCount() - 1);
         float x0 = zid.x, z0 = zid.z;
         drain(s, conn, 120);
         check(Math.abs(zid.x - x0) + Math.abs(zid.z - z0) < 0.05F,
            "outside 16 it stands");
         float d0 = Math.abs(zid.x - s.player().x) + Math.abs(zid.z - s.player().z);
         s.player().teleport(80.5F, hmid + 1.0F, 92.5F);
         drain(s, conn, 120);
         float d1 = Math.abs(zid.x - s.player().x) + Math.abs(zid.z - s.player().z);
         check(d1 < d0, "inside 16 it engages (" + d0 + " -> " + d1 + ")");
      }

      {
         File dir = new File("mobworld-burn");
         ItemTest.wipeDir(dir);
         LocalConnection conn = new LocalConnection();
         GameServer s = new GameServer(conn, dir, 444L);
         int x = 40, z = 40;
         int h = pad(s, x, z);
         conn.sendToServer(new TimeCycle());
         drain(s, conn, 3);
         s.spawnZombie(x + 0.5F, h + 1.0F, z + 0.5F);
         drain(s, conn, 70);
         check(s.mob(0).hp == 19, "noon burns once (" + s.mob(0).hp + ")");
         conn.sendToServer(new TimeCycle());
         drain(s, conn, 3);
         conn.sendToServer(new TimeCycle());
         drain(s, conn, 3);
         int hp = s.mob(0).hp;
         drain(s, conn, 120);
         check(s.mob(0).hp == hp, "midnight burns nothing (" + hp + ")");
      }

      {
         File dir = new File("mobworld-punch");
         ItemTest.wipeDir(dir);
         LocalConnection conn = new LocalConnection();
         GameServer s = new GameServer(conn, dir, 444L);
         toMidnight(s, conn);
         int x = 50, z = 50;
         int h = pad(s, x, z);
         s.player().teleport(x + 0.5F, h + 1.0F, z + 0.5F);
         drain(s, conn, 5);
         s.spawnZombie(s.player().x + 1.5F, h + 1.0F, s.player().z);
         int id = s.mob(0).id;
         Zombie biter = s.mob(0);
         float px = s.player().x, pz = s.player().z;
         float zx0 = s.mob(0).x;
         AttackMob punch = new AttackMob();
         punch.entityId = id;
         conn.sendToServer(punch);
         ArrayList<Packet> out = drain(s, conn, 3);
         boolean sawHurt = false;
         for (Packet p : out) {
            if (p instanceof MobHurt && ((MobHurt)p).entityId == id) {
               sawHurt = true;
               check(((MobHurt)p).hp == 19, "hurt syncs hp");
            }
         }
         check(sawHurt, "punch answers MobHurt");
         check(s.mob(0).hp == 19, "punch deals 1 (" + s.mob(0).hp + ")");
         check(Math.abs(s.mob(0).x - zx0) > 0.01F || s.mob(0).xd != 0.0F, "punch knocks back");
         conn.sendToServer(punch);
         ArrayList<Packet> out2 = drain(s, conn, 3);
         boolean sawHurt2 = false;
         for (Packet p : out2) {
            if (p instanceof MobHurt && ((MobHurt)p).entityId == id) {
               sawHurt2 = true;
            }
         }
         check(!sawHurt2 && s.mob(0).hp == 19, "immunity holds the window");
         for (int i = 0; i < 30 && present(s, id); i++) {
            s.player().hp = 20;
            s.player().teleport(px, h + 1.0F, pz);
            park(biter, px + 1.5F, h + 1.0F, pz);
            conn.sendToServer(punch);
            drain(s, conn, 61);
         }
         check(!present(s, id), "death removes + mirrors");
         conn.sendToServer(punch);
         ArrayList<Packet> out3 = drain(s, conn, 3);
         boolean sawHurt3 = false;
         for (Packet p : out3) {
            if (p instanceof MobHurt && ((MobHurt)p).entityId == id) {
               sawHurt3 = true;
            }
         }
         check(!sawHurt3, "dead ids ignored");

      }

      {
         File dir = new File("mobworld-mirror");
         ItemTest.wipeDir(dir);
         LocalConnection conn = new LocalConnection();
         GameServer s = new GameServer(conn, dir, 444L);
         toMidnight(s, conn);
         int h = pad(s, 30, 30);
         s.player().teleport(30.5F, h + 1.0F, 27.5F);
         com.strata.client.MobRenderer mirror =
            new com.strata.client.MobRenderer(s.level());
         mirror.spawn(7, 30.5F, h + 1.0F, 30.5F);
         check(mirror.get(7) != null, "mirror tracks spawn");
         float x0 = mirror.get(7).x;
         com.strata.core.AABB box = s.player().bb;
         for (int i = 0; i < 60; i++) {
            mirror.tick(box, s.player().x, s.player().y, s.player().z, s.timeOfDay());
         }
          check(mirror.get(7).x != x0 || mirror.get(7).z != 30.5F, "mirror integrates motion");
          check(mirror.get(7).hp == 20, "mirror ticks never damage");
          check(mirror.get(7).walkPhase > 0.0F, "stride accumulates from displacement");
         mirror.hurt(7, 12, 0.5F, 0.0F);
         check(mirror.get(7).hp == 12, "mirror applies hurt hp");
         check(mirror.get(7).xd == 0.5F, "mirror applies hurt velocity");
         mirror.hurt(99, 5, 0.0F, 0.0F);
         check(mirror.get(7).hp == 12, "unknown hurt ids ignored");
          mirror.remove(7);
          check(mirror.get(7) == null, "mirror drops on remove");
       }

       {
          MeshBuilder b = new MeshBuilder();
          b.init();
          int n = MobRenderer.emitZombie(b, 10.0F, 20.0F, 30.0F,
             0.0F, 0.0F, 0.0F, 1.0F);
          check(n == 144, "biped emits 144 verts (" + n + ")");
          check(b.count() == 144, "builder holds biped");
          float[] v = b.vertices();
          float[] t = b.texCoords();
          for (int i = 0; i < 144; i++) {
             check(t[i * 2] >= 0.0F && t[i * 2] <= 1.0F
                && t[i * 2 + 1] >= 0.0F && t[i * 2 + 1] <= 1.0F,
                "biped uv inside skin");
          }
          float[][] centers = {{9.875F, 20.375F, 30.0F}, {10.125F, 20.375F, 30.0F},
             {10.0F, 21.125F, 30.0F},
             armCenter(10.0F, 20.0F, 30.0F, -0.3125F, -MobRenderer.ARM_SPLAY),
             armCenter(10.0F, 20.0F, 30.0F, 0.3125F, MobRenderer.ARM_SPLAY),
             {10.0F, 21.75F, 30.0F}};
          for (int q = 0; q < 36; q++) {
             float e1x = v[(q * 4 + 1) * 3] - v[q * 4 * 3];
             float e1y = v[(q * 4 + 1) * 3 + 1] - v[q * 4 * 3 + 1];
             float e1z = v[(q * 4 + 1) * 3 + 2] - v[q * 4 * 3 + 2];
             float e2x = v[(q * 4 + 2) * 3] - v[q * 4 * 3];
             float e2y = v[(q * 4 + 2) * 3 + 1] - v[q * 4 * 3 + 1];
             float e2z = v[(q * 4 + 2) * 3 + 2] - v[q * 4 * 3 + 2];
             float nx = e1y * e2z - e1z * e2y;
             float ny = e1z * e2x - e1x * e2z;
             float nz = e1x * e2y - e1y * e2x;
             float mx = 0.0F, my = 0.0F, mz = 0.0F;
             for (int k = 0; k < 4; k++) {
                mx += v[(q * 4 + k) * 3];
                my += v[(q * 4 + k) * 3 + 1];
                mz += v[(q * 4 + k) * 3 + 2];
             }
             float[] c = centers[q / 6];
             float dot = nx * (mx / 4.0F - c[0]) + ny * (my / 4.0F - c[1])
                + nz * (mz / 4.0F - c[2]);
             check(dot > 0.0F, "biped quad " + q + " winds outward");
          }
          float minY = v[1], maxY = v[1];
          for (int i = 0; i < 144; i++) {
             if (v[i * 3 + 1] < minY) minY = v[i * 3 + 1];
             if (v[i * 3 + 1] > maxY) maxY = v[i * 3 + 1];
          }
          check(Math.abs(minY - 20.0F) < 1e-3, "feet on the box bottom");
          check(Math.abs(maxY - 22.0F) < 1e-3, "2.0 tall (head overhangs the 1.8 box, vanilla-authentic)");
          for (int i = 140; i < 144; i++) {
             check(t[i * 2] >= 8.0F / 64.0F - 1e-6 && t[i * 2] <= 16.0F / 64.0F + 1e-6
                && t[i * 2 + 1] >= 8.0F / 64.0F - 1e-6 && t[i * 2 + 1] <= 16.0F / 64.0F + 1e-6,
                "head front wears the face");
          }
          int[][] boneRects = {{0, 16, 4, 4}, {0, 16, 4, 4}, {16, 16, 8, 4},
             {40, 16, 4, 4}, {40, 16, 4, 4}, {0, 0, 8, 8}};
          for (int p = 0; p < 6; p++) {
             int tx = boneRects[p][0], ty = boneRects[p][1];
             int w = boneRects[p][2], d = boneRects[p][3];
             for (int i = (p * 6 + 1) * 4; i < (p * 6 + 1) * 4 + 4; i++) {
                float bu = t[i * 2] * 64.0F, bv = t[i * 2 + 1] * 64.0F;
                check(bu >= tx + d - 1e-3F && bu <= tx + d + w + 1e-3F
                   && bv >= ty - 1e-3F && bv <= ty + d + 1e-3F,
                   "part " + p + " bottom on vanilla rect (got u=" + bu + " v=" + bv + ")");
             }
          }
          MeshBuilder b2 = new MeshBuilder();
          b2.init();
          MobRenderer.emitZombie(b2, 10.0F, 20.0F, 30.0F,
             0.0F, 0.35F, 1.0F, 1.0F);
          float[] v2 = b2.vertices();
          boolean stepped = false;
          for (int i = 0; i < 144 * 3; i++) {
             if (Math.abs(v[i] - v2[i]) > 1e-4) { stepped = true; break; }
          }
          check(stepped, "stride swings legs");
          b2.init();
          MobRenderer.emitZombie(b2, 10.0F, 20.0F, 30.0F,
             (float)Math.PI / 2.0F, 0.0F, 0.0F, 1.0F);
          float[] v3 = b2.vertices();
          float noseX = (v3[140 * 3] + v3[141 * 3] + v3[142 * 3] + v3[143 * 3]) / 4.0F;
          check(noseX > 10.0F, "yaw turns the nose (+x at pi/2)");
          b2.init();
          MobRenderer.emitZombie(b2, 10.0F, 20.0F, 30.0F,
             (float)Math.PI * 2.0F, 0.0F, 0.0F, 1.0F);
          float[] v4 = b2.vertices();
          boolean back = true;
          for (int i = 0; i < 144 * 3; i++) {
             if (Math.abs(v[i] - v4[i]) > 1e-3) { back = false; break; }
          }
          check(back, "full yaw returns");
          b2.init();
          MobRenderer.emitZombie(b2, 10.0F, 20.0F, 30.0F,
             (float)Math.PI / 2.0F, 0.0F, 0.0F, 1.0F);
          float[] v5 = b2.vertices();
          float[] ra = partCenter(v5, 3);
          float[] la = partCenter(v5, 4);
          checkF(ra[0], 10.25F, "facing+x: right arm leads at +0.25");
          checkF(la[0], 10.25F, "facing+x: left arm leads at +0.25");
          checkF(ra[2], 29.625F, "facing+x: right arm on -z side");
          checkF(la[2], 30.375F, "facing+x: left arm on +z side");
          float[] rl = partCenter(v5, 0);
          float[] ll = partCenter(v5, 1);
          checkF(rl[2], 29.875F, "facing+x: right leg on -z side");
          checkF(ll[2], 30.125F, "facing+x: left leg on +z side");
          checkF(rl[0], 10.0F, "facing+x: right leg on travel axis");
          checkF(ll[0], 10.0F, "facing+x: left leg on travel axis");
       }

       if (failures == 0) System.out.println("MOB PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

