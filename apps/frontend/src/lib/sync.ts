import { db, LocalOutboxItem } from './db';
import { useSyncStore } from '../stores/useSyncStore';
import { useChatStore } from '../stores/useChatStore';
import { mediaStorageService } from '../services/mediaStorage';

class SyncEngine {
  private syncIntervalId: any = null;
  private wsConnection: WebSocket | null = null;
  private maxRetries = 5;

  public setWebSocket(ws: WebSocket | null) {
    this.wsConnection = ws;
  }

  public start() {
    window.addEventListener('online', this.handleOnline.bind(this));
    window.addEventListener('offline', this.handleOffline.bind(this));
    
    // Check queue every 10 seconds
    this.syncIntervalId = setInterval(() => {
      this.processOutbox();
    }, 10000);
  }

  public stop() {
    window.removeEventListener('online', this.handleOnline);
    window.removeEventListener('offline', this.handleOffline);
    if (this.syncIntervalId) {
      clearInterval(this.syncIntervalId);
    }
  }

  private handleOnline() {
    useSyncStore.getState().setOnlineStatus(true);
    this.processOutbox();
  }

  private handleOffline() {
    useSyncStore.getState().setOnlineStatus(false);
  }

  /**
   * Optimistic Update: Insert data locally and register outbox sync task instantly
   */
  public async optimisticSave(
    type: 'message_send' | 'nearby_post' | 'help_request' | 'marketplace_request' | 'marketplace_bid',
    payload: any
  ): Promise<string> {
    const localId = payload.id || crypto.randomUUID();
    const now = new Date().toISOString();

    // 1. Write optimistically to corresponding IndexedDB table
    switch (type) {
      case 'message_send':
        await db.messages.put({
          id: localId,
          chatId: payload.chatId,
          senderId: payload.senderId,
          contentType: payload.contentType,
          textContent: payload.textContent,
          attachmentUrl: payload.attachmentUrl,
          createdAt: now
        });
        // Render instantly in Zustand chat thread
        useChatStore.getState().addMessage(payload.chatId, {
          id: localId,
          chatId: payload.chatId,
          senderId: payload.senderId,
          contentType: payload.contentType,
          textContent: payload.textContent,
          attachmentUrl: payload.attachmentUrl,
          createdAt: now
        });
        break;

      case 'nearby_post':
        await db.nearbyFeed.put({
          id: localId,
          creatorId: payload.creatorId,
          title: payload.title,
          feedType: payload.feedType,
          category: payload.category,
          description: payload.description,
          latitude: payload.latitude,
          longitude: payload.longitude,
          reputationScore: 0,
          createdAt: now,
          distanceMeters: 0
        });
        break;

      case 'help_request':
        await db.helpRequests.put({
          id: localId,
          requesterId: payload.requesterId,
          title: payload.title,
          description: payload.description,
          category: payload.category,
          latitude: payload.latitude,
          longitude: payload.longitude,
          status: 'open',
          minTrustScore: payload.minTrustScore || 3.0,
          createdAt: now
        });
        break;

      case 'marketplace_request':
        await db.needItNowRequests.put({
          id: localId,
          requesterId: payload.requesterId,
          title: payload.title,
          description: payload.description,
          category: payload.category,
          budgetEstimate: payload.budgetEstimate,
          latitude: payload.latitude,
          longitude: payload.longitude,
          status: 'active',
          expiresAt: new Date(Date.now() + 2 * 3600000).toISOString(),
          createdAt: now
        });
        break;
    }

    // 2. Insert transaction job in outbox
    await db.outbox.put({
      id: localId,
      type,
      payload: JSON.stringify(payload),
      createdAt: now,
      isPending: true,
      retryCount: 0
    });

    // 3. Proactively trigger sync attempt
    this.processOutbox();

    return localId;
  }

  /**
   * Process outbox items with backoff controls
   */
  public async processOutbox() {
    const isOnline = useSyncStore.getState().isOnline;
    if (!isOnline) return;

    const isSyncing = useSyncStore.getState().isSyncing;
    if (isSyncing) return;

    useSyncStore.getState().setSyncing(true);

    try {
      const pendingItems = await db.outbox
        .where('isPending')
        .equals(1)
        .toArray();

      for (const item of pendingItems) {
        if (item.retryCount >= this.maxRetries) {
          // Permanently block retrying failed item to prevent infinite loops
          continue;
        }

        // Exponential Backoff Check: wait 2^retryCount seconds since creation
        const creationTime = new Date(item.createdAt).getTime();
        const elapsedSeconds = (Date.now() - creationTime) / 1000;
        const backoffSeconds = Math.pow(2, item.retryCount);

        if (elapsedSeconds < backoffSeconds) {
          continue;
        }

        try {
          await this.syncItem(item);
          await db.outbox.delete(item.id);
          useSyncStore.getState().removeFromQueue(item.id);
        } catch (err) {
          console.error(`Error syncing outbox item ${item.id}:`, err);
          // Increment retry counter and store backoff state
          await db.outbox.update(item.id, {
            retryCount: item.retryCount + 1
          });
        }
      }
    } catch (err) {
      console.error('Offline sync execution failed:', err);
    } finally {
      useSyncStore.getState().setSyncing(false);
    }
  }

  private async syncItem(item: LocalOutboxItem): Promise<void> {
    const payload = JSON.parse(item.payload);
    const apiBase = import.meta.env.VITE_API_URL || '/api/v1';

    switch (item.type) {
      case 'message_send':
        // Handle E2EE attachments if present
        let attachmentUrl = payload.attachmentUrl;
        let size = 0;
        let checksum = '';
        let keyReference = '';

        if (payload.localFileBlob) {
          const fileMetadata = await mediaStorageService.uploadMedia(
            payload.localFileBlob,
            payload.bucketType || 'chat-media',
            payload.chatId,
            `${item.id}.enc`
          );
          attachmentUrl = fileMetadata.url;
          size = fileMetadata.size;
          checksum = fileMetadata.checksum;
          keyReference = fileMetadata.keyReference;
        }

        const messagePacket = {
          event_type: 'message_send',
          payload: {
            local_id: item.id,
            chat_id: payload.chatId,
            content_type: payload.contentType,
            text_content: payload.textContent,
            attachment_url: attachmentUrl,
            media_size: size,
            checksum: checksum,
            encryption_key_reference: keyReference
          }
        };

        if (this.wsConnection && this.wsConnection.readyState === WebSocket.OPEN) {
          this.wsConnection.send(JSON.stringify(messagePacket));
        } else {
          const res = await fetch(`${apiBase}/chats/messages`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${localStorage.getItem('auth_token') || ''}`
            },
            body: JSON.stringify(messagePacket.payload)
          });
          if (!res.ok) throw new Error('API Message delivery failed');
        }
        break;

      case 'nearby_post':
        const postRes = await fetch(`${apiBase}/nearby/posts`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('auth_token') || ''}`
          },
          body: JSON.stringify(payload)
        });
        if (!postRes.ok) throw new Error('API Nearby post upload failed');
        break;

      case 'help_request':
        const helpRes = await fetch(`${apiBase}/help/requests`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('auth_token') || ''}`
          },
          body: JSON.stringify(payload)
        });
        if (!helpRes.ok) throw new Error('API SOS request upload failed');
        break;

      case 'marketplace_request':
        const reqRes = await fetch(`${apiBase}/marketplace/requests`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('auth_token') || ''}`
          },
          body: JSON.stringify(payload)
        });
        if (!reqRes.ok) throw new Error('API Gig request upload failed');
        break;
    }
  }
}

export const syncEngine = new SyncEngine();
