package com.strata.world.storage.nbt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class RegionFile {
   private static final int SECTOR = 4096;
   private static final int HEADER_SECTORS = 2;
   private static final byte VERSION_ZLIB = 2;
   private static final double COMPACT_WASTE_FRACTION = 0.5;
   private final File path;
   private RandomAccessFile file;
   private final int[] offsets = new int[1024];
   private final int[] timestamps = new int[1024];
   private int wastedSectors = 0;

   public RegionFile(File path) throws IOException {
      path.getParentFile().mkdirs();
      this.path = path;
      if (!path.exists()) {
         this.file = new RandomAccessFile(path, "rw");
         for (int i = 0; i < 1024; i++) {
            this.file.writeInt(0);
         }
         for (int i = 0; i < 1024; i++) {
            this.file.writeInt(0);
         }
      } else {
         if (path.length() < SECTOR * HEADER_SECTORS) {
            throw new IOException("truncated region file " + path + " (" + path.length() + " bytes)");
         }
         this.file = new RandomAccessFile(path, "rw");
         this.readHeaders();
         int used = 0;
         for (int i = 0; i < 1024; i++) {
            used += this.offsets[i] & 0xFF;
         }
         int total = (int)(this.file.length() / SECTOR);
         this.wastedSectors = Math.max(0, total - used - HEADER_SECTORS);
      }
   }

   private void readHeaders() throws IOException {
      this.file.seek(0);
      for (int i = 0; i < 1024; i++) {
         this.offsets[i] = this.file.readInt();
      }
      for (int i = 0; i < 1024; i++) {
         this.timestamps[i] = this.file.readInt();
      }
   }

   private static int index(int lx, int lz) {
      return (lx & 31) + ((lz & 31) << 5);
   }

   public synchronized boolean hasChunk(int lx, int lz) {
      return this.offsets[index(lx, lz)] != 0;
   }

   public synchronized byte[] readChunk(int lx, int lz) throws IOException {
      int offset = this.offsets[index(lx, lz)];
      if (offset == 0) {
         return null;
      }
      int sector = offset >> 8;
      int count = offset & 0xFF;
      if (sector < HEADER_SECTORS || count <= 0) {
         throw new IOException("Corrupt region offset for chunk " + lx + "," + lz);
      }
      this.file.seek((long)sector * SECTOR);
      int length = this.file.readInt();
      if (length <= 0 || length > count * SECTOR) {
         throw new IOException("Corrupt region chunk length " + length);
      }
      int version = this.file.readByte() & 0xFF;
      byte[] data = new byte[length - 1];
      this.file.readFully(data);
      if (version == VERSION_ZLIB) {
         return inflate(new InflaterInputStream(new ByteArrayInputStream(data)), length);
      } else if (version == 1) {
         return inflate(new java.util.zip.GZIPInputStream(new ByteArrayInputStream(data)), length);
      }
      throw new IOException("Unknown region chunk version " + version);
   }

   private static byte[] inflate(InputStream in, int hint) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream(hint);
      byte[] buf = new byte[8192];
      int n;
      while ((n = in.read(buf)) > 0) {
         out.write(buf, 0, n);
      }
      in.close();
      return out.toByteArray();
   }

   public synchronized void writeChunk(int lx, int lz, byte[] nbt) throws IOException {
      ByteArrayOutputStream deflated = new ByteArrayOutputStream(nbt.length);
      DeflaterOutputStream out = new DeflaterOutputStream(deflated);
      out.write(nbt);
      out.close();
      byte[] data = deflated.toByteArray();
      int total = data.length + 5; 
      int sectors = (total + SECTOR - 1) / SECTOR;
      int idx = index(lx, lz);
      int old = this.offsets[idx];
      int oldSector = old >> 8;
      int oldCount = old & 0xFF;
      int sector;
      if (old != 0 && oldCount >= sectors) {
         sector = oldSector; 
         this.wastedSectors += oldCount - sectors; 
      } else {
         if (old != 0) {
            this.wastedSectors += oldCount; 
         }
         sector = (int)((this.file.length() + SECTOR - 1) / SECTOR); 
         if (sector < HEADER_SECTORS) {
            sector = HEADER_SECTORS;
         }
      }
      this.file.seek((long)sector * SECTOR);
      this.file.writeInt(data.length + 1);
      this.file.writeByte(VERSION_ZLIB);
      this.file.write(data);
      int pad = sectors * SECTOR - total;
      for (int i = 0; i < pad; i++) {
         this.file.writeByte(0);
      }
      this.offsets[idx] = (sector << 8) | sectors;
      this.timestamps[idx] = (int)(System.currentTimeMillis() / 1000L);
      this.file.seek((long)idx * 4);
      this.file.writeInt(this.offsets[idx]);
      this.file.seek((long)(1024 + idx) * 4);
      this.file.writeInt(this.timestamps[idx]);
   }

    public synchronized void close() throws IOException {
       this.file.close();
    }

    public synchronized void clearChunk(int lx, int lz) throws IOException {
       int idx = index(lx, lz);
       int old = this.offsets[idx];
       if (old == 0) {
          return;
       }
       this.wastedSectors += old & 0xFF;
       this.offsets[idx] = 0;
       this.timestamps[idx] = 0;
       this.file.seek((long)idx * 4);
       this.file.writeInt(0);
       this.file.seek((long)(1024 + idx) * 4);
       this.file.writeInt(0);
    }

   public synchronized double wasteFraction() throws IOException {
      long total = this.file.length() / SECTOR;
      if (total <= HEADER_SECTORS) {
         return 0.0;
      }
      return Math.min(1.0, (double)this.wastedSectors / (double)total);
   }

   public synchronized boolean maybeCompact() throws IOException {
      if (this.wasteFraction() <= COMPACT_WASTE_FRACTION) {
         return false;
      }
      this.compact();
      return true;
   }

   private void compact() throws IOException {
      byte[][] live = new byte[1024][];
      int skipped = 0;
      for (int i = 0; i < 1024; i++) {
         if (this.offsets[i] != 0) {
            int lx = i & 31;
            int lz = (i >> 5) & 31;
            try {
               live[i] = this.readChunk(lx, lz);
            } catch (Exception e) {
               live[i] = null;
               skipped++;
            }
         }
      }
      if (skipped > 0) {
         com.strata.core.Log.warn("world", "compact drops " + skipped
            + " unreadable chunk(s) in " + this.path.getName());
      }
      this.file.close();
      File tmp = new File(this.path.getAbsolutePath() + ".compact-tmp");
      boolean renamed = false;
      try {
         if (tmp.exists() && !tmp.delete()) {
            throw new IOException("cannot clear compact tmp " + tmp);
         }
         this.file = new RandomAccessFile(tmp, "rw");
         for (int i = 0; i < 1024; i++) {
            this.file.writeInt(0);
         }
         for (int i = 0; i < 1024; i++) {
            this.file.writeInt(0);
         }
         java.util.Arrays.fill(this.offsets, 0);
         this.wastedSectors = 0;
         for (int i = 0; i < 1024; i++) {
            if (live[i] != null) {
               int lx = i & 31;
               int lz = (i >> 5) & 31;
               this.writeCompacted(lx, lz, live[i]);
            }
         }
         this.file.close();
         if (!tmp.renameTo(this.path)) {
            throw new IOException("compact rename failed for " + this.path);
         }
         renamed = true;
         this.file = new RandomAccessFile(this.path, "rw");
      } finally {
         if (!renamed) {
            tmp.delete();
            this.file = new RandomAccessFile(this.path, "rw");
            this.readHeaders();
            int used = 0;
            for (int i = 0; i < 1024; i++) {
               used += this.offsets[i] & 0xFF;
            }
            int total = (int)(this.file.length() / SECTOR);
            this.wastedSectors = Math.max(0, total - used - HEADER_SECTORS);
         }
      }
   }

   private void writeCompacted(int lx, int lz, byte[] nbt) throws IOException {
      ByteArrayOutputStream deflated = new ByteArrayOutputStream(nbt.length);
      DeflaterOutputStream out = new DeflaterOutputStream(deflated);
      out.write(nbt);
      out.close();
      byte[] data = deflated.toByteArray();
      int total = data.length + 5;
      int sectors = (total + SECTOR - 1) / SECTOR;
      int idx = index(lx, lz);
      int sector = (int)((this.file.length() + SECTOR - 1) / SECTOR);
      if (sector < HEADER_SECTORS) {
         sector = HEADER_SECTORS;
      }
      this.file.seek((long)sector * SECTOR);
      this.file.writeInt(data.length + 1);
      this.file.writeByte(VERSION_ZLIB);
      this.file.write(data);
      int pad = sectors * SECTOR - total;
      for (int i = 0; i < pad; i++) {
         this.file.writeByte(0);
      }
      this.offsets[idx] = (sector << 8) | sectors;
      this.timestamps[idx] = (int)(System.currentTimeMillis() / 1000L);
      this.file.seek((long)idx * 4);
      this.file.writeInt(this.offsets[idx]);
      this.file.seek((long)(1024 + idx) * 4);
      this.file.writeInt(this.timestamps[idx]);
   }
}

