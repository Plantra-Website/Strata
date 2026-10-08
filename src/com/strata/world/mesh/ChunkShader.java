package com.strata.world.mesh;

import com.strata.core.Log;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class ChunkShader {
   private static final String VERT =
      "#version 120\n"
      + "varying vec3 vLight;\n"
      + "varying vec3 vTint;\n"
      + "varying vec2 vUv;\n"
      + "varying float vFogDist;\n"
      + "void main() {\n"
      + "   gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n"
      + "   vLight = gl_Color.rgb;\n"
      + "   vTint = gl_SecondaryColor.rgb;\n"
      + "   vUv = gl_MultiTexCoord0.st;\n"
      + "   vFogDist = -(gl_ModelViewMatrix * gl_Vertex).z;\n"
      + "}\n";
   private static final String FRAG =
      "#version 120\n"
      + "uniform sampler2D tex;\n"
      + "uniform int uSub;\n"
      + "uniform vec3 uFogColor;\n"
      + "uniform float uFogStart;\n"
      + "uniform float uFogEnd;\n"
      + "uniform float uFogOn;\n"
      + "varying vec3 vLight;\n"
      + "varying vec3 vTint;\n"
      + "varying vec2 vUv;\n"
      + "varying float vFogDist;\n"
      + "void main() {\n"
      + "   vec4 tx = texture2D(tex, vUv);\n"
      + "   if (tx.a < 0.5) discard;\n"
      + "   float s = max(vLight.g * 15.0 - float(uSub), 0.0) / 15.0;\n"
      + "   float lum = max(pow(s, 1.3), pow(vLight.b, 1.3));\n"
      + "   vec3 col = tx.rgb * vLight.r * vTint * lum;\n"
      + "   float f = uFogOn * clamp((vFogDist - uFogStart) / max(uFogEnd - uFogStart, 0.0001), 0.0, 1.0);\n"
      + "   col = mix(col, uFogColor, f);\n"
      + "   gl_FragColor = vec4(col, tx.a);\n"
      + "}\n";

   private static int program = 0;
   private static int uSub;
   private static int uFogColor;
   private static int uFogStart;
   private static int uFogEnd;
   private static int uFogOn;
   private static int sub;
   private static float fogR;
   private static float fogG;
   private static float fogB;
   private static float fogStart;
   private static float fogEnd;
   private static float fogOn;

   public static void frame(int sub, float fogR, float fogG, float fogB,
         float fogStart, float fogEnd, boolean fogOn) {
      ChunkShader.sub = sub;
      ChunkShader.fogR = fogR;
      ChunkShader.fogG = fogG;
      ChunkShader.fogB = fogB;
      ChunkShader.fogStart = fogStart;
      ChunkShader.fogEnd = fogEnd;
      ChunkShader.fogOn = fogOn ? 1.0F : 0.0F;
   }

   public static void bind() {
      ensureInit();
      GL20.glUseProgram(program);
      GL20.glUniform1i(uSub, sub);
      GL20.glUniform3f(uFogColor, fogR, fogG, fogB);
      GL20.glUniform1f(uFogStart, fogStart);
      GL20.glUniform1f(uFogEnd, fogEnd);
      GL20.glUniform1f(uFogOn, fogOn);
   }

   public static void unbind() {
      if (program != 0) {
         GL20.glUseProgram(0);
      }
   }

   private static void ensureInit() {
      if (program != 0) {
         return;
      }
      int vs = compile(GL20.GL_VERTEX_SHADER, VERT, "vertex");
      int fs = compile(GL20.GL_FRAGMENT_SHADER, FRAG, "fragment");
      program = GL20.glCreateProgram();
      GL20.glAttachShader(program, vs);
      GL20.glAttachShader(program, fs);
      GL20.glLinkProgram(program);
      if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
         String log = GL20.glGetProgramInfoLog(program, 2048);
         throw new RuntimeException("ChunkShader link failed: " + log);
      }
      uSub = GL20.glGetUniformLocation(program, "uSub");
      uFogColor = GL20.glGetUniformLocation(program, "uFogColor");
      uFogStart = GL20.glGetUniformLocation(program, "uFogStart");
      uFogEnd = GL20.glGetUniformLocation(program, "uFogEnd");
      uFogOn = GL20.glGetUniformLocation(program, "uFogOn");
      Log.info("render", "chunk lighting shader ready (program " + program + ")");
   }

   private static int compile(int type, String src, String name) {
      int sh = GL20.glCreateShader(type);
      GL20.glShaderSource(sh, src);
      GL20.glCompileShader(sh);
      if (GL20.glGetShaderi(sh, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
         String log = GL20.glGetShaderInfoLog(sh, 2048);
         throw new RuntimeException("ChunkShader " + name + " compile failed: " + log);
      }
      return sh;
   }
}

