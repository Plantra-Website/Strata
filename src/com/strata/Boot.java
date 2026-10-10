package com.strata;

import com.strata.client.GameClient;
import com.strata.core.Log;

public class Boot {
   public static void main(String[] args) {
      preloadNatives();
      GameClient client = new GameClient();
      for (int i = 0; i < args.length; i++) {
         if ("--seed".equals(args[i]) && i + 1 < args.length) {
            try {
               client.setSeedOverride(Long.parseLong(args[++i]));
            } catch (NumberFormatException e) {
               Log.error("boot", "bad --seed value, using stored/default");
            }
          } else if ("--world".equals(args[i]) && i + 1 < args.length) {
             client.setWorldDir(new java.io.File(cleanArg(args[++i])));
          } else if ("--import".equals(args[i]) && i + 1 < args.length) {
             client.setImportImage(new java.io.File(cleanArg(args[++i])));
          } else if ("--pack".equals(args[i]) && i + 1 < args.length) {
             client.setPackName(cleanArg(args[++i]));
          } else if ("--terminal".equals(args[i])) {
             String spec = null;
             if (i + 1 < args.length && args[i + 1].matches("\\d+x\\d+(:\\d+)?")) {
                spec = args[++i];
             }
             client.setTerminalTap(spec);
          }
      }
       client.applyImportDefaults();
       client.run();
    }

    private static String cleanArg(String a) {
       String p = a.trim();
       if (p.length() >= 2 && p.startsWith("\"") && p.endsWith("\"")) {
          p = p.substring(1, p.length() - 1).trim();
       }
       return p;
    }

   static String nativeLibName(String os, String arch) {
      String o = os.toLowerCase(java.util.Locale.ROOT);
      boolean mac = o.contains("mac") || o.contains("darwin");
      boolean win = o.contains("win");
      if (mac) {
         return "liblwjgl.dylib";
      }
      if (win) {
         return "lwjgl64.dll";
      }
      return "liblwjgl64.so";
   }

   private static void preloadNatives() {
      try {
         String lib = nativeLibName(System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
         java.io.InputStream in = Boot.class.getResourceAsStream("/native/" + lib);
         if (in != null) {
            java.io.File tmp = new java.io.File(System.getProperty("java.io.tmpdir"), "strata-natives");
            tmp.mkdirs();
            java.io.File out = new java.io.File(tmp, lib);
            java.nio.file.Files.copy(in, out.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            in.close();
            System.load(out.getAbsolutePath());
            System.setProperty("org.lwjgl.librarypath", tmp.getAbsolutePath());
            Log.info("natives", "loaded bundled " + out.getAbsolutePath());
            return;
         }
      } catch (Exception e) {
         Log.warn("natives", "bundled extract failed, trying dirs: " + e.getMessage());
      }
      String[] candidates = {
         System.getProperty("user.dir") + "/lib/native",
         System.getProperty("user.dir") + "/../lib/native",
         System.getProperty("user.home") + "/Library/Java/Extensions"
      };
      String[] libs = {"liblwjgl.dylib"};
      java.util.HashSet<String> loaded = new java.util.HashSet<>();
      boolean anyLoaded = false;
      for (String dir : candidates) {
         for (String lib : libs) {
            if (loaded.contains(lib)) {
               continue;
            }
            java.io.File f = new java.io.File(dir, lib);
            if (!f.isFile()) {
               continue;
            }
            try {
               System.load(f.getAbsolutePath());
                Log.info("natives", "loaded " + f.getAbsolutePath());
               if (System.getProperty("org.lwjgl.librarypath") == null) {
                  System.setProperty("org.lwjgl.librarypath", dir);
               }
               loaded.add(lib);
               anyLoaded = true;
            } catch (UnsatisfiedLinkError e) {
                Log.warn("natives", "FAILED " + f.getAbsolutePath() + ": " + e.getMessage());
            }
         }
      }
      if (!anyLoaded) {
       Log.warn("natives", "no native dir found, falling back to java.library.path=" + System.getProperty("java.library.path"));
      }
   }
}

