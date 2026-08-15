/**
 * BharatConnect Repository Data Abstraction Layer (www/database/repositories.js)
 * 
 * Provides clean CRUD repository APIs isolating UI components from IndexedDB queries.
 */

class UserRepository {
    constructor(idb) {
        this.idb = idb;
        this.store = "users";
    }

    async getUserById(userId) {
        if (!userId) return null;
        return await this.idb.get(this.store, userId);
    }

    async saveUser(user) {
        if (!user || !user.id) return null;
        await this.idb.put(this.store, user);
        return user;
    }

    async saveUsers(users) {
        if (!users || !Array.isArray(users)) return;
        await this.idb.putMany(this.store, users);
    }

    async getAllUsers() {
        return await this.idb.getAll(this.store);
    }

    async findUserByPhone(phone) {
        if (!phone) return null;
        const norm = window.BharatConnectPhone ? window.BharatConnectPhone.normalizePhone(phone) : phone;
        const allUsers = await this.getAllUsers();
        return allUsers.find(u => {
            const uNorm = window.BharatConnectPhone ? window.BharatConnectPhone.normalizePhone(u.phone) : u.phone;
            return uNorm === norm;
        }) || null;
    }
}

class ConversationRepository {
    constructor(idb) {
        this.idb = idb;
        this.store = "conversations";
    }

    async getConversationById(convId) {
        if (!convId) return null;
        return await this.idb.get(this.store, convId);
    }

    async saveConversation(conv) {
        if (!conv || !conv.id) return null;
        conv.updated_at = conv.updated_at || new Date().toISOString();
        await this.idb.put(this.store, conv);
        return conv;
    }

    async saveConversations(convs) {
        if (!convs || !Array.isArray(convs)) return;
        await this.idb.putMany(this.store, convs);
    }

    async getAllConversations() {
        const convs = await this.idb.getAll(this.store);
        return convs.sort((a, b) => new Date(b.updated_at || 0) - new Date(a.updated_at || 0));
    }

    async deleteConversation(convId) {
        if (!convId) return false;
        return await this.idb.delete(this.store, convId);
    }
}

class MessageRepository {
    constructor(idb) {
        this.idb = idb;
        this.store = "messages";
    }

    async getMessageById(msgId) {
        if (!msgId) return null;
        return await this.idb.get(this.store, msgId);
    }

    async getMessageByClientId(clientMessageId) {
        if (!clientMessageId) return null;
        const results = await this.idb.getAll(this.store, "client_message_id", clientMessageId);
        return results && results.length > 0 ? results[0] : null;
    }

    async getMessagesByConversationId(conversationId, limit = 50, beforeSequence = null) {
        if (!conversationId) return [];
        const allMsgs = await this.idb.getAll(this.store, "conversation_id", conversationId);
        
        let filtered = allMsgs;
        if (beforeSequence !== null && beforeSequence !== undefined) {
            filtered = allMsgs.filter(m => (m.sequence || 0) < beforeSequence);
        }

        filtered.sort((a, b) => (a.sequence || 0) - (b.sequence || 0) || new Date(a.created_at || 0) - new Date(b.created_at || 0));
        
        if (limit && filtered.length > limit) {
            return filtered.slice(filtered.length - limit);
        }
        return filtered;
    }

    async saveMessage(msg) {
        if (!msg || !msg.id) return null;
        msg.created_at = msg.created_at || new Date().toISOString();
        msg.status = msg.status || "SENT";
        await this.idb.put(this.store, msg);
        return msg;
    }

    async saveMessages(msgs) {
        if (!msgs || !Array.isArray(msgs)) return;
        await this.idb.putMany(this.store, msgs);
    }

    async updateMessageStatus(msgId, status) {
        const msg = await this.getMessageById(msgId);
        if (msg) {
            msg.status = status;
            await this.saveMessage(msg);
        }
        return msg;
    }
}

class PostRepository {
    constructor(idb) {
        this.idb = idb;
        this.store = "posts";
    }

    async getPosts() {
        const posts = await this.idb.getAll(this.store);
        return posts.sort((a, b) => new Date(b.created_at || 0) - new Date(a.created_at || 0));
    }

    async savePost(post) {
        if (!post || !post.id) return null;
        post.created_at = post.created_at || new Date().toISOString();
        await this.idb.put(this.store, post);
        return post;
    }

    async savePosts(posts) {
        if (!posts || !Array.isArray(posts)) return;
        await this.idb.putMany(this.store, posts);
    }

    async toggleLike(postId) {
        const post = await this.idb.get(this.store, postId);
        if (post) {
            post.liked = !post.liked;
            post.likes = (post.likes || 0) + (post.liked ? 1 : -1);
            await this.savePost(post);
        }
        return post;
    }
}

class SyncRepository {
    constructor(idb) {
        this.idb = idb;
        this.store = "sync_queue";
    }

    async enqueueSyncItem(item) {
        const syncObj = {
            id: item.id || ('sync_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6)),
            operation_type: item.operation_type,
            entity_type: item.entity_type,
            entity_id: item.entity_id,
            payload: item.payload,
            created_at: new Date().toISOString(),
            attempts: 0,
            status: "PENDING",
            last_error: null
        };
        await this.idb.put(this.store, syncObj);
        return syncObj;
    }

    async getPendingSyncItems() {
        const items = await this.idb.getAll(this.store, "status", "PENDING");
        return items.sort((a, b) => new Date(a.created_at) - new Date(b.created_at));
    }

    async updateSyncStatus(syncId, status, errorMsg = null) {
        const item = await this.idb.get(this.store, syncId);
        if (item) {
            item.status = status;
            item.attempts = (item.attempts || 0) + 1;
            if (errorMsg) item.last_error = errorMsg;
            await this.idb.put(this.store, item);
        }
        return item;
    }

    async removeSyncItem(syncId) {
        return await this.idb.delete(this.store, syncId);
    }
}

// Global Repository Instances
window.userRepo = new UserRepository(window.bharatConnectIDB);
window.conversationRepo = new ConversationRepository(window.bharatConnectIDB);
window.messageRepo = new MessageRepository(window.bharatConnectIDB);
window.postRepo = new PostRepository(window.bharatConnectIDB);
window.syncRepo = new SyncRepository(window.bharatConnectIDB);
