package com.strata.world;

import com.strata.blocks.Blocks;
import com.strata.core.AABB;
import com.strata.world.storage.nbt.NBT;
import com.strata.world.storage.nbt.RegionFile;
import java.io.*;

public class LevelTest {
    static int failures = 0;

    static void check(boolean cond, String msg) {
        if (!cond) { failures++; System.out.println("FAIL: " + msg); }
    }

    static int surface(Level l, int x, int z) {
        for (int y = l.depth - 1; y >= 0; y--) {
            int t = l.getTile(x, y, z);
            if (t > 0 && t != 13 && t != 14 && t != 15 && t != 16 && t != 17 && t != 20 && t != 22
                && t != 11 && t != 26 && t != 27 && t != 28 && t != 29 && t != 30
                && t != 33 && t != 34 && t != 35 && t != 40 && t != 41) return y;
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        int[] xs = {-100000, -300, -17, -16, -1, 0, 1, 15, 16, 255, 99999};

        Level l = new Level(Level.WORLD_DEPTH);
        for (int x : xs) {
            int z = 99999 - x;
            check(l.getTile(x, 0, z) == 6, "bedrock floor @" + x);
            check(l.getTile(x, 127, z) == 0, "sky air @" + x);
            int top = surface(l, x, z);
            check(top >= 0 && top <= 127, "surface in range @" + x + " (got " + top + ")");
            int s = l.getTile(x, top, z);
            check(s == 1 || s == 2 || s == 3 || s == 5 || s == 25 || s == 31 || Blocks.isOre(s),
                "surface natural @" + x + " (got " + s + ")");
        }
        System.out.println("defaults ok");

        int ex = -5, ez = -7, eh = surface(l, ex, ez);
        int origSurface = l.getTile(ex, eh, ez);
        l.setTile(ex, eh, ez, 0);
        check(l.getTile(ex, eh, ez) == 0, "carved cell reads air");
        check(surface(l, ex + 1, ez) == surface(l, ex + 1, ez), "sanity");
        int nh = surface(l, ex + 1, ez);
        check(l.getTile(ex + 1, nh, ez) > 0, "neighbor surface intact");
        l.setTile(1000, 61, -2000, 0);
        int ay = -1;
        for (int y = 100; y < 126; y++) {
           if (l.getTile(1000, y, -2000) == 0 && l.getTile(1000, y + 1, -2000) == 0) { ay = y; break; }
        }
        check(ay > 0, "open sky found for edit");
        l.setTile(1000, ay, -2000, 3);
        check(l.getTile(1000, ay, -2000) == 3, "placed floating block");
        check(l.getTile(1000, ay + 1, -2000) == 0, "air above placed block");
        l.setTile(ex, eh, ez, origSurface);
        check(l.getTile(ex, eh, ez) == origSurface, "restored cell matches");
        System.out.println("edits ok");

        int bx = 10, bz = 10, bh = surface(l, bx, bz);
        for (int i = 1; i <= 8; i++) {
           l.setTile(bx, bh + i, bz, 0);
        }
        check(l.getBrightness(bx, bh + 1, bz) == 1.0f, "sky bright");
        check(l.getBrightness(bx, bh - 1, bz) == 0.0f, "buried dark");
        check(l.getSkyLevel(bx, bh, bz) == 0, "solid ground holds no sky");
        check(l.getSkyLevel(bx, bh + 1, bz) == 15, "shaft full above ground");
        l.setTile(bx, bh + 5, bz, 2);
        float shade = l.getBrightness(bx, bh + 1, bz);
        check(shade > 0.5f && shade < 1.0f, "pillar shades, not blacks (" + shade + ")");
        l.setTile(bx, bh + 5, bz, 0);
        check(l.getBrightness(bx, bh + 1, bz) == 1.0f, "bright again after pillar removed");
        l.setTile(bx, bh + 5, bz, Blocks.LEAF_ID);
        check(l.getBrightness(bx, bh + 1, bz) == 1.0f, "leaf pillar casts no shadow");
        check(l.getSkyLevel(bx, bh + 1, bz) == 15, "sky passes straight through leaves");
        l.setTile(bx, bh + 5, bz, 0);
        System.out.println("light ok");

        float noon = l.getBrightness(bx, bh + 1, bz);
        check(noon == 1.0f, "noon full bright");
        l.setSkylightSub(11);
        check(l.skylightSub() == 11, "sub sticks");
        float night = l.getBrightness(bx, bh + 1, bz);
        float moon = (float)Math.pow(4 / 15.0, 1.3);
        check(Math.abs(night - moon) < 1e-4, "midnight moonlight (" + night + ")");
        check(l.getSkyLevel(bx, bh + 1, bz) == 15, "raw store untouched by night");
        l.setTile(bx, bh + 1, bz, 12);
        check(l.getBrightness(bx, bh + 1, bz) > 0.9f, "torch pops at night");
        l.setTile(bx, bh + 1, bz, 0);
        l.setSkylightSub(0);
        check(l.getBrightness(bx, bh + 1, bz) == 1.0f, "dawn restores full bright");
        l.setSkylightSub(99);
        check(l.skylightSub() == 11, "sub clamps high");
        l.setSkylightSub(-5);
        check(l.skylightSub() == 0, "sub clamps low");

        int lipx = -1, liph = -1;
        for (int x = 10; x < 200 && lipx < 0; x++) {
           int h0 = surface(l, x, 0);
           boolean flat = h0 > 0;
           for (int dx = -4; dx <= 4 && flat; dx++) {
              int h1 = surface(l, x + dx, 0);
              if (h1 < 0 || Math.abs(h1 - h0) > 1) {
                 flat = false;
                 break;
              }
              for (int y = h0 + 1; y <= h0 + 6; y++) {
                 if (l.getTile(x + dx, y, 0) == Blocks.WOOD_ID) {
                    flat = false;
                    break;
                 }
              }
           }
           if (flat) {
              lipx = x;
              liph = h0;
           }
        }
        check(lipx >= 0, "flat lip site found");
        for (int dx = -2; dx <= 2; dx++) {
           for (int dy = 1; dy <= 6; dy++) {
              l.setTile(lipx + dx, liph + dy, 0, 0);
           }
        }
        for (int dx = -2; dx <= 2; dx++) {
           l.setTile(lipx + dx, liph + 3, 0, Blocks.STONE_ID);
        }
        int center = l.getSkyLevel(lipx, liph + 1, 0);
        int lipEdge = l.getSkyLevel(lipx - 2, liph + 1, 0);
        check(center > 0 && center < 15, "lip shades, not blacks (" + center + ")");
        check(lipEdge > 0 && lipEdge <= 15, "lip edge lit (" + lipEdge + ")");
        check(center <= lipEdge, "center dimmer than edge (" + center + " vs " + lipEdge + ")");
        for (int dx = -2; dx <= 2; dx++) {
           l.setTile(lipx + dx, liph + 3, 0, 0);
        }
        check(l.getSkyLevel(lipx, liph + 1, 0) == 15, "full bright after lip removed");
        System.out.println("lip ok");

        int tx = 20, tz = 20, th = -1;
        for (int r = 0; r < 60 && th < 0; r++) {
            for (int dx = -r; dx <= r && th < 0; dx++) {
                for (int dz = -r; dz <= r && th < 0; dz++) {
                    int cx = 20 + dx, cz = 20 + dz;
                    int h = surface(l, cx, cz);
                    if (h > 0 && l.getTile(cx, h + 1, cz) == 0
                        && l.getTile(cx, h + 2, cz) == 0 && l.getTile(cx, h + 3, cz) == 0) {
                        tx = cx;
                        tz = cz;
                        th = h;
                    }
                }
            }
        }
        check(th > 0, "dry torch rig site found");
        l.setTile(tx, th + 2, tz, 12);
        check(l.getBlockLevel(tx, th + 2, tz) == 14, "torch cell lit");
        check(l.getBlockLevel(tx, th + 1, tz) == 13, "glow falls off");
        check(l.getBrightness(tx, th + 1, tz) > 0.5f, "glow visible");
        check(l.getBrightness(tx + 3, th + 2, tz) > 0.3f, "glow reaches sideways");
        l.setTile(tx, th + 2, tz, 0);
        check(l.getBlockLevel(tx, th + 2, tz) == 0, "torch light removed");
        check(l.getBrightness(tx, th + 1, tz) == 1.0f, "daylight back after removal");
        System.out.println("torch ok");

        int lavaFound = 0;
        for (int x = -40; x <= 40 && lavaFound == 0; x += 2)
            for (int z = -40; z <= 40 && lavaFound == 0; z += 2)
                for (int y = 1; y < 10; y++)
                    if (l.getTile(x, y, z) == 11) { lavaFound++; break; }
        System.out.println("natural lava columns (sampled)=" + lavaFound);
        int lx = 40, lz = 40, origLava = l.getTile(lx, 5, lz);
        l.setTile(lx, 5, lz, 11);
        check(l.getBlockLevel(lx, 5, lz) == 15, "lava cell max lit");
        check(l.getBrightness(lx, 5, lz) == 1.0f, "lava full bright");
        l.setTile(lx, 5, lz, origLava);
        check(l.getBlockLevel(lx, 5, lz) == 0, "lava light removed");
        System.out.println("lava ok");

        int cx = 20, cz = 20, ch = surface(l, cx, cz);
        check(l.getCubes(new AABB(cx + 0.4f, ch + 2, cz + 0.4f, cx + 0.6f, ch + 3, cz + 0.6f)).isEmpty(), "sky box empty");
        check(!l.getCubes(new AABB(cx + 0.4f, ch - 1, cz + 0.4f, cx + 0.6f, ch + 1, cz + 0.6f)).isEmpty(), "ground box hits");
        System.out.println("cubes ok");

        l.save();
        File rf = new File("region/r.1.-4.mca"); 
        check(rf.exists() && rf.length() < 60000, "region file small (" + (rf.exists() ? rf.length() : -1) + " bytes)");
        RegionFile region = new RegionFile(rf);
        byte[] payload = region.readChunk(30, 3); 
        region.close();
        check(payload != null, "column chunk stored");
        NBT.CompoundTag root = NBT.readRoot(new DataInputStream(new ByteArrayInputStream(payload)));
        check(root.compound("Level").integer("DataVersion") == Level.SAVE_VERSION, "save stamped current version");
        Level l2 = new Level(Level.WORLD_DEPTH);
        check(l2.getTile(1000, ay, -2000) == 0, "far edit not loaded before region demand");
        l2.ensureRegions(62, -125, 62, -125);
        l2.ensureRegions(-1, -1, -1, -1);
        check(l2.getTile(1000, ay, -2000) == 3, "edit survives reload");
        int eh2 = surface(l2, ex, ez);
        check(eh2 == eh, "restored surface matches (" + eh2 + " vs " + eh + ")");
        int sh = surface(l2, 77, 77);
        int ss = l2.getTile(77, sh, 77);
        check(ss == 1 || ss == 2 || ss == 3 || ss == 5, "default intact after reload");
        int bh2 = surface(l2, bx, bz);
        check(l2.getBrightness(bx, bh2 + 1, bz) == 1.0f, "light intact after reload");
        l2.setTile(-300, 50, -300, 2);
        l2.save();
        Level l3 = new Level(Level.WORLD_DEPTH);
        l3.ensureRegions(-19, -19, -19, -19);
        check(l3.getTile(-300, 50, -300) == 2, "negative-region edit survives");
        System.out.println("saveload ok");

        if (failures == 0) System.out.println("ALL PASS");
        else { System.out.println(failures + " FAILURES"); System.exit(1); }
    }
}

