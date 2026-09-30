package com.strata.blocks;

public class FlowerBlock extends CrossBlock {
   static final float LIT = 0.5F;

   public FlowerBlock(int id, int texture) {
      super(id, texture);
   }

   public FlowerBlock(int id, int texture, float pickHalf, float pickHeight) {
      super(id, texture, pickHalf, pickHeight);
   }

   @Override
   public boolean canStay(BlockView level, int x, int y, int z) {
      int below = level.getTile(x, y - 1, z);
      return (below == Blocks.GRASS_ID || below == Blocks.DIRT_ID)
         && level.getBrightness(x, y, z) >= LIT;
   }
}

