package com.strata.server;

import com.strata.blocks.Blocks;
import com.strata.core.Log;
import com.strata.world.Level;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javax.imageio.ImageIO;

public final class WorldImporter {
   public static final int MAX = 256;
   public static final int DARK_ID = Blocks.COAL_ID;
   public static final int LIGHT_ID = Blocks.SAND_ID;
   public static final int ART_Y = 44;

   private WorldImporter() {
   }

   public static BufferedImage readImage(File f) {
      try {
         BufferedImage img = ImageIO.read(f);
         if (img != null) {
            return img;
         }
      } catch (Exception e) {
         Log.warn("import", "plain read failed, retrying stripped: " + e.getMessage());
      }
      try {
         byte[] raw = java.nio.file.Files.readAllBytes(f.toPath());
         byte[] clean = stripSegments(raw);
         if (clean != null) {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(clean));
            if (img != null) {
               Log.info("import", "decoded after APP-strip (" + raw.length + " -> " + clean.length + " bytes)");
               return img;
            }
         }
      } catch (Exception e) {
         Log.warn("import", "stripped read failed: " + e.getMessage());
      }
      return null;
   }

   static byte[] stripSegments(byte[] d) {
      try {
         if (d.length < 2 || (d[0] & 0xFF) != 0xFF || (d[1] & 0xFF) != 0xD8) {
            return null;
         }
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         out.write(d, 0, 2);
         int pos = 2;
         while (pos < d.length - 1) {
            if ((d[pos] & 0xFF) != 0xFF) {
               return null;
            }
            int m = d[pos + 1] & 0xFF;
            if (m == 0xD8 || m == 0xD9 || (m >= 0xD0 && m <= 0xD7) || m == 0x01) {
               pos += 2;
               continue;
            }
            int ln = ((d[pos + 2] & 0xFF) << 8) | (d[pos + 3] & 0xFF);
            if (ln < 2 || pos + 2 + ln > d.length + 1) {
               return null;
            }
            if ((m >= 0xE0 && m <= 0xEF) || m == 0xFE) {
               pos += 2 + ln;
               continue;
            }
            out.write(d, pos, 2 + ln);
            pos += 2 + ln;
            if (m == 0xDA) {
               out.write(d, pos, d.length - pos);
               break;
            }
         }
         return out.toByteArray();
      } catch (Exception e) {
         return null;
      }
   }

   public static void importImage(GameServer server, BufferedImage img) {
      int w = img.getWidth();
      int h = img.getHeight();
      double s = Math.min(1.0, MAX / (double)Math.max(w, h));
      int dw = Math.max(1, (int)Math.round(w * s));
      int dh = Math.max(1, (int)Math.round(h * s));
      if (dw != w || dh != h) {
         Log.info("import", "downscaled " + w + "x" + h + " to " + dw + "x" + dh + " (cap " + MAX + ")");
      }
      Level level = server.level();
      Player player = server.player();
      int cx = (int)Math.floor(player.x);
      int cz = (int)Math.floor(player.z);
      int x0 = cx - dw / 2;
      int z0 = cz - dh / 2;
      int dark = 0;
      int light = 0;
      int clear = 0;
      for (int ix = 0; ix < dw; ix++) {
         for (int iz = 0; iz < dh; iz++) {
            int sx = Math.min(w - 1, (int)(ix / s));
            int sy = Math.min(h - 1, (int)(iz / s));
            int px = img.getRGB(sx, sy);
            int a = (px >>> 24) & 0xFF;
            if (a < 128) {
               clear++;
               continue;
            }
            int r = (px >> 16) & 0xFF;
            int g = (px >> 8) & 0xFF;
            int b = px & 0xFF;
            int id = ((r + g + b) / 3 < 128) ? DARK_ID : LIGHT_ID;
            if (id == DARK_ID) {
               dark++;
            } else {
               light++;
            }
            level.setTileBulk(x0 + ix, ART_Y, z0 + iz, id);
         }
      }
      int tx = cx;
      int tz = z0 + dh + 8;
      for (int y = 30; y <= 50; y++) {
         level.setTileBulk(tx, y, tz, Blocks.STONE_ID);
      }
      for (int dx = -2; dx <= 2; dx++) {
         for (int dz = -2; dz <= 2; dz++) {
            level.setTileBulk(tx + dx, 51, tz + dz, Blocks.STONE_ID);
         }
      }
      float spawnX = tx + 0.5F;
      float spawnY = 52.0F;
      float spawnZ = tz + 0.5F;
      level.setSpawnPoint(spawnX, spawnY, spawnZ);
      player.teleport(spawnX, spawnY, spawnZ);
      player.yRot = 0.0F;
      player.xRot = 30.0F;
      Log.info("import", "pasted flat " + dw + "x" + dh + " (" + dark + " dark/" + light + " light/" + clear
         + " clear) at y" + ART_Y + ", tower spawn facing it (or fly with spectator ' for the top-down view)");
   }
}

