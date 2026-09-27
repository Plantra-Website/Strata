package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PlayerState extends Packet {
   public float x;
   public float y;
   public float z;
   public float yaw;
   public float pitch;
   public boolean onGround;
   public boolean spectator;
   public float breakProgress;
   public int hp;

   static {
      Packet.register(13, PlayerState::new);
   }

   @Override public int id() { return 13; }

   @Override
   public void write(DataOutput out) throws IOException {
      out.writeFloat(this.x);
      out.writeFloat(this.y);
      out.writeFloat(this.z);
      out.writeFloat(this.yaw);
      out.writeFloat(this.pitch);
      int flags = 0;
      if (this.onGround) flags |= 1;
      if (this.spectator) flags |= 2;
      out.writeByte(flags);
      out.writeFloat(this.breakProgress);
      out.writeInt(this.hp);
   }

   @Override
   public void read(DataInput in) throws IOException {
      this.x = in.readFloat();
      this.y = in.readFloat();
      this.z = in.readFloat();
      this.yaw = in.readFloat();
      this.pitch = in.readFloat();
      int flags = in.readByte() & 0xFF;
      this.onGround = (flags & 1) != 0;
      this.spectator = (flags & 2) != 0;
      this.breakProgress = in.readFloat();
      this.hp = in.readInt();
   }
}

