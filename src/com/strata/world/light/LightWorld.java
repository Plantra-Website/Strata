package com.strata.world.light;

public interface LightWorld {
   int depth();

   boolean isLightBlocker(int x, int y, int z);

   void lightColumnChanged(int x, int z, int y0, int y1);
}

