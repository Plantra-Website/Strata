package com.strata.client;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class GuiAssetTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static void sheet(String path) throws Exception {
      InputStream in = GuiAssetTest.class.getResourceAsStream(path);
      check(in != null, "present: " + path);
      if (in == null) {
         return;
      }
      BufferedImage img = ImageIO.read(in);
      in.close();
      check(img != null && img.getWidth() == 256 && img.getHeight() == 256,
         path + " is 256x256 (got " + (img == null ? "null" : img.getWidth() + "x" + img.getHeight()) + ")");
   }

   public static void main(String[] args) throws Exception {
      sheet("/textures/gui/widgets.png");
      sheet("/textures/gui/icons.png");
      sheet("/textures/gui/container/inventory.png");
      asciiSheet("/textures/ascii.png");
      if (failures == 0) System.out.println("GUIASSET PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }

   static void asciiSheet(String path) throws Exception {
      InputStream in = GuiAssetTest.class.getResourceAsStream(path);
      check(in != null, "present: " + path);
      if (in == null) {
         return;
      }
      BufferedImage img = ImageIO.read(in);
      in.close();
      check(img != null && img.getWidth() == 128 && img.getHeight() == 128,
         path + " is 128x128 (got " + (img == null ? "null" : img.getWidth() + "x" + img.getHeight()) + ")");
   }
}

