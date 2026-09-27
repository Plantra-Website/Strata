package com.strata.blocks;

import com.strata.core.Rng;

public final class Blocks {
   public static final int GRASS_ID = 1;
   public static final int STONE_ID = 2;
   public static final int DIRT_ID = 3;
   public static final int COBBLE_ID = 4;
   public static final int SAND_ID = 5;
   public static final int BEDROCK_ID = 6;
   public static final int COAL_ID = 7;
   public static final int IRON_ID = 8;
   public static final int GOLD_ID = 9;
   public static final int DIAMOND_ID = 10;
   public static final int LAVA_ID = 11;
   public static final int TORCH_ID = 12;
   public static final int LEAF_ID = 13;
   public static final int ROSE_ID = 14;
   public static final int TALL_GRASS_ID = 15;
   public static final int WOOD_ID = 16;
   public static final int DANDELION_ID = 17;
   public static final int GRAVEL_ID = 18;
   public static final int LAPIS_ID = 19;
   public static final int SAPLING_ID = 20;
   public static final int PLANKS_ID = 21;

   private static final Block[] BY_ID = new Block[256];

   static {
      int dirt = AtlasStitcher.slot("blocks/dirt.png");
      register(new CubeBlock(GRASS_ID,
         AtlasStitcher.slot("blocks/grass_top.png"),
         AtlasStitcher.slot("blocks/grass_side.png"), dirt,
         AtlasStitcher.slot("blocks/grass_side_overlay.png"), 0));
      register(new CubeBlock(STONE_ID, AtlasStitcher.slot("blocks/stone.png")));
      register(new CubeBlock(DIRT_ID, dirt));
      register(new CubeBlock(COBBLE_ID, AtlasStitcher.slot("blocks/cobblestone.png")));
      register(new CubeBlock(SAND_ID, AtlasStitcher.slot("blocks/sand.png")));
      register(new CubeBlock(BEDROCK_ID, AtlasStitcher.slot("blocks/bedrock.png")));
      register(new CubeBlock(COAL_ID, AtlasStitcher.slot("blocks/coal_ore.png")));
      register(new CubeBlock(IRON_ID, AtlasStitcher.slot("blocks/iron_ore.png")));
      register(new CubeBlock(GOLD_ID, AtlasStitcher.slot("blocks/gold_ore.png")));
      register(new CubeBlock(DIAMOND_ID, AtlasStitcher.slot("blocks/diamond_ore.png")));
      register(new CubeBlock(LAPIS_ID, AtlasStitcher.slot("blocks/lapis_ore.png")));
      register(new CubeBlock(GRAVEL_ID, AtlasStitcher.slot("blocks/gravel.png")));
      register(new CubeBlock(LAVA_ID, AtlasStitcher.slot("blocks/lava_still.png"), 15));
      register(new TorchBlock(TORCH_ID, AtlasStitcher.slot("blocks/torch_on.png"), 14));
      register(new CubeBlock(LEAF_ID, AtlasStitcher.slot("blocks/tinted/oak_leaves.png")));
      register(new CrossBlock(ROSE_ID, AtlasStitcher.slot("blocks/rose.png")));
      register(new CrossBlock(DANDELION_ID, AtlasStitcher.slot("blocks/dandelion.png")));
      register(new CrossBlock(TALL_GRASS_ID, AtlasStitcher.slot("blocks/tall_grass.png")));
      register(new CrossBlock(SAPLING_ID, AtlasStitcher.slot("blocks/oak_sapling.png")));
      register(new CubeBlock(WOOD_ID,
         AtlasStitcher.slot("blocks/oak_log_top.png"),
         AtlasStitcher.slot("blocks/oak_log_side.png"),
         AtlasStitcher.slot("blocks/oak_log_top.png")));
      register(new CubeBlock(PLANKS_ID, AtlasStitcher.slot("blocks/oak_planks.png")));
   }

   private Blocks() {
   }

   static void register(Block block) {
      BY_ID[block.id & 0xFF] = block;
   }

   public static Block byId(int id) {
      if (id <= 0 || id >= BY_ID.length) {
         return null;
      }
      return BY_ID[id];
   }

   public static boolean isSolid(int id) {
      Block b = byId(id);
      return b != null && b.solid;
   }

   public static boolean blocksLight(int id) {
      return isSolid(id) && id != LEAF_ID;
   }

   public static boolean isTranslucent(int id) {
      return !isSolid(id);
   }

   public static int emission(int id) {
      Block b = byId(id);
      return b == null ? 0 : b.light;
   }

   public static int particleTile(int id) {
      Block b = byId(id);
      return b == null ? 0 : b.particleTile();
   }

   public static int dropId(int id) {
      if (id == GRASS_ID) {
         return DIRT_ID;
      }
      if (id == STONE_ID) {
         return COBBLE_ID;
      }
      if (id == LEAF_ID) {
         return Rng.world().nextInt(4) == 0 ? SAPLING_ID : 0;
      }
      Block b = byId(id);
      return b == null ? 0 : id;
   }

   public static boolean isEmitterId(int id) {
      return id == TORCH_ID || id == LAVA_ID;
   }

   public static int emitterLevel(int id) {
      return id == LAVA_ID ? 15 : 14;
   }

   public static int hardness(int id) {
      if (id == BEDROCK_ID || id == LAVA_ID) {
         return -1;
      }
      if (id == TORCH_ID || id == ROSE_ID || id == DANDELION_ID
         || id == TALL_GRASS_ID || id == SAPLING_ID || id == LAVA_ID) {
         return 5;
      }
      if (id == LEAF_ID) {
         return 10;
      }
      if (id == DIRT_ID || id == SAND_ID || id == GRAVEL_ID) {
         return 15;
      }
      if (id == GRASS_ID) {
         return 18;
      }
      if (id == WOOD_ID || id == PLANKS_ID) {
         return 30;
      }
      if (id == STONE_ID || id == COBBLE_ID) {
         return 60;
      }
      if (id == COAL_ID || id == IRON_ID || id == GOLD_ID || id == DIAMOND_ID || id == LAPIS_ID) {
         return 90;
      }
      return 25;
   }

   public static BlockState stateOf(int id) {
      return BlockState.of(byId(id));
   }

   public static BlockState stateOf(int id, int data) {
      return BlockState.of(byId(id), data);
   }

   public static BlockState stateOf(Block block, int data) {
      return BlockState.of(block, data);
   }
}

