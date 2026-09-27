package com.strata.world.gen;

import com.strata.blocks.Blocks;

public class GenTest {
    static int failures = 0;

    static void check(boolean cond, String msg) {
        if (!cond) { failures++; System.out.println("FAIL: " + msg); }
    }

    public static void main(String[] args) {
        TerrainGenerator g = new TerrainGenerator(TerrainGenerator.DEFAULT_SEED, 64);
        TerrainGenerator g2 = new TerrainGenerator(TerrainGenerator.DEFAULT_SEED, 64);

        for (int i = 0; i < 500; i++) {
            int x = (i * 7919) % 2000 - 1000, z = (i * 4799) % 2000 - 1000;
            check(g.heightAt(x, z) == g2.heightAt(x, z), "height deterministic @" + x + "," + z);
            int h = g.heightAt(x, z);
            for (int y = 0; y < 64; y += 7)
                check(g.blockAt(x, y, z, h) == g2.blockAt(x, y, z, h), "block deterministic");
        }
        System.out.println("determinism ok");

        int lo = 99, hi = -99, beaches = 0, hillsCols = 0, plainsCols = 0, breaks = 0;
        for (int x = -64; x < 64; x++)
            for (int z = -64; z < 64; z++) {
                int h = g.heightAt(x, z);
                if (h < lo) lo = h;
                if (h > hi) hi = h;
                check(h >= 4 && h <= 56, "height in range @" + x + "," + z);
                int s = g.blockAt(x, h, z, h);
                if (s == 0) {
                    breaks++;
                } else {
                    check(s == Blocks.GRASS_ID || s == Blocks.SAND_ID, "surface is grass/sand, got " + s);
                    if (s == Blocks.SAND_ID) beaches++;
                }
                String b = g.biomeAt(x, z).id();
                if (b.equals("hills")) hillsCols++;
                else if (b.equals("plains")) plainsCols++;
                else check(false, "unknown biome " + b);
            }
        System.out.println("height range [" + lo + "," + hi + "] beaches=" + beaches + " hills=" + hillsCols + " plains=" + plainsCols + " surfaceBreaks=" + breaks);
        check(hi - lo >= 6, "terrain has relief");
        check(breaks > 0 && breaks < 500, "entrances exist but not pockmarked, got " + breaks);
        int glo = 99, ghi = -99;
        for (int x = -2000; x <= 2000; x += 8)
            for (int z = -2000; z <= 2000; z += 8) {
                int h = g.heightAt(x, z);
                if (h < glo) glo = h;
                if (h > ghi) ghi = h;
            }
        System.out.println("wide height range [" + glo + "," + ghi + "]");
        check(ghi >= 50, "mountains exist somewhere");
        check(beaches > 0, "beaches exist");
        check(hillsCols > 0 && plainsCols > 0, "both biomes present");
        check(Biomes.get("plains") != null && Biomes.get("hills") != null, "registry");

        for (int x = -64; x < 64; x += 3)
            for (int z = -64; z < 64; z += 3)
                check(g.blockAt(x, 0, z, g.heightAt(x, z)) == Blocks.BEDROCK_ID, "bedrock @ " + x + "," + z);
        System.out.println("bedrock ok");

        int X0 = -32, X1 = 32, Z0 = -32, Z1 = 64 - 32, Y0 = 2, Y1 = 48;
        int nx = X1 - X0, ny = Y1 - Y0, nz = Z1 - Z0;
        boolean[] cave = new boolean[nx * ny * nz];
        long stoneZone = 0, caveCells = 0;
        for (int x = X0; x < X1; x++)
            for (int z = Z0; z < Z1; z++) {
                int h = g.heightAt(x, z);
                for (int y = Y0; y < Y1 && y < h - 2; y++) {
                    stoneZone++;
                    if (g.blockAt(x, y, z, h) == 0) {
                        cave[(x - X0) * ny * nz + (y - Y0) * nz + (z - Z0)] = true;
                        caveCells++;
                    }
                }
            }
        boolean[] seen = new boolean[cave.length];
        int[] stack = new int[cave.length];
        int components = 0, largest = 0;
        int[][] dirs = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        for (int i = 0; i < cave.length; i++) {
            if (!cave[i] || seen[i]) continue;
            components++;
            int size = 0, sp = 0;
            stack[sp++] = i;
            seen[i] = true;
            while (sp > 0) {
                int c = stack[--sp];
                size++;
                int x = c / (ny * nz), rem = c % (ny * nz), y = rem / nz, z = rem % nz;
                for (int[] d : dirs) {
                    int xx = x + d[0], yy = y + d[1], zz = z + d[2];
                    if (xx < 0 || yy < 0 || zz < 0 || xx >= nx || yy >= ny || zz >= nz) continue;
                    int j = (xx * ny + yy) * nz + zz;
                    if (cave[j] && !seen[j]) { seen[j] = true; stack[sp++] = j; }
                }
            }
            if (size > largest) largest = size;
        }
        double frac = (double)caveCells / stoneZone;
        System.out.println("caves: fraction=" + String.format("%.3f", frac) + " components=" + components + " largest=" + largest);
        check(caveCells > 50, "caves exist");
        check(frac < 0.08, "not swiss-cheesed");
        check(largest >= 300, "tunnel systems connected");

        int coal = 0, iron = 0, gold = 0, diamond = 0, lapis = 0, gravel = 0;
        for (int x = -48; x < 48; x++)
            for (int z = -48; z < 48; z += 2) {
                int h = g.heightAt(x, z);
                for (int y = 1; y < h - 2 && y <= 40; y++) {
                    int b = g.blockAt(x, y, z, h);
                    if (b == Blocks.COAL_ID) coal++;
                    else if (b == Blocks.IRON_ID) iron++;
                    else if (b == Blocks.GOLD_ID) gold++;
                    else if (b == Blocks.DIAMOND_ID) diamond++;
                    else if (b == Blocks.LAPIS_ID) lapis++;
                    else if (b == Blocks.GRAVEL_ID) gravel++;
                }
            }
        System.out.println("ores: coal=" + coal + " iron=" + iron + " gold=" + gold + " diamond=" + diamond + " lapis=" + lapis + " gravel=" + gravel);
        check(coal > 20, "coal exists");
        check(iron > 10, "iron exists");
        check(gold > 0, "gold exists");
        check(diamond > 0, "diamond exists");
        check(lapis > 0, "lapis exists");
        check(gravel > 20, "gravel pockets exist");

        int lava = 0;
        for (int x = -48; x < 48; x += 2)
            for (int z = -48; z < 48; z += 2) {
                int h = g.heightAt(x, z);
                for (int y = 1; y < 10 && y < h; y++)
                    if (g.blockAt(x, y, z, h) == Blocks.LAVA_ID) lava++;
            }
        System.out.println("lava cells (sampled)=" + lava);
        check(lava > 0, "lava lakes exist");

        if (failures == 0) System.out.println("GEN PASS");
        else { System.out.println(failures + " FAILURES"); System.exit(1); }
    }
}

