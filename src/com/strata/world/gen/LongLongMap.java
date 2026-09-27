package com.strata.world.gen;

final class LongLongMap {
   private long[] keys;
   private long[] vals;
   private boolean[] used;
   private int mask;
   private int size;

   LongLongMap(int expected) {
      int cap = 16;
      while (cap < expected * 2) {
         cap <<= 1;
      }
      this.keys = new long[cap];
      this.vals = new long[cap];
      this.used = new boolean[cap];
      this.mask = cap - 1;
   }

   long get(long key) {
      int i = mix(key) & this.mask;
      while (this.used[i]) {
         if (this.keys[i] == key) {
            return this.vals[i];
         }
         i = (i + 1) & this.mask;
      }
      return 0L;
   }

   void orBits(long key, long bits) {
      int i = mix(key) & this.mask;
      while (this.used[i]) {
         if (this.keys[i] == key) {
            this.vals[i] |= bits;
            return;
         }
         i = (i + 1) & this.mask;
      }
      this.used[i] = true;
      this.keys[i] = key;
      this.vals[i] = bits;
      if (++this.size * 4 > this.keys.length * 3) {
         this.resize();
      }
   }

   private void resize() {
      long[] oldKeys = this.keys;
      long[] oldVals = this.vals;
      boolean[] oldUsed = this.used;
      int cap = oldKeys.length * 2;
      this.keys = new long[cap];
      this.vals = new long[cap];
      this.used = new boolean[cap];
      this.mask = cap - 1;
      this.size = 0;
      for (int i = 0; i < oldKeys.length; i++) {
         if (oldUsed[i]) {
            this.putFresh(oldKeys[i], oldVals[i]);
         }
      }
   }

   private void putFresh(long key, long val) {
      int i = mix(key) & this.mask;
      while (this.used[i]) {
         i = (i + 1) & this.mask;
      }
      this.used[i] = true;
      this.keys[i] = key;
      this.vals[i] = val;
      this.size++;
   }

   private static int mix(long x) {
      x ^= x >>> 33;
      x *= 0xFF51AFD7ED558CCDL;
      x ^= x >>> 33;
      return (int)x;
   }
}

