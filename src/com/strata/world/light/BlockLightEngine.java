package com.strata.world.light;

import com.strata.core.Profiler;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class BlockLightEngine {
   private final LightWorld world;
   private final ConcurrentHashMap<Long, byte[]> levels = new ConcurrentHashMap<>();
   private static final int[][] DIRS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

   public BlockLightEngine(LightWorld world) {
      this.world = world;
   }

   private static long key(int x, int z) {
      return ((long)x << 32) | (z & 0xFFFFFFFFL);
   }

   public int get(int x, int y, int z) {
      if (y < 0 || y >= this.world.depth()) {
         return 0;
      }
      byte[] arr = this.levels.get(key(x, z));
      return arr == null ? 0 : (arr[y] & 15);
   }

   private void set(int x, int y, int z, int level) {
      if (y < 0 || y >= this.world.depth()) {
         return;
      }
      long k = key(x, z);
      if (level <= 0) {
         byte[] arr = this.levels.get(k);
         if (arr != null) {
            arr[y] = 0;
         }
         return;
      }
      byte[] arr = this.levels.get(k);
      if (arr == null) {
         byte[] fresh = new byte[this.world.depth()];
         byte[] prev = this.levels.putIfAbsent(k, fresh);
         arr = (prev != null) ? prev : fresh;
      }
      arr[y] = (byte)(level & 15);
   }

   public void floodAdd(int x, int y, int z, int level) {
      if (level <= 0) {
         return;
      }
      Profiler.push("flood");
      try {
         this.floodAddInner(x, y, z, level);
      } finally {
         Profiler.pop();
      }
   }

   private void floodAddInner(int x, int y, int z, int level) {
      HashSet<Long> touched = new HashSet<>();
      if (level > this.get(x, y, z)) {
         this.set(x, y, z, level);
         touched.add(key(x, z));
      }
      ArrayDeque<int[]> queue = new ArrayDeque<>();
      queue.add(new int[]{x, y, z, level});
      this.propagateAdd(queue, touched);
      this.notifyColumns(touched);
   }

   private void propagateAdd(ArrayDeque<int[]> queue, HashSet<Long> touched) {
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
            if (this.get(nx, ny, nz) >= nl) {
               continue;
            }
            this.set(nx, ny, nz, nl);
            touched.add(key(nx, nz));
            queue.add(new int[]{nx, ny, nz, nl});
         }
      }
   }

   public void floodRemove(int x, int y, int z, int oldLevel) {
      Profiler.push("flood");
      try {
         this.floodRemoveInner(x, y, z, oldLevel);
      } finally {
         Profiler.pop();
      }
   }

   private void floodRemoveInner(int x, int y, int z, int oldLevel) {
      HashSet<Long> touched = new HashSet<>();
      ArrayDeque<int[]> queue = new ArrayDeque<>();
      ArrayDeque<int[]> readd = new ArrayDeque<>();
      this.set(x, y, z, 0);
      touched.add(key(x, z));
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
            int nl = this.get(nx, ny, nz);
            if (nl == 0) {
               continue;
            }
            touched.add(key(nx, nz));
            if (nl < c[3]) {
               this.set(nx, ny, nz, 0);
               queue.add(new int[]{nx, ny, nz, nl});
            } else {
               readd.add(new int[]{nx, ny, nz, nl});
            }
         }
      }
      while (!readd.isEmpty()) {
         int[] c = readd.poll();
         if (c[3] >= this.get(c[0], c[1], c[2])) {
            this.set(c[0], c[1], c[2], c[3]);
            touched.add(key(c[0], c[2]));
            ArrayDeque<int[]> q = new ArrayDeque<>();
            q.add(c);
            this.propagateAdd(q, touched);
         }
      }
      this.notifyColumns(touched);
   }

   private void notifyColumns(HashSet<Long> cols) {
      for (long k : cols) {
         int x = (int)(k >> 32);
         int z = (int)(k & 0xFFFFFFFFL);
         this.world.lightColumnChanged(x, z, 0, this.world.depth());
      }
   }

   public void clear() {
      this.levels.clear();
   }
}

