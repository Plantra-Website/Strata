package com.strata.client;

import com.strata.blocks.AtlasStitcher;
import com.strata.blocks.Block;
import com.strata.blocks.Blocks;
import com.strata.blocks.CubeBlock;
import com.strata.blocks.MeshBuilder;
import com.strata.core.MathHelper;
import com.strata.server.ItemEntity;
import com.strata.server.Player;
import com.strata.world.Level;
import com.strata.world.mesh.Textures;
import com.strata.world.mesh.Tesselator;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.opengl.GL11;

public class ItemRenderer {
   private final Level level;
   private final Map<Integer, ItemEntity> items = new HashMap<>();
   private final MeshBuilder scratch = new MeshBuilder();

   public ItemRenderer(Level level) {
      this.level = level;
   }

   public void spawn(int entityId, float x, float y, float z, float xd, float yd, float zd, int blockId) {
      ItemEntity e = new ItemEntity(entityId, blockId, x, y, z, xd, yd, zd);
      float br = this.level.getBrightness(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
      e.r = br;
      e.g = br;
      e.b = br;
      this.items.put(entityId, e);
   }

   public void remove(int entityId) {
      this.items.remove(entityId);
   }

   ItemEntity get(int entityId) {
      return this.items.get(entityId);
   }

   static int emitCube(MeshBuilder b, CubeBlock block, float cx, float cy, float cz,
         float half, double spin, float brightness) {
      float cos = (float)Math.cos(spin);
      float sin = (float)Math.sin(spin);
      float[] top = AtlasStitcher.uv(block.topTexture);
      float[] side = AtlasStitcher.uv(block.sideTexture);
      float[] bottom = AtlasStitcher.uv(block.bottomTexture);
      float[][][] px = new float[2][2][2];
      float[][][] py = new float[2][2][2];
      float[][][] pz = new float[2][2][2];
      for (int ix = 0; ix < 2; ix++) {
         for (int iy = 0; iy < 2; iy++) {
            for (int iz = 0; iz < 2; iz++) {
               float lx = (ix == 0 ? -half : half);
               float ly = (iy == 0 ? -half : half);
               float lz = (iz == 0 ? -half : half);
               px[ix][iy][iz] = cx + lx * cos + lz * sin;
               py[ix][iy][iz] = cy + ly;
               pz[ix][iy][iz] = cz - lx * sin + lz * cos;
            }
         }
      }
      int n = 0;
      n += face(b, 1.0F * brightness,
         px[0][1][0], py[0][1][0], pz[0][1][0], top[0], top[1],
         px[0][1][1], py[0][1][1], pz[0][1][1], top[0], top[3],
         px[1][1][1], py[1][1][1], pz[1][1][1], top[2], top[3],
         px[1][1][0], py[1][1][0], pz[1][1][0], top[2], top[1]);
      n += face(b, 0.5F * brightness,
         px[0][0][0], py[0][0][0], pz[0][0][0], bottom[0], bottom[1],
         px[1][0][0], py[1][0][0], pz[1][0][0], bottom[2], bottom[1],
         px[1][0][1], py[1][0][1], pz[1][0][1], bottom[2], bottom[3],
         px[0][0][1], py[0][0][1], pz[0][0][1], bottom[0], bottom[3]);
      n += face(b, 0.6F * brightness,
         px[1][0][1], py[1][0][1], pz[1][0][1], side[0], side[3],
         px[1][0][0], py[1][0][0], pz[1][0][0], side[2], side[3],
         px[1][1][0], py[1][1][0], pz[1][1][0], side[2], side[1],
         px[1][1][1], py[1][1][1], pz[1][1][1], side[0], side[1]);
      n += face(b, 0.6F * brightness,
         px[0][0][0], py[0][0][0], pz[0][0][0], side[0], side[3],
         px[0][0][1], py[0][0][1], pz[0][0][1], side[2], side[3],
         px[0][1][1], py[0][1][1], pz[0][1][1], side[2], side[1],
         px[0][1][0], py[0][1][0], pz[0][1][0], side[0], side[1]);
      n += face(b, 0.8F * brightness,
         px[0][0][1], py[0][0][1], pz[0][0][1], side[0], side[3],
         px[1][0][1], py[1][0][1], pz[1][0][1], side[2], side[3],
         px[1][1][1], py[1][1][1], pz[1][1][1], side[2], side[1],
         px[0][1][1], py[0][1][1], pz[0][1][1], side[0], side[1]);
      n += face(b, 0.8F * brightness,
         px[1][0][0], py[1][0][0], pz[1][0][0], side[0], side[3],
         px[0][0][0], py[0][0][0], pz[0][0][0], side[2], side[3],
         px[0][1][0], py[0][1][0], pz[0][1][0], side[2], side[1],
         px[1][1][0], py[1][1][0], pz[1][1][0], side[0], side[1]);
      return n;
   }

   private static int face(MeshBuilder b, float shade,
         float ax, float ay, float az, float au, float av,
         float bx, float by, float bz, float bu, float bv,
         float cx, float cy, float cz, float cu, float cv,
         float dx, float dy, float dz, float du, float dv) {
      b.color(shade, shade, shade);
      b.tex(au, av);
      b.vertex(ax, ay, az);
      b.tex(bu, bv);
      b.vertex(bx, by, bz);
      b.tex(cu, cv);
      b.vertex(cx, cy, cz);
      b.tex(du, dv);
      b.vertex(dx, dy, dz);
      return 4;
   }

   public void tick(float px, float py, float pz) {
      for (ItemEntity e : this.items.values()) {
         e.tick(this.level, px, py, pz);
      }
   }

   public void render(Player player, float a) {
      if (this.items.isEmpty()) {
         return;
      }
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      Textures.bind(Textures.loadAtlas(9728));

      float yRot = player.yRot;
      float xa = -(float)Math.cos(yRot * Math.PI / 180.0);
      float za = -(float)Math.sin(yRot * Math.PI / 180.0);

      Tesselator t = Tesselator.SHARED;
      t.init();
      for (ItemEntity e : this.items.values()) {
         float bob = (float)Math.sin((e.age + a) / 10.0 + e.spinPhase) * 0.1F + 0.1F;
         Block b = Blocks.byId(e.blockId);
         if (b instanceof CubeBlock) {
            this.scratch.init();
            emitCube(this.scratch, (CubeBlock)b, e.x, e.y + bob, e.z, 0.125F,
               (e.age + a) / 20.0 + e.spinPhase, (e.r + e.g + e.b) / 3.0F);
            t.drain(this.scratch);
            continue;
         }
         float px = e.x;
         float py = e.y + bob;
         float pz = e.z;
         float s = 0.22F;
         float[] uv = AtlasStitcher.uv(Blocks.particleTile(e.blockId));
         t.color(e.r, e.g, e.b);
         t.tex(uv[0], uv[3]);
         t.vertex(px - xa * s, py - s, pz - za * s);
         t.tex(uv[0], uv[1]);
         t.vertex(px - xa * s, py + s, pz - za * s);
         t.tex(uv[2], uv[1]);
         t.vertex(px + xa * s, py + s, pz + za * s);
         t.tex(uv[2], uv[3]);
         t.vertex(px + xa * s, py - s, pz + za * s);
      }
      t.flush();
      GL11.glDisable(GL11.GL_TEXTURE_2D);
   }
}

