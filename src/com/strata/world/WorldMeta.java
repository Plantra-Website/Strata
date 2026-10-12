package com.strata.world;

import com.strata.core.Log;
import com.strata.world.gen.TerrainGenerator;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
      WorldMeta fromTmp = null;
      File tmp = new File(worldDir, "world.dat.tmp");
      if (tmp.isFile()) {
         try {
            return read(worldDir, tmp);
         } catch (Exception e) {
            Log.warn("world", "world.dat.tmp unreadable, falling back to world.dat: " + e.getMessage());
         }
      }
      if (f.isFile()) {
         try {
            return read(worldDir, f);
         } catch (Exception e) {
            Log.warn("world", "world.dat corrupt, trying world.dat.tmp: " + e.getMessage());
            if (tmp.isFile()) {
               try {
                  return read(worldDir, tmp);
               } catch (Exception e2) {
                  Log.warn("world", "world.dat.tmp also unreadable, defaulting seed: " + e2.getMessage());
               }
            }
            return new WorldMeta(TerrainGenerator.DEFAULT_SEED, 0L);
         }
      }
      return new WorldMeta(TerrainGenerator.DEFAULT_SEED, 0L);
   }

   private static WorldMeta read(File worldDir, File f) throws Exception {
      DataInputStream in = new DataInputStream(new FileInputStream(f));
      try {
         int magic = in.readInt();
         int version = in.readInt();
         if (magic != MAGIC) {
            Log.warn("world", "ignoring " + f.getName() + " with bad magic");
            throw new java.io.IOException("bad magic");
         }
         long seed = in.readLong();
         long time = in.readLong();
         if (version != VERSION) {
            Log.warn("world", f.getName() + " is v" + version + ", not v" + VERSION
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
   }

   public static void save(File worldDir, long seed, long time,
         boolean voidWorld, float[] spawnOrNull, String packOrNull) {
      try {
         worldDir.mkdirs();
         File tmp = new File(worldDir, "world.dat.tmp");
         FileOutputStream fos = new FileOutputStream(tmp);
         DataOutputStream out = new DataOutputStream(fos);
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
            out.flush();
            fos.getFD().sync(); 
         } finally {
            out.close();
         }
         File dst = fileFor(worldDir);
         if (tmp.renameTo(dst)) {
            return;
         }
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

