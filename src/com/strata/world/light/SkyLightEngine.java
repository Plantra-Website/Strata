package com.strata.world.light;

import com.strata.core.BlockerProbe;
import com.strata.core.Dirs;
import com.strata.core.Profiler;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SkyLightEngine {
   private final LightWorld world;
   private final ConcurrentHashMap<Long, byte[]> levels = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<Long, Integer> heights = new ConcurrentHashMap<>();
   private final Set<Long> seeded = ConcurrentHashMap.newKeySet();

   public SkyLightEngine(LightWorld world) {
      this.world = world;
   }

   private static final int Z_MUL = 0x9E3779B1;
   private static final int Z_INV = 0xe8b2f51;

   private static long columnKey(int x, int z) {
      return ((long)x << 32) | ((z * Z_MUL) & 0xFFFFFFFFL);
   }

   private static int keyZ(long k) {
      return (int)k * Z_INV;
   }

   public int opaqueHeight(int x, int z) {
      long k = columnKey(x, z);
      Integer cached = this.heights.get(k);
      if (cached != null) {
         return cached;
      }
      int y = this.world.depth() - 1;
      while (y > 0 && !this.world.isLightBlocker(x, y, z)) {
         y--;
      }
      this.heights.put(k, y);
      return y;
   }

   public int get(int x, int y, int z) {
      if (y >= this.world.depth()) {
         return 15;
      }
      if (y < 0) {
         return 0;
      }
      if (y >= this.opaqueHeight(x, z) && !this.world.isLightBlocker(x, y, z)) {
         return 15;
      }
      this.ensureSeeded(x, z);
      byte[] arr = this.levels.get(columnKey(x, z));
      return arr == null ? 0 : (arr[y] & 15);
   }

   public void onEdit(int x, int y, int z, boolean wasOpaque, boolean isOpaque, int oldDepth, int newDepth) {
      if (wasOpaque == isOpaque) {
         return;
      }
      HashSet<Long> touched = new HashSet<>();
      if (!wasOpaque && isOpaque && newDepth > oldDepth) {
         for (int yy = oldDepth; yy < newDepth; yy++) {
            this.writeCell(x, yy, z, 0, touched);
         }
      } else if (wasOpaque && !isOpaque && newDepth < oldDepth) {
         for (int yy = newDepth; yy < oldDepth; yy++) {
            this.writeCell(x, yy, z, 15, touched);
         }
      }
      int lo = oldDepth < newDepth ? oldDepth : newDepth;
      int hi = oldDepth > newDepth ? oldDepth : newDepth;
      for (int yy = lo; yy <= hi; yy++) {
         this.updateSkyAt(x, yy, z, touched);
      }
      if (y < lo || y > hi) {
         this.updateSkyAt(x, y, z, touched);
      }
      this.notifyColumns(touched);
   }

   public void invalidate(int x, int z) {
      this.heights.remove(columnKey(x, z));
   }

   public void reconcileSkyChunk(int ccx, int ccz) {
      this.reconcileFill(ccx, ccz, null);
   }

   public void reconcileSkyChunk(int ccx, int ccz, BlockerProbe tiles) {
      this.reconcileFill(ccx, ccz, tiles);
   }

   private void reconcileFill(int ccx, int ccz, BlockerProbe tiles) {
      int x0 = ccx * 16;
      int z0 = ccz * 16;
      for (int x = x0 - 1; x <= x0 + 16; x++) {
         for (int z = z0 - 1; z <= z0 + 16; z++) {
            if (tiles == null) {
               this.ensureSeeded(x, z);
            } else {
               this.seedFromSnapshot(x, z, tiles);
            }
         }
      }
      HashSet<Long> touched = new HashSet<>();
      ArrayDeque<int[]> queue = new ArrayDeque<>();
      for (int x = x0 - 1; x <= x0 + 16; x++) {
         for (int z = z0 - 1; z <= z0 + 16; z++) {
            byte[] arr = this.levels.get(columnKey(x, z));
            if (arr == null) {
               continue;
            }
            for (int y = 0; y < this.world.depth(); y++) {
               boolean blocker = tiles == null
                  ? this.world.isLightBlocker(x, y, z) : tiles.isBlocker(x, y, z);
               if ((arr[y] & 15) == 15 && !blocker) {
                  queue.add(new int[]{x, y, z, 15});
               }
            }
         }
      }
      if (tiles == null) {
         this.propagateUp(queue, touched);
      } else {
         this.propagateUpT(queue, touched, tiles, x0 - 16, z0 - 16);
      }
      this.notifyColumns(touched);
   }

   private static final int LW = 48;

   private byte[] windowColumn(byte[][] cache, int ox, int oz, int x, int z) {
      int dx = x - ox;
      int dz = z - oz;
      if (dx < 0 || dx >= LW || dz < 0 || dz >= LW) {
         this.ensureSeeded(x, z);
         return this.levels.get(columnKey(x, z));
      }
      int i = dz * LW + dx;
      byte[] arr = cache[i];
      if (arr == null) {
         this.ensureSeeded(x, z);
         arr = this.levels.get(columnKey(x, z));
         cache[i] = arr;
      }
      return arr;
   }

   private void propagateUpT(ArrayDeque<int[]> queue, HashSet<Long> touched, BlockerProbe tiles, int ox, int oz) {
      byte[][] cache = new byte[LW * LW][];
      int depth = this.world.depth();
      while (!queue.isEmpty()) {
         int[] c = queue.poll();
         int nl = c[3] - 1;
         if (nl <= 0) {
            continue;
         }
         for (int[] d : Dirs.DIRS) {
            int nx = c[0] + d[0];
            int ny = c[1] + d[1];
            int nz = c[2] + d[2];
            if (ny < 0 || ny >= depth) {
               continue;
            }
            if (tiles.isBlocker(nx, ny, nz)) {
               continue;
            }
            byte[] arr = this.windowColumn(cache, ox, oz, nx, nz);
            if (arr == null) {
               if (this.readCell(nx, ny, nz) >= nl) {
                  continue;
               }
               this.writeCell(nx, ny, nz, nl, touched);
               queue.add(new int[]{nx, ny, nz, nl});
               continue;
            }
            if ((arr[ny] & 15) >= nl) {
               continue;
            }
            arr[ny] = (byte)(nl & 15);
            touched.add(columnKey(nx, nz));
            queue.add(new int[]{nx, ny, nz, nl});
         }
      }
   }

   public void clear() {
      this.levels.clear();
      this.heights.clear();
      this.seeded.clear();
   }

   public void clearSeeded() {
      this.seeded.clear();
   }

   private void ensureSeeded(int x, int z) {
      long k = columnKey(x, z);
      if (this.seeded.add(k)) {
         this.seedColumn(x, z);
      }
   }

   private void seedColumn(int x, int z) {
      Profiler.push("sky");
      try {
         int depth = this.world.depth();
         long k = columnKey(x, z);
         byte[] arr = this.levels.get(k);
         if (arr == null) {
            byte[] fresh = new byte[depth];
            byte[] prev = this.levels.putIfAbsent(k, fresh);
            arr = (prev != null) ? prev : fresh;
         }
         for (int y = depth - 1; y >= 0; y--) {
            if (this.world.isLightBlocker(x, y, z)) {
               break;
            }
            arr[y] = 15;
         }
      } finally {
         Profiler.pop();
      }
   }

   private void seedFromSnapshot(int x, int z, BlockerProbe tiles) {
      long k = columnKey(x, z);
      if (!this.seeded.add(k)) {
         return;
      }
      int depth = this.world.depth();
      byte[] arr = this.levels.get(k);
      if (arr == null) {
         byte[] fresh = new byte[depth];
         byte[] prev = this.levels.putIfAbsent(k, fresh);
         arr = (prev != null) ? prev : fresh;
      }
      for (int y = depth - 1; y >= 0; y--) {
         if (tiles.isBlocker(x, y, z)) {
            break;
         }
         arr[y] = 15;
      }
   }

   private int readCell(int x, int y, int z) {
      if (y < 0 || y >= this.world.depth()) {
         return 0;
      }
      this.ensureSeeded(x, z);
      byte[] arr = this.levels.get(columnKey(x, z));
      return arr == null ? 0 : (arr[y] & 15);
   }

   private void writeCell(int x, int y, int z, int level, HashSet<Long> touched) {
      if (y < 0 || y >= this.world.depth()) {
         return;
      }
      long k = columnKey(x, z);
      byte[] arr = this.levels.get(k);
      if (arr == null) {
         if (level <= 0) {
            return;
         }
         byte[] fresh = new byte[this.world.depth()];
         byte[] prev = this.levels.putIfAbsent(k, fresh);
         arr = (prev != null) ? prev : fresh;
      }
      if ((arr[y] & 15) != (level & 15)) {
         arr[y] = (byte)(level & 15);
         touched.add(k);
      }
   }

   private void updateSkyAt(int x, int y, int z, HashSet<Long> touched) {
      Profiler.push("sky");
      try {
         this.ensureSeeded(x, z);
         int old = this.readCell(x, y, z);
         int fresh;
         if (this.world.isLightBlocker(x, y, z)) {
            fresh = 0;
         } else if (y >= this.opaqueHeight(x, z)) {
            fresh = 15;
         } else {
            int best = 0;
            for (int[] d : Dirs.DIRS) {
               int nl = this.readCell(x + d[0], y + d[1], z + d[2]) - 1;
               if (nl > best) {
                  best = nl;
               }
            }
            fresh = best;
         }
         if (fresh > old) {
            this.writeCell(x, y, z, fresh, touched);
            ArrayDeque<int[]> queue = new ArrayDeque<>();
            queue.add(new int[]{x, y, z, fresh});
            this.propagateUp(queue, touched);
         } else if (fresh < old) {
            this.writeCell(x, y, z, fresh, touched);
            this.propagateDown(x, y, z, old, touched);
         }
      } finally {
         Profiler.pop();
      }
   }

   private void propagateUp(ArrayDeque<int[]> queue, HashSet<Long> touched) {
      while (!queue.isEmpty()) {
         int[] c = queue.poll();
         int nl = c[3] - 1;
         if (nl <= 0) {
            continue;
         }
         for (int[] d : Dirs.DIRS) {
            int nx = c[0] + d[0];
            int ny = c[1] + d[1];
            int nz = c[2] + d[2];
            if (ny < 0 || ny >= this.world.depth()) {
               continue;
            }
            if (this.world.isLightBlocker(nx, ny, nz)) {
               continue;
            }
            if (this.readCell(nx, ny, nz) >= nl) {
               continue;
            }
            this.writeCell(nx, ny, nz, nl, touched);
            queue.add(new int[]{nx, ny, nz, nl});
         }
      }
   }

   private void propagateDown(int x, int y, int z, int oldLevel, HashSet<Long> touched) {
      ArrayDeque<int[]> queue = new ArrayDeque<>();
      ArrayDeque<int[]> readd = new ArrayDeque<>();
      queue.add(new int[]{x, y, z, oldLevel});
      while (!queue.isEmpty()) {
         int[] c = queue.poll();
         for (int[] d : Dirs.DIRS) {
            int nx = c[0] + d[0];
            int ny = c[1] + d[1];
            int nz = c[2] + d[2];
            if (ny < 0 || ny >= this.world.depth()) {
               continue;
            }
            int nl = this.readCell(nx, ny, nz);
            if (nl == 0) {
               continue;
            }
            if (nl < c[3]) {
               this.writeCell(nx, ny, nz, 0, touched);
               queue.add(new int[]{nx, ny, nz, nl});
            } else {
               readd.add(new int[]{nx, ny, nz, nl});
            }
         }
      }
      while (!readd.isEmpty()) {
         int[] c = readd.poll();
         if (c[3] >= this.readCell(c[0], c[1], c[2])) {
            this.writeCell(c[0], c[1], c[2], c[3], touched);
            ArrayDeque<int[]> q = new ArrayDeque<>();
            q.add(c);
            this.propagateUp(q, touched);
         }
      }
   }

   private void notifyColumns(HashSet<Long> cols) {
      for (long k : cols) {
         this.world.lightColumnChanged((int)(k >> 32), keyZ(k), 0, this.world.depth());
      }
   }
}

