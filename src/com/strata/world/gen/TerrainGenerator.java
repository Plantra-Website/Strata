package com.strata.world.gen;

import com.strata.blocks.Blocks;

import java.util.concurrent.ConcurrentHashMap;

public class TerrainGenerator {
   public static final long DEFAULT_SEED = 17L;
    public static final int SEA_LEVEL = 39;
    public static final int MAX_HEIGHT = 56;
    public static final int MIN_HEIGHT = 4;
    static final int TREE_DENSITY = 2;
    static final int FLOWER_DENSITY = 2;
    static final int GRASS_DENSITY = 8; 
    static final int TRUNK_MIN = 4;
    static final int TRUNK_VAR = 3; 
    static final int CANOPY_R = 2;

    private final long seed;
    private final int depth;
    private final boolean voided;
   private final Noise heightNoise;
   private final Noise oreNoise;
   private final CaveCarver carver;
   private final BiomeProvider biomes;

    public TerrainGenerator(long seed, int depth) {
       this(seed, depth, false);
    }

    public TerrainGenerator(long seed, int depth, boolean voided) {
       this.seed = seed;
       this.depth = depth;
       this.voided = voided;
       this.heightNoise = new Noise(seed);
      this.oreNoise = new Noise(seed ^ 0xB5297A4D62983499L);
      this.carver = new CaveCarver(seed, depth);
      this.biomes = new Biomes.NoiseProvider(seed);
   }

   public Biome biomeAt(int x, int z) {
      return this.biomes.biomeAt(x, z);
   }

   public int heightAt(int x, int z) {
      double n = this.heightNoise.fbm2(x / 64.0, z / 64.0, 4);
      double t = this.biomes.blend(x, z);
      Biome lo = Biomes.plains();
      Biome hi = Biomes.hills();
      float base = (float)(lo.baseHeight() + (hi.baseHeight() - lo.baseHeight()) * t);
      float amp = (float)(lo.amplitude() + (hi.amplitude() - lo.amplitude()) * t);
      int h = Math.round(base + (float)(n * amp));
      if (h < MIN_HEIGHT) {
         h = MIN_HEIGHT;
      }
      if (h > MAX_HEIGHT) {
         h = MAX_HEIGHT;
      }
      return h;
   }

    public int blockAt(int x, int y, int z, int h) {
       if (this.voided) {
          return 0;
       }
       if (y > h) {
          int tree = this.treePart(x, y, z);
          if (tree != 0) {
             return tree;
          }
          if (y == h + 1) {
             return this.groundCover(x, z, h);
          }
          return 0;
       }
       if (y == 0) {
          return Blocks.BEDROCK_ID;
       }
      if (y > 1 && this.isCave(x, y, z)) {
         return y < 10 ? Blocks.LAVA_ID : 0;
      }
      Biome b = this.biomeAt(x, z);
      boolean beach = h <= SEA_LEVEL + 1;
      if (y == h) {
         return beach ? b.shoreBlock() : b.surfaceBlock();
      }
      if (y > h - 4) {
         return (beach && y >= h - 2) ? Blocks.SAND_ID : b.subsurfaceBlock();
      }
      int ore = this.oreAt(x, y, z);
      if (ore != 0) {
         return ore;
      }
      return Blocks.STONE_ID;
   }

   public int blockAt(int x, int y, int z) {
      if (y < 0 || y >= this.depth) {
         return 0;
      }
      return this.blockAt(x, y, z, this.heightAt(x, z));
   }

    static long hash2(long seed, int x, int z) {
       long h = seed + (long)x * 0x8DA6B343L + (long)z * 0xD8163841L;
       h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
       h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
       return h ^ (h >>> 31);
    }

    static int nonneg(long h, int mod) {
       return (int)((h & Long.MAX_VALUE) % mod);
    }

    private final ConcurrentHashMap<Long, Integer> trunkCache = new ConcurrentHashMap<>();

    int treeTrunkHeight(int ox, int oz) {
       long key = ((long)ox << 32) | (oz & 0xFFFFFFFFL);
       Integer cached = this.trunkCache.get(key);
       if (cached != null) {
          return cached;
       }
       int th = 0;
       if (nonneg(hash2(this.seed, ox, oz), 100) < TREE_DENSITY) {
          int base = this.heightAt(ox, oz);
          if (base > SEA_LEVEL + 1 && base + TRUNK_MIN + TRUNK_VAR + 1 < this.depth
             && this.blockAt(ox, base, oz, base) == Blocks.GRASS_ID) {
             th = TRUNK_MIN + nonneg(hash2(this.seed ^ 0x9E3779B9L, ox, oz), TRUNK_VAR);
          }
       }
       this.trunkCache.put(key, th);
       return th;
    }

   public int evictFar(int pcx, int pcz, int keepChunks) {
      int evicted = 0;
      var it = this.trunkCache.entrySet().iterator();
      while (it.hasNext()) {
         long k = it.next().getKey();
         int cx = Math.floorDiv((int)(k >> 32), 16);
         int cz = Math.floorDiv((int)(k & 0xFFFFFFFFL), 16);
         if (Math.abs(cx - pcx) > keepChunks || Math.abs(cz - pcz) > keepChunks) {
            it.remove();
            evicted++;
         }
      }
      return evicted + this.carver.evictFar(pcx, pcz, keepChunks);
   }

    int treePart(int x, int y, int z) {
       for (int ox = x - CANOPY_R; ox <= x + CANOPY_R; ox++) {
          for (int oz = z - CANOPY_R; oz <= z + CANOPY_R; oz++) {
             if (nonneg(hash2(this.seed, ox, oz), 100) >= TREE_DENSITY) {
                continue;
             }
             int th = this.treeTrunkHeight(ox, oz);
             if (th == 0) {
                continue;
             }
             int base = this.heightAt(ox, oz);
             int dx = x - ox;
             int dz = z - oz;
             if (dx == 0 && dz == 0 && y > base && y <= base + th) {
                return Blocks.WOOD_ID;
             }
             int top = base + th;
             int adx = dx < 0 ? -dx : dx;
             int adz = dz < 0 ? -dz : dz;
             if (y == top - 2 || y == top - 1) {
                if (adx <= 2 && adz <= 2 && !(adx == 2 && adz == 2)) {
                   return Blocks.LEAF_ID;
                }
             } else if (y == top) {
                if (adx <= 1 && adz <= 1) {
                   return Blocks.LEAF_ID;
                }
             } else if (y == top + 1) {
                if (adx + adz <= 1) {
                   return Blocks.LEAF_ID;
                }
             }
          }
       }
       return 0;
    }

    int groundCover(int x, int z, int h) {
       if (h <= SEA_LEVEL + 1) {
          return 0;
       }
       if (this.blockAt(x, h, z, h) != Blocks.GRASS_ID) {
          return 0;
       }
       int r = nonneg(hash2(this.seed ^ 0xC0E4L, x, z), 100);
       if (r < FLOWER_DENSITY / 2) {
          return Blocks.ROSE_ID;
       }
       if (r < FLOWER_DENSITY) {
          return Blocks.DANDELION_ID;
       }
       if (r < GRASS_DENSITY) {
          return Blocks.TALL_GRASS_ID;
       }
       if (nonneg(hash2(this.seed ^ 0x5EED1L, x, z), 200) == 0) {
          return Blocks.SAPLING_ID;
       }
       return 0;
    }

   private boolean isCave(int x, int y, int z) {
      return this.carver.isCarved(x, y, z);
   }

   public void touchCarve(int ccx, int ccz) {
      this.carver.isCarved(ccx * 16 + 8, 32, ccz * 16 + 8);
   }

   private int oreAt(int x, int y, int z) {
      if (y <= 30 && this.oreNoise.noise3(x / 14.0, y / 14.0, z / 14.0) > 0.45) {
         return Blocks.COAL_ID;
      }
      if (y <= 22 && this.oreNoise.noise3(x / 12.0 + 500, y / 12.0, z / 12.0) > 0.5) {
         return Blocks.IRON_ID;
      }
      if (y <= 14 && this.oreNoise.noise3(x / 11.0 + 1000, y / 11.0, z / 11.0) > 0.55) {
         return Blocks.GOLD_ID;
      }
       if (y <= 10 && this.oreNoise.noise3(x / 10.0 + 1500, y / 10.0, z / 10.0) > 0.6) {
          return Blocks.DIAMOND_ID;
       }
       if (y <= 12 && this.oreNoise.noise3(x / 8.0 + 3000, y / 8.0, z / 8.0) > 0.62) {
          return Blocks.LAPIS_ID;
       }
       if (y <= 40 && this.oreNoise.noise3(x / 9.0 + 2500, y / 9.0, z / 9.0) > 0.55) {
          return Blocks.GRAVEL_ID;
       }
       return 0;
    }
}

