package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class MobHurt extends Packet {
   public int entityId;
   public int hp;
   public float xd;
   public float yd;
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
      out.writeFloat(this.yd);
      out.writeFloat(this.zd);
   }

   @Override
   public void read(DataInput in) throws IOException {
      this.entityId = in.readInt();
      if (this.entityId < 0 || this.entityId > Packet.MAX_ENTITY_ID) {
         throw new IOException("entity id out of range: " + this.entityId);
      }
      this.hp = in.readInt();
      if (this.hp < 0 || this.hp > Packet.MAX_HP) {
         throw new IOException("hp out of range: " + this.hp);
      }
      this.xd = in.readFloat();
      this.yd = in.readFloat();
      this.zd = in.readFloat();
      if (!Float.isFinite(this.xd) || !Float.isFinite(this.yd) || !Float.isFinite(this.zd)) {
         throw new IOException("non-finite knockback velocity");
      }
   }
}

