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
   }

   @Override
   public void read(DataInput in) throws IOException {
      this.x = in.readInt();
      this.y = in.readInt();
      this.z = in.readInt();
      this.face = in.readByte();
      this.blockId = in.readByte() & 0xFF;
   }
}

