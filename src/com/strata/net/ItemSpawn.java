package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ItemSpawn extends Packet {
   public int entityId;
   public float x;
   public float y;
   public float z;
   public float xd;
   public float yd;
   public float zd;
   public int blockId;

   static {
      Packet.register(16, ItemSpawn::new);
   }

   @Override public int id() { return 16; }

   @Override
   public void write(DataOutput out) throws IOException {
      out.writeInt(this.entityId);
      out.writeFloat(this.x);
      out.writeFloat(this.y);
      out.writeFloat(this.z);
      out.writeFloat(this.xd);
      out.writeFloat(this.yd);
      out.writeFloat(this.zd);
      out.writeByte(this.blockId);
   }

   @Override
   public void read(DataInput in) throws IOException {
      this.entityId = in.readInt();
      this.x = in.readFloat();
      this.y = in.readFloat();
      this.z = in.readFloat();
      this.xd = in.readFloat();
      this.yd = in.readFloat();
      this.zd = in.readFloat();
      this.blockId = in.readByte() & 0xFF;
   }
}

