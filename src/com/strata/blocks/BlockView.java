package com.strata.blocks;

public interface BlockView {
   boolean isSolidTile(int x, int y, int z);
   float getBrightness(int x, int y, int z);
   default int getTile(int x, int y, int z) {
      return 0;
   }
   default BlockState getBlockState(int x, int y, int z) {
      return BlockState.of(null);
   }
   default int getSkyLevel(int x, int y, int z) {
      return 15;
   }
   default int getBlockLevel(int x, int y, int z) {
      return 0;
   }
}

