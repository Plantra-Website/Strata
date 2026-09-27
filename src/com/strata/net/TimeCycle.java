package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class TimeCycle extends Packet {
   static {
      Packet.register(21, TimeCycle::new);
   }

   @Override public int id() { return 21; }

   @Override
   public void write(DataOutput out) throws IOException {
   }

   @Override
   public void read(DataInput in) throws IOException {
   }
}

