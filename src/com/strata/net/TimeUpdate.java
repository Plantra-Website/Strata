package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class TimeUpdate extends Packet {
   public long time;

   static {
      Packet.register(14, TimeUpdate::new);
   }

   @Override public int id() { return 14; }

   @Override
   public void write(DataOutput out) throws IOException {
      out.writeLong(this.time);
   }

   @Override
   public void read(DataInput in) throws IOException {
      this.time = in.readLong();
   }
}

