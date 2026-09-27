package com.strata.net;

import java.util.ArrayDeque;
import java.util.Queue;

public class LocalConnection {
   private final Queue<Packet> toServer = new ArrayDeque<>();
   private final Queue<Packet> toClient = new ArrayDeque<>();

   public synchronized void sendToServer(Packet p) {
      this.toServer.add(p);
   }

   public synchronized void sendToClient(Packet p) {
      this.toClient.add(p);
   }

   public synchronized Packet pollServer() {
      return this.toServer.poll();
   }

   public synchronized Packet pollClient() {
      return this.toClient.poll();
   }
}

