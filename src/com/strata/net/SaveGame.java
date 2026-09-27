package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;

public class SaveGame extends Packet {
   static {
      Packet.register(3, SaveGame::new);
   }

   @Override public int id() { return 3; }
   @Override public void write(DataOutput out) { }
   @Override public void read(DataInput in) { }
}

