package com.strata.world.gen;

import com.strata.core.MathHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Noise {
   private final int[] p = new int[512];

   public Noise(long seed) {
      List<Integer> perm = new ArrayList<>(256);
      for (int i = 0; i < 256; i++) {
         perm.add(i);
      }
      Collections.shuffle(perm, new Random(seed));
      for (int i = 0; i < 512; i++) {
         this.p[i] = perm.get(i & 255);
      }
   }

   private static double fade(double t) {
      return t * t * t * (t * (t * 6 - 15) + 10);
   }

   private static double lerp(double a, double b, double t) {
      return a + t * (b - a);
   }

   private static double grad(int hash, double x, double y, double z) {
      int h = hash & 15;
      double u = h < 8 ? x : y;
      double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
      return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
   }

   public double noise3(double x, double y, double z) {
      int xi = MathHelper.floor(x) & 255;
      int yi = MathHelper.floor(y) & 255;
      int zi = MathHelper.floor(z) & 255;
      double xf = x - Math.floor(x);
      double yf = y - Math.floor(y);
      double zf = z - Math.floor(z);
      double u = fade(xf);
      double v = fade(yf);
      double w = fade(zf);
      int aaa = this.p[this.p[this.p[xi] + yi] + zi];
      int aba = this.p[this.p[this.p[xi] + yi + 1] + zi];
      int aab = this.p[this.p[this.p[xi] + yi] + zi + 1];
      int abb = this.p[this.p[this.p[xi] + yi + 1] + zi + 1];
      int baa = this.p[this.p[this.p[xi + 1] + yi] + zi];
      int bba = this.p[this.p[this.p[xi + 1] + yi + 1] + zi];
      int bab = this.p[this.p[this.p[xi + 1] + yi] + zi + 1];
      int bbb = this.p[this.p[this.p[xi + 1] + yi + 1] + zi + 1];
      return lerp(
         lerp(lerp(grad(aaa, xf, yf, zf), grad(baa, xf - 1, yf, zf), u),
              lerp(grad(aba, xf, yf - 1, zf), grad(bba, xf - 1, yf - 1, zf), u), v),
         lerp(lerp(grad(aab, xf, yf, zf - 1), grad(bab, xf - 1, yf, zf - 1), u),
              lerp(grad(abb, xf, yf - 1, zf - 1), grad(bbb, xf - 1, yf - 1, zf - 1), u), v), w);
   }

   public double noise2(double x, double y) {
      return this.noise3(x, y, 0.0);
   }

   public double fbm2(double x, double y, int octaves) {
      double total = 0.0;
      double amp = 0.5;
      double freq = 1.0;
      double norm = 0.0;
      for (int i = 0; i < octaves; i++) {
         total += this.noise2(x * freq, y * freq) * amp;
         norm += amp;
         amp *= 0.5;
         freq *= 2.0;
      }
      return total / norm;
   }
}

