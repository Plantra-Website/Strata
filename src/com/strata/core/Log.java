package com.strata.core;

public final class Log {
   public static final int ERROR = 0;
   public static final int WARN = 1;
   public static final int INFO = 2;

   private static int level = INFO;
   private static boolean quiet = false;
   private static java.io.PrintWriter fileOut = null;

   private Log() {
   }

   public static void setLevel(int newLevel) {
      level = newLevel;
   }

   public static void setQuiet(boolean q) {
      quiet = q;
   }

   public static synchronized void setFile(java.io.File f) {
      closeFile();
      if (f == null) {
         return;
      }
      try {
         fileOut = new java.io.PrintWriter(new java.io.FileWriter(f, false), true);
      } catch (java.io.IOException e) {
         fileOut = null;
         error("log", "cannot open log file " + f + ", staying on stdout");
      }
   }

   public static synchronized void closeFile() {
      if (fileOut != null) {
         fileOut.close();
         fileOut = null;
      }
   }

   public static synchronized void info(String tag, String msg) {
      if (!quiet && level >= INFO) {
         String line = "[" + tag + "] " + msg;
         if (fileOut != null) {
            fileOut.println(line);
         } else {
            System.out.println(line);
         }
      }
   }

   public static synchronized void warn(String tag, String msg) {
      if (!quiet && level >= WARN) {
         String line = "[" + tag + "] " + msg;
         if (fileOut != null) {
            fileOut.println(line);
         } else {
            System.out.println(line);
         }
      }
   }

   public static synchronized void error(String tag, String msg) {
      String line = "[" + tag + "] ERROR " + msg;
      System.err.println(line);
      if (fileOut != null) {
         fileOut.println(line);
      }
   }

   public static synchronized void error(String tag, String msg, Throwable e) {
      String line = "[" + tag + "] ERROR " + msg + ": " + e;
      System.err.println(line);
      e.printStackTrace(System.err);
      if (fileOut != null) {
         fileOut.println(line);
         e.printStackTrace(fileOut);
      }
   }

   public static synchronized void raw(String msg) {
      if (!quiet) {
         if (fileOut != null) {
            fileOut.println(msg);
         } else {
            System.out.println(msg);
         }
      }
   }
}

