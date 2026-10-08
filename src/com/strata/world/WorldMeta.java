package com.strata.world;

import com.strata.core.Log;
import com.strata.world.gen.TerrainGenerator;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public final class WorldMeta {
   private static final int MAGIC = 0x5244574D; 
   private static final int VERSION = 6;
   public static final int SLOTS = 36;

   public final long seed;
   public final long time;
   public final boolean voidWorld;
   public final boolean hasSpawn;
   public final float spawnX;
   public final float spawnY;
   public final float spawnZ;
   public final String pack;

   public WorldMeta(long seed, long time) {
      this(seed, time, false, null);
   }

   public WorldMeta(long seed, long time, boolean voidWorld, float[] spawnOrNull) {
      this(seed, time, voidWorld, spawnOrNull, "");
   }

   public WorldMeta(long seed, long time, boolean voidWorld, float[] spawnOrNull,
         String packOrNull) {
      this.seed = seed;
      this.time = time;
      this.voidWorld = voidWorld;
      this.hasSpawn = spawnOrNull != null && spawnOrNull.length >= 3;
      this.spawnX = this.hasSpawn ? spawnOrNull[0] : 0;
      this.spawnY = this.hasSpawn ? spawnOrNull[1] : 0;
      this.spawnZ = this.hasSpawn ? spawnOrNull[2] : 0;
      this.pack = packOrNull == null ? "" : packOrNull;
   }

   public static File fileFor(File worldDir) {
      return new File(worldDir, "world.dat");
   }

   public static WorldMeta load(File worldDir) {
      File f = fileFor(worldDir);
      if (!f.isFile()) {
         File tmp = new File(worldDir, "world.dat.tmp");
         if (tmp.isFile()) {
            Log.warn("world", "world.dat missing, recovering from world.dat.tmp");
            f = tmp;
         } else {
            return new WorldMeta(TerrainGenerator.DEFAULT_SEED, 0L);
         }
      }
      try {
         DataInputStream in = new DataInputStream(new FileInputStream(f));
         try {
            int magic = in.readInt();
            int version = in.readInt();
            if (magic != MAGIC) {
               Log.warn("world", "ignoring world.dat with bad magic");
               return new WorldMeta(TerrainGenerator.DEFAULT_SEED, 0L);
            }
            long seed = in.readLong();
            long time = in.readLong();
            if (version != VERSION) {
               Log.warn("world", "world.dat is v" + version + ", not v" + VERSION
                  + " — keeping seed/time, rest defaults");
               return new WorldMeta(seed, time);
            }
            boolean voidWorld = in.readByte() != 0;
            float[] spawn = null;
            if (in.readByte() != 0) {
               spawn = new float[]{in.readFloat(), in.readFloat(), in.readFloat()};
            }
            String pack = in.readUTF();
            return new WorldMeta(seed, time, voidWorld, spawn, pack);
         } finally {
            in.close();
         }
      } catch (Exception e) {
         Log.warn("world", "could not read world.dat, using defaults: " + e.getMessage());
         return new WorldMeta(TerrainGenerator.DEFAULT_SEED, 0L);
      }
   }

   public static void save(File worldDir, long seed, long time,
         boolean voidWorld, float[] spawnOrNull, String packOrNull) {
      try {
         worldDir.mkdirs();
         File tmp = new File(worldDir, "world.dat.tmp");
         DataOutputStream out = new DataOutputStream(new FileOutputStream(tmp));
         try {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeLong(seed);
            out.writeLong(time);
            out.writeByte(voidWorld ? 1 : 0);
            boolean hasSpawn = spawnOrNull != null && spawnOrNull.length >= 3;
            out.writeByte(hasSpawn ? 1 : 0);
            if (hasSpawn) {
               out.writeFloat(spawnOrNull[0]);
               out.writeFloat(spawnOrNull[1]);
               out.writeFloat(spawnOrNull[2]);
            }
            out.writeUTF(packOrNull == null ? "" : packOrNull);
         } finally {
            out.close();
         }
         File dst = fileFor(worldDir);
         if (dst.exists() && !dst.delete()) {
            Log.warn("world", "could not replace world.dat");
            return;
         }
         if (!tmp.renameTo(dst)) {
            Log.warn("world", "could not write world.dat");
         }
      } catch (Exception e) {
         Log.error("world", "world meta save failed", e);
      }
   }
}

