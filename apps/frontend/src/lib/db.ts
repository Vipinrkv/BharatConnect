import Dexie, { Table } from 'dexie';
import { Message, Chat } from '@bharatconnect/shared';

export interface LocalOutboxItem {
  id: string; // UUID local_id
  type: 'message_send' | 'nearby_post' | 'help_request' | 'marketplace_request' | 'marketplace_bid';
  payload: string; // JSON stringified payload
  createdAt: string;
  isPending: boolean;
  retryCount: number;
}

export interface LocalNearbyPost {
  id: string;
  creatorId: string;
  title: string;
  feedType: 'alert' | 'discussion' | 'observation';
  category: string;
  description: string;
  latitude: number;
  longitude: number;
  attachmentUrl?: string;
  reputationScore: number;
  createdAt: string;
  distanceMeters: number;
}

export interface LocalHelpRequest {
  id: string;
  requesterId: string;
  title: string;
  description: string;
  category: string;
  latitude: number;
  longitude: number;
  status: 'open' | 'assigned' | 'resolved';
  minTrustScore: number;
  createdAt: string;
}

export interface LocalNeedItNowRequest {
  id: string;
  requesterId: string;
  title: string;
  description: string;
  category: string;
  budgetEstimate?: number;
  latitude: number;
  longitude: number;
  status: 'active' | 'fulfilled' | 'expired';
  expiresAt: string;
  createdAt: string;
}

class BharatConnectDatabase extends Dexie {
  chats!: Table<Chat, string>;
  messages!: Table<Message, string>;
  outbox!: Table<LocalOutboxItem, string>;
  nearbyFeed!: Table<LocalNearbyPost, string>;
  helpRequests!: Table<LocalHelpRequest, string>;
  needItNowRequests!: Table<LocalNeedItNowRequest, string>;

  constructor() {
    super('BharatConnectDB');
    this.version(2).stores({
      chats: 'id, type, updatedAt',
      messages: 'id, chatId, senderId, createdAt',
      outbox: 'id, type, isPending, createdAt, retryCount',
      nearbyFeed: 'id, category, feedType, createdAt',
      helpRequests: 'id, category, status, createdAt',
      needItNowRequests: 'id, category, status, expiresAt, createdAt'
    });
  }
}

export const db = new BharatConnectDatabase();
