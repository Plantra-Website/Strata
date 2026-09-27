package com.strata.server;

import com.strata.blocks.Blocks;
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

   public static void main(String[] args) throws Exception {
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
      GameServer s = new GameServer(new LocalConnection(), dir, 555L, true);
      check(s.level().getTile(200, 40, -200) == 0, "void is air");
      WorldImporter.importImage(s, twoTone());
      s.save();

      int dark = 0, light = 0, offLevel = 0;
      int minX = 999, maxX = -999, minZ = 999, maxZ = -999;
      for (int x = -100; x < 100; x++) {
         for (int y = 30; y < 60; y++) {
            for (int z = -100; z < 100; z++) {
               int t = s.level().getTile(x, y, z);
               if (t == Blocks.COAL_ID || t == Blocks.SAND_ID) {
                  if (y != WorldImporter.ART_Y) {
                     offLevel++;
                  } else {
                     if (t == Blocks.COAL_ID) {
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
      check(dark == 27, "27 dark cells (got " + dark + ")");
      check(light == 36, "36 light cells (got " + light + ")");
      check(maxX - minX == 7 && maxZ - minZ == 7, "8x8 footprint");
      check(s.level().getBrightness(minX, WorldImporter.ART_Y + 1, minZ) == 1.0F, "bright above art");
      float[] spawn = s.spawnPoint();
      check(spawn != null, "spawn set");
      int tx = (int)Math.floor(spawn[0]);
      int tz = (int)Math.floor(spawn[2]);
      check(s.level().getTile(tx, 40, tz) == Blocks.STONE_ID, "tower stalk");
      check(s.level().getTile(tx, 51, tz) == Blocks.STONE_ID, "tower cap");
      check(Math.abs(s.player().x - spawn[0]) < 0.01, "player on spawn x");
      check(Math.abs(s.player().y - spawn[1]) < 0.01, "player on spawn y");
      check(s.player().yRot == 0.0F && s.player().xRot == 30.0F, "player faces art pitched down");

      GameServer re = new GameServer(new LocalConnection(), dir, null);
      check(re.level().getTile(200, 40, -200) == 0, "still void after reboot");
      int dark2 = 0;
      for (int x = -100; x < 100; x++) {
         for (int y = 30; y < 60; y++) {
            for (int z = -100; z < 100; z++) {
               if (re.level().getTile(x, y, z) == Blocks.COAL_ID) dark2++;
            }
         }
      }
      check(dark2 == 27, "art persists (" + dark2 + ")");
      float[] spawn2 = re.spawnPoint();
      check(spawn2 != null && spawn2[0] == spawn[0] && spawn2[1] == spawn[1] && spawn2[2] == spawn[2], "spawn persists");

      if (failures == 0) System.out.println("IMPORT PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

