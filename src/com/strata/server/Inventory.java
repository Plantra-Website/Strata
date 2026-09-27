package com.strata.server;

public class Inventory {
   public static final int HOTBAR = 9;
   public static final int SLOTS = 36;
   public final ItemStack[] slots = new ItemStack[SLOTS];

   public Inventory() {
      for (int i = 0; i < SLOTS; i++) {
         this.slots[i] = new ItemStack();
      }
   }

   public boolean add(int blockId, int count) {
      if (blockId <= 0 || count <= 0) {
         return true;
      }
      for (int i = 0; i < SLOTS; i++) {
         if (!this.slots[i].isEmpty() && this.slots[i].blockId == blockId) {
            int room = ItemStack.MAX - this.slots[i].count;
            int take = Math.min(room, count);
            this.slots[i].count += take;
            count -= take;
            if (count <= 0) {
               return true;
            }
         }
      }
      for (int i = 0; i < SLOTS; i++) {
         if (this.slots[i].isEmpty()) {
            int take = Math.min(ItemStack.MAX, count);
            this.slots[i].blockId = blockId;
            this.slots[i].count = take;
            count -= take;
            if (count <= 0) {
               return true;
            }
         }
      }
      return false;
   }

   public int consume(int slot, int count) {
      if (slot < 0 || slot >= SLOTS || count <= 0) {
         return 0;
      }
      ItemStack s = this.slots[slot];
      int take = Math.min(count, s.count);
      s.count -= take;
      if (s.count <= 0) {
         s.clear();
      }
      return take;
   }

   public void applyClick(int slot, ItemStack held) {
      if (slot < 0 || slot >= SLOTS) {
         return;
      }
      ItemStack s = this.slots[slot];
      if (held.isEmpty()) {
         held.blockId = s.blockId;
         held.count = s.count;
         s.clear();
      } else if (s.isEmpty()) {
         s.blockId = held.blockId;
         s.count = held.count;
         held.clear();
      } else if (s.blockId == held.blockId) {
         int room = ItemStack.MAX - s.count;
         int take = Math.min(room, held.count);
         s.count += take;
         held.count -= take;
         if (held.count <= 0) {
            held.clear();
         }
      } else {
         int b = s.blockId;
         int c = s.count;
         s.blockId = held.blockId;
         s.count = held.count;
         held.blockId = b;
         held.count = c;
      }
   }
   public void applySync(int[] blocks, int[] counts) {
      for (int i = 0; i < SLOTS; i++) {
         int b = (blocks != null && i < blocks.length) ? blocks[i] : 0;
         int c = (counts != null && i < counts.length) ? counts[i] : 0;
         this.slots[i] = new ItemStack(b, c);
      }
   }

   public void clear() {
      for (int i = 0; i < SLOTS; i++) {
         this.slots[i].clear();
      }
   }

   public boolean sameAs(Inventory other) {
      for (int i = 0; i < SLOTS; i++) {
         if (this.slots[i].blockId != other.slots[i].blockId
            || this.slots[i].count != other.slots[i].count) {
            return false;
         }
      }
      return true;
   }
}

