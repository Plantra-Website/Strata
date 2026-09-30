package com.strata.world;

import com.strata.core.Log;
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
import java.util.zip.GZIPInputStream;

final class LevelStorage {
   private static final int SAVE_MAGIC = 0x52445731; 
   static final int SAVE_VERSION = 2;
   private static final int LEGACY_W = 256;
   private static final int LEGACY_H = 256;
   private static final int LEGACY_D = 64;
   static final int REGION_SIZE = 32; 

   private final Level level;
   private final File worldDir;
   private final HashSet<Long> dirtyRegions = new HashSet<>();
   private final HashSet<Long> loadedRegions = new HashSet<>();
   private boolean warnedNewerSave = false;

   LevelStorage(Level level, File worldDir) {
      this.level = level;
      this.worldDir = worldDir;
   }

   static long regionKey(int rx, int rz) {
      return ((long)rx << 32) | (rz & 0xFFFFFFFFL);
   }

   static long regionKeyForColumn(int x, int z) {
      int ccx = Math.floorDiv(x, 16);
      int ccz = Math.floorDiv(z, 16);
      return regionKey(Math.floorDiv(ccx, REGION_SIZE), Math.floorDiv(ccz, REGION_SIZE));
   }

   void markDirty(int x, int z) {
      this.dirtyRegions.add(regionKeyForColumn(x, z));
   }

   private File regionFile(int rx, int rz) {
      return new File(new File(this.worldDir, "region"), "r." + rx + "." + rz + ".mca");
   }

   private int oldFlatTile(int y) {
      if (y < 0 || y >= this.level.depth) {
         return 0;
      }
      if (y == this.level.groundLevel) {
         return 1;
      } else if (y < this.level.groundLevel && y >= this.level.groundLevel - 3) {
         return 3;
      } else if (y < this.level.groundLevel - 3) {
         return 2;
      }
      return 0;
   }

   private void storeImportedCell(int x, int y, int z, int v) {
      if (y < 0 || y >= this.level.depth) {
         return;
      }
      this.level.editColumn(x, z)[y] = (byte)v;
   }

   void migrateLegacySave() {
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
         for (long key : this.level.columns.keySet()) {
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
         byte[] col = new byte[this.level.depth];
         in.readFully(col);
         for (int y = 0; y < this.level.depth; y++) {
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
            for (int y = 0; y < LEGACY_D && y < this.level.depth; y++) {
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

   void ensureRegions(int minCcx, int minCcz, int maxCcx, int maxCcz) {
      if (!this.level.loadFromDisk) {
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

   void loadAllRegions() {
      if (!this.level.loadFromDisk) {
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
      this.level.skyLight.clearSeeded();
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
               if (y < 0 || y >= this.level.depth) {
                  continue;
               }
               int v = blocks[i] & 0xFF;
               if (v != this.level.defaultTile(x, y, z)) {
                  this.level.editColumn(x, z)[y] = (byte)v;
                  any = true;
               }
               if (data != null && data[i] != 0) {
                  this.level.setData(x, y, z, data[i] & 0xFF);
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
       for (int s = 0; s < this.level.depth / 16; s++) {
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
             if (y < 0 || y >= this.level.depth) {
                v = 0;
             } else {
                byte[] col = this.level.columns.get(Level.columnKey(x, z));
                v = (col == null) ? this.level.defaultTile(x, y, z) : (col[y] & 0xFF);
                byte[] dcol = this.level.dataColumns.get(Level.columnKey(x, z));
                dv = (dcol == null) ? 0 : (dcol[y] & 0xFF);
             }
             blocks[i] = (byte)v;
             data[i] = (byte)dv;
             if (v != this.level.defaultTile(x, y, z)) {
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

   void save() {
      if (!this.level.loadFromDisk || this.dirtyRegions.isEmpty()) {
         return;
      }
      try {
         HashMap<Long, ArrayList<int[]>> byRegion = new HashMap<>();
         for (long rkey : this.dirtyRegions) {
            byRegion.put(rkey, new ArrayList<int[]>());
         }
         for (long ckey : this.level.columns.keySet()) {
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
}

