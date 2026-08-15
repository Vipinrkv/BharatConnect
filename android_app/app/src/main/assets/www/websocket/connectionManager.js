/**
 * BharatConnect Realtime WebSocket Connection Manager (www/websocket/connectionManager.js)
 * 
 * Manages resilient WebSocket connection lifecycle, exponential backoff reconnects,
 * heartbeat pings, event protocol parsing, message ACKs, and optimistic state updates.
 */

window.ConnectionState = {
    DISCONNECTED: 'DISCONNECTED',
    CONNECTING: 'CONNECTING',
    CONNECTED: 'CONNECTED',
    RECONNECTING: 'RECONNECTING',
    SYNCING: 'SYNCING'
};

class RealtimeConnectionManager {
    constructor() {
        this.socket = null;
        this.state = window.ConnectionState.DISCONNECTED;
        this.reconnectAttempts = 0;
        this.maxReconnectDelay = 30000;
        this.reconnectTimer = null;
        this.heartbeatTimer = null;
        this.heartbeatInterval = 30000;
        this.listeners = new Map();
        this.pendingAcks = new Map();
    }

    setState(newState) {
        console.log(`[ConnectionManager] State changed: ${this.state} -> ${newState}`);
        this.state = newState;
        this.emit('stateChange', { state: newState });
    }

    on(event, callback) {
        if (!this.listeners.has(event)) {
            this.listeners.set(event, []);
        }
        this.listeners.get(event).push(callback);
    }

    off(event, callback) {
        if (!this.listeners.has(event)) return;
        const callbacks = this.listeners.get(event).filter(cb => cb !== callback);
        this.listeners.set(event, callbacks);
    }

    emit(event, data) {
        if (!this.listeners.has(event)) return;
        this.listeners.get(event).forEach(cb => {
            try {
                cb(data);
            } catch (e) {
                console.error(`[ConnectionManager] Error in listener for event ${event}:`, e);
            }
        });
    }

    connect(url = null) {
        if (this.state === window.ConnectionState.CONNECTED || this.state === window.ConnectionState.CONNECTING) {
            return;
        }

        const wsBaseUrl = (window.BHARATCONNECT_CONFIG && window.BHARATCONNECT_CONFIG.WS_BASE_URL) || 'wss://bharatconnect-api.onrender.com/ws';
        const token = localStorage.getItem('bharatconnect_jwt_token') || '';
        const targetUrl = url || `${wsBaseUrl}/stream${token ? '?token=' + encodeURIComponent(token) : ''}`;

        this.setState(window.ConnectionState.CONNECTING);

        try {
            this.socket = new WebSocket(targetUrl);

            this.socket.onopen = () => {
                this.reconnectAttempts = 0;
                this.setState(window.ConnectionState.CONNECTED);
                this.startHeartbeat();
                this.emit('connected', {});
            };

            this.socket.onmessage = (event) => {
                this.handleMessage(event.data);
            };

            this.socket.onerror = (error) => {
                console.warn('[ConnectionManager] WebSocket error:', error);
                this.emit('error', error);
            };

            this.socket.onclose = (event) => {
                console.log('[ConnectionManager] Connection closed:', event.code, event.reason);
                this.stopHeartbeat();
                this.setState(window.ConnectionState.DISCONNECTED);
                this.scheduleReconnect();
            };

        } catch (e) {
            console.error('[ConnectionManager] Exception during WebSocket connection:', e);
            this.setState(window.ConnectionState.DISCONNECTED);
            this.scheduleReconnect();
        }
    }

    scheduleReconnect() {
        if (this.reconnectTimer) clearTimeout(this.reconnectTimer);

        // Exponential Backoff: 1s, 2s, 4s, 8s, 16s, max 30s with jitter
        const baseDelay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), this.maxReconnectDelay);
        const jitter = Math.floor(Math.random() * 1000);
        const delay = baseDelay + jitter;

        this.reconnectAttempts++;
        this.setState(window.ConnectionState.RECONNECTING);
        console.log(`[ConnectionManager] Scheduling reconnect attempt #${this.reconnectAttempts} in ${delay}ms`);

        this.reconnectTimer = setTimeout(() => {
            this.connect();
        }, delay);
    }

    startHeartbeat() {
        this.stopHeartbeat();
        this.heartbeatTimer = setInterval(() => {
            if (this.state === window.ConnectionState.CONNECTED && this.socket) {
                this.sendEvent('ping', { timestamp: new Date().toISOString() });
            }
        }, this.heartbeatInterval);
    }

    stopHeartbeat() {
        if (this.heartbeatTimer) {
            clearInterval(this.heartbeatTimer);
            this.heartbeatTimer = null;
        }
    }

    sendEvent(eventName, payload = {}, conversationId = null, clientMessageId = null) {
        if (this.state !== window.ConnectionState.CONNECTED || !this.socket) {
            console.warn('[ConnectionManager] Cannot send event while disconnected:', eventName);
            return false;
        }

        const eventFrame = {
            event: eventName,
            event_id: 'evt_' + Date.now() + '_' + Math.floor(Math.random() * 1000),
            conversation_id: conversationId,
            client_message_id: clientMessageId,
            timestamp: new Date().toISOString(),
            payload: payload
        };

        try {
            this.socket.send(JSON.stringify(eventFrame));
            return true;
        } catch (e) {
            console.error('[ConnectionManager] Error sending socket frame:', e);
            return false;
        }
    }

    handleMessage(rawData) {
        try {
            const data = JSON.parse(rawData);
            
            if (data.event === 'pong') {
                return;
            }

            console.log('[ConnectionManager] Received WebSocket event:', data.event, data);

            // Handle ACKs
            if (data.event === 'message.ack' && data.client_message_id) {
                if (window.messageRepo) {
                    window.messageRepo.getMessageByClientId(data.client_message_id).then(msg => {
                        if (msg) {
                            window.messageRepo.updateMessageStatus(msg.id, 'SENT');
                        }
                    });
                }
            }

            // Emit to event listeners
            this.emit(data.event || 'message', data);
            this.emit('*', data);

        } catch (e) {
            console.warn('[ConnectionManager] Unrecognized raw message:', rawData);
        }
    }

    disconnect() {
        if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
        this.stopHeartbeat();
        if (this.socket) {
            this.socket.close();
            this.socket = null;
        }
        this.setState(window.ConnectionState.DISCONNECTED);
    }
}

window.connectionManager = new RealtimeConnectionManager();
