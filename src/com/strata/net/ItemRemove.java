package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ItemRemove extends Packet {
   public int entityId;

   static {
      Packet.register(17, ItemRemove::new);
   }

   @Override public int id() { return 17; }

   @Override
   public void write(DataOutput out) throws IOException {
      out.writeInt(this.entityId);
   }

   @Override
   public void read(DataInput in) throws IOException {
      this.entityId = in.readInt();
      if (this.entityId < 0 || this.entityId > Packet.MAX_ENTITY_ID) {
         throw new IOException("entity id out of range: " + this.entityId);
      }
   }
}

