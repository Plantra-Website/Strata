package com.strata.world;

import com.strata.blocks.BlockState;
import com.strata.blocks.Blocks;
import com.strata.world.storage.nbt.NBT;

public class BlockStateTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static class Counter extends BlockEntity {
      int ticks = 0;
      int stored = 0;

      Counter(int x, int y, int z) {
         super(x, y, z);
      }

      @Override public String typeId() { return "counter"; }

      @Override public void tick(Level level) { this.ticks++; }

      @Override public NBT.CompoundTag save() {
         NBT.CompoundTag tag = new NBT.CompoundTag();
         tag.put("stored", new NBT.IntTag(this.stored));
         return tag;
      }

      @Override public void load(NBT.CompoundTag tag) {
         this.stored = tag.integer("stored");
      }
   }

   public static void main(String[] args) {
      check(BlockState.AIR.isAir() && BlockState.AIR.id() == 0, "air state");
      check(BlockState.of(null) == BlockState.AIR, "null block is air");
      check(Blocks.stateOf(0).isAir(), "id 0 is air state");
      check(Blocks.stateOf(999).isAir(), "unknown id is air state");
      BlockState a = Blocks.stateOf(Blocks.STONE_ID);
      check(!a.isAir() && a.data == 0 && a.id() == Blocks.STONE_ID, "default state");
      check(a.equals(Blocks.stateOf(Blocks.STONE_ID)), "state equality");
      check(!a.equals(Blocks.stateOf(Blocks.STONE_ID, 3)), "data distinguishes");
      check(Blocks.stateOf(Blocks.STONE_ID, 3).data == 3, "data carried");
      check(Blocks.byId(Blocks.STONE_ID).defaultState().equals(a), "defaultState()");
      System.out.println("values ok");

      Level l = new Level(64);

      int x = 20, z = -14;
      int top = 63;
      while (top > 0 && l.getTile(x, top, z) == 0) top--;
      check(l.getData(x, top, z) == 0, "data defaults 0");
      check(l.getBlockState(x, top, z).data == 0, "state defaults 0");
      l.setTile(x, top, z, Blocks.stateOf(Blocks.STONE_ID, 5));
      check(l.getTile(x, top, z) == Blocks.STONE_ID, "state edit keeps id");
      check(l.getData(x, top, z) == 5, "state edit stores data");
      check(l.getBlockState(x, top, z).equals(Blocks.stateOf(Blocks.STONE_ID, 5)), "state round-trip");
      l.setTile(x, top, z, Blocks.STONE_ID);
      check(l.getData(x, top, z) == 0, "plain edit clears data");
      check(l.getData(x + 1, top, z) == 0, "data is per-cell");
      System.out.println("data ok");

      check(l.getBlockEntity(x, top, z) == null, "no entity initially");
      l.setTile(x, top, z, Blocks.STONE_ID);
      Counter c = new Counter(x, top, z);
      c.stored = 42;
      l.setBlockEntity(c);
      check(l.getBlockEntity(x, top, z) == c, "entity stored");
      l.tickBlockEntities();
      l.tickBlockEntities();
      check(c.ticks == 2, "entity ticked twice (got " + c.ticks + ")");
      Counter c2 = new Counter(x, top, z);
      c2.load(c.save());
      check(c2.stored == 42, "entity NBT round-trip");
      l.setTile(x, top, z, 0);
      l.tickBlockEntities();
      check(l.getBlockEntity(x, top, z) == null, "entity dropped with block");
      l.setTile(x, top, z, Blocks.STONE_ID);
      Counter c3 = new Counter(x, top, z) {
         @Override public void tick(Level level) { this.remove(); }
      };
      l.setBlockEntity(c3);
      l.tickBlockEntities();
      check(l.getBlockEntity(x, top, z) == null, "self-removed entity dropped");
      check(l.getTile(x, top, z) == Blocks.STONE_ID, "self-remove keeps block");
      System.out.println("entities ok");

      if (failures > 0) { System.out.println(failures + " FAILURES"); System.exit(1); }
      System.out.println("STATE PASS");
   }
}

