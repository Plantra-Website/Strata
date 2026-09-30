package com.strata.world;

import com.strata.blocks.AtlasStitcher;
import com.strata.blocks.Blocks;
import com.strata.blocks.MeshBuilder;

public class TorchFaceTest {
    static int failures = 0;

    static void check(boolean cond, String msg) {
        if (!cond) { failures++; System.out.println("FAIL: " + msg); }
    }

    static void checkF(float got, float want, String msg) {
        if (Math.abs(got - want) > 1e-4) { failures++; System.out.println("FAIL: " + msg + " got=" + got + " want=" + want); }
    }

    public static void main(String[] args) {
        Level l = new Level(64);
        int bx = -1, by = 19, bz = -1;
        outer:
        for (int x = 40; x < 80; x++)
            for (int z = 40; z < 80; z++)
                if (l.getTile(x, by, z) == Blocks.STONE_ID && l.getTile(x, by + 1, z) == Blocks.STONE_ID) {
                    bx = x;
                    bz = z;
                    break outer;
                }
        check(bx >= 0, "probe site found");
        l.setTile(bx, by + 1, bz, 0); 
        MeshBuilder dark = new MeshBuilder();
        dark.init();
        Blocks.byId(Blocks.STONE_ID).render(dark, l, 1, bx, by, bz); 
        check(dark.count() == 4, "only top face (" + dark.count() + " verts)");
        float[] dc = dark.colors();
        for (int i = 0; i < dark.count(); i++) {
           checkF(dc[i * 3], 1.0f, "unlit top shade full");
           check(dc[i * 3 + 1] == 0.0f, "unlit top no sky");
           check(dc[i * 3 + 2] == 0.0f, "unlit top no block light");
        }
        l.setTile(bx, by + 1, bz, Blocks.TORCH_ID);
        MeshBuilder lit = new MeshBuilder();
        lit.init();
        Blocks.byId(Blocks.STONE_ID).render(lit, l, 1, bx, by, bz);
        check(lit.count() == 4, "torch doesn't cull floor face");
        boolean bright = true;
        float[] lc = lit.colors();
        for (int i = 0; i < lit.count(); i++) if (lc[i * 3 + 2] < 0.5f) bright = false;
        check(bright, "torch-lit top face block channel bright");

        l.setTile(bx, by + 1, bz, Blocks.TORCH_ID);
        MeshBuilder torch = new MeshBuilder();
        torch.init();
        Blocks.byId(Blocks.TORCH_ID).render(torch, l, 0, bx, by + 1, bz);
        check(torch.count() == 48, "4 sides + 2 caps x both windings (" + torch.count() + " verts)");
        float[] v = torch.vertices();
        float[] t = torch.texCoords();
        for (int i = 0; i < torch.count(); i++) {
           float px = v[i * 3] - bx;
           float pz = v[i * 3 + 2] - bz;
           float py = v[i * 3 + 1] - (by + 1);
           check(px >= 0.4375F && px <= 0.5625F, "post x in 2px column");
           check(pz >= 0.4375F && pz <= 0.5625F, "post z in 2px column");
           check(py >= 0.0F && py <= 0.625F, "post 10px tall");
        }
        float[] rect = AtlasStitcher.tileRect(AtlasStitcher.slot("blocks/torch_on.png"));
        float sideV0 = rect[1] + rect[3] * 6.05F / 16.0F;
        float sideV1 = rect[1] + rect[3] * 15.95F / 16.0F;
        for (int i = 0; i < 32; i++) {
           check(t[i * 2 + 1] >= sideV0 && t[i * 2 + 1] <= sideV1, "side v in art rows 6-15");
        }
        for (int i = 0; i < 32; i++) {
           boolean bottom = Math.abs(v[i * 3 + 1] - (by + 1)) < 1e-6F;
           float wantV = bottom ? sideV1 : sideV0;
           checkF(t[i * 2 + 1], wantV, "side v tracks height (no rotation)");
        }
        for (int i = 0; i < 32; i++) {
           float texU = (t[i * 2] - rect[0]) / rect[2] * 16.0F;
           float texV = (t[i * 2 + 1] - rect[1]) / rect[3] * 16.0F;
           check(texU >= 7.0F && texU < 9.0F, "side u inside stick columns 7-8 (got " + texU + ")");
           check(texV >= 6.0F && texV < 16.0F, "side v inside art rows 6-15 (got " + texV + ")");
        }
        float tu0 = rect[0] + rect[2] * 7.05F / 16.0F;
        float tu1 = rect[0] + rect[2] * 8.95F / 16.0F;
        float capTop1 = rect[1] + rect[3] * 7.95F / 16.0F;
        for (int i = 32; i < 40; i++) {
           check(t[i * 2] >= tu0 && t[i * 2] <= tu1, "top cap u in stick strip");
           check(t[i * 2 + 1] >= sideV0 && t[i * 2 + 1] <= capTop1, "top cap v in lit rows 6-7");
           checkF(v[i * 3 + 1], by + 1 + 0.625F, "top cap level");
        }
        for (int i = 40; i < 48; i++) {
           check(t[i * 2] >= tu0 && t[i * 2] <= tu1, "bottom cap u in stick strip");
           check(t[i * 2 + 1] >= rect[1] + rect[3] * 14.05F / 16.0F && t[i * 2 + 1] <= rect[1] + rect[3] * 15.95F / 16.0F, "bottom cap v in rows 14-15");
           checkF(v[i * 3 + 1], by + 1.0F, "bottom cap level");
        }
        MeshBuilder noTorchLayer = new MeshBuilder();
        noTorchLayer.init();
        Blocks.byId(Blocks.TORCH_ID).render(noTorchLayer, l, 1, bx, by + 1, bz);
        check(noTorchLayer.count() == 0, "torch only on lit layer");
        l.setTile(bx, by + 1, bz, 0);
        System.out.println(bright && failures == 0 ? "TORCHFACE PASS" : "TORCHFACE FAIL");
        if (failures > 0) System.exit(1);
    }
}

