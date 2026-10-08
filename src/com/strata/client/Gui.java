package com.strata.client;

import com.strata.blocks.AtlasStitcher;
import com.strata.blocks.BiomeTints;
import com.strata.blocks.Block;
import com.strata.blocks.Blocks;
import com.strata.blocks.CactusBlock;
import com.strata.blocks.CubeBlock;
import com.strata.blocks.SnowBlock;
import com.strata.core.Log;
import com.strata.server.Inventory;
import com.strata.server.ItemStack;
import com.strata.world.mesh.Textures;
import com.strata.world.mesh.Tesselator;
import org.lwjgl.opengl.GL11;

public class Gui {
   private int width;
   private int height;
   private int lastScale = 1;
   private int lastLoggedW = -1;
   private int lastLoggedH = -1;
   private int lastLoggedS = -1;

   public static int autoScale(int screenW, int screenH) {
      for (int s = 3; s >= 1; s--) {
         if (screenW / s >= 320 && screenH / s >= 240) {
            return s;
         }
      }
      return 1;
   }

   public static int resolveScale(int setting, int screenW, int screenH) {
      if (setting >= 1 && setting <= 3) {
         return setting;
      }
      return autoScale(screenW, screenH);
   }

   public void render(int screenWidth, int screenHeight, Inventory inventory, int selectedSlot, int scale, int hp, boolean spectator, String[] debug) {
      scale = resolveScale(scale, screenWidth, screenHeight);
      this.lastScale = scale;
      this.width = screenWidth / scale;
      this.height = screenHeight / scale;
      if (screenWidth != this.lastLoggedW || screenHeight != this.lastLoggedH || scale != this.lastLoggedS) {
         this.lastLoggedW = screenWidth;
         this.lastLoggedH = screenHeight;
         this.lastLoggedS = scale;
         Log.info("win",
            "gui " + screenWidth + "x" + screenHeight + " scale " + scale
            + " (" + this.width + "x" + this.height + " eff)");
      }

      GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glLoadIdentity();
      GL11.glOrtho(0.0, screenWidth, screenHeight, 0.0, 100.0, 300.0);
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glLoadIdentity();
      GL11.glTranslatef(0.0F, 0.0F, -200.0F);

      this.renderCrosshair();
      this.renderHotbar(inventory, selectedSlot, hp, spectator);
      this.renderDebug(debug);
   }

   private void renderCrosshair() {
      int cx = this.width / 2;
      int cy = this.height / 2;
      int size = 8;
      int thick = 1;

      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR);
      GL11.glDisable(GL11.GL_TEXTURE_2D);

      Tesselator t = Tesselator.SHARED;
      t.init();
      t.color(1.0F, 1.0F, 1.0F);
      this.fillQuad(t, cx - size, cy - thick, cx + size + 1, cy + thick + 1, 0xFFFFFFFF);
      this.fillQuad(t, cx - thick, cy - size, cx + thick + 1, cy + size + 1, 0xFFFFFFFF);
      t.flush();

      GL11.glDisable(GL11.GL_BLEND);
   }

   static final int BAR_PITCH = 20;
   static final int BAR_WELL = 3;
   static final int SEL_TX = 1;
   static final int SEL_TY = 23;
   static final int SEL_S = 22;
   static final float BAR_SCALE = 1.1F;

   private void renderHotbar(Inventory inventory, int selectedSlot, int hp, boolean spectator) {      int totalSlots = 9;
      float bs = Math.min(BAR_SCALE, this.width / 182.0F);
      int barWidth = Math.round(182 * bs);
      int barHeight = Math.round(22 * bs);
      int cell = Math.round(16 * bs);
      int selS = Math.round(SEL_S * bs);
      int pad = (cell - 16) / 2;
      int xStart = (this.width - barWidth) / 2;
      int yStart = this.height - barHeight - 8;

      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

      Tesselator t = Tesselator.SHARED;
      Textures.bind(Textures.loadTexture("/textures/gui/widgets.png", 9728));
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      t.init();
      this.widgetQuad(t, xStart, yStart, barWidth, barHeight, 0, 0, 182, 22);
      if (selectedSlot >= 0 && selectedSlot < totalSlots) {
         int hx = xStart + Math.round((BAR_WELL + selectedSlot * BAR_PITCH) * bs) - (selS - cell) / 2;
         int hy = yStart + Math.round(BAR_WELL * bs) - (selS - cell) / 2;
         this.widgetQuad(t, hx, hy, selS, selS, SEL_TX, SEL_TY, SEL_S, SEL_S);
      }
      t.flush();

      GL11.glDisable(GL11.GL_BLEND);

      GL11.glEnable(GL11.GL_TEXTURE_2D);
      int tex = Textures.loadAtlas(9728);
      Textures.bind(tex);

       t.init();
       for (int i = 0; i < Inventory.HOTBAR; i++) {
          int id = inventory.slots[i].blockId;
          int count = inventory.slots[i].count;
          if (id <= 0 || count <= 0) {
             continue;
          }
          this.renderItemIcon(xStart + Math.round((BAR_WELL + i * BAR_PITCH) * bs) + pad,
             yStart + Math.round(BAR_WELL * bs) + pad, id);
       }
       t.flush();

       Textures.bind(Textures.loadTexture("/textures/ascii.png", 9728));
       GL11.glEnable(GL11.GL_TEXTURE_2D);
       GL11.glEnable(GL11.GL_BLEND);
       GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
       t.init();
       for (int i = 0; i < Inventory.HOTBAR; i++) {
          int count = inventory.slots[i].count;
          if (inventory.slots[i].blockId <= 0 || count <= 0) {
             continue;
          }
          this.drawCount(t, xStart + Math.round((BAR_WELL + i * BAR_PITCH) * bs),
             yStart + Math.round(BAR_WELL * bs), count, bs);
       }
       t.flush();
       GL11.glDisable(GL11.GL_TEXTURE_2D);
       GL11.glDisable(GL11.GL_BLEND);
       this.renderHearts(hp, spectator, xStart, yStart, bs);
   }

   private void renderHearts(int hp, boolean spectator, int xStart, int yStart, float bs) {
      if (spectator) {
         return;
      }
      int pitch = Math.round(8 * bs);
      int size = (int)(9 * bs);
      int half = (int)(5 * bs);
      Tesselator t = Tesselator.SHARED;
      Textures.bind(Textures.loadTexture("/textures/gui/icons.png", 9728));
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      t.init();
      for (int i = 0; i < 10; i++) {
         int hx = xStart + i * pitch;
         int hy = yStart - Math.round(10 * bs);
         this.widgetQuad(t, hx, hy, size, size, 16, 0, 9, 9);
         if (hp >= i * 2 + 2) {
            this.widgetQuad(t, hx, hy, size, size, 52, 0, 9, 9);
         } else if (hp == i * 2 + 1) {
            this.widgetQuad(t, hx, hy, half, size, 16, 0, 5, 9);
            this.widgetQuad(t, hx, hy, size, size, 61, 0, 9, 9);
         }
      }
      t.flush();
      GL11.glDisable(GL11.GL_TEXTURE_2D);
   }

   static final int PANEL_W = 176;
   static final int PANEL_H = 166;
   static final int PITCH = 18;

   public int panelX(int screenW) {
      return (screenW - PANEL_W) / 2;
   }

   public int panelY(int screenH) {
      return (screenH - PANEL_H) / 2;
   }

   public int panelSlotX(int screenW, int stockSlot) {
      int c = stockSlot < Inventory.HOTBAR ? stockSlot : (stockSlot - Inventory.HOTBAR) % 9;
      return this.panelX(screenW) + 8 + c * PITCH;
   }

   public int panelSlotY(int screenH, int stockSlot) {
      int py = this.panelY(screenH);
      if (stockSlot < Inventory.HOTBAR) {
         return py + 142;
      }
      return py + 84 + ((stockSlot - Inventory.HOTBAR) / 9) * PITCH;
   }

   public int slotAt(int mx, int my) {
      for (int i = 0; i < Inventory.SLOTS; i++) {
         int x = this.panelSlotX(this.width, i);
         int y = this.panelSlotY(this.height, i);
         if (mx >= x && mx < x + PITCH && my >= y && my < y + PITCH) {
            return i;
         }
      }
      return -1;
   }

   private void slotRing(Tesselator t, int x, int y, int s) {
      this.fillQuad(t, x - 1, y - 1, x + s + 1, y, 0xFFFFFFFF);
      this.fillQuad(t, x - 1, y + s, x + s + 1, y + s + 1, 0xFFFFFFFF);
      this.fillQuad(t, x - 1, y, x, y + s, 0xFFFFFFFF);
      this.fillQuad(t, x + s, y, x + s + 1, y + s, 0xFFFFFFFF);
   }

   public void renderInventory(Inventory inventory, ItemStack held, int mouseX, int mouseY) {
      int gx = this.panelX(this.width);
      int gy = this.panelY(this.height);

      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      Tesselator t = Tesselator.SHARED;
      t.init();
      this.fillQuad(t, 0, 0, this.width, this.height, 0xAA000000);
      t.flush();

      Textures.bind(Textures.loadTexture("/textures/gui/container/inventory.png", 9728));
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      t.init();
      this.widgetQuad(t, gx, gy, PANEL_W, PANEL_H, 0, 0, PANEL_W, PANEL_H);
      t.flush();

      Textures.bind(Textures.loadAtlas(9728));
      t.init();
      for (int i = 0; i < Inventory.SLOTS; i++) {
         int id = inventory.slots[i].blockId;
         int count = inventory.slots[i].count;
         if (id <= 0 || count <= 0) {
            continue;
         }
         this.renderItemIcon(this.panelSlotX(this.width, i), this.panelSlotY(this.height, i), id);
      }
      if (!held.isEmpty()) {
         this.renderItemIcon(mouseX - 8, mouseY - 8, held.blockId);
      }
      t.flush();

      Textures.bind(Textures.loadTexture("/textures/ascii.png", 9728));
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      t.init();
      for (int i = 0; i < Inventory.SLOTS; i++) {
         int count = inventory.slots[i].count;
         if (inventory.slots[i].blockId <= 0 || count <= 0) {
            continue;
         }
         this.drawCount(t, this.panelSlotX(this.width, i), this.panelSlotY(this.height, i), count);
      }
      if (!held.isEmpty()) {
         this.drawCount(t, mouseX - 8, mouseY - 8, held.count);
      }
      t.flush();

      GL11.glDisable(GL11.GL_TEXTURE_2D);
      t.init();
      int hover = this.slotAt(mouseX, mouseY);
      if (hover >= 0) {
         this.slotRing(t, this.panelSlotX(this.width, hover), this.panelSlotY(this.height, hover), 16);
      }
      t.flush();
      GL11.glDisable(GL11.GL_BLEND);
   }

   private void widgetQuad(Tesselator t, int sx, int sy, int sw, int sh, int tx, int ty, int tw, int th) {
      float u0 = tx / 256.0F;
      float v0 = ty / 256.0F;
      float u1 = (tx + tw) / 256.0F;
      float v1 = (ty + th) / 256.0F;
      int s = this.lastScale;
      t.color(1.0F, 1.0F, 1.0F, 1.0F);
      t.tex(u0, v0);
      t.vertex(sx * s, sy * s, 0.0F);
      t.tex(u0, v1);
      t.vertex(sx * s, (sy + sh) * s, 0.0F);
      t.tex(u1, v1);
      t.vertex((sx + sw) * s, (sy + sh) * s, 0.0F);
      t.tex(u1, v0);
      t.vertex((sx + sw) * s, sy * s, 0.0F);
   }

   static float[] isoProj(float x, float y, float z) {
      float x1 = (float)(0.70710678 * (x - z));
      float y2 = (float)(-0.86602540 * y + 0.35355339 * (x + z));
      return new float[]{x1, y2};
   }

   private void isoVert(Tesselator t, float u, float v, int x, int y, float bx, float by, float bz, int s) {
      float[] p = isoProj(bx, by, bz);
      t.tex(u, v);
      t.vertex((x + 8F + 10F * p[0]) * s, (y + 8.72F + 10F * p[1]) * s, 0.0F);
   }

   private void renderItemIcon(int x, int y, int blockId) {
      Block b = Blocks.byId(blockId);
      if (b instanceof CactusBlock c) {
         float[] white = BiomeTints.colorFor(BiomeTints.NONE, BiomeTints.DEFAULT_BIOME);
         this.isoBox(x, y, c.topTexture, c.sideTexture,
            1.0F / 16.0F, 0.0F, 1.0F / 16.0F, 15.0F / 16.0F, 1.0F, 15.0F / 16.0F,
            white, white, -1, white);
         return;
      }
      if (b instanceof SnowBlock) {
         float[] white = BiomeTints.colorFor(BiomeTints.NONE, BiomeTints.DEFAULT_BIOME);
         this.isoBox(x, y, b.texture, b.texture,
            0.0F, 0.0F, 0.0F, 1.0F, 2.0F / 16.0F, 1.0F, white, white, -1, white);
         return;
      }
      if (!(b instanceof CubeBlock c)) {
         this.renderTileIcon(x, y, 16, Blocks.particleTile(blockId));
         return;
      }
      float[] topTint = BiomeTints.colorFor(AtlasStitcher.tintKind(c.topTexture), BiomeTints.DEFAULT_BIOME);
      float[] sideTint = BiomeTints.colorFor(AtlasStitcher.tintKind(c.sideTexture), BiomeTints.DEFAULT_BIOME);
      float[] overlayTint = BiomeTints.colorFor(
         c.overlayTexture < 0 ? BiomeTints.NONE : AtlasStitcher.tintKind(c.overlayTexture), BiomeTints.DEFAULT_BIOME);
      this.isoBox(x, y, c.topTexture, c.sideTexture,
         0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F,
         topTint, sideTint, c.overlayTexture, overlayTint);
   }

   private void isoBox(int x, int y, int topTile, int sideTile,
         float x0, float y0, float z0, float x1, float y1, float z1,
         float[] topTint, float[] sideTint, int overlay, float[] overlayTint) {
      float[] top = AtlasStitcher.uv(topTile);
      float[] side = AtlasStitcher.uv(sideTile);
      Tesselator t = Tesselator.SHARED;
      int s = this.lastScale;
      t.color(topTint[0], topTint[1], topTint[2]);
      this.isoVert(t, top[2], top[3], x, y, x1, y1, z1, s);
      this.isoVert(t, top[2], top[1], x, y, x1, y1, z0, s);
      this.isoVert(t, top[0], top[1], x, y, x0, y1, z0, s);
      this.isoVert(t, top[0], top[3], x, y, x0, y1, z1, s);
      t.color(0.6F * sideTint[0], 0.6F * sideTint[1], 0.6F * sideTint[2]);
      this.isoVert(t, side[0], side[3], x, y, x1, y0, z1, s);
      this.isoVert(t, side[2], side[3], x, y, x1, y0, z0, s);
      this.isoVert(t, side[2], side[1], x, y, x1, y1, z0, s);
      this.isoVert(t, side[0], side[1], x, y, x1, y1, z1, s);
      t.color(0.8F * sideTint[0], 0.8F * sideTint[1], 0.8F * sideTint[2]);
      this.isoVert(t, side[0], side[1], x, y, x0, y1, z1, s);
      this.isoVert(t, side[0], side[3], x, y, x0, y0, z1, s);
      this.isoVert(t, side[2], side[3], x, y, x1, y0, z1, s);
      this.isoVert(t, side[2], side[1], x, y, x1, y1, z1, s);
      if (overlay >= 0) {
         float[] ov = AtlasStitcher.uv(overlay);
         t.color(0.6F * overlayTint[0], 0.6F * overlayTint[1], 0.6F * overlayTint[2]);
         this.isoVert(t, ov[0], ov[3], x, y, x1 + 0.02F, y0, z1, s);
         this.isoVert(t, ov[2], ov[3], x, y, x1 + 0.02F, y0, z0, s);
         this.isoVert(t, ov[2], ov[1], x, y, x1 + 0.02F, y1, z0, s);
         this.isoVert(t, ov[0], ov[1], x, y, x1 + 0.02F, y1, z1, s);
         t.color(0.8F * overlayTint[0], 0.8F * overlayTint[1], 0.8F * overlayTint[2]);
         this.isoVert(t, ov[0], ov[1], x, y, x0, y1, z1 + 0.02F, s);
         this.isoVert(t, ov[0], ov[3], x, y, x0, y0, z1 + 0.02F, s);
         this.isoVert(t, ov[2], ov[3], x, y, x1, y0, z1 + 0.02F, s);
         this.isoVert(t, ov[2], ov[1], x, y, x1, y1, z1 + 0.02F, s);
      }
   }

   static float[] asciiUV(char ch) {
      int cell = ch & 0xFF;
      float u0 = (cell % 16) * 8.0F / 128.0F;
      float v0 = (cell / 16) * 8.0F / 128.0F;
      return new float[]{u0, v0, u0 + 8.0F / 128.0F, v0 + 8.0F / 128.0F};
   }

   static int drawStringWidth(String s) {
      return s == null ? 0 : s.length() * 6;
   }

   private void drawGlyph(Tesselator t, int x, int y, char ch, int col) {
      this.drawGlyph(t, x, y, ch, col, 8, 1);
   }

   private void drawGlyph(Tesselator t, int x, int y, char ch, int col, int glyphPx, int shadowPx) {
      float a = (col >> 24 & 0xFF) / 255.0F;
      float r = (col >> 16 & 0xFF) / 255.0F;
      float g = (col >> 8 & 0xFF) / 255.0F;
      float b = (col & 0xFF) / 255.0F;
      float[] uv = asciiUV(ch);
      float u0 = uv[0];
      float v0 = uv[1];
      float u1 = uv[2];
      float v1 = uv[3];
      int s = this.lastScale;
      t.color(r, g, b, a);
      t.tex(u0, v0);
      t.vertex(x * s, y * s, 0.0F);
      t.tex(u0, v1);
      t.vertex(x * s, (y + glyphPx) * s, 0.0F);
      t.tex(u1, v1);
      t.vertex((x + glyphPx) * s, (y + glyphPx) * s, 0.0F);
      t.tex(u1, v0);
      t.vertex((x + glyphPx) * s, y * s, 0.0F);
   }

   private void drawCount(Tesselator t, int x, int y, int count) {
      this.drawCount(t, x, y, count, 1.0F);
   }

   private void drawCount(Tesselator t, int x, int y, int count, float bs) {
      if (count <= 1) {
         return;
      }
      String s = Integer.toString(count);
      int adv = Math.round(6 * bs);
      int glyph = Math.round(8 * bs);
      int shadow = Math.max(1, Math.round(bs));
      int sx = x + Math.round(17 * bs) - s.length() * adv;
      int sy = y + Math.round(9 * bs);
      for (int i = 0; i < s.length(); i++) {
         char ch = s.charAt(i);
         if (ch < '0' || ch > '9') {
            continue;
         }
         this.drawGlyph(t, sx + i * adv + shadow, sy + shadow, ch, 0xFF3f3f3f, glyph, shadow);
         this.drawGlyph(t, sx + i * adv, sy, ch, 0xFFFFFFFF, glyph, shadow);
      }
   }

   private void drawString(Tesselator t, int x, int y, String s) {
      if (s == null) {
         return;
      }
      for (int i = 0; i < s.length(); i++) {
         char ch = s.charAt(i);
         if (ch < 32 || ch > 126) {
            continue;
         }
         this.drawGlyph(t, x + i * 6 + 1, y + 1, ch, 0xFF3f3f3f);
         this.drawGlyph(t, x + i * 6, y, ch, 0xFFFFFFFF);
      }
   }

   private void renderDebug(String[] lines) {
      if (lines == null || lines.length == 0) {
         return;
      }
      Tesselator t = Tesselator.SHARED;
      Textures.bind(Textures.loadTexture("/textures/ascii.png", 9728));
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      t.init();
      for (int i = 0; i < lines.length; i++) {
         if (lines[i] != null) {
            this.drawString(t, 4, 4 + i * 10, lines[i]);
         }
      }
      t.flush();
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      GL11.glDisable(GL11.GL_BLEND);
   }

   private void renderTileIcon(int x, int y, int size, int texIndex) {
      float[] r = AtlasStitcher.uv(texIndex);
      float u0 = r[0];
      float v0 = r[1];
      float u1 = r[2];
      float v1 = r[3];
      int s = this.lastScale;

      Tesselator t = Tesselator.SHARED;
      t.color(1.0F, 1.0F, 1.0F);
      t.tex(u0, v0);
      t.vertex(x * s, y * s, 0.0F);
      t.tex(u0, v1);
      t.vertex(x * s, (y + size) * s, 0.0F);
      t.tex(u1, v1);
      t.vertex((x + size) * s, (y + size) * s, 0.0F);
      t.tex(u1, v0);
      t.vertex((x + size) * s, y * s, 0.0F);
   }

   private void fillQuad(Tesselator t, int x0, int y0, int x1, int y1, int col) {
      float a = (col >> 24 & 0xFF) / 255.0F;
      float r = (col >> 16 & 0xFF) / 255.0F;
      float g = (col >> 8 & 0xFF) / 255.0F;
      float b = (col & 0xFF) / 255.0F;
      int s = this.lastScale;

      t.color(r, g, b, a);
      t.vertex(x0 * s, y1 * s, 0.0F);
      t.vertex(x1 * s, y1 * s, 0.0F);
      t.vertex(x1 * s, y0 * s, 0.0F);
      t.vertex(x0 * s, y0 * s, 0.0F);
   }
}

