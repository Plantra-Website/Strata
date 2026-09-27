package com.strata.world;

import com.strata.world.storage.nbt.NBT;

public abstract class BlockEntity {
   public final int x;
   public final int y;
   public final int z;
   private boolean removed = false;

   protected BlockEntity(int x, int y, int z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public abstract String typeId();

   public void tick(Level level) {
   }

   public abstract NBT.CompoundTag save();

   public abstract void load(NBT.CompoundTag tag);

   public final void remove() {
      this.removed = true;
   }

   public final boolean isRemoved() {
      return this.removed;
   }
}

