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
      p.read(in);
      return p;
   }
}

