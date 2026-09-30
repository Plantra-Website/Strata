package com.strata.world.mesh;

import com.strata.blocks.Block;
import com.strata.blocks.BlockState;
import com.strata.blocks.Blocks;
import com.strata.blocks.MeshBuilder;
import com.strata.core.AABB;
import com.strata.core.Config;
import com.strata.core.Debug;
import com.strata.core.GlResources;
import com.strata.core.Profiler;
import com.strata.world.Level;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

class MeshData {
   final float[][] verts = new float[3][];
   final float[][] texs = new float[3][];
   final float[][] cols = new float[3][];
   final int[] counts = new int[3];
   final int version;
   int y0;
   int y1;
   MeshData(int version, int depth) {
      this.version = version;
      this.y0 = 0;
      this.y1 = depth;
   }
}

public class Chunk {
   public AABB aabb;
   public final Level level;
   public final int x0;
   public final int z0;
   public volatile boolean dirty = true;
   volatile int meshState = 0; 
   volatile boolean built = false;
    volatile MeshData pending = null;
    volatile int dirtyVersion = 0;
    volatile int queueDist = Integer.MAX_VALUE;
    volatile long queueSeq = 0L;
   static final int STRIDE_FLOATS = 8;
   static final int STRIDE_BYTES = STRIDE_FLOATS * 4;
   private final int[] vbo = new int[3]; 
   private final int[] counts = new int[3];
   private boolean hasVbo = false;
   public static int updates = 0;
   public static long uploadNs = 0L;

   public int vertCount(int layer) {
      return this.hasVbo ? this.counts[layer] : 0;
   }

   public boolean uploaded() {
      return this.hasVbo;
   }

   public Chunk(Level level, int x0, int z0) {
      this.level = level;
      this.x0 = x0;
      this.z0 = z0;
      this.aabb = new AABB(x0, 0, z0, x0 + 16, level.depth, z0 + 16);
   }

   private static final ThreadLocal<MeshBuilder[]> BUILDERS = new ThreadLocal<MeshBuilder[]>() {
      @Override
      protected MeshBuilder[] initialValue() {
         return new MeshBuilder[]{new MeshBuilder(), new MeshBuilder(), new MeshBuilder()};
      }
   };

     public MeshData mesh() {
       Profiler.push("mesh");
       long t0 = System.nanoTime();
      try {
          ChunkView view = new ChunkView(this.level,
             Math.floorDiv(this.x0, 16), Math.floorDiv(this.z0, 16));
          if (!this.built) {
             this.level.reconcileSkyChunk(Math.floorDiv(this.x0, 16), Math.floorDiv(this.z0, 16), view);
          }
          long t1 = System.nanoTime();
          MeshData m = this.meshInner(view);
         this.built = true;
          long t2 = System.nanoTime();
          long ms = (t2 - t0) / 1000000L;
          Debug.slow("mesh", ms, Config.SLOW_MESH_MS,
             "chunk " + this.x0 / 16 + "," + this.z0 / 16
             + " verts=" + (m.counts[0] + m.counts[1] + m.counts[2])
             + " reconcile=" + (t1 - t0) / 1000000L + "ms"
             + " inner=" + (t2 - t1) / 1000000L + "ms");
          return m;
       } finally {
          Profiler.pop();
       }
    }

     private MeshData meshInner(ChunkView view) {
       MeshData m = new MeshData(this.dirtyVersion, this.level.depth);
       MeshBuilder[] builders = BUILDERS.get();
       for (MeshBuilder b : builders) {
          b.init();
       }
       int depth = this.level.depth;
       for (int x = this.x0; x < this.x0 + 16; x++) {
          for (int z = this.z0; z < this.z0 + 16; z++) {
             byte[] col = view.column(x, z);
             byte[] dataCol = view.dataColumn(x, z);
            int top = depth - 1;
            while (top >= 0 && col[top] == 0) {
               top--;
            }
            for (int y = 0; y <= top; y++) {
               int tileId = col[y] & 0xFF;
               if (tileId == 0) {
                  continue;
               }
               Block block = Blocks.byId(tileId);
               if (block == null) {
                  continue;
               }
               if (block.light > 0 && this.level.getBlockLevel(x, y, z) < block.light) {
                  this.level.floodAdd(x, y, z, block.light);
               }
                int data = dataCol == null ? 0 : dataCol[y] & 0xFF;
                BlockState state = data == 0 ? block.state : Blocks.stateOf(block, data);
                block.render(builders[0], view, 0, x, y, z, state);
                block.render(builders[1], view, 1, x, y, z, state);
                if (tileId == Blocks.WATER_ID) {
                   block.render(builders[2], view, 2, x, y, z, state);
                }
            }
         }
      }
      for (int layer = 0; layer < 3; layer++) {
         MeshBuilder b = builders[layer];
         m.counts[layer] = b.count();
         if (m.counts[layer] > 0) {
            m.verts[layer] = b.vertices();
            m.texs[layer] = b.texCoords();
            m.cols[layer] = b.colors();
         }
      }
      int lo = this.level.depth;
      int hi = 0;
      boolean any = false;
      for (int layer = 0; layer < 3; layer++) {
         float[] v = m.verts[layer];
         if (v == null) {
            continue;
         }
         for (int i = 1; i < v.length; i += 3) {
            int y = (int)v[i];
            if (y < lo) {
               lo = y;
            }
            if (y > hi) {
               hi = y;
            }
            any = true;
         }
      }
      if (any) {
         m.y0 = lo;
         m.y1 = hi + 1;
      }
      return m;
   }

   private static FloatBuffer staging = BufferUtils.createFloatBuffer(1 << 20);

     public void upload(MeshData m) {
        Profiler.push("upload");
        long t0 = System.nanoTime();
        try {
           this.uploadInner(m);
        } finally {
           Profiler.pop();
           long ms = (System.nanoTime() - t0) / 1000000L;
           Debug.slow("upload", ms, Config.SLOW_UPLOAD_MS,
              "chunk " + this.x0 / 16 + "," + this.z0 / 16
              + " verts=" + (m.counts[0] + m.counts[1] + m.counts[2]));
        }
     }

    private void uploadInner(MeshData m) {
      long t0 = System.nanoTime();
      for (int layer = 0; layer < 3; layer++) {
         this.counts[layer] = m.counts[layer];
         if (m.counts[layer] == 0) {
            continue;
         }
         this.ensureVbo(layer);
         GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo[layer]);
         GL15.glBufferData(GL15.GL_ARRAY_BUFFER, stage(interleave(m.verts[layer], m.texs[layer], m.cols[layer], m.counts[layer])), GL15.GL_STATIC_DRAW);
      }
      GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
      uploadNs += System.nanoTime() - t0;
      updates++;
      this.hasVbo = true;
      this.aabb = new AABB(this.x0, m.y0, this.z0, this.x0 + 16, m.y1, this.z0 + 16);
      if (this.dirtyVersion == m.version) {
         this.dirty = false;
      }
   }

   private void ensureVbo(int layer) {
      if (this.vbo[layer] == 0) {
         this.vbo[layer] = GL15.glGenBuffers();
         GlResources.track(this.vbo[layer], "chunk");
      }
   }

   private static FloatBuffer stage(float[] data) {
      if (staging.capacity() < data.length) {
         staging = BufferUtils.createFloatBuffer(Math.max(1024, Integer.highestOneBit(data.length - 1) << 2));
      }
      staging.clear();
      staging.put(data);
      staging.flip();
      return staging;
   }

   static float[] interleave(float[] verts, float[] texs, float[] cols, int count) {
      float[] out = new float[count * STRIDE_FLOATS];
      for (int i = 0; i < count; i++) {
         out[i * 8] = verts[i * 3];
         out[i * 8 + 1] = verts[i * 3 + 1];
         out[i * 8 + 2] = verts[i * 3 + 2];
         out[i * 8 + 3] = texs[i * 2];
         out[i * 8 + 4] = texs[i * 2 + 1];
         out[i * 8 + 5] = cols[i * 3];
         out[i * 8 + 6] = cols[i * 3 + 1];
         out[i * 8 + 7] = cols[i * 3 + 2];
      }
      return out;
   }

   public void render(int layer, boolean fullBright) {
      if (!this.hasVbo || this.counts[layer] == 0) {
         return;
      }
      GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo[layer]);
      GL11.glVertexPointer(3, GL11.GL_FLOAT, STRIDE_BYTES, 0L);
      GL11.glTexCoordPointer(2, GL11.GL_FLOAT, STRIDE_BYTES, 3 * 4L);
      if (!fullBright) {
         GL11.glColorPointer(3, GL11.GL_FLOAT, STRIDE_BYTES, 5 * 4L);
      }
      GL11.glDrawArrays(GL11.GL_QUADS, 0, this.counts[layer]);
   }

   public void setDirty() {
      this.dirty = true;
      this.dirtyVersion++;
   }

    public void dispose() {
       boolean any = false;
       for (int id : this.vbo) {
          if (id != 0) {
             any = true;
          }
       }
       if (!any) {
          this.hasVbo = false;
          return;
       }
       java.nio.IntBuffer ids = BufferUtils.createIntBuffer(this.vbo.length);
       ids.put(this.vbo);
       ids.flip();
       GL15.glDeleteBuffers(ids);
       for (int i = 0; i < this.vbo.length; i++) {
          if (this.vbo[i] != 0) {
             GlResources.release(this.vbo[i]);
          }
       }
       this.hasVbo = false;
    }
}

