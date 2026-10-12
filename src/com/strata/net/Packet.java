package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class Packet {
   public abstract int id();
   public abstract void write(DataOutput out) throws IOException;
   public abstract void read(DataInput in) throws IOException;

   static final int MAX_COLUMNS = 1024;   
   static final int MAX_COLUMN_CELLS = 16 * 16 * 128; 
   static final int MAX_ENTITY_ID = 1 << 20;
   static final int MAX_HP = 20;
   static final int INVENTORY_SLOTS = 36;
   static final int HOTBAR_SLOTS = 9;
   static final int WORLD_DEPTH = 128;
   static final int WORLD_WIDTH = 30_000_000; 

   private static final Map<Integer, Supplier<Packet>> REGISTRY = new HashMap<>();

   static void register(int id, Supplier<Packet> factory) {
      REGISTRY.put(id, factory);
   }

   public static Packet decode(int id, DataInput in) throws IOException {
      Supplier<Packet> factory = REGISTRY.get(id);
      if (factory == null) {
         throw new IOException("Unknown packet id " + id);
      }
      Packet p = factory.get();
      try {
         p.read(in);
      } catch (IOException e) {
         throw new IOException("packet 0x" + Integer.toHexString(id) + " malformed: " + e.getMessage(), e);
      }
      return p;
   }

   static int readBlockId(DataInput in) throws IOException {
      int id = in.readByte() & 0xFF;
      if (id < 0 || id > 255) {
         throw new IOException("block id out of range: " + id);
      }
      return id;
   }

   static int readCoord(DataInput in) throws IOException {
      int v = in.readInt();
      if (v < -WORLD_WIDTH || v > WORLD_WIDTH) {
         throw new IOException("coordinate out of range: " + v);
      }
      return v;
   }
}

