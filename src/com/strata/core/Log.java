package com.strata.core;

public final class Log {
   public static final int ERROR = 0;
   public static final int WARN = 1;
   public static final int INFO = 2;

   private static int level = INFO;
   private static boolean quiet = false;

   private Log() {
   }

   public static void setLevel(int newLevel) {
      level = newLevel;
   }

   public static void setQuiet(boolean q) {
      quiet = q;
   }

   public static void info(String tag, String msg) {
      if (!quiet && level >= INFO) {
         System.out.println("[" + tag + "] " + msg);
      }
   }

   public static void warn(String tag, String msg) {
      if (!quiet && level >= WARN) {
         System.out.println("[" + tag + "] " + msg);
      }
   }

   public static void error(String tag, String msg) {
      System.err.println("[" + tag + "] ERROR " + msg);
   }

   public static void error(String tag, String msg, Throwable e) {
      System.err.println("[" + tag + "] ERROR " + msg + ": " + e);
      e.printStackTrace(System.err);
   }

   public static void raw(String msg) {
      if (!quiet) {
         System.out.println(msg);
      }
   }
}

