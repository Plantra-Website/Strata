package com.strata.blocks;

public class ReedBlock extends CrossBlock {
   public ReedBlock(int id, int texture) {
      this(id, texture, 0.375F, 1.0F);
   }

   public ReedBlock(int id, int texture, float pickHalf, float pickHeight) {
      super(id, texture, pickHalf, pickHeight);
   }

   @Override
   public boolean canStay(BlockView level, int x, int y, int z) {
      int below = level.getTile(x, y - 1, z);
      if (below == Blocks.REED_ID) {
         return true;
      }
      if (below != Blocks.GRASS_ID && below != Blocks.DIRT_ID && below != Blocks.SAND_ID) {
         return false;
      }
      return level.getTile(x + 1, y - 1, z) == Blocks.WATER_ID
         || level.getTile(x - 1, y - 1, z) == Blocks.WATER_ID
         || level.getTile(x, y - 1, z + 1) == Blocks.WATER_ID
         || level.getTile(x, y - 1, z - 1) == Blocks.WATER_ID;
   }
}

