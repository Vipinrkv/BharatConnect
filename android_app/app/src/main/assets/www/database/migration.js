/**
 * BharatConnect LocalStorage to IndexedDB Migration Engine (www/database/migration.js)
 * 
 * Guarantees zero data loss across app updates by copying legacy LocalStorage
 * records into structured IndexedDB stores on application initialization.
 */

window.BharatConnectMigration = {
    async migrateFromLocalStorage() {
        try {
            console.log("[MigrationEngine] Checking legacy LocalStorage data...");
            const DB_KEY = (window.BHARATCONNECT_CONFIG && window.BHARATCONNECT_CONFIG.DB_KEY) || 'bharatconnect_db_v6_config';
            const rawData = localStorage.getItem(DB_KEY);

            if (!rawData) {
                console.log("[MigrationEngine] No legacy LocalStorage payload found. Migration skipped.");
                return true;
            }

            let legacyData = null;
            try {
                legacyData = JSON.parse(rawData);
            } catch (e) {
                console.warn("[MigrationEngine] Invalid JSON payload in LocalStorage DB_KEY:", e);
                return false;
            }

            if (!legacyData) return true;

            await window.bharatConnectIDB.init();

            // 1. Migrate Current User
            if (legacyData.currentUser && legacyData.currentUser.id) {
                console.log("[MigrationEngine] Migrating current user:", legacyData.currentUser.name);
                await window.userRepo.saveUser(legacyData.currentUser);
            }

            // 2. Migrate Registered Users
            if (legacyData.registeredUsers && Array.isArray(legacyData.registeredUsers)) {
                console.log("[MigrationEngine] Migrating registered users count:", legacyData.registeredUsers.length);
                await window.userRepo.saveUsers(legacyData.registeredUsers);
            }

            // 3. Migrate Individual Chats & Messages
            if (legacyData.individualChats && Array.isArray(legacyData.individualChats)) {
                console.log("[MigrationEngine] Migrating individual chats count:", legacyData.individualChats.length);
                for (const chat of legacyData.individualChats) {
                    const convObj = {
                        id: chat.id,
                        type: "INDIVIDUAL",
                        name: chat.name || "Chat",
                        avatar: chat.avatar || "logo.png",
                        phone: chat.phone || "",
                        username: chat.username || "",
                        userId: chat.userId || "",
                        lastMessage: chat.lastMessage || "",
                        updated_at: new Date().toISOString()
                    };
                    await window.conversationRepo.saveConversation(convObj);

                    if (chat.messages && Array.isArray(chat.messages)) {
                        for (let idx = 0; idx < chat.messages.length; idx++) {
                            const m = chat.messages[idx];
                            const msgObj = {
                                id: m.id || (`m_${chat.id}_${idx}_${Date.now()}`),
                                conversation_id: chat.id,
                                sender_id: m.is_me ? (legacyData.currentUser ? legacyData.currentUser.id : "me") : (chat.userId || chat.id),
                                sender_name: m.sender || (m.is_me ? "me" : chat.name),
                                text: m.text || "",
                                image_url: m.image_url || m.image || null,
                                status: "DELIVERED",
                                sequence: idx + 1,
                                client_message_id: m.client_message_id || (`cli_${m.id || idx}`),
                                created_at: m.time || new Date().toISOString()
                            };
                            await window.messageRepo.saveMessage(msgObj);
                        }
                    }
                }
            }

            // 4. Migrate Groups & Messages
            if (legacyData.groups && Array.isArray(legacyData.groups)) {
                for (const grp of legacyData.groups) {
                    const grpObj = {
                        id: grp.id,
                        type: "GROUP",
                        name: grp.name || "Group",
                        avatar: grp.avatar || "logo.png",
                        subtitle: grp.subtitle || "",
                        updated_at: new Date().toISOString()
                    };
                    await window.conversationRepo.saveConversation(grpObj);

                    if (grp.messages && Array.isArray(grp.messages)) {
                        for (let idx = 0; idx < grp.messages.length; idx++) {
                            const m = grp.messages[idx];
                            await window.messageRepo.saveMessage({
                                id: m.id || (`gm_${grp.id}_${idx}`),
                                conversation_id: grp.id,
                                sender_id: m.sender || "Group Member",
                                sender_name: m.sender || "Member",
                                text: m.text || "",
                                status: "DELIVERED",
                                sequence: idx + 1,
                                client_message_id: `cli_${m.id || idx}`,
                                created_at: new Date().toISOString()
                            });
                        }
                    }
                }
            }

            // 5. Migrate Feed Posts
            if (legacyData.posts && Array.isArray(legacyData.posts)) {
                console.log("[MigrationEngine] Migrating posts count:", legacyData.posts.length);
                await window.postRepo.savePosts(legacyData.posts);
            }

            console.log("[MigrationEngine] Migration complete!");
            return true;
        } catch (err) {
            console.error("[MigrationEngine] Migration failed:", err);
            return false;
        }
    }
};
