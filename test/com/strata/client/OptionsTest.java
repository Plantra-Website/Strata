package com.strata.client;

import java.io.File;
import java.io.PrintWriter;
import org.lwjgl.input.Keyboard;

public class OptionsTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) throws Exception {
      Options d = Options.load(new File("does-not-exist-options.txt"));
      check(d.keyFwd == Keyboard.KEY_W, "default fwd is W");
      check(d.keyQuit == Keyboard.KEY_DELETE, "default quit is Delete");
      check(d.keyRelease == Keyboard.KEY_ESCAPE, "default release is ESC");
      check(d.viewRadius == com.strata.core.Config.VIEW_RADIUS, "default radius matches Config");
      check(d.sensitivity == 1.0F, "default sensitivity 1");

      Options o = new Options();
      o.keyFwd = Keyboard.KEY_E;
      o.keyQuit = Keyboard.KEY_Q;
      o.keyGive = Keyboard.KEY_G;
      o.viewRadius = 9;
      o.guiScale = 2;
      o.sensitivity = 2.5F;
      o.tintBlend = false;
      File f = new File("options-roundtrip.txt");
      o.save(f);
      Options back = Options.load(f);
      check(back.keyFwd == Keyboard.KEY_E, "fwd roundtrips");
      check(back.keyQuit == Keyboard.KEY_Q, "quit roundtrips");
      check(back.keyGive == Keyboard.KEY_G, "give key roundtrips");
      check(back.viewRadius == 9, "radius roundtrips");
      check(back.guiScale == 2, "gui scale roundtrips");
      check(Gui.autoScale(1024, 768) == 3, "auto scale 1024x768");
      check(Gui.autoScale(640, 480) == 2, "auto scale 640x480");
      check(Gui.autoScale(320, 240) == 1, "auto scale minimum");
      check(Gui.autoScale(3840, 2160) == 3, "auto caps at vanilla 3 on huge screens");
      check(Gui.resolveScale(2, 9999, 9999) == 2, "pinned scale wins");
      check(back.sensitivity == 2.5F, "sensitivity roundtrips");
      check(!back.tintBlend, "tint blend roundtrips");
      f.delete();

      File bad = new File("options-bad.txt");
      PrintWriter w = new PrintWriter(new java.io.FileWriter(bad));
      w.println("keyFwd=KEY_NOT_A_KEY");
      w.println("viewRadius=banana");
      w.println("sensitivity=99.0");
      w.println("bogusOption=123");
      w.println("no-equals-sign");
      w.close();
      Options b = Options.load(bad);
      check(b.keyFwd == Keyboard.KEY_W, "unknown key falls back");
      check(b.viewRadius == com.strata.core.Config.VIEW_RADIUS, "bad radius falls back");
      check(b.sensitivity == 1.0F, "wild sensitivity resets");
      bad.delete();

      File clamp = new File("options-clamp.txt");
      PrintWriter c = new PrintWriter(new java.io.FileWriter(clamp));
      c.println("viewRadius=99");
      c.println("sensitivity=0.01");
      c.close();
      Options m = Options.load(clamp);
      check(m.viewRadius == 10, "radius clamps high");
      check(m.sensitivity == 1.0F, "tiny sensitivity resets");
      clamp.delete();

      if (failures == 0) System.out.println("OPTIONS PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

