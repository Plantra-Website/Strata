package com.strata.blocks;

public class TagsTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) {
      BlockTags.load();
      check(Blocks.isFluid(Blocks.LAVA_ID) && Blocks.isFluid(Blocks.WATER_ID), "lava+water fluid");
      check(!Blocks.isFluid(Blocks.STONE_ID) && !Blocks.isFluid(0), "stone+air not fluid");
      check(!Blocks.isFluid(300) && !Blocks.isFluid(-1), "wild ids read false");
      check(Blocks.isLeaves(Blocks.LEAF_ID) && Blocks.isLeaves(Blocks.BIRCH_LEAVES_ID)
         && Blocks.isLeaves(Blocks.SPRUCE_LEAVES_ID), "all leaves");
      check(!Blocks.isLeaves(Blocks.WOOD_ID), "wood not leaves");
      check(Blocks.isLog(Blocks.WOOD_ID) && Blocks.isLog(Blocks.BIRCH_LOG_ID)
         && Blocks.isLog(Blocks.SPRUCE_LOG_ID), "all logs");
      check(Blocks.isSapling(Blocks.SAPLING_ID) && Blocks.isSapling(Blocks.BIRCH_SAPLING_ID)
         && Blocks.isSapling(Blocks.SPRUCE_SAPLING_ID), "all saplings");
      check(!Blocks.isSapling(Blocks.TALL_GRASS_ID), "tuft not sapling");
      check(Blocks.isFlower(Blocks.ROSE_ID) && Blocks.isFlower(Blocks.DANDELION_ID), "both flowers");
      check(Blocks.isMushroom(Blocks.MUSHROOM_BROWN_ID) && Blocks.isMushroom(Blocks.MUSHROOM_RED_ID), "both mushrooms");
      check(!Blocks.isMushroom(Blocks.MUSHROOM_STEM_ID), "giant parts are not ground mushrooms");
      check(Blocks.isOre(Blocks.COAL_ID) && Blocks.isOre(Blocks.IRON_ID) && Blocks.isOre(Blocks.GOLD_ID)
         && Blocks.isOre(Blocks.DIAMOND_ID) && Blocks.isOre(Blocks.LAPIS_ID)
         && Blocks.isOre(Blocks.REDSTONE_ORE_ID), "all six ores");
      check(!Blocks.isOre(Blocks.STONE_ID) && !Blocks.isOre(Blocks.GRAVEL_ID), "stone+gravel not ore");
      check(Blocks.isFalling(Blocks.SAND_ID) && Blocks.isFalling(Blocks.GRAVEL_ID), "sand+gravel fall");
      check(!Blocks.isFalling(Blocks.SANDSTONE_ID), "sandstone stays put");

      if (failures == 0) System.out.println("TAGS PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

