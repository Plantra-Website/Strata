package com.strata.blocks;
import com.strata.core.Config;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class AtlasTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static boolean blackedOut(String name) {
      if (!Config.LEAF_BLACKOUT) {
         return false;
      }
      int leafBase = Blocks.byId(Blocks.LEAF_ID).texture;
      int s = AtlasStitcher.slot(name);
      if (s == leafBase) {
         return true;
      }
      for (int a : AtlasStitcher.altsFor(leafBase)) {
         if (s == a) {
            return true;
         }
      }
      return false;
   }

   public static void main(String[] args) throws Exception {
      AtlasStitcher.validate();
      BufferedImage atlas = AtlasStitcher.stitch();
      int sheet = AtlasStitcher.sheetPx();
      check(sheet == 2048 || sheet == 4096 || sheet == 8192, "sheet auto-grows (" + sheet + "px)");
      check(atlas.getWidth() == sheet && atlas.getHeight() == sheet, "atlas matches picked sheet");
      check(AtlasStitcher.TILES.length > 16, "more than one row (" + AtlasStitcher.TILES.length + " tiles)");
      check(AtlasStitcher.TILES.length <= AtlasStitcher.MAX_TILES, "atlas fits the sheet");

      for (int t = 0; t < AtlasStitcher.TILES.length; t++) {
         float[] r = AtlasStitcher.tileRect(t);
         float[] uv = AtlasStitcher.uv(t);
         check(uv[0] == r[0] && uv[1] == r[1], "tile " + t + " uv origin on rect");
         check(uv[2] == r[0] + r[2] && uv[3] == r[1] + r[3], "tile " + t + " uv spans the full footprint (no crop)");
         check(uv[2] > uv[0] && uv[3] > uv[1], "tile " + t + " has area");
         int[] px = AtlasStitcher.tileRectPx(t);
         check(px[0] >= 0 && px[1] >= 0 && px[0] + px[2] <= sheet && px[1] + px[3] <= sheet,
            "tile " + t + " inside the sheet");
         for (int o = t + 1; o < AtlasStitcher.TILES.length; o++) {
            int[] q = AtlasStitcher.tileRectPx(o);
            boolean overlap = px[0] < q[0] + q[2] && q[0] < px[0] + px[2]
               && px[1] < q[1] + q[3] && q[1] < px[1] + px[3];
            check(!overlap, "tiles " + t + " and " + o + " disjoint");
         }
      }

      for (int t = 0; t < AtlasStitcher.TILES.length; t++) {
         InputStream in = AtlasTest.class.getResourceAsStream("/textures/" + AtlasStitcher.TILES[t]);
         check(in != null, "tile file present: " + AtlasStitcher.TILES[t]);
         BufferedImage src = ImageIO.read(in);
         in.close();
         check(src.getWidth() >= 8 && src.getHeight() >= 8, "tile sane: " + AtlasStitcher.TILES[t]);
         int fw = src.getWidth();
         int fh = src.getWidth() <= src.getHeight() && src.getHeight() % src.getWidth() == 0
            ? src.getWidth() : src.getHeight();
         boolean same = true;
         int[] rect = AtlasStitcher.tileRectPx(t);
         check(rect[2] == fw && rect[3] == fh, "tile " + t + " packed one frame");
         for (int x = 0; x < fw && same; x++) {
             for (int y = 0; y < fh && same; y++) {
                int px = src.getRGB(x, y);
                int want = px;
               if (blackedOut(AtlasStitcher.TILES[t]) && ((px >>> 24) & 0xFF) < 128) {
                  want = 0xFF000000;
               }
               if (atlas.getRGB(rect[0] + x, rect[1] + y) != want) {
                  same = false;
               }
            }
         }
         check(same, "tile " + t + " composed at its index (" + AtlasStitcher.TILES[t] + ")");
      }

      check(AtlasStitcher.animFrames(AtlasStitcher.slot("blocks/lava_still.png")) == 16, "lava animates");
      check(AtlasStitcher.animFrames(AtlasStitcher.slot("blocks/torch_on.png")) == 1, "torch static");
      check(AtlasStitcher.animFrames(AtlasStitcher.slot("blocks/dirt.png")) == 1, "dirt static");

      for (int id = 1; id < 256; id++) {
         Block b = Blocks.byId(id);
         if (b != null) {
            check(b.texture >= 0 && b.texture < AtlasStitcher.TILES.length,
               "block id " + id + " texture in range (got " + b.texture + ")");
         }
      }

      check(AtlasStitcher.slot("blocks/grass_top.png") == 0, "grass top slot");
      check(AtlasStitcher.slot("blocks/grass_side_overlay.png") == 32, "overlay slot");
      check(AtlasStitcher.slot("blocks/tinted/oak_leaves.png") == 15, "oak leaves slot");
      check(AtlasStitcher.slot("blocks/destroy_stage_0.png") == 22, "crack slot");
      check(AtlasStitcher.slot("blocks/destroy_stage_9.png") == 31, "last crack slot");
      boolean threw = false;
      try {
         AtlasStitcher.slot("blocks/mithril.png");
      } catch (RuntimeException e) {
         threw = true;
      }
      check(threw, "unknown texture name throws");

       check(AtlasStitcher.tintKind(AtlasStitcher.slot("blocks/grass_top.png")) == BiomeTints.GRASS, "grass top kind");
       check(AtlasStitcher.tintKind(AtlasStitcher.slot("blocks/grass_side_overlay.png")) == BiomeTints.GRASS, "overlay kind");
       check(AtlasStitcher.tintKind(AtlasStitcher.slot("blocks/tall_grass.png")) == BiomeTints.GRASS, "tuft kind");
       check(AtlasStitcher.tintKind(AtlasStitcher.slot("blocks/reeds.png")) == BiomeTints.GRASS, "reeds kind");
       check(AtlasStitcher.tintKind(AtlasStitcher.slot("blocks/tinted/oak_leaves.png")) == BiomeTints.FOLIAGE, "oak kind");
       check(AtlasStitcher.tintKind(AtlasStitcher.slot("blocks/vine.png")) == BiomeTints.FOLIAGE, "vine kind");
       check(AtlasStitcher.tintKind(AtlasStitcher.slot("blocks/birch_leaves.png")) == BiomeTints.BIRCH, "birch kind");
       check(AtlasStitcher.tintKind(AtlasStitcher.slot("blocks/spruce_leaves.png")) == BiomeTints.PINE, "pine kind");
       check(AtlasStitcher.tintKind(AtlasStitcher.slot("blocks/stone.png")) == BiomeTints.NONE, "stone kind");
       check(AtlasStitcher.tintKind(AtlasStitcher.slot("blocks/grass_side.png")) == BiomeTints.NONE, "dirt side kind");
       check(AtlasStitcher.tintKind(AtlasStitcher.slot("blocks/rose.png")) == BiomeTints.NONE, "flower kind");

       float[] swamp = BiomeTints.colorFor(BiomeTints.GRASS, 6);
       check(Math.abs(swamp[0] - 106 / 255.0F) < 1e-4 && Math.abs(swamp[1] - 112 / 255.0F) < 1e-4
          && Math.abs(swamp[2] - 57 / 255.0F) < 1e-4, "swamp grass murk");
       float[] birch = BiomeTints.colorFor(BiomeTints.BIRCH, 4);
       check(Math.abs(birch[0] - 128 / 255.0F) < 1e-4 && Math.abs(birch[1] - 167 / 255.0F) < 1e-4
          && Math.abs(birch[2] - 85 / 255.0F) < 1e-4, "birch fixed (biome-independent)");
       float[] pine = BiomeTints.colorFor(BiomeTints.PINE, 12);
       check(Math.abs(pine[0] - 97 / 255.0F) < 1e-4, "pine fixed");
       float[] none = BiomeTints.colorFor(BiomeTints.NONE, 6);
       check(none[0] == 1.0F && none[1] == 1.0F && none[2] == 1.0F, "none is white");
       float[] plains = BiomeTints.colorFor(BiomeTints.GRASS, 1);
       float[] clamped = BiomeTints.colorFor(BiomeTints.GRASS, 99);
       check(clamped == plains, "bad biome falls back to plains");
       float[] oak = BiomeTints.colorFor(BiomeTints.FOLIAGE, 1);
       check(Math.abs(oak[0] - 119 / 255.0F) < 1e-4, "plains foliage matches old bake");

      {
         final java.util.concurrent.atomic.AtomicReference<Throwable> boom =
            new java.util.concurrent.atomic.AtomicReference<>();
         Thread[] racers = new Thread[8];
         for (int i = 0; i < racers.length; i++) {
            final int k = i;
            racers[i] = new Thread(() -> {
               try {
                  for (int j = 0; j < 6; j++) {
                     if ((k + j) % 2 == 0) {
                        AtlasStitcher.stitch();
                     } else {
                        AtlasStitcher.altsFor(15);
                        AtlasStitcher.uv(0);
                     }
                  }
               } catch (Throwable t) {
                  boom.compareAndSet(null, t);
               }
            });
            racers[i].start();
         }
         for (Thread racer : racers) {
            racer.join();
         }
         check(boom.get() == null, "concurrent stitch clean ("
            + (boom.get() == null ? "ok" : boom.get().toString()) + ")");
         check(AtlasStitcher.slot("blocks/grass_top.png") == 0, "slots stable after race");
         float[] uv = AtlasStitcher.uv(0);
         check(uv[2] > uv[0] && uv[3] > uv[1], "rects sane after race");
      }

      if (failures > 0) { System.out.println(failures + " FAILURES"); System.exit(1); }
      System.out.println("ATLAS PASS");
   }
}

