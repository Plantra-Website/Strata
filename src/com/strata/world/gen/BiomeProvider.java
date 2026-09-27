package com.strata.world.gen;

public interface BiomeProvider {
   Biome biomeAt(int x, int z);

   double blend(int x, int z);
}

