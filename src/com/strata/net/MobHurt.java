package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class MobHurt extends Packet {
   public int entityId;
   public int hp;
   public float xd;
   public float zd;

   static {
      Packet.register(25, MobHurt::new);
   }

   @Override public int id() { return 25; }

   @Override
   public void write(DataOutput out) throws IOException {
      out.writeInt(this.entityId);
      out.writeInt(this.hp);
      out.writeFloat(this.xd);
      out.writeFloat(this.zd);
   }

   @Override
   public void read(DataInput in) throws IOException {
      this.entityId = in.readInt();
      this.hp = in.readInt();
      this.xd = in.readFloat();
      this.zd = in.readFloat();
   }
}

