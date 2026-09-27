package com.strata.blocks;

public final class BlockState {
   public final Block block;
   public final int data;

   public static final BlockState AIR = new BlockState(null, 0);

   private BlockState(Block block, int data) {
      this.block = block;
      this.data = data & 0xFF;
   }

   public static BlockState of(Block block) {
      return block == null ? AIR : new BlockState(block, 0);
   }

   public static BlockState of(Block block, int data) {
      return block == null ? AIR : new BlockState(block, data);
   }

   public boolean isAir() {
      return this.block == null;
   }

   public int id() {
      return this.block == null ? 0 : this.block.id;
   }

   @Override
   public boolean equals(Object o) {
      if (!(o instanceof BlockState s)) {
         return false;
      }
       return this.id() == s.id() && this.data == s.data;
   }

   @Override
   public int hashCode() {
      return this.id() * 31 + this.data;
   }

   @Override
   public String toString() {
      return this.isAir() ? "air" : this.block.getClass().getSimpleName() + ":" + this.data;
   }
}

