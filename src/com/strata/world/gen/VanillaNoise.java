package com.strata.world.gen;

public class VanillaNoise {
   private final int[] permutations = new int[512];
   private final double xOffset;
   private final double yOffset;
   private final double zOffset;

   public VanillaNoise(java.util.Random rand) {
      this.xOffset = rand.nextDouble() * 256.0D;
      this.yOffset = rand.nextDouble() * 256.0D;
      this.zOffset = rand.nextDouble() * 256.0D;
      for (int i = 0; i < 256; i++) {
         this.permutations[i] = i;
      }
      for (int i = 0; i < 256; i++) {
         int j = rand.nextInt(256 - i) + i;
         int t = this.permutations[i];
         this.permutations[i] = this.permutations[j];
         this.permutations[j] = t;
         this.permutations[i + 256] = this.permutations[i];
      }
   }

   private static double lerp(double t, double a, double b) {
      return a + t * (b - a);
   }

   private static double fade(double t) {
      return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
   }

   private static double grad2(int hash, double x, double z) {
      int h = hash & 15;
      double ux = (double)(1 - ((h & 8) >> 3)) * x;
      double uz = h < 4 ? 0.0D : (h != 12 && h != 14 ? z : x);
      return ((h & 1) == 0 ? ux : -ux) + ((h & 2) == 0 ? uz : -uz);
   }

   private static double grad3(int hash, double x, double y, double z) {
      int h = hash & 15;
      double u = h < 8 ? x : y;
      double v = h < 4 ? y : (h != 12 && h != 14 ? z : x);
      return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
   }

   void sample(double[] buf, double x, double y, double z, int xSize, int ySize, int zSize,
         double xFreq, double yFreq, double zFreq, double amplitude) {
      double invAmp = 1.0D / amplitude;
      if (ySize == 1) {
         int i = 0;
         for (int ix = 0; ix < xSize; ix++) {
            double px = x + (double)ix * xFreq + this.xOffset;
            int lx = (int)px;
            if (px < (double)lx) {
               lx--;
            }
            int hx = lx & 255;
            px -= (double)lx;
            double fx = fade(px);
            for (int iz = 0; iz < zSize; iz++) {
               double pz = z + (double)iz * zFreq + this.zOffset;
               int lz = (int)pz;
               if (pz < (double)lz) {
                  lz--;
               }
               int hz = lz & 255;
               pz -= (double)lz;
               double fz = fade(pz);
               int h00 = this.permutations[hx] + 0;
               int hh00 = this.permutations[h00] + hz;
               int h10 = this.permutations[hx + 1] + 0;
               int hh10 = this.permutations[h10] + hz;
               double n00 = lerp(fx, grad2(this.permutations[hh00], px, pz),
                  grad3(this.permutations[hh10], px - 1.0D, 0.0D, pz));
               double n01 = lerp(fx, grad3(this.permutations[hh00 + 1], px, 0.0D, pz - 1.0D),
                  grad3(this.permutations[hh10 + 1], px - 1.0D, 0.0D, pz - 1.0D));
               buf[i++] += lerp(fz, n00, n01) * invAmp;
            }
         }
         return;
      }
      int i = 0;
      int lastY = -1;
      double n00y0 = 0.0D, n10y0 = 0.0D, n00y1 = 0.0D, n10y1 = 0.0D;
      for (int ix = 0; ix < xSize; ix++) {
         double px = x + (double)ix * xFreq + this.xOffset;
         int lx = (int)px;
         if (px < (double)lx) {
            lx--;
         }
         int hx = lx & 255;
         px -= (double)lx;
         double fx = fade(px);
         for (int iz = 0; iz < zSize; iz++) {
            double pz = z + (double)iz * zFreq + this.zOffset;
            int lz = (int)pz;
            if (pz < (double)lz) {
               lz--;
            }
            int hz = lz & 255;
            pz -= (double)lz;
            double fz = fade(pz);
            for (int iy = 0; iy < ySize; iy++) {
               double py = y + (double)iy * yFreq + this.yOffset;
               int ly = (int)py;
               if (py < (double)ly) {
                  ly--;
               }
               int hy = ly & 255;
               py -= (double)ly;
               double fy = fade(py);
               if (iy == 0 || hy != lastY) {
                  lastY = hy;
                  int h00 = this.permutations[hx] + hy;
                  int hh00 = this.permutations[h00] + hz;
                  int hh01 = this.permutations[h00 + 1] + hz;
                  int h10 = this.permutations[hx + 1] + hy;
                  int hh10 = this.permutations[h10] + hz;
                  int hh11 = this.permutations[h10 + 1] + hz;
                  n00y0 = lerp(fx, grad3(this.permutations[hh00], px, py, pz),
                     grad3(this.permutations[hh10], px - 1.0D, py, pz));
                  n10y0 = lerp(fx, grad3(this.permutations[hh01], px, py - 1.0D, pz),
                     grad3(this.permutations[hh11], px - 1.0D, py - 1.0D, pz));
                  n00y1 = lerp(fx, grad3(this.permutations[hh00 + 1], px, py, pz - 1.0D),
                     grad3(this.permutations[hh10 + 1], px - 1.0D, py, pz - 1.0D));
                  n10y1 = lerp(fx, grad3(this.permutations[hh01 + 1], px, py - 1.0D, pz - 1.0D),
                     grad3(this.permutations[hh11 + 1], px - 1.0D, py - 1.0D, pz - 1.0D));
               }
               double nx0 = lerp(fy, n00y0, n10y0);
               double nx1 = lerp(fy, n00y1, n10y1);
               buf[i++] += lerp(fz, nx0, nx1) * invAmp;
            }
         }
      }
   }
}

