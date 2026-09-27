package com.strata.server;

import com.strata.blocks.Blocks;
import com.strata.core.AABB;
import com.strata.core.MathHelper;
import com.strata.core.Rng;
import com.strata.net.InputState;
import com.strata.world.Level;

public class Player extends Entity {
   public float yRot;
   public float xRot;
   public boolean spectator = false;
   public int hp = 20;
   private float fallPeak = 0.0F;
   private boolean landed = true;
   private int lavaClock = 0;

   public Player(Level level) {
      this.level = level;
      this.resetPos();
   }

   @Override
   protected float yOffset() {
      return 1.62F;
   }

   private void resetPos() {
        this.landed = false;
        float[] spawn = this.level.spawnPoint();
        if (spawn != null) {
           this.teleport(spawn[0], spawn[1], spawn[2]);
           return;
        }
        float x = Rng.range(-16.0F, 16.0F);
        float z = Rng.range(-16.0F, 16.0F);
        int ix = MathHelper.floor(x);
        int iz = MathHelper.floor(z);
        boolean placed = false;
        outer:
        for (int r = 0; r <= 8; r++) {
           for (int dx = -r; dx <= r; dx++) {
              for (int dz = -r; dz <= r; dz++) {
                 if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                    continue;
                 }
                 int cx = ix + dx;
                 int cz = iz + dz;
                 int top = -1;
                 for (int y = this.level.depth - 1; y >= 0; y--) {
                    if (this.level.isSolidTile(cx, y, cz)) {
                       top = y;
                       break;
                    }
                 }
                 if (top >= 0 && top + 2 < this.level.depth
                    && !this.level.isSolidTile(cx, top + 1, cz)
                    && !this.level.isSolidTile(cx, top + 2, cz)) {
                    this.setPos(cx + 0.5F, top + 1.9F, cz + 0.5F);
                    placed = true;
                    break outer;
                 }
              }
           }
        }
        if (!placed) {
           this.setPos(x, this.level.depth + 10, z);
        }
     }

   public void teleport(float x, float y, float z) {
      this.setPos(x, y, z);
      this.xo = x;
      this.yo = y;
      this.zo = z;
      this.xd = 0.0F;
      this.yd = 0.0F;
      this.zd = 0.0F;
   }

   private void setPos(float x, float y, float z) {
      this.x = x;
      this.y = y;
      this.z = z;
      float w = 0.3F;
      float h = 0.9F;
      this.bb = new AABB(x - w, y - h, z - w, x + w, y + h, z + w);
      this.fallPeak = this.bb.y0;
   }

   public void hurt(int halfHearts) {
      if (this.spectator || halfHearts <= 0) {
         return;
      }
      this.hp -= halfHearts;
      if (this.hp < 0) {
         this.hp = 0;
      }
   }

   public void respawn() {
      this.hp = 20;
      this.lavaClock = 0;
      this.resetPos();
   }

   public void turn(float xo, float yo) {
      this.yRot = (float)(this.yRot + xo * 0.15);
      this.xRot = (float)(this.xRot - yo * 0.15);
      if (this.xRot < -90.0F) {
         this.xRot = -90.0F;
      }

      if (this.xRot > 90.0F) {
         this.xRot = 90.0F;
      }
   }

     public void tick(InputState in) {
       this.xo = this.x;
       this.yo = this.y;
       this.zo = this.z;
       this.yRot = in.yaw;
       this.xRot = in.pitch;
       if (this.spectator) {
          this.tickSpectator(in);
          return;
       }
       float xa = 0.0F;
      float ya = 0.0F;
      if (in.reset) {
         this.resetPos();
      }

      if (in.fwd) {
         ya--;
      }

      if (in.back) {
         ya++;
      }

      if (in.left) {
         xa--;
      }

      if (in.right) {
         xa++;
      }

      if (in.jump && this.onGround) {
         this.yd = 0.12F;
      }

      this.moveRelative(xa, ya, this.onGround ? 0.02F : 0.005F);
      this.yd = (float)(this.yd - 0.005);
      this.move(this.xd, this.yd, this.zd);
      this.xd *= 0.91F;
      this.yd *= 0.98F;
      this.zd *= 0.91F;
      if (this.onGround) {
         this.xd *= 0.8F;
         this.zd *= 0.8F;
      }
      if (!this.spectator) {
         float feet = this.bb.y0;
         if (this.onGround) {
            if (this.landed) {
               float fall = this.fallPeak - feet;
               if (fall > 3.5F) {
                  this.hurt((int)(fall - 3.0F));
               }
            }
            this.landed = true;
            this.fallPeak = feet;
         } else if (feet > this.fallPeak) {
            this.fallPeak = feet;
         }
         int feetId = this.level.getTile(MathHelper.floor(this.x), MathHelper.floor(feet + 0.1F), MathHelper.floor(this.z));
         int underId = this.level.getTile(MathHelper.floor(this.x), MathHelper.floor(feet - 0.1F), MathHelper.floor(this.z));
         if (feetId == Blocks.LAVA_ID || underId == Blocks.LAVA_ID) {
            if (++this.lavaClock >= 30) {
               this.lavaClock = 0;
               this.hurt(4);
            }
         } else {
            this.lavaClock = 0;
         }
      }
   }

    private void tickSpectator(InputState in) {
       if (in.reset) {
          this.resetPos();
       }

       float xa = 0.0F;
       float za = 0.0F;
       if (in.fwd) {
          za--;
       }

       if (in.back) {
          za++;
       }

       if (in.left) {
          xa--;
       }

       if (in.right) {
          xa++;
       }

       this.moveRelative(xa, za, 0.09F);
       float lift = 0.0F;
       if (in.up) {
          lift += 1.0F; 
       }

       if (in.down) {
          lift -= 1.0F; 
       }

       this.yd += (lift * 0.18F - this.yd) * 0.35F;
       this.x += this.xd;
       this.y += this.yd;
       this.z += this.zd;
       this.xd *= 0.7F;
       this.zd *= 0.7F;
       this.onGround = false;
       this.setPos(this.x, this.y, this.z);
    }

    public void evict() {
       for (int i = 0; i < 64 && !this.level.getCubes(this.bb).isEmpty(); i++) {
          this.setPos(this.x, this.y + 1.0F, this.z);
       }
    }

    public void applyState(float x, float y, float z, boolean onGround, boolean spectator, int hp) {
       this.xo = this.x;
       this.yo = this.y;
       this.zo = this.z;
       this.setPos(x, y, z);
       this.x = x;
       this.y = y;
       this.z = z;
       this.onGround = onGround;
       this.spectator = spectator;
       this.hp = hp;
    }

    public void moveRelative(float xa, float za, float speed) {
      float dist = xa * xa + za * za;
      if (!(dist < 0.01F)) {
         dist = speed / (float)Math.sqrt(dist);
         xa *= dist;
         za *= dist;
         float sin = (float)Math.sin(this.yRot * Math.PI / 180.0);
         float cos = (float)Math.cos(this.yRot * Math.PI / 180.0);
         this.xd += xa * cos - za * sin;
         this.zd += za * cos + xa * sin;
      }
   }
}

