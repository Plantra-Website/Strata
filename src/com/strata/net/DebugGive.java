package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class DebugGive extends Packet {
   static {
      Packet.register(20, DebugGive::new);
   }

   @Override public int id() { return 20; }

   @Override
   public void write(DataOutput out) throws IOException {
   }

   @Override
   public void read(DataInput in) throws IOException {
   }
}

