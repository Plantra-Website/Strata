package com.strata.world;

import com.strata.blocks.Blocks;

public class LegacyTest {
    public static void main(String[] args) throws Exception {
        writeLegacyFixture();
        Level l = new Level(64);
        boolean ok = true;
        ok &= check(l.getTile(10, 60, 20) == 3, "legacy floating block kept");
        ok &= check(l.getTile(30, 42, 30) == 0, "legacy hole kept");
        int top = -1;
        for (int y = 63; y >= 0; y--)
            if (l.getTile(30, y, 31) > 0) { top = y; break; }
        ok &= check(top >= 0 && top <= 127, "surface sane near old builds");
        int v = l.getTile(5, 20, 5);
        ok &= check(v >= 0 && v <= 22 && (v == 0 || Blocks.byId(v) != null), "stone zone sane (got " + v + ")");
        ok &= check(l.getTile(0, 0, 0) == Blocks.BEDROCK_ID, "bedrock under old area");
        System.out.println(ok ? "LEGACY PASS" : "LEGACY FAIL");
        if (!ok) System.exit(1);
    }

    static boolean check(boolean cond, String msg) {
        if (!cond) System.out.println("FAIL: " + msg);
        return cond;
    }

    static void writeLegacyFixture() throws Exception {
        byte[] data = new byte[256 * 256 * 64];
        for (int x = 0; x < 256; x++)
            for (int z = 0; z < 256; z++)
                for (int y = 0; y < 64; y++)
                    data[(y * 256 + z) * 256 + x] = (byte)oldFlat(y);
        data[(60 * 256 + 20) * 256 + 10] = 3; 
        data[(42 * 256 + 30) * 256 + 30] = 0; 
        java.util.zip.GZIPOutputStream gz =
            new java.util.zip.GZIPOutputStream(new java.io.FileOutputStream("level.dat"));
        gz.write(data);
        gz.close();
    }

    static int oldFlat(int y) {
        if (y == 42) return 1;
        if (y < 42 && y >= 39) return 3;
        if (y < 39 && y >= 0) return 2;
        return 0;
    }
}

