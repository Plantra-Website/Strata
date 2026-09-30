package com.strata.world.gen;

public class GenSmoothZoom extends GenLayer {
   public GenSmoothZoom(long seed, GenLayer parent) {
      super(seed);
      this.parent = parent;
   }

   public static GenLayer stack(long seed, GenLayer layer, int n) {
      GenLayer out = layer;
      for (int i = 0; i < n; i++) {
         out = new GenSmoothZoom(seed + (long)i, out);
      }
      return out;
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
            zoomed[base++ + stride] = a + (b - a) * this.nextInt(256) / 256;
            zoomed[base] = a + (c - a) * this.nextInt(256) / 256;
            int e = a + (c - a) * this.nextInt(256) / 256;
            int f = b + (d - b) * this.nextInt(256) / 256;
            zoomed[base++ + stride] = e + (f - e) * this.nextInt(256) / 256;
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

