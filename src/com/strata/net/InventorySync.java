package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class InventorySync extends Packet {
   public static final int SLOTS = 36;
   public final int[] blocks = new int[SLOTS];
   public final int[] counts = new int[SLOTS];
   public int heldBlock = 0;
   public int heldCount = 0;

   static {
      Packet.register(18, InventorySync::new);
   }

   @Override public int id() { return 18; }

   @Override
   public void write(DataOutput out) throws IOException {
      for (int i = 0; i < SLOTS; i++) {
         out.writeByte(this.blocks[i]);
      }
      for (int i = 0; i < SLOTS; i++) {
         out.writeByte(this.counts[i]);
      }
      out.writeByte(this.heldBlock);
      out.writeByte(this.heldCount);
   }

   @Override
   public void read(DataInput in) throws IOException {
      for (int i = 0; i < SLOTS; i++) {
         this.blocks[i] = in.readByte() & 0xFF;
      }
      for (int i = 0; i < SLOTS; i++) {
         this.counts[i] = in.readByte() & 0xFF;
      }
      this.heldBlock = in.readByte() & 0xFF;
      this.heldCount = in.readByte() & 0xFF;
   }
}

