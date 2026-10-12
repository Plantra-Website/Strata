package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SlotClick extends Packet {
   public int slot;

   static {
      Packet.register(19, SlotClick::new);
   }

   @Override public int id() { return 19; }

   @Override
   public void write(DataOutput out) throws IOException {
      out.writeByte(this.slot);
   }

   @Override
   public void read(DataInput in) throws IOException {
      this.slot = in.readByte() & 0xFF;
      if (this.slot < 0 || this.slot >= Packet.INVENTORY_SLOTS) {
         throw new IOException("slot out of range: " + this.slot);
      }
   }
}

