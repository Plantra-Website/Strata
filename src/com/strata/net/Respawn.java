package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Respawn extends Packet {
   static {
      Packet.register(27, Respawn::new);
   }

   @Override public int id() { return 27; }

   @Override
   public void write(DataOutput out) throws IOException {
   }

   @Override
   public void read(DataInput in) throws IOException {
   }
}

