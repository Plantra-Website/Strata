package com.strata.world.gen;

public class GenZoomFuzzy extends GenLayer {
   public GenZoomFuzzy(long seed, GenLayer parent) {
      super(seed);
      this.parent = parent;
   }

   private int choose(int a, int b) {
      return this.nextInt(2) == 0 ? a : b;
   }

   private int choose(int a, int b, int c, int d) {
      int r = this.nextInt(4);
      return r == 0 ? a : (r == 1 ? b : (r == 2 ? c : d));
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
            zoomed[base++ + stride] = this.choose(a, b);
            zoomed[base] = this.choose(a, c);
            zoomed[base++ + stride] = this.choose(a, c, b, d);
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

