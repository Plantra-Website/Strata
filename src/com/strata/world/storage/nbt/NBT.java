package com.strata.world.storage.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NBT {
   private NBT() {
   }

   public abstract static class Tag {
      public abstract byte id();
   }

   public static class ByteTag extends Tag {
      public byte value;
      public ByteTag(byte v) { this.value = v; }
      @Override public byte id() { return 1; }
   }

   public static class ShortTag extends Tag {
      public short value;
      public ShortTag(short v) { this.value = v; }
      @Override public byte id() { return 2; }
   }

   public static class IntTag extends Tag {
      public int value;
      public IntTag(int v) { this.value = v; }
      @Override public byte id() { return 3; }
   }

   public static class LongTag extends Tag {
      public long value;
      public LongTag(long v) { this.value = v; }
      @Override public byte id() { return 4; }
   }

   public static class FloatTag extends Tag {
      public float value;
      public FloatTag(float v) { this.value = v; }
      @Override public byte id() { return 5; }
   }

   public static class DoubleTag extends Tag {
      public double value;
      public DoubleTag(double v) { this.value = v; }
      @Override public byte id() { return 6; }
   }

   public static class ByteArrayTag extends Tag {
      public byte[] value;
      public ByteArrayTag(byte[] v) { this.value = v; }
      @Override public byte id() { return 7; }
   }

   public static class StringTag extends Tag {
      public String value;
      public StringTag(String v) { this.value = v; }
      @Override public byte id() { return 8; }
   }

   public static class ListTag extends Tag {
      public byte elementType;
      public List<Tag> value = new ArrayList<>();
      public ListTag(byte elementType) { this.elementType = elementType; }
      @Override public byte id() { return 9; }
   }

   public static class CompoundTag extends Tag {
      public Map<String, Tag> value = new LinkedHashMap<>();
      @Override public byte id() { return 10; }
      public void put(String name, Tag tag) { this.value.put(name, tag); }
      public Tag get(String name) { return this.value.get(name); }
      public CompoundTag compound(String name) { return (CompoundTag)this.value.get(name); }
      public int integer(String name) { return ((IntTag)this.value.get(name)).value; }
   }

   public static class IntArrayTag extends Tag {
      public int[] value;
      public IntArrayTag(int[] v) { this.value = v; }
      @Override public byte id() { return 11; }
   }

   public static void writeRoot(CompoundTag root, DataOutput out) throws IOException {
      out.writeByte(10);
      out.writeUTF("");
      writeCompoundPayload(root, out);
   }

   public static CompoundTag readRoot(DataInput in) throws IOException {
      int type = in.readByte() & 0xFF;
      if (type != 10) {
         throw new IOException("NBT root is not a compound (type " + type + ")");
      }
      in.readUTF(); 
      CompoundTag root = new CompoundTag();
      readCompoundPayload(root, in);
      return root;
   }

   private static void writeTag(String name, Tag tag, DataOutput out) throws IOException {
      out.writeByte(tag.id());
      out.writeUTF(name);
      writePayload(tag, out);
   }

   private static void writePayload(Tag tag, DataOutput out) throws IOException {
      if (tag instanceof ByteTag) {
         out.writeByte(((ByteTag)tag).value);
      } else if (tag instanceof ShortTag) {
         out.writeShort(((ShortTag)tag).value);
      } else if (tag instanceof IntTag) {
         out.writeInt(((IntTag)tag).value);
      } else if (tag instanceof LongTag) {
         out.writeLong(((LongTag)tag).value);
      } else if (tag instanceof FloatTag) {
         out.writeFloat(((FloatTag)tag).value);
      } else if (tag instanceof DoubleTag) {
         out.writeDouble(((DoubleTag)tag).value);
      } else if (tag instanceof ByteArrayTag) {
         byte[] v = ((ByteArrayTag)tag).value;
         out.writeInt(v.length);
         out.write(v);
      } else if (tag instanceof StringTag) {
         out.writeUTF(((StringTag)tag).value);
      } else if (tag instanceof ListTag list) {
          out.writeByte(list.elementType);
         out.writeInt(list.value.size());
         for (Tag e : list.value) {
            writePayload(e, out);
         }
      } else if (tag instanceof CompoundTag) {
         writeCompoundPayload((CompoundTag)tag, out);
      } else if (tag instanceof IntArrayTag) {
         int[] v = ((IntArrayTag)tag).value;
         out.writeInt(v.length);
         for (int i : v) {
            out.writeInt(i);
         }
      } else {
         throw new IOException("Unknown NBT tag " + tag.getClass());
      }
   }

   private static void writeCompoundPayload(CompoundTag tag, DataOutput out) throws IOException {
      for (Map.Entry<String, Tag> e : tag.value.entrySet()) {
         writeTag(e.getKey(), e.getValue(), out);
      }
      out.writeByte(0); 
   }

   private static void readCompoundPayload(CompoundTag tag, DataInput in) throws IOException {
      while (true) {
         int type = in.readByte() & 0xFF;
         if (type == 0) {
            return;
         }
         String name = in.readUTF();
         tag.value.put(name, readPayload(type, in));
      }
   }

   private static Tag readPayload(int type, DataInput in) throws IOException {
      switch (type) {
         case 1: return new ByteTag(in.readByte());
         case 2: return new ShortTag(in.readShort());
         case 3: return new IntTag(in.readInt());
         case 4: return new LongTag(in.readLong());
         case 5: return new FloatTag(in.readFloat());
         case 6: return new DoubleTag(in.readDouble());
         case 7: {
            int len = in.readInt();
            if (len < 0 || len > 1048576) {
               throw new IOException("oversize byte array tag (" + len + ")");
            }
            byte[] v = new byte[len];
            in.readFully(v);
            return new ByteArrayTag(v);
         }
         case 8: return new StringTag(in.readUTF());
         case 9: {
            int elem = in.readByte() & 0xFF;
            int len = in.readInt();
            if (len < 0 || len > 262144) {
               throw new IOException("oversize list tag (" + len + ")");
            }
            ListTag list = new ListTag((byte)elem);
            for (int i = 0; i < len; i++) {
               list.value.add(readPayload(elem, in));
            }
            return list;
         }
         case 10: {
            CompoundTag c = new CompoundTag();
            readCompoundPayload(c, in);
            return c;
         }
         case 11: {
            int len = in.readInt();
            if (len < 0 || len > 262144) {
               throw new IOException("oversize int array tag (" + len + ")");
            }
            int[] v = new int[len];
            for (int i = 0; i < v.length; i++) {
               v[i] = in.readInt();
            }
            return new IntArrayTag(v);
         }
         default: throw new IOException("Unknown NBT tag id " + type);
      }
   }
}

