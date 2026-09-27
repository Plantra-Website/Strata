package com.strata.blocks;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class AtlasTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
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
         int[] tint = AtlasStitcher.TINTS.get(AtlasStitcher.TILES[t]);
         boolean same = true;
         int[] rect = AtlasStitcher.tileRectPx(t);
         check(rect[2] == fw && rect[3] == fh, "tile " + t + " packed one frame");
         for (int x = 0; x < fw && same; x++) {
            for (int y = 0; y < fh && same; y++) {
               int px = src.getRGB(x, y);
               int want = px;
               if (tint != null) {
                  int a = (px >>> 24) & 0xFF;
                  int v = px & 0xFF;
                  want = (a << 24) | (v * tint[0] / 255 << 16) | (v * tint[1] / 255 << 8) | (v * tint[2] / 255);
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

      for (String name : AtlasStitcher.TINTS.keySet()) {
         boolean found = false;
         for (String tile : AtlasStitcher.TILES) {
            if (tile.equals(name)) {
               found = true;
            }
         }
         check(found, "tint names a stitched tile: " + name);
      }

      if (failures > 0) { System.out.println(failures + " FAILURES"); System.exit(1); }
      System.out.println("ATLAS PASS");
   }
}

