package com.strata.core;

import java.util.HashMap;
import java.util.Map;

public final class GlResources {
   private static final HashMap<Integer, String> live = new HashMap<>();
   private static int peak = 0;

   private GlResources() {
   }

   public static synchronized void track(int id, String owner) {
      if (id != 0) {
         live.put(id, owner);
         if (live.size() > peak) {
            peak = live.size();
         }
      }
   }

   public static synchronized void release(int id) {
      live.remove(id);
   }

   public static synchronized int live() {
      return live.size();
   }

   public static synchronized int peak() {
      return peak;
   }

   public static synchronized void dump() {
      Log.info("gl", "live objects: " + live.size() + " (peak " + peak + ")");
      for (Map.Entry<Integer, String> e : live.entrySet()) {
         Log.info("gl", "  id " + e.getKey() + " owner " + e.getValue());
      }
   }

   public static synchronized void reset() {
      live.clear();
      peak = 0;
   }
}

