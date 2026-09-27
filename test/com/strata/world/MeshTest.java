package com.strata.world;

import com.strata.blocks.AtlasStitcher;
import com.strata.blocks.Blocks;
import com.strata.blocks.MeshBuilder;

public class MeshTest {
    static int failures = 0;

    static void check(boolean cond, String msg) {
        if (!cond) { failures++; System.out.println("FAIL: " + msg); }
    }

    static void checkF(float got, float want, String msg) {
        if (Math.abs(got - want) > 1e-6) { failures++; System.out.println("FAIL: " + msg + " got=" + got + " want=" + want); }
    }

    static int surface(Level l, int x, int z) {
        for (int y = 63; y >= 0; y--)
            if (l.getTile(x, y, z) > 0) return y;
        return -1;
    }

    static int topFace(com.strata.blocks.MeshBuilder b, float topY) {
       float[] v = b.vertices();
       for (int i = 0; i + 3 < b.count(); i += 4) {
          boolean top = true;
          for (int k = 0; k < 4; k++) {
             if (v[(i + k) * 3 + 1] != topY) {
                top = false;
                break;
             }
          }
          if (top) {
             return i;
          }
       }
       return -1;
    }

    public static void main(String[] args) {
        Level level = new Level(64);
        int x = 10, z = 20;
        int h = surface(level, x, z);
        level.setTile(x, h, z, Blocks.GRASS_ID);
        level.setTile(x + 1, h, z, 0);
        level.setTile(x - 1, h, z, 0);
        level.setTile(x, h, z + 1, 0);
        level.setTile(x, h, z - 1, 0);
        level.setTile(x, h + 1, z, 0);
        int[][] cells = {{x, z}, {x + 1, z}, {x - 1, z}, {x, z + 1}, {x, z - 1}};
        for (int[] c : cells) {
           for (int y = h + 1; y < 64; y++) {
              level.setTile(c[0], y, c[1], 0);
           }
        }
        level.getSkyLevel(x, h + 1, z);
        MeshBuilder b = new MeshBuilder();
        b.init();
        Blocks.byId(Blocks.GRASS_ID).render(b, level, 0, x, h, z);
        check(b.count() == 36, "mesa emits 5 faces + 4 overlays (" + b.count() + " verts)");
        float[] v = b.vertices();
        float[] t = b.texCoords();
        float[] c = b.colors();
        int first = 0;
        for (int i = 0; i < 4; i++)
            checkF(v[i * 3 + 1], h + 1.0f, "top face level");
        float[] guv = AtlasStitcher.uv(AtlasStitcher.slot("blocks/grass_top.png"));
        java.util.ArrayList<java.util.HashSet<String>> wantSets = new java.util.ArrayList<>();
        int grassBase = AtlasStitcher.slot("blocks/grass_top.png");
        int[] grassTiles = new int[AtlasStitcher.altsFor(grassBase).length + 1];
        grassTiles[0] = grassBase;
        int[] alts = AtlasStitcher.altsFor(grassBase);
        for (int i = 0; i < alts.length; i++) {
           grassTiles[i + 1] = alts[i];
        }
        for (int gt : grassTiles) {
           float[] gr = AtlasStitcher.uv(gt);
           java.util.HashSet<String> set = new java.util.HashSet<>();
           set.add(gr[0] + "," + gr[1]);
           set.add(gr[0] + "," + gr[3]);
           set.add(gr[2] + "," + gr[1]);
           set.add(gr[2] + "," + gr[3]);
           wantSets.add(set);
        }
        for (int k = 0; k < 4; k++) {
            checkF(v[(first + k) * 3], x + new float[]{1, 1, 0, 0}[k], "top vert x");
            checkF(v[(first + k) * 3 + 2], z + new float[]{1, 0, 0, 1}[k], "top vert z");
            java.util.HashSet<String> gotUV = new java.util.HashSet<>();
            for (int q = 0; q < 4; q++) {
               gotUV.add(t[(first + q) * 2] + "," + t[(first + q) * 2 + 1]);
            }
            check(wantSets.contains(gotUV), "top uv in a candidate tile set");
            checkF(c[(first + k) * 3], 1.0f, "top full bright r");
            checkF(c[(first + k) * 3 + 1], 1.0f, "top full bright g");
            checkF(c[(first + k) * 3 + 2], 1.0f, "top full bright b");
        }
        MeshBuilder b2 = new MeshBuilder();
        b2.init();
        int vx = 30, vy = 20, vz = 30;
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++)
                    level.setTile(vx + dx, vy + dy, vz + dz, Blocks.STONE_ID);
        Blocks.byId(Blocks.STONE_ID).render(b2, level, 0, vx, vy, vz);
        check(b2.count() == 0, "buried block emits nothing (got " + b2.count() + ")");
        MeshBuilder b3 = new MeshBuilder();
        b3.init();
        Blocks.byId(Blocks.STONE_ID).render(b3, level, 1, vx, vy, vz);
        check(b3.count() == 0, "buried block emits nothing on layer 1 (got " + b3.count() + ")");
        MeshBuilder big = new MeshBuilder();
        big.init();
        big.color(1, 1, 1);
        for (int i = 0; i < 5000; i++) { big.tex(0, 0); big.vertex(i, i, i); }
        check(big.count() == 5000, "grow capacity");
        checkF(big.vertices()[4999 * 3], 4999f, "grown data intact");
        System.out.println("mesa verts=" + b.count());
        float[] ov = com.strata.blocks.AtlasStitcher.uv(
           com.strata.blocks.AtlasStitcher.slot("blocks/grass_side_overlay.png"));
        int overlayVerts = 0;
        for (int i = 0; i < b.count(); i++) {
           if (t[i * 2] >= ov[0] && t[i * 2] <= ov[2] && t[i * 2 + 1] >= ov[1] && t[i * 2 + 1] <= ov[3]) {
              overlayVerts++;
              float ox = v[i * 3], oz = v[i * 3 + 2];
              boolean onFace = (ox == x || ox == x + 1) && (oz == z || oz == z + 1);
              check(!onFace, "overlay vert offset off face");
           }
        }
         check(overlayVerts == 16, "4 overlay quads (got " + overlayVerts + " verts)");
         for (int i = 0; i < b.count(); i++) {
            if (t[i * 2] >= ov[0] && t[i * 2] <= ov[2] && t[i * 2 + 1] >= ov[1] && t[i * 2 + 1] <= ov[3]) {
               boolean top = v[i * 3 + 1] > h + 0.5f;
               checkF(t[i * 2 + 1], top ? ov[1] : ov[3], "overlay v tracks height");
            }
         }
         int dirtBase = AtlasStitcher.slot("blocks/dirt.png");
         int[] dirtAlts = AtlasStitcher.altsFor(dirtBase);
         int[] dirtTiles = new int[dirtAlts.length + 1];
         dirtTiles[0] = dirtBase;
         for (int i = 0; i < dirtAlts.length; i++) {
            dirtTiles[i + 1] = dirtAlts[i];
         }
         java.util.HashSet<String> orders = new java.util.HashSet<>();
         java.util.HashSet<Integer> seenTiles = new java.util.HashSet<>();
         String firstSig = null;
         for (int i = 0; i < 64; i++) {
            int cx = 200 + i, cy = 60, cz = 200;
            level.setTile(cx, cy, cz, Blocks.DIRT_ID);
            MeshBuilder rb = new MeshBuilder();
            rb.init();
            Blocks.byId(Blocks.DIRT_ID).render(rb, level, 0, cx, cy, cz);
            int top = topFace(rb, cy + 1.0F);
            check(top >= 0, "platform top emits");
            if (top < 0) {
               continue;
            }
            float[] rt = rb.texCoords();
            StringBuilder sig = new StringBuilder();
            for (int k = 0; k < 4; k++) {
               sig.append(rt[(top + k) * 2]).append(',').append(rt[(top + k) * 2 + 1]).append(';');
            }
            if (i == 0) {
               firstSig = sig.toString();
            }
            orders.add(sig.toString());
            for (int dt : dirtTiles) {
               float[] r = AtlasStitcher.uv(dt);
               if (rt[top * 2] >= r[0] && rt[top * 2] <= r[2] && rt[top * 2 + 1] >= r[1] && rt[top * 2 + 1] <= r[3]) {
                  seenTiles.add(dt);
               }
            }
         }
         check(orders.size() > 1, "top rotations vary (" + orders.size() + " orders)");
         check(seenTiles.size() == dirtTiles.length, "every variant appears (" + seenTiles.size() + "/" + dirtTiles.length + ")");
         MeshBuilder rb2 = new MeshBuilder();
         rb2.init();
         Blocks.byId(Blocks.DIRT_ID).render(rb2, level, 0, 200, 60, 200);
         int top2 = topFace(rb2, 61.0F);
         check(top2 >= 0, "re-render top found");
         float[] rt2 = rb2.texCoords();
         StringBuilder sig2 = new StringBuilder();
         for (int k = 0; k < 4; k++) {
            sig2.append(rt2[(top2 + k) * 2]).append(',').append(rt2[(top2 + k) * 2 + 1]).append(';');
         }
         check(firstSig.equals(sig2.toString()), "re-render identical (no flicker)");
         com.strata.core.Config.BLOCK_ROTATION = false;
         for (int i = 0; i < 16; i++) {
            int cx = 300 + i, cy = 60, cz = 300;
            level.setTile(cx, cy, cz, Blocks.DIRT_ID);
            MeshBuilder rb = new MeshBuilder();
            rb.init();
            Blocks.byId(Blocks.DIRT_ID).render(rb, level, 0, cx, cy, cz);
            int top = topFace(rb, cy + 1.0F);
            check(top >= 0, "flag-off top found");
            if (top < 0) {
               continue;
            }
            float[] rt = rb.texCoords();
            boolean pinned = false;
            for (int dt : dirtTiles) {
               float[] e = AtlasStitcher.uv(dt);
               if (rt[top * 2] == e[2] && rt[top * 2 + 1] == e[3]) {
                  pinned = true;
               }
            }
            check(pinned, "corner matched a dirt tile");
         }
         com.strata.core.Config.BLOCK_ROTATION = true;
        if (failures == 0) System.out.println("MESH PASS");
        else { System.out.println(failures + " FAILURES"); System.exit(1); }
    }
}

