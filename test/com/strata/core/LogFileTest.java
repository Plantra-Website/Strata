package com.strata.core;

public class LogFileTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) throws Exception {
      java.io.File f = new java.io.File("logfiletest.log");
      if (f.exists() && !f.delete()) {
         check(false, "cannot clear stale log");
      }
      Log.setFile(f);
      try {
         Log.info("t", "hello-info");
         Log.warn("t", "hello-warn");
         Log.raw("hello-raw");
         Log.error("t", "hello-error");
      } finally {
         Log.closeFile();
      }
      String body = new String(java.nio.file.Files.readAllBytes(f.toPath()),
         java.nio.charset.StandardCharsets.UTF_8);
      check(body.contains("[t] hello-info"), "info in file");
      check(body.contains("[t] hello-warn"), "warn in file");
      check(body.contains("hello-raw"), "raw in file");
      check(body.contains("[t] ERROR hello-error"), "error in file");
      check(f.delete(), "log cleans up");
      Log.setFile(null);
      Log.info("t", "back-on-stdout");

      if (failures > 0) { System.out.println(failures + " FAILURES"); System.exit(1); }
      System.out.println("logfile ok");
   }
}

