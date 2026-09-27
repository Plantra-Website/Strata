package com.strata.server;

import com.strata.HitResult;
import com.strata.blocks.Block;
import com.strata.blocks.Blocks;
import com.strata.core.AABB;
import com.strata.core.Config;
import com.strata.core.DayCycle;
import com.strata.core.Log;
import com.strata.core.Profiler;
import com.strata.core.Rng;
import com.strata.world.Level;
import com.strata.world.LevelListener;
import com.strata.world.PlayerData;
import com.strata.world.WorldMeta;
import com.strata.world.gen.TerrainGenerator;
import com.strata.net.BreakEffect;
import com.strata.net.BulkTiles;
import com.strata.net.DebugGive;
import com.strata.net.FallingSpawn;
import com.strata.net.HurtSelf;
import com.strata.net.InputState;
import com.strata.net.InventorySync;
import com.strata.net.ItemRemove;
import com.strata.net.ItemSpawn;
import com.strata.net.SlotClick;
import com.strata.net.LocalConnection;
import com.strata.net.Packet;
import com.strata.net.PlaceBlock;
import com.strata.net.PlayerState;
import com.strata.net.SaveGame;
import com.strata.net.SpectateToggle;
import com.strata.net.TileUpdate;
import com.strata.net.TimeCycle;
import com.strata.net.TimeUpdate;

public class GameServer implements LevelListener {
   private final Level level;
   private final Player player;
   private final LocalConnection conn;
   private final java.io.File worldDir;
   private final long seed;
   private final boolean voidWorld;
   private String packName;
   private final Inventory inventory = new Inventory();
   private final java.util.ArrayList<ItemEntity> items = new java.util.ArrayList<>();
   private final java.util.ArrayList<FallingBlock> falling = new java.util.ArrayList<>();
   private int nextEntityId = 1;
   private final ItemStack held = new ItemStack();
   private long timeOfDay = 0;
   private float destroyProgress = 0.0F;
   private HitResult prevHitResult = null;
   private InputState lastInput = new InputState();
   private int autosaveClock = 0;

    public GameServer(LocalConnection conn) {
       this(conn, new java.io.File("."), null);
    }

    public GameServer(LocalConnection conn, java.io.File worldDir, Long seedOverride) {
       this(conn, worldDir, seedOverride, false);
    }

    public GameServer(LocalConnection conn, java.io.File worldDir, Long seedOverride, boolean forceVoid) {
       this.conn = conn;
       this.worldDir = worldDir;
       boolean freshWorld = !WorldMeta.fileFor(worldDir).isFile();
       WorldMeta meta = WorldMeta.load(worldDir);
       long seed = seedOverride != null ? seedOverride : meta.seed;
       this.seed = seed;
       boolean voidWorld = forceVoid || meta.voidWorld;
       this.voidWorld = voidWorld;
       this.packName = meta.pack == null ? "" : meta.pack;
       Rng.reseed(seed);
       this.level = new Level(64, true, new TerrainGenerator(seed, 64, voidWorld), worldDir);
       this.level.loadAllRegions();
       this.level.seedEmitters();
       this.timeOfDay = seedOverride != null && seedOverride != meta.seed ? 0L : meta.time;
       PlayerData resume = PlayerData.load(this.worldDir);
       if (resume.present) {
          this.inventory.applySync(resume.blocks, resume.counts);
          this.held.blockId = resume.heldBlock;
          this.held.count = resume.heldCount;
          if (this.held.count <= 0) {
             this.held.clear();
          }
       } else if (freshWorld) {
          this.inventory.add(Blocks.TORCH_ID, 8);
       }
       if (meta.hasSpawn) {
          this.level.setSpawnPoint(meta.spawnX, meta.spawnY, meta.spawnZ);
       }
       this.player = new Player(this.level);
       if (resume.present) {
          this.player.teleport(resume.x, resume.y, resume.z);
          this.player.yRot = resume.yaw;
          this.player.xRot = resume.pitch;
          this.player.hp = resume.hp;
       }
       this.level.addListener(this);
    }

   public Player player() {
       return this.player;
    }

    public long seed() {
       return this.seed;
    }

    public long timeOfDay() {
       return this.timeOfDay;
    }

    public float[] spawnPoint() {
       return this.level.spawnPoint();
    }

    public void setPackName(String name) {
       if (name == null) {
          this.packName = "";
          return;
       }
       String file = name;
       int slash = Math.max(file.lastIndexOf('/'), file.lastIndexOf(java.io.File.separatorChar));
       if (slash >= 0) {
          file = file.substring(slash + 1);
       }
       if (file.toLowerCase().endsWith(".zip")) {
          file = file.substring(0, file.length() - 4);
       }
       this.packName = file;
    }

    public String packName() {
       return this.packName;
    }

    public Level level() {
       return this.level;
    }

   public BulkTiles snapshot() {
      BulkTiles bulk = new BulkTiles();
      bulk.columns = this.level.snapshotEdits();
      return bulk;
   }

   public void save() {
      this.level.save();
      int[] blocks = new int[Inventory.SLOTS];
      int[] counts = new int[Inventory.SLOTS];
      for (int i = 0; i < Inventory.SLOTS; i++) {
         blocks[i] = this.inventory.slots[i].blockId;
         counts[i] = this.inventory.slots[i].count;
      }
      WorldMeta.save(this.worldDir, this.seed, this.timeOfDay,
         this.voidWorld, this.level.spawnPoint(), this.packName);
      PlayerData.save(this.worldDir, this.player.x, this.player.y, this.player.z,
         this.player.yRot, this.player.xRot, this.player.hp,
         blocks, counts, this.held.blockId, this.held.count);
   }

   int fallingCount() {
      return this.falling.size();
   }

   FallingBlock falling(int i) {
      return this.falling.get(i);
   }

   public InventorySync inventoryState() {      InventorySync s = new InventorySync();
      for (int i = 0; i < Inventory.SLOTS; i++) {
         s.blocks[i] = this.inventory.slots[i].blockId;
         s.counts[i] = this.inventory.slots[i].count;
      }
      s.heldBlock = this.held.blockId;
      s.heldCount = this.held.count;
      return s;
   }

   private void sendInv() {
      this.conn.sendToClient(this.inventoryState());
   }

   public void tick() {
      Packet p;
      while ((p = this.conn.pollServer()) != null) {
         if (p instanceof InputState) {
            this.lastInput = (InputState)p;
         } else if (p instanceof PlaceBlock) {
            this.handlePlace((PlaceBlock)p);
         } else if (p instanceof SaveGame) {
            this.save();
         } else if (p instanceof DebugGive) {
            this.inventory.clear();
            this.held.clear();
            int[] kit = {Blocks.DIRT_ID, Blocks.STONE_ID, Blocks.COBBLE_ID,
               Blocks.TORCH_ID, Blocks.LEAF_ID, Blocks.SAND_ID, Blocks.PLANKS_ID};
            for (int i = 0; i < kit.length; i++) {
               this.inventory.slots[i] = new ItemStack(kit[i], ItemStack.MAX);
            }
            this.sendInv();
            Log.info("debug", "gave builder kit (hotbar replaced: dirt/stone/cobble/torch/leaves/sand/planks x64)");
          } else if (p instanceof SlotClick) {
             this.inventory.applyClick(((SlotClick)p).slot, this.held);
             this.sendInv();
          } else if (p instanceof HurtSelf) {
             this.player.hurt(1);
             Log.info("debug", "self-hurt (hp=" + this.player.hp + ")");
          } else if (p instanceof SpectateToggle) {
             this.player.spectator = !this.player.spectator;
             if (!this.player.spectator) {
                this.player.evict();
             }
          } else if (p instanceof TimeCycle) {
             this.timeOfDay = DayCycle.nextStop(this.timeOfDay, Config.DAY_LENGTH);
             Log.info("debug", "time cycled to " + DayCycle.stopName(this.timeOfDay, Config.DAY_LENGTH));
          }
      }

       this.timeOfDay++;
       Profiler.push("sim");
       this.player.tick(this.lastInput);
       if (this.player.hp <= 0) {
          Log.info("game", "player died; respawning");
          this.player.respawn();
       }
      this.tickBreaking(this.lastInput);
      this.tickItems();
      this.tickFallings();
       this.level.tickBlockEntities();
       this.level.tickScheduled();
       Profiler.pop();

      PlayerState state = new PlayerState();
      state.x = this.player.x;
      state.y = this.player.y;
      state.z = this.player.z;
      state.yaw = this.player.yRot;
      state.pitch = this.player.xRot;
      state.onGround = this.player.onGround;
      state.spectator = this.player.spectator;
      state.breakProgress = this.destroyProgress;
      state.hp = this.player.hp;
      this.conn.sendToClient(state);

      TimeUpdate time = new TimeUpdate();
      time.time = this.timeOfDay;
      this.conn.sendToClient(time);

      if (++this.autosaveClock >= Config.AUTOSAVE_TICKS) {
         this.autosaveClock = 0;
         this.save();
         Log.info("world", "autosaved (t=" + this.timeOfDay + ")");
      }
   }

   private void tickBreaking(InputState in) {
      HitResult hit = null;
      if (in.breaking) {
         hit = new HitResult(in.breakX, in.breakY, in.breakZ, in.breakFace);
      }
      if (hit != null) {
         if (this.prevHitResult == null || this.prevHitResult.x != hit.x || this.prevHitResult.y != hit.y || this.prevHitResult.z != hit.z) {
            this.destroyProgress = 0.0F;
            this.prevHitResult = hit;
         }

         int tileId = this.level.getTile(hit.x, hit.y, hit.z);
         if (tileId > 0 && Blocks.hardness(tileId) > 0) {
            this.destroyProgress += 1.0F / Blocks.hardness(tileId);

            if (this.destroyProgress >= 1.0F) {
               int brokenId = tileId;
               this.breakBlock(hit.x, hit.y, hit.z, brokenId);
               this.settleAbove(hit.x, hit.y, hit.z);
               this.popUnsupported(hit.x, hit.y + 1, hit.z);
               this.wakeLava(hit.x, hit.y, hit.z);
               this.destroyProgress = 0.0F;
               this.prevHitResult = null;
            }
         }
      } else {
         this.destroyProgress = 0.0F;
         this.prevHitResult = null;
      }
   }

   private void breakBlock(int x, int y, int z, int tileId) {
      this.level.setTile(x, y, z, 0);
      BreakEffect effect = new BreakEffect();
      effect.x = x;
      effect.y = y;
      effect.z = z;
      effect.texIndex = Blocks.particleTile(tileId);
      this.conn.sendToClient(effect);
      this.spawnDrop(x, y, z, tileId);
   }

   private void popUnsupported(int x, int y, int z) {
      while (y < this.level.depth) {
         int id = this.level.getTile(x, y, z);
         Block b = id > 0 ? Blocks.byId(id) : null;
         if (b != null && b.needsSupport() && !this.level.isSolidTile(x, y - 1, z)) {
            this.breakBlock(x, y, z, id);
            this.settleAbove(x, y, z);
         }
         y++;
      }
   }

   private void wakeLava(int x, int y, int z) {
      if (this.level.getTile(x + 1, y, z) == Blocks.LAVA_ID) {
         this.level.scheduleTick(x + 1, y, z, Level.LAVA_TICKS);
      }
      if (this.level.getTile(x - 1, y, z) == Blocks.LAVA_ID) {
         this.level.scheduleTick(x - 1, y, z, Level.LAVA_TICKS);
      }
      if (this.level.getTile(x, y + 1, z) == Blocks.LAVA_ID) {
         this.level.scheduleTick(x, y + 1, z, Level.LAVA_TICKS);
      }
      if (this.level.getTile(x, y - 1, z) == Blocks.LAVA_ID) {
         this.level.scheduleTick(x, y - 1, z, Level.LAVA_TICKS);
      }
      if (this.level.getTile(x, y, z + 1) == Blocks.LAVA_ID) {
         this.level.scheduleTick(x, y, z + 1, Level.LAVA_TICKS);
      }
      if (this.level.getTile(x, y, z - 1) == Blocks.LAVA_ID) {
         this.level.scheduleTick(x, y, z - 1, Level.LAVA_TICKS);
      }
   }

   private void settleAbove(int x, int y, int z) {
      for (int yy = y; yy < this.level.depth; yy++) {
         int id = this.level.getTile(x, yy, z);
         if ((id == Blocks.SAND_ID || id == Blocks.GRAVEL_ID)
            && yy > 0 && this.level.getTile(x, yy - 1, z) == 0) {
            this.spawnFalling(x, yy, z, id);
         }
      }
   }

   private void spawnFalling(int x, int y, int z, int id) {
      this.level.setTile(x, y, z, 0);
      FallingBlock e = new FallingBlock(this.level, this.nextEntityId++, id, x + 0.5F, y + 0.5F, z + 0.5F);
      this.falling.add(e);
      FallingSpawn s = new FallingSpawn();
      s.entityId = e.id;
      s.blockId = e.blockId;
      s.x = e.x;
      s.y = e.y;
      s.z = e.z;
      this.conn.sendToClient(s);
   }

   private void tickFallings() {
      for (int i = 0; i < this.falling.size(); i++) {
         FallingBlock e = this.falling.get(i);
         e.tick();
         if (e.bb.y0 < 0.0F) {
            this.falling.remove(i--);
            this.sendFallRemove(e);
            this.spawnDrop(e.landX(), 0, e.landZ(), e.blockId);
            continue;
         }
         if (!e.onGround) {
            continue;
         }
         this.falling.remove(i--);
         this.sendFallRemove(e);
         int fx = e.landX(), fy = e.landY(), fz = e.landZ();
         int occupant = this.level.getTile(fx, fy, fz);
         if (occupant > 0) {
            Block b = Blocks.byId(occupant);
            if (b != null && b.needsSupport()) {
               this.breakBlock(fx, fy, fz, occupant);
            } else {
               this.spawnDrop(fx, fy, fz, e.blockId);
               continue;
            }
         }
         this.level.setTile(fx, fy, fz, e.blockId);
      }
   }

   private void sendFallRemove(FallingBlock e) {
      ItemRemove r = new ItemRemove();
      r.entityId = e.id;
      this.conn.sendToClient(r);
   }

   private void spawnDrop(int x, int y, int z, int brokenId) {
      int drop = Blocks.dropId(brokenId);
      if (drop <= 0) {
         return;
      }
      java.util.Random rng = Rng.world();
      ItemEntity e = new ItemEntity(this.nextEntityId++, drop,
         x + 0.5F, y + 0.5F, z + 0.5F,
         (rng.nextFloat() - 0.5F) * 0.4F, 0.35F, (rng.nextFloat() - 0.5F) * 0.4F);
      float br = this.level.getBrightness(x, y, z);
      e.r = br;
      e.g = br;
      e.b = br;
      this.items.add(e);
      ItemSpawn s = new ItemSpawn();
      s.entityId = e.id;
      s.x = e.x;
      s.y = e.y;
      s.z = e.z;
      s.xd = e.xd;
      s.yd = e.yd;
      s.zd = e.zd;
      s.blockId = e.blockId;
      this.conn.sendToClient(s);
   }

   private void tickItems() {
      for (int i = 0; i < this.items.size(); i++) {
         ItemEntity e = this.items.get(i);
         e.tick(this.level, this.player.x, this.player.y, this.player.z);
         if (e.expired()) {
            this.despawn(i--, e);
         } else if (e.pickupDelay <= 0 && e.near(this.player.x, this.player.y, this.player.z)) {
            if (this.inventory.add(e.blockId, 1)) {
               this.despawn(i--, e);
               this.sendInv();
            }
         }
      }
   }

   private void despawn(int i, ItemEntity e) {
      this.items.remove(i);
      ItemRemove r = new ItemRemove();
      r.entityId = e.id;
      this.conn.sendToClient(r);
   }

   private void handlePlace(PlaceBlock p) {
      int x = p.x;
      int y = p.y;
      int z = p.z;
      if (p.face == 0) y--;
      if (p.face == 1) y++;
      if (p.face == 2) z--;
      if (p.face == 3) z++;
      if (p.face == 4) x--;
      if (p.face == 5) x++;

      AABB blockAABB = new AABB(x, y, z, x + 1, y + 1, z + 1);
      if (blockAABB.intersects(this.player.bb)) {
         return;
      }
      Block placing = Blocks.byId(p.blockId);
      if (placing != null && placing.needsSupport() && !this.level.isSolidTile(x, y - 1, z)) {
         return;
      }
      boolean paid = false;
      for (int i = 0; i < Inventory.SLOTS; i++) {
         if (!this.inventory.slots[i].isEmpty() && this.inventory.slots[i].blockId == p.blockId) {
            paid = this.inventory.consume(i, 1) == 1;
            break;
         }
      }
      if (!paid) {
         return;
      }
      this.level.setTile(x, y, z, p.blockId);
      if (p.blockId == Blocks.SAPLING_ID) {
         this.level.scheduleTick(x, y, z, Level.SAPLING_TICKS);
      }
      this.settleAbove(x, y, z);
      this.popUnsupported(x, y + 1, z);
      this.wakeLava(x, y, z);
      this.sendInv();
   }

   @Override
   public void tileChanged(int x, int y, int z) {
      TileUpdate update = new TileUpdate();
      update.x = x;
      update.y = y;
      update.z = z;
      update.type = this.level.getTile(x, y, z);
      this.conn.sendToClient(update);
   }

   @Override
   public void lightColumnChanged(int x, int z, int y0, int y1) {
   }
}

