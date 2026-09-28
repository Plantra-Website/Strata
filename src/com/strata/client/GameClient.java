package com.strata.client;

import com.strata.HitResult;
import com.strata.blocks.AtlasStitcher;
import com.strata.blocks.Blocks;
import com.strata.core.Config;
import com.strata.core.DayCycle;
import com.strata.core.Debug;
import com.strata.core.Log;
import com.strata.core.MathHelper;
import com.strata.core.Profiler;
import com.strata.core.Timer;
import com.strata.net.BreakEffect;
import com.strata.net.BulkTiles;
import com.strata.net.DebugGive;
import com.strata.net.InputState;
import com.strata.net.InventorySync;
import com.strata.net.ItemRemove;
import com.strata.net.FallingSpawn;
import com.strata.net.HurtSelf;
import com.strata.net.ItemSpawn;
import com.strata.net.LocalConnection;
import com.strata.net.Packet;
import com.strata.net.PlaceBlock;
import com.strata.net.PlayerState;
import com.strata.net.SlotClick;
import com.strata.net.SaveGame;
import com.strata.net.SpectateToggle;
import com.strata.net.TileUpdate;
import com.strata.net.TimeCycle;
import com.strata.net.TimeUpdate;
import com.strata.server.GameServer;
import com.strata.server.Inventory;
import com.strata.server.ItemStack;
import com.strata.server.Player;
import com.strata.world.Level;
import com.strata.world.mesh.LevelRenderer;
import com.strata.world.mesh.Raycaster;
import com.strata.world.mesh.Textures;
import com.strata.world.mesh.Chunk;
import java.io.File;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import javax.swing.JOptionPane;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

public class GameClient implements Runnable {
   private int width;
   private int height;
   private final FloatBuffer fogColor = BufferUtils.createFloatBuffer(4);
   private final Timer timer = new Timer(60.0F);
   private Level level;
   private LevelRenderer levelRenderer;
   private Player player;
   private ParticleEngine particleEngine;
   private ItemRenderer itemRenderer;
   private FallingRenderer fallingRenderer;
   private int ambientClock = 0;
   private float prevTickY = 0.0F;
   private boolean prevTickGround = true;
   private final Inventory inventory = new Inventory();
   private boolean inventoryOpen = false;
   private final ItemStack held = new ItemStack();
   private final Gui gui = new Gui();
   private HitResult hitResult = null;
   private int selectedSlot = 0;
   private int paintTile = 1;
   private float destroyProgress = 0.0F;
   private long clientTime = 0;
   private boolean lastSpectator = false;
   private LocalConnection conn;
   private GameServer server;
   private int lastSub = -1;
   private Options options = new Options();
   private boolean fullBright = false;
   private boolean showDebug = false;
   private int fps = 0;
   private boolean leftDown = false;
   private boolean lastActive = true;
   private boolean mouseReleased = false;
   private java.io.File worldDir = new java.io.File(DEFAULT_WORLD);
   private Long seedOverride = null;
   private java.io.File importImage = null;
   private String packName = null;
   private static final int PERF_SAMPLES = 180;
   private long perfTotalNs;
   private long perfPickNs;
   private long perfWorldNs;
   private long perfHudNs;
   private long perfHitNs;
   private long perfPartNs;
   private long perfOverNs;
   private long perfGuiNs;
   private long perfSwapNs;
   private long perfMaxNs;
   private int perfSamples;
   private long perfGcCount;
   private long perfGcMs;
    public static final long DAY_LENGTH = Config.DAY_LENGTH;

   public static float dayAmount(long time) {
      double sun = Math.sin(time * Math.PI * 2.0 / DAY_LENGTH);
      double t = (sun + 0.08) / 0.28;
      if (t < 0.0) {
         t = 0.0;
      }
      if (t > 1.0) {
         t = 1.0;
      }
      return (float)(t * t * (3.0 - 2.0 * t));
   }

   private static long bootMarkNs = 0L;

   private static void mark(String stage) {
      long now = System.nanoTime();
      if (bootMarkNs == 0L) {
         bootMarkNs = now;
      }
       Log.info("boot", "+" + (now - bootMarkNs) / 1000000L + "ms " + stage);
   }

   public static final String DEFAULT_WORLD = "saves/testing";

   public void setWorldDir(java.io.File dir) {
      this.worldDir = dir;
   }

   public void setSeedOverride(Long seed) {
      this.seedOverride = seed;
   }

   public void setImportImage(java.io.File image) {
      this.importImage = image;
   }

   public void setPackName(String name) {
      this.packName = name;
   }

   public void applyImportDefaults() {
      if (this.importImage != null && this.worldDir.getPath().equals(DEFAULT_WORLD)) {
         this.worldDir = new java.io.File("import");
      }
   }

   public void init() throws LWJGLException, IOException {
      mark("init start");
      AtlasStitcher.validate();
      this.options = Options.load(new File("options.txt"));
      Config.VIEW_RADIUS = this.options.viewRadius;
      Log.info("options", "viewRadius=" + Config.VIEW_RADIUS + " sensitivity=" + this.options.sensitivity);
      int col = 920330;
      this.fogColor.put(new float[]{(col >> 16 & 0xFF) / 255.0F, (col >> 8 & 0xFF) / 255.0F, (col & 0xFF) / 255.0F, 1.0F});
      ((Buffer)this.fogColor).flip();
      Display.setDisplayMode(new DisplayMode(1024, 768));
      Display.setTitle("Strata");
      Display.setResizable(true);
      Display.create();
      mark("display created");
       Log.info("win", "mode=" + Display.getDisplayMode() + " visible=" + Display.isVisible() + " active=" + Display.isActive());
       Log.info("gl", "vendor=" + GL11.glGetString(GL11.GL_VENDOR) + " renderer=" + GL11.glGetString(GL11.GL_RENDERER) + " version=" + GL11.glGetString(GL11.GL_VERSION));
      Keyboard.create();
      Mouse.create();
      mark("input created");
      this.width = Display.getDisplayMode().getWidth();
      this.height = Display.getDisplayMode().getHeight();
       GL11.glEnable(GL11.GL_TEXTURE_2D);
       GL11.glShadeModel(GL11.GL_SMOOTH);
       GL11.glClearDepth(1.0);
       GL11.glEnable(GL11.GL_DEPTH_TEST);
       GL11.glDepthFunc(GL11.GL_LEQUAL);
       GL11.glEnable(GL11.GL_ALPHA_TEST);
       GL11.glAlphaFunc(GL11.GL_GREATER, 0.5F);
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glLoadIdentity();
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      this.conn = new LocalConnection();
      this.server = new GameServer(this.conn, this.worldDir, this.seedOverride, this.importImage != null);
      mark("server ready (seed=" + this.server.seed() + " time=" + this.server.timeOfDay() + ")");
      if (this.packName != null) {
         this.server.setPackName(this.packName);
      }
      String pack = this.server.packName();
      if (pack != null && !pack.isEmpty()) {
         java.io.File packDir = new java.io.File("pack");
         packDir.mkdirs();
         String zipName = pack.toLowerCase().endsWith(".zip") ? pack : pack + ".zip";
         java.io.File zip = new java.io.File(packDir, new java.io.File(zipName).getName());
         if (zip.isFile()) {
            java.util.Map<String, java.awt.image.BufferedImage> tiles =
               com.strata.blocks.TexturePack.loadPack(zip);
            com.strata.blocks.AtlasStitcher.setPack(tiles);
            Log.info("pack", "active " + zip.getPath() + " (" + tiles.size() + " overrides)");
         } else {
            Log.warn("pack", "missing " + zip.getPath() + ", built-ins stand in");
         }
      }
      if (this.importImage != null) {
         if (com.strata.world.WorldMeta.fileFor(this.worldDir).isFile()) {
            Log.error("boot", "--import needs a fresh world dir (world.dat exists in " + this.worldDir + ")");
            javax.swing.JOptionPane.showMessageDialog(null,
               "Cannot --import into " + this.worldDir + ":\nworld.dat already exists.\nPick another --world dir or delete it.",
               "Import refused", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.exit(1);
         }
         java.awt.image.BufferedImage img =
            com.strata.server.WorldImporter.readImage(this.importImage);
         if (img == null) {
            String why = this.importImage.isFile() ? " (undecodable — convert to sRGB PNG/JPG)"
               : " (no such file — check pasted quotes/spaces in the --import arg)";
            Log.error("boot", "cannot read import image " + this.importImage + why);
            javax.swing.JOptionPane.showMessageDialog(null, "Cannot read " + this.importImage,
               "Import failed", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.exit(1);
         }
         com.strata.server.WorldImporter.importImage(this.server, img);
         this.server.save();
      }
       this.level = new Level(64, false, this.server.level().generator());
      BulkTiles bulk = this.server.snapshot();
      this.level.applyBulk(bulk.columns);
      this.level.seedEmitters();
      mark("mirror synced (" + bulk.columns.size() + " edited columns)");
      this.lastSub = DayCycle.subFor(dayAmount(this.clientTime));
      this.level.setSkylightSub(this.lastSub);
      this.player = new Player(this.level);
      this.player.yRot = this.server.player().yRot;
      this.player.xRot = this.server.player().xRot;
      float[] spawn = this.server.spawnPoint();
      if (spawn != null) {
         this.player.teleport(spawn[0], spawn[1], spawn[2]);
         this.player.yRot = 0.0F;
         this.player.xRot = 30.0F;
      }
      this.levelRenderer = new LevelRenderer(this.level);
      mark("renderer built (spawn chunks meshed+uploaded)");
      this.particleEngine = new ParticleEngine(this.level);
      this.itemRenderer = new ItemRenderer(this.level);
      this.fallingRenderer = new FallingRenderer(this.level);
      InventorySync stock = this.server.inventoryState();
      this.inventory.applySync(stock.blocks, stock.counts);
      mark("stock synced");
      this.updateSelectedTile();
      Mouse.setCursorPosition(this.width / 2, this.height / 2);
      Mouse.setGrabbed(true);
      mark("init done, entering loop");
   }

    private void updateTitle() {
      Display.setTitle("Strata"
         + (this.lastSpectator ? " [SPECTATOR]" : "")
         + (this.fullBright ? " [FULLBRIGHT]" : "")
         + (this.timer.timeScale != 1.0F ? " [" + this.timer.timeScale + "x]" : ""));
   }

   private String[] debugLines() {
      long t = this.clientTime % DAY_LENGTH;
      if (t < 0) {
         t += DAY_LENGTH;
      }
      String phase = DayCycle.STOP_NAMES[(int)(t * 4.0 / DAY_LENGTH + 0.5) % 4];
      return new String[]{
         "Strata F3",
         "fps " + this.fps,
         String.format("xyz %.1f / %.1f / %.1f", this.player.x, this.player.y, this.player.z),
         phase + " t" + this.clientTime + " sub " + this.lastSub,
         this.levelRenderer.census() + " meshq=" + this.levelRenderer.meshQueueDepth(),
         "hp " + this.player.hp + " radius " + Config.VIEW_RADIUS,
      };
   }

   private void updateSelectedTile() {
      if (this.selectedSlot >= 0 && this.selectedSlot < Inventory.HOTBAR) {
         this.paintTile = this.inventory.slots[this.selectedSlot].blockId;
      } else {
         this.paintTile = 0;
      }
   }

   private void setInventoryOpen(boolean open) {
      if (this.inventoryOpen == open) {
         return;
      }
      this.inventoryOpen = open;
      if (open) {
         this.mouseReleased = true;
         this.leftDown = false;
         if (Mouse.isGrabbed()) {
            Mouse.setGrabbed(false);
         }
         Log.info("debug", "inventory open");
      } else {
         this.mouseReleased = false;
         Log.info("debug", "inventory closed");
      }
   }

   public void destroy() {
      this.server.save();
      this.options.save(new File("options.txt"));
      Mouse.destroy();
      Keyboard.destroy();
      Display.destroy();
   }

   @Override
   public void run() {
      try {
         this.init();
       } catch (Exception e) {
          Log.error("boot", "failed to start", e);
            JOptionPane.showMessageDialog(null, e.toString(), "Failed to start Strata", JOptionPane.ERROR_MESSAGE);
          System.exit(1);
       }

       long lastTime = System.currentTimeMillis();
       int frames = 0;
       int dbgCountdown = Config.DBG_STATUS_EVERY;

       try {
          while (!Keyboard.isKeyDown(this.options.keyQuit)
             && !Keyboard.isKeyDown(Keyboard.KEY_BACK)
             && !Display.isCloseRequested()) {
             boolean active = Display.isActive();
             if (active != this.lastActive) {
                this.lastActive = active;
                Log.info("win", active ? "active (mouse grabbed)" : "inactive (mouse released)");
             }
             if (active) {
                if (!this.mouseReleased && !Mouse.isGrabbed()) {
                   Mouse.setCursorPosition(this.width / 2, this.height / 2);
                   Mouse.setGrabbed(true);
                   while (Mouse.next()) {
                   }
                   this.leftDown = Mouse.isButtonDown(0);
                   Mouse.getDX();
                   Mouse.getDY();
                }
             } else if (Mouse.isGrabbed()) {
                Mouse.setGrabbed(false);
             }
             long frameStart = System.nanoTime();
             this.timer.advanceTime();

             for (int i = 0; i < this.timer.ticks; i++) {
                this.conn.sendToServer(this.gatherInput());
                this.server.tick();
                this.pumpServerPackets();
                this.tick();
             }

             this.render(this.timer.alpha);
             frames++;
             Profiler.endFrame();

             long frameMs = (System.nanoTime() - frameStart) / 1000000L;
             if (frameMs > Config.SLOW_FRAME_MS) {
                Log.warn("frame", "slow frame " + frameMs + "ms");
                Debug.dumpRecent();
             }
             if (--dbgCountdown <= 0) {
                dbgCountdown = Config.DBG_STATUS_EVERY;
                Debug.statusLine();
                Log.info("dbg", this.levelRenderer.census()
                   + " meshq=" + this.levelRenderer.meshQueueDepth());
             }

            while (System.currentTimeMillis() >= lastTime + 1000L) {
                Log.raw(frames + " fps, " + Chunk.updates + " visible=" + Display.isVisible() + " active=" + Display.isActive());
               Chunk.updates = 0;
               Chunk.uploadNs = 0L;
               lastTime += 1000L;
               this.fps = frames;
               frames = 0;
            }
         }
       } catch (Exception e) {
          Log.error("run", "main loop crashed", e);
       } finally {
         this.destroy();
      }
   }

   private InputState gatherInput() {
      InputState in = new InputState();
      in.yaw = this.player.yRot;
      in.pitch = this.player.xRot;
      if (this.inventoryOpen) {
         return in;
      }
      in.fwd = Keyboard.isKeyDown(200) || Keyboard.isKeyDown(this.options.keyFwd);
      in.back = Keyboard.isKeyDown(208) || Keyboard.isKeyDown(this.options.keyBack);
      in.left = Keyboard.isKeyDown(203) || Keyboard.isKeyDown(this.options.keyLeft);
      in.right = Keyboard.isKeyDown(205) || Keyboard.isKeyDown(this.options.keyRight);
      in.jump = Keyboard.isKeyDown(this.options.keyJump) || Keyboard.isKeyDown(219);
      in.reset = Keyboard.isKeyDown(this.options.keyRespawn);
      in.up = Keyboard.isKeyDown(this.options.keyJump);
      in.down = Keyboard.isKeyDown(this.options.keyDown) || Keyboard.isKeyDown(54);
      in.breaking = this.leftDown && this.hitResult != null;
      if (in.breaking) {
         in.breakX = this.hitResult.x;
         in.breakY = this.hitResult.y;
         in.breakZ = this.hitResult.z;
         in.breakFace = this.hitResult.f;
      }
      return in;
   }

   private void pumpServerPackets() {
      Packet p;
      while ((p = this.conn.pollClient()) != null) {
         if (p instanceof TileUpdate u) {
             this.level.setTile(u.x, u.y, u.z, u.type);
         } else if (p instanceof PlayerState s) {
             this.player.applyState(s.x, s.y, s.z, s.onGround, s.spectator, s.hp);
            this.destroyProgress = s.breakProgress;
            if (s.spectator != this.lastSpectator) {
               this.lastSpectator = s.spectator;
               this.updateTitle();
                Log.info("debug", "spectator " + (s.spectator ? "ON (WASD fly, Space up, Shift down)" : "OFF"));
            }
         } else if (p instanceof TimeUpdate) {
            this.clientTime = ((TimeUpdate)p).time;
         } else if (p instanceof BreakEffect e) {
             this.particleEngine.addBlockDestroyParticles(e.x, e.y, e.z, e.texIndex);
         } else if (p instanceof ItemSpawn s) {
             this.itemRenderer.spawn(s.entityId, s.x, s.y, s.z, s.xd, s.yd, s.zd, s.blockId);
         } else if (p instanceof FallingSpawn s) {
             this.fallingRenderer.spawn(s.entityId, s.blockId, s.x, s.y, s.z);
         } else if (p instanceof ItemRemove) {
            this.itemRenderer.remove(((ItemRemove)p).entityId);
            this.fallingRenderer.remove(((ItemRemove)p).entityId);
         } else if (p instanceof InventorySync s) {
             this.inventory.applySync(s.blocks, s.counts);
            this.held.blockId = s.heldBlock;
            this.held.count = s.heldCount;
            if (this.held.count <= 0) {
               this.held.clear();
            }
            this.updateSelectedTile();
         }
      }
   }

   public void tick() {
      this.particleEngine.tick();
      this.itemRenderer.tick(this.player.x, this.player.y, this.player.z);
      this.fallingRenderer.tick();

      if (this.leftDown && this.hitResult != null) {
         int tileId = this.level.getTile(this.hitResult.x, this.hitResult.y, this.hitResult.z);
         if (tileId > 0) {
             this.particleEngine.addBlockHitParticles(this.hitResult.x, this.hitResult.y, this.hitResult.z, this.hitResult.f, Blocks.particleTile(tileId));
         }
      }
      this.tickAmbient();
   }

   private void tickAmbient() {
      float fallSpeed = this.prevTickY - this.player.y;
      if (!this.prevTickGround && this.player.onGround && fallSpeed > 0.15F) {
         this.particleEngine.addPuff(this.player.x, this.player.bb.y0, this.player.z,
            Blocks.particleTile(Blocks.DIRT_ID));
      }
      this.prevTickY = this.player.y;
      this.prevTickGround = this.player.onGround;
      if (++this.ambientClock < 8) {
         return;
      }
      this.ambientClock = 0;
      java.util.ArrayList<int[]> embers =
         this.level.emittersNear(this.player.x, this.player.y, this.player.z, 12.0F, 32);
      java.util.Collections.shuffle(embers);
      int lit = 0;
      for (int i = 0; i < embers.size() && lit < 2; i++) {
         if (Math.random() > 0.5) {
            continue;
         }
         int[] e = embers.get(i);
         if (this.level.getTile(e[0], e[1], e[2]) == Blocks.TORCH_ID) {
            this.particleEngine.addEmber(e[0] + 0.5F, e[1] + 0.7F, e[2] + 0.5F);
            this.particleEngine.addSmoke(e[0] + 0.5F, e[1] + 0.7F, e[2] + 0.5F);
            lit++;
         }
      }
      java.util.ArrayList<int[]> surfaces = new java.util.ArrayList<>();
      int px = MathHelper.floor(this.player.x);
      int py = MathHelper.floor(this.player.bb.y0);
      int pz = MathHelper.floor(this.player.z);
      for (int x = px - 6; x <= px + 6; x++) {
         for (int z = pz - 6; z <= pz + 6; z++) {
            for (int y = py - 3; y <= py + 3; y++) {
               if (this.level.getTile(x, y, z) == Blocks.LAVA_ID
                  && this.level.getTile(x, y + 1, z) == 0) {
                  surfaces.add(new int[]{x, y, z});
               }
            }
         }
      }
      java.util.Collections.shuffle(surfaces);
      int bubbles = 0;
      for (int i = 0; i < surfaces.size() && bubbles < 2; i++) {
         if (Math.random() > 0.01) {
            continue;
         }
         int[] c = surfaces.get(i);
         this.particleEngine.addBubble(c[0] + 0.5F, c[1] + 1.0F, c[2] + 0.5F);
         bubbles++;
      }
   }

   private void moveCameraToPlayer(float a) {
      GL11.glTranslatef(0.0F, 0.0F, -0.3F);
      GL11.glRotatef(this.player.xRot, 1.0F, 0.0F, 0.0F);
      GL11.glRotatef(this.player.yRot, 0.0F, 1.0F, 0.0F);
      float x = this.player.xo + (this.player.x - this.player.xo) * a;
      float y = this.player.yo + (this.player.y - this.player.yo) * a;
      float z = this.player.zo + (this.player.z - this.player.zo) * a;
      GL11.glTranslatef(-x, -y, -z);
   }

   private void setupCamera(float a) {
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glLoadIdentity();
      GLU.gluPerspective(70.0F, (float)this.width / this.height, 0.05F, 1000.0F);
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glLoadIdentity();
      this.moveCameraToPlayer(a);
   }

   private void pick(float a) {
      float ex = this.player.xo + (this.player.x - this.player.xo) * a;
      float ey = this.player.yo + (this.player.y - this.player.yo) * a;
      float ez = this.player.zo + (this.player.z - this.player.zo) * a;
      this.hitResult = Raycaster.pick(this.level, ex, ey, ez, this.player.yRot, this.player.xRot, 5.0);
   }

   public void render(float a) throws IOException {
      Textures.animateAtlas();
      if (Display.wasResized()) {
         int w = Display.getWidth();
         int h = Display.getHeight();
         if (w > 0 && h > 0 && (w != this.width || h != this.height)) {
            this.width = w;
            this.height = h;
            GL11.glViewport(0, 0, this.width, this.height);
            Log.info("win", "resized to " + this.width + "x" + this.height);
         }
      }
      long r0 = System.nanoTime();
      float xo = Mouse.getDX() * this.options.sensitivity;
      float yo = Mouse.getDY() * this.options.sensitivity;
      if (!this.inventoryOpen) {
         this.player.turn(xo, yo);
      }
      this.pick(a);
      long r1 = System.nanoTime();

      int dWheel = Mouse.getDWheel();
      if (dWheel != 0) {
         if (dWheel > 0) {
            this.selectedSlot = (this.selectedSlot - 1 + 9) % 9;
         } else {
            this.selectedSlot = (this.selectedSlot + 1) % 9;
         }
         this.updateSelectedTile();
      }

      while (Mouse.next()) {
          if (this.inventoryOpen) {
             if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
                int gs = Gui.resolveScale(this.options.guiScale, this.width, this.height);
                int slot = this.gui.slotAt(Mouse.getX() / gs, (this.height - Mouse.getY()) / gs);
               if (slot >= 0) {
                  SlotClick click = new SlotClick();
                  click.slot = slot;
                  this.conn.sendToServer(click);
               }
            }
            continue;
         }
         if (this.mouseReleased) {
            if (Mouse.getEventButtonState()) {
               this.mouseReleased = false;
            }
            continue;
         }
         if (Mouse.getEventButton() == 0) {
            this.leftDown = Mouse.getEventButtonState();
         }
          if (Mouse.getEventButton() == 1 && Mouse.getEventButtonState() && this.hitResult != null && this.paintTile > 0) {
            PlaceBlock place = new PlaceBlock();
            place.x = this.hitResult.x;
            place.y = this.hitResult.y;
            place.z = this.hitResult.z;
            place.face = this.hitResult.f;
            place.blockId = this.paintTile;
            this.conn.sendToServer(place);
         }
      }

      while (Keyboard.next()) {
         if (Keyboard.getEventKeyState()) {
            if (Keyboard.getEventKey() >= Keyboard.KEY_1 && Keyboard.getEventKey() <= Keyboard.KEY_9) {
               this.selectedSlot = Keyboard.getEventKey() - Keyboard.KEY_1;
               this.updateSelectedTile();
            }

            if (Keyboard.getEventKey() == this.options.keySave) {
               this.conn.sendToServer(new SaveGame());
            }

            if (Keyboard.getEventKey() == this.options.keySpectator) {
               this.conn.sendToServer(new SpectateToggle());
            }

            if (Keyboard.getEventKey() == this.options.keyRelease) {
               if (this.inventoryOpen) {
                  this.setInventoryOpen(false);
               }
               this.mouseReleased = true;
               this.leftDown = false;
               if (Mouse.isGrabbed()) {
                  Mouse.setGrabbed(false);
               }
               Log.info("win", "mouse released; click back in to grab");
            }

            if (Keyboard.getEventKey() == this.options.keyInventory) {
               this.setInventoryOpen(!this.inventoryOpen);
            }

            if (Keyboard.getEventKey() == this.options.keyGive) {
               this.conn.sendToServer(new DebugGive());
            }

            if (Keyboard.getEventKey() == this.options.keyFullbright) {
               this.fullBright = !this.fullBright;
               this.updateTitle();
               Log.info("debug", "fullbright " + (this.fullBright ? "ON" : "OFF"));
            }

            if (Keyboard.getEventKey() == Keyboard.KEY_F3) {
               this.showDebug = !this.showDebug;
               Log.info("debug", "overlay " + (this.showDebug ? "ON" : "OFF"));
            }

            if (Keyboard.getEventKey() == Keyboard.KEY_LBRACKET) {
               Config.VIEW_RADIUS = Math.max(2, Config.VIEW_RADIUS - 1);
               Log.info("debug", "render distance " + Config.VIEW_RADIUS);
            }

            if (Keyboard.getEventKey() == Keyboard.KEY_RBRACKET) {
               Config.VIEW_RADIUS = Math.min(10, Config.VIEW_RADIUS + 1);
               Log.info("debug", "render distance " + Config.VIEW_RADIUS);
            }

            if (Keyboard.getEventKey() == Keyboard.KEY_SEMICOLON) {
               this.conn.sendToServer(new TimeCycle());
            }

            if (Keyboard.getEventKey() == Keyboard.KEY_SLASH) {
               this.conn.sendToServer(new HurtSelf());
            }

            if (Keyboard.getEventKey() == Keyboard.KEY_COMMA) {
               this.timer.timeScale = Math.max(0.125F, this.timer.timeScale / 2.0F);
               this.updateTitle();
               Log.info("debug", "tick rate " + this.timer.timeScale + "x");
            }

            if (Keyboard.getEventKey() == Keyboard.KEY_PERIOD) {
               this.timer.timeScale = Math.min(8.0F, this.timer.timeScale * 2.0F);
               this.updateTitle();
               Log.info("debug", "tick rate " + this.timer.timeScale + "x");
            }
         }
      }

      float day = dayAmount(this.clientTime);
      int sub = DayCycle.subFor(day);
      if (sub != this.lastSub) {
         this.lastSub = sub;
         this.level.setSkylightSub(sub);
         this.levelRenderer.markAllDirty();
         Log.info("light", "skylight sub " + sub + " (day=" + String.format("%.2f", day) + "), remesh wave");
      }
      GL11.glClearColor(0.01F + (0.5F - 0.01F) * day, 0.02F + (0.8F - 0.02F) * day, 0.06F + (1.0F - 0.06F) * day, 0.0F);
      float fogScale = 0.15F + 0.85F * day;
      ((Buffer)this.fogColor).clear();
      this.fogColor.put(new float[]{0.0549F * fogScale, 0.0431F * fogScale, 0.0392F * fogScale, 1.0F});
      ((Buffer)this.fogColor).flip();
      GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
      this.setupCamera(a);
      GL11.glEnable(GL11.GL_CULL_FACE);
      GL11.glEnable(GL11.GL_FOG);
      GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
       GL11.glFogf(GL11.GL_FOG_DENSITY, Config.FOG_DENSITY);
      GL11.glFog(GL11.GL_FOG_COLOR, this.fogColor);
      GL11.glDisable(GL11.GL_FOG);

      this.levelRenderer.render(this.player.x, this.player.z, this.fullBright, 0);
      if (!this.fullBright) {
         GL11.glEnable(GL11.GL_FOG);
      }
      this.levelRenderer.render(this.player.x, this.player.z, this.fullBright, 1);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      long r2 = System.nanoTime();

      if (this.hitResult != null) {
         this.levelRenderer.renderHit(this.hitResult, this.destroyProgress);
      }
      long rHit = System.nanoTime();

      this.particleEngine.render(this.player, a, 0);
      this.itemRenderer.render(this.player, a);
      this.fallingRenderer.render();
      long rPart = System.nanoTime();

      GL11.glDisable(GL11.GL_FOG);

      long rOver = System.nanoTime();

      int guiScale = Gui.resolveScale(this.options.guiScale, this.width, this.height);
      this.gui.render(this.width, this.height, this.inventory, this.selectedSlot, guiScale, this.player.hp, this.player.spectator,
         this.showDebug ? this.debugLines() : null);
      if (this.inventoryOpen) {
         this.gui.renderInventory(this.inventory, this.held,
            Mouse.getX() / guiScale, (this.height - Mouse.getY()) / guiScale);
      }
      long rGui = System.nanoTime();

      Display.update();
      long r3 = System.nanoTime();
      this.perfTotalNs += r3 - r0;
      this.perfPickNs += r1 - r0;
      this.perfWorldNs += r2 - r1;
      this.perfHudNs += r3 - r2;
      this.perfHitNs += rHit - r2;
      this.perfPartNs += rPart - rHit;
      this.perfOverNs += rOver - rPart;
      this.perfGuiNs += rGui - rOver;
      this.perfSwapNs += r3 - rGui;
      if (r3 - r0 > this.perfMaxNs) {
         this.perfMaxNs = r3 - r0;
      }
      if (++this.perfSamples >= PERF_SAMPLES) {
         this.printPerf();
      }
   }

   private void printPerf() {
      double n = this.perfSamples;
      long heapUsedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576L;
      long gcCount = 0L;
      long gcMs = 0L;
      try {
         for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += gc.getCollectionCount();
            gcMs += gc.getCollectionTime();
         }
      } catch (Throwable t) {
      }
       Log.raw(String.format("[perf] frame avg %.1fms max %.1f (pick %.1f, world %.1f, hit %.1f, part %.1f, over %.1f, gui %.1f, swap %.1f) | chunks %d+%d verts %dk+%dk | upload avg %.1fms x%d submit %d | heap %dMB gc +%dcoll/+%dms | meshq %d @%d,%d",
         this.perfTotalNs / n / 1000000.0, this.perfMaxNs / 1000000.0,
         this.perfPickNs / n / 1000000.0, this.perfWorldNs / n / 1000000.0,
         this.perfHitNs / n / 1000000.0, this.perfPartNs / n / 1000000.0, this.perfOverNs / n / 1000000.0,
         this.perfGuiNs / n / 1000000.0, this.perfSwapNs / n / 1000000.0,
         this.levelRenderer.drawn0, this.levelRenderer.drawn1,
         this.levelRenderer.verts0 / 1000, this.levelRenderer.verts1 / 1000,
         Chunk.updates > 0 ? Chunk.uploadNs / (double)Chunk.updates / 1000000.0 : 0.0, Chunk.updates, this.levelRenderer.submitted,
         heapUsedMb, gcCount - this.perfGcCount, gcMs - this.perfGcMs,
         this.levelRenderer.meshQueueDepth(),
         (int)this.player.x, (int)this.player.z));
      this.perfTotalNs = 0L;
      this.perfPickNs = 0L;
      this.perfWorldNs = 0L;
      this.perfHudNs = 0L;
      this.perfHitNs = 0L;
      this.perfPartNs = 0L;
      this.perfOverNs = 0L;
      this.perfGuiNs = 0L;
      this.perfSwapNs = 0L;
      this.perfMaxNs = 0L;
      this.perfSamples = 0;
      this.perfGcCount = gcCount;
      this.perfGcMs = gcMs;
      this.levelRenderer.drawn0 = 0;
      this.levelRenderer.drawn1 = 0;
      this.levelRenderer.verts0 = 0;
      this.levelRenderer.verts1 = 0;
      this.levelRenderer.submitted = 0;
   }
}

