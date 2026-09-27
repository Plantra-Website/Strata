package com.strata.core;

public class Timer {
   private static final long NS_PER_SECOND = 1_000_000_000L;
   private static final long MAX_CATCHUP_NS = NS_PER_SECOND;
   private static final int MAX_TICKS_PER_FRAME = 100;

   private final float ticksPerSecond;
   private long lastTime;
   public int ticks;
   public float alpha;
   public float timeScale = 1.0F;

   public Timer(float ticksPerSecond) {
      this.ticksPerSecond = ticksPerSecond;
      this.lastTime = System.nanoTime();
   }

   public void advanceTime() {
      long now = System.nanoTime();
      long passedNs = now - this.lastTime;
      this.lastTime = now;
      if (passedNs < 0L) {
         passedNs = 0L;
      }
      if (passedNs > MAX_CATCHUP_NS) {
         passedNs = MAX_CATCHUP_NS;
      }
      float passedTicks = passedNs * this.timeScale * this.ticksPerSecond / (float)NS_PER_SECOND;
      float total = passedTicks + this.alpha;
      this.ticks = (int)total;
      if (this.ticks > MAX_TICKS_PER_FRAME) {
         this.ticks = MAX_TICKS_PER_FRAME;
      }
      this.alpha = total - this.ticks;
   }
}

