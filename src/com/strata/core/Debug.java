package com.strata.core;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Debug {
   private static final ConcurrentHashMap<String, String> workers = new ConcurrentHashMap<>();
   private static final String[] ring = new String[24];
   private static int ringPos = 0;

   private Debug() {
   }

   public static void slow(String tag, long ms, long thresholdMs, String detail) {
      String line = ms + "ms " + detail + " [" + Thread.currentThread().getName() + "]";
      synchronized (ring) {
         ring[ringPos] = tag + ": " + line;
         ringPos = (ringPos + 1) % ring.length;
      }
      if (ms > thresholdMs) {
         Log.warn(tag, "SLOW " + line);
      }
   }

   public static void worker(String name, String state) {
      workers.put(name, state);
   }

   public static void statusLine() {
      StringBuilder sb = new StringBuilder("workers:");
      ArrayList<String> names = new ArrayList<>(workers.keySet());
      java.util.Collections.sort(names);
      for (String n : names) {
         sb.append(" ").append(n).append("=").append(workers.get(n));
      }
      Log.info("dbg", sb.toString());
   }

   public static void dumpRecent() {
      Log.warn("dbg", "recent slow events (newest last):");
      synchronized (ring) {
         for (int i = 0; i < ring.length; i++) {
            String s = ring[(ringPos + i) % ring.length];
            if (s != null) {
               Log.warn("dbg", "  " + s);
            }
         }
      }
   }

   static int ringUsed() {
      int n = 0;
      synchronized (ring) {
         for (String s : ring) {
            if (s != null) {
               n++;
            }
         }
      }
      return n;
   }

   static Map<String, String> workerStates() {
      return new ConcurrentHashMap<>(workers);
   }
}

