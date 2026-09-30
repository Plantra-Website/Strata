package com.strata.core;

public final class Config {
   private Config() {
   }

   public static int VIEW_RADIUS = 6;
   public static final int PREBUILD_RADIUS = 2;
   public static final int SUBMIT_BUDGET = 8;
   public static final int UPLOAD_BUDGET = 8;
   public static final int MESH_WORKERS = 5;

   public static boolean BLOCK_ROTATION = false;

   public static boolean FANCY_LEAVES = true;

   public static boolean LEAF_BLACKOUT = false;

   public static final long SLOW_MESH_MS = 150L;
   public static final long SLOW_UPLOAD_MS = 25L;
   public static final long SLOW_SETTILE_MS = 25L;
   public static final long SLOW_FRAME_MS = 250L;
   public static final int DBG_STATUS_EVERY = 300;

   public static final long DAY_LENGTH = 28800L;
   public static final String VERSION = "0.1.0.0";
   public static final int AUTOSAVE_TICKS = 18000;
}

