package com.strata.client;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import com.strata.core.Log;

public class FrameTap {
   public static final String PATH = "/tmp/strata-frame.bin";
   private static final int DEFAULT_FPS = 15;

   private final int tw;
   private final int th;
   private final int targetFps;
   private final long minIntervalMs;
   private ByteBuffer pixels;
   private final byte[] out;
   private long counter = 0L;
   private long lastMs = 0L;
   private boolean dead = false;
   private int lastSrcW = -1;
   private int lastSrcH = -1;

   public FrameTap(int tw, int th, int fps) {
      this.tw = tw;
      this.th = th;
      if (fps < 1 || fps > 60) {
         fps = DEFAULT_FPS;
      }
      this.targetFps = fps;
      this.minIntervalMs = 1000L / fps;
      this.out = new byte[8 + tw * th * 3];
      try {
         new File(PATH).delete();
      } catch (Throwable t) {
      }
   }

   public static FrameTap fromSpec(String spec) {
      int w = 160;
      int h = 120;
      int fps = DEFAULT_FPS;
      if (spec != null) {
         String size = spec.trim();
         int colon = size.indexOf(':');
         if (colon >= 0) {
            try {
               fps = Integer.parseInt(size.substring(colon + 1).trim());
            } catch (NumberFormatException e) {
               Log.warn("term", "bad --terminal fps '" + spec + "', using " + DEFAULT_FPS);
               fps = DEFAULT_FPS;
            }
            size = size.substring(0, colon);
         }
         String[] p = size.trim().split("x");
         if (p.length == 2) {
            try {
               w = Integer.parseInt(p[0].trim());
               h = Integer.parseInt(p[1].trim());
            } catch (NumberFormatException e) {
               Log.warn("term", "bad --terminal size '" + spec + "', using 160x120");
               w = 160;
               h = 120;
            }
         }
      }
      if (fps < 1 || fps > 60) {
         Log.warn("term", "clamping --terminal fps to 1..60");
         fps = Math.max(1, Math.min(60, fps));
      }
      if (w < 16 || h < 16 || w > 640 || h > 480) {
         Log.warn("term", "clamping --terminal size to 16..640x16..480");
         w = Math.max(16, Math.min(640, w));
         h = Math.max(16, Math.min(480, h));
      }
      Log.info("term", "terminal tap " + w + "x" + h + "@" + fps + "fps -> " + PATH);
      return new FrameTap(w, h, fps);
   }

   public int targetFps() {
      return this.targetFps;
   }

   public int tapWidth() {
      return this.tw;
   }

   public int tapHeight() {
      return this.th;
   }

   public void tap(int srcW, int srcH) {
      if (this.dead) {
         return;
      }
      long now = System.currentTimeMillis();
      if (now - this.lastMs < this.minIntervalMs && this.counter > 0L) {
         return;
      }
      this.lastMs = now;
      try {
         if (srcW <= 0 || srcH <= 0 || srcW > 4096 || srcH > 4096) {
            return;
         }
         if (srcW != this.lastSrcW || srcH != this.lastSrcH) {
            int need = srcW * srcH * 3;
            this.pixels = BufferUtils.createByteBuffer(need);
            this.lastSrcW = srcW;
            this.lastSrcH = srcH;
            return;
         }
         int need = srcW * srcH * 3;
         if (this.pixels == null || this.pixels.capacity() < need) {
            this.pixels = BufferUtils.createByteBuffer(need);
         }
         ((Buffer) this.pixels).clear();
         GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
         GL11.glReadPixels(0, 0, srcW, srcH, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, this.pixels);
         String head = String.format("%08d", this.counter % 100000000L);
         for (int i = 0; i < 8; i++) {
            this.out[i] = (byte) head.charAt(i);
         }
         int o = 8;
         for (int ty = 0; ty < this.th; ty++) {
            int sy = srcH - 1 - (ty * srcH / this.th);
            int row = sy * srcW * 3;
            for (int tx = 0; tx < this.tw; tx++) {
               int sx = tx * srcW / this.tw;
               int s = row + sx * 3;
               this.out[o++] = this.pixels.get(s);
               this.out[o++] = this.pixels.get(s + 1);
               this.out[o++] = this.pixels.get(s + 2);
            }
         }
         File tmp = new File(PATH + ".tmp");
         FileOutputStream fos = new FileOutputStream(tmp);
         try {
            fos.write(this.out);
         } finally {
            fos.close();
         }
         Files.move(tmp.toPath(), new File(PATH).toPath(),
            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         this.counter++;
      } catch (Throwable t) {
         Log.warn("term", "tap failed, disabled: " + t.getMessage());
         this.dead = true;
      }
   }

   public void close() {
      try {
         new File(PATH).delete();
      } catch (Throwable t) {
      }
   }
}

