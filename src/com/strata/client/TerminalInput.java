package com.strata.client;

import com.strata.core.Log;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TerminalInput {
   public static final String PATH = defaultPath();
   private static final long FRESH_MS = 2000L;

   private static String testPath = null;

   private static String defaultPath() {
      String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
      if (os.contains("win")) {
         String tmp = System.getProperty("java.io.tmpdir", ".");
         if (!tmp.endsWith("\\") && !tmp.endsWith("/")) {
            tmp += File.separator;
         }
         return tmp + "strata-input.bin";
      }
      return "/tmp/strata-input.bin";
   }

   static void setPathForTest(String p) {
      testPath = p;
   }

   private static File inputFile() {
      return testPath != null ? new File(testPath) : new File(PATH);
   }

   private final Set<Integer> keysDown = new HashSet<>();
   private final Map<String, Integer> nameCache = new HashMap<>();
   private final Set<String> warnedNames = new HashSet<>();
   private int lastSeq = -1;
   private int mouseX = -1;
   private int mouseY = -1;
   private int buttons = 0;
   private int wheelAcc = 0;
   private int lastWheelApplied = 0;
   private int pendingDX = 0;
   private int pendingDY = 0;
   private int pendingWheel = 0;
   private boolean fresh = false;

   public boolean poll() {
      File f = inputFile();
      if (!f.isFile() || System.currentTimeMillis() - f.lastModified() > FRESH_MS) {
         fresh = false;
         return false;
      }
      java.util.List<String> lines;
      try {
         lines = Files.readAllLines(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
      } catch (Exception e) {
         fresh = false;
         return false;
      }
      int seq = -1;
      Set<Integer> keys = new HashSet<>();
      int mx = -1;
      int my = -1;
      int btns = 0;
      int wheel = wheelAcc;
      boolean haveMouse = false;
      for (String raw : lines) {
         String line = raw.trim();
         if (line.startsWith("SEQ ")) {
            try {
               seq = Integer.parseInt(line.substring(4).trim());
            } catch (NumberFormatException e) {
               return keepStale();
            }
         } else if (line.equals("KEYS") || line.startsWith("KEYS ")) {
            String rest = line.length() > 4 ? line.substring(4).trim() : "";
            if (!rest.isEmpty()) {
               for (String name : rest.split("\\s+")) {
                  int code = resolve(name);
                  if (code != 0) {
                     keys.add(code);
                  }
               }
            }
         } else if (line.startsWith("MOUSE ")) {
            String[] p = line.substring(6).trim().split("\\s+");
            if (p.length < 3) {
               return keepStale();
            }
            try {
               mx = Integer.parseInt(p[0]);
               my = Integer.parseInt(p[1]);
               btns = Integer.parseInt(p[2]);
               haveMouse = true;
            } catch (NumberFormatException e) {
               return keepStale();
            }
         } else if (line.startsWith("WHEEL ")) {
            try {
               wheel = Integer.parseInt(line.substring(6).trim());
            } catch (NumberFormatException e) {
               return keepStale();
            }
         }
      }
      if (seq < 0) {
         return keepStale();
      }
      if (seq == lastSeq) {
         fresh = true;
         return true;
      }
      lastSeq = seq;
      keysDown.clear();
      keysDown.addAll(keys);
      if (haveMouse) {
         if (mouseX >= 0 && mouseY >= 0) {
            pendingDX += mx - mouseX;
            pendingDY += my - mouseY;
         }
         mouseX = mx;
         mouseY = my;
         buttons = btns;
      }
      pendingWheel += wheel - lastWheelApplied;
      if (wheel < lastWheelApplied) {
         pendingWheel = 0;
      }
      lastWheelApplied = wheel;
      fresh = true;
      return true;
   }

   private boolean keepStale() {
      fresh = false;
      return false;
   }

   private int resolve(String name) {
      Integer cached = nameCache.get(name);
      if (cached != null) {
         return cached;
      }
      int code = 0;
      try {
         code = org.lwjgl.input.Keyboard.getKeyIndex(name);
      } catch (Throwable t) {
         code = 0;
      }
      if (code == 0 && warnedNames.add(name)) {
         Log.warn("term", "unknown key name '" + name + "', ignoring");
      }
      nameCache.put(name, code);
      return code;
   }

   public boolean isFresh() {
      return fresh;
   }

   public int mouseCellX() {
      return mouseX;
   }

   public int mouseCellY() {
      return mouseY;
   }

   public boolean isDown(int lwjglCode) {
      return fresh && keysDown.contains(lwjglCode);
   }

   public boolean isButtonDown(int btn) {
      return fresh && (buttons & (1 << btn)) != 0;
   }

   public int drainDX() {
      int d = pendingDX;
      pendingDX = 0;
      return d;
   }

   public int drainDY() {
      int d = pendingDY;
      pendingDY = 0;
      return d;
   }

   public int drainWheel() {
      int d = pendingWheel;
      pendingWheel = 0;
      return d;
   }
}

