package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;

public class SpectateToggle extends Packet {
   static {
      Packet.register(4, SpectateToggle::new);
   }

   @Override public int id() { return 4; }
   @Override public void write(DataOutput out) { }
   @Override public void read(DataInput in) { }
}

