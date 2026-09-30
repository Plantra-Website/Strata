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
   public float stepHeight = 0.0F;

   protected Entity() {
   }

   protected float yOffset() {
      return 0.0F;
   }

   public void move(float xa, float ya, float za) {
      float xaOrg = xa;
      float yaOrg = ya;
      float zaOrg = za;
      float sx0 = this.bb.x0;
      float sy0 = this.bb.y0;
      float sz0 = this.bb.z0;
      float sx1 = this.bb.x1;
      float sy1 = this.bb.y1;
      float sz1 = this.bb.z1;
      float sxd = this.xd;
      float szd = this.zd;
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

      if (this.stepHeight > 0.0F && this.onGround && (xaOrg != xa || zaOrg != za)) {
         float directX = this.bb.x0 - sx0;
         float directZ = this.bb.z0 - sz0;
         float ny0 = this.bb.y0;
         float ny1 = this.bb.y1;
         float oxd = this.xd;
         float oyd = this.yd;
         float ozd = this.zd;
         boolean og = this.onGround;
         this.bb.x0 = sx0;
         this.bb.y0 = sy0;
         this.bb.z0 = sz0;
         this.bb.x1 = sx1;
         this.bb.y1 = sy1;
         this.bb.z1 = sz1;
         this.stepLeg(0.0F, this.stepHeight, 0.0F);
         this.stepLeg(xaOrg, 0.0F, zaOrg);
         boolean settled = this.stepLeg(0.0F, -this.stepHeight, 0.0F);
         float steppedX = this.bb.x0 - sx0;
         float steppedZ = this.bb.z0 - sz0;
         if (steppedX * steppedX + steppedZ * steppedZ >= directX * directX + directZ * directZ) {
            this.onGround = settled;
            this.xd = sxd;
            this.zd = szd;
         } else {
            this.bb.x0 = sx0 + directX;
            this.bb.y0 = ny0;
            this.bb.z0 = sz0 + directZ;
            this.bb.x1 = sx1 + directX;
            this.bb.y1 = ny1;
            this.bb.z1 = sz1 + directZ;
            this.xd = oxd;
            this.yd = oyd;
            this.zd = ozd;
            this.onGround = og;
         }
      }

      this.x = (this.bb.x0 + this.bb.x1) / 2.0F;
      this.y = this.bb.y0 + this.yOffset();
      this.z = (this.bb.z0 + this.bb.z1) / 2.0F;
   }

   private boolean stepLeg(float xa, float ya, float za) {
      List<AABB> legs = this.level.getCubes(this.bb.expand(xa, ya, za));
      boolean hit = false;
      for (int i = 0; i < legs.size(); i++) {
         float ny = legs.get(i).clipYCollide(this.bb, ya);
         if (ny != ya) {
            hit = true;
         }
         ya = ny;
      }
      this.bb.move(0.0F, ya, 0.0F);
      for (int i = 0; i < legs.size(); i++) {
         float nx = legs.get(i).clipXCollide(this.bb, xa);
         if (nx != xa) {
            hit = true;
         }
         xa = nx;
      }
      this.bb.move(xa, 0.0F, 0.0F);
      for (int i = 0; i < legs.size(); i++) {
         float nz = legs.get(i).clipZCollide(this.bb, za);
         if (nz != za) {
            hit = true;
         }
         za = nz;
      }
      this.bb.move(0.0F, 0.0F, za);
      return hit;
   }
}

