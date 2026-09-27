package com.strata.world.light;

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
   private static final int[][] DIRS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

   public SkyLightEngine(LightWorld world) {
      this.world = world;
   }

   private static long columnKey(int x, int z) {
      return ((long)x << 32) | (z & 0xFFFFFFFFL);
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
      HashSet<Long> touched = new HashSet<>();
      for (int lx = 0; lx < 16; lx++) {
         for (int lz = 0; lz < 16; lz++) {
            int x = ccx * 16 + lx;
            int z = ccz * 16 + lz;
            int h = this.opaqueHeight(x, z);
            int m = h;
            m = Math.min(m, this.opaqueHeight(x - 1, z));
            m = Math.min(m, this.opaqueHeight(x + 1, z));
            m = Math.min(m, this.opaqueHeight(x, z - 1));
            m = Math.min(m, this.opaqueHeight(x, z + 1));
            this.checkSkyline(x, z, m, touched);
            this.checkSkyline(x - 1, z, h, touched);
            this.checkSkyline(x + 1, z, h, touched);
            this.checkSkyline(x, z - 1, h, touched);
            this.checkSkyline(x, z + 1, h, touched);
         }
      }
      this.notifyColumns(touched);
   }

   private void checkSkyline(int x, int z, int other, HashSet<Long> touched) {
      int h = this.opaqueHeight(x, z);
      if (h > other) {
         for (int y = other; y <= h; y++) {
            this.updateSkyAt(x, y, z, touched);
         }
      } else if (h < other) {
         for (int y = h; y <= other; y++) {
            this.updateSkyAt(x, y, z, touched);
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
            for (int[] d : DIRS) {
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
         for (int[] d : DIRS) {
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
         for (int[] d : DIRS) {
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
         this.world.lightColumnChanged((int)(k >> 32), (int)(k & 0xFFFFFFFFL), 0, this.world.depth());
      }
   }
}

