package com.strata.server;

public class LivingEntity extends Entity {
   public int hp;

   protected LivingEntity(int hp) {
      this.hp = hp;
   }

   public void hurt(int halfHearts) {
      if (halfHearts <= 0) {
         return;
      }
      this.hp -= halfHearts;
      if (this.hp < 0) {
         this.hp = 0;
      }
   }

   public boolean alive() {
      return this.hp > 0;
   }
}

