package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class MobSpawn extends Packet {
   public int entityId;
   public int mobType;
   public float x;
   public float y;
   public float z;

   public static final int ZOMBIE = 0;

   static {
      Packet.register(24, MobSpawn::new);
   }

   @Override public int id() { return 24; }

   @Override
   public void write(DataOutput out) throws IOException {
      out.writeInt(this.entityId);
      out.writeInt(this.mobType);
      out.writeFloat(this.x);
      out.writeFloat(this.y);
      out.writeFloat(this.z);
   }

   @Override
   public void read(DataInput in) throws IOException {
      this.entityId = in.readInt();
      this.mobType = in.readInt();
      this.x = in.readFloat();
      this.y = in.readFloat();
      this.z = in.readFloat();
   }
}

