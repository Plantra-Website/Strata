package com.strata.world;

import com.strata.core.Log;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public final class PlayerData {
   private static final int MAGIC = 0x52445750; 
   private static final int VERSION = 2;

   public final boolean present;
   public final float x;
   public final float y;
   public final float z;
   public final float yaw;
   public final float pitch;
   public final int hp;
   public final int[] blocks = new int[WorldMeta.SLOTS];
   public final int[] counts = new int[WorldMeta.SLOTS];
   public final int heldBlock;
   public final int heldCount;

   public PlayerData(float x, float y, float z, float yaw, float pitch, int hp) {
      this(x, y, z, yaw, pitch, hp, null, null, 0, 0);
   }

   public PlayerData(float x, float y, float z, float yaw, float pitch, int hp,
         int[] blocks, int[] counts, int heldBlock, int heldCount) {
      this.present = true;
      this.x = x;
      this.y = y;
      this.z = z;
      this.yaw = yaw;
      this.pitch = pitch;
      this.hp = hp < 1 ? 20 : (hp > 20 ? 20 : hp);
      for (int i = 0; i < WorldMeta.SLOTS; i++) {
         this.blocks[i] = (blocks != null && i < blocks.length) ? blocks[i] : 0;
         this.counts[i] = (counts != null && i < counts.length) ? counts[i] : 0;
      }
      this.heldBlock = heldBlock;
      this.heldCount = heldCount;
   }

   private PlayerData() {
      this.present = false;
      this.x = 0.0F;
      this.y = 0.0F;
      this.z = 0.0F;
      this.yaw = 0.0F;
      this.pitch = 0.0F;
      this.hp = 20;
      this.heldBlock = 0;
      this.heldCount = 0;
   }

   public static File fileFor(File worldDir) {
      return new File(worldDir, "player.dat");
   }

   public static PlayerData load(File worldDir) {
      File f = fileFor(worldDir);
      if (!f.isFile()) {
         File tmp = new File(worldDir, "player.dat.tmp");
         if (tmp.isFile()) {
            Log.warn("world", "player.dat missing, recovering from player.dat.tmp");
            f = tmp;
         } else {
            return new PlayerData();
         }
      }
      try {
         DataInputStream in = new DataInputStream(new FileInputStream(f));
         try {
            int magic = in.readInt();
            int version = in.readInt();
            if (magic != MAGIC) {
               Log.warn("world", "ignoring player.dat with bad magic");
               return new PlayerData();
            }
            if (version > VERSION) {
               Log.warn("world", "player.dat version " + version + " newer than " + VERSION + ", reading anyway");
            }
            boolean present = in.readByte() != 0;
            float x = 0.0F, y = 0.0F, z = 0.0F, yaw = 0.0F, pitch = 0.0F;
            int hp = 20;
            int[] blocks = new int[WorldMeta.SLOTS];
            int[] counts = new int[WorldMeta.SLOTS];
            int heldBlock = 0, heldCount = 0;
            if (present) {
               x = in.readFloat();
               y = in.readFloat();
               z = in.readFloat();
               yaw = in.readFloat();
               pitch = in.readFloat();
               hp = in.readInt();
               for (int i = 0; i < WorldMeta.SLOTS; i++) {
                  blocks[i] = in.readByte() & 0xFF;
               }
               for (int i = 0; i < WorldMeta.SLOTS; i++) {
                  counts[i] = in.readByte() & 0xFF;
               }
               heldBlock = in.readByte() & 0xFF;
               heldCount = in.readByte() & 0xFF;
            }
            if (!present) {
               return new PlayerData();
            }
            return new PlayerData(x, y, z, yaw, pitch, hp, blocks, counts, heldBlock, heldCount);
         } finally {
            in.close();
         }
      } catch (Exception e) {
         Log.warn("world", "could not read player.dat, scattering: " + e.getMessage());
         return new PlayerData();
      }
   }

   public static void save(File worldDir, float x, float y, float z, float yaw, float pitch, int hp,
         int[] blocks, int[] counts, int heldBlock, int heldCount) {
      try {
         worldDir.mkdirs();
         File tmp = new File(worldDir, "player.dat.tmp");
         DataOutputStream out = new DataOutputStream(new FileOutputStream(tmp));
         try {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeByte(1);
            out.writeFloat(x);
            out.writeFloat(y);
            out.writeFloat(z);
            out.writeFloat(yaw);
            out.writeFloat(pitch);
            out.writeInt(hp);
            for (int i = 0; i < WorldMeta.SLOTS; i++) {
               out.writeByte((blocks != null && i < blocks.length) ? blocks[i] : 0);
            }
            for (int i = 0; i < WorldMeta.SLOTS; i++) {
               out.writeByte((counts != null && i < counts.length) ? counts[i] : 0);
            }
            out.writeByte(heldBlock);
            out.writeByte(heldCount);
         } finally {
            out.close();
         }
         File dst = fileFor(worldDir);
         if (dst.exists() && !dst.delete()) {
            Log.warn("world", "could not replace player.dat");
            return;
         }
         if (!tmp.renameTo(dst)) {
            Log.warn("world", "could not write player.dat");
         }
      } catch (Exception e) {
         Log.error("world", "player save failed", e);
      }
   }
}

