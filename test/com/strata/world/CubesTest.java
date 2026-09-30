package com.strata.world;

import com.strata.blocks.Blocks;
import com.strata.core.AABB;
import java.util.List;

public class CubesTest {
    static int failures = 0;

    static void check(boolean cond, String msg) {
        if (!cond) { failures++; System.out.println("FAIL: " + msg); }
    }

    static int surface(Level l, int x, int z) {
        for (int y = 63; y >= 0; y--) {
            int t = l.getTile(x, y, z);
            if (t > 0 && Blocks.isSolid(t)) return y;
        }
        return -1;
    }

    public static void main(String[] args) {
        Level l = new Level(64);
        int nx = 0, nz = 0, h = 0;
        boolean found = false;
        for (int x = -30; x <= -2 && !found; x += 3)
            for (int z = -30; z <= 30 && !found; z += 7) {
                int sh = surface(l, x, z);
                if (sh >= 10 && l.getTile(x - 1, sh - 1, z) > 0 && l.getTile(x, sh - 1, z) > 0) {
                    nx = x;
                    nz = z;
                    h = sh;
                    found = true;
                }
            }
        check(found, "probe ground found");
        if (!found) { System.out.println("CUBES ABORT"); System.exit(1); }
        List<AABB> neg = l.getCubes(new AABB(nx - 0.3f, h - 0.5f, nz + 0.4f, nx + 0.3f, h + 0.5f, nz + 0.6f));
        boolean hasBoundary = false;
        for (AABB b : neg) if (b.x0 == nx - 1.0f) hasBoundary = true;
        check(!neg.isEmpty(), "negative box finds ground");
        check(hasBoundary, "negative box includes overlapped cell x=" + (nx - 1));
        int px = 0, ph = 0;
        boolean pfound = false;
        for (int x = 2; x <= 30 && !pfound; x += 3) {
            int sh = surface(l, x, nz);
            if (sh >= 10 && l.getTile(x, sh - 1, nz) > 0) {
                px = x;
                ph = sh;
                pfound = true;
            }
        }
        check(pfound, "positive probe ground found");
        List<AABB> pos = l.getCubes(new AABB(px - 0.3f, ph - 0.5f, nz + 0.4f, px + 0.3f, ph + 0.5f, nz + 0.6f));
        check(!pos.isEmpty(), "positive box finds ground");
        check(l.getCubes(new AABB(nx + 0.4f, h + 2, nz + 0.4f, nx + 0.6f, h + 3, nz + 0.6f)).isEmpty(), "sky empty");
        if (failures == 0) System.out.println("CUBES PASS");
        else { System.out.println(failures + " FAILURES"); System.exit(1); }
    }
}

