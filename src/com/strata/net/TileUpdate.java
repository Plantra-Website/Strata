package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class TileUpdate extends Packet {
   public int x;
   public int y;
   public int z;
   public int type;
   public int data;

   static {
      Packet.register(11, TileUpdate::new);
   }

   @Override public int id() { return 11; }

   @Override
   public void write(DataOutput out) throws IOException {
      out.writeInt(this.x);
      out.writeInt(this.y);
      out.writeInt(this.z);
      out.writeByte(this.type);
      out.writeByte(this.data);
   }

   @Override
   public void read(DataInput in) throws IOException {
      this.x = Packet.readCoord(in);
      this.y = Packet.readCoord(in);
      this.z = Packet.readCoord(in);
      this.type = Packet.readBlockId(in);
      this.data = in.readByte() & 0xFF;
      if (this.data > 15) {
         throw new IOException("data out of range: " + this.data);
      }
   }
}

