package com.strata;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class DepTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static final HashMap<String, HashSet<String>> ALLOWED = new HashMap<>();

   static void allow(String from, String... tos) {
      HashSet<String> s = new HashSet<>();
      for (String t : tos) {
         s.add(t);
      }
      ALLOWED.put(from, s);
   }

   static {
      allow("", "client", "core");
      allow("blocks", "core");
      allow("core");
      allow("net", "core");
      allow("world", "blocks", "core", "world.gen", "world.light", "world.storage.nbt");
      allow("world.gen", "blocks", "core");
      allow("world.light");
      allow("world.mesh", "", "blocks", "core", "world");
      allow("world.storage.nbt");
      allow("server", "", "blocks", "core", "net", "world", "world.gen");
      allow("client", "", "blocks", "core", "net", "server", "world", "world.gen", "world.mesh");
   }

   public static void main(String[] args) throws Exception {
      ArrayList<File> roots = new ArrayList<>();
      for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
         File d = new File(entry, "com/strata");
         if (d.isDirectory()) {
            roots.add(d);
         }
      }
      check(!roots.isEmpty(), "found class roots on classpath");
      ArrayList<File> classes = new ArrayList<>();
      for (File r : roots) {
         collect(r, classes);
      }
      check(!classes.isEmpty(), "found classes to scan");
      int scanned = 0;
      for (File f : classes) {
         if (f.getName().endsWith("Test.class")) {
            continue;
         }
         String rel = relPath(f);
         String fromPkg = pkgOf(rel);
         for (String ref : referencedPackages(f)) {
            if (ref.equals(fromPkg)) {
               continue;
            }
            if (ref.equals("core")) {
               continue; 
            }
            HashSet<String> allowed = ALLOWED.get(fromPkg);
            check(allowed != null && allowed.contains(ref),
               rel + " (" + disp(fromPkg) + ") references " + disp(ref));
         }
         scanned++;
      }
      System.out.println("scanned " + scanned + " classes");
      if (failures > 0) { System.out.println(failures + " FAILURES"); System.exit(1); }
      System.out.println("DEP PASS");
   }

   static String disp(String pkg) {
      return pkg.isEmpty() ? "<root>" : pkg;
   }

   static void collect(File dir, ArrayList<File> out) {
      File[] files = dir.listFiles();
      if (files == null) {
         return;
      }
      for (File f : files) {
         if (f.isDirectory()) {
            collect(f, out);
         } else if (f.getName().endsWith(".class") && !f.getName().contains("$")) {
            out.add(f);
         }
      }
   }

   static String relPath(File f) {
      String p = f.getAbsolutePath().replace(File.separatorChar, '/');
      int i = p.indexOf("com/strata/");
      return p.substring(i + "com/strata/".length());
   }

   static String pkgOf(String rel) {
      int slash = rel.lastIndexOf('/');
      return slash < 0 ? "" : rel.substring(0, slash).replace('/', '.');
   }

   static HashSet<String> referencedPackages(File f) throws Exception {
      HashSet<String> refs = new HashSet<>();
      DataInputStream in = new DataInputStream(new FileInputStream(f));
      try {
         if (in.readInt() != (int)0xCAFEBABE) {
            throw new RuntimeException("not a class file: " + f);
         }
         in.readUnsignedShort();
         in.readUnsignedShort();
         int cpCount = in.readUnsignedShort();
         for (int i = 1; i < cpCount; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
               case 1: {
                  int len = in.readUnsignedShort();
                  byte[] bytes = new byte[len];
                  in.readFully(bytes);
                  String s = new String(bytes, "UTF-8");
                  String pkg = extractRubydungPackage(s);
                  if (pkg != null) {
                     refs.add(pkg);
                  }
                  break;
               }
               case 3: case 4: in.skipBytes(4); break;
               case 5: case 6: in.skipBytes(8); i++; break;
               case 7: case 8: case 16: case 19: case 20: in.skipBytes(2); break;
               case 9: case 10: case 11: case 12: case 17: case 18: in.skipBytes(4); break;
               case 15: in.skipBytes(3); break;
               default: throw new RuntimeException("unknown constant tag " + tag + " in " + f);
            }
         }
      } finally {
         in.close();
      }
      return refs;
   }

   static String extractRubydungPackage(String s) {
      String prefix = "com/strata/";
      int i = s.indexOf(prefix);
      if (i < 0) {
         return null;
      }
      String rest = s.substring(i + prefix.length());
      int end = rest.length();
      for (int j = 0; j < rest.length(); j++) {
         char c = rest.charAt(j);
         if (c != '/' && (c < 'A' || c > 'z') && c != '$') {
            end = j;
            break;
         }
      }
      rest = rest.substring(0, end);
      int slash = rest.lastIndexOf('/');
      if (slash < 0) {
         return ""; 
      }
      return rest.substring(0, slash).replace('/', '.');
   }
}

