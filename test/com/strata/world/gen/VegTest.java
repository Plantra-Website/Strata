package com.strata.world.gen;

import com.strata.blocks.Block;
import com.strata.blocks.Blocks;
import com.strata.blocks.BlockState;
import com.strata.blocks.BlockView;
import com.strata.blocks.CrossBlock;
import com.strata.blocks.CubeBlock;
import com.strata.blocks.AtlasStitcher;
import com.strata.blocks.MeshBuilder;
import com.strata.core.Config;
import java.awt.image.BufferedImage;

public class VegTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static class FullBright implements BlockView {
      @Override public boolean isSolidTile(int x, int y, int z) { return false; }
      @Override public float getBrightness(int x, int y, int z) { return 1.0F; }
   }

   public static void main(String[] args) throws Exception {
      check(Blocks.byId(Blocks.LEAF_ID) instanceof CubeBlock, "leaf is a cube");
      check(Blocks.byId(Blocks.WOOD_ID) instanceof CubeBlock, "wood is a cube");
      check(Blocks.byId(Blocks.ROSE_ID) instanceof CrossBlock, "rose is a cross");
      check(Blocks.byId(Blocks.DANDELION_ID) instanceof CrossBlock, "dandelion is a cross");
      check(Blocks.byId(Blocks.TALL_GRASS_ID) instanceof CrossBlock, "grass is a cross");
      check(Blocks.byId(Blocks.SAPLING_ID) instanceof CrossBlock, "sapling is a cross");
      check(Blocks.byId(Blocks.LEAF_ID).texture == AtlasStitcher.slot("blocks/tinted/oak_leaves.png"), "leaf tile");
      check(Blocks.byId(Blocks.ROSE_ID).texture == AtlasStitcher.slot("blocks/rose.png"), "rose tile");
      check(Blocks.byId(Blocks.DANDELION_ID).texture == AtlasStitcher.slot("blocks/dandelion.png"), "dandelion tile");
      check(Blocks.byId(Blocks.TALL_GRASS_ID).texture == AtlasStitcher.slot("blocks/tall_grass.png"), "tuft tile");
      check(Blocks.byId(Blocks.SAPLING_ID).texture == AtlasStitcher.slot("blocks/oak_sapling.png"), "sapling tile");
      check(Blocks.isSolid(Blocks.LEAF_ID) && Blocks.isSolid(Blocks.WOOD_ID), "leaf+wood solid");
      check(!Blocks.isSolid(Blocks.ROSE_ID) && !Blocks.isSolid(Blocks.TALL_GRASS_ID), "flower+grass walk-through");
      check(Blocks.isTranslucent(Blocks.ROSE_ID), "flower doesn't occlude");
      check(!Blocks.isTranslucent(Blocks.LEAF_ID), "leaf occludes (opaque classic)");
      System.out.println("registry ok");

      BufferedImage img = AtlasStitcher.stitch();
      int sheet = AtlasStitcher.sheetPx();
      check(img != null && img.getWidth() == sheet && img.getHeight() == sheet
         && (sheet == 2048 || sheet == 4096 || sheet == 8192), "atlas loads picked sheet (" + sheet + "px)");
      int leafT = AtlasStitcher.slot("blocks/tinted/oak_leaves.png");
      int roseT = AtlasStitcher.slot("blocks/rose.png");
      int dandT = AtlasStitcher.slot("blocks/dandelion.png");
      int tuftT = AtlasStitcher.slot("blocks/tall_grass.png");
      int sapT = AtlasStitcher.slot("blocks/oak_sapling.png");
      int woodT = AtlasStitcher.slot("blocks/oak_log_side.png");
      check(tileStats(img, roseT, false, true), "rose tile red+cutout");
      check(tintIs(Blocks.LEAF_ID, 0, 119 / 255.0F, 171 / 255.0F, 47 / 255.0F), "leaf tint plains foliage");
      check(tintIs(Blocks.TALL_GRASS_ID, 0, 145 / 255.0F, 189 / 255.0F, 89 / 255.0F), "tuft tint plains grass");
      check(tintIs(Blocks.ROSE_ID, 0, 1.0F, 1.0F, 1.0F), "rose untinted");
      check(tileStats(img, sapT, false, false), "sapling green+cutout");
      check(tileStats(img, woodT, true, false) || brownPixels(img, woodT) > 100, "wood brown+opaque");
      check(transparentFraction(img, roseT) > 0.5, "rose mostly transparent");
      if (Config.LEAF_BLACKOUT) {
         check(transparentFraction(img, leafT) == 0, "leaves blacked out (blackout experiment on)");
         for (int a : AtlasStitcher.altsFor(leafT)) {
            check(transparentFraction(img, a) == 0, "leaf alt blacked out too (mesh picks alts at random)");
         }
      } else {
         check(transparentFraction(img, leafT) > 0.1, "leaves are cutout (fancy, not opaque)");
      }
      check(transparentFraction(img, tuftT) > 0.25, "tuft partly transparent");
      check(redPixels(img, roseT) > 5, "rose has red bloom");
      check(greenPixels(img, tuftT) > 10, "tuft has green blades");
      check(yellowPixels(img, dandT) > 5, "dandelion is yellow");
      System.out.println("art ok");

      Block flower = Blocks.byId(Blocks.ROSE_ID);
      MeshBuilder b = new MeshBuilder();
      b.init();
      flower.render(b, new FullBright(), 0, 3, 7, -2, BlockState.of(flower));
      check(b.count() == 16, "cross emits 16 verts (got " + b.count() + ")");
      float[] v = b.vertices();
      float[] t = b.texCoords();
      float[] uv = AtlasStitcher.uv(roseT);
      for (int i = 0; i < b.count(); i++) {
         check(v[i * 3] >= 3.0F && v[i * 3] <= 4.0F, "cross x in cell");
         check(v[i * 3 + 1] >= 7.0F && v[i * 3 + 1] <= 8.0F, "cross y in cell");
         check(v[i * 3 + 2] >= -2.0F && v[i * 3 + 2] <= -1.0F, "cross z in cell");
         check(t[i * 2] >= uv[0] && t[i * 2] <= uv[2], "cross u in tile");
         check(t[i * 2 + 1] >= uv[1] && t[i * 2 + 1] <= uv[3], "cross v in tile");
      }
      MeshBuilder b1 = new MeshBuilder();
      b1.init();
      flower.render(b1, new FullBright(), 1, 3, 7, -2, BlockState.of(flower));
      check(b1.count() == 0, "full-bright cross lives on layer 0");
      System.out.println("shape ok");

      TerrainGenerator g = new TerrainGenerator(TerrainGenerator.DEFAULT_SEED, 128);
      int wood = 0, leaf = 0, rose = 0, dand = 0, tuft = 0, sap = 0;
      int birch = 0, spruce = 0;
      int dead = 0, shroom = 0, reed = 0, cactus = 0, snow = 0, lily = 0, ice = 0;
      int lakeWater = 0, lakeLava = 0, pumpkin = 0, clay = 0, vine = 0, sideVine = 0;
      java.util.ArrayList<int[]> pumpkinSpots = new java.util.ArrayList<>();
      java.util.ArrayList<int[]> lavaSpots = new java.util.ArrayList<>();
      for (int x = -120; x <= 120; x++) {
         for (int z = -120; z <= 120; z++) {
            int h = g.heightAt(x, z);
            if (g.blockAt(x, h, z, h) == Blocks.CLAY_ID) {
               clay++;
            }
            for (int y = h + 1; y <= h + 12 && y < 128; y++) {
               int id = g.blockAt(x, y, z, h);
               check(id == g.blockAt(x, y, z, h), "deterministic @" + x + "," + y + "," + z);
               if (Blocks.isLog(id)) {
                  wood++;
                  if (id == Blocks.BIRCH_LOG_ID) {
                     birch++;
                  }
                  if (id == Blocks.SPRUCE_LOG_ID) {
                     spruce++;
                  }
                  check(g.treeTrunkHeight(x, z) > 0, "trunk rooted @" + x + "," + z);
                  int base = g.heightAt(x, z);
                  int th = g.treeTrunkHeight(x, z);
                  check(y > base && y <= base + th, "trunk in range @" + x + "," + y + "," + z);
                  check(id == TerrainGenerator.logForSpecies(g.treeSpecies(x, z)), "log matches species @" + x + "," + z);
               } else if (Blocks.isLeaves(id)) {
                  leaf++;
                  check(y > h, "canopy above surface");
                  check(canopyHosted(g, x, y, z), "canopy hosted @" + x + "," + y + "," + z);
               } else if (Blocks.isFlower(id)) {
                  if (id == Blocks.ROSE_ID) {
                     rose++;
                  } else {
                     dand++;
                  }
                  check(y == h + 1, "flower on surface");
                  check(coverGround(g, x, z, h), "flower on grass @" + x + "," + z);
               } else if (id == Blocks.TALL_GRASS_ID) {
                  tuft++;
                  check(y == h + 1, "tuft on surface");
                  check(coverGround(g, x, z, h), "tuft on grass @" + x + "," + z);
               } else if (Blocks.isSapling(id)) {
                  sap++;
                  check(y == h + 1, "sapling on surface");
                  check(coverGround(g, x, z, h), "sapling on grass @" + x + "," + z);
               } else if (id == Blocks.DEADBUSH_ID) {
                  dead++;
                  check(y == h + 1, "deadbush on surface");
                  int surf = g.blockAt(x, h, z, h);
                  check(surf == Blocks.SAND_ID || surf == Blocks.GRASS_ID, "deadbush on sand/grass @" + x + "," + z);
               } else if (Blocks.isMushroom(id)) {
                  shroom++;
                  check(y == h + 1, "mushroom on surface");
                  int surf = g.blockAt(x, h, z, h);
                  check(surf == Blocks.GRASS_ID || surf == Blocks.DIRT_ID, "mushroom on grass/dirt @" + x + "," + z);
               } else if (id == Blocks.REED_ID) {
                  reed++;
                  check(y > h && y <= h + 4, "reed in stack @" + x + "," + y + "," + z);
                  int surf = g.blockAt(x, h, z, h);
                  int biome = g.genBiomeAt(x, z);
                  check(g.isReedSite(x, z, h, surf, biome), "reed rooted @" + x + "," + z);
                  check(y - h <= TerrainGenerator.reedHeight(x, z, TerrainGenerator.DEFAULT_SEED), "reed height @" + x + "," + y + "," + z);
               } else if (id == Blocks.CACTUS_ID) {
                  cactus++;
                  check(y > h && y <= h + 4, "cactus in stack @" + x + "," + y + "," + z);
                  int surf = g.blockAt(x, h, z, h);
                  int biome = g.genBiomeAt(x, z);
                  check(g.isCactusSite(x, z, h, surf, biome), "cactus rooted @" + x + "," + z);
                  check(y - h <= TerrainGenerator.cactusHeight(x, z, TerrainGenerator.DEFAULT_SEED), "cactus height @" + x + "," + y + "," + z);
               } else if (id == Blocks.PUMPKIN_ID) {
                  pumpkin++;
                  check(y == h + 1, "pumpkin on surface");
                  check(g.blockAt(x, h, z, h) == Blocks.GRASS_ID, "pumpkin on grass @" + x + "," + z);
                  pumpkinSpots.add(new int[]{x, z});
               } else if (id == Blocks.VINE_ID) {
                  vine++;
                  check(y > h, "vine hangs above surface");
                  check(g.vinePart(x, y, z) == Blocks.VINE_ID, "vine hosted @" + x + "," + y + "," + z);
                  if (sideLeaf(x, y, z, g)) {
                     sideVine++;
                  }
               } else if (id == Blocks.SNOW_LAYER_ID) {
                  snow++;
                  check(y == h + 1, "snow on surface");
                  check(GenBiomes.snowy(g.genBiomeAt(x, z)), "snow in cold biome @" + x + "," + z);
               } else if (id == Blocks.LILYPAD_ID) {
                  lily++;
                  check(g.blockAt(x, y - 1, z, h) == Blocks.WATER_ID,
                     "lilypad on water @" + x + "," + y + "," + z);
               } else if (id == Blocks.ICE_ID) {
                  ice++;
                  check(y == TerrainGenerator.SEA_LEVEL - 1
                     || g.isLakeIce(x, y, z, g.columnLakes(x, z)), "ice placed @" + x + "," + y + "," + z);
               } else if (id == Blocks.WATER_ID) {
                  check(y <= TerrainGenerator.SEA_LEVEL || g.lakeLiquidAt(x, y, z) == Blocks.WATER_ID,
                     "water fills to sea, lakes above @" + x + "," + y + "," + z);
                  if (y > TerrainGenerator.SEA_LEVEL) {
                     lakeWater++;
                  }
               } else if (id == Blocks.LAVA_ID) {
                  check(g.lakeLiquidAt(x, y, z) == Blocks.LAVA_ID || y < 10,
                     "lava from lakes or deep floods @" + x + "," + y + "," + z);
                  if (g.lakeLiquidAt(x, y, z) == Blocks.LAVA_ID) {
                     lakeLava++;
                     if (lavaSpots.size() < 40) {
                        lavaSpots.add(new int[]{x, y, z});
                     }
                  }
               } else {
                  check(id == 0, "only vegetation above surface (got " + id + ")");
               }
            }
         }
      }
      System.out.println("wood=" + wood + " leaf=" + leaf + " rose=" + rose + " dandelion=" + dand + " tuft=" + tuft + " sapling=" + sap + " birchLogs=" + birch + " spruceLogs=" + spruce
         + " dead=" + dead + " shroom=" + shroom + " reed=" + reed + " cactus=" + cactus + " snow=" + snow + " lily=" + lily + " ice=" + ice
         + " lakeWater=" + lakeWater + " lakeLava=" + lakeLava
         + " pumpkin=" + pumpkin + " clay=" + clay + " vine=" + vine);
      check(lakeWater > 0, "water lakes exist (1/4 chunks)");
      check(lakeLava > 0, "lava lakes exist (1/8 chunks)");
      int shell = 0;
      int[][] dirs6 = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
      for (int[] p : lavaSpots) {
         for (int[] d : dirs6) {
            if (g.lavaShellAt(p[0] + d[0], p[1] + d[1], p[2] + d[2]) != 0) {
               shell++;
            }
         }
      }
      System.out.println("lavaShell=" + shell);
      check(shell > 0, "lava lakes wear stone shells");
      int springWater = 0, springLava = 0;
      for (int ccx = -4; ccx <= 3; ccx++) {
         for (int ccz = -4; ccz <= 3; ccz++) {
            for (TerrainGenerator.Spring s : g.springsForChunk(ccx, ccz)) {
               int id = g.blockAt(s.x, s.y, s.z, g.heightAt(s.x, s.z));
               check(id == s.fluid, "spring reads fluid @" + s.x + "," + s.y + "," + s.z);
               if (s.fluid == Blocks.WATER_ID) {
                  springWater++;
               } else {
                  springLava++;
                  check(s.fluid == Blocks.LAVA_ID, "lava spring id");
               }
            }
         }
      }
      System.out.println("springs=" + springWater + " water + " + springLava + " lava (8x8)");
      check(springWater > 0, "water seeps exist (50/chunk tries)");
      check(springLava > 0, "lava seeps exist (20/chunk tries)");
      check(pumpkin > 0, "pumpkins exist (rare grass, got " + pumpkin + ")");
      int grouped = 0;
      for (int[] p : pumpkinSpots) {
         for (int[] q : pumpkinSpots) {
            if (p != q && Math.abs(p[0] - q[0]) <= 8 && Math.abs(p[1] - q[1]) <= 8) {
               grouped++;
               break;
            }
         }
      }
      System.out.println("pumpkinGrouped=" + grouped + "/" + pumpkin);
      check(pumpkinSpots.size() == pumpkin, "spots track pumpkins");
      check(grouped * 10 >= pumpkin * 6, "pumpkins grow in groups (grouped " + grouped + "/" + pumpkin + ")");
      check(vine > 0, "vines drape swamp canopies (got " + vine + ")");
      check(sideVine * 2 > vine, "vines hug leaf sides, not under-hangs (" + sideVine + "/" + vine + ")");
      check(wood > 20, "trees exist");
      check(leaf > wood * 5, "canopies dwarf trunks");
      check(rose > 20 && dand > 20, "both flowers exist");
      check(tuft > rose + dand, "grass outnumbers flowers");
      check(sap == 0, "no wild saplings (leaf drops only, got " + sap + ")");
      check(birch > 0, "birch exists (forest 1/5)");
      check(dead > 0, "deadbush exists (desert/swamp)");
      check(shroom > 0, "mushrooms exist (swamp)");
      check(reed > 0, "reeds exist (waterline, got " + reed + ")");
      check(lily > 0, "lilypads exist (swamp water, got " + lily + ")");
      int cactusNear = 0;
      for (int x = 64; x <= 144; x++) {
         for (int z = -336; z <= -264; z++) {
            int h = g.heightAt(x, z);
            for (int y = h + 1; y <= h + 4 && y < 128; y++) {
               if (g.blockAt(x, y, z, h) == Blocks.CACTUS_ID) {
                  cactusNear++;
               }
            }
         }
      }
      System.out.println("cactusNearDesert=" + cactusNear);
      check(cactusNear > 0, "cactus exists (desert window)");
      TerrainGenerator g1 = new TerrainGenerator(1L, 128);
      int snowNear = 0, iceNear = 0, openWater = 0;
      for (int x = 96; x <= 184; x++) {
         for (int z = -144; z <= -56; z++) {
            int h = g1.heightAt(x, z);
            for (int y = h + 1; y <= h + 2 && y < 128; y++) {
               int id = g1.blockAt(x, y, z, h);
               if (id == Blocks.SNOW_LAYER_ID) {
                  snowNear++;
                  int ground = g1.blockAt(x, h, z, h);
                  check(ground != Blocks.ICE_ID && Blocks.isSolid(ground) && !Blocks.isLeaves(ground),
                     "snow on solid @" + x + "," + z + " (got " + ground + ")");
               }
            }
            int wtop = TerrainGenerator.SEA_LEVEL - 1;
            if (wtop >= h && g1.blockAt(x, wtop, z, h) == Blocks.ICE_ID) {
               iceNear++;
            }
            if (wtop >= h && g1.blockAt(x, wtop, z, h) == Blocks.WATER_ID
               && GenBiomes.snowy(g1.genBiomeAt(x, z))) {
               openWater++;
            }
         }
      }
      System.out.println("snowNear=" + snowNear + " iceNear=" + iceNear + " openWater=" + openWater);
      check(snowNear > 0, "snow layers exist (cold window, seed 1)");
      check(iceNear > 0, "ice exists (cold water, seed 1)");
      check(openWater == 0, "frozen seas ice fully (no open shorelines, seed 1)");
      int rockSnow = 0;
      for (int x = 200; x <= 264; x++) {
         for (int z = 0; z <= 64; z++) {
            int h = g1.heightAt(x, z);
            if (h <= TerrainGenerator.SEA_LEVEL) {
               continue;
            }
            int ground = g1.blockAt(x, h, z, h);
            if ((ground == Blocks.SAND_ID || ground == Blocks.STONE_ID || ground == Blocks.CLAY_ID)
               && g1.blockAt(x, h + 1, z, h) == Blocks.SNOW_LAYER_ID) {
               rockSnow++;
            }
         }
      }
      System.out.println("rockSnow=" + rockSnow);
      check(rockSnow > 0, "snow covers cold rock/sand, not just grass");
      int mycel = 0, isleShroom = 0, giantStem = 0, giantCap = 0, shoreClay = 0;
      for (int x = 4040; x <= 4120; x++) {
         for (int z = 2820; z <= 2900; z++) {
            int biome = g.genBiomeAt(x, z);
            if (biome != GenBiomes.MUSHROOM_ISLAND && biome != GenBiomes.MUSHROOM_SHORE) {
               continue;
            }
            int h = g.heightAt(x, z);
            if (g.blockAt(x, h, z, h) == Blocks.MYCELIUM_ID) {
               mycel++;
            }
            if (g.blockAt(x, h, z, h) == Blocks.CLAY_ID) {
               shoreClay++;
            }
            for (int y = h + 1; y <= h + 8 && y < 128; y++) {
               int id = g.blockAt(x, y, z, h);
               check(id == g.blockAt(x, y, z, h), "isle deterministic @" + x + "," + y + "," + z);
               if (id == Blocks.MUSHROOM_BROWN_ID || id == Blocks.MUSHROOM_RED_ID) {
                  isleShroom++;
                  check(y == h + 1, "isle shroom on surface");
               } else if (id == Blocks.MUSHROOM_STEM_ID) {
                  giantStem++;
                  check(g.shroomStemHeight(x, z) > 0, "giant stem rooted @" + x + "," + z);
               } else if (id == Blocks.MUSHROOM_CAP_BROWN_ID || id == Blocks.MUSHROOM_CAP_RED_ID) {
                  giantCap++;
                  check(y > h, "giant cap above surface");
               } else if (id == Blocks.REED_ID) {
                  check(y > h && y <= h + 3, "isle reed in stack");
               } else if (id == Blocks.WATER_ID || id == Blocks.LAVA_ID) {
                  check(y <= TerrainGenerator.SEA_LEVEL || g.lakeLiquidAt(x, y, z) == id,
                     "isle water is sea or lake @" + x + "," + y + "," + z);
               } else {
                  check(id == 0, "isle only shrooms+giants+shore-reeds+lakes above surface (got " + id + ")");
               }
            }
         }
      }
      System.out.println("mycel=" + mycel + " isleShroom=" + isleShroom + " giantStem=" + giantStem + " giantCap=" + giantCap + " shoreClay=" + shoreClay);
      check(mycel > 0, "mycelium paints the isle");
      check(giantStem > 0, "giant mushrooms fruit (stems)");
      check(giantCap > 0, "giant mushrooms fruit (caps)");
      check(shoreClay > 0, "clay discs punch shore sand (got " + shoreClay + ")");
      int spruceNear = 0, spruceLeaf = 0;
      for (int x = 240; x <= 304; x++) {
         for (int z = -32; z <= 32; z++) {
            int h = g.heightAt(x, z);
            for (int y = h + 1; y <= h + 12 && y < 128; y++) {
               int id = g.blockAt(x, y, z, h);
               if (id == Blocks.SPRUCE_LOG_ID) {
                  spruceNear++;
               } else if (id == Blocks.SPRUCE_LEAVES_ID) {
                  spruceLeaf++;
                  check(y > h, "taiga canopy above surface");
                  check(canopyHosted(g, x, y, z), "taiga canopy hosted @" + x + "," + y + "," + z);
               }
            }
         }
      }
      System.out.println("spruceNearTaiga=" + spruceNear + " spruceLeafTaiga=" + spruceLeaf);
      check(spruceNear > 0, "spruce exists (taiga window)");
      check(spruceLeaf > spruceNear, "taiga canopies outfit trunks");
      check(TerrainGenerator.logForSpecies(TerrainGenerator.BIRCH) == Blocks.BIRCH_LOG_ID, "birch log maps");
      check(TerrainGenerator.leavesForSpecies(TerrainGenerator.SPRUCE_TALL) == Blocks.SPRUCE_LEAVES_ID, "spruce leaves map");
      check(g.treeTrunkHeight(0, 0) == g.treeTrunkHeight(0, 0), "trunk memo stable");
      check(g.treeSpecies(0, 0) == g.treeSpecies(0, 0), "species memo stable");

      if (failures > 0) { System.out.println(failures + " FAILURES"); System.exit(1); }
      System.out.println("VEG PASS");
   }

   static boolean coverGround(TerrainGenerator g, int x, int z, int h) {
      return g.blockAt(x, h, z, h) == Blocks.GRASS_ID;
   }

   static boolean sideLeaf(int x, int y, int z, TerrainGenerator g) {
      int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
      for (int[] d : dirs) {
         if (Blocks.isLeaves(g.treePart(x + d[0], y, z + d[1]))) {
            return true;
         }
      }
      return false;
   }

   static boolean canopyHosted(TerrainGenerator g, int x, int y, int z) {
      for (int ox = x - 5; ox <= x + 5; ox++) {
         for (int oz = z - 5; oz <= z + 5; oz++) {
            int th = g.treeTrunkHeight(ox, oz);
            if (th == 0) {
               continue;
            }
            int species = g.treeSpecies(ox, oz);
            boolean cone = species == TerrainGenerator.SPRUCE_SHORT
               || species == TerrainGenerator.SPRUCE_TALL;
            int top = g.heightAt(ox, oz) + th;
            boolean leaf;
            if (cone) {
               leaf = TreeShapes.spruce(x - ox, y - top, z - oz, th,
                  species == TerrainGenerator.SPRUCE_TALL, x, z);
            } else if (species == TerrainGenerator.SWAMP_OAK) {
               leaf = TreeShapes.swamp(x - ox, y - top, z - oz, x, z);
            } else {
               leaf = TreeShapes.round(x - ox, y - top, z - oz, x, z);
            }
            if (leaf) {
               return true;
            }
         }
      }
      return false;
   }

   static int[] rect(int tile) {
      return AtlasStitcher.tileRectPx(tile);
   }

   static boolean tintIs(int id, int layer, float r, float g, float b) {
      MeshBuilder mb = new MeshBuilder();
      mb.init();
      Blocks.byId(id).render(mb, new FullBright(), layer, 4, 60, 4);
      if (mb.count() == 0) {
         return false;
      }
      float[] t = mb.tints();
      for (int i = 0; i < mb.count(); i++) {
         if (Math.abs(t[i * 3] - r) > 1e-4 || Math.abs(t[i * 3 + 1] - g) > 1e-4
            || Math.abs(t[i * 3 + 2] - b) > 1e-4) {
            return false;
         }
      }
      return true;
   }

   static boolean tileStats(BufferedImage img, int tile, boolean opaque, boolean wantRed) {     long r = 0, g = 0, b = 0;
      int n = 0;
      int[] rc = rect(tile);
      for (int x = rc[0]; x < rc[0] + rc[2]; x++) {
         for (int y = rc[1]; y < rc[1] + rc[3]; y++) {
            int px = img.getRGB(x, y);
            int a = (px >>> 24) & 0xFF;
            if (opaque && a != 255) {
               return false;
            }
            if (a < 128) {
               continue;
            }
            r += (px >> 16) & 0xFF;
            g += (px >> 8) & 0xFF;
            b += px & 0xFF;
            n++;
         }
      }
      if (wantRed) {
         return n > 0 && r / n > g / n;
      }
      return n > 0 && g / n > r / n && g / n > b / n;
   }

   static double transparentFraction(BufferedImage img, int tile) {
      int clear = 0;
      int[] rc = rect(tile);
      for (int x = rc[0]; x < rc[0] + rc[2]; x++) {
         for (int y = rc[1]; y < rc[1] + rc[3]; y++) {
            if (((img.getRGB(x, y) >>> 24) & 0xFF) < 128) {
               clear++;
            }
         }
      }
      return clear / (double)(rc[2] * rc[3]);
   }

   static int redPixels(BufferedImage img, int tile) {
      return countPixels(img, tile, 150, -1, -1, 100, -1, -1);
   }

   static int greenPixels(BufferedImage img, int tile) {
      return countPixels(img, tile, -1, -1, 100, -1, -1, 120);
   }

   static int yellowPixels(BufferedImage img, int tile) {
      return countPixels(img, tile, 150, -1, 150, -1, -1, 120);
   }

   static int brownPixels(BufferedImage img, int tile) {
      return countPixels(img, tile, 70, 160, 40, 130, 20, 90);
   }

   static int countPixels(BufferedImage img, int tile, int rLo, int rHi, int gLo, int gHi, int bLo, int bHi) {
      int n = 0;
      int[] rc = rect(tile);
      for (int x = rc[0]; x < rc[0] + rc[2]; x++) {
         for (int y = rc[1]; y < rc[1] + rc[3]; y++) {
            int px = img.getRGB(x, y);
            if (((px >>> 24) & 0xFF) < 128) {
               continue;
            }
            int r = (px >> 16) & 0xFF, g = (px >> 8) & 0xFF, b = px & 0xFF;
            if ((rLo < 0 || r >= rLo) && (rHi < 0 || r <= rHi)
               && (gLo < 0 || g >= gLo) && (gHi < 0 || g <= gHi)
               && (bLo < 0 || b >= bLo) && (bHi < 0 || b <= bHi)) {
               n++;
            }
         }
      }
      return n;
   }
}

