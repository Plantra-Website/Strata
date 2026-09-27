package com.strata.server;

import com.strata.core.AABB;
import com.strata.core.MathHelper;
import com.strata.world.Level;

public class FallingBlock extends Entity {
   public final int id;
   public final int blockId;

   public FallingBlock(Level level, int id, int blockId, float x, float y, float z) {
      this.level = level;
      this.id = id;
      this.blockId = blockId;
      float h = 0.49F;
      this.bb = new AABB(x - h, y - h, z - h, x + h, y + h, z + h);
      this.x = x;
      this.y = y;
      this.z = z;
   }

   @Override
   protected float yOffset() {
      return 0.49F;
   }

   public void tick() {
      this.yd -= 0.03F;
      this.xd *= 0.98F;
      this.yd *= 0.98F;
      this.zd *= 0.98F;
      this.move(this.xd, this.yd, this.zd);
   }

   public int landX() {
      return MathHelper.floor(this.x);
   }

   public int landY() {
      return MathHelper.floor(this.bb.y0 + 0.05F);
   }

   public int landZ() {
      return MathHelper.floor(this.z);
   }
}

