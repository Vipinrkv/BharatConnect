/**
 * Database Abstraction & Storage Engine for BharatConnect
 * Handles data persistence, querying, and user session states.
 */

class DatabaseEngine {
  constructor() {
    this.users = [
      {
        user_id: 'u-101',
        username: 'vipin_k',
        display_name: 'Vipin Kumar',
        email: 'vipin@bharatconnect.com',
        phone: '+91 98765 43210',
        status_message: 'Building the future of BharatConnect 🚀',
        bio: 'Senior Architect & Developer',
        presence: 'ONLINE',
        last_seen: new Date().toISOString()
      },
      {
        user_id: 'u-102',
        username: 'rahul_dev',
        display_name: 'Rahul Sharma',
        email: 'rahul@bharatconnect.com',
        phone: '+91 98123 45678',
        status_message: 'Available for text only 💬',
        bio: 'Fullstack Engineer | Open Source Enthusiast',
        presence: 'ONLINE',
        last_seen: new Date().toISOString()
      },
      {
        user_id: 'u-103',
        username: 'priya_design',
        display_name: 'Priya Patel',
        email: 'priya@bharatconnect.com',
        phone: '+91 98999 11122',
        status_message: 'Designing UI/UX 🎨',
        bio: 'Lead Product Designer',
        presence: 'IDLE',
        last_seen: new Date(Date.now() - 1000 * 60 * 15).toISOString()
      },
      {
        user_id: 'u-104',
        username: 'ananya_pm',
        display_name: 'Ananya Verma',
        email: 'ananya@bharatconnect.com',
        phone: '+91 97777 33344',
        status_message: 'In product reviews 📋',
        bio: 'Product Manager @ BharatConnect',
        presence: 'OFFLINE',
        last_seen: new Date(Date.now() - 1000 * 60 * 120).toISOString()
      }
    ];

    this.chats = [
      {
        chat_id: 'c-direct-1',
        chat_type: 'DIRECT',
        title: null,
        participants: ['u-101', 'u-102'],
        created_at: new Date(Date.now() - 1000 * 60 * 60 * 24).toISOString(),
        pinned_by: [],
        archived_by: [],
        muted_by: []
      },
      {
        chat_id: 'c-direct-2',
        chat_type: 'DIRECT',
        title: null,
        participants: ['u-101', 'u-103'],
        created_at: new Date(Date.now() - 1000 * 60 * 60 * 48).toISOString(),
        pinned_by: ['u-101'],
        archived_by: [],
        muted_by: []
      },
      {
        chat_id: 'c-group-1',
        chat_type: 'GROUP',
        title: 'BharatConnect Core Team 🇮🇳',
        description: 'Official Phase 1 Architecture & Execution channel.',
        owner_id: 'u-101',
        participants: ['u-101', 'u-102', 'u-103', 'u-104'],
        roles: {
          'u-101': 'OWNER',
          'u-102': 'ADMIN',
          'u-103': 'MEMBER',
          'u-104': 'MEMBER'
        },
        created_at: new Date(Date.now() - 1000 * 60 * 60 * 72).toISOString(),
        pinned_by: ['u-101'],
        archived_by: [],
        muted_by: []
      }
    ];

    this.messages = [
      {
        message_id: 'm-1',
        chat_id: 'c-direct-1',
        sender_id: 'u-102',
        seq_id: 1,
        client_message_id: 'client-m1',
        content: 'Hey Vipin! Have you had a chance to review the Phase 1 architecture doc?',
        is_edited: false,
        is_deleted: false,
        is_forwarded: false,
        is_pinned: false,
        status: 'READ',
        created_at: new Date(Date.now() - 1000 * 60 * 45).toISOString()
      },
      {
        message_id: 'm-2',
        chat_id: 'c-direct-1',
        sender_id: 'u-101',
        seq_id: 2,
        client_message_id: 'client-m2',
        content: 'Yes Rahul! Designed all 19 modules without media bloat. Latency is under 50ms.',
        is_edited: false,
        is_deleted: false,
        is_forwarded: false,
        is_pinned: true,
        status: 'READ',
        created_at: new Date(Date.now() - 1000 * 60 * 30).toISOString()
      },
      {
        message_id: 'm-3',
        chat_id: 'c-group-1',
        sender_id: 'u-101',
        seq_id: 1,
        client_message_id: 'client-m3',
        content: 'Welcome everyone to the BharatConnect Core Team group!',
        is_edited: false,
        is_deleted: false,
        is_forwarded: false,
        is_pinned: true,
        status: 'READ',
        created_at: new Date(Date.now() - 1000 * 60 * 60 * 2).toISOString()
      },
      {
        message_id: 'm-4',
        chat_id: 'c-group-1',
        sender_id: 'u-103',
        seq_id: 2,
        client_message_id: 'client-m4',
        content: 'The dark mode color palette looks incredibly sleek and modern!',
        is_edited: false,
        is_deleted: false,
        is_forwarded: false,
        is_pinned: false,
        status: 'READ',
        created_at: new Date(Date.now() - 1000 * 60 * 50).toISOString()
      }
    ];

    this.blocks = {};
    this.contacts = { 'u-101': ['u-102', 'u-103', 'u-104'] };
  }

  // User Operations
  async getUserById(userId) {
    return this.users.find(u => u.user_id === userId) || this.users[0];
  }

  async getUserByUsername(username) {
    return this.users.find(u => u.username.toLowerCase() === (username || '').toLowerCase());
  }

  async searchUsers(query) {
    const q = (query || '').toLowerCase();
    if (!q) return this.users;
    return this.users.filter(u =>
      u.username.toLowerCase().includes(q) ||
      u.display_name.toLowerCase().includes(q) ||
      u.email.toLowerCase().includes(q) ||
      u.phone.includes(q)
    );
  }

  async updateUserPresence(userId, presence) {
    const user = this.users.find(u => u.user_id === userId);
    if (user) {
      user.presence = presence;
      user.last_seen = new Date().toISOString();
    }
    return user;
  }

  // Chat Operations
  async getChatsForUser(userId) {
    const userChats = this.chats.filter(c => c.participants.includes(userId));
    return userChats.map(chat => {
      const chatMsgs = this.messages.filter(m => m.chat_id === chat.chat_id);
      const lastMsg = chatMsgs[chatMsgs.length - 1] || null;
      const unreadCount = chatMsgs.filter(m => m.sender_id !== userId && m.status !== 'READ').length;

      let targetUser = null;
      if (chat.chat_type === 'DIRECT') {
        const otherId = chat.participants.find(p => p !== userId) || userId;
        targetUser = this.users.find(u => u.user_id === otherId);
      }

      return {
        ...chat,
        targetUser,
        last_message: lastMsg,
        unread_count: unreadCount,
        is_pinned: (chat.pinned_by || []).includes(userId),
        is_archived: (chat.archived_by || []).includes(userId),
        is_muted: (chat.muted_by || []).includes(userId)
      };
    });
  }

  async getChatById(chatId) {
    return this.chats.find(c => c.chat_id === chatId);
  }

  async createDirectChat(userId, targetUserId) {
    let existing = this.chats.find(c =>
      c.chat_type === 'DIRECT' &&
      c.participants.includes(userId) &&
      c.participants.includes(targetUserId)
    );

    if (!existing) {
      existing = {
        chat_id: `c-direct-${Date.now()}`,
        chat_type: 'DIRECT',
        title: null,
        participants: [userId, targetUserId],
        created_at: new Date().toISOString(),
        pinned_by: [],
        archived_by: [],
        muted_by: []
      };
      this.chats.push(existing);
    }
    return existing;
  }

  async createGroupChat(ownerId, title, description, participantIds) {
    const participants = Array.from(new Set([ownerId, ...(participantIds || [])]));
    const newGroup = {
      chat_id: `c-group-${Date.now()}`,
      chat_type: 'GROUP',
      title: title || 'New Group Chat',
      description: description || '',
      owner_id: ownerId,
      participants,
      roles: { [ownerId]: 'OWNER' },
      created_at: new Date().toISOString(),
      pinned_by: [],
      archived_by: [],
      muted_by: []
    };
    this.chats.push(newGroup);
    return newGroup;
  }

  // Message Operations
  async getMessagesForChat(chatId) {
    return this.messages.filter(m => m.chat_id === chatId);
  }

  async saveMessage({ chatId, senderId, content, clientMessageId, parentMessageId }) {
    const chatMsgs = this.messages.filter(m => m.chat_id === chatId);
    const seq_id = chatMsgs.length + 1;

    const newMsg = {
      message_id: `m-${Date.now()}`,
      chat_id: chatId,
      sender_id: senderId,
      seq_id,
      client_message_id: clientMessageId || `client-${Date.now()}`,
      parent_message_id: parentMessageId || null,
      content,
      is_edited: false,
      is_deleted: false,
      is_forwarded: false,
      is_pinned: false,
      status: 'DELIVERED',
      created_at: new Date().toISOString()
    };

    this.messages.push(newMsg);
    return newMsg;
  }

  async updateMessage(messageId, senderId, content) {
    const msg = this.messages.find(m => m.message_id === messageId);
    if (msg && msg.sender_id === senderId) {
      msg.content = content;
      msg.is_edited = true;
      msg.updated_at = new Date().toISOString();
      return msg;
    }
    return null;
  }

  async deleteMessage(messageId, senderId, deleteType) {
    const msg = this.messages.find(m => m.message_id === messageId);
    if (msg) {
      if (deleteType === 'everyone' && msg.sender_id === senderId) {
        msg.is_deleted = true;
        msg.content = 'This message was deleted';
        msg.updated_at = new Date().toISOString();
        return msg;
      }
    }
    return null;
  }

  async markMessagesRead(chatId, userId) {
    const chatMsgs = this.messages.filter(m => m.chat_id === chatId && m.sender_id !== userId);
    chatMsgs.forEach(m => m.status = 'READ');
    return chatMsgs;
  }
}

module.exports = new DatabaseEngine();
