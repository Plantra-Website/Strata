package com.strata.world.storage;

import com.strata.world.storage.nbt.NBT;
import com.strata.world.storage.nbt.RegionFile;
import java.io.*;
import java.util.zip.DeflaterOutputStream;

public class NbtTest {
    static int failures = 0;

    static void check(boolean cond, String msg) {
        if (!cond) { failures++; System.out.println("FAIL: " + msg); }
    }

    static NBT.CompoundTag chunkPayload(int x, int z) {
        NBT.CompoundTag root = new NBT.CompoundTag();
        NBT.CompoundTag level = new NBT.CompoundTag();
        level.put("xPos", new NBT.IntTag(x));
        level.put("zPos", new NBT.IntTag(z));
        NBT.ListTag sections = new NBT.ListTag((byte)10);
        for (int s = 0; s < 4; s++) {
            NBT.CompoundTag sec = new NBT.CompoundTag();
            sec.put("Y", new NBT.ByteTag((byte)s));
            byte[] blocks = new byte[4096];
            for (int i = 0; i < blocks.length; i++) blocks[i] = (byte)((i + s + x + z) & 0xFF);
            sec.put("Blocks", new NBT.ByteArrayTag(blocks));
            sections.value.add(sec);
        }
        level.put("Sections", sections);
        level.put("Name", new NBT.StringTag("test"));
        root.put("Level", level);
        return root;
    }

    static byte[] toBytes(NBT.CompoundTag root) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        NBT.writeRoot(root, new DataOutputStream(b));
        return b.toByteArray();
    }

    static NBT.CompoundTag fromBytes(byte[] data) throws Exception {
        return NBT.readRoot(new DataInputStream(new ByteArrayInputStream(data)));
    }

    static byte[] nestBytes(int depth) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(b);
        d.writeByte(10); 
        d.writeUTF("");
        for (int i = 0; i < depth; i++) {
            d.writeByte(10); 
            d.writeUTF("");
        }
        for (int i = 0; i <= depth; i++) {
            d.writeByte(0); 
        }
        d.flush();
        return b.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        com.strata.server.ItemTest.wipeDir(new File("nbttest-region"));
        NBT.CompoundTag root = chunkPayload(-5, 17);
        NBT.CompoundTag back = fromBytes(toBytes(root));
        NBT.CompoundTag lvl = back.compound("Level");
        check(lvl.integer("xPos") == -5, "xPos survives");
        check(lvl.integer("zPos") == 17, "zPos survives");
        NBT.ListTag secs = (NBT.ListTag)lvl.get("Sections");
        check(secs.value.size() == 4, "4 sections");
        for (int s = 0; s < 4; s++) {
            NBT.CompoundTag sec = (NBT.CompoundTag)secs.value.get(s);
            byte[] orig = ((NBT.ByteArrayTag)((NBT.CompoundTag)secs_check(root, s)).get("Blocks")).value;
            byte[] got = ((NBT.ByteArrayTag)sec.get("Blocks")).value;
            check(got.length == 4096 && java.util.Arrays.equals(orig, got), "section " + s + " blocks intact");
        }
        check(((NBT.StringTag)lvl.get("Name")).value.equals("test"), "string tag");
        System.out.println("nbt ok");

        File dir = new File("nbttest-region");
        RegionFile r = new RegionFile(new File(dir, "r.0.0.mca"));
        check(!r.hasChunk(0, 0), "missing chunk absent");
        byte[] a = toBytes(chunkPayload(3, 7));
        byte[] b = toBytes(chunkPayload(-31, -31));
        r.writeChunk(3, 7, a);
        r.writeChunk(31, 31, b);
        check(r.hasChunk(3, 7) && r.hasChunk(31, 31), "written chunks present");
        NBT.CompoundTag ra = fromBytes(r.readChunk(3, 7));
        check(ra.compound("Level").integer("xPos") == 3, "region payload A intact");
        NBT.CompoundTag rb = fromBytes(r.readChunk(31, 31));
        check(rb.compound("Level").integer("xPos") == -31, "region payload B intact");
        byte[] a2 = toBytes(chunkPayload(99, 99));
        r.writeChunk(3, 7, a2);
        check(fromBytes(r.readChunk(3, 7)).compound("Level").integer("xPos") == 99, "overwrite works");
        r.close();
        RegionFile r2 = new RegionFile(new File(dir, "r.0.0.mca"));
        check(fromBytes(r2.readChunk(3, 7)).compound("Level").integer("xPos") == 99, "persists across reopen");
        check(fromBytes(r2.readChunk(31, 31)).compound("Level").integer("zPos") == -31, "second chunk persists");
        r2.close();
        System.out.println("region ok");

        File cfile = new File(dir, "r.9.9.mca");
        if (cfile.exists()) cfile.delete();
        RegionFile rc = new RegionFile(cfile);
        byte[] big = new byte[20000];
        new java.util.Random(1234).nextBytes(big);
        rc.writeChunk(0, 0, big);
        byte[] small = toBytes(chunkPayload(1, 1));
        rc.writeChunk(0, 0, small); 
        byte[] big2 = new byte[20000];
        new java.util.Random(5678).nextBytes(big2);
        rc.writeChunk(1, 1, big2);
        rc.writeChunk(1, 1, small); 
        double waste = rc.wasteFraction();
        check(waste > 0.0, "waste tracked (" + waste + ")");
        long lenBefore = cfile.length();
        for (int i = 0; i < 6; i++) {
            byte[] rnd = new byte[20000];
            new java.util.Random(100 + i).nextBytes(rnd);
            rc.writeChunk(2, 2, rnd);
        }
        rc.writeChunk(2, 2, small);
        check(rc.wasteFraction() > 0.5, "waste exceeds half (" + rc.wasteFraction() + ")");
        check(rc.maybeCompact(), "compaction triggers");
        check(rc.wasteFraction() == 0.0, "waste reset");
        check(cfile.length() < lenBefore + 6 * 24576L, "file repacked (" + lenBefore + " -> " + cfile.length() + ")");
        check(java.util.Arrays.equals(rc.readChunk(0, 0), small), "chunk survives compact");
        check(java.util.Arrays.equals(rc.readChunk(1, 1), small), "second chunk survives");
        check(java.util.Arrays.equals(rc.readChunk(2, 2), small), "churned chunk survives");
        check(!rc.maybeCompact(), "no second compact when clean");
        rc.close();
        System.out.println("compact ok");

        File torn = new File(dir, "r.7.7.mca");
        java.io.FileOutputStream tornOut = new java.io.FileOutputStream(torn);
        tornOut.write(new byte[100]);
        tornOut.close();
        boolean threw = false;
        try {
           new RegionFile(torn);
        } catch (java.io.IOException e) {
           threw = true;
        }
        check(threw, "short region file throws");
        boolean capped = false;
        try {
           NBT.readRoot(new DataInputStream(new ByteArrayInputStream(oversizeRoot())));
        } catch (java.io.IOException e) {
           capped = true;
        } catch (OutOfMemoryError e) {
           capped = false;
        }
        check(capped, "oversize NBT stays IOException");

        try {
            fromBytes(nestBytes(10000));
            check(false, "deep nesting read without throwing");
        } catch (IOException e) {
            check(e.getMessage() != null && e.getMessage().contains("too deep"),
                "deep nesting rejected as IOException (" + e.getMessage() + ")");
        } catch (StackOverflowError e) {
            check(false, "deep nesting blew the stack instead of hitting the cap");
        }
        NBT.CompoundTag wrap = new NBT.CompoundTag();
        NBT.CompoundTag innerWrap = new NBT.CompoundTag();
        innerWrap.put("v", new NBT.IntTag(7));
        wrap.put("wrap", innerWrap);
        check(fromBytes(toBytes(wrap)).compound("wrap").integer("v") == 7, "sane nesting still reads");
        System.out.println("depth ok");

        {
            RegionFile ro = new RegionFile(new File(dir, "r.4.4.mca"));
            byte[] blob = new byte[1200000]; 
            new java.util.Random(4242).nextBytes(blob);
            boolean rejected = false;
            try {
                ro.writeChunk(3, 4, blob);
            } catch (IOException e) {
                rejected = e.getMessage() != null && e.getMessage().contains("too large");
            }
            check(rejected, "oversize chunk write rejected loudly");
            check(!ro.hasChunk(3, 4), "rejected chunk left unwritten");
            ro.writeChunk(3, 4, toBytes(chunkPayload(3, 4)));
            check(fromBytes(ro.readChunk(3, 4)).compound("Level").integer("xPos") == 3,
                "region still writable after rejection");
            ro.close();
            System.out.println("oversize ok");
        }

        {
            File gfile = new File(dir, "r.5.5.mca");
            if (gfile.exists()) gfile.delete();
            RegionFile rg = new RegionFile(gfile);
            byte[] keep = toBytes(chunkPayload(1, 2));
            byte[] write = toBytes(chunkPayload(6, 6));
            rg.writeChunk(1, 2, keep);
            rg.writeChunk(6, 6, write);
            rg.close();
            try (RandomAccessFile f = new RandomAccessFile(gfile, "rw")) {
                int idx = (6 & 31) + ((6 & 31) << 5);
                f.seek((long)idx * 4);
                f.writeInt((1 << 8) | 8);
            }
            RegionFile rg2 = new RegionFile(gfile);
            rg2.writeChunk(6, 6, write); 
            check(java.util.Arrays.equals(rg2.readChunk(1, 2), keep),
                "neighbor intact after corrupt-offset write");
            check(java.util.Arrays.equals(rg2.readChunk(6, 6), write),
                "corrupt-offset chunk rewritten readable");
            rg2.close();
            System.out.println("corrupt-offset ok");
        }

        {
            byte[] bomb;
            try {
                ByteArrayOutputStream bombBytes = new ByteArrayOutputStream();
                DeflaterOutputStream d = new DeflaterOutputStream(bombBytes);
                byte[] zeros = new byte[65536];
                for (int i = 0; i < 272; i++) d.write(zeros); 
                d.close();
                bomb = bombBytes.toByteArray();
            } catch (IOException e) {
                throw new Exception("bomb build failed", e);
            }
            int sectors = (bomb.length + 1 + 4 + 4095) / 4096;
            File bfile = new File(dir, "r.6.6.mca");
            if (bfile.exists()) bfile.delete();
            try (RandomAccessFile f = new RandomAccessFile(bfile, "rw")) {
                f.setLength(8192L + sectors * 4096L);
                int idx = (1 & 31) + ((1 & 31) << 5);
                f.seek((long)idx * 4);
                f.writeInt((2 << 8) | sectors);
                f.seek(8192L);
                f.writeInt(bomb.length + 1);
                f.writeByte(2); 
                f.write(bomb);
            }
            boolean bombCaught = false;
            try (RegionFile rb2 = new RegionFile(bfile)) {
                rb2.readChunk(1, 1);
            } catch (IOException e) {
                bombCaught = true;
            } catch (OutOfMemoryError e) {
                bombCaught = false;
            }
            check(bombCaught, "decompression bomb rejected as IOException");
            System.out.println("inflate-cap ok");
        }

        if (failures == 0) System.out.println("ALL PASS");
        else { System.out.println(failures + " FAILURES"); System.exit(1); }
    }

    static NBT.Tag secs_check(NBT.CompoundTag root, int s) {
        return ((NBT.ListTag)root.compound("Level").get("Sections")).value.get(s);
    }

    static byte[] oversizeRoot() throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        out.writeByte(10);
        out.writeUTF("");
        out.writeByte(7);
        out.writeUTF("x");
        out.writeInt(1 << 20);
        out.writeByte(0);
        out.close();
        return b.toByteArray();
    }
}

