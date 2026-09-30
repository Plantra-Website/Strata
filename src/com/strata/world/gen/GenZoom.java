package com.strata.world.gen;

public class GenZoom extends GenLayer {
   public GenZoom(long seed, GenLayer parent) {
      super(seed);
      this.parent = parent;
   }

   public static GenLayer stack(long seed, GenLayer layer, int n) {
      GenLayer out = layer;
      for (int i = 0; i < n; i++) {
         out = new GenZoom(seed + (long)i, out);
      }
      return out;
   }

   private int pick2(int a, int b) {
      return this.nextInt(2) == 0 ? a : b;
   }

   private int majority(int a, int b, int c, int d) {
      if (b == c && c == d) {
         return b;
      } else if (a == b && a == c) {
         return a;
      } else if (a == b && a == d) {
         return a;
      } else if (a == c && a == d) {
         return a;
      } else if (a == b && c != d) {
         return a;
      } else if (a == c && b != d) {
         return a;
      } else if (a == d && b != c) {
         return a;
      } else if (b == a && c != d) {
         return b;
      } else if (b == c && a != d) {
         return b;
      } else if (b == d && a != c) {
         return b;
      } else if (c == a && b != d) {
         return c;
      } else if (c == b && a != d) {
         return c;
      } else if (c == d && a != b) {
         return c;
      } else if (d == a && b != c) {
         return c;
      } else if (d == b && a != c) {
         return c;
      } else if (d == c && a != b) {
         return d;
      } else {
         int r = this.nextInt(4);
         return r == 0 ? a : (r == 1 ? b : (r == 2 ? c : d));
      }
   }

   @Override
   public int[] generate(int x, int z, int w, int h) {
      int px = x >> 1;
      int pz = z >> 1;
      int pw = (w >> 1) + 3;
      int ph = (h >> 1) + 3;
      int[] p = this.parent.generate(px, pz, pw, ph);
      int[] zoomed = new int[pw * 2 * ph * 2];
      int stride = pw << 1;
      for (int dz = 0; dz < ph - 1; dz++) {
         int row = dz << 1;
         int base = row * stride;
         int a = p[0 + (dz + 0) * pw];
         int b = p[0 + (dz + 1) * pw];
         for (int dx = 0; dx < pw - 1; dx++) {
            this.initCellSeed((dx + px << 1), (dz + pz << 1));
            int c = p[dx + 1 + (dz + 0) * pw];
            int d = p[dx + 1 + (dz + 1) * pw];
            zoomed[base] = a;
            zoomed[base++ + stride] = this.pick2(a, b);
            zoomed[base] = this.pick2(a, c);
            zoomed[base++ + stride] = this.majority(a, c, b, d);
            a = c;
            b = d;
         }
      }
      int[] out = new int[w * h];
      for (int dz = 0; dz < h; dz++) {
         System.arraycopy(zoomed, (dz + (z & 1)) * (pw << 1) + (x & 1), out, dz * w, w);
      }
      return out;
   }
}

