package com.strata.server;

import com.strata.blocks.AtlasStitcher;
import com.strata.blocks.Block;
import com.strata.blocks.Blocks;
import com.strata.blocks.CubeBlock;
import com.strata.net.LocalConnection;
import java.awt.image.BufferedImage;
import java.io.File;

public class ImportTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static BufferedImage twoTone() {
      BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
      for (int x = 0; x < 8; x++) {
         for (int y = 0; y < 8; y++) {
            boolean edge = x == 0 || y == 0 || x == 7 || y == 7;
            img.setRGB(x, y, edge ? 0xFF000000 : 0xFFFFFFFF);
         }
      }
      img.setRGB(0, 0, 0x00000000);
      return img;
   }

   static BufferedImage gradient() {
      BufferedImage img = new BufferedImage(WorldImporter.RAMP.length, 4, BufferedImage.TYPE_INT_ARGB);
      for (int i = 0; i < WorldImporter.RAMP.length; i++) {
         int v = (int)Math.round(WorldImporter.RAMP_LUMA[i]);
         int rgb = 0xFF000000 | (v << 16) | (v << 8) | v;
         for (int y = 0; y < 4; y++) {
            img.setRGB(i, y, rgb);
         }
      }
      img.setRGB(0, 3, 0x00000000);
      return img;
   }

   static int tierOf(int id) {
      for (int i = 0; i < WorldImporter.RAMP.length; i++) {
         if (WorldImporter.RAMP[i] == id) return i;
      }
      return -1;
   }

   static double texLuma(BufferedImage tex) {
      double tot = 0;
      int n = 0;
      for (int y = 0; y < tex.getHeight(); y++) {
         for (int x = 0; x < tex.getWidth(); x++) {
            int px = tex.getRGB(x, y);
            if (((px >>> 24) & 0xFF) < 128) continue;
            int r = (px >> 16) & 0xFF;
            int g = (px >> 8) & 0xFF;
            int b = px & 0xFF;
            tot += 0.299 * r + 0.587 * g + 0.114 * b;
            n++;
         }
      }
      return n == 0 ? -1 : tot / n;
   }

   public static void main(String[] args) throws Exception {
      check(WorldImporter.RAMP.length == WorldImporter.RAMP_LUMA.length,
         "RAMP and RAMP_LUMA same length");
      double prevL = -1;
      for (int i = 0; i < WorldImporter.RAMP.length; i++) {
         Block blk = Blocks.byId(WorldImporter.RAMP[i]);
         check(blk != null, "ramp block resolves: id " + WorldImporter.RAMP[i]);
         if (blk == null) continue;
         int slot = blk instanceof CubeBlock ? ((CubeBlock)blk).topTexture : blk.texture;
         String name = AtlasStitcher.TILES[slot];
         java.io.InputStream in = ImportTest.class.getResourceAsStream("/textures/" + name);
         check(in != null, "top texture on classpath: " + name);
         if (in == null) continue;
         BufferedImage tex = javax.imageio.ImageIO.read(in);
         check(tex != null, "top texture decodes: " + name);
         if (tex == null) continue;
         double lum = texLuma(tex);
         check(lum > prevL, "ramp luma rises at tier " + i + " (" + name + " = " + lum
            + ", prev " + prevL + ")");
         check(Math.abs(lum - WorldImporter.RAMP_LUMA[i]) <= 8.0,
            "RAMP_LUMA[" + i + "] matches texture (" + WorldImporter.RAMP_LUMA[i]
            + " vs measured " + lum + ") — reskinned? update the ladder");
         prevL = lum;
      }

      check(WorldImporter.tierFor(0, 0, 0) == 0, "black -> darkest tier");
      check(WorldImporter.tierFor(255, 255, 255) == WorldImporter.RAMP.length - 1,
         "white -> lightest tier");
      for (int i = 0; i < WorldImporter.RAMP_LUMA.length; i++) {
         int v = (int)Math.round(WorldImporter.RAMP_LUMA[i]);
         check(WorldImporter.tierFor(v, v, v) == i,
            "gray " + v + " -> tier " + i + " (got " + WorldImporter.tierFor(v, v, v) + ")");
      }
      int prevT = 0;
      boolean rising = true;
      for (int v = 0; v <= 255; v++) {
         int t = WorldImporter.tierFor(v, v, v);
         if (t < prevT) rising = false;
         prevT = t;
      }
      check(rising, "gray sweep is monotone (never steps down)");
      check(WorldImporter.tierFor(0, 255, 0) > WorldImporter.tierFor(255, 0, 0),
         "green outranks red (0.299/0.587/0.114 weights)");

      BufferedImage plain = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
      for (int x = 0; x < 16; x++) {
         for (int y = 0; y < 16; y++) {
            plain.setRGB(x, y, ((x * 16) << 16) | ((y * 16) << 8) | 128);
         }
      }
      File jpg = new File("strip-roundtrip.jpg");
      javax.imageio.ImageIO.write(plain, "jpg", jpg);
      byte[] raw = java.nio.file.Files.readAllBytes(jpg.toPath());
      byte[] clean = WorldImporter.stripSegments(raw);
      check(clean != null && clean.length > 0, "strip keeps good files");
      BufferedImage back = WorldImporter.readImage(jpg);
      check(back != null && back.getWidth() == 16 && back.getHeight() == 16, "stripped JPEG decodes");
      jpg.delete();
      check(WorldImporter.stripSegments(new byte[]{1, 2, 3}) == null, "non-JPEG rejected");

      File dir = new File("importworld");
      com.strata.server.ItemTest.wipeDir(dir);
      GameServer s = new GameServer(new LocalConnection(), dir, 555L, true);
      check(s.level().getTile(200, 40, -200) == 0, "void is air");
      WorldImporter.importImage(s, twoTone());
      s.save();

      int dark = 0, light = 0, offLevel = 0;
      int minX = 999, maxX = -999, minZ = 999, maxZ = -999;
      int darkest = WorldImporter.RAMP[0];
      int lightest = WorldImporter.RAMP[WorldImporter.RAMP.length - 1];
      for (int x = -100; x < 100; x++) {
         for (int y = 30; y < 60; y++) {
            for (int z = -100; z < 100; z++) {
               int t = s.level().getTile(x, y, z);
               if (t == darkest || t == lightest) {
                  if (y != WorldImporter.ART_Y) {
                     offLevel++;
                  } else {
                     if (t == darkest) {
                        dark++;
                     } else {
                        light++;
                     }
                     minX = Math.min(minX, x);
                     maxX = Math.max(maxX, x);
                     minZ = Math.min(minZ, z);
                     maxZ = Math.max(maxZ, z);
                  }
               }
            }
         }
      }
      check(offLevel == 0, "sheet is flat (nothing off level)");
      check(dark == 27, "27 darkest cells (got " + dark + ")");
      check(light == 36, "36 lightest cells (got " + light + ")");
      check(maxX - minX == 7 && maxZ - minZ == 7, "8x8 footprint");
      check(s.level().getBrightness(minX, WorldImporter.ART_Y + 1, minZ) == 1.0F, "bright above art");
      float[] spawn = s.spawnPoint();
      check(spawn != null, "spawn set");
      int tx = (int)Math.floor(spawn[0]);
      int tz = (int)Math.floor(spawn[2]);
      check(s.level().getTile(tx, 40, tz) == Blocks.STONE_ID, "tower stalk");
      check(s.level().getTile(tx, 51, tz) == Blocks.STONE_ID, "tower cap");
      check(Math.abs(s.player().x - spawn[0]) < 0.01, "player on spawn x");
      check(Math.abs(s.player().y - (spawn[1] + 1.62F)) < 0.01, "player on spawn y");
      check(Math.abs(s.player().bb.y0 - spawn[1]) < 0.01, "feet on spawn platform");
      check(s.player().yRot == 0.0F && s.player().xRot == 30.0F, "player faces art pitched down");

      GameServer re = new GameServer(new LocalConnection(), dir, null);
      check(re.level().getTile(200, 40, -200) == 0, "still void after reboot");
      int dark2 = 0;
      for (int x = -100; x < 100; x++) {
         for (int y = 30; y < 60; y++) {
            for (int z = -100; z < 100; z++) {
               if (re.level().getTile(x, y, z) == darkest) dark2++;
            }
         }
      }
      check(dark2 == 27, "art persists (" + dark2 + ")");
      float[] spawn2 = re.spawnPoint();
      check(spawn2 != null && spawn2[0] == spawn[0] && spawn2[1] == spawn[1] && spawn2[2] == spawn[2], "spawn persists");

      File dir2 = new File("importworld2");
      com.strata.server.ItemTest.wipeDir(dir2);
      GameServer g = new GameServer(new LocalConnection(), dir2, 556L, true);
      int cx0 = (int)Math.floor(g.player().x);
      int cz0 = (int)Math.floor(g.player().z);
      WorldImporter.importImage(g, gradient());
      int x0 = cx0 - WorldImporter.RAMP.length / 2;
      int z0 = cz0 - 2;
      boolean tierById = true;
      for (int x = x0; x < x0 + WorldImporter.RAMP.length; x++) {
         for (int z = z0; z <= z0 + 3; z++) {
            int tier = tierOf(g.level().getTile(x, WorldImporter.ART_Y, z));
            if (x == x0 && z == z0 + 3) continue; 
            if (tier != x - x0) {
               tierById = false;
            }
         }
      }
      check(tierById, "every column maps to its own ramp tier");
      check(g.level().getTile(x0, WorldImporter.ART_Y, z0 + 3) == 0,
         "transparent corner stays air");
      check(g.level().getTile(x0 + 1, WorldImporter.ART_Y, z0 + 3) == WorldImporter.RAMP[1],
         "neighbor column fills beside the cutout");

      if (failures == 0) System.out.println("IMPORT PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

