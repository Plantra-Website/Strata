package com.strata.world.gen;

public interface Biome {
   String id();

   float baseHeight();

   float amplitude();

   int surfaceBlock();

   int subsurfaceBlock();

   int shoreBlock();
}

