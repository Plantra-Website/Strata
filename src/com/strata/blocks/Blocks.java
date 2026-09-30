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
   public static final int WATER_ID = 22;
   public static final int SANDSTONE_ID = 23;
   public static final int ICE_ID = 24;
   public static final int MYCELIUM_ID = 25;
   public static final int CACTUS_ID = 26;
   public static final int REED_ID = 27;
   public static final int DEADBUSH_ID = 28;
   public static final int MUSHROOM_BROWN_ID = 29;
   public static final int MUSHROOM_RED_ID = 30;
   public static final int CLAY_ID = 31;
   public static final int PUMPKIN_ID = 32;
   public static final int VINE_ID = 33;
   public static final int LILYPAD_ID = 34;
   public static final int SNOW_LAYER_ID = 35;
   public static final int BIRCH_LOG_ID = 36;
   public static final int SPRUCE_LOG_ID = 37;
   public static final int BIRCH_LEAVES_ID = 38;
   public static final int SPRUCE_LEAVES_ID = 39;
   public static final int BIRCH_SAPLING_ID = 40;
   public static final int SPRUCE_SAPLING_ID = 41;
   public static final int MOSSY_COBBLE_ID = 42;
   public static final int REDSTONE_ORE_ID = 43;
   public static final int MOB_SPAWNER_ID = 44;
   public static final int MUSHROOM_STEM_ID = 45;
   public static final int MUSHROOM_CAP_BROWN_ID = 46;
   public static final int MUSHROOM_CAP_RED_ID = 47;
   public static final int SNOW_BLOCK_ID = 48;

   private static final Block[] BY_ID = new Block[256];

   static {
      int dirt = AtlasStitcher.slot("blocks/dirt.png");
      register(new CubeBlock(GRASS_ID,
         AtlasStitcher.slot("blocks/grass_top.png"),
         AtlasStitcher.slot("blocks/grass_side.png"), dirt,
         AtlasStitcher.slot("blocks/grass_side_overlay.png"), 0).hardness(18).drops(DIRT_ID));
      register(new CubeBlock(STONE_ID, AtlasStitcher.slot("blocks/stone.png")).hardness(60).drops(COBBLE_ID));
      register(new CubeBlock(DIRT_ID, dirt).hardness(15));
      register(new CubeBlock(COBBLE_ID, AtlasStitcher.slot("blocks/cobblestone.png")).hardness(60));
      register(new CubeBlock(SAND_ID, AtlasStitcher.slot("blocks/sand.png")).hardness(15));
      register(new CubeBlock(BEDROCK_ID, AtlasStitcher.slot("blocks/bedrock.png")).hardness(-1));
      register(new CubeBlock(COAL_ID, AtlasStitcher.slot("blocks/coal_ore.png")).hardness(90));
      register(new CubeBlock(IRON_ID, AtlasStitcher.slot("blocks/iron_ore.png")).hardness(90));
      register(new CubeBlock(GOLD_ID, AtlasStitcher.slot("blocks/gold_ore.png")).hardness(90));
      register(new CubeBlock(DIAMOND_ID, AtlasStitcher.slot("blocks/diamond_ore.png")).hardness(90));
      register(new CubeBlock(LAPIS_ID, AtlasStitcher.slot("blocks/lapis_ore.png")).hardness(90));
      register(new CubeBlock(GRAVEL_ID, AtlasStitcher.slot("blocks/gravel.png")).hardness(15));
      register(new LavaBlock(LAVA_ID, AtlasStitcher.slot("blocks/lava_still.png"), 15).hardness(5));
      register(new WaterBlock(WATER_ID, AtlasStitcher.slot("blocks/water_still.png"), 0).hardness(5));
      register(new TorchBlock(TORCH_ID, AtlasStitcher.slot("blocks/torch_on.png"), 14).hardness(5));
      register(new CubeBlock(LEAF_ID, AtlasStitcher.slot("blocks/tinted/oak_leaves.png")).hardness(10)
         .drops(() -> Rng.world().nextInt(4) == 0 ? SAPLING_ID : 0));
      register(new FlowerBlock(ROSE_ID, AtlasStitcher.slot("blocks/rose.png")).hardness(5));
      register(new FlowerBlock(DANDELION_ID, AtlasStitcher.slot("blocks/dandelion.png")).hardness(5));
      register(new FlowerBlock(TALL_GRASS_ID, AtlasStitcher.slot("blocks/tall_grass.png"), 0.4F, 0.8F).hardness(5));
      register(new FlowerBlock(SAPLING_ID, AtlasStitcher.slot("blocks/oak_sapling.png")).hardness(5));
      register(new CubeBlock(WOOD_ID,
         AtlasStitcher.slot("blocks/oak_log_top.png"),
         AtlasStitcher.slot("blocks/oak_log_side.png"),
         AtlasStitcher.slot("blocks/oak_log_top.png")).hardness(30));
      register(new CubeBlock(PLANKS_ID, AtlasStitcher.slot("blocks/oak_planks.png")).hardness(30));
      register(new CubeBlock(SANDSTONE_ID,
         AtlasStitcher.slot("blocks/sandstone_top.png"),
         AtlasStitcher.slot("blocks/sandstone_side.png"),
         AtlasStitcher.slot("blocks/sandstone_bottom.png")).hardness(60));
      register(new IceBlock(ICE_ID, AtlasStitcher.slot("blocks/ice.png")).hardness(15).drops(0));
      register(new CubeBlock(MYCELIUM_ID,
         AtlasStitcher.slot("blocks/mycelium_top.png"),
         AtlasStitcher.slot("blocks/mycelium_side.png"), dirt).hardness(18).drops(DIRT_ID));
      register(new CactusBlock(CACTUS_ID,
         AtlasStitcher.slot("blocks/cactus_top.png"),
         AtlasStitcher.slot("blocks/cactus_side.png"),
         AtlasStitcher.slot("blocks/cactus_bottom.png")).hardness(15));
      register(new ReedBlock(REED_ID, AtlasStitcher.slot("blocks/reeds.png"), 0.375F, 1.0F).hardness(5));
      register(new DeadbushBlock(DEADBUSH_ID, AtlasStitcher.slot("blocks/deadbush.png"), 0.4F, 0.8F).hardness(5).drops(0));
      register(new MushroomBlock(MUSHROOM_BROWN_ID, AtlasStitcher.slot("blocks/mushroom_brown.png"), 0.2F, 0.4F).hardness(5));
      register(new MushroomBlock(MUSHROOM_RED_ID, AtlasStitcher.slot("blocks/mushroom_red.png"), 0.2F, 0.4F).hardness(5));
      register(new CubeBlock(CLAY_ID, AtlasStitcher.slot("blocks/clay.png")).hardness(15));
      register(new CubeBlock(PUMPKIN_ID,
         AtlasStitcher.slot("blocks/pumpkin_top.png"),
         AtlasStitcher.slot("blocks/pumpkin_side.png"),
         AtlasStitcher.slot("blocks/pumpkin_side.png")).hardness(30));
      register(new VineBlock(VINE_ID, AtlasStitcher.slot("blocks/vine.png")).hardness(5).drops(0));
      register(new LilyBlock(LILYPAD_ID, AtlasStitcher.slot("blocks/waterlily.png")).hardness(5));
      register(new SnowBlock(SNOW_LAYER_ID, AtlasStitcher.slot("blocks/snow.png")).hardness(5).drops(0));
      register(new CubeBlock(BIRCH_LOG_ID,
         AtlasStitcher.slot("blocks/birch_log_top.png"),
         AtlasStitcher.slot("blocks/birch_log_side.png"),
         AtlasStitcher.slot("blocks/birch_log_top.png")).hardness(30));
      register(new CubeBlock(SPRUCE_LOG_ID,
         AtlasStitcher.slot("blocks/spruce_log_top.png"),
         AtlasStitcher.slot("blocks/spruce_log_side.png"),
         AtlasStitcher.slot("blocks/spruce_log_top.png")).hardness(30));
      register(new CubeBlock(BIRCH_LEAVES_ID, AtlasStitcher.slot("blocks/birch_leaves.png")).hardness(10)
         .drops(() -> Rng.world().nextInt(4) == 0 ? BIRCH_SAPLING_ID : 0));
      register(new CubeBlock(SPRUCE_LEAVES_ID, AtlasStitcher.slot("blocks/spruce_leaves.png")).hardness(10)
         .drops(() -> Rng.world().nextInt(4) == 0 ? SPRUCE_SAPLING_ID : 0));
      register(new FlowerBlock(BIRCH_SAPLING_ID, AtlasStitcher.slot("blocks/birch_sapling.png")).hardness(5));
      register(new FlowerBlock(SPRUCE_SAPLING_ID, AtlasStitcher.slot("blocks/spruce_sapling.png")).hardness(5));
      register(new CubeBlock(MOSSY_COBBLE_ID, AtlasStitcher.slot("blocks/mossy_cobblestone.png")).hardness(60));
      register(new CubeBlock(REDSTONE_ORE_ID, AtlasStitcher.slot("blocks/redstone_ore.png")).hardness(90));
      register(new CubeBlock(MOB_SPAWNER_ID, AtlasStitcher.slot("blocks/mob_spawner.png")).hardness(60).drops(0));
      register(new CubeBlock(MUSHROOM_STEM_ID,
         AtlasStitcher.slot("blocks/mushroom_block_inside.png"),
         AtlasStitcher.slot("blocks/mushroom_block_skin_stem.png"),
         AtlasStitcher.slot("blocks/mushroom_block_inside.png")).hardness(30));
      register(new CubeBlock(MUSHROOM_CAP_BROWN_ID,
         AtlasStitcher.slot("blocks/mushroom_block_skin_brown.png"),
         AtlasStitcher.slot("blocks/mushroom_block_skin_brown.png"),
         AtlasStitcher.slot("blocks/mushroom_block_inside.png")).hardness(10));
      register(new CubeBlock(MUSHROOM_CAP_RED_ID,
         AtlasStitcher.slot("blocks/mushroom_block_skin_red.png"),
         AtlasStitcher.slot("blocks/mushroom_block_skin_red.png"),
         AtlasStitcher.slot("blocks/mushroom_block_inside.png")).hardness(10));
      register(new CubeBlock(SNOW_BLOCK_ID, AtlasStitcher.slot("blocks/snow.png")).hardness(15).drops(SNOW_LAYER_ID));
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

   public static boolean isFluid(int id) {
      return BlockTags.fluid(id);
   }

   public static boolean isLeaves(int id) {
      return BlockTags.leaves(id);
   }

   public static boolean isLog(int id) {
      return BlockTags.logs(id);
   }

   public static boolean isSapling(int id) {
      return BlockTags.saplings(id);
   }

   public static boolean isFlower(int id) {
      return BlockTags.flowers(id);
   }

   public static boolean isMushroom(int id) {
      return BlockTags.mushrooms(id);
   }

   public static boolean isOre(int id) {
      return BlockTags.ores(id);
   }

   public static boolean isFalling(int id) {
      return BlockTags.falling(id);
   }

   public static boolean blocksLight(int id) {
      return isSolid(id) && !isLeaves(id) && id != CACTUS_ID && id != ICE_ID;
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
      Block b = byId(id);
      return b == null ? 0 : b.drop.getAsInt();
   }

   public static boolean isEmitterId(int id) {
      return id == TORCH_ID || id == LAVA_ID;
   }

   public static int emitterLevel(int id) {
      return id == LAVA_ID ? 15 : 14;
   }

   public static int hardness(int id) {
      Block b = byId(id);
      return b == null ? 25 : b.hardness;
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

