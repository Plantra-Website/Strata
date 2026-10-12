package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PlaceBlock extends Packet {
   public int x;
   public int y;
   public int z;
   public int face;
   public int blockId;
   public int slot;

   static {
      Packet.register(2, PlaceBlock::new);
   }

   @Override public int id() { return 2; }

   @Override
   public void write(DataOutput out) throws IOException {
      out.writeInt(this.x);
      out.writeInt(this.y);
      out.writeInt(this.z);
      out.writeByte(this.face);
      out.writeByte(this.blockId);
      out.writeByte(this.slot);
   }

   @Override
   public void read(DataInput in) throws IOException {
      this.x = Packet.readCoord(in);
      this.y = Packet.readCoord(in);
      this.z = Packet.readCoord(in);
      this.face = in.readByte();
      if (this.face < 0 || this.face > 5) {
         throw new IOException("face out of range: " + this.face);
      }
      this.blockId = Packet.readBlockId(in);
      int slot = in.readByte() & 0xFF;
      if (slot > Packet.HOTBAR_SLOTS - 1) {
         throw new IOException("slot out of range: " + slot);
      }
      this.slot = slot;
   }
}

