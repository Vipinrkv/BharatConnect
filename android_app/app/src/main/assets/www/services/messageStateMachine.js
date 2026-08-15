/**
 * BharatConnect Message State Machine (www/services/messageStateMachine.js)
 * 
 * Formal message delivery lifecycle management supporting optimistic UI,
 * explicit message states, client message IDs, and receipt transitions.
 */

window.MessageStatus = {
    LOCAL_CREATED: 'LOCAL_CREATED',
    SENDING: 'SENDING',
    SENT: 'SENT',
    DELIVERED: 'DELIVERED',
    READ: 'READ',
    FAILED: 'FAILED',
    RETRYING: 'RETRYING'
};

class MessageStateMachine {
    constructor() {
        this.statusMap = new Map();
    }

    createOptimisticMessage(conversationId, text, senderId, senderName, mediaUrl = null) {
        const timestamp = new Date().toISOString();
        const clientMsgId = 'cli_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6);
        const msgId = 'msg_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6);

        const msgObj = {
            id: msgId,
            conversation_id: conversationId,
            client_message_id: clientMsgId,
            sender_id: senderId,
            sender_name: senderName,
            text: text,
            image_url: mediaUrl,
            status: window.MessageStatus.LOCAL_CREATED,
            time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            created_at: timestamp,
            is_me: true
        };

        return msgObj;
    }

    async transitionStatus(messageId, targetStatus) {
        const validStatuses = Object.values(window.MessageStatus);
        if (!validStatuses.includes(targetStatus)) {
            console.warn(`[MessageStateMachine] Invalid target status: ${targetStatus}`);
            return false;
        }

        if (window.messageRepo) {
            await window.messageRepo.updateMessageStatus(messageId, targetStatus);
        }

        this.statusMap.set(messageId, targetStatus);
        console.log(`[MessageStateMachine] Message ${messageId} -> ${targetStatus}`);
        return true;
    }
}

window.messageStateMachine = new MessageStateMachine();
