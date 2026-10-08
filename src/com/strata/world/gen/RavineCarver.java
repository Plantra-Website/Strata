package com.strata.world.gen;

import com.strata.core.Log;
import com.strata.core.MathHelper;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class RavineCarver {
   private static final int REGION = 32;
   private static final int REACH_CHUNKS = 8;
   private static final long STREAM_SALT = 0x5A17E5L;
   private final long seed;
   private final int depth;
   private final ConcurrentHashMap<Long, RegionCarves> regions = new ConcurrentHashMap<>();

   private record RegionCarves(LongLongMap lo, LongLongMap hi) {
   }

   public RavineCarver(long seed, int depth) {
      this.seed = seed;
      this.depth = depth;
   }

   private static long regionKey(int rx, int rz) {
      return ((long)rx << 32) | (rz & 0xFFFFFFFFL);
   }

   private static long columnKey(int x, int z) {
      return ((long)x << 32) | (z & 0xFFFFFFFFL);
   }

   public boolean isCarved(int x, int y, int z) {
      if (y < 1 || y >= this.depth) {
         return false;
      }
      int ccx = Math.floorDiv(x, 16);
      int ccz = Math.floorDiv(z, 16);
      int rx = Math.floorDiv(ccx, REGION);
      int rz = Math.floorDiv(ccz, REGION);
      final int frx = rx;
      final int frz = rz;
      long key = regionKey(rx, rz);
      RegionCarves rc = this.regions.get(key);
      if (rc == null) {
         rc = this.regions.computeIfAbsent(key, k -> {
            long t0 = System.nanoTime();
            RegionCarves r = this.carveRegion(frx, frz);
            long ms = (System.nanoTime() - t0) / 1000000L;
            if (ms > 100L) {
               Log.info("perf", "region ravine " + frx + "," + frz + " took " + ms + "ms"
                  + " [" + Thread.currentThread().getName() + "]");
            }
            return r;
         });
      }
      long mask = (y < 64 ? rc.lo() : rc.hi()).get(columnKey(x, z));
      return (mask & (1L << (y < 64 ? y : y - 64))) != 0L;
   }

   public int evictFar(int pcx, int pcz, int keepChunks) {
      int rx0 = Math.floorDiv(pcx - keepChunks, REGION);
      int rx1 = Math.floorDiv(pcx + keepChunks, REGION);
      int rz0 = Math.floorDiv(pcz - keepChunks, REGION);
      int rz1 = Math.floorDiv(pcz + keepChunks, REGION);
      int evicted = 0;
      var it = this.regions.entrySet().iterator();
      while (it.hasNext()) {
         long k = it.next().getKey();
         int rx = (int)(k >> 32);
         int rz = (int)(k & 0xFFFFFFFFL);
         if (rx < rx0 || rx > rx1 || rz < rz0 || rz > rz1) {
            it.remove();
            evicted++;
         }
      }
      return evicted;
   }

   private RegionCarves carveRegion(int rx, int rz) {
      LongLongMap lo = new LongLongMap(1 << 15);
      LongLongMap hi = new LongLongMap(1 << 12);
      Carver carver = new Carver(lo, hi, rx, rz);
      int minCx = rx * REGION - REACH_CHUNKS;
      int maxCx = rx * REGION + REGION + REACH_CHUNKS;
      int minCz = rz * REGION - REACH_CHUNKS;
      int maxCz = rz * REGION + REGION + REACH_CHUNKS;
      for (int ccx = minCx; ccx < maxCx; ccx++) {
         for (int ccz = minCz; ccz < maxCz; ccz++) {
            Random rand = new Random(this.seed ^ STREAM_SALT
               ^ (ccx * 341873128712L + ccz * 132897987541L));
            if (rand.nextInt(50) != 0) {
               continue;
            }
            double x = ccx * 16 + rand.nextInt(16);
            double y = rand.nextInt(rand.nextInt(40) + 8) + 20;
            double z = ccz * 16 + rand.nextInt(16);
            float yaw = rand.nextFloat() * (float)Math.PI * 2.0F;
            float pitch = (rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
            float width = (rand.nextFloat() * 2.0F + rand.nextFloat()) * 2.0F;
            carver.worm(new Random(rand.nextLong()), x, y, z, width, yaw, pitch);
         }
      }
      return new RegionCarves(lo, hi);
   }

   private class Carver {
      private final LongLongMap lo;
      private final LongLongMap hi;
      private final int rx;
      private final int rz;

      Carver(LongLongMap lo, LongLongMap hi, int rx, int rz) {
         this.lo = lo;
         this.hi = hi;
         this.rx = rx;
         this.rz = rz;
      }

      void worm(Random rand, double x, double y, double z, float width, float yaw, float pitch) {
         double originX = x;
         double originZ = z;
         float verticalDrift = 0.0F;
         float horizontalDrift = 0.0F;
         int maxSteps = 8 * 16 - 16 - rand.nextInt((8 * 16 - 16) / 4);
         float[] mult = new float[RavineCarver.this.depth];
         for (int i = 0; i < mult.length; i++) {
            if (i == 0 || rand.nextInt(3) == 0) {
               float w = 1.0F + rand.nextFloat() * rand.nextFloat() * 1.0F;
               mult[i] = w * w;
            }
         }
         for (int step = 0; step < maxSteps; step++) {
            double radius = 1.5D + Math.sin(step * Math.PI / maxSteps) * width;
            double vRadius = radius * 3.0D;
            radius *= rand.nextFloat() * 0.25D + 0.75D;
            vRadius *= rand.nextFloat() * 0.25D + 0.75D;
            float cosPitch = (float)Math.cos(pitch);
            float sinPitch = (float)Math.sin(pitch);
            x += Math.cos(yaw) * cosPitch;
            y += sinPitch;
            z += Math.sin(yaw) * cosPitch;
            pitch *= 0.7F;
            pitch += verticalDrift * 0.05F;
            yaw += horizontalDrift * 0.05F;
            verticalDrift = verticalDrift * 0.8F
               + (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat() * 2.0F;
            horizontalDrift = horizontalDrift * 0.5F
               + (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat() * 4.0F;

            if (rand.nextInt(4) != 0) {
               double dx = x - originX;
               double dz = z - originZ;
               double bound = maxSteps + width + 2.0F + 16.0F;
               if (dx * dx + dz * dz > bound * bound) {
                  return;
               }
               double reach = radius * 1.25 + 1.0;
               int regionX0 = this.rx * REGION * 16;
               int regionZ0 = this.rz * REGION * 16;
               if (x + reach >= regionX0 && x - reach < regionX0 + REGION * 16
                  && z + reach >= regionZ0 && z - reach < regionZ0 + REGION * 16) {
                  this.carveNode(x, y, z, radius, vRadius, mult);
               }
            }
         }
      }

      void carveNode(double x, double y, double z, double radius, double vRadius, float[] mult) {
         int depth = RavineCarver.this.depth;
         int x0 = MathHelper.floor(x - radius) - 1;
         int x1 = MathHelper.floor(x + radius) + 1;
         int z0 = MathHelper.floor(z - radius) - 1;
         int z1 = MathHelper.floor(z + radius) + 1;
         for (int cx = x0; cx <= x1; cx++) {
            int ccx = Math.floorDiv(cx, 16);
            if (ccx < this.rx * REGION || ccx >= this.rx * REGION + REGION) {
               continue;
            }
            double nx = (cx + 0.5 - x) / radius;
            double nxx = nx * nx;
            if (nxx >= 1.0) {
               continue;
            }
            for (int cz = z0; cz <= z1; cz++) {
               int ccz = Math.floorDiv(cz, 16);
               if (ccz < this.rz * REGION || ccz >= this.rz * REGION + REGION) {
                  continue;
               }
               double nz = (cz + 0.5 - z) / radius;
               double horizontal = nxx + nz * nz;
               if (horizontal >= 1.0) {
                  continue;
               }
               int cy1 = (int)Math.min(depth - 8, Math.floor(y + vRadius) + 1);
               int cy0 = (int)Math.max(1, Math.floor(y - vRadius) - 1);
               long loBits = 0L;
               long hiBits = 0L;
               for (int cy = cy1; cy >= cy0; cy--) {
                  double ny = (cy + 0.5 - y) / vRadius;
                  if (horizontal * mult[cy] + ny * ny / 6.0D < 1.0D) {
                     if (cy < 64) {
                        loBits |= 1L << cy;
                     } else {
                        hiBits |= 1L << (cy - 64);
                     }
                  }
               }
               long key = columnKey(cx, cz);
               if (loBits != 0L) {
                  this.lo.orBits(key, loBits);
               }
               if (hiBits != 0L) {
                  this.hi.orBits(key, hiBits);
               }
            }
         }
      }
   }
}

