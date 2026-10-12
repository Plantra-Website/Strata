package com.strata.world;

import com.strata.core.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.util.HashMap;
import java.util.Map;

public final class SessionLock {
   private static final Map<File, Lock> LOCKS = new HashMap<>();

   private SessionLock() {
   }

   public static synchronized Lock acquire(File worldDir) {
      File absDir = worldDir.getAbsoluteFile();
      Lock existing = LOCKS.get(absDir);
      if (existing != null) {
         return existing; 
      }
      File lockFile = new File(absDir, "session.lock");
      RandomAccessFile raf = null;
      try {
         raf = new RandomAccessFile(lockFile, "rw");
         FileChannel ch = raf.getChannel();
         try {
            java.nio.channels.FileLock lock = ch.tryLock();
            if (lock == null) {
               return null; 
            }
            Lock l = new Lock(raf, lock, lockFile);
            LOCKS.put(absDir, l);
            return l;
         } catch (OverlappingFileLockException e) {
            return null;
         }
      } catch (FileNotFoundException e) {
         try {
            absDir.mkdirs();
            raf = new RandomAccessFile(lockFile, "rw");
            FileChannel ch = raf.getChannel();
            java.nio.channels.FileLock lock = ch.tryLock();
            if (lock == null) {
               return null;
            }
            Lock l = new Lock(raf, lock, lockFile);
            LOCKS.put(absDir, l);
            return l;
         } catch (Exception e2) {
            Log.warn("world", "could not create session.lock in " + absDir + ": " + e2);
            return null;
         }
      } catch (Exception e) {
         Log.warn("world", "could not lock world dir " + absDir + ": " + e);
         if (raf != null) {
            try { raf.close(); } catch (Exception ignored) { }
         }
         return null;
      }
   }

   public static class Lock {
      private final RandomAccessFile raf;
      private final java.nio.channels.FileLock lock;
      private final File lockFile;

      Lock(RandomAccessFile raf, java.nio.channels.FileLock lock, File lockFile) {
         this.raf = raf;
         this.lock = lock;
         this.lockFile = lockFile;
      }

      public void release() {
         try {
            lock.release();
         } catch (Exception ignored) { }
         try {
            raf.close();
         } catch (Exception ignored) { }
         LOCKS.remove(lockFile.getParentFile().getAbsoluteFile());
      }
   }
}

