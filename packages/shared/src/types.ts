export interface UserProfile {
  id: string;
  phone: string;
  displayName?: string;
  avatarUrl?: string;
  locationCoordinates?: {
    latitude: number;
    longitude: number;
  };
  locationUpdatedAt?: string;
  isVerifiedHelper: boolean;
  helperTrustScore: number;
  createdAt: string;
}

export interface Chat {
  id: string;
  type: 'direct' | 'group';
  title?: string;
  avatarUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export type MessageContentType = 'text' | 'image' | 'video' | 'audio' | 'location';

export interface Message {
  id: string;
  chatId: string;
  senderId: string;
  contentType: MessageContentType;
  textContent?: string;
  attachmentUrl?: string;
  locationContent?: {
    latitude: number;
    longitude: number;
  };
  createdAt: string;
}

export type MessageReceiptStatus = 'sent' | 'delivered' | 'read';

export interface MessageReceipt {
  messageId: string;
  profileId: string;
  status: MessageReceiptStatus;
  updatedAt: string;
}

export interface WSPayload<T = any> {
  eventType: string;
  payload: T;
}
