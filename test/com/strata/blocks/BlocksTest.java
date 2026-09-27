package com.strata.blocks;

public class BlocksTest {
   public static void main(String[] args) {
      check(Blocks.byId(0) == null, "air has no instance");
      check(Blocks.byId(999) == null, "unknown id null");
      check(Blocks.byId(Blocks.GRASS_ID).texture == AtlasStitcher.slot("blocks/grass_side.png"), "grass side tile");
      check(Blocks.byId(Blocks.DIAMOND_ID).texture == AtlasStitcher.slot("blocks/diamond_ore.png"), "diamond tile");
      check(Blocks.byId(Blocks.TORCH_ID).texture == AtlasStitcher.slot("blocks/torch_on.png"), "torch tile");
      CubeBlock grass = (CubeBlock)Blocks.byId(Blocks.GRASS_ID);
      check(grass.topTexture == AtlasStitcher.slot("blocks/grass_top.png"), "grass top");
      check(grass.bottomTexture == AtlasStitcher.slot("blocks/dirt.png"), "grass bottom dirt");
      check(grass.overlayTexture == AtlasStitcher.slot("blocks/grass_side_overlay.png"), "grass overlay");
      check(Blocks.byId(Blocks.LEAF_ID).texture == AtlasStitcher.slot("blocks/tinted/oak_leaves.png"), "oak leaves");
      CubeBlock wood = (CubeBlock)Blocks.byId(Blocks.WOOD_ID);
      check(wood.sideTexture == AtlasStitcher.slot("blocks/oak_log_side.png"), "log side");
      check(wood.topTexture == AtlasStitcher.slot("blocks/oak_log_top.png"), "log top");
      check(wood.bottomTexture == wood.topTexture, "log top==bottom");
      check(Blocks.byId(Blocks.ROSE_ID) instanceof CrossBlock, "rose is a cross");
      check(Blocks.byId(Blocks.DANDELION_ID) instanceof CrossBlock, "dandelion is a cross");
      check(Blocks.byId(Blocks.SAPLING_ID) instanceof CrossBlock, "sapling is a cross");
      check(Blocks.byId(Blocks.GRAVEL_ID) instanceof CubeBlock, "gravel is a cube");
      check(Blocks.byId(Blocks.LAPIS_ID) instanceof CubeBlock, "lapis is a cube");
      check(Blocks.particleTile(Blocks.STONE_ID) == Blocks.byId(Blocks.STONE_ID).texture, "uniform particle tile");
      check(Blocks.particleTile(Blocks.GRASS_ID) == grass.sideTexture, "grass particle shows side");
      check(Blocks.particleTile(0) == 0, "air particle tile 0");
      check(Blocks.particleTile(999) == 0, "unknown particle tile 0");
      check(Blocks.isSolid(Blocks.STONE_ID), "stone solid");
      check(Blocks.isSolid(Blocks.LAVA_ID), "lava solid (walkable, no fluids yet)");
      check(Blocks.isSolid(Blocks.GRAVEL_ID), "gravel solid (static until tick queue)");
      check(!Blocks.isSolid(Blocks.TORCH_ID), "torch walk-through");
      check(!Blocks.isSolid(Blocks.SAPLING_ID), "sapling walk-through");
      check(!Blocks.isSolid(0), "air not solid");
      check(Blocks.isTranslucent(0) && Blocks.isTranslucent(Blocks.TORCH_ID), "air+torch translucent");
      check(!Blocks.isTranslucent(Blocks.DIRT_ID), "dirt occludes");
      check(Blocks.blocksLight(Blocks.STONE_ID), "stone blocks light");
      check(Blocks.blocksLight(Blocks.WOOD_ID), "wood blocks light");
      check(!Blocks.blocksLight(Blocks.LEAF_ID), "leaves pass light");
      check(!Blocks.blocksLight(Blocks.TORCH_ID), "torch passes light");
      check(!Blocks.blocksLight(0), "air passes light");
      check(Blocks.emission(Blocks.LAVA_ID) == 15, "lava emits 15");
      check(Blocks.emission(Blocks.TORCH_ID) == 14, "torch emits 14");
      check(Blocks.emission(Blocks.GRASS_ID) == 0, "grass dark");
      check(Blocks.emission(0) == 0, "air dark");
      System.out.println("BLOCKS PASS");
   }

   private static void check(boolean cond, String msg) {
      if (!cond) {
         System.out.println("FAIL: " + msg);
         System.exit(1);
      }
   }
}

