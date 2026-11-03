package com.strata.core;

public final class SoundEngine {
   private SoundEngine() {
   }

   private static boolean AL;

   private static int[] sources = new int[0];
   private static int sourceIdx = 0;

   private static final java.util.HashMap<String, Integer> buffers =
      new java.util.HashMap<>();

   private static float lx, ly, lz;


   public static void init() {
      try {
         Class.forName("org.lwjgl.openal.AL");
         Class.forName("org.lwjgl.openal.ALC");
         AL = true;
      } catch (ClassNotFoundException e) {
         Log.warn("sound", "OpenAL bindings not present in lwjgl.jar — sound disabled");
         return;
      }
      try {
         org.lwjgl.openal.AL.create();
         int count = 8;
         java.nio.IntBuffer srcBuf = java.nio.IntBuffer.allocate(count);
         org.lwjgl.openal.AL10.alGenSources(srcBuf);
         sources = new int[count];
         srcBuf.flip();
         srcBuf.get(sources);
         for (int s : sources) {
            org.lwjgl.openal.AL10.alSourcef(s, org.lwjgl.openal.AL10.AL_GAIN, 1.0f);
            org.lwjgl.openal.AL10.alSourcef(s, org.lwjgl.openal.AL10.AL_PITCH, 1.0f);
            org.lwjgl.openal.AL10.alSource3f(s, org.lwjgl.openal.AL10.AL_POSITION, 0, 0, 0);
            org.lwjgl.openal.AL10.alSource3f(s, org.lwjgl.openal.AL10.AL_VELOCITY, 0, 0, 0);
         }
         Log.info("sound", "OpenAL initialized (" + sources.length + " sources)");
      } catch (Exception e) {
         AL = false;
         Log.warn("sound", "OpenAL init failed — sound disabled: " + e.getMessage());
      }
   }

   public static void shutdown() {
      if (!AL || sources.length == 0) {
         return;
      }
      try {
         for (int s : sources) {
            org.lwjgl.openal.AL10.alDeleteSources(s);
         }
         for (Integer b : buffers.values()) {
            org.lwjgl.openal.AL10.alDeleteBuffers(b);
         }
         buffers.clear();
         org.lwjgl.openal.AL.destroy();
      } catch (Exception e) {
         Log.warn("sound", "OpenAL shutdown error: " + e.getMessage());
      }
      sources = new int[0];
   }


   public static void load(String name) {
      if (!AL) {
         return;
      }
      if (buffers.containsKey(name)) {
         return;
      }
      try {
         java.io.InputStream in = SoundEngine.class.getResourceAsStream("/sounds/" + name + ".wav");
         if (in == null) {
            java.io.File f = new java.io.File("res/sounds/" + name + ".wav");
            if (!f.isFile()) {
               return;
            }
            in = new java.io.FileInputStream(f);
         }
         int buffer = loadWav(in);
         in.close();
         if (buffer != 0) {
            buffers.put(name, buffer);
            Log.info("sound", "loaded " + name + ".wav");
         }
      } catch (Exception e) {
         Log.warn("sound", "failed to load " + name + ": " + e.getMessage());
      }
   }

   private static int loadWav(java.io.InputStream in) throws Exception {
      java.io.DataInputStream dis = new java.io.DataInputStream(in);
      if (dis.readInt() != 0x46464952) { 
         return 0;
      }
      dis.skipBytes(4); 
      if (dis.readInt() != 0x45564157) { 
         return 0;
      }
      int audioFmt = 0, channels = 0, sampleRate = 0, bits = 0;
      short[] data = null;
      while (dis.available() > 0) {
         int chunkId = dis.readInt();
         int chunkSize = dis.readInt();
         if (chunkId == 0x20746D66) { 
            if (chunkSize < 16) {
               return 0;
            }
            audioFmt = dis.readShort() & 0xFFFF;
            channels = dis.readShort() & 0xFFFF;
            sampleRate = dis.readInt();
            dis.skipBytes(6); 
            bits = dis.readShort() & 0xFFFF;
            if (chunkSize > 16) {
               dis.skipBytes(chunkSize - 16);
            }
         } else if (chunkId == 0x61746164) { 
            int bytes = chunkSize;
            if (bits == 8) {
               data = new short[bytes]; 
               byte[] raw = new byte[bytes];
               dis.readFully(raw);
               for (int i = 0; i < bytes; i++) {
                  data[i] = (short)(((raw[i] & 0xFF) - 128) << 8);
               }
            } else {
               data = new short[bytes / 2];
               byte[] raw = new byte[bytes];
               dis.readFully(raw);
               for (int i = 0; i < data.length; i++) {
                  data[i] = (short)((raw[i * 2] & 0xFF) | ((raw[i * 2 + 1] & 0xFF) << 8));
               }
            }
            break;
         } else {
            dis.skipBytes(chunkSize);
         }
      }
      if (data == null || audioFmt != 1) {
         return 0;
      }
      int buffer = org.lwjgl.openal.AL10.alGenBuffers();
      int format;
      if (channels == 1) {
         format = org.lwjgl.openal.AL10.AL_FORMAT_MONO16;
      } else if (channels == 2) {
         format = org.lwjgl.openal.AL10.AL_FORMAT_STEREO16;
      } else {
         return 0;
      }
      java.nio.ShortBuffer sbuf = java.nio.ShortBuffer.wrap(data);
      org.lwjgl.openal.AL10.alBufferData(buffer, format, sbuf, sampleRate);
      return buffer;
   }


   public static void playSound(String name, float x, float y, float z,
                                float volume, float pitch) {
      if (!AL || sources.length == 0) {
         return;
      }
      Integer buffer = buffers.get(name);
      if (buffer == null) {
         load(name);
         buffer = buffers.get(name);
         if (buffer == null) {
            return;
         }
      }
      int src = -1;
      int start = sourceIdx;
      do {
         int state = org.lwjgl.openal.AL10.alGetSourcei(sources[sourceIdx],
               org.lwjgl.openal.AL10.AL_SOURCE_STATE);
         if (state != org.lwjgl.openal.AL10.AL_PLAYING) {
            src = sources[sourceIdx];
            break;
         }
         sourceIdx = (sourceIdx + 1) % sources.length;
      } while (sourceIdx != start);
      if (src == -1) {
         src = sources[sourceIdx];
         sourceIdx = (sourceIdx + 1) % sources.length;
      }
      try {
         org.lwjgl.openal.AL10.alSourcei(src, org.lwjgl.openal.AL10.AL_BUFFER, buffer);
         org.lwjgl.openal.AL10.alSourcef(src, org.lwjgl.openal.AL10.AL_GAIN, volume);
         org.lwjgl.openal.AL10.alSourcef(src, org.lwjgl.openal.AL10.AL_PITCH, pitch);
         org.lwjgl.openal.AL10.alSource3f(src, org.lwjgl.openal.AL10.AL_POSITION,
               x - lx, y - ly, z - lz);
         org.lwjgl.openal.AL10.alSourcePlay(src);
      } catch (Exception e) {
         Log.warn("sound", "playSound(" + name + ") failed: " + e.getMessage());
      }
   }


   public static void setListener(float x, float y, float z) {
      lx = x;
      ly = y;
      lz = z;
      if (AL) {
         try {
            org.lwjgl.openal.AL10.alListener3f(org.lwjgl.openal.AL10.AL_POSITION, x, y, z);
            org.lwjgl.openal.AL10.alListener3f(org.lwjgl.openal.AL10.AL_VELOCITY, 0, 0, 0);
         } catch (Exception e) {
         }
      }
   }
}

