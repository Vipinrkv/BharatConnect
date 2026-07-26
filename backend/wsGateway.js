/**
 * WebSocket Realtime Connection Gateway for BharatConnect
 * Handles socket sessions, heartbeat, presence states, and event broadcasting.
 */
const WebSocket = require('ws');

class WSGateway {
  constructor() {
    this.activeSockets = new Map(); // user_id -> Set of WebSocket clients
  }

  initialize(server) {
    this.wss = new WebSocket.Server({ server });
    this.wss.on('connection', (ws) => this.handleConnection(ws));
    console.log('WebSocket Gateway initialized successfully.');
  }

  handleConnection(ws) {
    ws.userId = null;

    ws.on('message', async (rawMsg) => {
      try {
        const data = JSON.parse(rawMsg);
        if (this.onMessageCallback) {
          await this.onMessageCallback(ws, data);
        }
      } catch (err) {
        console.error('WebSocket parsing error:', err);
      }
    });

    ws.on('close', () => {
      if (ws.userId && this.activeSockets.has(ws.userId)) {
        this.activeSockets.get(ws.userId).delete(ws);
        if (this.activeSockets.get(ws.userId).size === 0) {
          this.activeSockets.delete(ws.userId);
          if (this.onDisconnectCallback) {
            this.onDisconnectCallback(ws.userId);
          }
        }
      }
    });
  }

  registerUserSocket(userId, ws) {
    ws.userId = userId;
    if (!this.activeSockets.has(userId)) {
      this.activeSockets.set(userId, new Set());
    }
    this.activeSockets.get(userId).add(ws);
  }

  setMessageHandler(callback) {
    this.onMessageCallback = callback;
  }

  setDisconnectHandler(callback) {
    this.onDisconnectCallback = callback;
  }

  broadcastToUsers(userIds, eventType, payload) {
    const messageJson = JSON.stringify({ event: eventType, payload });
    userIds.forEach(uid => {
      const userSockets = this.activeSockets.get(uid);
      if (userSockets) {
        userSockets.forEach(ws => {
          if (ws.readyState === WebSocket.OPEN) {
            ws.send(messageJson);
          }
        });
      }
    });
  }

  sendToSocket(ws, eventType, payload) {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ event: eventType, payload }));
    }
  }
}

module.exports = new WSGateway();
