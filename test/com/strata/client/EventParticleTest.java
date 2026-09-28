package com.strata.client;

import com.strata.blocks.Blocks;
import com.strata.world.Level;
import java.util.ArrayList;

public class EventParticleTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) {
      Level l = new Level(64);
      ParticleEngine engine = new ParticleEngine(l);

      engine.addEmber(0.5F, 40.5F, 0.5F);
      engine.addBubble(10.5F, 40.5F, 10.5F);
      check(engine.count() == 2, "ember + bubble spawn");
      engine.tick();
      check(engine.count() == 3, "young lava pop trails smoke");

      engine.addPuff(20.5F, 40.5F, 20.5F, Blocks.particleTile(Blocks.DIRT_ID));
      check(engine.count() == 8, "puff adds 5");

      int x = 30, z = 30;
      int h = 0;
      for (int y = 63; y >= 0; y--) {
         int t = l.getTile(x, y, z);
         if (t > 0 && t != 13 && t != 14 && t != 15 && t != 16 && t != 17 && t != 20) { h = y; break; }
      }
      l.setTile(x, h + 1, z, Blocks.TORCH_ID);
      ArrayList<int[]> near = l.emittersNear(x + 0.5F, h + 1.5F, z + 0.5F, 12.0F, 6);
      boolean found = false;
      for (int[] e : near) {
         if (e[0] == x && e[1] == h + 1 && e[2] == z) found = true;
      }
      check(found, "scan finds the placed torch");
      ArrayList<int[]> far = l.emittersNear(x + 100.5F, h + 1.5F, z + 0.5F, 12.0F, 6);
      check(far.isEmpty(), "scan finds nothing 100 away");

      Particle up = new Particle(l, 0.5F, 40.0F, 0.5F, 0.0F, 0.0F, 0.0F,
         Blocks.particleTile(Blocks.TORCH_ID));
      up.gravity = -0.012F;
      for (int i = 0; i < 5; i++) {
         up.tick();
      }
      check(up.y > 40.0F, "negative gravity rises (y=" + up.y + ")");

      float[] fuv = ParticleAtlas.uv(ParticleEngine.FLAME_SPRITE);
      int[] fr = ParticleAtlas.rectPx(ParticleEngine.FLAME_SPRITE);
      check(fuv[0] == fr[0] / 256.0F && fuv[1] == fr[1] / 256.0F
         && fuv[2] == (fr[0] + fr[2]) / 256.0F && fuv[3] == (fr[1] + fr[3]) / 256.0F,
         "flame UV = packed flame rect");
      float[] buv = ParticleAtlas.uv(ParticleEngine.BUBBLE_SPRITE);
      int[] br = ParticleAtlas.rectPx(ParticleEngine.BUBBLE_SPRITE);
      check(buv[0] == br[0] / 256.0F && buv[2] == (br[0] + br[2]) / 256.0F,
         "bubble UV = packed bubble rect");
      check(ParticleAtlas.indexOf("flame.png") == ParticleEngine.FLAME_SPRITE, "flame resolves by name");
      Particle flame = Particle.sprite(l, 0.5F, 40.0F, 0.5F, 0.0F, 0.02F, 0.0F, ParticleEngine.FLAME_SPRITE);
      check(flame.particleSheet, "sheet sprite flags the sheet batch");

      Particle smoke = Particle.sprite(l, 0.5F, 40.0F, 0.5F, 0.0F, 0.0F, 0.0F, ParticleEngine.SMOKE_SPRITE);
      smoke.smokeAnim = true;
      smoke.lifetime = 40;
      smoke.tick();
      float[] seven = ParticleAtlas.uv(ParticleAtlas.smokeIndex(7));
      check(Math.abs(smoke.u0 - seven[0]) < 1e-6F, "smoke starts at smoke7");
      for (int i = 0; i < 20; i++) {
         smoke.tick();
      }
      float[] mid = ParticleAtlas.uv(ParticleAtlas.smokeIndex(3));
      check(Math.abs(smoke.u0 - mid[0]) < 1e-6F && Math.abs(smoke.v0 - mid[1]) < 1e-6F,
         "smoke rewinds toward smoke0 (u=" + smoke.u0 + ")");

      ParticleEngine trailEngine = new ParticleEngine(l);
      trailEngine.addBubble(0.5F, 40.5F, 0.5F);
      trailEngine.tick();
      check(trailEngine.count() == 2, "young lava pop trails smoke");

      Level flat = new Level(64);
      int fx = 40, fz = 40;
      int fh = 0;
      for (int y = 63; y >= 0; y--) {
         int t = flat.getTile(fx, y, fz);
         if (t > 0 && t != 13 && t != 14 && t != 15 && t != 16 && t != 17 && t != 20) { fh = y; break; }
      }
      flat.setTile(fx, fh + 1, fz, Blocks.LAVA_ID);
      ParticleEngine lavaEngine = new ParticleEngine(flat);
      lavaEngine.addBubble(fx + 0.5F, fh + 2.5F, fz + 0.5F);
      lavaEngine.sample(0).yd = 0.0F;
      lavaEngine.sample(0).trailSmoke = false;
      int diedAt = -1;
      for (int i = 1; i <= 40; i++) {
         lavaEngine.tick();
         if (lavaEngine.count() == 0) { diedAt = i; break; }
      }
      check(diedAt > 0, "lava touchdown kills the pop (died at tick " + diedAt + ")");
      int sx = fx + 8;
      int sh = 0;
      for (int y = 63; y >= 0; y--) {
         int t = flat.getTile(sx, y, fz);
         if (t > 0 && t != 13 && t != 14 && t != 15 && t != 16 && t != 17 && t != 20) { sh = y; break; }
      }
      ParticleEngine stoneEngine = new ParticleEngine(flat);
      stoneEngine.addBubble(sx + 0.5F, sh + 2.5F, fz + 0.5F);
      stoneEngine.sample(0).yd = 0.0F;
      stoneEngine.sample(0).trailSmoke = false;
      for (int i = 0; i < 30; i++) {
         stoneEngine.tick();
      }
      check(stoneEngine.count() == 1, "stone touchdown persists (no lava to die on)");

      int n = ParticleAtlas.SPRITES.length;
      check(n == 10, "ten particle sprites");
      for (int i = 0; i < n; i++) {
         int[] r = ParticleAtlas.rectPx(i);
         check(r[2] > 0 && r[3] > 0, "sprite " + i + " has area");
         check(r[0] >= 0 && r[1] >= 0 && r[0] + r[2] <= 256 && r[1] + r[3] <= 256,
            "sprite " + i + " inside the sheet");
         for (int j = i + 1; j < n; j++) {
            int[] q = ParticleAtlas.rectPx(j);
            boolean overlap = r[0] < q[0] + q[2] && q[0] < r[0] + r[2]
               && r[1] < q[1] + q[3] && q[1] < r[1] + r[3];
            check(!overlap, "sprites " + i + " and " + j + " disjoint");
         }
      }
      for (int f = 0; f < 7; f++) {
         check(ParticleAtlas.smokeIndex(f + 1) == ParticleAtlas.smokeIndex(f) + 1,
            "smoke frames contiguous");
      }

      if (failures == 0) System.out.println("EVENTPART PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

