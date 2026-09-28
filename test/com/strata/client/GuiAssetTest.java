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
      widgetsSelector();
      if (failures == 0) System.out.println("GUIASSET PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }

   static void widgetsSelector() throws Exception {
      InputStream in = GuiAssetTest.class.getResourceAsStream("/textures/gui/widgets.png");
      check(in != null, "present for selector check");
      if (in == null) {
         return;
      }
      BufferedImage img = ImageIO.read(in);
      in.close();
      int opaque = 0;
      for (int y = Gui.SEL_TY; y < Gui.SEL_TY + Gui.SEL_S; y++) {
         for (int x = Gui.SEL_TX; x < Gui.SEL_TX + Gui.SEL_S; x++) {
            if (((img.getRGB(x, y) >>> 24) & 0xFF) > 20) {
               opaque++;
            }
         }
      }
      check(opaque > Gui.SEL_S, "selector rect non-empty (" + opaque + "px)");
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

