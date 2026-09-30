package com.strata.world.gen;

import com.strata.blocks.Blocks;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class GenChunkFill {
   private final long seed;
   private final int depth;
   private final VanillaOctaves gen1;
   private final VanillaOctaves gen2;
   private final VanillaOctaves gen3;
   private final VanillaOctaves gen4;
   private final VanillaOctaves gen5;
   private final VanillaOctaves gen6;
   private final GenLayer coarseHead;
   private final GenLayer voronoiHead;
   private final ThreadLocal<Random> rand = new ThreadLocal<Random>() {
      @Override protected Random initialValue() {
         return new Random();
      }
   };
   private final ConcurrentHashMap<Long, byte[]> fills = new ConcurrentHashMap<>();
   private static final float[] KERNEL = new float[25];

   static {
      for (int dx = -2; dx <= 2; dx++) {
         for (int dz = -2; dz <= 2; dz++) {
            KERNEL[dx + 2 + (dz + 2) * 5] =
               10.0F / (float)Math.sqrt(dx * dx + dz * dz + 0.2F);
         }
      }
   }

   public GenChunkFill(long seed, int depth, VanillaOctaves gen1, VanillaOctaves gen2,
         VanillaOctaves gen3, VanillaOctaves gen4, VanillaOctaves gen5, VanillaOctaves gen6,
         GenLayer coarseHead, GenLayer voronoiHead) {
      this.seed = seed;
      this.depth = depth;
      this.gen1 = gen1;
      this.gen2 = gen2;
      this.gen3 = gen3;
      this.gen4 = gen4;
      this.gen5 = gen5;
      this.gen6 = gen6;
      this.coarseHead = coarseHead;
      this.voronoiHead = voronoiHead;
   }

   public int evictFar(int pcx, int pcz, int keepChunks) {
      int evicted = 0;
      var it = this.fills.entrySet().iterator();
      while (it.hasNext()) {
         long k = it.next().getKey();
         int cx = (int)(k >> 32);
         int cz = (int)(k & 0xFFFFFFFFL);
         if (Math.abs(cx - pcx) > keepChunks || Math.abs(cz - pcz) > keepChunks) {
            it.remove();
            evicted++;
         }
      }
      var iv = this.veinCache.entrySet().iterator();
      while (iv.hasNext()) {
         long k = iv.next().getKey();
         int cx = (int)(k >> 32);
         int cz = (int)(k & 0xFFFFFFFFL);
         if (Math.abs(cx - pcx) > keepChunks || Math.abs(cz - pcz) > keepChunks) {
            iv.remove();
            evicted++;
         }
      }
      return evicted;
   }

   public byte[] fill(int ccx, int ccz) {
      long key = ((long)ccx << 32) | (ccz & 0xFFFFFFFFL);
      byte[] hit = this.fills.get(key);
      if (hit != null) {
         return hit;
      }
      return this.fills.computeIfAbsent(key, k -> this.build(ccx, ccz));
   }

   private int index(int x, int z, int y) {
      return (x * 16 + z) * this.depth + y;
   }

   private byte[] build(int ccx, int ccz) {
      int yCells = this.depth / 8;
      int ySize = yCells + 1;
      byte[] cells = new byte[16 * this.depth * 16];
      double[] density = this.densityField(ccx, ccz, ySize);
      for (int cx = 0; cx < 4; cx++) {
         for (int cz = 0; cz < 4; cz++) {
            for (int cy = 0; cy < yCells; cy++) {
               double d000 = density[((cx + 0) * 5 + cz + 0) * ySize + cy + 0];
               double d001 = density[((cx + 0) * 5 + cz + 1) * ySize + cy + 0];
               double d100 = density[((cx + 1) * 5 + cz + 0) * ySize + cy + 0];
               double d101 = density[((cx + 1) * 5 + cz + 1) * ySize + cy + 0];
               double y00 = d000;
               double y01 = d001;
               double y10 = d100;
               double y11 = d101;
               double y00n = (density[((cx + 0) * 5 + cz + 0) * ySize + cy + 1] - d000) * 0.125D;
               double y01n = (density[((cx + 0) * 5 + cz + 1) * ySize + cy + 1] - d001) * 0.125D;
               double y10n = (density[((cx + 1) * 5 + cz + 0) * ySize + cy + 1] - d100) * 0.125D;
               double y11n = (density[((cx + 1) * 5 + cz + 1) * ySize + cy + 1] - d101) * 0.125D;
               for (int sy = 0; sy < 8; sy++) {
                  double x0 = y00;
                  double x1 = y01;
                  double dxx0 = (y10 - y00) * 0.25D;
                  double dxx1 = (y11 - y01) * 0.25D;
                  for (int sx = 0; sx < 4; sx++) {
                     double v = x0 - (x1 - x0) * 0.25D;
                     double dzd = (x1 - x0) * 0.25D;
                     for (int sz = 0; sz < 4; sz++) {
                        v += dzd;
                        int x = sx + cx * 4;
                        int z = sz + cz * 4;
                        int y = cy * 8 + sy;
                        if (v > 0.0D) {
                           cells[this.index(x, z, y)] = (byte)Blocks.STONE_ID;
                        } else if (y < TerrainGenerator.SEA_LEVEL) {
                           cells[this.index(x, z, y)] = (byte)Blocks.WATER_ID;
                        }
                     }
                     x0 += dxx0;
                     x1 += dxx1;
                  }
                  y00 += y00n;
                  y01 += y01n;
                  y10 += y10n;
                  y11 += y11n;
               }
            }
         }
      }
      this.paint(ccx, ccz, cells);
      this.stampVeins(ccx, ccz, cells);
      return cells;
   }

   static final class Vein {
      final int id, size, cx, cy, cz;
      final long seed;
      Vein(int id, int size, int cx, int cy, int cz, long seed) {
         this.id = id;
         this.size = size;
         this.cx = cx;
         this.cy = cy;
         this.cz = cz;
         this.seed = seed;
      }
   }

   private final ConcurrentHashMap<Long, java.util.ArrayList<Vein>> veinCache = new ConcurrentHashMap<>();

   java.util.ArrayList<Vein> veinsForChunk(int ccx, int ccz) {
      long key = ((long)ccx << 32) | (ccz & 0xFFFFFFFFL);
      java.util.ArrayList<Vein> hit = this.veinCache.get(key);
      if (hit != null) {
         return hit;
      }
      java.util.ArrayList<Vein> veins = new java.util.ArrayList<>(82);
      this.rollVeins(veins, ccx, ccz, Blocks.DIRT_ID, 32, 20, 0, 128, 0xD12701L, false);
      this.rollVeins(veins, ccx, ccz, Blocks.GRAVEL_ID, 32, 10, 0, 128, 0x62A9E1L, false);
      this.rollVeins(veins, ccx, ccz, Blocks.COAL_ID, 16, 20, 0, 128, 0xC0A101L, false);
      this.rollVeins(veins, ccx, ccz, Blocks.IRON_ID, 8, 20, 0, 64, 0x120001L, false);
      this.rollVeins(veins, ccx, ccz, Blocks.GOLD_ID, 8, 2, 0, 32, 0x601D01L, false);
      this.rollVeins(veins, ccx, ccz, Blocks.REDSTONE_ORE_ID, 7, 8, 0, 16, 0x2ED501L, false);
      this.rollVeins(veins, ccx, ccz, Blocks.DIAMOND_ID, 7, 1, 0, 16, 0xD1A401L, false);
      this.rollVeins(veins, ccx, ccz, Blocks.LAPIS_ID, 6, 1, 0, 0, 0x1AB501L, true);
      this.veinCache.put(key, veins);
      return veins;
   }

   private void rollVeins(java.util.ArrayList<Vein> veins, int ccx, int ccz,
         int id, int size, int count, int yMin, int yMax, long salt, boolean triangular) {
      for (int n = 0; n < count; n++) {
         long vs = this.seed ^ (salt + (long)n * 0x9E3779B9L)
            ^ ((long)ccx * 341873128712L + (long)ccz * 132897987541L);
         Random rand = new Random(vs);
         int cx = ccx * 16 + rand.nextInt(16);
         int cz = ccz * 16 + rand.nextInt(16);
         int cy = triangular ? rand.nextInt(16) + rand.nextInt(16)
            : yMin + rand.nextInt(yMax - yMin);
         veins.add(new Vein(id, size, cx, cy, cz, vs ^ 0x5EED01L));
      }
   }

   private void stampVeins(int ccx, int ccz, byte[] cells) {
      int x0 = ccx * 16, x1 = x0 + 15, z0 = ccz * 16, z1 = z0 + 15;
      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            for (Vein vein : this.veinsForChunk(ccx + dx, ccz + dz)) {
               int reach = vein.size / 4 + 1;
               if (vein.cx + reach < x0 || vein.cx - reach > x1
                  || vein.cz + reach < z0 || vein.cz - reach > z1) {
                  continue;
               }
               this.stampVein(ccx, ccz, cells, vein);
            }
         }
      }
   }

   private void stampVein(int ccx, int ccz, byte[] cells, Vein vein) {
      Random rand = new Random(vein.seed);
      float azimuth = rand.nextFloat() * (float)Math.PI;
      double x0 = vein.cx + Math.sin(azimuth) * vein.size / 8.0F;
      double x1 = vein.cx - Math.sin(azimuth) * vein.size / 8.0F;
      double z0 = vein.cz + Math.cos(azimuth) * vein.size / 8.0F;
      double z1 = vein.cz - Math.cos(azimuth) * vein.size / 8.0F;
      double y0 = vein.cy + rand.nextInt(3) - 2;
      double y1 = vein.cy + rand.nextInt(3) - 2;
      for (int i = 0; i <= vein.size; i++) {
         double px = x0 + (x1 - x0) * i / vein.size;
         double py = y0 + (y1 - y0) * i / vein.size;
         double pz = z0 + (z1 - z0) * i / vein.size;
         double spread = rand.nextDouble() * vein.size / 16.0D;
         double rxz = (Math.sin(i * Math.PI / vein.size) + 1.0D) * spread + 1.0D;
         double ry = (Math.sin(i * Math.PI / vein.size) + 1.0D) * spread + 1.0D;
         int bx0 = floorCell(px - rxz / 2.0D);
         int by0 = floorCell(py - ry / 2.0D);
         int bz0 = floorCell(pz - rxz / 2.0D);
         int bx1 = floorCell(px + rxz / 2.0D);
         int by1 = floorCell(py + ry / 2.0D);
         int bz1 = floorCell(pz + rxz / 2.0D);
         for (int bx = bx0; bx <= bx1; bx++) {
            double qx = (bx + 0.5D - px) / (rxz / 2.0D);
            if (qx * qx >= 1.0D) {
               continue;
            }
            for (int by = by0; by <= by1; by++) {
               if (by <= 0 || by >= this.depth) {
                  continue;
               }
               double qy = (by + 0.5D - py) / (ry / 2.0D);
               if (qx * qx + qy * qy >= 1.0D) {
                  continue;
               }
               for (int bz = bz0; bz <= bz1; bz++) {
                  double qz = (bz + 0.5D - pz) / (rxz / 2.0D);
                  if (qx * qx + qy * qy + qz * qz >= 1.0D) {
                     continue;
                  }
                  int lx = bx - ccx * 16;
                  int lz = bz - ccz * 16;
                  if (lx < 0 || lx > 15 || lz < 0 || lz > 15) {
                     continue;
                  }
                  int idx = this.index(lx, lz, by);
                  if ((cells[idx] & 0xFF) == Blocks.STONE_ID) {
                     cells[idx] = (byte)vein.id;
                  }
               }
            }
         }
      }
   }

   private static int floorCell(double v) {
      int i = (int)v;
      return v < i ? i - 1 : i;
   }

    private void paint(int ccx, int ccz, byte[] cells) {
       Random rand = this.rand.get();
       rand.setSeed((long)ccx * 341873128712L + (long)ccz * 132897987541L);
       double[] stoneNoise = this.gen4.generate(null, ccx * 16, 0, ccz * 16, 16, 1, 16,
          1.0D / 16.0D, 1.0D / 16.0D, 1.0D / 16.0D);
       int[] biomes = this.voronoiHead.generate(ccx * 16, ccz * 16, 16, 16);
       for (int z = 0; z < 16; z++) {
          for (int x = 0; x < 16; x++) {
             int biome = biomes[x + z * 16];
             int depth = (int)(stoneNoise[z + x * 16] / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
             int counter = -1;
             int top = this.topBlock(biome);
             int filler = this.fillerBlock(biome);
             for (int y = this.depth - 1; y >= 0; y--) {
                int idx = this.index(x, z, y);
                if (y <= rand.nextInt(5)) {
                   cells[idx] = (byte)Blocks.BEDROCK_ID;
                } else {
                   int cur = cells[idx] & 0xFF;
                   if (cur == 0) {
                      counter = -1;
                   } else if (cur == Blocks.STONE_ID) {
                      if (counter == -1) {
                         if (depth <= 0) {
                            top = 0;
                            filler = Blocks.STONE_ID;
                         } else if (y >= TerrainGenerator.SEA_LEVEL - 4
                            && y <= TerrainGenerator.SEA_LEVEL + 1) {
                            top = this.topBlock(biome);
                            filler = this.fillerBlock(biome);
                         }
                         if (y < TerrainGenerator.SEA_LEVEL && top == 0) {
                            top = Blocks.WATER_ID;
                         }
                         counter = depth;
                         if (y >= TerrainGenerator.SEA_LEVEL - 1) {
                            cells[idx] = (byte)top;
                         } else {
                            cells[idx] = (byte)filler;
                         }
                      } else if (counter > 0) {
                         counter--;
                         cells[idx] = (byte)filler;
                         if (counter == 0 && filler == Blocks.SAND_ID) {
                            counter = rand.nextInt(4);
                            filler = Blocks.SANDSTONE_ID;
                         }
                      }
                   }
                }
             }
          }
       }
    }

   private int topBlock(int biome) {
      if (biome == GenBiomes.DESERT || biome == GenBiomes.MUSHROOM_SHORE) {
         return Blocks.SAND_ID;
      }
      if (biome == GenBiomes.MUSHROOM_ISLAND) {
         return Blocks.MYCELIUM_ID;
      }
      return Blocks.GRASS_ID;
   }

   private int fillerBlock(int biome) {
      if (biome == GenBiomes.DESERT || biome == GenBiomes.MUSHROOM_SHORE) {
         return Blocks.SAND_ID;
      }
      return Blocks.DIRT_ID;
   }

   private double[] densityField(int ccx, int ccz, int ySize) {
      double[] buf = new double[5 * ySize * 5];
      double[] n5 = this.gen5.generate2D(null, ccx * 4, ccz * 4, 5, 5, 1.121D, 1.121D);
      double[] n6 = this.gen6.generate2D(null, ccx * 4, ccz * 4, 5, 5, 200.0D, 200.0D);
      double[] n3 = this.gen3.generate(null, ccx * 4, 0, ccz * 4, 5, ySize, 5,
         684.412D / 80.0D, 684.412D / 160.0D, 684.412D / 80.0D);
      double[] n1 = this.gen1.generate(null, ccx * 4, 0, ccz * 4, 5, ySize, 5,
         684.412D, 684.412D, 684.412D);
      double[] n2 = this.gen2.generate(null, ccx * 4, 0, ccz * 4, 5, ySize, 5,
         684.412D, 684.412D, 684.412D);
      int[] biomes = this.coarseHead.generate(ccx * 4 - 2, ccz * 4 - 2, 10, 10);
      int l = 0;
      int k = 0;
      for (int ix = 0; ix < 5; ix++) {
         for (int iz = 0; iz < 5; iz++) {
            float wSum = 0.0F;
            float maxSum = 0.0F;
            float minSum = 0.0F;
            int center = biomes[ix + 2 + (iz + 2) * 10];
            float centerMin = GenBiomes.minHeight(center);
            for (int dx = -2; dx <= 2; dx++) {
               for (int dz = -2; dz <= 2; dz++) {
                  int nb = biomes[ix + dx + 2 + (iz + dz + 2) * 10];
                  float w = KERNEL[dx + 2 + (dz + 2) * 5] / (GenBiomes.minHeight(nb) + 2.0F);
                  if (GenBiomes.minHeight(nb) > centerMin) {
                     w /= 2.0F;
                  }
                  maxSum += GenBiomes.maxHeight(nb) * w;
                  minSum += GenBiomes.minHeight(nb) * w;
                  wSum += w;
               }
            }
            float avgMax = maxSum / wSum * 0.9F + 0.1F;
            float avgMin = (minSum / wSum * 4.0F - 1.0F) / 8.0F;
            double hill = n6[k++] / 8000.0D;
            if (hill < 0.0D) {
               hill = -hill * 0.3D;
            }
            hill = hill * 3.0D - 2.0D;
            if (hill < 0.0D) {
               hill /= 2.0D;
               if (hill < -1.0D) {
                  hill = -1.0D;
               }
               hill /= 1.4D;
               hill /= 2.0D;
            } else {
               if (hill > 1.0D) {
                  hill = 1.0D;
               }
               hill /= 8.0D;
            }
            for (int y = 0; y < ySize; y++) {
               double base = avgMin;
               double scale = avgMax;
               base += hill * 0.2D;
               base = base * (double)ySize / 16.0D;
               double mid = (double)ySize / 2.0D + base * 4.0D;
               double depth = ((double)y - mid) * 12.0D * 128.0D / (double)this.depth / scale;
               if (depth < 0.0D) {
                  depth *= 4.0D;
               }
               double d1 = n1[l] / 512.0D;
               double d2 = n2[l] / 512.0D;
               double t = (n3[l] / 10.0D + 1.0D) / 2.0D;
               double density = t < 0.0D ? d1 : t > 1.0D ? d2 : d1 + (d2 - d1) * t;
               density -= depth;
               if (y > ySize - 4) {
                  double f = (double)((float)(y - (ySize - 4)) / 3.0F);
                  density = density * (1.0D - f) + -10.0D * f;
               }
               buf[l] = density;
               l++;
            }
         }
      }
      return buf;
   }
}

