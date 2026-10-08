package com.strata.client;

import com.strata.blocks.AtlasStitcher;
import com.strata.blocks.BiomeTints;
import com.strata.blocks.Blocks;
import com.strata.core.MathHelper;
import com.strata.server.Player;
import com.strata.world.Level;
import com.strata.world.mesh.Textures;
import com.strata.world.mesh.Tesselator;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL11;

public class ParticleEngine {
   private final Level level;
   private final List<Particle> particles = new ArrayList<>();

   public ParticleEngine(Level level) {
      this.level = level;
   }

   public void add(Particle p) {
      this.particles.add(p);
   }

   public int count() {
      return this.particles.size();
   }

   Particle sample(int i) {
      return this.particles.get(i);
   }

   public static final int FLAME_SPRITE = ParticleAtlas.indexOf("flame.png");
   public static final int BUBBLE_SPRITE = ParticleAtlas.indexOf("lava.png");
   public static final int SMOKE_SPRITE = ParticleAtlas.indexOf("generic_7.png");

   public void addEmber(float x, float y, float z) {
      Particle p = Particle.sprite(this.level, x, y, z, 0.0F, 0.0F, 0.0F, FLAME_SPRITE);
      p.xd *= 0.15F;
      p.yd = 0.004F;
      p.zd *= 0.15F;
      p.gravity = 0.0F;
      p.lifetime = Math.min(p.lifetime, 44);
      this.add(p);
   }

   public void addSmoke(float x, float y, float z) {
      Particle p = Particle.sprite(this.level, x, y, z, 0.0F, 0.0F, 0.0F, SMOKE_SPRITE);
      p.xd *= 0.15F;
      p.zd *= 0.15F;
      p.yd = 0.01F;
      p.gravity = -0.002F;
      p.lifetime = 8 + (int)(Math.random() * 32);
      p.smokeAnim = true;
      p.r = 0.35F;
      p.g = 0.35F;
      p.b = 0.35F;
      this.add(p);
   }

   public void addBubble(float x, float y, float z) {
      Particle p = Particle.sprite(this.level, x, y, z, 0.0F, 0.0F, 0.0F, BUBBLE_SPRITE);
      p.xd *= 0.5F;
      p.zd *= 0.5F;
      p.yd = 0.02F + (float)Math.random() * 0.13F;
      p.gravity = 0.005F;
      p.lifetime = 48 + (int)(Math.random() * 112);
      p.trailSmoke = true;
      p.shrink = true;
      p.lavaPop = true;
      this.add(p);
   }

   public void addPuff(float x, float y, float z, int texIndex) {
      for (int i = 0; i < 5; i++) {
         float a = (float)(Math.random() * Math.PI * 2.0);
         Particle p = new Particle(this.level, x, y + 0.1F, z,
            (float)Math.cos(a) * 0.06F, 0.02F, (float)Math.sin(a) * 0.06F, texIndex);
         p.lifetime = Math.min(p.lifetime, 15);
         this.add(p);
      }
   }

   public void addBlockDestroyParticles(int x, int y, int z, int texIndex) {
      int count = 4;
      for (int xx = 0; xx < count; xx++) {
         for (int yy = 0; yy < count; yy++) {
            for (int zz = 0; zz < count; zz++) {
               float px = x + (xx + 0.5F) / count;
               float py = y + (yy + 0.5F) / count;
               float pz = z + (zz + 0.5F) / count;
               Particle p = new Particle(this.level, px, py, pz, px - (x + 0.5F), py - (y + 0.5F), pz - (z + 0.5F), texIndex);
               tintByTile(p, texIndex, px, pz);
               this.add(p);
            }
         }
      }
   }

   private void tintByTile(Particle p, int texIndex, float x, float z) {
      float[] tc = BiomeTints.colorFor(AtlasStitcher.tintKind(texIndex),
         this.level.biomeAt(MathHelper.floor(x), MathHelper.floor(z)));
      p.tr = tc[0];
      p.tg = tc[1];
      p.tb = tc[2];
      p.r *= tc[0];
      p.g *= tc[1];
      p.b *= tc[2];
   }

   public void addBlockHitParticles(int x, int y, int z, int face, int texIndex) {
      float px = x + (float)Math.random();
      float py = y + (float)Math.random();
      float pz = z + (float)Math.random();
      if (face == 0) py = y - 0.05F;
      if (face == 1) py = y + 1.05F;
      if (face == 2) pz = z - 0.05F;
      if (face == 3) pz = z + 1.05F;
      if (face == 4) px = x - 0.05F;
      if (face == 5) px = x + 1.05F;
      Particle p = new Particle(this.level, px, py, pz, 0.0F, 0.0F, 0.0F, texIndex);
      tintByTile(p, texIndex, px, pz);
      this.add(p);
   }

   public void tick() {
      for (int i = 0; i < this.particles.size(); i++) {
         Particle p = this.particles.get(i);
         if (p.trailSmoke && (p.age == 0 || Math.random() * 5.0 > (float)p.age / Math.max(1, p.lifetime))) {
            this.addSmoke(p.x, p.y, p.z);
         }
         p.tick();
         if (!p.isDead() && p.lavaPop) {
            int bx = MathHelper.floor(p.x);
            int by = MathHelper.floor(p.y - 0.05F);
            int bz = MathHelper.floor(p.z);
            if ((this.level.getTile(bx, by, bz) == Blocks.LAVA_ID
               || this.level.getTile(bx, by - 1, bz) == Blocks.LAVA_ID)
               && (p.onGround || p.yd < 0.0F)) {
               p.age = p.lifetime;
            }
         }
         if (p.isDead()) {
            this.particles.remove(i--);
         }
      }
   }

   public void render(Player player, float a, int layer) {
      if (this.particles.isEmpty() || layer != 0) {
         return;
      }

      GL11.glEnable(GL11.GL_TEXTURE_2D);

      float yRot = player.yRot;
      float xRot = player.xRot;
      float xa = -(float)Math.cos(yRot * Math.PI / 180.0);
      float za = -(float)Math.sin(yRot * Math.PI / 180.0);
      float xa2 = (float)Math.sin(yRot * Math.PI / 180.0) * (float)Math.sin(xRot * Math.PI / 180.0);
      float za2 = -(float)Math.cos(yRot * Math.PI / 180.0) * (float)Math.sin(xRot * Math.PI / 180.0);
      float ya = (float)Math.cos(xRot * Math.PI / 180.0);

      Tesselator t = Tesselator.SHARED;

      Textures.bind(Textures.loadAtlas(9728));
      t.init();
      for (int i = 0; i < this.particles.size(); i++) {
         Particle p = this.particles.get(i);
         if (!p.particleSheet) {
            p.render(t, a, xa, ya, za, xa2, za2);
         }
      }
      t.flush();

      Textures.bind(ParticleAtlas.texture());
      GL11.glEnable(GL11.GL_ALPHA_TEST);
      GL11.glAlphaFunc(GL11.GL_GREATER, 0.5F);
      t.init();
      for (int i = 0; i < this.particles.size(); i++) {
         Particle p = this.particles.get(i);
         if (p.particleSheet) {
            p.render(t, a, xa, ya, za, xa2, za2);
         }
      }
      t.flush();
      GL11.glDisable(GL11.GL_TEXTURE_2D);
   }
}

