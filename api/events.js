/**
 * Realtime WebSocket Event Handler Dispatcher for BharatConnect
 * Connects WebSocket socket actions to Backend Services and Gateway Broadcasting.
 */
const chatService = require('../backend/services/chatService');
const wsGateway = require('../backend/wsGateway');

class EventDispatcher {
  initialize() {
    wsGateway.setMessageHandler((ws, data) => this.handleEvent(ws, data));
    wsGateway.setDisconnectHandler((userId) => this.handleDisconnect(userId));
  }

  async handleEvent(ws, data) {
    const { event, payload } = data;

    switch (event) {
      case 'auth': {
        const userId = payload.user_id || 'u-101';
        wsGateway.registerUserSocket(userId, ws);
        await chatService.updateUserPresence(userId, 'ONLINE');

        wsGateway.sendToSocket(ws, 'authenticated', { user_id: userId });
        const users = await chatService.searchUsers('');
        wsGateway.broadcastToUsers(
          users.map(u => u.user_id),
          'user.presence',
          { user_id: userId, presence: 'ONLINE' }
        );
        break;
      }

      case 'message.send': {
        if (!ws.userId) return;
        const { message, chat } = await chatService.processIncomingMessage(payload, ws.userId);

        // Send ACK back to sender
        wsGateway.sendToSocket(ws, 'message.ack', message);

        // Broadcast incoming message to participants
        const recipients = chat.participants.filter(id => id !== ws.userId);
        wsGateway.broadcastToUsers(recipients, 'message.receive', message);
        break;
      }

      case 'message.edit': {
        if (!ws.userId) return;
        const { message_id, content } = payload;
        const updatedMsg = await chatService.editMessage(message_id, ws.userId, content);
        if (updatedMsg) {
          const chats = await chatService.getUserChats(ws.userId);
          const chat = chats.find(c => c.chat_id === updatedMsg.chat_id);
          if (chat) {
            wsGateway.broadcastToUsers(chat.participants, 'message.updated', updatedMsg);
          }
        }
        break;
      }

      case 'message.delete': {
        if (!ws.userId) return;
        const { message_id, delete_type } = payload;
        const deletedMsg = await chatService.deleteMessage(message_id, ws.userId, delete_type);
        if (deletedMsg) {
          const chats = await chatService.getUserChats(ws.userId);
          const chat = chats.find(c => c.chat_id === deletedMsg.chat_id);
          if (chat) {
            wsGateway.broadcastToUsers(chat.participants, 'message.updated', deletedMsg);
          }
        }
        break;
      }

      case 'typing.start':
      case 'typing.stop': {
        if (!ws.userId) return;
        const { chat_id } = payload;
        const chats = await chatService.getUserChats(ws.userId);
        const chat = chats.find(c => c.chat_id === chat_id);
        if (chat) {
          const recipients = chat.participants.filter(id => id !== ws.userId);
          wsGateway.broadcastToUsers(recipients, event, { chat_id, user_id: ws.userId });
        }
        break;
      }

      case 'message.read': {
        if (!ws.userId) return;
        const { chat_id } = payload;
        await chatService.markChatRead(chat_id, ws.userId);
        const chats = await chatService.getUserChats(ws.userId);
        const chat = chats.find(c => c.chat_id === chat_id);
        if (chat) {
          wsGateway.broadcastToUsers(chat.participants, 'message.read_receipt', { chat_id, read_by: ws.userId });
        }
        break;
      }
    }
  }

  async handleDisconnect(userId) {
    await chatService.updateUserPresence(userId, 'OFFLINE');
    const users = await chatService.searchUsers('');
    wsGateway.broadcastToUsers(
      users.map(u => u.user_id),
      'user.presence',
      { user_id: userId, presence: 'OFFLINE' }
    );
  }
}

module.exports = new EventDispatcher();
