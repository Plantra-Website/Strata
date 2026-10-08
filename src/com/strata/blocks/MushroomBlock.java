package com.strata.blocks;

public class MushroomBlock extends CrossBlock {
   static final float DARK = 0.8F;

   public MushroomBlock(int id, int texture) {
      this(id, texture, 0.2F, 0.4F);
   }

   public MushroomBlock(int id, int texture, float pickHalf, float pickHeight) {
      super(id, texture, pickHalf, pickHeight);
   }

   @Override
   public boolean canStay(BlockView level, int x, int y, int z) {
      int below = level.getTile(x, y - 1, z);
      if (below == Blocks.MYCELIUM_ID) {
         return true;
      }
      return level.getBrightness(x, y, z) < DARK && Blocks.isOpaqueCube(below);
   }
}

