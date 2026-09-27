package com.strata.world.mesh;

import com.strata.blocks.AtlasStitcher;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class Textures {
   private static final HashMap<String, Integer> idMap = new HashMap<>();
   private static int lastId = -9999999;

   public static int loadTexture(String resourceName, int mode) {
      Integer cached = idMap.get(resourceName);
      if (cached != null) {
         return cached;
      }
      try {
         BufferedImage img = ImageIO.read(Textures.class.getResourceAsStream(resourceName));
         if (img == null) {
            throw new RuntimeException("unreadable gui texture " + resourceName);
         }
         int id = upload(img, mode);
         idMap.put(resourceName, id);
         return id;
      } catch (IOException e) {
         throw new RuntimeException("missing gui texture " + resourceName + ": " + e);
      }
   }

   public static int loadAtlas(int mode) {
      Integer cached = idMap.get("/atlas");
      if (cached != null) {
         return cached;
      }
      int id = upload(AtlasStitcher.stitch(), mode);
      idMap.put("/atlas", id);
      return id;
   }

   private static int upload(BufferedImage img, int mode) {
      IntBuffer ib = BufferUtils.createIntBuffer(1);
      GL11.glGenTextures(ib);
      int id = ib.get(0);
      bind(id);
      GL11.glTexParameteri(3553, 10241, mode);
      GL11.glTexParameteri(3553, 10240, mode);
      int w = img.getWidth();
      int h = img.getHeight();
      ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * 4);
      int[] rawPixels = new int[w * h];
      img.getRGB(0, 0, w, h, rawPixels, 0, w);

      for (int i = 0; i < rawPixels.length; i++) {
         int a = rawPixels[i] >> 24 & 0xFF;
         int r = rawPixels[i] >> 16 & 0xFF;
         int g = rawPixels[i] >> 8 & 0xFF;
         int b = rawPixels[i] & 0xFF;
         rawPixels[i] = a << 24 | b << 16 | g << 8 | r;
      }

      pixels.asIntBuffer().put(rawPixels);
      GL11.glTexImage2D(3553, 0, 6408, w, h, 0, 6408, 5121, pixels);
      return id;
   }

   private static final long ANIM_FRAME_NS = 66666666L;
   private static final java.util.HashMap<Integer, Integer> animLast = new java.util.HashMap<>();

   public static void animateAtlas() {
      Integer id = idMap.get("/atlas");
      if (id == null) {
         return;
      }
      java.util.Set<Integer> tiles = AtlasStitcher.animTiles();
      if (tiles.isEmpty()) {
         return;
      }
      long now = System.nanoTime();
      bind(id);
      for (int tile : tiles) {
         int n = AtlasStitcher.animFrames(tile);
         int f = (int)((now / ANIM_FRAME_NS) % n);
         Integer last = animLast.get(tile);
         if (last != null && last == f) {
            continue;
         }
         animLast.put(tile, f);
         java.awt.image.BufferedImage img = AtlasStitcher.animFrame(tile, f);
         int[] r = AtlasStitcher.tileRectPx(tile);
         int w = img.getWidth();
         int h = img.getHeight();
         ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * 4);
         int[] rawPixels = new int[w * h];
         img.getRGB(0, 0, w, h, rawPixels, 0, w);
         for (int i = 0; i < rawPixels.length; i++) {
            int a = rawPixels[i] >> 24 & 0xFF;
            int rr = rawPixels[i] >> 16 & 0xFF;
            int g = rawPixels[i] >> 8 & 0xFF;
            int b = rawPixels[i] & 0xFF;
            rawPixels[i] = a << 24 | b << 16 | g << 8 | rr;
         }
         pixels.asIntBuffer().put(rawPixels);
         GL11.glTexSubImage2D(3553, 0, r[0], r[1], w, h, 6408, 5121, pixels);
      }
   }

   public static void bind(int id) {
      if (id != lastId) {
         GL11.glBindTexture(3553, id);
         lastId = id;
      }
   }
}

