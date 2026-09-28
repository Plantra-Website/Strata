package com.strata.world;

import com.strata.blocks.BlockState;
import com.strata.blocks.BlockView;
import com.strata.blocks.Blocks;
import com.strata.core.AABB;
import com.strata.core.Config;
import com.strata.core.Debug;
import com.strata.core.Log;
import com.strata.core.MathHelper;
import com.strata.world.gen.TerrainGenerator;
import com.strata.world.light.BlockLightEngine;
import com.strata.world.light.LightWorld;
import com.strata.world.light.SkyLightEngine;
import com.strata.world.storage.nbt.NBT;
import com.strata.world.storage.nbt.RegionFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

public class Level implements BlockView, LightWorld {
   public final int depth;
   public final int groundLevel;
    private final ConcurrentHashMap<Long, byte[]> columns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, byte[]> computed = new ConcurrentHashMap<>();
    private final SkyLightEngine skyLight;
    private final BlockLightEngine blockLight;
    private final ConcurrentHashMap<Long, Integer> heightCache = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<Long, byte[]> dataColumns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, BlockEntity> blockEntities = new ConcurrentHashMap<>();
    private static final float[] LIGHT_CURVE = new float[16];

   static {
      for (int i = 0; i < 16; i++) {
         LIGHT_CURVE[i] = (float)Math.pow(i / 15.0, 1.3);
      }
   }
   private final TerrainGenerator gen;
   private final HashSet<Long> dirtyRegions = new HashSet<>();
   private final HashSet<Long> loadedRegions = new HashSet<>();
   private final boolean loadFromDisk;
   private float[] spawnPoint = null;
   private final File worldDir;
   private final ArrayList<LevelListener> levelListeners = new ArrayList<>();
    private static final int SAVE_MAGIC = 0x52445731; 
    public static final int SAVE_VERSION = 2;
    private boolean warnedNewerSave = false;
   private static final int LEGACY_W = 256;
   private static final int LEGACY_H = 256;
   private static final int LEGACY_D = 64;
   private static final int REGION_SIZE = 32; 

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
       this.worldDir = worldDir;
       this.skyLight = new SkyLightEngine(this);
       this.blockLight = new BlockLightEngine(this);
       if (loadFromDisk) {
          this.migrateLegacySave();
       }
    }

    public void setSpawnPoint(float x, float y, float z) {
       this.spawnPoint = new float[]{x, y, z};
    }

    public float[] spawnPoint() {
       return this.spawnPoint == null ? null : this.spawnPoint.clone();
    }

    private File regionFile(int rx, int rz) {
       return new File(new File(this.worldDir, "region"), "r." + rx + "." + rz + ".mca");
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
       this.dirtyRegions.add(regionKeyForColumn(x, z));
    }

    public void applyBulk(Map<Long, byte[]> edits) {
       for (Map.Entry<Long, byte[]> e : edits.entrySet()) {
          this.columns.put(e.getKey(), e.getValue());
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

   private static long columnKey(int x, int z) {
      return ((long)x << 32) | (z & 0xFFFFFFFFL);
   }

   private static long regionKey(int rx, int rz) {
      return ((long)rx << 32) | (rz & 0xFFFFFFFFL);
   }

   private static long regionKeyForColumn(int x, int z) {
      int ccx = Math.floorDiv(x, 16);
      int ccz = Math.floorDiv(z, 16);
      return regionKey(Math.floorDiv(ccx, REGION_SIZE), Math.floorDiv(ccz, REGION_SIZE));
   }

   private int defaultTile(int x, int y, int z) {
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

   private int oldFlatTile(int y) {
      if (y < 0 || y >= this.depth) {
         return 0;
      }
      if (y == this.groundLevel) {
         return 1;
      } else if (y < this.groundLevel && y >= this.groundLevel - 3) {
         return 3;
      } else if (y < this.groundLevel - 3) {
         return 2;
      }
      return 0;
   }

    private void storeImportedCell(int x, int y, int z, int v) {
       if (y < 0 || y >= this.depth) {
          return;
       }
       this.editColumn(x, z)[y] = (byte)v;
    }

   private void migrateLegacySave() {
      File regionDir = new File(this.worldDir, "region");
      File legacy = new File(this.worldDir, "level.dat");
      if (regionDir.exists() || !legacy.exists()) {
         return;
      }
      try {
         ByteArrayOutputStream raw = new ByteArrayOutputStream();
         DataInputStream dis = new DataInputStream(new GZIPInputStream(new FileInputStream(legacy)));
         byte[] buf = new byte[65536];
         int n;
         while ((n = dis.read(buf)) > 0) {
            raw.write(buf, 0, n);
         }
         dis.close();
         byte[] data = raw.toByteArray();
         if (data.length >= 4 && ((data[0] & 0xFF) << 24 | (data[1] & 0xFF) << 16 | (data[2] & 0xFF) << 8 | (data[3] & 0xFF)) == SAVE_MAGIC) {
            this.importSparse(data);
         } else if (data.length == LEGACY_W * LEGACY_H * LEGACY_D) {
            this.importFullArray(data);
         } else {
             Log.warn("world", "ignoring level.dat with unknown format (" + data.length + " bytes)");
            return;
         }
         File renamed = new File(this.worldDir, "level.dat.imported");
         if (renamed.exists()) {
            renamed.delete();
         }
         if (!legacy.renameTo(renamed)) {
             Log.warn("world", "could not rename level.dat after import, it may re-import next launch");
         } else {
             Log.info("world", "migrated level.dat to region files (kept as level.dat.imported)");
         }
         for (long key : this.columns.keySet()) {
            int x = (int)(key >> 32);
            int z = (int)(key & 0xFFFFFFFFL);
            this.dirtyRegions.add(regionKeyForColumn(x, z));
         }
       } catch (Exception e) {
          Log.error("world", "migration failed", e);
       }
    }

    private void importSparse(byte[] data) throws Exception {
      DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
      in.readInt(); 
      int count = in.readInt();
      int kept = 0;
      for (int i = 0; i < count; i++) {
         long key = in.readLong();
         int x = (int)(key >> 32);
         int z = (int)(key & 0xFFFFFFFFL);
         byte[] col = new byte[this.depth];
         in.readFully(col);
         for (int y = 0; y < this.depth; y++) {
            int v = col[y] & 0xFF;
            if (v != this.oldFlatTile(y)) {
               this.storeImportedCell(x, y, z, v);
               kept++;
            }
         }
      }
       Log.info("world", "imported sparse save (" + kept + " edited cells)");
   }

   private void importFullArray(byte[] data) {
      int imported = 0;
      for (int x = 0; x < LEGACY_W; x++) {
         for (int z = 0; z < LEGACY_H; z++) {
            for (int y = 0; y < LEGACY_D && y < this.depth; y++) {
               int old = data[(y * LEGACY_H + z) * LEGACY_W + x] & 0xFF;
               if (old != this.oldFlatTile(y)) {
                  this.storeImportedCell(x, y, z, old);
                  imported++;
               }
            }
         }
      }
       Log.info("world", "imported legacy save (" + imported + " edited cells)");
   }

    private byte[] defaultColumn(int x, int z) {
       byte[] col = new byte[this.depth];
       for (int y = 0; y < this.depth; y++) {
          col[y] = (byte)this.defaultTile(x, y, z);
       }
       return col;
    }

    private byte[] editColumn(int x, int z) {
       long key = columnKey(x, z);
       byte[] col = this.columns.get(key);
       if (col == null) {
          byte[] fresh = this.defaultColumn(x, z);
          byte[] prev = this.columns.putIfAbsent(key, fresh);
          col = (prev != null) ? prev : fresh;
       }
       return col;
    }

   public void ensureRegions(int minCcx, int minCcz, int maxCcx, int maxCcz) {      if (!this.loadFromDisk) {
         return;
      }
      int rx0 = Math.floorDiv(minCcx, REGION_SIZE);
      int rx1 = Math.floorDiv(maxCcx, REGION_SIZE);
      int rz0 = Math.floorDiv(minCcz, REGION_SIZE);
      int rz1 = Math.floorDiv(maxCcz, REGION_SIZE);
      for (int rx = rx0; rx <= rx1; rx++) {
         for (int rz = rz0; rz <= rz1; rz++) {
            if (this.loadedRegions.add(regionKey(rx, rz))) {
               this.loadRegion(rx, rz);
            }
         }
      }
   }

   public void warmCarves(int minCcx, int minCcz, int maxCcx, int maxCcz) {
      for (int ccx = minCcx; ccx <= maxCcx; ccx++) {
         for (int ccz = minCcz; ccz <= maxCcz; ccz++) {
            this.gen.touchCarve(ccx, ccz);
         }
      }
   }

   public void loadAllRegions() {
      if (!this.loadFromDisk) {
         return;
      }
      File dir = new File(this.worldDir, "region");
      File[] files = dir.listFiles();
      if (files == null) {
         return;
      }
      for (File f : files) {
         String name = f.getName();
         if (!name.startsWith("r.") || !name.endsWith(".mca")) {
            continue;
         }
         String[] parts = name.substring(2, name.length() - 4).split("\\.");
         if (parts.length != 2) {
            continue;
         }
         try {
            int rx = Integer.parseInt(parts[0]);
            int rz = Integer.parseInt(parts[1]);
            if (this.loadedRegions.add(regionKey(rx, rz))) {
               this.loadRegion(rx, rz);
            }
         } catch (NumberFormatException e) {
            Log.warn("world", "skipping odd region file " + name);
         }
      }
   }

    private void loadRegion(int rx, int rz) {
       File file = this.regionFile(rx, rz);
       if (!file.exists()) {
          return;
       }
       this.skyLight.clearSeeded();
      int count = 0;
      try {
         RegionFile region = new RegionFile(file);
         for (int lx = 0; lx < REGION_SIZE; lx++) {
            for (int lz = 0; lz < REGION_SIZE; lz++) {
               if (!region.hasChunk(lx, lz)) {
                  continue;
               }
               byte[] nbt = region.readChunk(lx, lz);
               if (nbt != null && this.readColumnChunk(nbt)) {
                  count++;
               }
            }
         }
         region.close();
      } catch (Exception e) {
          Log.warn("world", "failed to load region " + rx + "," + rz + ": " + e);
         return;
      }
      if (count > 0) {
          Log.info("world", "loaded region " + rx + "," + rz + " (" + count + " columns)");
      }
   }

   private boolean readColumnChunk(byte[] nbt) {
      try {
         NBT.CompoundTag root = NBT.readRoot(new DataInputStream(new ByteArrayInputStream(nbt)));
         NBT.CompoundTag lvl = root.compound("Level");
         if (lvl == null) {
            return false;
         }
          int ccx = lvl.integer("xPos");
          int ccz = lvl.integer("zPos");
          NBT.Tag versionTag = lvl.get("DataVersion");
          int dataVersion = versionTag instanceof NBT.IntTag ? lvl.integer("DataVersion") : 0;
          if (dataVersion > SAVE_VERSION && !this.warnedNewerSave) {
             this.warnedNewerSave = true;
             Log.warn("world", "save version " + dataVersion + " newer than " + SAVE_VERSION + ", loading anyway");
          }
         NBT.Tag sectionsTag = lvl.get("Sections");
         if (!(sectionsTag instanceof NBT.ListTag)) {
            return false;
         }
         boolean any = false;
         for (NBT.Tag t : ((NBT.ListTag)sectionsTag).value) {
            if (!(t instanceof NBT.CompoundTag sec)) {
               continue;
            }
             NBT.Tag yTag = sec.get("Y");
            NBT.Tag blocksTag = sec.get("Blocks");
            if (!(yTag instanceof NBT.ByteTag) || !(blocksTag instanceof NBT.ByteArrayTag)) {
               continue;
            }
            byte[] blocks = ((NBT.ByteArrayTag)blocksTag).value;
            if (blocks.length != 4096) {
               continue;
            }
            int s = ((NBT.ByteTag)yTag).value;
            NBT.Tag dataTag = sec.get("Data");
            byte[] data = null;
            if (dataTag instanceof NBT.ByteArrayTag && ((NBT.ByteArrayTag)dataTag).value.length == 4096) {
               data = ((NBT.ByteArrayTag)dataTag).value;
            }
            for (int i = 0; i < 4096; i++) {
               int ly = (i >> 8) & 15;
               int lz = (i >> 4) & 15;
               int lx = i & 15;
               int x = ccx * 16 + lx;
               int y = s * 16 + ly;
               int z = ccz * 16 + lz;
               if (y < 0 || y >= this.depth) {
                  continue;
               }
               int v = blocks[i] & 0xFF;
               if (v != this.defaultTile(x, y, z)) {
                  this.editColumn(x, z)[y] = (byte)v;
                  any = true;
               }
               if (data != null && data[i] != 0) {
                  this.setData(x, y, z, data[i] & 0xFF);
               }
            }
         }
         return any;
      } catch (Exception e) {
          Log.warn("world", "skipping corrupt column chunk: " + e);
         return false;
      }
   }

   private byte[] writeColumnChunk(int ccx, int ccz) throws Exception {
       NBT.CompoundTag root = new NBT.CompoundTag();
       NBT.CompoundTag lvl = new NBT.CompoundTag();
       lvl.put("xPos", new NBT.IntTag(ccx));
       lvl.put("zPos", new NBT.IntTag(ccz));
       lvl.put("DataVersion", new NBT.IntTag(SAVE_VERSION));
       NBT.ListTag sections = new NBT.ListTag((byte)10);
       for (int s = 0; s < this.depth / 16; s++) {
          byte[] blocks = new byte[4096];
          byte[] data = new byte[4096];
          boolean differs = false;
          boolean hasData = false;
          for (int i = 0; i < 4096; i++) {
             int ly = (i >> 8) & 15;
             int lz = (i >> 4) & 15;
             int lx = i & 15;
             int x = ccx * 16 + lx;
             int y = s * 16 + ly;
             int z = ccz * 16 + lz;
             int v;
             int dv = 0;
             if (y < 0 || y >= this.depth) {
                v = 0;
             } else {
                byte[] col = this.columns.get(columnKey(x, z));
                v = (col == null) ? this.defaultTile(x, y, z) : (col[y] & 0xFF);
                byte[] dcol = this.dataColumns.get(columnKey(x, z));
                dv = (dcol == null) ? 0 : (dcol[y] & 0xFF);
             }
             blocks[i] = (byte)v;
             data[i] = (byte)dv;
             if (v != this.defaultTile(x, y, z)) {
                differs = true;
             }
             if (dv != 0) {
                hasData = true;
             }
          }
          if (differs || hasData) {
             NBT.CompoundTag sec = new NBT.CompoundTag();
             sec.put("Y", new NBT.ByteTag((byte)s));
             sec.put("Blocks", new NBT.ByteArrayTag(blocks));
             if (hasData) {
                sec.put("Data", new NBT.ByteArrayTag(data));
             }
             sections.value.add(sec);
          }
       }
      lvl.put("Sections", sections);
      root.put("Level", lvl);
      ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
      NBT.writeRoot(root, new DataOutputStream(out));
      return out.toByteArray();
   }

   public void save() {
      if (!this.loadFromDisk || this.dirtyRegions.isEmpty()) {
         return;
      }
      try {
         HashMap<Long, ArrayList<int[]>> byRegion = new HashMap<>();
         for (long rkey : this.dirtyRegions) {
            byRegion.put(rkey, new ArrayList<int[]>());
         }
         for (long ckey : this.columns.keySet()) {
            int x = (int)(ckey >> 32);
            int z = (int)(ckey & 0xFFFFFFFFL);
            long rkey = regionKeyForColumn(x, z);
            ArrayList<int[]> list = byRegion.get(rkey);
            if (list != null) {
               int ccx = Math.floorDiv(x, 16);
               int ccz = Math.floorDiv(z, 16);
               int[] cc = new int[]{ccx, ccz};
               boolean seen = false;
               for (int[] e : list) {
                  if (e[0] == ccx && e[1] == ccz) {
                     seen = true;
                     break;
                  }
               }
               if (!seen) {
                  list.add(cc);
               }
            }
         }
         for (Map.Entry<Long, ArrayList<int[]>> e : byRegion.entrySet()) {
            long rkey = e.getKey();
            int rx = (int)(rkey >> 32);
            int rz = (int)(rkey & 0xFFFFFFFFL);
            RegionFile region = new RegionFile(this.regionFile(rx, rz));
            for (int[] cc : e.getValue()) {
               int lx = Math.floorMod(cc[0], REGION_SIZE);
               int lz = Math.floorMod(cc[1], REGION_SIZE);
               region.writeChunk(lx, lz, this.writeColumnChunk(cc[0], cc[1]));
            }
            if (region.maybeCompact()) {
               Log.info("world", "compacted region " + rx + "," + rz + " (waste exceeded 50%)");
            }
            region.close();
         }
          Log.info("world", "saved " + byRegion.size() + " region(s)");
         this.dirtyRegions.clear();
       } catch (Exception e) {
          Log.error("world", "save failed", e);
       }
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
       return Blocks.isSolid(this.getTile(x, y, z));
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

    private static long entityKey(int x, int y, int z) {
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

   public static final int SAPLING_TICKS = 12000;
   private final HashMap<Long, Long> scheduled = new HashMap<>();
   private long scheduledClock = 0;

   public void scheduleTick(int x, int y, int z, int delay) {
      long key = entityKey(x, y, z);
      long due = this.scheduledClock + delay;
      Long cur = this.scheduled.get(key);
      if (cur == null || due < cur) {
         this.scheduled.put(key, due);
      }
   }

   public void tickScheduled() {
      this.scheduledClock++;
      if (this.scheduled.isEmpty()) {
         return;
      }
      ArrayList<Long> due = new ArrayList<>();
      for (Map.Entry<Long, Long> e : this.scheduled.entrySet()) {
         if (e.getValue() <= this.scheduledClock) {
            due.add(e.getKey());
         }
      }
      for (int i = 0; i < due.size(); i++) {
         long key = due.get(i);
         this.scheduled.remove(key);
         int ex = (int)(key >> 38);
         if ((ex & 0x2000000) != 0) {
            ex |= ~0x3FFFFFF;
         }
         int ez = (int)((key >> 12) & 0x3FFFFFF);
         if ((ez & 0x2000000) != 0) {
            ez |= ~0x3FFFFFF;
         }
         this.onScheduledTick(ex, (int)(key & 0xFFF), ez);
      }
   }

   private void onScheduledTick(int x, int y, int z) {
      if (this.getTile(x, y, z) == Blocks.SAPLING_ID) {
         this.growTree(x, y, z);
      } else if (this.getTile(x, y, z) == Blocks.LAVA_ID) {
         this.flowLava(x, y, z);
      }
   }

   public static final int LAVA_TICKS = 90; 
   public static final int LAVA_RANGE = 4;

   private void flowLava(int x, int y, int z) {
      int level = this.getData(x, y, z);
      if (y > 0 && this.getTile(x, y - 1, z) == 0) {
         this.placeFlow(x, y - 1, z, level);
         return;
      }
      if (level >= LAVA_RANGE) {
         return;
      }
      if (this.getTile(x + 1, y, z) == 0) {
         this.placeFlow(x + 1, y, z, level + 1);
         return;
      }
      if (this.getTile(x - 1, y, z) == 0) {
         this.placeFlow(x - 1, y, z, level + 1);
         return;
      }
      if (this.getTile(x, y, z + 1) == 0) {
         this.placeFlow(x, y, z + 1, level + 1);
         return;
      }
      if (this.getTile(x, y, z - 1) == 0) {
         this.placeFlow(x, y, z - 1, level + 1);
         return;
      }
   }

   private void placeFlow(int x, int y, int z, int level) {
      this.setTile(x, y, z, Blocks.LAVA_ID);
      this.setData(x, y, z, level);
      this.scheduleTick(x, y, z, LAVA_TICKS);
   }

   void growTree(int x, int y, int z) {
      if (!this.isSolidTile(x, y - 1, z)) {
         return;
      }
      int th = 4 + (((x * 374761393 + z * 668265263) & 0x7FFFFFFF) % 3);
      for (int i = 0; i < th; i++) {
         if (this.getTile(x, y + i, z) != 0 && this.getTile(x, y + i, z) != Blocks.SAPLING_ID) {
            return;
         }
      }
      int top = y - 1 + th;
      for (int i = 0; i < th; i++) {
         this.setTile(x, y + i, z, Blocks.WOOD_ID);
      }
      for (int ly = top - 2; ly <= top + 1; ly++) {
         for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
               int adx = dx < 0 ? -dx : dx;
               int adz = dz < 0 ? -dz : dz;
               boolean leaf = false;
               if (ly == top - 2 || ly == top - 1) {
                  leaf = adx <= 2 && adz <= 2 && !(adx == 2 && adz == 2);
               } else if (ly == top) {
                  leaf = adx <= 1 && adz <= 1 && !(dx == 0 && dz == 0);
               } else {
                  leaf = adx + adz <= 1;
               }
               if (leaf && this.getTile(x + dx, ly, z + dz) == 0) {
                  this.setTile(x + dx, ly, z + dz, Blocks.LEAF_ID);
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

    private void setData(int x, int y, int z, int data) {
       if (y < 0 || y >= this.depth) {
          return;
       }
       long key = columnKey(x, z);
       if (data == 0) {
          byte[] col = this.dataColumns.get(key);
          if (col != null) {
             col[y] = 0;
          }
          return;
       }
       byte[] col = this.dataColumns.get(key);
       if (col == null) {
          byte[] fresh = new byte[this.depth];
          byte[] prev = this.dataColumns.putIfAbsent(key, fresh);
          col = (prev != null) ? prev : fresh;
       }
       col[y] = (byte)(data & 0xFF);
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
       this.dirtyRegions.add(regionKeyForColumn(x, z));
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
          int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
          for (int[] d : dirs) {
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

