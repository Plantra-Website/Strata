package com.strata.client;

import com.strata.core.Log;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TerminalViewer {
   private static final String[] VIEWER_FLAGS = {"--mode", "--fps", "--scale", "--once"};
   private static final String[] GAME_FLAGS = {"--seed", "--world", "--import", "--pack", "--terminal"};

   public static int slurp(String[] args, int from, List<String> out) {
      int i = from;
      while (i < args.length) {
         String a = args[i];
         if ("--file".equals(a)) {
            Log.warn("term", "--file is fixed to " + FrameTap.PATH + ", ignoring viewer override");
            i++;
            if (i < args.length && !args[i].startsWith("--")) {
               i++;
            }
            continue;
         }
         boolean known = false;
         for (String f : VIEWER_FLAGS) {
            if (f.equals(a)) {
               known = true;
               break;
            }
         }
         if (!known) {
            break;
         }
         out.add(a);
         i++;
         if (!"--once".equals(a) && i < args.length && !args[i].startsWith("--")) {
            out.add(args[i]);
            i++;
         }
      }
      return i;
   }

   public static int viewScale(java.util.List<String> viewerArgs) {
      int scale = 1;
      for (int i = 0; i < viewerArgs.size(); i++) {
         if ("--scale".equals(viewerArgs.get(i)) && i + 1 < viewerArgs.size()) {
            try {
               int v = Integer.parseInt(viewerArgs.get(i + 1).trim());
               if (v >= 1 && v <= 8) {
                  scale = v;
               } else {
                  Log.warn("term", "ignoring out-of-range --scale " + viewerArgs.get(i + 1));
               }
            } catch (NumberFormatException e) {
               Log.warn("term", "ignoring bad --scale " + viewerArgs.get(i + 1));
            }
         }
      }
      return scale;
   }

   static File resolve() {
      String env = System.getenv("STRATA_VIEWER");
      if (env != null && !env.isEmpty()) {
         File f = new File(env);
         if (f.isFile() && f.canExecute()) {
            return f;
         }
         Log.warn("term", "STRATA_VIEWER=" + env + " not executable, trying defaults");
      }
      File dev = new File(System.getProperty("user.dir", "."), "viewer");
      if (dev.isFile() && dev.canExecute()) {
         return dev;
      }
      try {
         String resName = null;
         InputStream in = TerminalViewer.class.getResourceAsStream("/terminal/viewer.exe");
         if (in != null) {
            resName = "viewer.exe";
         } else {
            in = TerminalViewer.class.getResourceAsStream("/terminal/viewer");
            if (in != null) {
               resName = "viewer";
            }
         }
         if (in != null) {
            File dir = TermTmp.exeDir("bin");
            File out = new File(dir, resName);
            try (InputStream src = in) {
               Files.copy(src, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            out.setExecutable(true);
            if (out.canExecute() || File.separatorChar == '\\') {
               return out;
            }
         }
      } catch (Exception e) {
         Log.warn("term", "viewer extract failed: " + e.getMessage());
      }
      return null;
   }

   static java.util.List<String> buildCommand(File bin, FrameTap tap, java.util.List<String> viewerArgs) {
      java.util.List<String> cmd = new java.util.ArrayList<>();
      cmd.add(bin.getAbsolutePath());
      cmd.add(String.valueOf(tap.tapWidth()));
      cmd.add(String.valueOf(tap.tapHeight()));
      cmd.addAll(viewerArgs);
      cmd.add("--file");
      cmd.add(FrameTap.PATH);
      return cmd;
   }

   public static Process launch(FrameTap tap, List<String> viewerArgs) {
      if (tap == null) {
         return null;
      }
      File bin;
      try {
         bin = resolve();
      } catch (Throwable t) {
         Log.warn("term", "viewer resolve failed: " + t.getMessage());
         return null;
      }
      if (bin == null) {
         Log.info("term", "no viewer binary (./viewer or jar resource), game runs without terminal mirror");
         return null;
      }
      List<String> cmd = buildCommand(bin, tap, viewerArgs);
      try {
         ProcessBuilder pb = new ProcessBuilder(cmd);
         pb.inheritIO();
         String lib = System.getenv("STRATA_VIEWER_LIB");
         if ((lib == null || lib.isEmpty()) && new File("/tmp/ij-plugins").exists()) {
            lib = "/tmp/ij-plugins/extensions/json-c/lib"
               + ":/tmp/ij-plugins/extensions/gmp/lib"
               + ":/tmp/ij-plugins/extensions/gnucobol/lib";
         }
         if (lib != null && !lib.isEmpty()) {
            Map<String, String> e = pb.environment();
            e.put("DYLD_LIBRARY_PATH", lib);
            e.put("LD_LIBRARY_PATH", lib);
         }
         Process p = pb.start();
         Log.info("term", "viewer pid " + p.pid() + ": " + String.join(" ", cmd));
         return p;
      } catch (Throwable t) {
         Log.warn("term", "viewer launch failed (" + t.getMessage() + "), game runs without terminal mirror");
         return null;
      }
   }

   public static void stop(Process p) {
      if (p == null || !p.isAlive()) {
         return;
      }
      try {
         p.destroy();
         if (!p.waitFor(2L, TimeUnit.SECONDS)) {
            p.destroyForcibly();
         }
      } catch (Throwable t) {
      }
   }

   static File resolveBridge() {
      String env = System.getenv("STRATA_TERMINPUT");
      if (env != null && !env.isEmpty()) {
         File f = new File(env);
         if (f.isFile() && f.canExecute()) {
            return f;
         }
         Log.warn("term", "STRATA_TERMINPUT=" + env + " not executable, trying defaults");
      }
      File dev = new File(System.getProperty("user.dir", "."), "terminput");
      if (dev.isFile() && dev.canExecute()) {
         return dev;
      }
      try {
         InputStream in = TerminalViewer.class.getResourceAsStream("/terminal/terminput");
         if (in != null) {
            File dir = TermTmp.exeDir("bin");
            File out = new File(dir, "terminput");
            try (InputStream src = in) {
               Files.copy(src, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            out.setExecutable(true);
            if (out.canExecute()) {
               return out;
            }
         }
      } catch (Exception e) {
         Log.warn("term", "input bridge extract failed: " + e.getMessage());
      }
      return null;
   }

   public static Process launchInput() {
      File bin;
      try {
         bin = resolveBridge();
      } catch (Throwable t) {
         Log.warn("term", "input bridge resolve failed: " + t.getMessage());
         return null;
      }
      if (bin == null) {
         Log.info("term", "no input bridge binary, playing via window input");
         return null;
      }
      List<String> cmd = new ArrayList<>();
      cmd.add(bin.getAbsolutePath());
      cmd.add(TerminalInput.PATH);
      cmd.add(FrameTap.PATH);
      try {
         ProcessBuilder pb = new ProcessBuilder(cmd);
         pb.inheritIO();
         Process p = pb.start();
         Log.info("term", "input bridge pid " + p.pid() + ": " + String.join(" ", cmd));
         return p;
      } catch (Throwable t) {
         Log.warn("term", "input bridge launch failed (" + t.getMessage() + "), playing via window input");
         return null;
      }
   }
}

