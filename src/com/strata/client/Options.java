package com.strata.client;

import com.strata.core.Config;
import com.strata.core.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import org.lwjgl.input.Keyboard;

public class Options {
   public int keyFwd = Keyboard.KEY_W;
   public int keyBack = Keyboard.KEY_S;
   public int keyLeft = Keyboard.KEY_A;
   public int keyRight = Keyboard.KEY_D;
   public int keyJump = Keyboard.KEY_SPACE;
   public int keyDown = Keyboard.KEY_LSHIFT;
   public int keySave = Keyboard.KEY_RETURN;
   public int keyRespawn = Keyboard.KEY_R;
   public int keySpectator = Keyboard.KEY_APOSTROPHE;
   public int keyFullbright = Keyboard.KEY_BACKSLASH;
   public int keyRelease = Keyboard.KEY_ESCAPE;
   public int keyQuit = Keyboard.KEY_DELETE;
   public int keyInventory = Keyboard.KEY_E;
   public int keyGive = Keyboard.KEY_EQUALS;
   public int viewRadius = Config.VIEW_RADIUS;
   public float sensitivity = 1.0F;
   public int guiScale = 0;

   public static Options load(File file) {
      Options o = new Options();
      if (!file.isFile()) {
         return o;
      }
      try {
         BufferedReader in = new BufferedReader(new FileReader(file));
         String line;
         while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
               continue;
            }
            int eq = line.indexOf('=');
            if (eq < 0) {
               continue;
            }
            o.set(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
         }
         in.close();
      } catch (Exception e) {
         Log.warn("options", "could not read " + file + ", using defaults: " + e.getMessage());
      }
      o.clamp();
      return o;
   }

   private void set(String name, String value) {
      try {
         if (name.equals("viewRadius")) {
            this.viewRadius = Integer.parseInt(value);
            return;
         }
         if (name.equals("sensitivity")) {
            this.sensitivity = Float.parseFloat(value);
            return;
         }
         if (name.equals("guiScale")) {
            this.guiScale = Integer.parseInt(value);
            return;
         }
         int key = Keyboard.getKeyIndex(value);
         if (key == Keyboard.KEY_NONE) {
            Log.warn("options", "unknown key '" + value + "' for " + name + ", keeping default");
            return;
         }
         if (name.equals("keyFwd")) this.keyFwd = key;
         else if (name.equals("keyBack")) this.keyBack = key;
         else if (name.equals("keyLeft")) this.keyLeft = key;
         else if (name.equals("keyRight")) this.keyRight = key;
         else if (name.equals("keyJump")) this.keyJump = key;
         else if (name.equals("keyDown")) this.keyDown = key;
         else if (name.equals("keySave")) this.keySave = key;
         else if (name.equals("keyRespawn")) this.keyRespawn = key;
         else if (name.equals("keySpectator")) this.keySpectator = key;
         else if (name.equals("keyFullbright")) this.keyFullbright = key;
         else if (name.equals("keyRelease")) this.keyRelease = key;
         else if (name.equals("keyQuit")) this.keyQuit = key;
         else if (name.equals("keyInventory")) this.keyInventory = key;
         else if (name.equals("keyGive")) this.keyGive = key;
         else Log.warn("options", "unknown option '" + name + "', ignoring");
      } catch (NumberFormatException e) {
         Log.warn("options", "bad value '" + value + "' for " + name + ", keeping default");
      }
   }

   private void clamp() {
      if (this.viewRadius < 2) this.viewRadius = 2;
      if (this.viewRadius > 10) this.viewRadius = 10;
      if (!(this.sensitivity >= 0.1F) || !(this.sensitivity <= 3.0F)) this.sensitivity = 1.0F;
      if (this.guiScale < 0 || this.guiScale > 3) this.guiScale = 0;
   }

   public void save(File file) {
      try {
         PrintWriter out = new PrintWriter(new FileWriter(file));
         out.println("# Strata options — key names like KEY_W, KEY_SPACE (LWJGL Keyboard).");
         out.println("# Unknown names/values fall back to defaults; delete to regenerate.");
         out.println("keyFwd=" + Keyboard.getKeyName(this.keyFwd));
         out.println("keyBack=" + Keyboard.getKeyName(this.keyBack));
         out.println("keyLeft=" + Keyboard.getKeyName(this.keyLeft));
         out.println("keyRight=" + Keyboard.getKeyName(this.keyRight));
         out.println("keyJump=" + Keyboard.getKeyName(this.keyJump));
         out.println("keyDown=" + Keyboard.getKeyName(this.keyDown));
         out.println("keySave=" + Keyboard.getKeyName(this.keySave));
         out.println("keyRespawn=" + Keyboard.getKeyName(this.keyRespawn));
         out.println("keySpectator=" + Keyboard.getKeyName(this.keySpectator));
         out.println("keyFullbright=" + Keyboard.getKeyName(this.keyFullbright));
         out.println("keyRelease=" + Keyboard.getKeyName(this.keyRelease));
         out.println("keyQuit=" + Keyboard.getKeyName(this.keyQuit));
         out.println("keyInventory=" + Keyboard.getKeyName(this.keyInventory));
         out.println("keyGive=" + Keyboard.getKeyName(this.keyGive));
         out.println("viewRadius=" + this.viewRadius);
         out.println("sensitivity=" + this.sensitivity);
         out.println("guiScale=" + this.guiScale);
         out.close();
      } catch (Exception e) {
         Log.error("options", "save failed", e);
      }
   }
}

