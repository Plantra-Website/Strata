package com.strata.world.mesh;

import com.strata.blocks.AtlasStitcher;

public class CrackStageTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) {
      check(LevelRenderer.crackStageFor(0.0F) == 0, "zero clamps to 0");
      check(LevelRenderer.crackStageFor(-1.0F) == 0, "negative clamps to 0");
      check(LevelRenderer.crackStageFor(1.0F) == 9, "one clamps to 9");
      check(LevelRenderer.crackStageFor(5.0F) == 9, "overrun clamps to 9");
      check(LevelRenderer.crackStageFor(0.04F) == 0, "first tick stage 0");
      check(LevelRenderer.crackStageFor(0.15F) == 1, "0.15 -> 1");
      check(LevelRenderer.crackStageFor(0.99F) == 9, "0.99 -> 9");
      for (int s = 0; s < 10; s++) {
         int slot = AtlasStitcher.slot("blocks/destroy_stage_" + s + ".png");
         float[] uv = AtlasStitcher.uv(slot);
         check(uv[2] > uv[0] && uv[3] > uv[1], "stage " + s + " has area");
      }
      if (failures == 0) System.out.println("CRACK PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

