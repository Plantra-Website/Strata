package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class FallingSpawn extends Packet {
   public int entityId;
   public int blockId;
   public float x;
   public float y;
   public float z;

   static {
      Packet.register(22, FallingSpawn::new);
   }

   @Override public int id() { return 22; }

   @Override
   public void write(DataOutput out) throws IOException {
      out.writeInt(this.entityId);
      out.writeInt(this.blockId);
      out.writeFloat(this.x);
      out.writeFloat(this.y);
      out.writeFloat(this.z);
   }

   @Override
   public void read(DataInput in) throws IOException {
      this.entityId = in.readInt();
      this.blockId = in.readInt();
      this.x = in.readFloat();
      this.y = in.readFloat();
      this.z = in.readFloat();
   }
}

