export interface UserProfile {
  id: string;
  name: string;
  username: string;
  avatar: string;
  avatarBg: string;
  status: 'online' | 'idle' | 'dnd' | 'offline';
  customStatus?: string;
  bio: string;
  role: 'member' | 'verified_responder' | 'moderator' | 'admin';
  location?: string;
  distance?: string; // e.g. "250m away"
}

export interface Message {
  id: string;
  senderId: string;
  text: string;
  timestamp: string;
  status: 'sent' | 'delivered' | 'read';
  image?: string;
}

export interface Chat {
  id: string;
  user: UserProfile;
  lastMessage: string;
  unreadCount: number;
  timestamp: string;
  isGroup: boolean;
  isChannel: boolean;
  category: 'direct' | 'group' | 'channel' | 'secret';
  messages: Message[];
}

export interface NearbyPost {
  id: string;
  user: UserProfile;
  content: string;
  timestamp: string;
  likes: number;
  comments: number;
  hasLiked?: boolean;
  distance: string;
  media?: string;
  tag?: string;
}

export interface HelpRequest {
  id: string;
  title: string;
  description: string;
  category: 'medical' | 'food' | 'utility' | 'shelter' | 'rescue';
  urgency: 'critical' | 'high' | 'medium' | 'info';
  location: string;
  postedBy: UserProfile;
  timestamp: string;
  verifiedBy: string | null; // Name of authorized agency or null
  coordinates?: { lat: number; lng: number };
  status: 'unresolved' | 'investigating' | 'resolved';
  respondersCount: number;
}

export interface AppSettings {
  theme: 'dark' | 'light';
  notificationsEnabled: boolean;
  accessibilityTextSize: 'small' | 'medium' | 'large';
  hapticFeedback: boolean;
  soundEffects: boolean;
  locationSharing: boolean;
}
