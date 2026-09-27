package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class TileUpdate extends Packet {
   public int x;
   public int y;
   public int z;
   public int type;

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
   }

   @Override
   public void read(DataInput in) throws IOException {
      this.x = in.readInt();
      this.y = in.readInt();
      this.z = in.readInt();
      this.type = in.readByte() & 0xFF;
   }
}

