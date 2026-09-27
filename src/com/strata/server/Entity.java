package com.strata.server;

import com.strata.core.AABB;
import com.strata.world.Level;
import java.util.List;

public class Entity {
   protected Level level;
   public float x;
   public float y;
   public float z;
   public float xo;
   public float yo;
   public float zo;
   public float xd;
   public float yd;
   public float zd;
   public AABB bb;
   public boolean onGround = false;

   protected Entity() {
   }

   protected float yOffset() {
      return 0.0F;
   }

   public void move(float xa, float ya, float za) {
     float xaOrg = xa;
     float yaOrg = ya;
     float zaOrg = za;
     List<AABB> aABBs = this.level.getCubes(this.bb.expand(xa, ya, za));

     for (int i = 0; i < aABBs.size(); i++) {
        ya = aABBs.get(i).clipYCollide(this.bb, ya);
     }

     this.bb.move(0.0F, ya, 0.0F);

     for (int i = 0; i < aABBs.size(); i++) {
        xa = aABBs.get(i).clipXCollide(this.bb, xa);
     }

     this.bb.move(xa, 0.0F, 0.0F);

     for (int i = 0; i < aABBs.size(); i++) {
        za = aABBs.get(i).clipZCollide(this.bb, za);
     }

     this.bb.move(0.0F, 0.0F, za);
     this.onGround = yaOrg != ya && yaOrg < 0.0F;
     if (xaOrg != xa) {
        this.xd = 0.0F;
     }

     if (yaOrg != ya) {
        this.yd = 0.0F;
     }

     if (zaOrg != za) {
        this.zd = 0.0F;
     }

      this.x = (this.bb.x0 + this.bb.x1) / 2.0F;
      this.y = this.bb.y0 + this.yOffset();
      this.z = (this.bb.z0 + this.bb.z1) / 2.0F;
   }
}

