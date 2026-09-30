package com.strata.blocks;

import com.strata.core.Json;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class BlockTags {
   private BlockTags() {
   }

   private static volatile boolean loaded;
   private static final Object LOCK = new Object();

   private static final boolean[] FLUID = new boolean[256];
   private static final boolean[] LEAVES = new boolean[256];
   private static final boolean[] LOGS = new boolean[256];
   private static final boolean[] SAPLINGS = new boolean[256];
   private static final boolean[] FLOWERS = new boolean[256];
   private static final boolean[] MUSHROOMS = new boolean[256];
   private static final boolean[] ORES = new boolean[256];
   private static final boolean[] FALLING = new boolean[256];

   public static void load() {
      ensure();
   }

   public static boolean fluid(int id) {
      ensure();
      return id >= 0 && id < 256 && FLUID[id];
   }

   public static boolean leaves(int id) {
      ensure();
      return id >= 0 && id < 256 && LEAVES[id];
   }

   public static boolean logs(int id) {
      ensure();
      return id >= 0 && id < 256 && LOGS[id];
   }

   public static boolean saplings(int id) {
      ensure();
      return id >= 0 && id < 256 && SAPLINGS[id];
   }

   public static boolean flowers(int id) {
      ensure();
      return id >= 0 && id < 256 && FLOWERS[id];
   }

   public static boolean mushrooms(int id) {
      ensure();
      return id >= 0 && id < 256 && MUSHROOMS[id];
   }

   public static boolean ores(int id) {
      ensure();
      return id >= 0 && id < 256 && ORES[id];
   }

   public static boolean falling(int id) {
      ensure();
      return id >= 0 && id < 256 && FALLING[id];
   }

   private static void ensure() {
      if (!loaded) {
         synchronized (LOCK) {
            if (!loaded) {
               read("fluid", FLUID);
               read("leaves", LEAVES);
               read("logs", LOGS);
               read("saplings", SAPLINGS);
               read("flowers", FLOWERS);
               read("mushrooms", MUSHROOMS);
               read("ores", ORES);
               read("falling", FALLING);
               loaded = true;
            }
         }
      }
   }

   private static void read(String name, boolean[] set) {
      String path = "/tags/" + name + ".json";
      InputStream in = BlockTags.class.getResourceAsStream(path);
      if (in == null) {
         throw new RuntimeException("missing block tag res" + path + " (ship it under res/tags/)");
      }
      String text = readAll(in, path);
      Map<String, Object> root;
      try {
         root = Json.parseObject(text);
      } catch (RuntimeException e) {
         throw new RuntimeException("bad block tag res" + path + ": " + e.getMessage());
      }
      Object values = root.get("values");
      if (!(values instanceof List)) {
         throw new RuntimeException("bad block tag res" + path + ": missing \"values\" array");
      }
      for (Object v : (List<?>)values) {
         if (!(v instanceof Double) || ((Double)v).doubleValue() != Math.floor(((Double)v).doubleValue())) {
            throw new RuntimeException("bad block tag res" + path + ": non-id value " + v);
         }
         int id = ((Double)v).intValue();
         if (id <= 0 || id >= 256 || Blocks.byId(id) == null) {
            throw new RuntimeException("bad block tag res" + path + ": unknown block id " + v);
         }
         set[id] = true;
      }
   }

   private static String readAll(InputStream in, String path) {
      try {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         byte[] buf = new byte[4096];
         int n;
         while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
         }
         in.close();
         return new String(out.toByteArray(), StandardCharsets.UTF_8);
      } catch (java.io.IOException e) {
         throw new RuntimeException("bad block tag res" + path + ": unreadable (" + e.getMessage() + ")");
      }
   }
}

