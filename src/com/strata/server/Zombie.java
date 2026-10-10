package com.strata.server;

import com.strata.blocks.Blocks;
import com.strata.core.AABB;
import com.strata.core.Config;
import com.strata.core.DayCycle;
import com.strata.core.MathHelper;
import com.strata.world.Level;

public class Zombie extends LivingEntity {
   public static final int MAX_HP = 20;
   public static final int MELEE_DAMAGE = 4;
   public static final int MELEE_COOLDOWN = 60;
   public static final int BURN_TICKS = 60;
   public static final int PUNCH_INVULN = 18;
   public static final float DAY_BURN_ABOVE = 0.15F;
   public static final float SEEK_ACCEL = 0.010F;
   public static final float SEEK_MAX = 0.030F;
   public static final float SEEK_RANGE = 16.0F;
   public static final float MELEE_REACH = 2.0F;
   public static final float KNOCK_H = 0.24F;
   public static final float KNOCK_LIFT = 0.1F;
   public static final float SKY_BURN_AT = 14;

   public final int id;
   public float r = 1.0F;
   public float g = 1.0F;
   public float b = 1.0F;
    public int attackCooldown = 0;
    public int invuln = 0;
    public int burnClock = 0;
   private float fallPeak;
   private boolean landed = true;
   private int lavaClock = 0;
   private int cactusClock = 0;
   public float facing = 0.0F;
   public float walkPhase = 0.0F;
   public float swingAmount = 0.0F;
   public float swingPrev = 0.0F;
   public float ybo;

   public Zombie(int id, float x, float y, float z) {
      super(MAX_HP);
      this.id = id;
      float w = 0.3F;
      this.bb = new AABB(x - w, y, z - w, x + w, y + 1.8F, z + w);
      this.x = x;
      this.y = y + 1.62F;
      this.z = z;
      this.stepHeight = 0.5F;
      this.fallPeak = y;
   }

   @Override
   protected float yOffset() {
      return 1.62F;
   }

   public void tick(Level level, float px, float py, float pz, AABB playerBox,
         long timeOfDay, Player target) {
      this.level = level;
      if (this.bb.y0 < -10.0F) {
         this.hurt(20);
         return;
      }
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      this.ybo = this.bb.y0;
      float dx = px - this.x;
      float dz = pz - this.z;
      float dist = (float)Math.sqrt(dx * dx + dz * dz);
      float dy = py - this.y;
      boolean wantsMove = dx * dx + dy * dy + dz * dz < SEEK_RANGE * SEEK_RANGE
         && dist > 1.0F;
      if (wantsMove && dist > 0.001F) {
         float sp = (float)Math.sqrt(this.xd * this.xd + this.zd * this.zd);
         if (sp < SEEK_MAX) {
            this.xd += dx / dist * SEEK_ACCEL;
            this.zd += dz / dist * SEEK_ACCEL;
            sp = (float)Math.sqrt(this.xd * this.xd + this.zd * this.zd);
            if (sp > SEEK_MAX) {
               this.xd = this.xd / sp * SEEK_MAX;
               this.zd = this.zd / sp * SEEK_MAX;
            }
         }
      }
      int waterFeet = this.level.getTile(MathHelper.floor(this.x),
         MathHelper.floor(this.bb.y0 + 0.1F), MathHelper.floor(this.z));
      int waterUnder = this.level.getTile(MathHelper.floor(this.x),
         MathHelper.floor(this.bb.y0 - 0.1F), MathHelper.floor(this.z));
      boolean inWater = waterFeet == Blocks.WATER_ID || waterUnder == Blocks.WATER_ID;
      this.yd -= inWater ? 0.0012F : 0.005F;
      this.move(this.xd, this.yd, this.zd);
      this.xd *= inWater ? 0.8F : 0.91F;
      this.yd *= 0.98F;
      this.zd *= inWater ? 0.8F : 0.91F;
      if (inWater) {
         this.fallPeak = this.bb.y0;
      }
      if (this.onGround) {
         int groundId = this.level.getTile(MathHelper.floor(this.x),
            MathHelper.floor(this.bb.y0 - 0.1F), MathHelper.floor(this.z));
         float grip = groundId == Blocks.ICE_ID ? 0.98F : 0.8F;
         this.xd *= grip;
         this.zd *= grip;
      }
      float stepX = this.x - this.xo;
      float stepZ = this.z - this.zo;
      float step = (float)Math.sqrt(stepX * stepX + stepZ * stepZ);
      float pace = Math.min(step * 4.0F, 1.0F);
      this.swingPrev = this.swingAmount;
      this.swingAmount += (pace - this.swingAmount) * 0.4F;
      this.walkPhase += this.swingAmount;
      if (step > 0.0001F) {
         float want = (float)Math.atan2(stepX, -stepZ);
         float diff = want - this.facing;
         while (diff > (float)Math.PI) {
            diff -= (float)Math.PI * 2.0F;
         }
         while (diff < -(float)Math.PI) {
            diff += (float)Math.PI * 2.0F;
         }
         this.facing += diff * 0.4F;
      }
      float ledgeUp = playerBox == null ? -1.0F : playerBox.y0 - this.bb.y0;
      boolean ledgeAbove = ledgeUp > 0.5F && ledgeUp < 1.5F && dist < 1.5F;
      if (this.onGround && ((wantsMove && (this.xd == 0.0F || this.zd == 0.0F))
            || ledgeAbove)) {
         this.yd = 0.12F;
      }
      if (this.onGround) {
         if (this.landed) {
            float fall = this.fallPeak - this.bb.y0;
            if (fall > 3.0F) {
               this.hurt((int)Math.ceil(fall - 3.0F));
            }
         }
         this.landed = true;
         this.fallPeak = this.bb.y0;
      } else if (this.bb.y0 > this.fallPeak) {
         this.fallPeak = this.bb.y0;
      }
      int feetId = this.level.getTile(MathHelper.floor(this.x),
         MathHelper.floor(this.bb.y0 + 0.1F), MathHelper.floor(this.z));
      int underId = this.level.getTile(MathHelper.floor(this.x),
         MathHelper.floor(this.bb.y0 - 0.1F), MathHelper.floor(this.z));
      if (feetId == Blocks.LAVA_ID || underId == Blocks.LAVA_ID) {
         if (++this.lavaClock >= 30) {
            this.lavaClock = 0;
            this.hurt(4);
         }
      } else {
         this.lavaClock = 0;
      }
      if (feetId == Blocks.CACTUS_ID || underId == Blocks.CACTUS_ID) {
         if (++this.cactusClock >= 30) {
            this.cactusClock = 0;
            this.hurt(1);
         }
      } else {
         this.cactusClock = 0;
      }
      this.burnClock++;
      if (this.burnClock >= BURN_TICKS) {
         this.burnClock = 0;
         if (DayCycle.amount(timeOfDay, Config.DAY_LENGTH) > DAY_BURN_ABOVE
            && level.getSkyLevel(MathHelper.floor(this.x),
               MathHelper.floor(this.bb.y0 + 1.5F), MathHelper.floor(this.z)) >= SKY_BURN_AT) {
            this.hurt(1);
         }
      }
      if (target == null || !this.alive()) {
         return;
      }
      if (this.attackCooldown > 0) {
         this.attackCooldown--;
      }
      if (this.invuln > 0) {
         this.invuln--;
      }
      if (this.attackCooldown <= 0) {
         float rx = target.x - this.x;
         float ry = target.y - this.y;
         float rz = target.z - this.z;
         if (rx * rx + ry * ry + rz * rz < MELEE_REACH * MELEE_REACH
            && this.bb.y0 < playerBox.y1 && this.bb.y1 > playerBox.y0
            && level.sightClear(this.x, this.y, this.z,
               target.x, target.y, target.z)) {
            target.hurt(MELEE_DAMAGE);
            knockBack(target, this.x, this.z, KNOCK_H, KNOCK_LIFT);
            this.attackCooldown = MELEE_COOLDOWN;
         }
      }
   }

    public boolean punch(float fromX, float fromZ) {
       if (!this.alive() || this.invuln > 0) {
          return false;
       }
       this.hurt(1);
       knockBack(this, fromX, fromZ, KNOCK_H, KNOCK_LIFT);
       this.invuln = PUNCH_INVULN;
       return true;
    }
}

