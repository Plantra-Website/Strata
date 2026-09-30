package com.strata.world.gen;

import com.strata.core.Log;
import com.strata.core.MathHelper;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class CaveCarver {
   private static final int REGION = 32;
   private static final int REACH_CHUNKS = 10;
   private final long seed;
   private final int depth;
   private final ConcurrentHashMap<Long, RegionCarves> regions = new ConcurrentHashMap<>();

   private record RegionCarves(LongLongMap masks) {
   }

   public CaveCarver(long seed, int depth) {
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
                Log.info("perf", "region carve " + frx + "," + frz + " took " + ms + "ms"
                   + " [" + Thread.currentThread().getName() + "]");
             }
             return r;
          });
       }
       long mask = rc.masks.get(columnKey(x, z));
       return (mask & (1L << y)) != 0L;
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
       LongLongMap masks = new LongLongMap(1 << 15);
       Caver caver = new Caver(masks, rx, rz);
      int minCx = rx * REGION - REACH_CHUNKS;
      int maxCx = rx * REGION + REGION + REACH_CHUNKS;
      int minCz = rz * REGION - REACH_CHUNKS;
      int maxCz = rz * REGION + REGION + REACH_CHUNKS;
      for (int ccx = minCx; ccx < maxCx; ccx++) {
         for (int ccz = minCz; ccz < maxCz; ccz++) {
            Random rand = new Random(this.seed ^ (ccx * 341873128712L + ccz * 132897987541L));
            int systems = rand.nextInt(rand.nextInt(rand.nextInt(40) + 1) + 1);
            if (rand.nextInt(15) != 0) {
               systems = 0;
            }
            for (int s = 0; s < systems; s++) {
               double x = ccx * 16 + rand.nextInt(16);
               double y = rand.nextInt(rand.nextInt(this.depth - 8) + 8);
               double z = ccz * 16 + rand.nextInt(16);
               int worms = 1;
               if (rand.nextInt(4) == 0) {
                  long nodeSeed = rand.nextLong();
                  float roomWidth = 1.0F + rand.nextFloat() * 6.0F;
                  caver.largeNode(nodeSeed, x, y, z, roomWidth);
                  worms += rand.nextInt(4);
               }
               for (int w = 0; w < worms; w++) {
                  float yaw = rand.nextFloat() * (float)Math.PI * 2.0F;
                  float pitch = (rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
                  float width = rand.nextFloat() * 2.0F + rand.nextFloat();
                  if (rand.nextInt(10) == 0) {
                     width *= rand.nextFloat() * rand.nextFloat() * 3.0F + 1.0F;
                  }
                  caver.node(new Random(rand.nextLong()), x, y, z, width, yaw, pitch, 0, 0, 1.0);
               }
            }
         }
      }
      return new RegionCarves(masks);
   }

    private class Caver {
       private final LongLongMap masks;
       private final int rx;
       private final int rz;

       Caver(LongLongMap masks, int rx, int rz) {
         this.masks = masks;
         this.rx = rx;
         this.rz = rz;
      }

      void largeNode(long nodeSeed, double x, double y, double z, float width) {
         this.node(new Random(nodeSeed), x, y, z, width, 0.0F, 0.0F, -1, -1, 0.5);
      }

      void node(Random rand, double x, double y, double z, float width, float yaw, float pitch, int step, int maxSteps, double squash) {
         double originX = x;
         double originZ = z;
         float verticalDrift = 0.0F;
         float horizontalDrift = 0.0F;
         if (maxSteps <= 0) {
            int range = 8 * 16 - 16; 
            maxSteps = range - rand.nextInt(range / 4);
         }
         boolean single = false;
         if (step == -1) {
            step = maxSteps / 2;
            single = true;
         }
         int branchAt = rand.nextInt(maxSteps / 2) + maxSteps / 4;
         boolean flatChance = rand.nextInt(6) == 0;

         for (; step < maxSteps; step++) {
            double radius = 1.5D + Math.sin(step * Math.PI / maxSteps) * width;
            double vRadius = radius * squash;
            float cosPitch = (float)Math.cos(pitch);
            float sinPitch = (float)Math.sin(pitch);
            x += Math.cos(yaw) * cosPitch;
            y += sinPitch;
            z += Math.sin(yaw) * cosPitch;
            if (flatChance) {
               pitch *= 0.92F;
            } else {
               pitch *= 0.7F;
            }
            pitch += verticalDrift * 0.1F;
            yaw += horizontalDrift * 0.1F;
            verticalDrift = verticalDrift * 0.9F
               + (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat() * 2.0F;
            horizontalDrift = horizontalDrift * 0.75F
               + (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat() * 4.0F;

            if (!single && step == branchAt && width > 1.0F) {
               this.node(new Random(rand.nextLong()), x, y, z,
                  rand.nextFloat() * 0.5F + 0.5F, yaw - (float)Math.PI * 0.5F, pitch / 3.0F, step, maxSteps, 1.0);
               this.node(new Random(rand.nextLong()), x, y, z,
                  rand.nextFloat() * 0.5F + 0.5F, yaw + (float)Math.PI * 0.5F, pitch / 3.0F, step, maxSteps, 1.0);
               return;
            }

            if (single || rand.nextInt(4) != 0) {
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
                  this.carveNode(x, y, z, radius, vRadius);
               }
               if (single) {
                  break;
               }
            }
         }
   }

      void carveNode(double x, double y, double z, double radius, double vRadius) {
         int depth = CaveCarver.this.depth;
         int x0 = MathHelper.floor(x - radius) - 1;
         int x1 = MathHelper.floor(x + radius) + 1;
         int y0 = MathHelper.floor(y - vRadius) - 1;
         int y1 = MathHelper.floor(y + vRadius) + 1;
         int z0 = MathHelper.floor(z - radius) - 1;
         int z1 = MathHelper.floor(z + radius) + 1;
         if (y0 < 1) {
            y0 = 1;
         }
         if (y1 > depth - 8) {
            y1 = depth - 8;
         }
         for (int cx = x0; cx <= x1; cx++) {
            int ccx = Math.floorDiv(cx, 16);
            if (ccx < this.rx * REGION || ccx >= this.rx * REGION + REGION) {
               continue;
            }
            double nx = (cx + 0.5 - x) / radius;
            if (nx * nx >= 1.0) {
               continue;
            }
            for (int cz = z0; cz <= z1; cz++) {
               int ccz = Math.floorDiv(cz, 16);
               if (ccz < this.rz * REGION || ccz >= this.rz * REGION + REGION) {
                  continue;
               }
               double nz = (cz + 0.5 - z) / radius;
               if (nx * nx + nz * nz >= 1.0) {
                  continue;
               }
                for (int cy = y1; cy >= y0; cy--) {
                   double ny = (cy + 0.5 - y) / vRadius;
                   if (ny > -0.7D && nx * nx + ny * ny + nz * nz < 1.0D) {
                      this.masks.orBits(columnKey(cx, cz), 1L << cy);
                   }
                }
            }
         }
      }
   }
}

