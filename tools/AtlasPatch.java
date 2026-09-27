// One-shot atlas painter (run manually, then keep for the record):
//   javac -d /tmp/atlastools tools/AtlasPatch.java && java -cp /tmp/atlastools AtlasPatch
// Paints tiles 12-14 (leaf, flower, tall grass) into res/terrain.png with a
// fixed seed, so the art is reproducible. Aborts without writing if any
// existing tile 0-11 has translucent pixels (the renderer uses alpha-test
// cutout for the new tiles; old tiles must stay fully opaque).
// Tile geometry: tile N occupies x in [N*16, N*16+16), y in [0, 16).
// Image top (y=0) renders at quad top (no flip in Textures).
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

public class AtlasPatch {
   static final int TILE = 16;

   public static void main(String[] args) throws Exception {
      File png = new File("res/terrain.png");
      BufferedImage img = ImageIO.read(png);
      if (img.getWidth() != 256 || img.getHeight() != 256) {
         throw new RuntimeException("unexpected atlas size " + img.getWidth() + "x" + img.getHeight());
      }
      // Safety gate: old tiles must be fully opaque for alpha-test cutout.
      for (int t = 0; t < 12; t++) {
         int min = 255;
         for (int x = t * TILE; x < t * TILE + TILE; x++) {
            for (int y = 0; y < TILE; y++) {
               int a = (img.getRGB(x, y) >>> 24) & 0xFF;
               if (a < min) {
                  min = a;
               }
            }
         }
         System.out.println("tile " + t + " minAlpha=" + min);
         if (min < 255) {
            throw new RuntimeException("tile " + t + " has translucent pixels, aborting (alpha test would punch holes)");
         }
      }
      Random rng = new Random(0xC10C);
      paintLeaf(img, 12, rng);
      paintFlower(img, 13, rng);
      paintTuft(img, 14, rng);
      paintWood(img, 15, rng);
      ImageIO.write(img, "png", png);
      System.out.println("atlas patched: tiles 12,13,14,15");
   }

   static int rgba(int r, int g, int b, int a) {
      return (a << 24) | (r << 16) | (g << 8) | b;
   }

   static void px(BufferedImage img, int tile, int x, int y, int color) {
      img.setRGB(tile * TILE + x, y, color);
   }

   // Opaque leaf mass: mid-green noise, dark speckles, a few deep holes.
   static void paintLeaf(BufferedImage img, int tile, Random rng) {
      for (int x = 0; x < TILE; x++) {
         for (int y = 0; y < TILE; y++) {
            int roll = rng.nextInt(100);
            int r, g, b;
            if (roll < 10) {
               r = 18; g = 52; b = 18; // deep hole
            } else if (roll < 28) {
               r = 30; g = 84; b = 28; // dark speckle
            } else {
               int v = rng.nextInt(33) - 16;
               r = 52 + v; g = 122 + v; b = 44 + v / 2; // lit mass
            }
            px(img, tile, x, y, rgba(r, g, b, 255));
         }
      }
   }

   // Opaque bark: brown mass, dark vertical furrows, lighter ridges, knots.
   static void paintWood(BufferedImage img, int tile, Random rng) {
      for (int x = 0; x < TILE; x++) {
         for (int y = 0; y < TILE; y++) {
            int v = rng.nextInt(25) - 12;
            int r, g, b;
            if (x % 4 == 3) {
               r = 70 + v; g = 54 + v; b = 34 + v / 2; // furrow
            } else if (x % 4 == 0) {
               r = 128 + v; g = 102 + v; b = 64 + v / 2; // ridge light
            } else {
               r = 106 + v; g = 84 + v; b = 52 + v / 2; // mass
            }
            px(img, tile, x, y, rgba(r, g, b, 255));
         }
      }
      // Two knots.
      int[][] knots = {{4, 5}, {11, 10}};
      for (int[] k : knots) {
         for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 0; dy++) {
               px(img, tile, k[0] + dx, k[1] + dy, rgba(58, 44, 28, 255));
            }
         }
         px(img, tile, k[0], k[1], rgba(40, 30, 18, 255));
      }
   }

   // Red bloom on a stem, transparent surround (cutout).
   static void paintFlower(BufferedImage img, int tile, Random rng) {
      for (int x = 0; x < TILE; x++) {
         for (int y = 0; y < TILE; y++) {
            px(img, tile, x, y, rgba(0, 0, 0, 0));
         }
      }
      // Stem: x=7..8, y=7..15, shaded right side.
      for (int y = 7; y < TILE; y++) {
         px(img, tile, 7, y, rgba(44, 110, 40, 255));
         px(img, tile, 8, y, rgba(30, 78, 28, 255));
      }
      // Sepal leaves.
      px(img, tile, 6, 11, rgba(44, 110, 40, 255));
      px(img, tile, 9, 12, rgba(30, 78, 28, 255));
      // Bloom: 5-wide head, rows y=2..6, red with light top / dark bottom.
      for (int x = 5; x <= 10; x++) {
         for (int y = 2; y <= 6; y++) {
            int dx = x - 7; // center 7.5
            if (dx * dx + (y - 4) * (y - 4) > 7) {
               continue;
            }
            int r = 200, g = 30, b = 26;
            if (y <= 3) { r = 232; g = 62; b = 56; } // sunlit top
            if (y == 6) { r = 150; g = 22; b = 20; } // shaded base
            px(img, tile, x, y, rgba(r + rng.nextInt(13) - 6, g, b, 255));
         }
      }
      // Heart.
      px(img, tile, 7, 4, rgba(250, 220, 90, 255));
      px(img, tile, 8, 4, rgba(250, 220, 90, 255));
   }

   // Grass blades, transparent surround (cutout).
   static void paintTuft(BufferedImage img, int tile, Random rng) {
      for (int x = 0; x < TILE; x++) {
         for (int y = 0; y < TILE; y++) {
            px(img, tile, x, y, rgba(0, 0, 0, 0));
         }
      }
      int[] blades = {2, 5, 8, 11, 13};
      for (int bx : blades) {
         int top = 4 + rng.nextInt(5); // blade tip row
         for (int y = top; y < TILE; y++) {
            int shade = 90 + (y - top) * 4 + rng.nextInt(17) - 8;
            int r = 62, g = Math.min(255, 110 + shade / 2), b = 40;
            px(img, tile, bx, y, rgba(r, g, b, 255));
            if (y == top) { // light tip pixel
               px(img, tile, bx, y, rgba(150, 200, 110, 255));
            }
         }
      }
   }
}
