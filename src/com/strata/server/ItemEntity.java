package com.strata.server;

import com.strata.core.AABB;
import com.strata.world.Level;

public class ItemEntity extends Entity {
   public static final int PICKUP_DELAY = 30;
   public static final int EXPIRE_TICKS = 18000;
   public static final float MAGNET_RADIUS = 3.0F;

   public final int id;
   public final int blockId;
   public int age = 0;
   public int pickupDelay = PICKUP_DELAY;
   public final float spinPhase = (float)(Math.random() * Math.PI * 2.0);
   public float r = 1.0F;
   public float g = 1.0F;
   public float b = 1.0F;

   public ItemEntity(int id, int blockId, float x, float y, float z, float xd, float yd, float zd) {
      this.id = id;
      this.blockId = blockId;
      this.x = x;
      this.y = y;
      this.z = z;
      this.xd = xd;
      this.yd = yd;
      this.zd = zd;
   }

   public void tick(Level level, AABB box) {
      this.level = level;
      this.age++;
      if (this.pickupDelay > 0) {
         this.pickupDelay--;
      }
      float cx = (box.x0 + box.x1) / 2.0F;
      float cy = (box.y0 + box.y1) / 2.0F;
      float cz = (box.z0 + box.z1) / 2.0F;
      float dx = cx - this.x;
      float dy = cy - this.y;
      float dz = cz - this.z;
      float dist = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (this.pickupDelay <= 0 && dist < MAGNET_RADIUS && dist > 0.001F) {
         float pull = 0.06F;
         this.xd += dx / dist * pull;
         this.yd += dy / dist * pull;
         this.zd += dz / dist * pull;
      }
      this.yd -= 0.03F;
      this.xd *= 0.98F;
      this.yd *= 0.98F;
      this.zd *= 0.98F;

      float w = 0.125F;
      AABB bb = new AABB(this.x - w, this.y - w, this.z - w, this.x + w, this.y + w, this.z + w);
      float xa = this.xd;
      float ya = this.yd;
      float za = this.zd;
      float xaOrg = xa;
      float yaOrg = ya;
      float zaOrg = za;
      java.util.List<AABB> cubes = level.getCubes(bb.expand(xa, ya, za));
      for (int i = 0; i < cubes.size(); i++) {
         ya = cubes.get(i).clipYCollide(bb, ya);
      }
      bb.move(0.0F, ya, 0.0F);
      for (int i = 0; i < cubes.size(); i++) {
         xa = cubes.get(i).clipXCollide(bb, xa);
      }
      bb.move(xa, 0.0F, 0.0F);
      for (int i = 0; i < cubes.size(); i++) {
         za = cubes.get(i).clipZCollide(bb, za);
      }
      bb.move(0.0F, 0.0F, za);
      if (yaOrg != ya && yaOrg < 0.0F) {
         this.xd *= 0.7F;
         this.zd *= 0.7F;
      }
      if (xaOrg != xa) this.xd = 0.0F;
      if (yaOrg != ya) this.yd = 0.0F;
      if (zaOrg != za) this.zd = 0.0F;
      this.x = (bb.x0 + bb.x1) / 2.0F;
      this.y = (bb.y0 + bb.y1) / 2.0F;
      this.z = (bb.z0 + bb.z1) / 2.0F;
   }

   public boolean expired() {
      return this.age >= EXPIRE_TICKS;
   }

   public boolean near(AABB box) {
      return this.x > box.x0 - 1.0F && this.x < box.x1 + 1.0F
         && this.y > box.y0 - 0.5F && this.y < box.y1 + 0.5F
         && this.z > box.z0 - 1.0F && this.z < box.z1 + 1.0F;
   }
}

