package com.strata.world.mesh;

public class MeshStateTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) {
      com.strata.world.Level level = new com.strata.world.Level(64, false, null);
      Chunk c = new Chunk(level, 0, 0);

      check(c.dirty, "fresh chunk is dirty");
      check(c.meshState == 0, "fresh chunk meshState 0");
      check(c.dirtyVersion == 0, "fresh chunk version 0");

      c.setDirty();
      check(c.dirtyVersion == 1, "setDirty bumps version to 1");
      check(c.dirty, "setDirty keeps dirty set");

      c.setDirty();
      c.setDirty();
      check(c.dirtyVersion == 3, "three setDirty calls → version 3");
      check(c.dirty, "dirty stays set");

      c.meshState = 1;
      check(c.meshState == 1, "meshState can be 1 (queued)");
      c.meshState = 2;
      check(c.meshState == 2, "meshState can be 2 (ready)");
      c.meshState = 0;
      check(c.meshState == 0, "meshState can return to 0 (none)");

      check(c.pending == null, "pending starts null");

      check(!c.uploaded(), "fresh chunk not uploaded");

      if (failures == 0) System.out.println("MESHSTATE PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

