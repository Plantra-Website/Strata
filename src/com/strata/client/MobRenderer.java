package com.strata.client;

import com.strata.blocks.MeshBuilder;
import com.strata.core.AABB;
import com.strata.core.MathHelper;
import com.strata.server.Zombie;
import com.strata.world.Level;
import com.strata.world.mesh.Tesselator;
import com.strata.world.mesh.Textures;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.opengl.GL11;

public class MobRenderer {
   private final Level level;
   private final Map<Integer, Zombie> mobs = new HashMap<>();
   private final MeshBuilder scratch = new MeshBuilder();

   static final String SKIN = "/textures/entity/zombie.png";
   static final float STRIDE = 0.6662F;
   static final float SWING = 1.4F;
   public static final float ARM_RAISE = (float)Math.PI / 2.0F;
   public static final float ARM_SPLAY = 0.05F;

   public MobRenderer(Level level) {
      this.level = level;
   }

   public void spawn(int entityId, float x, float y, float z) {
      this.mobs.put(entityId, new Zombie(entityId, x, y, z));
   }

   public void hurt(int entityId, int hp, float xd, float zd) {
      Zombie z = this.mobs.get(entityId);
      if (z == null) {
         return;
      }
      z.hp = hp;
      z.xd = xd;
      z.zd = zd;
   }

   public void remove(int entityId) {
      this.mobs.remove(entityId);
   }

   public Zombie get(int entityId) {
      return this.mobs.get(entityId);
   }

   public Collection<Zombie> mobs() {
      return this.mobs.values();
   }

   public void tick(AABB playerBox, float px, float py, float pz, long timeOfDay) {
      for (Zombie z : this.mobs.values()) {
         z.tick(this.level, px, py, pz, playerBox, timeOfDay, null);
         float br = this.level.getBrightness(MathHelper.floor(z.x),
            MathHelper.floor(z.bb.y0 + 1.0F), MathHelper.floor(z.z));
         z.r = br;
         z.g = br;
         z.b = br;
      }
   }

   public void render(float a) {
      if (this.mobs.isEmpty()) {
         return;
      }
      Textures.bind(Textures.loadTexture(SKIN, 9728));
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glDisable(GL11.GL_CULL_FACE);
      Tesselator t = Tesselator.SHARED;
      t.init();
      for (Zombie z : this.mobs.values()) {
         float ix = z.xo + (z.x - z.xo) * a;
         float iy = z.ybo + (z.bb.y0 - z.ybo) * a;
         float iz = z.zo + (z.z - z.zo) * a;
         float amountI = z.swingPrev + (z.swingAmount - z.swingPrev) * a;
         float phaseI = z.walkPhase - z.swingAmount * (1.0F - a);
         this.scratch.init();
         emitZombie(this.scratch, ix, iy, iz, z.facing, phaseI, amountI, z.r);
         t.drain(this.scratch);
      }
      t.flush();
      GL11.glEnable(GL11.GL_CULL_FACE);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
   }

   public static int emitZombie(MeshBuilder b, float fx, float fy, float fz,
         float facing, float walkPhase, float swingAmount, float brightness) {
      float swing = (float)Math.cos(walkPhase * STRIDE) * SWING * swingAmount;
      int n = 0;
      n += emitPart(b, 0, 16, 4, 12, 4, -0.125F, 0.375F, 0.0F,
         -0.125F, 0.75F, 0.0F, swing, 0.0F, fx, fy, fz, facing, brightness);
      n += emitPart(b, 0, 16, 4, 12, 4, 0.125F, 0.375F, 0.0F,
         0.125F, 0.75F, 0.0F, -swing, 0.0F, fx, fy, fz, facing, brightness);
      n += emitPart(b, 16, 16, 8, 12, 4, 0.0F, 1.125F, 0.0F,
         0.0F, 0.0F, 0.0F, 0.0F, 0.0F, fx, fy, fz, facing, brightness);
      n += emitPart(b, 40, 16, 4, 12, 4, -0.375F, 1.125F, 0.0F,
         -0.3125F, 1.375F, 0.0F, ARM_RAISE, -ARM_SPLAY, fx, fy, fz, facing, brightness);
      n += emitPart(b, 40, 16, 4, 12, 4, 0.375F, 1.125F, 0.0F,
         0.3125F, 1.375F, 0.0F, ARM_RAISE, ARM_SPLAY, fx, fy, fz, facing, brightness);
      n += emitPart(b, 0, 0, 8, 8, 8, 0.0F, 1.75F, 0.0F,
         0.0F, 0.0F, 0.0F, 0.0F, 0.0F, fx, fy, fz, facing, brightness);
      return n;
   }

   static int emitPart(MeshBuilder b, int tx, int ty, int w, int h, int d,
         float cx, float cy, float cz, float px, float py, float pz,
         float rotX, float rotZ, float fx, float fy, float fz,
         float facing, float brightness) {
      float hx = w / 32.0F, hy = h / 32.0F, hz = d / 32.0F;
      float[][] c = new float[8][3];
      int k = 0;
      for (int ix = 0; ix < 2; ix++) {
         for (int iy = 0; iy < 2; iy++) {
            for (int iz = 0; iz < 2; iz++) {
               float lx = cx + (ix == 0 ? -hx : hx) - px;
               float ly = cy + (iy == 0 ? -hy : hy) - py;
               float lz = cz + (iz == 0 ? -hz : hz) - pz;
               float cosX = (float)Math.cos(rotX), sinX = (float)Math.sin(rotX);
               float y1 = ly * cosX - lz * sinX;
               float z1 = ly * sinX + lz * cosX;
               float cosZ = (float)Math.cos(rotZ), sinZ = (float)Math.sin(rotZ);
               float x2 = lx * cosZ - y1 * sinZ;
               float y2 = lx * sinZ + y1 * cosZ;
               float cosF = (float)Math.cos(facing), sinF = (float)Math.sin(facing);
               c[k][0] = fx + px * cosF - pz * sinF + x2 * cosF - z1 * sinF;
               c[k][1] = fy + py + y2;
               c[k][2] = fz + px * sinF + pz * cosF + x2 * sinF + z1 * cosF;
               k++;
            }
         }
      }
      float uW = tx / 64.0F, uW1 = (tx + d) / 64.0F;
      float uE0 = (tx + d + w) / 64.0F, uE1 = (tx + d + w + d) / 64.0F;
      float uB0 = (tx + d + d + w) / 64.0F, uB1 = (tx + d + d + w + w) / 64.0F;
      float uT0 = (tx + d + w) / 64.0F, uT1 = (tx + d + w + w) / 64.0F;
      float uF1 = (tx + d + w) / 64.0F;
      float vTB0 = ty / 64.0F, vTB1 = (ty + d) / 64.0F;
      float vS0 = (ty + d) / 64.0F, vS1 = (ty + d + h) / 64.0F;
      int n = 0;
      n += quad(b, c[2], c[3], c[7], c[6], uT0, uT1, vTB0, vTB1, brightness);
      n += quad(b, c[0], c[4], c[5], c[1], uW1, uF1, vTB0, vTB1, brightness * 0.5F);
      n += quad(b, c[5], c[4], c[6], c[7], uE0, uE1, vS0, vS1, brightness * 0.6F);
      n += quad(b, c[1], c[3], c[2], c[0], uW, uW1, vS0, vS1, brightness * 0.6F);
      n += quad(b, c[1], c[5], c[7], c[3], uB0, uB1, vS0, vS1, brightness * 0.8F);
      n += quad(b, c[4], c[0], c[2], c[6], uW1, uF1, vS0, vS1, brightness * 0.8F);
      return n;
   }

   private static int quad(MeshBuilder b, float[] a, float[] q, float[] d, float[] e,
         float u0, float u1, float v0, float v1, float shade) {
      b.color(shade, shade, shade);
      b.tex(u0, v1);
      b.vertex(a[0], a[1], a[2]);
      b.tex(u1, v1);
      b.vertex(q[0], q[1], q[2]);
      b.tex(u1, v0);
      b.vertex(d[0], d[1], d[2]);
      b.tex(u0, v0);
      b.vertex(e[0], e[1], e[2]);
      return 4;
   }
}

