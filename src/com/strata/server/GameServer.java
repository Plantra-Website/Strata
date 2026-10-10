package com.strata.server;

import com.strata.HitResult;
import com.strata.blocks.Block;
import com.strata.blocks.Blocks;
import com.strata.blocks.Fluid;
import com.strata.blocks.TorchBlock;
import com.strata.core.AABB;
import com.strata.core.Config;
import com.strata.core.DayCycle;
import com.strata.core.Log;
import com.strata.core.MathHelper;
import com.strata.core.Profiler;
import com.strata.core.Rng;
import com.strata.world.Level;
import com.strata.world.LevelListener;
import com.strata.world.PlayerData;
import com.strata.world.WorldMeta;
import com.strata.world.gen.TerrainGenerator;
import com.strata.net.BreakEffect;
import com.strata.net.BulkTiles;
import com.strata.net.AttackMob;
import com.strata.net.DebugGive;
import com.strata.net.FallingSpawn;
import com.strata.net.HurtSelf;
import com.strata.net.InputState;
import com.strata.net.InventorySync;
import com.strata.net.ItemRemove;
import com.strata.net.ItemSpawn;
import com.strata.net.MobHurt;
import com.strata.net.MobSpawn;
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
   private final java.util.ArrayList<Zombie> mobs = new java.util.ArrayList<>();
   private int spawnClock = 0;
   private final ItemStack held = new ItemStack();
   private long timeOfDay = 0;
   private float destroyProgress = 0.0F;
   private HitResult prevHitResult = null;
   private int breakCooldown = 0;
   private InputState lastInput = new InputState();
   private int autosaveClock = 0;
   private int restlessClock = 0;

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
       this.level = new Level(Level.WORLD_DEPTH, true, new TerrainGenerator(seed, Level.WORLD_DEPTH, voidWorld), worldDir);
       this.level.setDropSink((x, y, z, blockId) -> this.spawnDrop(x, y, z, blockId));
       this.level.loadAllRegions();
       this.level.seedEmitters();
       this.level.reseedTicks();
       this.timeOfDay = seedOverride != null && seedOverride != meta.seed ? 0L : meta.time;
       PlayerData resume = PlayerData.load(this.worldDir);
       if (resume.present) {
          this.inventory.applySync(resume.blocks, resume.counts);
          this.held.blockId = resume.heldBlock;
          this.held.count = Math.min(resume.heldCount, ItemStack.MAX);
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
          this.player.teleport(resume.x, resume.y - Player.EYE_HEIGHT, resume.z);
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
      bulk.datas = this.level.snapshotData();
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
               Blocks.TORCH_ID, Blocks.LEAF_ID, Blocks.SAND_ID, Blocks.PLANKS_ID,
               Blocks.WATER_ID};
            for (int i = 0; i < kit.length; i++) {
               this.inventory.slots[i] = new ItemStack(kit[i], ItemStack.MAX);
            }
            int[] kitExtra = {Blocks.SANDSTONE_ID, Blocks.ICE_ID, Blocks.MYCELIUM_ID,
               Blocks.CACTUS_ID, Blocks.REED_ID, Blocks.DEADBUSH_ID,
               Blocks.MUSHROOM_BROWN_ID, Blocks.MUSHROOM_RED_ID, Blocks.CLAY_ID,
               Blocks.PUMPKIN_ID, Blocks.VINE_ID, Blocks.LILYPAD_ID,
               Blocks.SNOW_LAYER_ID, Blocks.SNOW_BLOCK_ID, Blocks.BIRCH_LOG_ID,
               Blocks.SPRUCE_LOG_ID, Blocks.BIRCH_LEAVES_ID, Blocks.SPRUCE_LEAVES_ID,
               Blocks.BIRCH_SAPLING_ID, Blocks.SPRUCE_SAPLING_ID, Blocks.MOSSY_COBBLE_ID,
               Blocks.REDSTONE_ORE_ID, Blocks.ROSE_ID, Blocks.DANDELION_ID,
               Blocks.TALL_GRASS_ID, Blocks.SAPLING_ID};
            int slot = Inventory.HOTBAR;
            for (int i = 0; i < kitExtra.length && slot < Inventory.SLOTS; i++) {
               this.inventory.slots[slot++] = new ItemStack(kitExtra[i], ItemStack.MAX);
            }
            this.sendInv();
            Log.info("debug", "gave builder kit (hotbar replaced: dirt/stone/cobble/torch/leaves/sand/planks/water x64)");
          } else if (p instanceof AttackMob) {
             this.handleAttack((AttackMob)p);
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
          this.destroyProgress = 0.0F;
          this.prevHitResult = null;
          this.breakCooldown = 0;
       }
      this.tickBreaking(this.lastInput);
      this.tickItems();
      this.tickFallings();
      this.tickMobs();
       this.level.tickBlockEntities();
       this.level.tickScheduled();
       for (long key : this.level.drainFluidSupport(256)) {
          int ex = (int)(key >> 38);
          if ((ex & 0x2000000) != 0) {
             ex |= ~0x3FFFFFF;
          }
          int ez = (int)((key >> 12) & 0x3FFFFFF);
          if ((ez & 0x2000000) != 0) {
             ez |= ~0x3FFFFFF;
          }
          int ey = (int)(key & 0xFFF);
          this.notifySupport(ex, ey, ez);
          this.popUnsupported(ex, ey, ez);
       }
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
      if (++this.restlessClock >= 15) {
         this.restlessClock = 0;
         int pcx = Math.floorDiv((int)Math.floor(this.player.x), 16);
         int pcz = Math.floorDiv((int)Math.floor(this.player.z), 16);
         this.level.wakeRestlessFluids(pcx - 2, pcz - 2, pcx + 2, pcz + 2);
      }
   }

    private void tickBreaking(InputState in) {
       HitResult hit = null;
       if (!this.player.spectator && in.breaking) {
          hit = new HitResult(in.breakX, in.breakY, in.breakZ, in.breakFace);
       }
      if (this.breakCooldown > 0) {
         this.breakCooldown--;
      }
      if (hit != null) {
         if (this.prevHitResult == null || this.prevHitResult.x != hit.x || this.prevHitResult.y != hit.y || this.prevHitResult.z != hit.z) {
            this.destroyProgress = 0.0F;
            this.prevHitResult = hit;
         }

         int tileId = this.level.getTile(hit.x, hit.y, hit.z);
         if (tileId > 0 && Blocks.hardness(tileId) > 0 && this.breakCooldown <= 0) {
            this.destroyProgress += 1.0F / Blocks.hardness(tileId);

             if (this.destroyProgress >= 1.0F) {
                int brokenId = tileId;
                this.breakBlock(hit.x, hit.y, hit.z, brokenId);
                this.notifySupport(hit.x, hit.y, hit.z);
                this.popUnsupported(hit.x, hit.y, hit.z);
               this.wakeFluid(hit.x, hit.y, hit.z);
               this.destroyProgress = 0.0F;
               this.prevHitResult = null;
               this.breakCooldown = 15;
            }
         }
      } else {
         this.destroyProgress = 0.0F;
         this.prevHitResult = null;
         this.breakCooldown = 0;
      }
   }

   private void breakBlock(int x, int y, int z, int tileId) {
      this.level.setTile(x, y, z, 0);
      if (tileId == Blocks.ICE_ID && y > 0) {
         int below = this.level.getTile(x, y - 1, z);
         if (below != 0 && (Blocks.isSolid(below) || Blocks.isFluid(below))) {
            this.level.setTile(x, y, z, Blocks.WATER_ID);
            this.wakeFluid(x, y, z);
         }
      }
      BreakEffect effect = new BreakEffect();
      effect.x = x;
      effect.y = y;
      effect.z = z;
      effect.texIndex = Blocks.particleTile(tileId);
      this.conn.sendToClient(effect);
      this.spawnDrop(x, y, z, tileId);
   }

   private void popUnsupported(int x, int y, int z) {
      this.popColumn(x, y, z);
      this.popColumn(x + 1, y, z);
      this.popColumn(x - 1, y, z);
      this.popColumn(x, y, z + 1);
      this.popColumn(x, y, z - 1);
   }

    private void popColumn(int x, int y, int z) {
       for (int yy = y - 1; yy >= 0; yy--) {
          int id = this.level.getTile(x, yy, z);
          Block b = id > 0 ? Blocks.byId(id) : null;
          if (b != null && !b.canStay(this.level, x, yy, z)) {
             this.breakBlock(x, yy, z, id);
             this.notifySupport(x, yy, z);
          } else {
             break;
          }
       }
       while (y < this.level.depth) {
         int id = this.level.getTile(x, y, z);
         Block b = id > 0 ? Blocks.byId(id) : null;
         if (b != null && !b.canStay(this.level, x, y, z)) {
            this.breakBlock(x, y, z, id);
             this.notifySupport(x, y, z);
         }
         y++;
      }
   }

   private void wakeFluid(int x, int y, int z) {
      this.wakeFluidCell(x + 1, y, z);
      this.wakeFluidCell(x - 1, y, z);
      this.wakeFluidCell(x, y + 1, z);
      this.wakeFluidCell(x, y - 1, z);
      this.wakeFluidCell(x, y, z + 1);
      this.wakeFluidCell(x, y, z - 1);
   }

   private void wakeFluidCell(int x, int y, int z) {
      int id = this.level.getTile(x, y, z);
      if (Blocks.isFluid(id)) {
         this.level.scheduleTick(x, y, z, Fluid.of(id).ticks);
      }
   }

    private void notifySupport(int x, int y, int z) {
       java.util.ArrayDeque<long[]> queue = new java.util.ArrayDeque<>();
       queue.add(new long[]{x, y, z});
       queue.add(new long[]{x + 1, y, z});
       queue.add(new long[]{x - 1, y, z});
       queue.add(new long[]{x, y + 1, z});
       queue.add(new long[]{x, y - 1, z});
       queue.add(new long[]{x, y, z + 1});
       queue.add(new long[]{x, y, z - 1});
       while (!queue.isEmpty()) {
          long[] c = queue.removeFirst();
          int cx = (int)c[0], cy = (int)c[1], cz = (int)c[2];
          int id = cy < 0 || cy >= this.level.depth ? 0 : this.level.getTile(cx, cy, cz);
          if (!Blocks.isFalling(id)) {
             continue;
          }
          int below = cy > 0 ? this.level.getTile(cx, cy - 1, cz) : 1;
          if (below == 0 || Blocks.isFluid(below) || below == Blocks.SNOW_LAYER_ID
             || below == Blocks.LILYPAD_ID) {
             this.spawnFalling(cx, cy, cz, id);
             queue.add(new long[]{cx + 1, cy, cz});
             queue.add(new long[]{cx - 1, cy, cz});
             queue.add(new long[]{cx, cy + 1, cz});
             queue.add(new long[]{cx, cy - 1, cz});
             queue.add(new long[]{cx, cy, cz + 1});
             queue.add(new long[]{cx, cy, cz - 1});
          }
       }
    }

   private void spawnFalling(int x, int y, int z, int id) {
      this.level.setTile(x, y, z, 0);
      this.wakeFluid(x, y, z);
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
            continue;
         }
         if (!e.onGround) {
            continue;
         }
         this.falling.remove(i--);
         this.sendFallRemove(e);
         int fx = e.landX(), fy = e.landY(), fz = e.landZ();
         int occupant = this.level.getTile(fx, fy, fz);
         if (occupant > 0 && !Blocks.isFluid(occupant)) {
            if (!Blocks.isSolid(occupant)) {
               this.breakBlock(fx, fy, fz, occupant);
            } else {
               this.spawnDrop(fx, fy + 1, fz, e.blockId);
               continue;
            }
         }
         this.level.setTile(fx, fy, fz, e.blockId);
         this.wakeFluid(fx, fy, fz);
         this.notifySupport(fx, fy, fz);
         this.popUnsupported(fx, fy, fz);
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
         x + 0.2F + rng.nextFloat() * 0.6F, y + 0.2F + rng.nextFloat() * 0.6F, z + 0.2F + rng.nextFloat() * 0.6F,
         (rng.nextFloat() - 0.5F) * 0.2F, 0.15F, (rng.nextFloat() - 0.5F) * 0.2F);
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
         e.tick(this.level, this.player.bb);
         if (e.y < -10.0F) {
            this.despawn(i--, e);
         } else if (e.expired()) {
            this.despawn(i--, e);
         } else if (e.pickupDelay <= 0 && e.near(this.player.bb)) {
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

   private void tickMobs() {
      for (int i = 0; i < this.mobs.size(); i++) {
         Zombie z = this.mobs.get(i);
         z.tick(this.level, this.player.x, this.player.y, this.player.z,
            this.player.bb, this.timeOfDay, this.player);
         if (!z.alive()) {
            this.mobs.remove(i--);
            ItemRemove r = new ItemRemove();
            r.entityId = z.id;
            this.conn.sendToClient(r);
         }
      }
      for (int i = 0; i < this.mobs.size(); i++) {
         Zombie z = this.mobs.get(i);
         float dx = z.x - this.player.x;
         float dz = z.z - this.player.z;
         if (dx * dx + dz * dz > 64.0F * 64.0F) {
            this.mobs.remove(i--);
            ItemRemove r = new ItemRemove();
            r.entityId = z.id;
            this.conn.sendToClient(r);
         }
      }
      if (++this.spawnClock >= 100) {
         this.spawnClock = 0;
         this.trySpawnMob();
      }
   }

   int mobCount() {
      return this.mobs.size();
   }

   Zombie mob(int i) {
      return this.mobs.get(i);
   }

   int spawnZombie(float x, float y, float z) {
      Zombie mob = new Zombie(this.nextEntityId++, x, y, z);
      this.mobs.add(mob);
      MobSpawn s = new MobSpawn();
      s.entityId = mob.id;
      s.mobType = MobSpawn.ZOMBIE;
      s.x = mob.x;
      s.y = mob.y;
      s.z = mob.z;
      this.conn.sendToClient(s);
      return mob.id;
   }

   private void trySpawnMob() {
      if (this.mobs.size() >= 6) {
         return;
      }
      if (DayCycle.amount(this.timeOfDay, Config.DAY_LENGTH) >= 0.1F) {
         return;
      }
      java.util.Random rng = Rng.world();
      for (int t = 0; t < 4; t++) {
         float a = rng.nextFloat() * (float)Math.PI * 2.0F;
         float d = 16.0F + rng.nextFloat() * 16.0F;
         int x = MathHelper.floor(this.player.x + Math.sin(a) * d);
         int z = MathHelper.floor(this.player.z + Math.cos(a) * d);
         int top = -1;
         for (int y = this.level.depth - 1; y >= 0; y--) {
            int id = this.level.getTile(x, y, z);
            if (id != 0 && Blocks.isSolid(id)) {
               top = y;
               break;
            }
         }
         if (top < 0 || top + 2 >= this.level.depth) {
            continue;
         }
         if (this.level.getTile(x, top + 1, z) != 0 || this.level.getTile(x, top + 2, z) != 0) {
            continue;
         }
         this.spawnZombie(x + 0.5F, top + 1.0F, z + 0.5F);
         return;
      }
   }

   private void handleAttack(AttackMob p) {
      for (Zombie z : this.mobs) {
         if (z.id != p.entityId) {
            continue;
         }
         float dx = z.x - this.player.x;
         float dy = z.y - this.player.y;
         float dz = z.z - this.player.z;
         if (dx * dx + dy * dy + dz * dz > 36.0F) {
            return;
         }
         if (!this.level.sightClear(this.player.x, this.player.y, this.player.z,
               z.x, z.y, z.z)) {
            return;
         }
         if (z.punch(this.player.x, this.player.z)) {
            MobHurt h = new MobHurt();
            h.entityId = z.id;
            h.hp = z.hp;
            h.xd = z.xd;
            h.zd = z.zd;
            this.conn.sendToClient(h);
         }
         return;
      }
   }

    private boolean payStock(int blockId, int slot) {
       if (slot >= 0 && slot < Inventory.SLOTS
          && !this.inventory.slots[slot].isEmpty() && this.inventory.slots[slot].blockId == blockId) {
          return this.inventory.consume(slot, 1) == 1;
       }
       for (int i = 0; i < Inventory.SLOTS; i++) {
          if (!this.inventory.slots[i].isEmpty() && this.inventory.slots[i].blockId == blockId) {
             return this.inventory.consume(i, 1) == 1;
          }
       }
       return false;
    }

    private boolean validFace(int face) {
       return face >= 0 && face <= 5;
    }

    private void handlePlace(PlaceBlock p) {
      if (this.player.spectator) {
         return;
      }
      int x = p.x;
      int y = p.y;
      int z = p.z;
      if (!this.validFace(p.face)) {
         return;
      }
      if (p.face == 0) y--;
      if (p.face == 1) y++;
      if (p.face == 2) z--;
      if (p.face == 3) z++;
      if (p.face == 4) x--;
      if (p.face == 5) x++;

      Block placing = Blocks.byId(p.blockId);
      int target = this.level.getTile(x, y, z);
      if (target != 0 && !Blocks.isFluid(target) && target != Blocks.TALL_GRASS_ID
         && target != Blocks.SNOW_LAYER_ID && target != Blocks.DEADBUSH_ID) {
         return;
      }
      if (placing != null) {
         AABB shape = placing.collisionBox(this.level, x, y, z);
         if (shape != null && shape.intersects(this.player.bb)) {
            return;
         }
      }
      if (p.blockId == Blocks.SNOW_LAYER_ID) {
         int tx = x, ty = y, tz = z;
         if (!(this.level.getTile(tx, ty, tz) == Blocks.SNOW_LAYER_ID
            && (this.level.getData(tx, ty, tz) & 7) < 7)) {
            tx = p.x;
            ty = p.y;
            tz = p.z;
         }
         if (this.level.getTile(tx, ty, tz) == Blocks.SNOW_LAYER_ID
            && (this.level.getData(tx, ty, tz) & 7) < 7) {
            if (!this.payStock(p.blockId, p.slot)) {
               return;
            }
             this.level.setTile(tx, ty, tz,
                Blocks.stateOf(Blocks.byId(p.blockId), (this.level.getData(tx, ty, tz) & 7) + 1));
             this.notifySupport(tx, ty, tz);
             this.popUnsupported(tx, ty, tz);
            this.wakeFluid(tx, ty, tz);
            this.sendInv();
            return;
         }
      }
      boolean wallTorch = p.blockId == Blocks.TORCH_ID && p.face >= 2 && p.face <= 5;
      if (p.blockId == Blocks.TORCH_ID && p.face == 0) {
         return;
      }
      if (placing != null) {
         if (wallTorch) {
            if (!TorchBlock.wallSolid(this.level, x, y, z, p.face)) {
               return;
            }
         } else if (!placing.canStay(this.level, x, y, z)) {
            return;
         }
      }
      if (!this.payStock(p.blockId, p.slot)) {
         return;
      }
      if (wallTorch) {
         this.level.setTile(x, y, z, Blocks.stateOf(placing, p.face));
      } else {
         this.level.setTile(x, y, z, p.blockId);
      }
      if (Blocks.isSapling(p.blockId)) {
         this.level.scheduleTick(x, y, z, Level.SAPLING_TICKS);
      }
      if (p.blockId == Blocks.REED_ID || p.blockId == Blocks.CACTUS_ID) {
         this.level.scheduleTick(x, y, z, Level.GROWTH_TICKS);
      }
      if (p.blockId == Blocks.TORCH_ID) {
         this.level.armMeltCheck(x, y, z, 5);
      } else if (p.blockId == Blocks.LAVA_ID) {
         this.level.armMeltCheck(x, y, z, 7);
      } else if (p.blockId == Blocks.ICE_ID || p.blockId == Blocks.SNOW_LAYER_ID
         || p.blockId == Blocks.SNOW_BLOCK_ID) {
         this.level.armMeltCheck(x, y, z, 7);
      }
      if (Blocks.isFluid(p.blockId)) {
         this.level.scheduleTick(x, y, z, Fluid.of(p.blockId).ticks);
      }
       this.notifySupport(x, y, z);
       this.popUnsupported(x, y, z);
       this.wakeFluid(x, y, z);
       this.sendInv();
    }

   @Override
   public void tileChanged(int x, int y, int z) {
      TileUpdate update = new TileUpdate();
      update.x = x;
      update.y = y;
      update.z = z;
      update.type = this.level.getTile(x, y, z);
      update.data = this.level.getData(x, y, z);
      this.conn.sendToClient(update);
   }

   @Override
   public void lightColumnChanged(int x, int z, int y0, int y1) {
   }
}

