// One-shot atlas splitter (run once, kept for the record):
//   javac -d /tmp/atlastools tools/SplitAtlas.java && java -cp /tmp/atlastools SplitAtlas
// Explodes res/terrain.png's top row into res/textures/blocks/NN_name.png
// (lossless: same pixels, 16x16 each). From here on the per-tile PNGs are
// canonical and the atlas is stitched at startup (see AtlasStitcher) —
// never hand-edit a stitched atlas again.
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class SplitAtlas {
   static final String[] NAMES = {
      "grass", "stone", "dirt", "cobble",
      "sand", "bedrock", "coal", "iron",
      "gold", "diamond", "lava", "torch",
      "leaf", "flower", "tuft", "wood",
   };

   public static void main(String[] args) throws Exception {
      BufferedImage img = ImageIO.read(new File("res/terrain.png"));
      if (img.getWidth() != 256 || img.getHeight() != 256) {
         throw new RuntimeException("unexpected atlas size");
      }
      File dir = new File("res/textures/blocks");
      if (!dir.isDirectory() && !dir.mkdirs()) {
         throw new RuntimeException("cannot create " + dir);
      }
      for (int t = 0; t < NAMES.length; t++) {
         BufferedImage tile = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
         for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
               tile.setRGB(x, y, img.getRGB(t * 16 + x, y));
            }
         }
         File out = new File(dir, NAMES[t] + ".png");
         if (out.exists()) {
            throw new RuntimeException("refusing to overwrite " + out + " (delete by hand if re-splitting)");
         }
         ImageIO.write(tile, "png", out);
         System.out.println("wrote " + out);
      }
   }
}
