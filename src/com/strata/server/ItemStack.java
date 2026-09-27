package com.strata.server;

public class ItemStack {
   public static final int MAX = 64;
   public int blockId = 0;
   public int count = 0;

   public ItemStack() {
   }

   public ItemStack(int blockId, int count) {
      this.blockId = blockId;
      this.count = Math.max(0, Math.min(MAX, count));
      if (this.count == 0) {
         this.blockId = 0;
      }
   }

   public boolean isEmpty() {
      return this.count <= 0 || this.blockId <= 0;
   }

   public void clear() {
      this.blockId = 0;
      this.count = 0;
   }
}

