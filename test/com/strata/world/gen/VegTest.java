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
      check(tileStats(img, leafT, false, false), "leaf tile green+dominant");
      check(tileStats(img, roseT, false, true), "rose tile red+cutout");
      check(tileStats(img, tuftT, false, false), "tuft tile green+cutout");
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

      TerrainGenerator g = new TerrainGenerator(TerrainGenerator.DEFAULT_SEED, 64);
      int wood = 0, leaf = 0, rose = 0, dand = 0, tuft = 0, sap = 0;
      for (int x = -120; x <= 120; x++) {
         for (int z = -120; z <= 120; z++) {
            int h = g.heightAt(x, z);
            for (int y = h + 1; y <= h + 8 && y < 64; y++) {
               int id = g.blockAt(x, y, z, h);
               check(id == g.blockAt(x, y, z, h), "deterministic @" + x + "," + y + "," + z);
               if (id == Blocks.WOOD_ID) {
                  wood++;
                  check(g.treeTrunkHeight(x, z) > 0, "trunk rooted @" + x + "," + z);
                  int base = g.heightAt(x, z);
                  int th = g.treeTrunkHeight(x, z);
                  check(y > base && y <= base + th, "trunk in range @" + x + "," + y + "," + z);
               } else if (id == Blocks.LEAF_ID) {
                  leaf++;
                  check(y > h, "canopy above surface");
                  check(canopyHosted(g, x, y, z), "canopy hosted @" + x + "," + y + "," + z);
               } else if (id == Blocks.ROSE_ID) {
                  rose++;
                  check(y == h + 1, "rose on surface");
                  check(coverGround(g, x, z, h), "rose on grass @" + x + "," + z);
               } else if (id == Blocks.DANDELION_ID) {
                  dand++;
                  check(y == h + 1, "dandelion on surface");
                  check(coverGround(g, x, z, h), "dandelion on grass @" + x + "," + z);
               } else if (id == Blocks.TALL_GRASS_ID) {
                  tuft++;
                  check(y == h + 1, "tuft on surface");
                  check(coverGround(g, x, z, h), "tuft on grass @" + x + "," + z);
               } else if (id == Blocks.SAPLING_ID) {
                  sap++;
                  check(y == h + 1, "sapling on surface");
                  check(coverGround(g, x, z, h), "sapling on grass @" + x + "," + z);
               } else {
                  check(id == 0, "only vegetation above surface (got " + id + ")");
               }
            }
         }
      }
      System.out.println("wood=" + wood + " leaf=" + leaf + " rose=" + rose + " dandelion=" + dand + " tuft=" + tuft + " sapling=" + sap);
      check(wood > 20, "trees exist");
      check(leaf > wood * 5, "canopies dwarf trunks");
      check(rose > 20 && dand > 20, "both flowers exist");
      check(tuft > rose + dand, "grass outnumbers flowers");
      check(sap > 0, "saplings exist");
      check(g.treeTrunkHeight(0, 0) == g.treeTrunkHeight(0, 0), "trunk memo stable");

      if (failures > 0) { System.out.println(failures + " FAILURES"); System.exit(1); }
      System.out.println("VEG PASS");
   }

   static boolean coverGround(TerrainGenerator g, int x, int z, int h) {
      return h > TerrainGenerator.SEA_LEVEL + 1 && g.blockAt(x, h, z, h) == Blocks.GRASS_ID;
   }

   static boolean canopyHosted(TerrainGenerator g, int x, int y, int z) {
      for (int ox = x - 2; ox <= x + 2; ox++) {
         for (int oz = z - 2; oz <= z + 2; oz++) {
            int th = g.treeTrunkHeight(ox, oz);
            if (th == 0) {
               continue;
            }
            int base = g.heightAt(ox, oz);
            int top = base + th;
            int adx = Math.abs(x - ox), adz = Math.abs(z - oz);
            if (y == top - 2 || y == top - 1) {
               if (adx <= 2 && adz <= 2 && !(adx == 2 && adz == 2)) {
                  return true;
               }
            } else if (y == top) {
               if (adx <= 1 && adz <= 1) {
                  return true;
               }
            } else if (y == top + 1) {
               if (adx + adz <= 1) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   static int[] rect(int tile) {
      return AtlasStitcher.tileRectPx(tile);
   }

   static boolean tileStats(BufferedImage img, int tile, boolean opaque, boolean wantRed) {
      long r = 0, g = 0, b = 0;
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

