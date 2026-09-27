package com.strata.client;

import com.strata.blocks.AtlasStitcher;
import com.strata.core.AABB;
import com.strata.core.MathHelper;
import com.strata.world.Level;
import com.strata.world.mesh.Tesselator;
import java.util.List;

public class Particle {
   private final Level level;
   public float xo;
   public float yo;
   public float zo;
   public float x;
   public float y;
   public float z;
   public float xd;
   public float yd;
   public float zd;
   public AABB bb;
   public boolean onGround = false;
   public int age = 0;
   public int lifetime = 0;
   public float size;
   public float u0;
   public float v0;
   public float u1;
   public float v1;
   public float r = 1.0F;
   public float g = 1.0F;
   public float b = 1.0F;
   public float gravity = 0.025F;

   public Particle(Level level, float x, float y, float z, float xd, float yd, float zd, int texIndex) {
      this(level, x, y, z, xd, yd, zd, atlasUV(texIndex), false);
   }

   public static Particle sprite(Level level, float x, float y, float z, float xd, float yd, float zd, int sprite) {
      return new Particle(level, x, y, z, xd, yd, zd, spriteUV(sprite), true);
   }

   private static float[] atlasUV(int texIndex) {
      float[] r = AtlasStitcher.tileRect(texIndex);
      float du = (float)Math.random() * 0.6F * r[2];
      float dv = (float)Math.random() * 0.6F * r[3];
      return new float[]{r[0] + du, r[1] + dv, r[0] + du + 0.25F * r[2], r[1] + dv + 0.25F * r[3]};
   }

   static float[] spriteUV(int sprite) {
      float u0 = (sprite % 16) * 8.0F / 128.0F;
      float v0 = (sprite / 16) * 8.0F / 128.0F;
      return new float[]{u0, v0, u0 + 8.0F / 128.0F, v0 + 8.0F / 128.0F};
   }

   public boolean particleSheet = false;

   public boolean smokeAnim = false;

   public boolean trailSmoke = false;
   public boolean shrink = false;
   public boolean lavaPop = false;

   private Particle(Level level, float x, float y, float z, float xd, float yd, float zd, float[] uv, boolean sheet) {
      this.level = level;
      this.x = x;
      this.y = y;
      this.z = z;
      this.xo = x;
      this.yo = y;
      this.zo = z;
      this.xd = xd + (float)(Math.random() * 2.0 - 1.0) * 0.04F;
      this.yd = yd + (float)(Math.random() * 2.0 - 1.0) * 0.04F;
      this.zd = zd + (float)(Math.random() * 2.0 - 1.0) * 0.04F;
      
      float speed = (float)(Math.random() + Math.random() + 1.0) * 0.15F;
      float d = (float)Math.sqrt(this.xd * this.xd + this.yd * this.yd + this.zd * this.zd);
      if (d > 0.001F) {
         this.xd = this.xd / d * speed * 0.15F;
         this.yd = this.yd / d * speed * 0.15F + 0.06F;
         this.zd = this.zd / d * speed * 0.15F;
      }

      float w = 0.05F;
      this.bb = new AABB(x - w, y - w, z - w, x + w, y + w, z + w);
      this.size = (float)(Math.random() * 0.08 + 0.04);
      this.lifetime = (int)(10.0 / (Math.random() * 0.8 + 0.2));

      this.u0 = uv[0];
      this.v0 = uv[1];
      this.u1 = uv[2];
      this.v1 = uv[3];
      this.particleSheet = sheet;
      
      float br = level.getBrightness(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
      this.r = br;
      this.g = br;
      this.b = br;
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;

      if (this.age++ >= this.lifetime) {
         return;
      }

      if (this.smokeAnim) {
         int f = 7 - this.age * 8 / Math.max(1, this.lifetime);
         if (f < 0) {
            f = 0;
         }
         float[] uv = spriteUV(f);
         this.u0 = uv[0];
         this.v0 = uv[1];
         this.u1 = uv[2];
         this.v1 = uv[3];
      }

      this.yd -= this.gravity;

      this.move(this.xd, this.yd, this.zd);

      this.xd *= 0.98F;
      this.yd *= 0.98F;
      this.zd *= 0.98F;
      if (this.onGround) {
         this.xd *= 0.7F;
         this.zd *= 0.7F;
      }
   }

   public void move(float xa, float ya, float za) {
      float xaOrg = xa;
      float yaOrg = ya;
      float zaOrg = za;
      List<AABB> cubes = this.level.getCubes(this.bb.expand(xa, ya, za));

      for (int i = 0; i < cubes.size(); i++) {
         ya = cubes.get(i).clipYCollide(this.bb, ya);
      }
      this.bb.move(0.0F, ya, 0.0F);

      for (int i = 0; i < cubes.size(); i++) {
         xa = cubes.get(i).clipXCollide(this.bb, xa);
      }
      this.bb.move(xa, 0.0F, 0.0F);

      for (int i = 0; i < cubes.size(); i++) {
         za = cubes.get(i).clipZCollide(this.bb, za);
      }
      this.bb.move(0.0F, 0.0F, za);

      this.onGround = yaOrg != ya && yaOrg < 0.0F;

      if (xaOrg != xa) this.xd = 0.0F;
      if (yaOrg != ya) this.yd = 0.0F;
      if (zaOrg != za) this.zd = 0.0F;

      this.x = (this.bb.x0 + this.bb.x1) / 2.0F;
      this.y = (this.bb.y0 + this.bb.y1) / 2.0F;
      this.z = (this.bb.z0 + this.bb.z1) / 2.0F;
   }

   public void render(Tesselator t, float a, float xa, float ya, float za, float xa2, float za2) {
      float px = this.xo + (this.x - this.xo) * a;
      float py = this.yo + (this.y - this.yo) * a;
      float pz = this.zo + (this.z - this.zo) * a;

      float s = this.size;
      if (this.shrink) {
         float f = (float)this.age / Math.max(1, this.lifetime);
         s *= 1.0F - f * f;
      }
      t.color(this.r, this.g, this.b);
      t.tex(this.u0, this.v1);
      t.vertex(px - xa * s - xa2 * s, py - ya * s, pz - za * s - za2 * s);
      t.tex(this.u0, this.v0);
      t.vertex(px - xa * s + xa2 * s, py + ya * s, pz - za * s + za2 * s);
      t.tex(this.u1, this.v0);
      t.vertex(px + xa * s + xa2 * s, py + ya * s, pz + za * s + za2 * s);
      t.tex(this.u1, this.v1);
      t.vertex(px + xa * s - xa2 * s, py - ya * s, pz + za * s - za2 * s);
   }

   public boolean isDead() {
      return this.age >= this.lifetime;
   }
}

