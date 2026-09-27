package com.strata.blocks;

public class MeshBuilder {
   private float[] verts = new float[4096 * 3];
   private float[] texs = new float[4096 * 2];
   private float[] cols = new float[4096 * 3];
   private int vertices = 0;
   private float u;
   private float v;
   private float r;
   private float g;
   private float b;
   private boolean hasColor = false;
   private boolean hasTexture = false;

   public void init() {
      this.vertices = 0;
      this.hasColor = false;
      this.hasTexture = false;
   }

   public void tex(float u, float v) {
      this.hasTexture = true;
      this.u = u;
      this.v = v;
   }

   public void color(float r, float g, float b) {
      this.hasColor = true;
      this.r = r;
      this.g = g;
      this.b = b;
   }

   public void vertex(float x, float y, float z) {      if (this.vertices * 3 + 2 >= this.verts.length) {
         this.grow();
      }
      this.verts[this.vertices * 3] = x;
      this.verts[this.vertices * 3 + 1] = y;
      this.verts[this.vertices * 3 + 2] = z;
      if (this.hasTexture) {
         this.texs[this.vertices * 2] = this.u;
         this.texs[this.vertices * 2 + 1] = this.v;
      }

      if (this.hasColor) {
         this.cols[this.vertices * 3] = this.r;
         this.cols[this.vertices * 3 + 1] = this.g;
         this.cols[this.vertices * 3 + 2] = this.b;
      }

      this.vertices++;
   }

   private void grow() {
      float[] nv = new float[this.verts.length * 2];
      System.arraycopy(this.verts, 0, nv, 0, this.verts.length);
      this.verts = nv;
      float[] nt = new float[this.texs.length * 2];
      System.arraycopy(this.texs, 0, nt, 0, this.texs.length);
      this.texs = nt;
      float[] nc = new float[this.cols.length * 2];
      System.arraycopy(this.cols, 0, nc, 0, this.cols.length);
      this.cols = nc;
   }

   public int count() {
      return this.vertices;
   }

   public void quad(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, float ua, float va, float ub, float vb, float uc, float vc, float ud, float vd) {
      this.tex(ua, va);
      this.vertex(ax, ay, az);
      this.tex(ub, vb);
      this.vertex(bx, by, bz);
      this.tex(uc, vc);
      this.vertex(cx, cy, cz);
      this.tex(ud, vd);
      this.vertex(dx, dy, dz);
      this.tex(ua, va);
      this.vertex(ax, ay, az);
      this.tex(ud, vd);
      this.vertex(dx, dy, dz);
      this.tex(uc, vc);
      this.vertex(cx, cy, cz);
      this.tex(ub, vb);
      this.vertex(bx, by, bz);
   }

   public float[] vertices() {
      float[] out = new float[this.vertices * 3];
      System.arraycopy(this.verts, 0, out, 0, out.length);
      return out;
   }

   public float[] texCoords() {
      float[] out = new float[this.vertices * 2];
      System.arraycopy(this.texs, 0, out, 0, out.length);
      return out;
   }

   public float[] colors() {
      float[] out = new float[this.vertices * 3];
      System.arraycopy(this.cols, 0, out, 0, out.length);
      return out;
   }
}

