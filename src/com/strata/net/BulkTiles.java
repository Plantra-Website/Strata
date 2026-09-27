package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BulkTiles extends Packet {
   public Map<Long, byte[]> columns = new HashMap<>();

   static {
      Packet.register(12, BulkTiles::new);
   }

   @Override public int id() { return 12; }

   @Override
   public void write(DataOutput out) throws IOException {
      out.writeInt(this.columns.size());
      for (Map.Entry<Long, byte[]> e : this.columns.entrySet()) {
         out.writeLong(e.getKey());
         byte[] col = e.getValue();
         out.writeInt(col.length);
         out.write(col);
      }
   }

   @Override
   public void read(DataInput in) throws IOException {
      int count = in.readInt();
      this.columns = new HashMap<>(count * 2 + 1);
      for (int i = 0; i < count; i++) {
         long key = in.readLong();
         byte[] col = new byte[in.readInt()];
         in.readFully(col);
         this.columns.put(key, col);
      }
   }
}

