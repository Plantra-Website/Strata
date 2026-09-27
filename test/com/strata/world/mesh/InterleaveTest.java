package com.strata.world.mesh;

public class InterleaveTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   public static void main(String[] args) {
      float[] v = {1, 2, 3, 4, 5, 6};
      float[] t = {7, 8, 9, 10};
      float[] c = {11, 12, 13, 14, 15, 16};
      float[] out = Chunk.interleave(v, t, c, 2);
      float[] want = {1, 2, 3, 7, 8, 11, 12, 13, 4, 5, 6, 9, 10, 14, 15, 16};
      check(out.length == want.length, "length " + out.length);
      for (int i = 0; i < want.length; i++) {
         if (out[i] != want[i]) {
            check(false, "idx " + i + " got=" + out[i] + " want=" + want[i]);
            break;
         }
      }
      check(Chunk.STRIDE_BYTES == 32, "stride 32");
      if (failures == 0) System.out.println("INTERLEAVE PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

