package com.strata.client;

import com.strata.world.mesh.Textures;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public final class ParticleAtlas {
   public static final String[] SPRITES = {
      "flame.png", "lava.png",
      "generic_0.png", "generic_1.png", "generic_2.png", "generic_3.png",
      "generic_4.png", "generic_5.png", "generic_6.png", "generic_7.png",
   };
   public static final int SHEET = 256;

   private static int[][] RECTS = null;
   private static BufferedImage sheet = null;

   private ParticleAtlas() {
   }

   public static int indexOf(String name) {
      for (int i = 0; i < SPRITES.length; i++) {
         if (SPRITES[i].equals(name)) {
            return i;
         }
      }
      throw new RuntimeException("unknown particle sprite " + name);
   }

   public static float[] uv(int index) {
      ensureStitched();
      int[] r = RECTS[index];
      return new float[]{r[0] / (float)SHEET, r[1] / (float)SHEET,
         (r[0] + r[2]) / (float)SHEET, (r[1] + r[3]) / (float)SHEET};
   }

   public static int smokeIndex(int f) {
      if (f < 0) {
         f = 0;
      }
      if (f > 7) {
         f = 7;
      }
      return indexOf("generic_" + f + ".png");
   }

   static int[] rectPx(int index) {
      ensureStitched();
      return new int[]{RECTS[index][0], RECTS[index][1], RECTS[index][2], RECTS[index][3]};
   }

   public static int texture() {
      ensureStitched();
      return Textures.loadImage("/particles", sheet, 9728);
   }

   private static synchronized void ensureStitched() {
      if (RECTS != null) {
         return;
      }
      stitch();
   }

   private static void stitch() {
      Map<String, BufferedImage> art = new HashMap<>();
      ArrayList<String> names = new ArrayList<>();
      for (String name : SPRITES) {
         String path = "/textures/particles/" + name;
         try {
            InputStream in = ParticleAtlas.class.getResourceAsStream(path);
            if (in == null) {
               throw new RuntimeException("missing particle sprite " + path);
            }
            BufferedImage img = ImageIO.read(in);
            in.close();
            if (img == null) {
               throw new RuntimeException("unreadable particle sprite " + path);
            }
            art.put(name, img);
            names.add(name);
         } catch (RuntimeException e) {
            throw e;
         } catch (Exception e) {
            throw new RuntimeException("failed to read particle sprite " + path + ": " + e);
         }
      }
      ArrayList<Integer> order = new ArrayList<>();
      for (int i = 0; i < names.size(); i++) {
         order.add(i);
      }
      order.sort((a, b) -> {
         BufferedImage ia = art.get(names.get(a));
         BufferedImage ib = art.get(names.get(b));
         if (ia.getHeight() != ib.getHeight()) {
            return ib.getHeight() - ia.getHeight();
         }
         return ib.getWidth() - ia.getWidth();
      });
      int[][] rect = new int[names.size()][];
      int shelfX = 0, shelfY = 0, shelfH = 0;
      for (int k = 0; k < order.size(); k++) {
         int t = order.get(k);
         BufferedImage img = art.get(names.get(t));
         int w = img.getWidth();
         int h = img.getHeight();
         if (shelfX + w > SHEET) {
            shelfX = 0;
            shelfY += shelfH;
            shelfH = 0;
         }
         if (shelfY + h > SHEET || w > SHEET || h > SHEET) {
            throw new RuntimeException("particle sheet full at " + names.get(t)
               + " (" + w + "x" + h + "): grow SHEET");
         }
         rect[t] = new int[]{shelfX, shelfY, w, h};
         shelfX += w;
         if (h > shelfH) {
            shelfH = h;
         }
      }
      BufferedImage out = new BufferedImage(SHEET, SHEET, BufferedImage.TYPE_INT_ARGB);
      for (int t = 0; t < names.size(); t++) {
         BufferedImage img = art.get(names.get(t));
         int[] r = rect[t];
         for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
               out.setRGB(r[0] + x, r[1] + y, img.getRGB(x, y));
            }
         }
      }
      RECTS = rect;
      sheet = out;
   }
}

