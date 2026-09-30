package com.strata.world;

import com.strata.blocks.BlockState;
import com.strata.blocks.BlockView;
import com.strata.blocks.Blocks;
import com.strata.blocks.Fluid;
import com.strata.core.AABB;
import com.strata.core.Config;
import com.strata.core.Debug;
import com.strata.core.Dirs;
import com.strata.core.Log;
import com.strata.core.MathHelper;
import com.strata.world.gen.TerrainGenerator;
import com.strata.world.gen.TreeShapes;
import com.strata.world.light.BlockLightEngine;
import com.strata.world.light.LightWorld;
import com.strata.world.light.SkyLightEngine;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Level implements BlockView, LightWorld {
   public final int depth;
   public static final int WORLD_DEPTH = 128;
   public final int groundLevel;
    final ConcurrentHashMap<Long, byte[]> columns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, byte[]> computed = new ConcurrentHashMap<>();
    final SkyLightEngine skyLight;
    private final BlockLightEngine blockLight;
    private final ConcurrentHashMap<Long, Integer> heightCache = new ConcurrentHashMap<>();
   final ConcurrentHashMap<Long, byte[]> dataColumns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, BlockEntity> blockEntities = new ConcurrentHashMap<>();
    private static final float[] LIGHT_CURVE = new float[16];

   static {
      for (int i = 0; i < 16; i++) {
         LIGHT_CURVE[i] = (float)Math.pow(i / 15.0, 1.3);
      }
   }
   private final TerrainGenerator gen;
   final LevelStorage storage;
   final boolean loadFromDisk;
   private float[] spawnPoint = null;
   private final ArrayList<LevelListener> levelListeners = new ArrayList<>();
    public static final int SAVE_VERSION = LevelStorage.SAVE_VERSION;

   public Level(int depth) {
      this(depth, true);
   }

    public Level(int depth, boolean loadFromDisk) {
       this(depth, loadFromDisk, new TerrainGenerator(TerrainGenerator.DEFAULT_SEED, depth));
    }

    public Level(int depth, boolean loadFromDisk, TerrainGenerator gen) {
       this(depth, loadFromDisk, gen, new File("."));
    }

    public Level(int depth, boolean loadFromDisk, TerrainGenerator gen, File worldDir) {
       this.depth = depth;
       this.groundLevel = depth * 2 / 3;
       this.gen = gen;
       this.loadFromDisk = loadFromDisk;
       this.storage = new LevelStorage(this, worldDir);
       this.fluids = new FluidSimulator(this);
       this.skyLight = new SkyLightEngine(this);
       this.blockLight = new BlockLightEngine(this);
       if (loadFromDisk) {
          this.storage.migrateLegacySave();
       }
    }

    public void setSpawnPoint(float x, float y, float z) {
       this.spawnPoint = new float[]{x, y, z};
    }

    public float[] spawnPoint() {
       return this.spawnPoint == null ? null : this.spawnPoint.clone();
    }


    public TerrainGenerator generator() {
       return this.gen;
    }

    public HashMap<Long, byte[]> snapshotEdits() {
       HashMap<Long, byte[]> out = new HashMap<>(this.columns.size() * 2 + 1);
       for (Map.Entry<Long, byte[]> e : this.columns.entrySet()) {
          out.put(e.getKey(), e.getValue().clone());
       }
       return out;
    }

    public HashMap<Long, byte[]> snapshotData() {
       HashMap<Long, byte[]> out = new HashMap<>(this.dataColumns.size() * 2 + 1);
       for (Map.Entry<Long, byte[]> e : this.dataColumns.entrySet()) {
          out.put(e.getKey(), e.getValue().clone());
       }
       return out;
    }

   public ArrayList<int[]> emittersNear(float px, float py, float pz, float radius, int cap) {
      ArrayList<int[]> out = new ArrayList<>();
      for (Map.Entry<Long, byte[]> e : this.columns.entrySet()) {
         long key = e.getKey();
         int x = (int)(key >> 32);
         int z = (int)(key & 0xFFFFFFFFL);
         float dx = x + 0.5F - px;
         float dz = z + 0.5F - pz;
         if (dx * dx + dz * dz > radius * radius) {
            continue;
         }
         byte[] col = e.getValue();
         for (int y = 0; y < this.depth && y < col.length; y++) {
            int id = col[y] & 0xFF;
            if (Blocks.isEmitterId(id) && Math.abs(y + 0.5F - py) <= radius) {
               out.add(new int[]{x, y, z});
               if (out.size() >= cap) {
                  return out;
               }
            }
         }
      }
      return out;
   }

   public void seedEmitters() {
      for (Map.Entry<Long, byte[]> e : this.columns.entrySet()) {
         long key = e.getKey();
         int x = (int)(key >> 32);
         int z = (int)(key & 0xFFFFFFFFL);
         byte[] col = e.getValue();
         for (int y = 0; y < this.depth && y < col.length; y++) {
            int id = col[y] & 0xFF;
            if (Blocks.isEmitterId(id)) {
               int want = Blocks.emitterLevel(id);
               if (this.getBlockLevel(x, y, z) < want) {
                  this.floodAdd(x, y, z, want);
               }
            }
         }
      }
   }

    public void setTileBulk(int x, int y, int z, int type) {
       if (y < 0 || y >= this.depth) {
          return;
       }
       long key = columnKey(x, z);
       this.editColumn(x, z)[y] = (byte)type;
       this.computed.remove(key);
       this.storage.markDirty(x, z);
    }

    public void applyBulk(Map<Long, byte[]> edits) {
       this.applyBulk(edits, null);
    }

    public void applyBulk(Map<Long, byte[]> edits, Map<Long, byte[]> datas) {
       for (Map.Entry<Long, byte[]> e : edits.entrySet()) {
          this.columns.put(e.getKey(), e.getValue());
       }
       if (datas != null) {
          for (Map.Entry<Long, byte[]> e : datas.entrySet()) {
             this.dataColumns.put(e.getKey(), e.getValue());
          }
       }
       this.computed.clear();
       this.skyLight.clear();
    }

   public int evictFar(int pcx, int pcz, int keepChunks) {
      int evicted = evictWindow(this.computed, pcx, pcz, keepChunks);
      evicted += evictWindow(this.heightCache, pcx, pcz, keepChunks);
      return evicted + this.gen.evictFar(pcx, pcz, keepChunks);
   }

   private static int evictWindow(Map<Long, ?> map, int pcx, int pcz, int keepChunks) {
      int evicted = 0;
      var it = map.entrySet().iterator();
      while (it.hasNext()) {
         long k = it.next().getKey();
         int cx = Math.floorDiv((int)(k >> 32), 16);
         int cz = Math.floorDiv((int)(k & 0xFFFFFFFFL), 16);
         if (Math.abs(cx - pcx) > keepChunks || Math.abs(cz - pcz) > keepChunks) {
            it.remove();
            evicted++;
         }
      }
      return evicted;
   }

   int computedSize() {
      return this.computed.size();
   }

   int heightCacheSize() {
      return this.heightCache.size();
   }

   static long columnKey(int x, int z) {
      return ((long)x << 32) | (z & 0xFFFFFFFFL);
   }



   int defaultTile(int x, int y, int z) {
      if (y < 0 || y >= this.depth) {
         return 0;
      }
      long key = columnKey(x, z);
      Integer h = this.heightCache.get(key);
      if (h == null) {
         h = this.gen.heightAt(x, z);
         this.heightCache.put(key, h);
      }
      return this.gen.blockAt(x, y, z, h);
   }






    private byte[] defaultColumn(int x, int z) {
       byte[] col = new byte[this.depth];
       for (int y = 0; y < this.depth; y++) {
          col[y] = (byte)this.defaultTile(x, y, z);
       }
       return col;
    }

    byte[] editColumn(int x, int z) {
       long key = columnKey(x, z);
       byte[] col = this.columns.get(key);
       if (col == null) {
          byte[] fresh = this.defaultColumn(x, z);
          byte[] prev = this.columns.putIfAbsent(key, fresh);
          col = (prev != null) ? prev : fresh;
       }
       return col;
    }

   public void ensureRegions(int minCcx, int minCcz, int maxCcx, int maxCcz) {
      this.storage.ensureRegions(minCcx, minCcz, maxCcx, maxCcz);
   }

   public void warmCarves(int minCcx, int minCcz, int maxCcx, int maxCcz) {
      for (int ccx = minCcx; ccx <= maxCcx; ccx++) {
         for (int ccz = minCcz; ccz <= maxCcz; ccz++) {
            this.gen.touchCarve(ccx, ccz);
         }
      }
   }

   public void loadAllRegions() {
      this.storage.loadAllRegions();
   }




   public void save() {
      this.storage.save();
   }

    public void addListener(LevelListener levelListener) {
      this.levelListeners.add(levelListener);
   }

   public int getTile(int x, int y, int z) {
       if (y < 0 || y >= this.depth) {
          return 0;
       }
       long key = columnKey(x, z);
       byte[] col = this.columns.get(key);
       if (col != null) {
          return col[y] & 0xFF;
       }
       col = this.computed.get(key);
       if (col == null) {
          byte[] fresh = this.defaultColumn(x, z);
          byte[] prev = this.computed.putIfAbsent(key, fresh);
          col = (prev != null) ? prev : fresh;
       }
       return col[y] & 0xFF;
    }

   public boolean isSolidTile(int x, int y, int z) {
      int id = this.getTile(x, y, z);
      if (id == Blocks.SNOW_LAYER_ID) {
         return (this.getData(x, y, z) & 7) >= 3;
      }
      return Blocks.isSolid(id);
   }

    public int getData(int x, int y, int z) {
       if (y < 0 || y >= this.depth) {
          return 0;
       }
       byte[] col = this.dataColumns.get(columnKey(x, z));
       return col == null ? 0 : col[y] & 0xFF;
    }

    public BlockState getBlockState(int x, int y, int z) {
       return Blocks.stateOf(this.getTile(x, y, z), this.getData(x, y, z));
    }

    static long entityKey(int x, int y, int z) {
       return ((long)(x & 0x3FFFFFF) << 38) | ((long)(z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    public BlockEntity getBlockEntity(int x, int y, int z) {
       return this.blockEntities.get(entityKey(x, y, z));
    }

    public void setBlockEntity(BlockEntity entity) {
       if (entity != null) {
          this.blockEntities.put(entityKey(entity.x, entity.y, entity.z), entity);
       }
    }

    public void removeBlockEntity(int x, int y, int z) {
       this.blockEntities.remove(entityKey(x, y, z));
    }

    public void tickBlockEntities() {
       if (this.blockEntities.isEmpty()) {
          return;
       }
       ArrayList<BlockEntity> snapshot = new ArrayList<>(this.blockEntities.values());
       for (int i = 0; i < snapshot.size(); i++) {
          BlockEntity e = snapshot.get(i);
          if (e.isRemoved() || this.getTile(e.x, e.y, e.z) == 0) {
             this.blockEntities.remove(entityKey(e.x, e.y, e.z), e);
             continue;
          }
          e.tick(this);
          if (e.isRemoved()) {
             this.blockEntities.remove(entityKey(e.x, e.y, e.z), e);
          }
       }
    }

    public boolean isLightBlocker(int x, int y, int z) {
       return Blocks.blocksLight(this.getTile(x, y, z));
    }

   public static final int SAPLING_TICKS = FluidSimulator.SAPLING_TICKS;
   public static final int GROWTH_TICKS = FluidSimulator.GROWTH_TICKS;
   final FluidSimulator fluids;

   public void scheduleTick(int x, int y, int z, int delay) {
      this.fluids.scheduleTick(x, y, z, delay);
   }

   public void tickScheduled() {
      this.fluids.tickScheduled();
   }


   public static final int LAVA_TICKS = Fluid.LAVA.ticks; 
   public static final int LAVA_RANGE = Fluid.LAVA.range;
   public static final int WATER_TICKS = Fluid.WATER.ticks; 
   public static final int WATER_RANGE = Fluid.WATER.range;



   void growTree(int x, int y, int z) {
      if (!this.isSolidTile(x, y - 1, z)) {
         return;
      }
      int sapling = this.getTile(x, y, z);
      boolean birch = sapling == Blocks.BIRCH_SAPLING_ID;
      boolean spruce = sapling == Blocks.SPRUCE_SAPLING_ID;
      int species = birch ? TerrainGenerator.BIRCH
         : spruce ? TerrainGenerator.SPRUCE_SHORT : TerrainGenerator.OAK;
      int log = TerrainGenerator.logForSpecies(species);
      int leaves = TerrainGenerator.leavesForSpecies(species);
      int th = birch ? 5 + (((x * 374761393 + z * 668265263) & 0x7FFFFFFF) % 3)
         : 4 + (((x * 374761393 + z * 668265263) & 0x7FFFFFFF) % 3);
      for (int i = 0; i < th; i++) {
         if (this.getTile(x, y + i, z) != 0 && this.getTile(x, y + i, z) != sapling) {
            return;
         }
      }
      int top = y - 1 + th;
      for (int i = 0; i < th; i++) {
         this.setTile(x, y + i, z, log);
      }
      for (int ly = top - (spruce ? 3 : 2); ly <= top + 1; ly++) {
         for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
               boolean leaf = spruce ? TreeShapes.spruce(dx, ly - top, dz)
                  : TreeShapes.round(dx, ly - top, dz, x + dx, z + dz);
               if (leaf && this.getTile(x + dx, ly, z + dz) == 0) {
                  this.setTile(x + dx, ly, z + dz, leaves);
               }
            }
         }
      }
   }

   public ArrayList<AABB> getCubes(AABB aABB) {
      ArrayList<AABB> aABBs = new ArrayList<>();
      int x0 = MathHelper.floor(aABB.x0);
      int x1 = MathHelper.floor(aABB.x1) + 1;
      int y0 = MathHelper.floor(aABB.y0);
      int y1 = MathHelper.floor(aABB.y1) + 1;
      int z0 = MathHelper.floor(aABB.z0);
      int z1 = MathHelper.floor(aABB.z1) + 1;
      if (y0 < 0) {
         y0 = 0;
      }

      if (y1 > this.depth) {
         y1 = this.depth;
      }

      for (int x = x0; x < x1; x++) {
         for (int y = y0; y < y1; y++) {
            for (int z = z0; z < z1; z++) {
               if (this.isSolidTile(x, y, z)) {
                  aABBs.add(new AABB(x, y, z, x + 1, y + 1, z + 1));
               }
            }
         }
      }

      return aABBs;
   }

    @Override
    public int depth() {
       return this.depth;
    }

    @Override
    public void lightColumnChanged(int x, int z, int y0, int y1) {
       for (int i = 0; i < this.levelListeners.size(); i++) {
          this.levelListeners.get(i).lightColumnChanged(x, z, y0, y1);
       }
    }

    public void reconcileSkyChunk(int ccx, int ccz) {
       this.skyLight.reconcileSkyChunk(ccx, ccz);
    }

    private volatile int skylightSub = 0;

    public void setSkylightSub(int sub) {
       this.skylightSub = sub < 0 ? 0 : (sub > 11 ? 11 : sub);
    }

    public int skylightSub() {
       return this.skylightSub;
    }

    public float getBrightness(int x, int y, int z) {
       int sky = this.getSkyLevel(x, y, z) - this.skylightSub;
       if (sky < 0) {
          sky = 0;
       }
       float skyB = LIGHT_CURVE[sky & 15];
       float block = LIGHT_CURVE[this.getBlockLevel(x, y, z) & 15];
       return skyB > block ? skyB : block;
    }

    public int getSkyLevel(int x, int y, int z) {
       return this.skyLight.get(x, y, z);
    }

    public int getBlockLevel(int x, int y, int z) {
       return this.blockLight.get(x, y, z);
    }

    public void floodAdd(int x, int y, int z, int level) {
       this.blockLight.floodAdd(x, y, z, level);
    }

    public void setTile(int x, int y, int z, BlockState state) {
       this.setTile(x, y, z, state.id());
       this.setData(x, y, z, state.data);
    }

    void setData(int x, int y, int z, int data) {
       if (y < 0 || y >= this.depth) {
          return;
       }
       data = data & 0xFF;
       long key = columnKey(x, z);
       byte[] col = this.dataColumns.get(key);
       int prev = col == null ? 0 : col[y] & 0xFF;
       if (prev == data) {
          return;
       }
       if (data == 0) {
          col[y] = 0;
       } else {
          if (col == null) {
             byte[] fresh = new byte[this.depth];
             byte[] old = this.dataColumns.putIfAbsent(key, fresh);
             col = (old != null) ? old : fresh;
          }
          col[y] = (byte)data;
       }
       for (int i = 0; i < this.levelListeners.size(); i++) {
          this.levelListeners.get(i).tileChanged(x, y, z);
       }
    }

     public void setTile(int x, int y, int z, int type) {
       if (y < 0 || y >= this.depth) {
          return;
       }
       long t0 = System.nanoTime();
       try {
          this.setTileInner(x, y, z, type);
       } finally {
          long ms = (System.nanoTime() - t0) / 1000000L;
          Debug.slow("setTile", ms, Config.SLOW_SETTILE_MS,
             "edit " + x + "," + y + "," + z + " -> " + type);
       }
     }

     private void setTileInner(int x, int y, int z, int type) {
      if (y < 0 || y >= this.depth) {
         return;
      }
       long key = columnKey(x, z);
       int oldDepth = this.skyLight.opaqueHeight(x, z);
       byte[] col = this.columns.get(key);
       if (col == null) {
          if (type == this.defaultTile(x, y, z)) {
             return;
          }
          byte[] base = this.computed.get(key);
          byte[] fresh = (base != null) ? base.clone() : this.defaultColumn(x, z);
          byte[] prev = this.columns.putIfAbsent(key, fresh);
          col = (prev != null) ? prev : fresh;
       }
       int prev = this.getTile(x, y, z);
       col[y] = (byte)type;
       this.computed.remove(key);
       this.setData(x, y, z, 0);
       if (type == 0) {
          this.removeBlockEntity(x, y, z);
       }
       this.skyLight.invalidate(x, z);
       this.storage.markDirty(x, z);
       int newDepth = this.skyLight.opaqueHeight(x, z);

       int oldLight = this.getBlockLevel(x, y, z);
       int newCellLevel = Blocks.isEmitterId(type) ? Blocks.emitterLevel(type) : 0;
       if (oldLight > newCellLevel) {
          this.blockLight.floodRemove(x, y, z, oldLight);
          if (newCellLevel > 0) {
             this.blockLight.floodAdd(x, y, z, newCellLevel);
          }
       } else if (newCellLevel > oldLight) {
          this.blockLight.floodAdd(x, y, z, newCellLevel);
       }
       boolean wasOpaque = Blocks.blocksLight(prev);
       boolean isOpaque = Blocks.blocksLight(type);
       this.skyLight.onEdit(x, y, z, wasOpaque, isOpaque, oldDepth, newDepth);
       if (wasOpaque && !isOpaque) {
           for (int[] d : Dirs.DIRS) {
             int nl = this.getBlockLevel(x + d[0], y + d[1], z + d[2]);
             if (nl > 0) {
                this.floodAdd(x + d[0], y + d[1], z + d[2], nl);
             }
          }
       }

      for (int i = 0; i < this.levelListeners.size(); i++) {
         this.levelListeners.get(i).tileChanged(x, y, z);
      }
      if (oldDepth != newDepth) {
         int yl0 = oldDepth < newDepth ? oldDepth : newDepth;
         int yl1 = oldDepth > newDepth ? oldDepth : newDepth;
         for (int i = 0; i < this.levelListeners.size(); i++) {
            this.levelListeners.get(i).lightColumnChanged(x, z, yl0, yl1);
         }
      }
   }
}

