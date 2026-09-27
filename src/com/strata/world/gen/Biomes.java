package com.strata.world.gen;

import com.strata.blocks.Blocks;

import java.util.HashMap;
import java.util.Map;

public final class Biomes {
   private static final Map<String, Biome> REGISTRY = new HashMap<>();

   static {
      register(new Plains());
      register(new Hills());
   }

   private Biomes() {
   }

   public static void register(Biome biome) {
      REGISTRY.put(biome.id(), biome);
   }

   public static Biome get(String id) {
      return REGISTRY.get(id);
   }

   public static Biome plains() {
      return REGISTRY.get("plains");
   }

   public static Biome hills() {
      return REGISTRY.get("hills");
   }

   public static class Plains implements Biome {
      @Override public String id() { return "plains"; }
      @Override public float baseHeight() { return 41.0F; }
      @Override public float amplitude() { return 2.5F; }
      @Override public int surfaceBlock() { return Blocks.GRASS_ID; }
      @Override public int subsurfaceBlock() { return Blocks.DIRT_ID; }
      @Override public int shoreBlock() { return Blocks.SAND_ID; }
   }

   public static class Hills implements Biome {
      @Override public String id() { return "hills"; }
      @Override public float baseHeight() { return 44.0F; }
      @Override public float amplitude() { return 12.0F; }
      @Override public int surfaceBlock() { return Blocks.GRASS_ID; }
      @Override public int subsurfaceBlock() { return Blocks.DIRT_ID; }
      @Override public int shoreBlock() { return Blocks.SAND_ID; }
   }

   public static class NoiseProvider implements BiomeProvider {
      private final Noise noise;

      public NoiseProvider(long seed) {
         this.noise = new Noise(seed ^ 0x9E3779B97F4A7C15L);
      }

      private double mask(int x, int z) {
         double n = this.noise.fbm2(x / 256.0, z / 256.0, 2);
         double t = n / 0.2;
         if (t < 0.0) {
            t = 0.0;
         }
         if (t > 1.0) {
            t = 1.0;
         }
         return t * t * (3.0 - 2.0 * t);
      }

      @Override
      public Biome biomeAt(int x, int z) {
         return this.mask(x, z) > 0.5 ? hills() : plains();
      }

      @Override
      public double blend(int x, int z) {
         return this.mask(x, z);
      }
   }
}

