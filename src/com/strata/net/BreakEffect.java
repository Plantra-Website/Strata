package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class BreakEffect extends Packet {
   public int x;
   public int y;
   public int z;
   public int texIndex;

   static {
      Packet.register(15, BreakEffect::new);
   }

   @Override public int id() { return 15; }

   @Override
   public void write(DataOutput out) throws IOException {
      out.writeInt(this.x);
      out.writeInt(this.y);
      out.writeInt(this.z);
      out.writeByte(this.texIndex);
   }

   @Override
   public void read(DataInput in) throws IOException {
      this.x = Packet.readCoord(in);
      this.y = Packet.readCoord(in);
      this.z = Packet.readCoord(in);
      this.texIndex = in.readByte() & 0xFF;
      if (this.texIndex < 0 || this.texIndex > 255) {
         throw new IOException("texIndex out of range: " + this.texIndex);
      }
   }
}

