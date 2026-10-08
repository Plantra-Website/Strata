package com.strata.world.mesh;

import com.strata.HitResult;
import com.strata.blocks.AtlasStitcher;
import com.strata.blocks.Block;
import com.strata.blocks.Blocks;
import com.strata.blocks.MeshBuilder;
import com.strata.core.Config;
import com.strata.core.Debug;
import com.strata.core.Log;
import com.strata.core.MathHelper;
import com.strata.world.Level;
import com.strata.world.LevelListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;

public class LevelRenderer implements LevelListener {
    private static final int CHUNK_SIZE = 16;
   private final Level level;
   private final HashMap<Long, Chunk> chunks = new HashMap<>();
   private static final java.util.Comparator<Chunk> MESH_ORDER = new java.util.Comparator<Chunk>() {
      @Override public int compare(Chunk a, Chunk b) {
         if (a.queueDist != b.queueDist) {
            return a.queueDist - b.queueDist;
         }
         return a.queueSeq < b.queueSeq ? -1 : (a.queueSeq == b.queueSeq ? 0 : 1);
      }
   };
   private final AtomicLong meshSeq = new AtomicLong();
   private final BlockingQueue<Chunk> meshQueue = new PriorityBlockingQueue<>(128, MESH_ORDER);
   private volatile long carveOffer = Long.MIN_VALUE;
   private int lastCx = Integer.MIN_VALUE;
   private int lastCz = Integer.MIN_VALUE;
   private int lastRadius = Integer.MIN_VALUE;
   private Frustum cachedFrustum;
   private final MeshBuilder crackScratch = new MeshBuilder();
   public int drawn0;
   public int drawn1;
   public int drawn2;
   public int verts0;
   public int verts1;
   public int verts2;
   public int renderedLastFrame;
   public int submitted;

    private record MeshWorker(BlockingQueue<Chunk> queue, String name) implements Runnable {
        MeshWorker(BlockingQueue<Chunk> queue, int id) {
            this(queue, "mesh-" + id);
        }

        @Override
        public void run() {
            Debug.worker(this.name, "idle");
            while (true) {
                Chunk failed = null;
                try {
                    Chunk c = this.queue.take();
                    failed = c;
                    Debug.worker(this.name, "chunk " + c.x0 / 16 + "," + c.z0 / 16);
                    MeshData m = c.mesh();
                    synchronized (c) {
                        c.pending = m;
                        c.meshState = 2;
                    }
                    Debug.worker(this.name, "idle");
                } catch (InterruptedException e) {
                    return;
                } catch (Throwable e) {
                    Log.error("mesh", "worker failed", e);
                    if (failed != null) {
                        synchronized (failed) {
                            failed.meshState = 0;
                        }
                    }
                }
            }
        }
        }

    public int meshQueueDepth() {
       return this.meshQueue.size();
    }

   public String census() {
      int total = 0, dirty = 0, inflight = 0, ready = 0, built = 0;
      for (Chunk c : this.chunks.values()) {
         total++;
         if (c.dirty) {
            dirty++;
         }
         synchronized (c) {
            if (c.meshState == 1) {
               inflight++;
            } else if (c.meshState == 2) {
               ready++;
            }
         }
         if (c.vertCount(0) + c.vertCount(1) + c.vertCount(2) > 0) {
            built++;
         }
      }
      return "chunks=" + total + " dirty=" + dirty + " inflight=" + inflight + " ready=" + ready + " built=" + built
         + " rendered=" + this.renderedLastFrame;
   }

   public LevelRenderer(Level level) {
      this.level = level;
      level.addListener(this);
       for (int i = 0; i < Config.MESH_WORKERS; i++) {
          Thread worker = new Thread(new MeshWorker(this.meshQueue, i), "chunk-mesher-" + i);
          worker.setDaemon(true);
          worker.start();
       }
       Thread carveAhead = new Thread(() -> {
          long last = Long.MIN_VALUE;
          while (true) {
             try {
                int warmed = 0;
                for (Chunk c : this.meshQueue) {
                   if (warmed >= 8) {
                      break;
                   }
                   int ccx = Math.floorDiv(c.x0, CHUNK_SIZE);
                   int ccz = Math.floorDiv(c.z0, CHUNK_SIZE);
                   this.level.warmCarves(ccx, ccz, ccx, ccz);
                   warmed++;
                }
                long co = this.carveOffer;
                if (co != last) {
                   last = co;
                   int pcx = (int)(co >> 32);
                   int pcz = (int)(co & 0xFFFFFFFFL);
                   int r = Config.VIEW_RADIUS + 1;
                   this.level.warmCarves(pcx - r, pcz - r, pcx + r, pcz + r);
                }
                Thread.sleep(warmed >= 8 ? 50L : 500L);
             } catch (InterruptedException e) {
                return;
             }
          }
       }, "carve-ahead");
       carveAhead.setDaemon(true);
       carveAhead.start();

      this.ensureRadius(0, 0, Config.PREBUILD_RADIUS);
      int queued = 0;
      for (Chunk c : this.chunks.values()) {
         int before = c.meshState;
         this.offer(c, 0, 0);
         if (c.meshState != before) {
            queued++;
         }
      }
      Log.info("world", "queued " + queued + " spawn chunks (streaming, first frame immediate)");
   }

   private static long chunkKey(int cx, int cz) {
      return ((long)cx << 32) | (cz & 0xFFFFFFFFL);
   }

   private static int chunkCoord(float v) {
      return Math.floorDiv(MathHelper.floor(v), CHUNK_SIZE);
   }

   private Chunk getOrCreate(int cx, int cz) {
      long key = chunkKey(cx, cz);
      Chunk c = this.chunks.get(key);
      if (c == null) {
         c = new Chunk(this.level, cx * CHUNK_SIZE, cz * CHUNK_SIZE);
         this.chunks.put(key, c);
      }
      return c;
   }

   private void ensureRadius(int pcx, int pcz) {
      this.ensureRadius(pcx, pcz, Config.VIEW_RADIUS);
   }

    private void ensureRadius(int pcx, int pcz, int radius) {
      for (int cx = pcx - radius; cx <= pcx + radius; cx++) {
         for (int cz = pcz - radius; cz <= pcz + radius; cz++) {
            this.getOrCreate(cx, cz);
         }
      }
      this.level.ensureRegions(pcx - radius, pcz - radius, pcx + radius, pcz + radius);
   }

   private void unloadFar(int pcx, int pcz) {
      Iterator<Map.Entry<Long, Chunk>> it = this.chunks.entrySet().iterator();
      while (it.hasNext()) {
         Chunk c = it.next().getValue();
         int cx = Math.floorDiv(c.x0, CHUNK_SIZE);
         int cz = Math.floorDiv(c.z0, CHUNK_SIZE);
         if (Math.abs(cx - pcx) > Config.VIEW_RADIUS + 1 || Math.abs(cz - pcz) > Config.VIEW_RADIUS + 1) {
            c.dispose();
            it.remove();
         }
      }
      this.level.evictFar(pcx, pcz, Config.VIEW_RADIUS + 1);
   }

    private void offer(Chunk c, int pcx, int pcz) {
       synchronized (c) {
          if (c.dirty && c.meshState == 0) {
             c.meshState = 1;
             int dist = Math.abs(Math.floorDiv(c.x0, CHUNK_SIZE) - pcx)
                + Math.abs(Math.floorDiv(c.z0, CHUNK_SIZE) - pcz);
             boolean drawn = c.uploaded();
             if (drawn && dist <= 3) {
                c.queueDist = dist - 1000;   
             } else if (!drawn) {
                c.queueDist = dist;          
             } else {
                c.queueDist = dist + 1000;   
             }
             c.queueSeq = this.meshSeq.getAndIncrement();
             this.meshQueue.offer(c);
          }
       }
    }

   private void uploadReady() {
      int n = this.drainReady(Config.UPLOAD_BUDGET, true);
      if (n < Config.UPLOAD_BUDGET) {
         this.drainReady(Config.UPLOAD_BUDGET - n, false);
      }
   }

   private int drainReady(int budget, boolean uploaded) {
      int n = 0;
      for (Chunk c : this.chunks.values()) {
         if (n >= budget) {
            break;
         }
         if (c.uploaded() != uploaded) {
            continue;
         }
         MeshData m = null;
         synchronized (c) {
            if (c.meshState == 2) {
               m = c.pending;
               c.pending = null;
               c.meshState = 0;
            }
         }
         if (m != null) {
            c.upload(m);
            n++;
         }
      }
      return n;
   }

   private int offerDirty(int pcx, int pcz, int n, boolean uploaded) {
      for (Chunk c : this.chunks.values()) {
         if (n >= Config.SUBMIT_BUDGET) {
            break;
         }
         if (c.uploaded() != uploaded) {
            continue;
         }
         if (c.dirty) {
            int before = c.meshState;
            this.offer(c, pcx, pcz);
            if (c.meshState != before) {
               n++;
               this.submitted++;
            }
         }
      }
      return n;
   }

   public void render(float px, float pz, boolean fullBright, int layer) {
      if (layer == 0) {
         int pcx = chunkCoord(px);
         int pcz = chunkCoord(pz);
         if (pcx != this.lastCx || pcz != this.lastCz || Config.VIEW_RADIUS != this.lastRadius) {
            this.lastCx = pcx;
            this.lastCz = pcz;
            this.lastRadius = Config.VIEW_RADIUS;
            this.ensureRadius(pcx, pcz);
            this.unloadFar(pcx, pcz);
         }
         this.carveOffer = chunkKey(pcx, pcz);
         Frustum live = Frustum.getFrustum();
         int n = 0;
         n = this.offerDirty(pcx, pcz, n, false);        
         if (n < Config.SUBMIT_BUDGET) {
            n = this.offerDirty(pcx, pcz, n, true);      
         }
         this.uploadReady();
         this.cachedFrustum = live;
      }
      Frustum frustum = this.cachedFrustum;
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      Textures.bind(Textures.loadAtlas(9728));
      GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
      GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
      if (fullBright) {
         GL11.glColor3f(1.0F, 1.0F, 1.0F);
      } else {
         GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
      }
      GL11.glEnableClientState(GL14.GL_SECONDARY_COLOR_ARRAY);
      ChunkShader.bind();

      int passed = 0;
      boolean water = layer == 2;
      if (water) {
         GL11.glEnable(GL11.GL_BLEND);
         GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      }
      for (Chunk c : this.chunks.values()) {
         if (frustum.cubeInFrustum(c.aabb)) {
            passed++;
            if (layer == 0) {
               this.drawn0++;
               this.verts0 += c.vertCount(0);
            } else if (layer == 1) {
               this.drawn1++;
               this.verts1 += c.vertCount(1);
            } else {
               this.drawn2++;
               this.verts2 += c.vertCount(2);
            }
            c.render(layer, fullBright);
         }
      }
      if (water) {
         GL11.glDisable(GL11.GL_BLEND);
      }
      ChunkShader.unbind();
      if (layer == 0) {
         this.renderedLastFrame = passed;
      } else {
         this.renderedLastFrame += passed;
      }

      GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
      GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
      if (!fullBright) {
         GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
      }
      GL11.glDisableClientState(GL14.GL_SECONDARY_COLOR_ARRAY);
      GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
   }

    public void renderHit(HitResult h, float progress) {
       com.strata.blocks.Block picked = null;
       int pickTile = this.level.getTile(h.x, h.y, h.z);
       if (pickTile > 0) {
          picked = Blocks.byId(pickTile);
       }
       com.strata.core.AABB box = picked == null ? null : picked.pickBox(this.level, h.x, h.y, h.z);
       float x0 = box == null ? h.x : box.x0;
       float y0 = box == null ? h.y : box.y0;
       float z0 = box == null ? h.z : box.z0;
       float x1 = box == null ? h.x + 1.0F : box.x1;
       float y1 = box == null ? h.y + 1.0F : box.y1;
       float z1 = box == null ? h.z + 1.0F : box.z1;

       GL11.glDisable(GL11.GL_ALPHA_TEST);
       GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
      GL11.glLineWidth(2.0F);
      GL11.glDisable(GL11.GL_TEXTURE_2D);

      float o = 0.002F;
      GL11.glBegin(GL11.GL_LINE_STRIP);
      GL11.glVertex3f(x0 - o, y0 - o, z0 - o);
      GL11.glVertex3f(x1 + o, y0 - o, z0 - o);
      GL11.glVertex3f(x1 + o, y0 - o, z1 + o);
      GL11.glVertex3f(x0 - o, y0 - o, z1 + o);
      GL11.glVertex3f(x0 - o, y0 - o, z0 - o);
      GL11.glEnd();

      GL11.glBegin(GL11.GL_LINE_STRIP);
      GL11.glVertex3f(x0 - o, y1 + o, z0 - o);
      GL11.glVertex3f(x1 + o, y1 + o, z0 - o);
      GL11.glVertex3f(x1 + o, y1 + o, z1 + o);
      GL11.glVertex3f(x0 - o, y1 + o, z1 + o);
      GL11.glVertex3f(x0 - o, y1 + o, z0 - o);
      GL11.glEnd();

      GL11.glBegin(GL11.GL_LINES);
      GL11.glVertex3f(x0 - o, y0 - o, z0 - o);
      GL11.glVertex3f(x0 - o, y1 + o, z0 - o);
      GL11.glVertex3f(x1 + o, y0 - o, z0 - o);
      GL11.glVertex3f(x1 + o, y1 + o, z0 - o);
      GL11.glVertex3f(x1 + o, y0 - o, z1 + o);
      GL11.glVertex3f(x1 + o, y1 + o, z1 + o);
      GL11.glVertex3f(x0 - o, y0 - o, z1 + o);
      GL11.glVertex3f(x0 - o, y1 + o, z1 + o);
      GL11.glEnd();

      if (progress > 0.0F) {
         int tileId = this.level.getTile(h.x, h.y, h.z);
         Block block = tileId > 0 ? Blocks.byId(tileId) : null;
         if (block != null) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            Textures.bind(Textures.loadAtlas(9728));
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            int crackTile = AtlasStitcher.slot("blocks/destroy_stage_" + crackStageFor(progress) + ".png");
            this.crackScratch.init();
            block.renderCrack(this.crackScratch, this.level, h.x, h.y, h.z, crackTile);
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(-1.0F, -1.0F);
            Tesselator t = Tesselator.SHARED;
            t.init();
            t.drain(this.crackScratch);
            t.flush();
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
         }
      }

      GL11.glDisable(GL11.GL_BLEND);
      GL11.glEnable(GL11.GL_ALPHA_TEST);
      GL11.glAlphaFunc(GL11.GL_GREATER, 0.5F);
   }

   static int crackStageFor(float progress) {
      if (progress <= 0.0F) {
         return 0;
      }
      if (progress >= 1.0F) {
         return 9;
      }
      return (int)(progress * 10.0F);
   }

   public void setDirty(int x0, int y0, int z0, int x1, int y1, int z1) {
      int cx0 = Math.floorDiv(x0, CHUNK_SIZE);
      int cx1 = Math.floorDiv(x1, CHUNK_SIZE);
      int cz0 = Math.floorDiv(z0, CHUNK_SIZE);
      int cz1 = Math.floorDiv(z1, CHUNK_SIZE);

      for (int cx = cx0; cx <= cx1; cx++) {
         for (int cz = cz0; cz <= cz1; cz++) {
            Chunk c = this.chunks.get(chunkKey(cx, cz));
            if (c != null) {
               c.setDirty();
            }
         }
      }
   }

   @Override
   public void tileChanged(int x, int y, int z) {
      this.setDirty(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
   }

   @Override
   public void lightColumnChanged(int x, int z, int y0, int y1) {
      this.setDirty(x - 1, y0 - 1, z - 1, x + 1, y1 + 1, z + 1);
   }
}

