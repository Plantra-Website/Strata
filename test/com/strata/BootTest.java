package com.strata;

public class BootTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) {
      check(Boot.nativeLibName("Mac OS X", "aarch64").equals("liblwjgl.dylib"), "mac arm64 dylib");
      check(Boot.nativeLibName("Mac OS X", "x86_64").equals("liblwjgl.dylib"), "mac intel dylib");
      check(Boot.nativeLibName("Darwin", "arm64").equals("liblwjgl.dylib"), "darwin arm64 dylib");
      check(Boot.nativeLibName("Windows 10", "amd64").equals("lwjgl64.dll"), "windows dll");
      check(Boot.nativeLibName("Windows 11", "aarch64").equals("lwjgl64.dll"), "windows arm dll");
      check(Boot.nativeLibName("Linux", "amd64").equals("liblwjgl64.so"), "linux so");
      check(Boot.nativeLibName("Linux", "aarch64").equals("liblwjgl64.so"), "linux arm so");
      check(Boot.nativeLibName("", "").equals("liblwjgl64.so"), "unknown os falls back to linux so");

      check(Boot.platformDir("Mac OS X", "aarch64").equals("mac-arm64"), "mac arm64 dir");
      check(Boot.platformDir("Mac OS X", "x86_64").equals("mac-x64"), "mac intel dir");
      check(Boot.platformDir("Darwin", "arm64").equals("mac-arm64"), "darwin arm dir");
      check(Boot.platformDir("Windows 10", "amd64").equals("windows-x64"), "windows dir");
      check(Boot.platformDir("Windows 11", "aarch64").equals("windows-x64"), "windows arm dir");
      check(Boot.platformDir("Linux", "amd64").equals("linux-x64"), "linux dir");
      check(Boot.platformDir("", "").equals("linux-x64"), "unknown os dir falls back to linux");
      check(java.util.Arrays.equals(Boot.nativeLibs("mac-arm64"),
         new String[]{"liblwjgl.dylib"}), "mac libs");
      check(java.util.Arrays.equals(Boot.nativeLibs("windows-x64"),
         new String[]{"lwjgl64.dll"}), "windows libs");
      check(java.util.Arrays.equals(Boot.nativeLibs("linux-x64"),
         new String[]{"liblwjgl64.so"}), "linux libs");

      if (failures == 0) System.out.println("BOOT PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

