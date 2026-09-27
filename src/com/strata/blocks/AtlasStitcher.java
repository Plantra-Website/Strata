package com.strata.blocks;

import com.strata.core.Log;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public final class AtlasStitcher {
   public static final int TILE = 16;
   public static final int COLS = 16;
   public static final int ATLAS_W = 2048;
   public static final int ATLAS_H = 2048;
   private static final int[] SHEET_SIZES = {2048, 4096, 8192};
   private static int SHEET_PX = 0;
   public static final int MAX_TILES = 1024;

   public static final String[] TILES = {
      "blocks/grass_top.png",
      "blocks/grass_side.png",
      "blocks/dirt.png",
      "blocks/stone.png",
      "blocks/cobblestone.png",
      "blocks/sand.png",
      "blocks/bedrock.png",
      "blocks/coal_ore.png",
      "blocks/iron_ore.png",
      "blocks/gold_ore.png",
      "blocks/diamond_ore.png",
      "blocks/lapis_ore.png",
      "blocks/gravel.png",
      "blocks/lava_still.png",
      "blocks/torch_on.png",
      "blocks/tinted/oak_leaves.png",
      "blocks/rose.png",
      "blocks/dandelion.png",
      "blocks/tall_grass.png",
      "blocks/oak_log_side.png",
      "blocks/oak_log_top.png",
      "blocks/oak_sapling.png",
      "blocks/destroy_stage_0.png",
      "blocks/destroy_stage_1.png",
      "blocks/destroy_stage_2.png",
      "blocks/destroy_stage_3.png",
      "blocks/destroy_stage_4.png",
      "blocks/destroy_stage_5.png",
      "blocks/destroy_stage_6.png",
      "blocks/destroy_stage_7.png",
      "blocks/destroy_stage_8.png",
      "blocks/destroy_stage_9.png",
      "blocks/grass_side_overlay.png",
      "blocks/oak_planks.png",
      "blocks/torch_on.png",
   };

   private AtlasStitcher() {
   }

   public static void validate() {
      ArrayList<String> missing = new ArrayList<>();
      for (String name : TILES) {
         if (AtlasStitcher.class.getResourceAsStream("/textures/" + name) == null) {
            missing.add(name);
         }
      }
      if (!missing.isEmpty()) {
         throw new RuntimeException("missing atlas tiles " + missing + " (rename the file or the TILES entry — both must agree)");
      }
   }

   private static Map<String, BufferedImage> pack = new HashMap<>();

   private static final ArrayList<String> ALT_NAMES = new ArrayList<>();
   private static final HashMap<Integer, int[]> ALT_SLOTS = new HashMap<>();

   public static int[] altsFor(int baseSlot) {
      ensureStitched();
      int[] alts = ALT_SLOTS.get(baseSlot);
      return alts == null ? new int[0] : alts.clone();
   }

   public static void setPack(Map<String, BufferedImage> overrides) {
      pack = overrides == null ? new HashMap<>() : overrides;
   }

   public static int slot(String name) {
      for (int i = 0; i < TILES.length; i++) {
         if (TILES[i].equals(name)) {
            return i;
         }
      }
      throw new RuntimeException("unknown atlas texture " + name);
   }

   static final HashMap<String, int[]> TINTS = new HashMap<>();

   private static final HashMap<Integer, boolean[]> ALPHA_MASKS = new HashMap<>();

   public static boolean[] alphaMask(int tile) {
      boolean[] m = ALPHA_MASKS.get(tile);
      if (m == null) {
         m = new boolean[256];
         java.util.Arrays.fill(m, true);
      }
      return m;
   }

   static {
      int[] grass = new int[]{145, 189, 89};
      int[] foliage = new int[]{119, 171, 47};
      TINTS.put("blocks/tall_grass.png", grass);
      TINTS.put("blocks/grass_top.png", grass);
      TINTS.put("blocks/grass_side_overlay.png", grass);
      TINTS.put("blocks/tinted/oak_leaves.png", foliage);
   }

   public static BufferedImage stitch() {
      if (TILES.length > MAX_TILES) {
         throw new RuntimeException(TILES.length + " tiles exceed the " + MAX_TILES + " slot index space");
      }
      ALPHA_MASKS.clear();
      ArrayList<String> names = new ArrayList<>();
      HashMap<Integer, int[]> altMap = new HashMap<>();
      HashMap<String, Integer> nameToSlot = new HashMap<>();
      for (int t = 0; t < TILES.length; t++) {
         names.add(TILES[t]);
         nameToSlot.put(TILES[t], t);
      }
      java.util.HashSet<String> seenBase = new java.util.HashSet<>();
      for (int t = 0; t < TILES.length; t++) {
         if (!seenBase.add(TILES[t])) {
            continue;
         }
         String base = TILES[t].substring(0, TILES[t].length() - 4);
         ArrayList<Integer> alts = new ArrayList<>();
         for (int n = 1; n <= 9; n++) {
            String alt = base + n + ".png";
            if (nameToSlot.containsKey(alt)) {
               continue;
            }
            if (pack.containsKey(alt) || AtlasStitcher.class.getResourceAsStream("/textures/" + alt) != null) {
               alts.add(names.size());
               nameToSlot.put(alt, names.size());
               names.add(alt);
            }
         }
         if (!alts.isEmpty()) {
            int[] arr = new int[alts.size()];
            for (int i = 0; i < arr.length; i++) {
               arr[i] = alts.get(i);
            }
            altMap.put(t, arr);
         }
      }
      ALT_NAMES.clear();
      for (int i = TILES.length; i < names.size(); i++) {
         ALT_NAMES.add(names.get(i));
      }
      ALT_SLOTS.clear();
      ALT_SLOTS.putAll(altMap);
      Integer[] order = new Integer[names.size()];
      java.util.ArrayList<java.util.ArrayList<BufferedImage>> allFrames =
         new java.util.ArrayList<>(names.size());
      for (int t = 0; t < names.size(); t++) {
         order[t] = t;
         BufferedImage raw = pack.containsKey(names.get(t)) ? pack.get(names.get(t)) : loadByName(names.get(t));
         allFrames.add(splitFrames(raw));
      }
      BufferedImage[] face = new BufferedImage[names.size()];
      for (int t = 0; t < names.size(); t++) {
         face[t] = allFrames.get(t).get(0);
      }
      java.util.Arrays.sort(order, (a, b) -> {
         int ha = face[a].getHeight();
         int hb = face[b].getHeight();
         if (ha != hb) {
            return hb - ha;
         }
         return face[b].getWidth() - face[a].getWidth();
      });
      int[][] rect = null;
      int size = 0;
      for (int s = 0; s < SHEET_SIZES.length && rect == null; s++) {
         size = SHEET_SIZES[s];
         rect = layout(order, face, size);
      }
      if (rect == null) {
         throw new RuntimeException("atlas full at " + SHEET_SIZES[SHEET_SIZES.length - 1]
            + "px: shrink art (fewer/large tiles share one sheet)");
      }
      if (size != SHEET_PX) {
         Log.info("atlas", "sheet " + size + "x" + size);
      }
      BufferedImage atlas = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
      ANIM.clear();
      for (int k = 0; k < order.length; k++) {
         int t = order[k];
         java.util.ArrayList<BufferedImage> frames = allFrames.get(t);
         int[] tint = tintFor(names.get(t));
         if (tint != null) {
            for (BufferedImage f : frames) {
               tintImage(f, tint);
            }
         }
         BufferedImage tile = frames.get(0);
         int w = tile.getWidth();
         int h = tile.getHeight();
         int dx = rect[t][0];
         int dy = rect[t][1];
         boolean[] mask = new boolean[256];
         for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
               atlas.setRGB(dx + x, dy + y, tile.getRGB(x, y));
            }
         }
         for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
               int px = tile.getRGB(x * w / 16, y * h / 16);
               mask[y * 16 + x] = ((px >>> 24) & 0xFF) >= 128;
            }
         }
         ALPHA_MASKS.put(t, mask);
         if (frames.size() > 1) {
            ANIM.put(t, frames);
         }
      }
      RECTS = rect;
      SHEET_PX = size;
      return atlas;
   }

   private static java.util.ArrayList<BufferedImage> splitFrames(BufferedImage img) {
      java.util.ArrayList<BufferedImage> out = new java.util.ArrayList<>();
      int w = img.getWidth();
      int h = img.getHeight();
      if (h <= w || h % w != 0) {
         out.add(exactCopy(img, 0, 0, w, h));
         return out;
      }
      for (int y = 0; y < h; y += w) {
         out.add(exactCopy(img, 0, y, w, w));
      }
      return out;
   }

   private static BufferedImage exactCopy(BufferedImage img, int x, int y, int w, int h) {
      BufferedImage f = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      java.awt.Graphics2D g = f.createGraphics();
      g.setComposite(java.awt.AlphaComposite.Src);
      g.drawImage(img, 0, 0, w, h, x, y, x + w, y + h, null);
      g.dispose();
      return f;
   }

   private static void tintImage(BufferedImage img, int[] tint) {
      int w = img.getWidth();
      int h = img.getHeight();
      for (int x = 0; x < w; x++) {
         for (int y = 0; y < h; y++) {
            int px = img.getRGB(x, y);
            int a = (px >>> 24) & 0xFF;
            int v = px & 0xFF; 
            img.setRGB(x, y, (a << 24) | (v * tint[0] / 255 << 16) | (v * tint[1] / 255 << 8) | (v * tint[2] / 255));
         }
      }
   }

   private static final HashMap<Integer, java.util.ArrayList<BufferedImage>> ANIM = new HashMap<>();

   public static int animFrames(int tile) {
      ensureStitched();
      java.util.ArrayList<BufferedImage> f = ANIM.get(tile);
      return f == null ? 1 : f.size();
   }

   public static BufferedImage animFrame(int tile, int frame) {
      ensureStitched();
      java.util.ArrayList<BufferedImage> f = ANIM.get(tile);
      if (f == null) {
         return null;
      }
      return f.get(frame % f.size());
   }

   public static java.util.Set<Integer> animTiles() {
      ensureStitched();
      return new java.util.HashSet<>(ANIM.keySet());
   }

   static int[] tintFor(String name) {
      int[] t = TINTS.get(name);
      if (t != null) {
         return t;
      }
      if (!name.endsWith(".png")) {
         return null;
      }
      String stem = name.substring(0, name.length() - 4);
      int i = stem.length();
      while (i > 0 && Character.isDigit(stem.charAt(i - 1))) {
         i--;
      }
      if (i == stem.length()) {
         return null;
      }
      return TINTS.get(stem.substring(0, i) + ".png");
   }

   private static int[][] layout(Integer[] order, BufferedImage[] art, int size) {
      int[][] rect = new int[order.length][];
      int shelfX = 0, shelfY = 0, shelfH = 0;
      for (int k = 0; k < order.length; k++) {
         int t = order[k];
         int w = art[t].getWidth();
         int h = art[t].getHeight();
         if (w <= 0 || h <= 0 || w > size || h > size) {
            return null;
         }
         if (shelfX + w > size) {
            shelfX = 0;
            shelfY += shelfH;
            shelfH = 0;
         }
         if (shelfY + h > size) {
            return null;
         }
         rect[t] = new int[]{shelfX, shelfY, w, h};
         shelfX += w;
         if (h > shelfH) {
            shelfH = h;
         }
      }
      return rect;
   }

   private static int[][] RECTS = null;

   private static synchronized void ensureStitched() {
      if (RECTS == null) {
         stitch();
      }
   }

   public static float[] uv(int tile) {
      float[] r = tileRect(tile);
      return new float[]{r[0], r[1], r[0] + r[2], r[1] + r[3]};
   }

   public static float[] tileRect(int tile) {
      ensureStitched();
      if (tile < 0 || tile >= RECTS.length || RECTS[tile] == null) {
         throw new RuntimeException("atlas tile " + tile + " has no packed rect");
      }
      int[] r = RECTS[tile];
      return new float[]{r[0] / (float)SHEET_PX, r[1] / (float)SHEET_PX,
         r[2] / (float)SHEET_PX, r[3] / (float)SHEET_PX};
   }

   public static int sheetPx() {
      ensureStitched();
      return SHEET_PX;
   }

   public static int[] tileRectPx(int tile) {
      ensureStitched();
      if (tile < 0 || tile >= RECTS.length || RECTS[tile] == null) {
         throw new RuntimeException("atlas tile " + tile + " has no packed rect");
      }
      return new int[]{RECTS[tile][0], RECTS[tile][1], RECTS[tile][2], RECTS[tile][3]};
   }

   private static BufferedImage loadByName(String name) {
      String path = "/textures/" + name;
      InputStream in = AtlasStitcher.class.getResourceAsStream(path);
      if (in == null) {
         throw new RuntimeException("missing atlas tile " + path + " (add the PNG, don't renumber)");
      }
      try {
         BufferedImage img = ImageIO.read(in);
         in.close();
         if (img == null) {
            throw new RuntimeException("unreadable atlas tile " + path);
         }
         return img;
      } catch (IOException e) {
         throw new RuntimeException("failed to read atlas tile " + path + ": " + e);
      }
   }
}

