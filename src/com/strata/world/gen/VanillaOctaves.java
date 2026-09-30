package com.strata.world.gen;

public class VanillaOctaves {
   private final VanillaNoise[] octaves;

   public VanillaOctaves(java.util.Random rand, int octaves) {
      this.octaves = new VanillaNoise[octaves];
      for (int i = 0; i < octaves; i++) {
         this.octaves[i] = new VanillaNoise(rand);
      }
   }

   private static long floorLong(double v) {
      long l = (long)v;
      return v < (double)l ? l - 1L : l;
   }

   public double[] generate(double[] buf, int x, int y, int z, int xSize, int ySize, int zSize,
         double xFreq, double yFreq, double zFreq) {
      if (buf == null) {
         buf = new double[xSize * ySize * zSize];
      } else {
         for (int i = 0; i < buf.length; i++) {
            buf[i] = 0.0D;
         }
      }
      double amp = 1.0D;
      for (int i = 0; i < this.octaves.length; i++) {
         double sx = (double)x * amp * xFreq;
         double sy = (double)y * amp * yFreq;
         double sz = (double)z * amp * zFreq;
         long ix = floorLong(sx);
         long iz = floorLong(sz);
         sx -= (double)ix;
         sz -= (double)iz;
         ix %= 16777216L;
         iz %= 16777216L;
         sx += (double)ix;
         sz += (double)iz;
         this.octaves[i].sample(buf, sx, sy, sz, xSize, ySize, zSize,
            xFreq * amp, yFreq * amp, zFreq * amp, amp);
         amp /= 2.0D;
      }
      return buf;
   }

   public double[] generate2D(double[] buf, int x, int z, int xSize, int zSize, double xScale, double zScale) {
      return this.generate(buf, x, 10, z, xSize, 1, zSize, xScale, 1.0D, zScale);
   }
}

