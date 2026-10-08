package com.strata.blocks;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

public class TexturePackTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static BufferedImage solid(int rgb) {
      return solid(rgb, 16);
   }

   static BufferedImage solid(int rgb, int size) {
      BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
      for (int x = 0; x < size; x++) {
         for (int y = 0; y < size; y++) {
            img.setRGB(x, y, rgb);
         }
      }
      return img;
   }

   static void add(ZipOutputStream z, String name, BufferedImage img) throws Exception {
      z.putNextEntry(new ZipEntry(name));
      ImageIO.write(img, "png", z);
      z.closeEntry();
   }

   public static void main(String[] args) throws Exception {
      File zip = new File("packtest.zip");
      ZipOutputStream z = new ZipOutputStream(new FileOutputStream(zip));
      add(z, "textures/blocks/sand.png", solid(0xFFFF0000));
      add(z, "textures/blocks/tiny.png", solid(0xFF00FF00)); 
      add(z, "textures/blocks/stone.png", solid(0xFF808080, 8)); 
      add(z, "textures/blocks/dirt.png", solid(0xFF0000FF, 64)); 
      z.putNextEntry(new ZipEntry("textures/items/stick.png")); 
      ImageIO.write(solid(0xFF0000FF), "png", z);
      z.closeEntry();
      z.close();

      Map<String, BufferedImage> pack = TexturePack.loadPack(zip);
      check(pack.size() == 4, "four usable entries (got " + pack.size() + ")");
      check(pack.containsKey("blocks/sand.png"), "sand override keyed");
      check((pack.get("blocks/sand.png").getRGB(0, 0) & 0xFFFFFF) == 0xFF0000, "sand pixels red");
      check(pack.get("blocks/stone.png").getWidth() == 8, "8px stone rides whole");
      check(pack.get("blocks/dirt.png").getWidth() == 64, "64px dirt rides whole");
      check((pack.get("blocks/dirt.png").getRGB(0, 0) & 0xFFFFFF) == 0x0000FF, "big art pixels intact");

      check(TexturePack.loadPack(new File("no-such-pack.zip")).isEmpty(), "missing zip falls back");

      AtlasStitcher.setPack(pack);
      BufferedImage atlas = AtlasStitcher.stitch();
      int slot = AtlasStitcher.slot("blocks/sand.png");
      int[] rect = AtlasStitcher.tileRectPx(slot);
      check((atlas.getRGB(rect[0], rect[1]) & 0xFFFFFF) == 0xFF0000, "atlas shows override");
      AtlasStitcher.setPack(null);
      BufferedImage plain = AtlasStitcher.stitch();
      int[] rect2 = AtlasStitcher.tileRectPx(slot);
      check((plain.getRGB(rect2[0], rect2[1]) & 0xFFFFFF) != 0xFF0000, "clear restores built-in");

      java.util.Map<String, BufferedImage> altPack = new java.util.HashMap<>();
      altPack.put("blocks/bedrock1.png", solid(0xFF00FF00, 32));
      altPack.put("blocks/bedrock2.png", solid(0xFF0000FF, 32));
      AtlasStitcher.setPack(altPack);
      AtlasStitcher.stitch();
      int rockSlot = AtlasStitcher.slot("blocks/bedrock.png");
      int[] rockAlts = AtlasStitcher.altsFor(rockSlot);
      check(rockAlts.length == 2, "two alts discovered (got " + rockAlts.length + ")");
      for (int a : rockAlts) {
         check(a >= AtlasStitcher.TILES.length, "alt index past canonical (" + a + ")");
         int[] r = AtlasStitcher.tileRectPx(a);
         check(r[2] == 32 && r[3] == 32, "alt packed whole");
      }
      AtlasStitcher.setPack(null);
      AtlasStitcher.stitch();
      check(AtlasStitcher.altsFor(rockSlot).length == 0, "clear drops injected alts");

       check(AtlasStitcher.kindFor("blocks/grass_top12.png") == AtlasStitcher.kindFor("blocks/grass_top.png"),
          "alt inherits base kind");
       check(AtlasStitcher.kindFor("blocks/destroy_stage_10.png") == BiomeTints.NONE, "digit tiles unaffected");
       check(AtlasStitcher.kindFor("blocks/stone.png") == BiomeTints.NONE, "untinted stays none");
      zip.delete();

      if (failures == 0) System.out.println("PACK PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

