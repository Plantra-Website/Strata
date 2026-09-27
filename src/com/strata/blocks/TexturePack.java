package com.strata.blocks;

import com.strata.core.Log;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

public final class TexturePack {
   public static final String PREFIX = "textures/blocks/";

   private TexturePack() {
   }

   public static Map<String, BufferedImage> loadPack(File zip) {
      Map<String, BufferedImage> out = new HashMap<>();
      if (zip == null || !zip.isFile()) {
         return out;
      }
      try {
         ZipFile zf = new ZipFile(zip);
         try {
            java.util.Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
               ZipEntry e = entries.nextElement();
               String name = e.getName();
               if (e.isDirectory() || !name.startsWith(PREFIX) || !name.endsWith(".png")) {
                  continue;
               }
               String tile = name.substring("textures/".length());
                try {
                   BufferedImage img = ImageIO.read(zf.getInputStream(e));
                   if (img == null) {
                      Log.warn("pack", "unreadable entry " + name + ", skipping");
                   } else {
                      out.put(tile, img);
                   }
                } catch (Exception ex) {
                  Log.warn("pack", "bad entry " + name + ", skipping: " + ex.getMessage());
               }
            }
         } finally {
            zf.close();
         }
      } catch (Exception e) {
         Log.warn("pack", "could not read " + zip + ", built-ins stand in: " + e.getMessage());
      }
      return out;
   }
}

