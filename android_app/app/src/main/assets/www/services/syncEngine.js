/**
 * BharatConnect Client Offline Sync Engine (www/services/syncEngine.js)
 * 
 * Manages the offline sync transaction queue, retries failed operations upon network
 * restoration, and reconciles state with server.
 */

class ClientSyncEngine {
    constructor() {
        this.isSyncing = false;
        this.init();
    }

    init() {
        window.addEventListener('online', () => {
            console.log('[SyncEngine] Network connection restored! Triggering sync...');
            this.pushPendingOperations();
        });

        if (window.connectionManager) {
            window.connectionManager.on('connected', () => {
                console.log('[SyncEngine] WebSocket connected! Reconciling pending operations...');
                this.pushPendingOperations();
            });
        }
    }

    async enqueueOperation(operationType, entityType, entityId, payload) {
        if (!window.syncRepo) return null;
        
        const syncItem = await window.syncRepo.enqueueSyncItem({
            operation_type: operationType,
            entity_type: entityType,
            entity_id: entityId,
            payload: payload
        });

        // Attempt immediate push if online
        if (navigator.onLine) {
            this.pushPendingOperations();
        }

        return syncItem;
    }

    async pushPendingOperations() {
        if (this.isSyncing || !navigator.onLine) return;
        if (!window.syncRepo) return;

        this.isSyncing = true;
        try {
            const pendingItems = await window.syncRepo.getPendingSyncItems();
            if (!pendingItems || pendingItems.length === 0) {
                this.isSyncing = false;
                return;
            }

            console.log(`[SyncEngine] Processing ${pendingItems.length} pending offline operations...`);

            const apiBaseUrl = (window.BHARATCONNECT_CONFIG && window.BHARATCONNECT_CONFIG.API_BASE_URL) || 'https://bharatconnect-api.onrender.com/api/v1';

            for (const item of pendingItems) {
                try {
                    let success = false;

                    if (item.operation_type === 'SEND_MESSAGE') {
                        // Try sending via WebSocket first
                        if (window.connectionManager && window.connectionManager.state === window.ConnectionState.CONNECTED) {
                            success = window.connectionManager.sendEvent('message.send', item.payload, item.payload.conversation_id, item.payload.client_message_id);
                        }

                        // REST Fallback
                        if (!success) {
                            const res = await fetch(`${apiBaseUrl}/chats/${item.payload.conversation_id}/messages`, {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify(item.payload)
                            });
                            success = res.ok;
                        }
                    } else if (item.operation_type === 'CREATE_POST') {
                        const res = await fetch(`${apiBaseUrl}/posts`, {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify(item.payload)
                        });
                        success = res.ok;
                    }

                    const attempts = (item.attempt_count || 0) + 1;
                    const maxDelay = 30000;
                    const backoffMs = Math.min(1000 * Math.pow(2, attempts - 1), maxDelay);
                    const now = Date.now();
                    
                    if (item.next_retry_at && new Date(item.next_retry_at).getTime() > now) {
                        continue;
                    }

                    if (success) {
                        await window.syncRepo.removeSyncItem(item.id);
                        console.log(`[SyncEngine] Offline operation ${item.id} synchronized successfully.`);
                    } else {
                        const nextRetry = new Date(now + backoffMs).toISOString();
                        await window.syncRepo.updateSyncStatus(item.id, 'PENDING', `Sync attempt #${attempts} failed. Next retry at ${nextRetry}`);
                    }
                } catch (err) {
                    const attempts = (item.attempt_count || 0) + 1;
                    console.warn(`[SyncEngine] Error syncing item ${item.id}:`, err);
                    await window.syncRepo.updateSyncStatus(item.id, 'PENDING', err.message);
                }
            }
        } catch (e) {
            console.error('[SyncEngine] Exception during pushPendingOperations:', e);
        } finally {
            this.isSyncing = false;
        }
    }
}

window.syncEngine = new ClientSyncEngine();
