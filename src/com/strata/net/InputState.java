package com.strata.net;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class InputState extends Packet {
   public boolean fwd;
   public boolean back;
   public boolean left;
   public boolean right;
   public boolean jump;
   public boolean reset;
   public boolean up;
   public boolean down;
   public float yaw;
   public float pitch;
   public boolean breaking;
   public int breakX;
   public int breakY;
   public int breakZ;
   public int breakFace;

   static {
      Packet.register(1, InputState::new);
   }

   @Override public int id() { return 1; }

   @Override
   public void write(DataOutput out) throws IOException {
      int flags = 0;
      if (this.fwd) flags |= 1;
      if (this.back) flags |= 2;
      if (this.left) flags |= 4;
      if (this.right) flags |= 8;
      if (this.jump) flags |= 16;
      if (this.reset) flags |= 32;
      if (this.up) flags |= 64;
      if (this.down) flags |= 128;
      if (this.breaking) flags |= 256;
      out.writeShort(flags);
      out.writeFloat(this.yaw);
      out.writeFloat(this.pitch);
      out.writeInt(this.breakX);
      out.writeInt(this.breakY);
      out.writeInt(this.breakZ);
      out.writeByte(this.breakFace);
   }

   @Override
   public void read(DataInput in) throws IOException {
      int flags = in.readShort() & 0xFFFF;
      this.fwd = (flags & 1) != 0;
      this.back = (flags & 2) != 0;
      this.left = (flags & 4) != 0;
      this.right = (flags & 8) != 0;
      this.jump = (flags & 16) != 0;
      this.reset = (flags & 32) != 0;
      this.up = (flags & 64) != 0;
      this.down = (flags & 128) != 0;
      this.breaking = (flags & 256) != 0;
      this.yaw = in.readFloat();
      this.pitch = in.readFloat();
      this.breakX = in.readInt();
      this.breakY = in.readInt();
      this.breakZ = in.readInt();
      this.breakFace = in.readByte();
   }
}

