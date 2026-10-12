package com.strata.client;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;

public final class TermTmp {
   public static final File DIR = createDir();
   public static final File FRAME = new File(DIR, "frame.bin");
   public static final File INPUT = new File(DIR, "input.bin");
   public static final File LOG = new File(DIR, "game.log");

   private TermTmp() {
   }

   private static File createDir() {
      try {
         java.nio.file.Path p;
         try {
            p = Files.createTempDirectory("strata-",
               PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
         } catch (UnsupportedOperationException e) {
            p = Files.createTempDirectory("strata-");
         }
         File dir = p.toFile();
         final File cleanup = dir;
         Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteRecursively(cleanup)));
         return dir;
      } catch (Exception e) {
         File dir = new File(System.getProperty("java.io.tmpdir", "."), "strata");
         dir.mkdirs();
         return dir;
      }
   }

   public static File exeDir(String name) {
      File d = new File(DIR, name);
      d.mkdirs();
      return d;
   }

   static void deleteRecursively(File f) {
      File[] kids = f.listFiles();
      if (kids != null) {
         for (File k : kids) {
            deleteRecursively(k);
         }
      }
      f.delete();
   }
}

