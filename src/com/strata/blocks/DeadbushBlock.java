package com.strata.blocks;

public class DeadbushBlock extends CrossBlock {
   public DeadbushBlock(int id, int texture) {
      this(id, texture, 0.4F, 0.8F);
   }

   public DeadbushBlock(int id, int texture, float pickHalf, float pickHeight) {
      super(id, texture, pickHalf, pickHeight);
   }

   @Override
   public boolean canStay(BlockView level, int x, int y, int z) {
      return level.getTile(x, y - 1, z) == Blocks.SAND_ID
         && level.getBrightness(x, y, z) >= FlowerBlock.LIT;
   }
}

