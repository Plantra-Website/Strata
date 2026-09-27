package com.strata.client;

import com.strata.blocks.Block;
import com.strata.blocks.Blocks;
import com.strata.blocks.CubeBlock;
import com.strata.blocks.MeshBuilder;
import com.strata.core.MathHelper;
import com.strata.server.FallingBlock;
import com.strata.world.Level;
import com.strata.world.mesh.Tesselator;
import com.strata.world.mesh.Textures;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.opengl.GL11;

public class FallingRenderer {
   private final Level level;
   private final Map<Integer, FallingBlock> falling = new HashMap<>();
   private final MeshBuilder scratch = new MeshBuilder();

   public FallingRenderer(Level level) {
      this.level = level;
   }

   public void spawn(int entityId, int blockId, float x, float y, float z) {
      this.falling.put(entityId, new FallingBlock(this.level, entityId, blockId, x, y, z));
   }

   public void remove(int entityId) {
      this.falling.remove(entityId);
   }

   public void tick() {
      for (FallingBlock e : this.falling.values()) {
         e.tick();
      }
   }

   public void render() {
      if (this.falling.isEmpty()) {
         return;
      }
      Tesselator t = Tesselator.SHARED;
      Textures.bind(Textures.loadAtlas(9728));
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      t.init();
      for (FallingBlock e : this.falling.values()) {
         Block b = Blocks.byId(e.blockId);
         if (!(b instanceof CubeBlock)) {
            continue;
         }
         float br = this.level.getBrightness(MathHelper.floor(e.x), MathHelper.floor(e.y), MathHelper.floor(e.z));
         this.scratch.init();
         ItemRenderer.emitCube(this.scratch, (CubeBlock)b, e.x, e.y, e.z, 0.50F, 0.0, br);
         t.drain(this.scratch);
      }
      t.flush();
   }
}

