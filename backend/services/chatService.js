/**
 * Core Chat Business Logic Service for BharatConnect
 * Interacts with database layer and applies state validation.
 */
const db = require('../../database/db');

class ChatService {
  async authenticateUser(username) {
    let user = await db.getUserByUsername(username);
    if (!user) {
      user = await db.getUserById('u-101');
    }
    await db.updateUserPresence(user.user_id, 'ONLINE');
    return user;
  }

  async getCurrentUser(userId) {
    return await db.getUserById(userId);
  }

  async searchUsers(query) {
    return await db.searchUsers(query);
  }

  async getUserChats(userId) {
    return await db.getChatsForUser(userId);
  }

  async createDirectChat(userId, targetUserId) {
    return await db.createDirectChat(userId, targetUserId);
  }

  async createGroupChat(ownerId, title, description, participantIds) {
    return await db.createGroupChat(ownerId, title, description, participantIds);
  }

  async getMessages(chatId) {
    return await db.getMessagesForChat(chatId);
  }

  async processIncomingMessage(payload, senderId) {
    const { chat_id, content, client_message_id, parent_message_id } = payload;
    const chat = await db.getChatById(chat_id);
    if (!chat) throw new Error('Chat room not found');

    const message = await db.saveMessage({
      chatId: chat_id,
      senderId,
      content,
      clientMessageId: client_message_id,
      parentMessageId: parent_message_id
    });

    return { message, chat };
  }

  async editMessage(messageId, senderId, content) {
    return await db.updateMessage(messageId, senderId, content);
  }

  async deleteMessage(messageId, senderId, deleteType) {
    return await db.deleteMessage(messageId, senderId, deleteType);
  }

  async markChatRead(chatId, userId) {
    return await db.markMessagesRead(chatId, userId);
  }

  async updateUserPresence(userId, presence) {
    return await db.updateUserPresence(userId, presence);
  }
}

module.exports = new ChatService();
