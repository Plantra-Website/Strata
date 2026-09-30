package com.strata.world.gen;

public class GenZoomVoronoi extends GenLayer {
   public GenZoomVoronoi(long seed, GenLayer parent) {
      super(seed);
      this.parent = parent;
   }

   @Override
   public int[] generate(int x, int z, int w, int h) {
      x -= 2;
      z -= 2;
      int shift = 2;
      int cell = 1 << shift;
      int px = x >> shift;
      int pz = z >> shift;
      int pw = (w >> shift) + 3;
      int ph = (h >> shift) + 3;
      int[] p = this.parent.generate(px, pz, pw, ph);
      int stride = pw << shift;
      int[] zoomed = new int[stride * (ph << shift)];
      for (int dz = 0; dz < ph - 1; dz++) {
         int a = p[0 + (dz + 0) * pw];
         int b = p[0 + (dz + 1) * pw];
         for (int dx = 0; dx < pw - 1; dx++) {
            double jitter = (double)cell * 0.9D;
            this.initCellSeed((dx + px << shift), (dz + pz << shift));
            double ax = ((double)this.nextInt(1024) / 1024.0D - 0.5D) * jitter;
            double az = ((double)this.nextInt(1024) / 1024.0D - 0.5D) * jitter;
            this.initCellSeed((dx + px + 1 << shift), (dz + pz << shift));
            double bx = ((double)this.nextInt(1024) / 1024.0D - 0.5D) * jitter + (double)cell;
            double bz = ((double)this.nextInt(1024) / 1024.0D - 0.5D) * jitter;
            this.initCellSeed((dx + px << shift), (dz + pz + 1 << shift));
            double cx = ((double)this.nextInt(1024) / 1024.0D - 0.5D) * jitter;
            double cz = ((double)this.nextInt(1024) / 1024.0D - 0.5D) * jitter + (double)cell;
            this.initCellSeed((dx + px + 1 << shift), (dz + pz + 1 << shift));
            double dx2 = ((double)this.nextInt(1024) / 1024.0D - 0.5D) * jitter + (double)cell;
            double dz2 = ((double)this.nextInt(1024) / 1024.0D - 0.5D) * jitter + (double)cell;
            int c = p[dx + 1 + (dz + 0) * pw];
            int d = p[dx + 1 + (dz + 1) * pw];
            for (int iz = 0; iz < cell; iz++) {
               int base = ((dz << shift) + iz) * stride + (dx << shift);
               for (int ix = 0; ix < cell; ix++) {
                  double da = ((double)iz - az) * ((double)iz - az) + ((double)ix - ax) * ((double)ix - ax);
                  double db = ((double)iz - bz) * ((double)iz - bz) + ((double)ix - bx) * ((double)ix - bx);
                  double dc = ((double)iz - cz) * ((double)iz - cz) + ((double)ix - cx) * ((double)ix - cx);
                  double dd = ((double)iz - dz2) * ((double)iz - dz2) + ((double)ix - dx2) * ((double)ix - dx2);
                  if (da < db && da < dc && da < dd) {
                     zoomed[base++] = a;
                  } else if (db < da && db < dc && db < dd) {
                     zoomed[base++] = c;
                  } else if (dc < da && dc < db && dc < dd) {
                     zoomed[base++] = b;
                  } else {
                     zoomed[base++] = d;
                  }
               }
            }
            a = c;
            b = d;
         }
      }
      int[] out = new int[w * h];
      for (int dz = 0; dz < h; dz++) {
         System.arraycopy(zoomed, (dz + (z & cell - 1)) * (pw << shift) + (x & cell - 1), out, dz * w, w);
      }
      return out;
   }
}

