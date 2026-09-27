package com.strata.core;

public class AABB {
   public float x0;
   public float y0;
   public float z0;
   public float x1;
   public float y1;
   public float z1;

   public AABB(float x0, float y0, float z0, float x1, float y1, float z1) {
      this.x0 = x0;
      this.y0 = y0;
      this.z0 = z0;
      this.x1 = x1;
      this.y1 = y1;
      this.z1 = z1;
   }

   public AABB expand(float xa, float ya, float za) {
      float nx0 = this.x0;
      float ny0 = this.y0;
      float nz0 = this.z0;
      float nx1 = this.x1;
      float ny1 = this.y1;
      float nz1 = this.z1;
      if (xa < 0.0F) {
         nx0 += xa;
      } else {
         nx1 += xa;
      }
      if (ya < 0.0F) {
         ny0 += ya;
      } else {
         ny1 += ya;
      }
      if (za < 0.0F) {
         nz0 += za;
      } else {
         nz1 += za;
      }
      return new AABB(nx0, ny0, nz0, nx1, ny1, nz1);
   }

   public float clipXCollide(AABB mover, float xa) {
      if (mover.y1 <= this.y0 || mover.y0 >= this.y1) {
         return xa;
      }
      if (mover.z1 <= this.z0 || mover.z0 >= this.z1) {
         return xa;
      }
      if (xa > 0.0F && mover.x1 <= this.x0) {
         xa = Math.min(xa, this.x0 - mover.x1);
      }
      if (xa < 0.0F && mover.x0 >= this.x1) {
         xa = Math.max(xa, this.x1 - mover.x0);
      }
      return xa;
   }

   public float clipYCollide(AABB mover, float ya) {
      if (mover.x1 <= this.x0 || mover.x0 >= this.x1) {
         return ya;
      }
      if (mover.z1 <= this.z0 || mover.z0 >= this.z1) {
         return ya;
      }
      if (ya > 0.0F && mover.y1 <= this.y0) {
         ya = Math.min(ya, this.y0 - mover.y1);
      }
      if (ya < 0.0F && mover.y0 >= this.y1) {
         ya = Math.max(ya, this.y1 - mover.y0);
      }
      return ya;
   }

   public float clipZCollide(AABB mover, float za) {
      if (mover.x1 <= this.x0 || mover.x0 >= this.x1) {
         return za;
      }
      if (mover.y1 <= this.y0 || mover.y0 >= this.y1) {
         return za;
      }
      if (za > 0.0F && mover.z1 <= this.z0) {
         za = Math.min(za, this.z0 - mover.z1);
      }
      if (za < 0.0F && mover.z0 >= this.z1) {
         za = Math.max(za, this.z1 - mover.z0);
      }
      return za;
   }

   public boolean intersects(AABB other) {
      if (other.x1 <= this.x0 || other.x0 >= this.x1) {
         return false;
      }
      if (other.y1 <= this.y0 || other.y0 >= this.y1) {
         return false;
      }
      return other.z1 > this.z0 && other.z0 < this.z1;
   }

   public void move(float xa, float ya, float za) {
      this.x0 += xa;
      this.y0 += ya;
      this.z0 += za;
      this.x1 += xa;
      this.y1 += ya;
      this.z1 += za;
   }
}

