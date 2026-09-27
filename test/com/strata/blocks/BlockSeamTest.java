package com.strata.blocks;

public class BlockSeamTest {
   static class MarkerBlock extends Block {
      MarkerBlock() {
         super(100, 0, true, 0);
      }

      @Override
      public void render(MeshBuilder b, BlockView view, int layer, int x, int y, int z) {
         if (layer != 0) {
            return;
         }
         b.color(1, 1, 1);
         b.tex(0, 0);
         b.vertex(x, y, z);
         b.vertex(x + 1, y, z);
         b.vertex(x + 1, y + 1, z);
         b.vertex(x, y + 1, z);
      }
   }

   static class AirView implements BlockView {
      @Override public boolean isSolidTile(int x, int y, int z) { return false; }
      @Override public float getBrightness(int x, int y, int z) { return 1.0F; }
   }

   public static void main(String[] args) {
      Blocks.register(new MarkerBlock());
      Block got = Blocks.byId(100);
      check(got != null && got instanceof MarkerBlock, "registered shape dispatches");
      MeshBuilder b = new MeshBuilder();
      b.init();
      got.render(b, new AirView(), 0, 5, 5, 5);
      check(b.count() == 4, "custom shape emits (" + b.count() + " verts)");
      float[] v = b.vertices();
      check(v[0] == 5.0f && v[1] == 5.0f && v[2] == 5.0f, "custom shape placed");
      MeshBuilder b1 = new MeshBuilder();
      b1.init();
      got.render(b1, new AirView(), 1, 5, 5, 5);
      check(b1.count() == 0, "layer split respected");
      check(Blocks.byId(Blocks.GRASS_ID) instanceof CubeBlock, "grass is a cube");
      check(Blocks.byId(Blocks.TORCH_ID) instanceof TorchBlock, "torch is a post");
      System.out.println("SEAM PASS");
   }

   private static void check(boolean cond, String msg) {
      if (!cond) {
         System.out.println("FAIL: " + msg);
         System.exit(1);
      }
   }
}

