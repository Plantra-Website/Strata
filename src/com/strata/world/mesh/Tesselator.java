package com.strata.world.mesh;

import com.strata.blocks.MeshBuilder;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class Tesselator {
   public static final Tesselator SHARED = new Tesselator();
   private static final int MAX_VERTICES = 100000;
   private final FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(300000);
   private final FloatBuffer texCoordBuffer = BufferUtils.createFloatBuffer(200000);
   private final FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(400000);
   private int vertices = 0;
   private float u;
   private float v;
   private float r;
   private float g;
   private float b;
   private float a = 1.0F;
   private boolean hasColor = false;
   private boolean hasTexture = false;

   public void flush() {
      ((Buffer)this.vertexBuffer).flip();
      ((Buffer)this.texCoordBuffer).flip();
      ((Buffer)this.colorBuffer).flip();
      GL11.glVertexPointer(3, 0, this.vertexBuffer);
      if (this.hasTexture) {
         GL11.glTexCoordPointer(2, 0, this.texCoordBuffer);
      }

      if (this.hasColor) {
         GL11.glColorPointer(4, 0, this.colorBuffer);
      }

      GL11.glEnableClientState(32884);
      if (this.hasTexture) {
         GL11.glEnableClientState(32888);
      }

      if (this.hasColor) {
         GL11.glEnableClientState(32886);
      }

      GL11.glDrawArrays(7, 0, this.vertices);
      GL11.glDisableClientState(32884);
      if (this.hasTexture) {
         GL11.glDisableClientState(32888);
      }

      if (this.hasColor) {
         GL11.glDisableClientState(32886);
      }

      this.clear();
   }

   private void clear() {
      this.vertices = 0;
      ((Buffer)this.vertexBuffer).clear();
      ((Buffer)this.texCoordBuffer).clear();
      ((Buffer)this.colorBuffer).clear();
   }

   public void init() {
      this.clear();
      this.hasColor = false;
      this.hasTexture = false;
      this.a = 1.0F;
   }

   public void tex(float u, float v) {
      this.hasTexture = true;
      this.u = u;
      this.v = v;
   }

   public void color(float r, float g, float b) {
      this.color(r, g, b, 1.0F);
   }

   public void color(float r, float g, float b, float a) {
      this.hasColor = true;
      this.r = r;
      this.g = g;
      this.b = b;
      this.a = a;
   }

   public void vertex(float x, float y, float z) {
      this.vertexBuffer.put(this.vertices * 3, x).put(this.vertices * 3 + 1, y).put(this.vertices * 3 + 2, z);
      if (this.hasTexture) {
         this.texCoordBuffer.put(this.vertices * 2, this.u).put(this.vertices * 2 + 1, this.v);
      }

      if (this.hasColor) {
         this.colorBuffer.put(this.vertices * 4, this.r).put(this.vertices * 4 + 1, this.g).put(this.vertices * 4 + 2, this.b).put(this.vertices * 4 + 3, this.a);
      }

      this.vertices++;
      if (this.vertices == MAX_VERTICES) {
         this.flush();
      }
   }

   public void drain(MeshBuilder b) {
      float[] v = b.vertices();
      float[] tu = b.texCoords();
      float[] tc = b.colors();
      for (int i = 0; i < b.count(); i++) {
         this.tex(tu[i * 2], tu[i * 2 + 1]);
         this.color(tc[i * 3], tc[i * 3 + 1], tc[i * 3 + 2]);
         this.vertex(v[i * 3], v[i * 3 + 1], v[i * 3 + 2]);
      }
   }
}

