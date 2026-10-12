package com.strata.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class Profiler {
   private static volatile boolean enabled = false;
   private static final int PRINT_EVERY = 600;
   private static final ThreadLocal<ArrayDeque<Entry>> stacks =
      ThreadLocal.withInitial(ArrayDeque::new);
   private static final HashMap<String, Long> totals = new HashMap<>();
   private static int frames = 0;

   private record Entry(String name, long start) {
   }

   private Profiler() {
   }

   public static void setEnabled(boolean on) {
      synchronized (Profiler.class) {
         enabled = on;
         totals.clear();
         frames = 0;
      }
   }

   public static boolean isEnabled() {
      return enabled;
   }

   public static void push(String name) {
      if (!enabled) {
         return;
      }
      stacks.get().addLast(new Entry(name, System.nanoTime()));
   }

   public static void pop() {
      if (!enabled) {
         return;
      }
      ArrayDeque<Entry> stack = stacks.get();
      if (stack.isEmpty()) {
         return;
      }
      Entry e = stack.removeLast();
      long dt = System.nanoTime() - e.start;
      synchronized (Profiler.class) {
         Long prev = totals.get(e.name);
         totals.put(e.name, prev == null ? dt : prev + dt);
      }
   }

   public static void endFrame() {
      if (!enabled) {
         return;
      }
      synchronized (Profiler.class) {
         frames++;
         if (frames < PRINT_EVERY) {
            return;
         }
         frames = 0;
         ArrayList<Map.Entry<String, Long>> rows = new ArrayList<>(totals.entrySet());
         Collections.sort(rows, new Comparator<Map.Entry<String, Long>>() {
            @Override public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
               return Long.compare(b.getValue(), a.getValue());
            }
         });
         StringBuilder sb = new StringBuilder();
         for (Map.Entry<String, Long> r : rows) {
            if (sb.length() > 0) {
               sb.append(" ");
            }
            sb.append(r.getKey()).append("=").append(String.format("%.1f", r.getValue() / 1000000.0)).append("ms");
         }
         Log.info("prof", sb.toString());
         totals.clear();
      }
   }
}

