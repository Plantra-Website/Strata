package com.strata.world.gen;

import com.strata.blocks.Blocks;

import java.util.concurrent.ConcurrentHashMap;

public class TerrainGenerator {
   public static final long DEFAULT_SEED = 17L;
    public static final int SEA_LEVEL = 63;
    public static final int MAX_HEIGHT = 56;
    public static final int MIN_HEIGHT = 4;
    static final int FLOWER_DENSITY = 2;
    static final int GRASS_DENSITY = 8; 
    static final int TRUNK_MIN = 4;
    static final int TRUNK_VAR = 3; 
    static final int CANOPY_R = 3; 
    public static final int OAK = 0;
    public static final int BIRCH = 1;
    public static final int SPRUCE_SHORT = 2;
    public static final int SPRUCE_TALL = 3;
    public static final int SWAMP_OAK = 4;
    public static final int BIG_OAK = 5;

    private final long seed;
    private final int depth;
    private final boolean voided;
   private final VanillaOctaves gen1;
   private final VanillaOctaves gen2;
   private final VanillaOctaves gen3;
   private final VanillaOctaves gen4;
   private final VanillaOctaves gen5;
   private final VanillaOctaves gen6;
   private final GenLayer coarseHead;
   private final GenLayer voronoiHead;
   private final GenChunkFill fill;
   private final CaveCarver carver;

    public TerrainGenerator(long seed, int depth) {
       this(seed, depth, false);
    }

    public TerrainGenerator(long seed, int depth, boolean voided) {
       this.seed = seed;
       this.depth = depth;
       this.voided = voided;
       java.util.Random rand = new java.util.Random(seed);
       this.gen1 = new VanillaOctaves(rand, 16);
       this.gen2 = new VanillaOctaves(rand, 16);
       this.gen3 = new VanillaOctaves(rand, 8);
       this.gen4 = new VanillaOctaves(rand, 4);
       this.gen5 = new VanillaOctaves(rand, 10);
       this.gen6 = new VanillaOctaves(rand, 16);
       GenLayer[] stack = GenLayer.buildStack(seed);
       this.coarseHead = stack[0];
       this.voronoiHead = stack[1];
        this.fill = new GenChunkFill(seed, depth, gen1, gen2, gen3, gen4, gen5, gen6, coarseHead, voronoiHead);
      this.carver = new CaveCarver(seed, depth);
   }

   private final ConcurrentHashMap<Long, Integer> biomeCache = new ConcurrentHashMap<>();

   public int genBiomeAt(int x, int z) {
      long key = ((long)x << 32) | (z & 0xFFFFFFFFL);
      Integer cached = this.biomeCache.get(key);
      if (cached != null) {
         return cached;
      }
      int b = this.voronoiHead.generate(x, z, 1, 1)[0];
      this.biomeCache.put(key, b);
      return b;
   }

   static final class Lobe {
      final int ox, oy, oz, liquid;
      final double rx, ry, rz;
      Lobe(int ox, int oy, int oz, double rx, double ry, double rz, int liquid) {
         this.ox = ox;
         this.oy = oy;
         this.oz = oz;
         this.rx = rx;
         this.ry = ry;
         this.rz = rz;
         this.liquid = liquid;
      }
   }

   static final class Lake {
      final Lobe[] lobes;
      Lake(Lobe[] lobes) {
         this.lobes = lobes;
      }
   }

   private final ConcurrentHashMap<Long, Lake[]> lakeCache = new ConcurrentHashMap<>();

   private Lake makeLake(int ccx, int ccz, long salt, int liquid, boolean lowBiome) {
      int lx = ccx * 16 + nonneg(hash2(this.seed ^ (salt + 1L), ccx, ccz), 16);
      int lz = ccz * 16 + nonneg(hash2(this.seed ^ (salt + 2L), ccx, ccz), 16);
      int h0 = this.rawHeightAt(lx, lz);
      if (h0 < SEA_LEVEL - 4 || h0 >= this.depth - 8) {
         return null;
      }
      if (lowBiome && !(h0 < SEA_LEVEL + 2
         || nonneg(hash2(this.seed ^ (salt + 3L), ccx, ccz), 10) == 0)) {
         return null;
      }
      int n = 4 + nonneg(hash2(this.seed ^ (salt + 4L), ccx, ccz), 4);
      Lobe[] lobes = new Lobe[n];
      for (int i = 0; i < n; i++) {
         long ls = salt + 10L + i;
         int ox = lx + nonneg(hash2(this.seed ^ (ls + 1L), ccx, ccz), 17) - 8;
         int oz = lz + nonneg(hash2(this.seed ^ (ls + 2L), ccx, ccz), 17) - 8;
         int oy = h0 - 2 + nonneg(hash2(this.seed ^ (ls + 3L), ccx, ccz), 5) - 2;
         double rx = (3 + nonneg(hash2(this.seed ^ (ls + 4L), ccx, ccz), 7)) / 2.0D;
         double rz = (3 + nonneg(hash2(this.seed ^ (ls + 5L), ccx, ccz), 7)) / 2.0D;
         double ry = (2 + nonneg(hash2(this.seed ^ (ls + 6L), ccx, ccz), 5)) / 2.0D;
         lobes[i] = new Lobe(ox, oy, oz, rx, ry, rz, liquid);
      }
      for (Lobe lobe : lobes) {
         if (this.lobeTouchesWater(lobe)) {
            return null;
         }
      }
      return new Lake(lobes);
   }

   private boolean lobeTouchesWater(Lobe lobe) {
      int x0 = (int)Math.floor(lobe.ox - lobe.rx);
      int x1 = (int)Math.ceil(lobe.ox + lobe.rx);
      int y0 = Math.max(1, (int)Math.floor(lobe.oy - lobe.ry));
      int y1 = Math.min(this.depth - 1, (int)Math.ceil(lobe.oy + lobe.ry));
      int z0 = (int)Math.floor(lobe.oz - lobe.rz);
      int z1 = (int)Math.ceil(lobe.oz + lobe.rz);
      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            for (int y = y0; y <= y1; y++) {
               double dx = (x - lobe.ox) / lobe.rx;
               double dy = (y - lobe.oy) / lobe.ry;
               double dz = (z - lobe.oz) / lobe.rz;
               if (dx * dx + dy * dy + dz * dz <= 1.0D
                  && this.fillCell(x, y, z) == Blocks.WATER_ID) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   Lake[] lakesForChunk(int ccx, int ccz) {
      long key = ((long)ccx << 32) | (ccz & 0xFFFFFFFFL);
      Lake[] hit = this.lakeCache.get(key);
      if (hit != null) {
         return hit;
      }
      Lake water = null, lava = null;
      if (nonneg(hash2(this.seed ^ 0x1A0001L, ccx, ccz), 4) == 0) {
         water = this.makeLake(ccx, ccz, 0x1A1000L, Blocks.WATER_ID, false);
      }
      if (nonneg(hash2(this.seed ^ 0x1B0001L, ccx, ccz), 8) == 0) {
         lava = this.makeLake(ccx, ccz, 0x1B1000L, Blocks.LAVA_ID, true);
      }
      Lake[] lakes = water == null
         ? (lava == null ? new Lake[0] : new Lake[]{lava})
         : (lava == null ? new Lake[]{water} : new Lake[]{water, lava});
      this.lakeCache.put(key, lakes);
      return lakes;
   }

   Lake[] columnLakes(int x, int z) {
      int ccx = Math.floorDiv(x, 16);
      int ccz = Math.floorDiv(z, 16);
      int lx = Math.floorMod(x, 16);
      int lz = Math.floorMod(z, 16);
      Lake[] own = this.lakesForChunk(ccx, ccz);
      boolean west = lx <= 12, east = lx >= 3, north = lz <= 12, south = lz >= 3;
      if (!west && !east && !north && !south) {
         return own;
      }
      java.util.ArrayList<Lake> all = new java.util.ArrayList<>(own.length + 2);
      for (Lake l : own) {
         all.add(l);
      }
      if (west) {
         for (Lake l : this.lakesForChunk(ccx - 1, ccz)) {
            all.add(l);
         }
      }
      if (east) {
         for (Lake l : this.lakesForChunk(ccx + 1, ccz)) {
            all.add(l);
         }
      }
      if (north) {
         for (Lake l : this.lakesForChunk(ccx, ccz - 1)) {
            all.add(l);
         }
      }
      if (south) {
         for (Lake l : this.lakesForChunk(ccx, ccz + 1)) {
            all.add(l);
         }
      }
      if ((west || east) && (north || south)) {
         int dx = west ? -1 : 1;
         int dz = north ? -1 : 1;
         for (Lake l : this.lakesForChunk(ccx + dx, ccz + dz)) {
            all.add(l);
         }
      }
      return all.toArray(new Lake[0]);
   }

   static int lakeCellFrom(Lake[] lakes, int x, int y, int z) {
      if (y <= 1) {
         return -1;
      }
      for (Lake lake : lakes) {
         for (Lobe lobe : lake.lobes) {
            double dx = x - lobe.ox;
            if (dx < -lobe.rx - 1.0D || dx > lobe.rx + 1.0D) {
               continue;
            }
            double dz = z - lobe.oz;
            if (dz < -lobe.rz - 1.0D || dz > lobe.rz + 1.0D) {
               continue;
            }
            double dy = y - lobe.oy;
            if (dy < -lobe.ry - 1.0D || dy > lobe.ry + 1.0D) {
               continue;
            }
            double q = dx * dx / (lobe.rx * lobe.rx)
               + dy * dy / (lobe.ry * lobe.ry)
               + dz * dz / (lobe.rz * lobe.rz);
            if (q <= 1.0D) {
               return y <= lobe.oy ? lobe.liquid : 0;
            }
         }
      }
      return -1;
   }

   int lakeLiquidAt(int x, int y, int z) {
      int r = lakeCellFrom(this.columnLakes(x, z), x, y, z);
      return r > 0 ? r : 0;
   }

   int rawHeightAt(int x, int z) {
      int ccx = Math.floorDiv(x, 16);
      int ccz = Math.floorDiv(z, 16);
      byte[] col = this.fill.fill(ccx, ccz);
      int lx = Math.floorMod(x, 16);
      int lz = Math.floorMod(z, 16);
      for (int y = this.depth - 1; y >= 0; y--) {
         int v = col[(lx * 16 + lz) * this.depth + y] & 0xFF;
         if (v > 0 && Blocks.isSolid(v)) {
            return y;
         }
      }
      return 0;
   }

   public int heightAt(int x, int z) {
      if (this.voided) {
         return 0;
      }
      Lake[] lakes = this.columnLakes(x, z);
      boolean covered = false;
      for (Lake lake : lakes) {
         for (Lobe lobe : lake.lobes) {
            if (Math.abs(x - lobe.ox) <= lobe.rx + 1.0D
               && Math.abs(z - lobe.oz) <= lobe.rz + 1.0D) {
               covered = true;
               break;
            }
         }
         if (covered) {
            break;
         }
      }
      if (!covered) {
         return this.rawHeightAt(x, z);
      }
      int ccx = Math.floorDiv(x, 16);
      int ccz = Math.floorDiv(z, 16);
      byte[] col = this.fill.fill(ccx, ccz);
      int lx = Math.floorMod(x, 16);
      int lz = Math.floorMod(z, 16);
      for (int y = this.depth - 1; y >= 0; y--) {
         if (lakeCellFrom(lakes, x, y, z) >= 0) {
            continue;
         }
         int v = col[(lx * 16 + lz) * this.depth + y] & 0xFF;
         if (v > 0 && Blocks.isSolid(v)) {
            return y;
         }
      }
      return 0;
   }

     public int blockAt(int x, int y, int z, int h) {
        if (this.voided) {
           return 0;
        }
        if (y < 0 || y >= this.depth) {
           return 0;
        }
        int v = this.fillCell(x, y, z);
        if (v == Blocks.WATER_ID && y < SEA_LEVEL
           && this.fillCell(x, y + 1, z) != Blocks.WATER_ID
           && GenBiomes.snowy(this.genBiomeAt(x, z))
           && this.fillCell(x + 1, y, z) == Blocks.WATER_ID
           && this.fillCell(x - 1, y, z) == Blocks.WATER_ID
           && this.fillCell(x, y, z + 1) == Blocks.WATER_ID
           && this.fillCell(x, y, z - 1) == Blocks.WATER_ID) {
           return Blocks.ICE_ID;
        }
        if (y >= h - 10 && y <= h + 8) {
           Lake[] lakes = this.columnLakes(x, z);
           int lake = lakeCellFrom(lakes, x, y, z);
           if (lake == Blocks.WATER_ID && this.isLakeIce(x, y, z, lakes)) {
              return Blocks.ICE_ID;
           }
           if (lake >= 0) {
              return lake;
           }
        }
        if (y >= SEA_LEVEL - 3 && y <= SEA_LEVEL + 1) {
           int disc = this.discAt(x, y, z, v);
           if (disc != 0) {
              return disc;
           }
        }
        if (v == 0 && y > h) {
           int tree = this.treePart(x, y, z);
           if (tree != 0) {
              return tree;
           }
           int shroom = this.shroomPart(x, y, z);
           if (shroom != 0) {
              return shroom;
           }
           if (y == SEA_LEVEL && this.fillCell(x, y - 1, z) == Blocks.WATER_ID) {
              if (this.genBiomeAt(x, z) == GenBiomes.SWAMPLAND
                 && nonneg(hash2(this.seed ^ 0x1A9FADL, x, z), 256) < 4) {
                 return Blocks.LILYPAD_ID;
              }
              return 0;
           }
           int stalk = this.stalkPart(x, y, z, h);
           if (stalk != 0) {
              return stalk;
           }
           int vine = this.vinePart(x, y, z);
           if (vine != 0) {
              return vine;
           }
           if (y == h + 1) {
              return this.groundCover(x, z, h);
           }
           return 0;
        }
       if (y == 0) {
          return Blocks.BEDROCK_ID;
       }
       if ((v == Blocks.STONE_ID || v == Blocks.DIRT_ID || v == Blocks.GRASS_ID)
          && y > 1 && this.isCave(x, y, z)) {
          return y < 10 ? Blocks.LAVA_ID : 0;
       }
       if ((v == Blocks.DIRT_ID || v == Blocks.GRASS_ID || v == Blocks.SAND_ID)
          && y >= SEA_LEVEL - 8 && y <= SEA_LEVEL + 8) {
          int shell = this.lavaShellAt(x, y, z);
          if (shell != 0) {
             return shell;
          }
       }
       return v;
    }

    int lavaShellAt(int x, int y, int z) {
       Lake[] lakes = this.columnLakes(x, z);
       for (Lake lake : lakes) {
          for (Lobe lobe : lake.lobes) {
             if (lobe.liquid != Blocks.LAVA_ID) {
                continue;
             }
             if (Math.abs(x - lobe.ox) > lobe.rx + 1.0D
                || Math.abs(z - lobe.oz) > lobe.rz + 1.0D
                || Math.abs(y - lobe.oy) > lobe.ry + 1.0D) {
                continue;
             }
             if (lakeCellFrom(lakes, x, y, z) >= 0) {
                continue;
             }
             int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
             for (int[] d : dirs) {
                if (lakeCellFrom(lakes, x + d[0], y + d[1], z + d[2]) == Blocks.LAVA_ID) {
                   if (y <= lobe.oy
                      || nonneg(hash2(this.seed ^ (0x5AE110L + y), x, z), 2) == 0) {
                      return Blocks.STONE_ID;
                   }
                   return 0;
                }
             }
          }
       }
       return 0;
    }

    boolean isLakeIce(int x, int y, int z, Lake[] lakes) {
       if (lakeCellFrom(lakes, x, y, z) != Blocks.WATER_ID) {
          return false;
       }
       if (lakeCellFrom(lakes, x, y + 1, z) == Blocks.WATER_ID) {
          return false;
       }
       if (!GenBiomes.snowy(this.genBiomeAt(x, z))) {
          return false;
       }
       return lakeCellFrom(lakes, x + 1, y, z) == Blocks.WATER_ID
          && lakeCellFrom(lakes, x - 1, y, z) == Blocks.WATER_ID
          && lakeCellFrom(lakes, x, y, z + 1) == Blocks.WATER_ID
          && lakeCellFrom(lakes, x, y, z - 1) == Blocks.WATER_ID;
    }

   private int fillCell(int x, int y, int z) {
      byte[] col = this.fill.fill(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
      return col[(Math.floorMod(x, 16) * 16 + Math.floorMod(z, 16)) * this.depth + y] & 0xFF;
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

    static int treeAttempts(int biome) {
       if (biome == GenBiomes.FOREST || biome == GenBiomes.TAIGA) {
          return 10;
       }
       if (biome == GenBiomes.SWAMPLAND) {
          return 2;
       }
       return 0;
    }

    int treeSpecies(int ox, int oz) {
       long key = ((long)ox << 32) | (oz & 0xFFFFFFFFL);
       Integer cached = this.trunkCache.get(key);
       if (cached != null) {
          return cached / 32;
       }
       this.treeTrunkHeight(ox, oz);
       cached = this.trunkCache.get(key);
       return cached == null ? OAK : cached / 32;
    }

    public static int logForSpecies(int species) {
       if (species == BIRCH) {
          return Blocks.BIRCH_LOG_ID;
       }
       if (species == SPRUCE_SHORT || species == SPRUCE_TALL) {
          return Blocks.SPRUCE_LOG_ID;
       }
       return Blocks.WOOD_ID;
    }

    public static int leavesForSpecies(int species) {
       if (species == BIRCH) {
          return Blocks.BIRCH_LEAVES_ID;
       }
       if (species == SPRUCE_SHORT || species == SPRUCE_TALL) {
          return Blocks.SPRUCE_LEAVES_ID;
       }
       return Blocks.LEAF_ID;
    }

    int treeTrunkHeight(int ox, int oz) {
       long key = ((long)ox << 32) | (oz & 0xFFFFFFFFL);
       Integer cached = this.trunkCache.get(key);
       if (cached != null) {
          return cached % 32;
       }
       int th = 0;
       int species = OAK;
       if (nonneg(hash2(this.seed, ox, oz), 256) >= 11) {
          this.trunkCache.put(key, species * 32 + th);
          return th;
       }
       int biome = this.genBiomeAt(ox, oz);
       int attempts = treeAttempts(biome);
       if (attempts > 0 && nonneg(hash2(this.seed ^ 0xBEEFL, ox, oz), 10) == 0) {
          attempts++;
       }
       if (attempts > 0 && nonneg(hash2(this.seed, ox, oz), 256) < attempts) {
          species = this.pickSpecies(ox, oz, biome);
          th = this.trunkForSpecies(species, ox, oz);
          int base = this.heightAt(ox, oz);
          int need = th + 2;
          if (!(base > SEA_LEVEL + 1 && base + need < this.depth
             && this.blockAt(ox, base, oz, base) == Blocks.GRASS_ID)) {
             th = 0;
          }
       }
       this.trunkCache.put(key, species * 32 + th);
       return th;
    }

    private int pickSpecies(int ox, int oz, int biome) {
       if (biome == GenBiomes.FOREST) {
          if (nonneg(hash2(this.seed ^ 0xF02E57L, ox, oz), 5) == 0) {
             return BIRCH;
          }
          if (nonneg(hash2(this.seed ^ 0xB160A6L, ox, oz), 10) == 0) {
             return BIG_OAK;
          }
          return OAK;
       }
       if (biome == GenBiomes.TAIGA) {
          if (nonneg(hash2(this.seed ^ 0x7A16AL, ox, oz), 3) == 0) {
             return SPRUCE_TALL;
          }
          return SPRUCE_SHORT;
       }
       if (biome == GenBiomes.SWAMPLAND) {
          return SWAMP_OAK;
       }
       return OAK;
    }

    private int trunkForSpecies(int species, int ox, int oz) {
       if (species == BIRCH) {
          return 5 + nonneg(hash2(this.seed ^ 0x9E3779B9L, ox, oz), 3);
       }
       if (species == BIG_OAK) {
          return 6 + nonneg(hash2(this.seed ^ 0x9E3779B9L, ox, oz), 4);
       }
       if (species == SPRUCE_SHORT) {
          return 6 + nonneg(hash2(this.seed ^ 0x9E3779B9L, ox, oz), 4);
       }
       if (species == SPRUCE_TALL) {
          return 7 + nonneg(hash2(this.seed ^ 0x9E3779B9L, ox, oz), 5);
       }
       if (species == SWAMP_OAK) {
          return 5 + nonneg(hash2(this.seed ^ 0x9E3779B9L, ox, oz), 4);
       }
       return TRUNK_MIN + nonneg(hash2(this.seed ^ 0x9E3779B9L, ox, oz), TRUNK_VAR);
    }

    int vinePart(int x, int y, int z) {
       if (nonneg(hash2(this.seed ^ 0xB10E12L, x, z), 256) >= 12) {
          return 0;
       }
       if (this.treePart(x, y, z) != 0) {
          return 0;
       }
       for (int ox = x - CANOPY_R; ox <= x + CANOPY_R; ox++) {
          for (int oz = z - CANOPY_R; oz <= z + CANOPY_R; oz++) {
             if (nonneg(hash2(this.seed, ox, oz), 256) >= 11) {
                continue;
             }
             int th = this.treeTrunkHeight(ox, oz);
             if (th == 0 || this.treeSpecies(ox, oz) != SWAMP_OAK) {
                continue;
             }
             int base = this.heightAt(ox, oz);
             int top = base + th;
             int len = 1 + nonneg(hash2(this.seed ^ 0xB10E11L, x, z), 4);
             for (int d = 1; d <= len; d++) {
                if (!TreeShapes.swamp(x - ox, (y + d) - top, z - oz)) {
                   continue;
                }
                boolean clear = true;
                for (int k = 1; k < d; k++) {
                   if (this.treePart(x, y + k, z) != 0) {
                      clear = false;
                      break;
                   }
                }
                if (clear) {
                   return Blocks.VINE_ID;
                }
                break;
             }
          }
       }
       return 0;
    }

    static final int SHROOM_BROWN_KIND = 0;
    static final int SHROOM_RED_KIND = 1;

    private final ConcurrentHashMap<Long, Integer> shroomCache = new ConcurrentHashMap<>();

    static int capForKind(int kind) {
       return kind == SHROOM_RED_KIND ? Blocks.MUSHROOM_CAP_RED_ID : Blocks.MUSHROOM_CAP_BROWN_ID;
    }

    int shroomKind(int ox, int oz) {
       long key = ((long)ox << 32) | (oz & 0xFFFFFFFFL);
       Integer cached = this.shroomCache.get(key);
       if (cached != null) {
          return cached / 32;
       }
       this.shroomStemHeight(ox, oz);
       cached = this.shroomCache.get(key);
       return cached == null ? SHROOM_BROWN_KIND : cached / 32;
    }

    int shroomStemHeight(int ox, int oz) {
       long key = ((long)ox << 32) | (oz & 0xFFFFFFFFL);
       Integer cached = this.shroomCache.get(key);
       if (cached != null) {
          return cached % 32;
       }
       int stem = 0;
       int kind = SHROOM_BROWN_KIND;
       if (nonneg(hash2(this.seed, ox, oz), 256) < 1) {
          int biome = this.genBiomeAt(ox, oz);
          if (biome == GenBiomes.MUSHROOM_ISLAND || biome == GenBiomes.MUSHROOM_SHORE) {
             kind = nonneg(hash2(this.seed ^ 0x5A2001AL, ox, oz), 2);
             stem = 4 + nonneg(hash2(this.seed ^ 0x57A1FA1L, ox, oz), 3);
             int base = this.heightAt(ox, oz);
             int ground = this.blockAt(ox, base, oz, base);
             if (!(base > SEA_LEVEL + 1 && base + stem + 2 < this.depth
                && (ground == Blocks.MYCELIUM_ID || ground == Blocks.GRASS_ID || ground == Blocks.DIRT_ID))) {
                stem = 0;
             }
          }
       }
       this.shroomCache.put(key, kind * 32 + stem);
       return stem;
    }

    int shroomPart(int x, int y, int z) {
       for (int ox = x - CANOPY_R; ox <= x + CANOPY_R; ox++) {
          for (int oz = z - CANOPY_R; oz <= z + CANOPY_R; oz++) {
             if (nonneg(hash2(this.seed, ox, oz), 256) >= 1) {
                continue;
             }
             int stem = this.shroomStemHeight(ox, oz);
             if (stem == 0) {
                continue;
             }
             int kind = this.shroomKind(ox, oz);
             int base = this.heightAt(ox, oz);
             int dx = x - ox;
             int dz = z - oz;
             if (dx == 0 && dz == 0 && y > base && y <= base + stem) {
                return Blocks.MUSHROOM_STEM_ID;
             }
             int top = base + stem;
             int adx = dx < 0 ? -dx : dx;
             int adz = dz < 0 ? -dz : dz;
             if (y == top) {
                if (adx <= 1 && adz <= 1 && !(dx == 0 && dz == 0)) {
                   return capForKind(kind);
                }
             } else if (y == top + 1) {
                if (adx <= 1 && adz <= 1) {
                   return capForKind(kind);
                }
             }
          }
       }
       return 0;
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
      var ib = this.biomeCache.entrySet().iterator();
      while (ib.hasNext()) {
         long k = ib.next().getKey();
         int cx = Math.floorDiv((int)(k >> 32), 16);
         int cz = Math.floorDiv((int)(k & 0xFFFFFFFFL), 16);
         if (Math.abs(cx - pcx) > keepChunks || Math.abs(cz - pcz) > keepChunks) {
            ib.remove();
            evicted++;
         }
      }
      var il = this.lakeCache.entrySet().iterator();
      while (il.hasNext()) {
         long k = il.next().getKey();
         int cx = (int)(k >> 32);
         int cz = (int)(k & 0xFFFFFFFFL);
         if (Math.abs(cx - pcx) > keepChunks || Math.abs(cz - pcz) > keepChunks) {
            il.remove();
            evicted++;
         }
      }
      var ic = this.discCache.entrySet().iterator();
      while (ic.hasNext()) {
         long k = ic.next().getKey();
         int cx = (int)(k >> 32);
         int cz = (int)(k & 0xFFFFFFFFL);
         if (Math.abs(cx - pcx) > keepChunks || Math.abs(cz - pcz) > keepChunks) {
            ic.remove();
            evicted++;
         }
      }
      var is = this.shroomCache.entrySet().iterator();
      while (is.hasNext()) {
         long k = is.next().getKey();
         int cx = Math.floorDiv((int)(k >> 32), 16);
         int cz = Math.floorDiv((int)(k & 0xFFFFFFFFL), 16);
         if (Math.abs(cx - pcx) > keepChunks || Math.abs(cz - pcz) > keepChunks) {
            is.remove();
            evicted++;
         }
      }
      var ip = this.patchCache.entrySet().iterator();
      while (ip.hasNext()) {
         long k = ip.next().getKey();
         int cx = (int)(k >> 32);
         int cz = (int)(k & 0xFFFFFFFFL);
         if (Math.abs(cx - pcx) > keepChunks || Math.abs(cz - pcz) > keepChunks) {
            ip.remove();
            evicted++;
         }
      }
       return evicted + this.carver.evictFar(pcx, pcz, keepChunks)
         + this.fill.evictFar(pcx, pcz, keepChunks);
   }

    int treePart(int x, int y, int z) {
       for (int ox = x - CANOPY_R; ox <= x + CANOPY_R; ox++) {
          for (int oz = z - CANOPY_R; oz <= z + CANOPY_R; oz++) {
             if (nonneg(hash2(this.seed, ox, oz), 256) >= 11) {
                continue;
             }
             int biome = this.genBiomeAt(ox, oz);
             int attempts = treeAttempts(biome);
             if (nonneg(hash2(this.seed ^ 0xBEEFL, ox, oz), 10) == 0) {
                attempts++;
             }
             if (attempts <= 0 || nonneg(hash2(this.seed, ox, oz), 256) >= attempts) {
                continue;
             }
             int th = this.treeTrunkHeight(ox, oz);
             if (th == 0) {
                continue;
             }
             int species = this.treeSpecies(ox, oz);
             int base = this.heightAt(ox, oz);
             int dx = x - ox;
             int dz = z - oz;
             int log = logForSpecies(species);
             int leaves = leavesForSpecies(species);
             if (dx == 0 && dz == 0 && y > base && y <= base + th) {
                return log;
             }
             int top = base + th;
             boolean cone = species == SPRUCE_SHORT || species == SPRUCE_TALL;
             boolean leaf;
             if (cone) {
                leaf = TreeShapes.spruce(dx, y - top, dz);
             } else if (species == SWAMP_OAK) {
                leaf = TreeShapes.swamp(dx, y - top, dz);
             } else {
                leaf = TreeShapes.round(dx, y - top, dz, x, z);
             }
             if (leaf) {
                return leaves;
             }
          }
       }
       return 0;
    }

   static final class Disc {
      final int lx, cy, lz, r, halfTh, fromA, fromB, to;
      Disc(int lx, int cy, int lz, int r, int halfTh, int fromA, int fromB, int to) {
         this.lx = lx;
         this.cy = cy;
         this.lz = lz;
         this.r = r;
         this.halfTh = halfTh;
         this.fromA = fromA;
         this.fromB = fromB;
         this.to = to;
      }
   }

   static final Disc[] NO_DISCS = new Disc[0];

   private final ConcurrentHashMap<Long, Disc[]> discCache = new ConcurrentHashMap<>();

   Disc[] discsForChunk(int ccx, int ccz) {
      long key = ((long)ccx << 32) | (ccz & 0xFFFFFFFFL);
      Disc[] hit = this.discCache.get(key);
      if (hit != null) {
         return hit == NO_DISCS ? null : hit;
      }
      java.util.ArrayList<Disc> discs = new java.util.ArrayList<>(5);
      Disc clay = this.clayDisc(ccx, ccz);
      if (clay != null) {
         discs.add(clay);
      }
      for (int i = 0; i < 4; i++) {
         Disc sand = this.sandDisc(ccx, ccz, i);
         if (sand != null) {
            discs.add(sand);
         }
      }
      Disc[] out = discs.isEmpty() ? NO_DISCS
         : discs.toArray(new Disc[0]);
      this.discCache.put(key, out);
      return out == NO_DISCS ? null : out;
   }

   private Disc clayDisc(int ccx, int ccz) {
      int lx = ccx * 16 + nonneg(hash2(this.seed ^ 0xC1A701L, ccx, ccz), 16);
      int lz = ccz * 16 + nonneg(hash2(this.seed ^ 0xC1A702L, ccx, ccz), 16);
      if (this.fillCell(lx, SEA_LEVEL - 1, lz) != Blocks.WATER_ID) {
         return null;
      }
      return new Disc(lx, SEA_LEVEL - 1, lz,
         2 + nonneg(hash2(this.seed ^ 0xC1A703L, ccx, ccz), 2), 1,
         Blocks.DIRT_ID, Blocks.CLAY_ID, Blocks.CLAY_ID);
   }

   private Disc sandDisc(int ccx, int ccz, int i) {
      long salt = this.seed ^ (0x5A0D01L + i);
      int lx = ccx * 16 + nonneg(hash2(salt ^ 0x11L, ccx, ccz), 16);
      int lz = ccz * 16 + nonneg(hash2(salt ^ 0x22L, ccx, ccz), 16);
      if (this.fillCell(lx, SEA_LEVEL - 1, lz) != Blocks.WATER_ID) {
         return null;
      }
      return new Disc(lx, SEA_LEVEL - 1, lz,
         2 + nonneg(hash2(salt ^ 0x33L, ccx, ccz), 5), 2,
         Blocks.DIRT_ID, Blocks.GRASS_ID, Blocks.SAND_ID);
   }

   int discAt(int x, int y, int z, int v) {
      if (y < SEA_LEVEL - 3 || y > SEA_LEVEL + 1) {
         return 0;
      }
      int ccx = Math.floorDiv(x, 16);
      int ccz = Math.floorDiv(z, 16);
      int lx = Math.floorMod(x, 16);
      int lz = Math.floorMod(z, 16);
      int found = 0;
      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            if (dx != 0 && (dx < 0 ? lx >= 6 : lx <= 9)) {
               continue;
            }
            if (dz != 0 && (dz < 0 ? lz >= 6 : lz <= 9)) {
               continue;
            }
            Disc[] discs = this.discsForChunk(ccx + dx, ccz + dz);
            if (discs == null) {
               continue;
            }
            for (Disc c : discs) {
               if (c == null) {
                  continue;
               }
               int dy = y - c.cy;
               if (dy < -c.halfTh || dy > c.halfTh) {
                  continue;
               }
               int ddx = x - c.lx, ddz = z - c.lz;
               if (ddx * ddx + ddz * ddz <= c.r * c.r
                  && (v == c.fromA || v == c.fromB)) {
                  found = c.to;
               }
            }
         }
      }
      return found;
   }

   static final class PumpkinPatch {
      final int cx, cz;
      PumpkinPatch(int cx, int cz) {
         this.cx = cx;
         this.cz = cz;
      }
   }

   static final PumpkinPatch NO_PATCH = new PumpkinPatch(Integer.MIN_VALUE, Integer.MIN_VALUE);

   private final ConcurrentHashMap<Long, PumpkinPatch> patchCache = new ConcurrentHashMap<>();

   PumpkinPatch patchForChunk(int ccx, int ccz) {
      long key = ((long)ccx << 32) | (ccz & 0xFFFFFFFFL);
      PumpkinPatch hit = this.patchCache.get(key);
      if (hit != null) {
         return hit == NO_PATCH ? null : hit;
      }
      PumpkinPatch patch = null;
      if (nonneg(hash2(this.seed ^ 0x9A17C01L, ccx, ccz), 32) == 0) {
         patch = new PumpkinPatch(ccx * 16 + nonneg(hash2(this.seed ^ 0x9A17C02L, ccx, ccz), 16),
            ccz * 16 + nonneg(hash2(this.seed ^ 0x9A17C03L, ccx, ccz), 16));
      }
      this.patchCache.put(key, patch == null ? NO_PATCH : patch);
      return patch;
   }

   static double scatterWeight(int d) {
      int a = d < 0 ? -d : d;
      return a > 7 ? 0.0D : (8 - a) / 64.0D;
   }

   boolean isPumpkinAt(int x, int z) {
      int ccx = Math.floorDiv(x, 16);
      int ccz = Math.floorDiv(z, 16);
      int lx = Math.floorMod(x, 16);
      int lz = Math.floorMod(z, 16);
      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            if (dx != 0 && (dx < 0 ? lx >= 8 : lx <= 7)) {
               continue;
            }
            if (dz != 0 && (dz < 0 ? lz >= 8 : lz <= 7)) {
               continue;
            }
            PumpkinPatch p = this.patchForChunk(ccx + dx, ccz + dz);
            if (p == null) {
               continue;
            }
            double w = 64.0D * scatterWeight(x - p.cx) * scatterWeight(z - p.cz);
            w *= 0.25D;
            if (w <= 0.0D) {
               continue;
            }
            if ((nonneg(hash2(this.seed ^ 0x9A17C1AL, x, z), 4096) / 4096.0D) < w) {
               return true;
            }
         }
      }
      return false;
   }

    static int flowerAttempts(int biome) {
       if (biome == GenBiomes.PLAINS) {
          return 4;
       }
       if (biome == GenBiomes.SWAMPLAND || biome == GenBiomes.MUSHROOM_ISLAND
          || biome == GenBiomes.MUSHROOM_SHORE) {
          return 0;
       }
       return 2;
    }

    static int grassAttempts(int biome) {
       if (biome == GenBiomes.PLAINS) {
          return 10;
       }
       if (biome == GenBiomes.FOREST) {
          return 2;
       }
       if (biome == GenBiomes.MUSHROOM_ISLAND || biome == GenBiomes.MUSHROOM_SHORE) {
          return 0;
       }
       return 1;
    }

    static int deadbushAttempts(int biome) {
       if (biome == GenBiomes.DESERT) {
          return 2;
       }
       if (biome == GenBiomes.SWAMPLAND) {
          return 1;
       }
       return 0;
    }

    static int mushroomAttempts(int biome) {
       if (biome == GenBiomes.SWAMPLAND) {
          return 8;
       }
       if (biome == GenBiomes.MUSHROOM_ISLAND || biome == GenBiomes.MUSHROOM_SHORE) {
          return 1;
       }
       return 0;
    }

    static int reedAttempts(int biome) {
       if (biome == GenBiomes.DESERT) {
          return 60;
       }
       if (biome == GenBiomes.SWAMPLAND) {
          return 20;
       }
       return 10;
    }

    static int cactusAttempts(int biome) {
       if (biome == GenBiomes.DESERT) {
          return 10;
       }
       return 0;
    }

    boolean nearWaterAt(int x, int y, int z) {
       return this.fillCell(x + 1, y, z) == Blocks.WATER_ID
          || this.fillCell(x - 1, y, z) == Blocks.WATER_ID
          || this.fillCell(x, y, z + 1) == Blocks.WATER_ID
          || this.fillCell(x, y, z - 1) == Blocks.WATER_ID;
    }

    boolean isReedSite(int x, int z, int h, int ground, int biome) {
       if (ground != Blocks.SAND_ID && ground != Blocks.GRASS_ID && ground != Blocks.DIRT_ID) {
          return false;
       }
       if (h < SEA_LEVEL - 1 || h > SEA_LEVEL + 1) {
          return false;
       }
       if (!this.nearWaterAt(x, h, z)) {
          return false;
       }
       return nonneg(hash2(this.seed ^ 0x2EEDL, x, z), 256) < reedAttempts(biome);
    }

    boolean isCactusSite(int x, int z, int h, int ground, int biome) {
       return this.isCactusSite(x, z, h, ground, biome, true);
    }

    private boolean isCactusSite(int x, int z, int h, int ground, int biome, boolean sides) {
       if (ground != Blocks.SAND_ID) {
          return false;
       }
       if (h <= SEA_LEVEL + 1) {
          return false;
       }
       if (sides && !this.cactusSidesClear(x, h + 1, z)) {
          return false;
       }
       return nonneg(hash2(this.seed ^ 0xCAC7D5L, x, z), 256) < cactusAttempts(biome);
    }

    private boolean cactusSidesClear(int x, int y, int z) {
       int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
       for (int[] d : dirs) {
          int nx = x + d[0];
          int nz = z + d[1];
          if (this.fillCell(nx, y, nz) != 0) {
             return false;
          }
          int nh = this.heightAt(nx, nz);
          if (nh == y - 1 && this.isCactusSite(nx, nz, nh,
             this.blockAt(nx, nh, nz, nh), this.genBiomeAt(nx, nz), false)) {
             return false;
          }
       }
       return true;
    }

    static int reedHeight(int x, int z, long seed) {
       int r = nonneg(hash2(seed ^ 0x2EED11L, x, z), 18);
       return r < 11 ? 2 : r < 16 ? 3 : 4;
    }

    static int cactusHeight(int x, int z, long seed) {
       int r = nonneg(hash2(seed ^ 0xCAC711L, x, z), 18);
       return r < 11 ? 1 : r < 16 ? 2 : 3;
    }

    int stalkPart(int x, int y, int z, int h) {
       if (y <= h + 1 || y > h + 4) {
          return 0;
       }
       int ground = this.blockAt(x, h, z, h);
       int biome = this.genBiomeAt(x, z);
       if (this.isReedSite(x, z, h, ground, biome)
          && y - h <= reedHeight(x, z, this.seed)) {
          return Blocks.REED_ID;
       }
       if (this.isCactusSite(x, z, h, ground, biome)
          && y - h <= cactusHeight(x, z, this.seed)) {
          return Blocks.CACTUS_ID;
       }
       return 0;
    }

    int groundCover(int x, int z, int h) {
       int ground = this.blockAt(x, h, z, h);
       int biome = this.genBiomeAt(x, z);
       if (this.isReedSite(x, z, h, ground, biome)) {
          return Blocks.REED_ID;
       }
       if (this.isCactusSite(x, z, h, ground, biome)) {
          return Blocks.CACTUS_ID;
       }
       if (ground == Blocks.GRASS_ID && h > SEA_LEVEL + 1 && this.isPumpkinAt(x, z)) {
          return Blocks.PUMPKIN_ID;
       }
        if (GenBiomes.snowy(biome) && h > SEA_LEVEL && ground != Blocks.ICE_ID
           && Blocks.isSolid(ground) && !Blocks.isLeaves(ground)) {
           return Blocks.SNOW_LAYER_ID;
        }
       if (ground == Blocks.SAND_ID) {
          if (nonneg(hash2(this.seed ^ 0xDEAD051L, x, z), 256) < deadbushAttempts(biome)) {
             return Blocks.DEADBUSH_ID;
          }
          return 0;
       }
       if (ground != Blocks.GRASS_ID && ground != Blocks.DIRT_ID && ground != Blocks.MYCELIUM_ID) {
          return 0;
       }
       if (h <= SEA_LEVEL + 1) {
          return 0;
       }
       if (nonneg(hash2(this.seed ^ 0x5A2001L, x, z), 256) < mushroomAttempts(biome)) {
          return nonneg(hash2(this.seed ^ 0xB20A01L, x, z), 3) == 0
             ? Blocks.MUSHROOM_RED_ID : Blocks.MUSHROOM_BROWN_ID;
       }
       if (nonneg(hash2(this.seed ^ 0x610BA1L, x, z), 1024) == 0) {
          return Blocks.MUSHROOM_BROWN_ID;
       }
       if (nonneg(hash2(this.seed ^ 0x610BA2L, x, z), 2048) == 0) {
          return Blocks.MUSHROOM_RED_ID;
       }
       if (nonneg(hash2(this.seed ^ 0xC0E4L, x, z), 256) < flowerAttempts(biome)) {
          return nonneg(hash2(this.seed ^ 0xF10AE2L, x, z), 4) == 0
             ? Blocks.ROSE_ID : Blocks.DANDELION_ID;
       }
       if (nonneg(hash2(this.seed ^ 0x62A55L, x, z), 256) < grassAttempts(biome)) {
          return Blocks.TALL_GRASS_ID;
       }
       return 0;
    }

   private boolean isCave(int x, int y, int z) {
      return this.carver.isCarved(x, y, z);
   }

   public void touchCarve(int ccx, int ccz) {
      this.carver.isCarved(ccx * 16 + 8, 32, ccz * 16 + 8);
   }

}

