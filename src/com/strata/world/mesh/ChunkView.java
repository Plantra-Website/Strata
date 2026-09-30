package com.strata.world.mesh;

import com.strata.blocks.BlockState;
import com.strata.blocks.BlockView;
import com.strata.blocks.Blocks;
import com.strata.core.BlockerProbe;
import com.strata.world.Level;

class ChunkView implements BlockView, BlockerProbe {
   private final Level level;
   private final int x0;
   private final int z0;
   private final byte[][] tiles = new byte[18 * 18][];
   private final byte[][] datas = new byte[18 * 18][];

   ChunkView(Level level, int ccx, int ccz) {
      this.level = level;
      this.x0 = ccx * 16 - 1;
      this.z0 = ccz * 16 - 1;
      for (int dx = 0; dx < 18; dx++) {
         for (int dz = 0; dz < 18; dz++) {
            int i = dz * 18 + dx;
            this.tiles[i] = level.readColumn(this.x0 + dx, this.z0 + dz);
            this.datas[i] = level.readDataColumn(this.x0 + dx, this.z0 + dz);
         }
      }
   }

   byte[] column(int x, int z) {
      return this.tiles[(z - this.z0) * 18 + (x - this.x0)];
   }

   byte[] dataColumn(int x, int z) {
      return this.datas[(z - this.z0) * 18 + (x - this.x0)];
   }

   private static final int EXT = 52;
   private byte[][] ext;

   private byte[] extColumn(int x, int z) {
      int dx = x - (this.x0 - 16);
      int dz = z - (this.z0 - 16);
      if (dx < 0 || dx >= EXT || dz < 0 || dz >= EXT) {
         return null;
      }
      if (this.ext == null) {
         this.ext = new byte[EXT * EXT][];
      }
      int i = dz * EXT + dx;
      byte[] col = this.ext[i];
      if (col == null) {
         col = this.level.readColumn(x, z);
         this.ext[i] = col;
      }
      return col;
   }

   private int slot(int x, int z) {
      int dx = x - this.x0;
      int dz = z - this.z0;
      if (dx < 0 || dx > 17 || dz < 0 || dz > 17) {
         return -1;
      }
      return dz * 18 + dx;
   }

   @Override
   public int getTile(int x, int y, int z) {
      if (y < 0 || y >= this.level.depth) {
         return 0;
      }
      int s = this.slot(x, z);
      if (s < 0) {
         return this.level.getTile(x, y, z);
      }
      return this.tiles[s][y] & 0xFF;
   }

   @Override
   public BlockState getBlockState(int x, int y, int z) {
      if (y < 0 || y >= this.level.depth) {
         return BlockState.of(null);
      }
      int s = this.slot(x, z);
      if (s < 0) {
         return this.level.getBlockState(x, y, z);
      }
      int id = this.tiles[s][y] & 0xFF;
      int data = this.datas[s] == null ? 0 : this.datas[s][y] & 0xFF;
      return Blocks.stateOf(id, data);
   }

   @Override
   public boolean isBlocker(int x, int y, int z) {
      if (y < 0 || y >= this.level.depth) {
         return false;
      }
      int s = this.slot(x, z);
      int id;
      if (s >= 0) {
         id = this.tiles[s][y] & 0xFF;
      } else {
         byte[] col = this.extColumn(x, z);
         id = col != null ? col[y] & 0xFF : this.level.getTile(x, y, z);
      }
      return Blocks.blocksLight(id);
   }

   @Override
   public boolean isSolidTile(int x, int y, int z) {
      return this.level.isSolidTile(x, y, z);
   }

   @Override
   public float getBrightness(int x, int y, int z) {
      return this.level.getBrightness(x, y, z);
   }

   @Override
   public int getSkyLevel(int x, int y, int z) {
      return this.level.getSkyLevel(x, y, z);
   }

   @Override
   public int getBlockLevel(int x, int y, int z) {
      return this.level.getBlockLevel(x, y, z);
   }
}

