package com.strata.net;

import com.strata.blocks.Blocks;
import com.strata.server.GameServer;
import com.strata.world.Level;
import java.io.*;
import java.util.HashMap;

public class NetTest {
    static int failures = 0;

    static void check(boolean cond, String msg) {
        if (!cond) { failures++; System.out.println("FAIL: " + msg); }
    }

    static Packet roundtrip(Packet p) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        p.write(new DataOutputStream(b));
        return Packet.decode(p.id(), new DataInputStream(new ByteArrayInputStream(b.toByteArray())));
    }

    public static void main(String[] args) throws Exception {
        InputState in = new InputState();
        in.fwd = true; in.jump = true; in.up = true; in.breaking = true;
        in.yaw = 1.5f; in.pitch = -0.3f;
        in.breakX = -100; in.breakY = 42; in.breakZ = 200; in.breakFace = 5;
        InputState in2 = (InputState)roundtrip(in);
        check(in2.fwd && in2.jump && in2.up && in2.breaking && !in2.back, "input flags");
        check(in2.yaw == 1.5f && in2.pitch == -0.3f, "input look");
        check(in2.breakX == -100 && in2.breakY == 42 && in2.breakZ == 200 && in2.breakFace == 5, "input target");

        PlaceBlock pl = new PlaceBlock();
        pl.x = 1; pl.y = -2; pl.z = 300000; pl.face = 4; pl.blockId = 9; pl.slot = 3;
        PlaceBlock pl2 = (PlaceBlock)roundtrip(pl);
        check(pl2.x == 1 && pl2.y == -2 && pl2.z == 300000 && pl2.face == 4 && pl2.blockId == 9 && pl2.slot == 3, "place");

        check(roundtrip(new SaveGame()) instanceof SaveGame, "save marker");
        check(roundtrip(new SpectateToggle()) instanceof SpectateToggle, "spectate marker");

       TileUpdate tu = new TileUpdate();
       tu.x = -7; tu.y = 63; tu.z = 8; tu.type = 12; tu.data = 3;
       TileUpdate tu2 = (TileUpdate)roundtrip(tu);
       check(tu2.x == -7 && tu2.y == 63 && tu2.z == 8 && tu2.type == 12 && tu2.data == 3, "tile update");

       BulkTiles bulk = new BulkTiles();
       bulk.columns = new HashMap<>();
       bulk.columns.put(123L, new byte[64]);
       bulk.columns.get(123L)[5] = 7;
       bulk.datas = new HashMap<>();
       bulk.datas.put(123L, new byte[64]);
       bulk.datas.get(123L)[5] = 2;
       BulkTiles bulk2 = (BulkTiles)roundtrip(bulk);
       check(bulk2.columns.size() == 1 && bulk2.columns.get(123L)[5] == 7, "bulk");
       check(bulk2.datas.size() == 1 && bulk2.datas.get(123L)[5] == 2, "bulk datas");

        PlayerState ps = new PlayerState();
        ps.x = 1.5f; ps.y = 2.5f; ps.z = -3.5f; ps.yaw = 3.0f; ps.pitch = 0.5f;
        ps.onGround = true; ps.spectator = true; ps.breakProgress = 0.75f;
        PlayerState ps2 = (PlayerState)roundtrip(ps);
        check(ps2.x == 1.5f && ps2.onGround && ps2.spectator && ps2.breakProgress == 0.75f, "player state");

        TimeUpdate tm = new TimeUpdate();
        tm.time = 987654321L;
        check(((TimeUpdate)roundtrip(tm)).time == 987654321L, "time");

        BreakEffect be = new BreakEffect();
        be.x = 1; be.y = 2; be.z = 3; be.texIndex = 9;
        BreakEffect be2 = (BreakEffect)roundtrip(be);
        check(be2.x == 1 && be2.z == 3 && be2.texIndex == 9, "break effect");

        ItemSpawn is = new ItemSpawn();
        is.entityId = 42; is.x = 1.5f; is.y = 2.5f; is.z = -3.5f;
        is.xd = 0.1f; is.yd = 0.35f; is.zd = -0.1f; is.blockId = 3;
        ItemSpawn is2 = (ItemSpawn)roundtrip(is);
        check(is2.entityId == 42 && is2.x == 1.5f && is2.zd == -0.1f && is2.blockId == 3, "item spawn");

        ItemRemove ir = new ItemRemove();
        ir.entityId = 42;
        check(((ItemRemove)roundtrip(ir)).entityId == 42, "item remove");

        InventorySync iv = new InventorySync();
        check(iv.blocks.length == com.strata.server.Inventory.SLOTS, "sync size matches stock");
        iv.blocks[0] = 3; iv.counts[0] = 64; iv.blocks[8] = 12; iv.counts[8] = 7;
        iv.blocks[35] = 5; iv.counts[35] = 9; iv.heldBlock = 4; iv.heldCount = 2;
        InventorySync iv2 = (InventorySync)roundtrip(iv);
        check(iv2.blocks[0] == 3 && iv2.counts[0] == 64 && iv2.blocks[8] == 12 && iv2.counts[8] == 7, "inventory sync");
        check(iv2.blocks[35] == 5 && iv2.counts[35] == 9, "main store syncs");
        check(iv2.heldBlock == 4 && iv2.heldCount == 2, "held stack syncs");

        SlotClick sc = new SlotClick();
        sc.slot = 35;
        check(((SlotClick)roundtrip(sc)).slot == 35, "slot click");

        check(roundtrip(new DebugGive()) instanceof DebugGive, "debug give marker");
        check(roundtrip(new TimeCycle()) instanceof TimeCycle, "time cycle marker");
        check(roundtrip(new HurtSelf()) instanceof HurtSelf, "hurt self marker");

        MobSpawn ms = new MobSpawn();
        ms.entityId = 9;
        ms.mobType = MobSpawn.ZOMBIE;
        ms.x = 1.5F;
        ms.y = 62.5F;
        ms.z = -3.5F;
        MobSpawn ms2 = (MobSpawn)roundtrip(ms);
        check(ms2.entityId == 9 && ms2.mobType == MobSpawn.ZOMBIE, "mob spawn ids");
        check(ms2.x == 1.5F && ms2.y == 62.5F && ms2.z == -3.5F, "mob spawn pos");
        MobHurt mh = new MobHurt();
        mh.entityId = 9;
        mh.hp = 17;
        mh.xd = 0.3F;
        mh.zd = -0.1F;
        MobHurt mh2 = (MobHurt)roundtrip(mh);
        check(mh2.entityId == 9 && mh2.hp == 17, "mob hurt hp");
        check(mh2.xd == 0.3F && mh2.zd == -0.1F, "mob hurt knockback");
        AttackMob am = new AttackMob();
        am.entityId = 9;
        check(((AttackMob)roundtrip(am)).entityId == 9, "attack mob id");

        FallingSpawn fs = new FallingSpawn();
        fs.entityId = 7;
        fs.blockId = 5;
        fs.x = 1.5F;
        fs.y = 62.5F;
        fs.z = -3.5F;
        FallingSpawn fs2 = (FallingSpawn)roundtrip(fs);
        check(fs2.entityId == 7 && fs2.blockId == 5, "falling spawn ids");
        check(fs2.x == 1.5F && fs2.y == 62.5F && fs2.z == -3.5F, "falling spawn pos");
        System.out.println("packets ok");

        LocalConnection conn = new LocalConnection();
        GameServer server = new GameServer(conn);
        InputState walk = new InputState();
        walk.fwd = true;
        float y0 = 0, z0 = 0;
        boolean gotState = false;
        for (int i = 0; i < 120; i++) {
            conn.sendToServer(walk);
            server.tick();
            Packet p;
            while ((p = conn.pollClient()) != null) {
                if (p instanceof PlayerState) {
                    PlayerState s = (PlayerState)p;
                    y0 = s.y; z0 = s.z;
                    gotState = true;
                }
            }
        }
        check(gotState, "server emits player state");
        check(y0 < 70.0f, "player under the sky (y=" + y0 + ")");
        System.out.println("sim ok (y=" + y0 + " z=" + z0 + ")");

        server.level().setTile(5, 59, 5, Blocks.DIRT_ID);
        server.level().setTile(5, 60, 5, 0);
        PlaceBlock place = new PlaceBlock();
        place.x = 5; place.y = 59; place.z = 5; place.face = 1; place.blockId = 12;
        conn.sendToServer(place);
        server.tick();
        boolean sawUpdate = false;
        Packet p;
        while ((p = conn.pollClient()) != null) {
            if (p instanceof TileUpdate) {
                TileUpdate u = (TileUpdate)p;
                if (u.x == 5 && u.y == 60 && u.z == 5 && u.type == 12) sawUpdate = true;
            }
        }
        check(sawUpdate, "placement produces TileUpdate at offset cell");
        PlaceBlock ghost = new PlaceBlock();
        ghost.x = 6; ghost.y = 59; ghost.z = 5; ghost.face = 1; ghost.blockId = 7;
        conn.sendToServer(ghost);
        server.tick();
        boolean sawGhost = false;
        while ((p = conn.pollClient()) != null) {
            if (p instanceof TileUpdate) {
                TileUpdate u = (TileUpdate)p;
                if (u.x == 6 && u.y == 60 && u.z == 5) sawGhost = true;
            }
        }
        check(!sawGhost, "unstocked placement rejected");
        BulkTiles snap = server.snapshot();
        check(snap.columns.size() > 0, "snapshot non-empty");
        Level mirror = new Level(64, false);
        mirror.applyBulk(snap.columns);
        check(mirror.getTile(5, 60, 5) == 12, "mirror applies bulk");
        System.out.println("sync ok");

        int nx = 60, nz = 60;
        int nh = 0;
        for (int y = 120; y >= 0; y--) {
           int t = server.level().getTile(nx + 1, y, nz + 1);
           if (t > 0 && com.strata.blocks.Blocks.isSolid(t)) { nh = y; break; }
        }
        for (int dx = 55; dx <= 67; dx++) {
           for (int dz = 55; dz <= 67; dz++) {
              server.level().setTile(dx, nh + 1, dz, Blocks.DIRT_ID);
              server.level().setTile(dx, nh + 2, dz, 0);
           }
        }
        server.level().setTile(61, nh + 2, nz + 1, Blocks.LAVA_ID);
        server.level().scheduleTick(61, nh + 2, nz + 1, Level.LAVA_TICKS);
        for (int i = 0; i < Level.LAVA_TICKS + 30; i++) {
           server.tick();
        }
        check(server.level().getTile(62, nh + 2, nz + 1) == Blocks.LAVA_ID, "server spread for mirror test");
        check(server.level().getData(62, nh + 2, nz + 1) == 2, "server flow level 2");
        BulkTiles snap2 = server.snapshot();
        Level mirror2 = new Level(Level.WORLD_DEPTH, false);
        mirror2.applyBulk(snap2.columns, snap2.datas);
        check(mirror2.getTile(62, nh + 2, nz + 1) == Blocks.LAVA_ID, "mirror applies flow tile");
        check(mirror2.getData(62, nh + 2, nz + 1) == 2, "mirror applies flow level");

        conn.sendToServer(new SpectateToggle());
        server.tick();
        boolean spec = false;
        long t1 = -1, t2 = -1;
        while ((p = conn.pollClient()) != null) {
            if (p instanceof PlayerState) spec = ((PlayerState)p).spectator;
            if (p instanceof TimeUpdate) { if (t1 < 0) t1 = ((TimeUpdate)p).time; else t2 = ((TimeUpdate)p).time; }
        }
        check(spec, "spectate on");
        conn.sendToServer(new SpectateToggle());
        server.tick();
        while ((p = conn.pollClient()) != null) {
            if (p instanceof PlayerState) spec = ((PlayerState)p).spectator;
            if (p instanceof TimeUpdate) t2 = ((TimeUpdate)p).time;
        }
        check(!spec, "spectate off");
        check(t2 > t1, "clock advances");
        System.out.println("control ok");

        if (failures == 0) System.out.println("NET PASS");
        else { System.out.println(failures + " FAILURES"); System.exit(1); }
    }
}

