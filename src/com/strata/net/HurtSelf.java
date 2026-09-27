package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class HurtSelf extends Packet {
   static {
      Packet.register(23, HurtSelf::new);
   }

   @Override public int id() { return 23; }

   @Override
   public void write(DataOutput out) throws IOException {
   }

   @Override
   public void read(DataInput in) throws IOException {
   }
}

