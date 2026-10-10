package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class AttackMob extends Packet {
   public int entityId;

   static {
      Packet.register(26, AttackMob::new);
   }

   @Override public int id() { return 26; }

   @Override
   public void write(DataOutput out) throws IOException {
      out.writeInt(this.entityId);
   }

   @Override
   public void read(DataInput in) throws IOException {
      this.entityId = in.readInt();
   }
}

