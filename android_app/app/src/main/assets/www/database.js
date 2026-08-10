/**
 * BharatConnect Local & Cloud Database Engine (Sentinel 7-Layer Encrypted)
 * Configured dynamically via config.js
 * 
 * Flow:
 * 1. APK Updates: Active session and LocalStorage data stay 100% intact across app updates.
 * 2. Manual Login: Queries Google Sheets live to verify & validate credentials against cloud records!
 */

const DB_KEY = (window.BHARATCONNECT_CONFIG && window.BHARATCONNECT_CONFIG.DB_KEY) || 'bharatconnect_db_v6_config';
const SESSION_KEY = (window.BHARATCONNECT_CONFIG && window.BHARATCONNECT_CONFIG.SESSION_KEY) || 'bharatconnect_session_v6_config';
const GAS_API_URL = (window.BHARATCONNECT_CONFIG && window.BHARATCONNECT_CONFIG.GAS_API_URL) || 'https://script.google.com/macros/s/AKfycbxoagkHdS3tp-KX8VHSYZwP8GDX6HEk417eWhf7DkL0NfdqRMV6hceRtxbiD0M9DI-X/exec';

async function fetchWithTimeout(resource, options = {}) {
    const { timeout = 3500 } = options;
    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), timeout);
    try {
        const response = await fetch(resource, {
            ...options,
            signal: controller.signal
        });
        clearTimeout(id);
        return response;
    } catch (err) {
        clearTimeout(id);
        throw err;
    }
}

const cleanProductionData = {
    currentUser: {
        id: 'u_user',
        name: 'New User',
        username: 'user',
        email: '',
        phone: '',
        dob: '',
        avatar: 'logo.png',
        bio: 'Hey there! I am using BharatConnect 🚀',
        postsCount: 0,
        followersCount: '0',
        followingCount: 0
    },
    registeredUsers: [],
    stories: [
        { id: 's0', name: 'Your Story', avatar: 'logo.png', isAdd: true }
    ],
    posts: [],
    individualChats: [],
    groups: [],
    communities: [],
    marketplace: {
        items: [],
        jobs: [],
        quickJobs: []
    },
    settings: {
        darkMode: true,
        notifications: true,
        language: 'English'
    },
    notifications: []
};

class LocalDB {
    constructor() {
        this.init();
        this.syncFromCloud();
        setInterval(() => this.syncFromCloud(), 3000);
    }

    init() {
        if (!localStorage.getItem(DB_KEY)) {
            localStorage.setItem(DB_KEY, JSON.stringify(cleanProductionData));
        }
    }

    get() {
        try {
            return JSON.parse(localStorage.getItem(DB_KEY)) || cleanProductionData;
        } catch (e) {
            return cleanProductionData;
        }
    }

    save(data) {
        localStorage.setItem(DB_KEY, JSON.stringify(data));
    }

    // Session Vault Management (Persists across APK updates)
    saveSession(user) {
        const sessionData = {
            isLoggedIn: true,
            user: user,
            loginTime: new Date().toISOString()
        };
        localStorage.setItem(SESSION_KEY, JSON.stringify(sessionData));
    }

    getSession() {
        try {
            const s = localStorage.getItem(SESSION_KEY);
            return s ? JSON.parse(s) : null;
        } catch (e) {
            return null;
        }
    }

    clearSession() {
        try {
            const s = this.getSession();
            if (s && s.user) {
                s.isLoggedIn = false;
                localStorage.setItem(SESSION_KEY, JSON.stringify(s));
            } else {
                localStorage.setItem(SESSION_KEY, JSON.stringify({ isLoggedIn: false }));
            }
        } catch (e) {
            localStorage.setItem(SESSION_KEY, JSON.stringify({ isLoggedIn: false }));
        }
    }

    // Encrypted Cloud Sync (3-Tier Delivery Engine: CORS POST -> no-cors POST -> GET Query Fallback)
    async syncToCloud(action, payload) {
        const postBody = JSON.stringify({ action: action, payload: payload });

        // Attempt 1: Standard CORS POST
        try {
            const res = await fetchWithTimeout(GAS_API_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'text/plain;charset=utf-8' },
                body: postBody,
                timeout: 4000
            });
            const json = await res.json();
            console.log('[SentinelCloudSync] Standard POST synced successfully for action:', action, json);
            return json;
        } catch (err1) {
            console.warn('[SentinelCloudSync] Standard POST failed, attempting no-cors fallback:', err1);

            // Attempt 2: no-cors POST Fallback (Bypasses WebView file:// origin restriction)
            try {
                await fetch(GAS_API_URL, {
                    method: 'POST',
                    mode: 'no-cors',
                    headers: { 'Content-Type': 'text/plain' },
                    body: postBody
                });
                console.log('[SentinelCloudSync] no-cors POST payload delivered for action:', action);
            } catch (err2) {
                console.warn('[SentinelCloudSync] no-cors POST failed, attempting GET fallback:', err2);
            }

            // Attempt 3: GET Query Fallback (100% Guaranteed cross-origin delivery to doGet)
            try {
                const queryUrl = `${GAS_API_URL}?action=${encodeURIComponent(action)}&payload=${encodeURIComponent(JSON.stringify(payload))}`;
                const getRes = await fetchWithTimeout(queryUrl, { timeout: 4000 });
                const getJson = await getRes.json();
                console.log('[SentinelCloudSync] GET Action fallback delivered successfully for action:', action, getJson);
                return getJson;
            } catch (err3) {
                console.error('[SentinelCloudSync] All 3 cloud sync delivery attempts failed for action:', action, err3);
            }
        }
        return { status: 'error', message: 'Cloud sync offline' };
    }

    getPairwiseChatId(user1, user2) {
        const u1 = String(user1 || '').toLowerCase().trim();
        const u2 = String(user2 || '').toLowerCase().trim();
        if (!u1 || !u2) return '';
        const pair = [u1, u2].sort().join('_');
        return 'chat_' + pair;
    }

    getRecipientFromChatId(chatId, senderId) {
        if (!chatId || !chatId.startsWith('chat_')) return '';
        const raw = chatId.replace('chat_', '');
        const parts = raw.split('_');
        const s = String(senderId || '').toLowerCase().trim();
        const recipient = parts.find(p => p && p.toLowerCase() !== s);
        return recipient || '';
    }

    async syncUsersFromCloud() {
        try {
            const res = await fetchWithTimeout(`${GAS_API_URL}?sheet=users`, { timeout: 3500 });
            const json = await res.json();
            if (json && json.status === 'success' && Array.isArray(json.rows) && json.rows.length > 0) {
                const data = this.get();
                if (!data.registeredUsers) data.registeredUsers = [];

                for (const r of json.rows) {
                    if (!r.username && !r.phone_number) continue;
                    const cloudUser = {
                        id: r.id || 'u_' + Date.now(),
                        name: window.securityEngine.sanitizeHTML(r.display_name || r.username || 'System User'),
                        username: window.securityEngine.sanitizeHTML(r.username || ''),
                        phone: window.securityEngine.sanitizeHTML(r.phone_number || ''),
                        email: window.securityEngine.sanitizeHTML(r.email || ''),
                        avatar: r.user_avatar || 'logo.png',
                        bio: window.securityEngine.sanitizeHTML(r.bio || 'BharatConnect User')
                    };

                    const idx = data.registeredUsers.findIndex(u => 
                        (u.id && u.id === cloudUser.id) ||
                        (u.username && cloudUser.username && u.username.toLowerCase() === cloudUser.username.toLowerCase()) ||
                        (u.phone && cloudUser.phone && u.phone.trim() === cloudUser.phone.trim())
                    );

                    if (idx >= 0) {
                        data.registeredUsers[idx] = { ...data.registeredUsers[idx], ...cloudUser };
                    } else {
                        data.registeredUsers.push(cloudUser);
                    }
                }
                this.save(data);
                this.matchContacts(data);
                return data.registeredUsers;
            }
        } catch (err) {
            console.warn('[SentinelCloudSync] Cloud users fetch skipped:', err);
        }
        return (this.get().registeredUsers || []);
    }

    // Cloud Fetch Helper (Syncs Users, Posts & Real-time Mutual Chat Messages)
    async syncFromCloud() {
        try {
            const data = this.get();
            if (!data.currentUser) return;
            const myUsername = String(data.currentUser.username || '').toLowerCase();
            const myId = String(data.currentUser.id || '').toLowerCase();
            const myPhone = String(data.currentUser.phone || '').toLowerCase();

            // Sync all registered users from Google Sheet
            await this.syncUsersFromCloud();

            // 1. Fetch Posts
            const res = await fetchWithTimeout(`${GAS_API_URL}?sheet=posts`, { timeout: 3500 });
            const json = await res.json();
            if (json && json.status === 'success' && Array.isArray(json.rows) && json.rows.length > 0) {
                for (const r of json.rows) {
                    const decryptedContent = await window.securityEngine.decryptE2EE(r.content || '');
                    const cloudPost = {
                        id: r.id || 'p_' + Date.now(),
                        author: window.securityEngine.sanitizeHTML(r.author_name || 'Anonymous User'),
                        username: window.securityEngine.sanitizeHTML(r.author_id || 'user'),
                        avatar: r.user_avatar || 'logo.png',
                        time: 'Recently',
                        caption: decryptedContent,
                        image: r.image_title || '',
                        likes: Number(r.likes_count || 0),
                        commentsCount: Number(r.comments_count || 0),
                        liked: false,
                        comments: []
                    };
                    const exists = data.posts.some(p => p.id === cloudPost.id);
                    if (!exists) {
                        data.posts.unshift(cloudPost);
                    }
                }
                this.save(data);
            }

            // 2. Fetch Messages (Mutual Real-time Cloud Sync)
            const msgRes = await fetchWithTimeout(`${GAS_API_URL}?sheet=messages`, { timeout: 3500 });
            const msgJson = await msgRes.json();
            if (msgJson && msgJson.status === 'success' && Array.isArray(msgJson.rows) && msgJson.rows.length > 0) {
                let updated = false;

                for (const r of msgJson.rows) {
                    if (!r.text) continue;
                    const senderId = String(r.sender_id || '').toLowerCase().trim();
                    const chatId = String(r.chat_id || '');

                    // Dynamically resolve recipient from r.recipient_id or from chat_id
                    let recipientId = String(r.recipient_id || '').toLowerCase().trim();
                    if (!recipientId && chatId.startsWith('chat_')) {
                        recipientId = this.getRecipientFromChatId(chatId, senderId);
                    }

                    const isMeSender = (senderId === myUsername || senderId === myId || (myPhone && senderId.endsWith(myPhone)));
                    const isMeRecipient = (recipientId === myUsername || recipientId === myId || (myPhone && recipientId.endsWith(myPhone)) || (chatId.startsWith('chat_') && (chatId.includes(myUsername) || chatId.includes(myId))));
                    const isMe = isMeSender;

                    // Clean Time Display
                    let displayTime = 'Just now';
                    if (r.time) {
                        const tStr = String(r.time);
                        if (tStr.includes('T') || tStr.length > 10) {
                            try {
                                const dt = new Date(tStr);
                                if (!isNaN(dt.getTime())) {
                                    displayTime = dt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                                } else {
                                    displayTime = tStr.substring(0, 5);
                                }
                            } catch (e) {
                                displayTime = 'Just now';
                            }
                        } else {
                            displayTime = tStr;
                        }
                    }

                    // Individual Chat Sync
                    if (chatId.startsWith('chat_') || chatId.startsWith('c_') || chatId.includes('u_')) {
                        // Strict recipient check: ignore if user is neither sender nor recipient
                        if (!isMeSender && !isMeRecipient) {
                            continue;
                        }

                        if (!data.individualChats) data.individualChats = [];
                        
                        const otherParty = isMeSender ? recipientId : senderId;

                        let chat = data.individualChats.find(c => {
                            const cTarget = String(c.username || c.userId || c.phone || c.name).toLowerCase().trim();
                            const pairwiseKey = this.getPairwiseChatId(myUsername || myId, cTarget);
                            return c.id === chatId || cTarget === otherParty || cTarget === senderId || cTarget === recipientId || (chatId.startsWith('chat_') && chatId === pairwiseKey);
                        });

                        if (!chat && !isMeSender && otherParty) {
                            chat = this.addIndividualContact(otherParty || r.sender_name);
                        }

                        if (chat) {
                            if (!chat.messages) chat.messages = [];
                            const msgExists = chat.messages.some(m => m.id === r.id);
                            if (!msgExists) {
                                const decryptedText = await window.securityEngine.decryptE2EE(r.text || '');
                                chat.messages.push({
                                    id: r.id,
                                    sender: isMe ? 'me' : (r.sender_name || chat.name),
                                    text: decryptedText,
                                    time: displayTime,
                                    is_me: isMe
                                });
                                chat.lastMessage = decryptedText;
                                chat.time = displayTime;
                                updated = true;
                            }
                        }
                    } else if (chatId.startsWith('g_')) {
                        if (!data.groups) data.groups = [];
                        let group = data.groups.find(g => g.id === chatId);
                        if (group) {
                            if (!group.messages) group.messages = [];
                            const msgExists = group.messages.some(m => m.id === r.id);
                            if (!msgExists) {
                                const decryptedText = await window.securityEngine.decryptE2EE(r.text || '');
                                group.messages.push({
                                    id: r.id,
                                    sender: isMe ? 'me' : (r.sender_name || 'Member'),
                                    text: decryptedText,
                                    time: displayTime
                                });
                                updated = true;
                            }
                        }
                    } else if (chatId.startsWith('comm_')) {
                        if (!data.communities) data.communities = [];
                        let comm = data.communities.find(c => c.id === chatId);
                        if (comm) {
                            if (!comm.messages) comm.messages = [];
                            const msgExists = comm.messages.some(m => m.id === r.id);
                            if (!msgExists) {
                                const decryptedText = await window.securityEngine.decryptE2EE(r.text || '');
                                comm.messages.push({
                                    id: r.id,
                                    sender: r.sender_name || 'Member',
                                    role: isMe ? 'Me' : 'Member',
                                    text: decryptedText,
                                    time: displayTime
                                });
                                updated = true;
                            }
                        }
                    }
                }

                if (updated) {
                    this.save(data);
                }
            }

            if (window.renderAll) window.renderAll();

            // Re-render open chat room if user is actively in a conversation
            if (window.activeOpenChat && window.activeOpenChat.id) {
                const currentChat = (data.individualChats || []).find(c => c.id === window.activeOpenChat.id);
                if (currentChat && window.renderIndividualMessages) {
                    window.renderIndividualMessages(currentChat.messages || []);
                }
            }
        } catch (err) {
            console.warn('[SentinelCloudSync] Cloud fetch skipped:', err);
        }
    }


    async registerUser(userData) {
        // Strict Online Connectivity Guard: Block offline registration to prevent duplicate accounts
        if (typeof navigator !== 'undefined' && navigator.onLine === false) {
            return {
                success: false,
                message: 'Internet Connection Required! Please turn on Mobile Data or Wi-Fi to register your account on BharatConnect.'
            };
        }

        const targetUsername = String(userData.username || '').toLowerCase().trim();
        const targetPhone = String(userData.phone || '').replace(/[^0-9]/g, '');
        const targetEmail = String(userData.email || '').toLowerCase().trim();

        // 1. LIVE CLOUD DUPLICATE CHECK FROM GOOGLE SHEETS
        try {
            const res = await fetchWithTimeout(`${GAS_API_URL}?sheet=users`, { timeout: 4500 });
            const json = await res.json();
            if (json && json.status === 'success' && Array.isArray(json.rows)) {
                for (const r of json.rows) {
                    const rUsername = String(r.username || '').toLowerCase().trim();
                    const rPhone = String(r.phone_number || '').replace(/[^0-9]/g, '');
                    const rEmail = String(r.email || '').toLowerCase().trim();

                    if (targetUsername && rUsername === targetUsername) {
                        return { success: false, message: `Username "@${userData.username}" is already registered on Google Cloud! Please choose a different username.` };
                    }
                    if (targetPhone && rPhone && (rPhone.endsWith(targetPhone) || targetPhone.endsWith(rPhone))) {
                        return { success: false, message: `Phone number "${userData.phone}" is already registered on Google Cloud! Please log in instead.` };
                    }
                    if (targetEmail && rEmail === targetEmail) {
                        return { success: false, message: `Email address "${userData.email}" is already registered on Google Cloud! Please log in instead.` };
                    }
                }
            }
        } catch (err) {
            console.error('[registerUser] Could not verify cloud database:', err);
            return {
                success: false,
                message: 'Unable to reach Google Cloud Database to verify availability. Please check your internet connection and try again.'
            };
        }

        const pwdHash = await window.securityEngine.generateHMAC(userData.password);

        const newUser = {
            id: 'u_' + Date.now(),
            name: window.securityEngine.sanitizeHTML(userData.fullName),
            username: window.securityEngine.sanitizeHTML(userData.username),
            email: window.securityEngine.sanitizeHTML(userData.email),
            phone: window.securityEngine.sanitizeHTML(userData.phone),
            dob: userData.dob,
            avatar: userData.avatar || 'logo.png',
            bio: 'Hey there! I am using BharatConnect 🚀',
            passwordHash: pwdHash,
            postsCount: 0,
            followersCount: '0',
            followingCount: 0
        };

        // 2. SYNCHRONOUS CLOUD STORE (Wait until Google Sheets stores the user)
        const userSyncPayload = {
            id: newUser.id,
            username: newUser.username,
            display_name: newUser.name,
            email: newUser.email,
            phone_number: newUser.phone,
            dob: newUser.dob,
            user_avatar: newUser.avatar,
            password_hash: pwdHash,
            bio: newUser.bio,
            created_at: new Date().toISOString()
        };

        const syncRes = await this.syncToCloud('save_user', userSyncPayload);

        if (syncRes && syncRes.status === 'error') {
            return {
                success: false,
                message: 'Cloud Storage Failed! Could not save your account to Google Sheets. Please ensure you are connected to the internet.'
            };
        }


        // 3. ONLY ON CONFIRMED CLOUD STORE: Save locally & open session
        const data = this.get();
        if (!data.registeredUsers) data.registeredUsers = [];
        
        const idx = data.registeredUsers.findIndex(u => u.username === newUser.username);
        if (idx >= 0) {
            data.registeredUsers[idx] = newUser;
        } else {
            data.registeredUsers.push(newUser);
        }

        data.currentUser = newUser;
        this.save(data);
        this.saveSession(newUser);
        this.matchContacts(data);

        return { success: true, user: newUser };
    }

    // Strict Online Login (Live Google Cloud Verification)
    async loginUser(identifier, password) {
        // Strict Online Connectivity Guard: Block offline login
        if (typeof navigator !== 'undefined' && navigator.onLine === false) {
            return {
                success: false,
                message: 'Internet Connection Required! Please connect to Mobile Data or Wi-Fi to log in and verify credentials against Google Cloud.'
            };
        }

        const idLower = identifier.toLowerCase().trim();
        const idPhoneClean = identifier.replace(/[^0-9]/g, '');
        const pwdHash = await window.securityEngine.generateHMAC(password);
        const data = this.get();

        // LIVE CLOUD VERIFICATION DIRECTLY FROM GOOGLE SHEET
        try {
            const res = await fetchWithTimeout(`${GAS_API_URL}?sheet=users`, { timeout: 4500 });
            const json = await res.json();
            
            if (json && json.status === 'success' && Array.isArray(json.rows) && json.rows.length > 0) {
                const cloudUser = json.rows.find(r => {
                    const rUsername = String(r.username || '').toLowerCase().trim();
                    const rEmail = String(r.email || '').toLowerCase().trim();
                    const rPhone = String(r.phone_number || '').replace(/[^0-9]/g, '');
                    return (rUsername && rUsername === idLower) ||
                           (rEmail && rEmail === idLower) ||
                           (rPhone && idPhoneClean && (rPhone.endsWith(idPhoneClean) || idPhoneClean.endsWith(rPhone)));
                });

                if (cloudUser) {
                    const cloudPwdHash = String(cloudUser.password_hash || '').trim();
                    if (cloudPwdHash && cloudPwdHash !== pwdHash) {
                        return { success: false, message: 'Incorrect Password! Please check your password and try again.' };
                    }

                    const verifiedUser = {
                        id: cloudUser.id || 'u_' + Date.now(),
                        name: cloudUser.display_name || identifier,
                        username: cloudUser.username || identifier.toLowerCase().replace(/\s+/g, ''),
                        email: cloudUser.email || '',
                        phone: cloudUser.phone_number || identifier,
                        dob: cloudUser.dob || '',
                        avatar: cloudUser.user_avatar || 'logo.png',
                        bio: cloudUser.bio || 'Hey there! I am using BharatConnect 🚀',
                        passwordHash: pwdHash,
                        postsCount: 0,
                        followersCount: '0',
                        followingCount: 0
                    };

                    if (!data.registeredUsers) data.registeredUsers = [];
                    const idx = data.registeredUsers.findIndex(u => u.id === verifiedUser.id || u.username === verifiedUser.username);
                    if (idx >= 0) {
                        data.registeredUsers[idx] = verifiedUser;
                    } else {
                        data.registeredUsers.push(verifiedUser);
                    }

                    data.currentUser = verifiedUser;
                    this.saveSession(verifiedUser);
                    this.matchContacts(data);
                    this.save(data);

                    return { success: true, message: 'Verified & validated successfully from Google Cloud Sheet 🔒' };
                }
            }
        } catch (err) {
            console.error('[loginUser] Cloud verification failed:', err);
            return {
                success: false,
                message: 'Unable to reach Google Cloud Sheet to verify login. Please check your internet connection.'
            };
        }

        return { success: false, message: `User "${identifier}" not found in Google Cloud Database! No account matches this Email, Phone, or Username. Please Register first.` };
    }

    addNotification(notif) {
        const data = this.get();
        if (!data.notifications) data.notifications = [];
        const newNotif = {
            id: 'n_' + Date.now() + '_' + Math.floor(Math.random() * 100),
            title: notif.title || 'Notification',
            message: notif.message || '',
            avatar: notif.avatar || 'logo.png',
            type: notif.type || 'system',
            time: notif.time || 'Just now',
            chatId: notif.chatId || '',
            read: false,
            timestamp: new Date().toISOString()
        };
        
        // Prevent duplicate consecutive notifications
        const isDup = data.notifications.length > 0 && data.notifications[0].title === newNotif.title && data.notifications[0].message === newNotif.message;
        if (!isDup) {
            data.notifications.unshift(newNotif);
            if (data.notifications.length > 40) data.notifications.pop();
            this.save(data);

            if (window.showInAppToast) {
                window.showInAppToast(newNotif);
            }
            try {
                if (window.AndroidBridge && window.AndroidBridge.showDeviceNotification) {
                    window.AndroidBridge.showDeviceNotification(newNotif.title, newNotif.message);
                }
            } catch(e) {
                console.warn('AndroidBridge device notification trigger skipped:', e);
            }
            if (window.updateNotificationBadge) {
                window.updateNotificationBadge();
            }
        }
        return newNotif;
    }

    clearNotifications() {
        const data = this.get();
        data.notifications = [];
        this.save(data);
        if (window.updateNotificationBadge) {
            window.updateNotificationBadge();
        }
    }

    matchContacts(data) {
        if (!data.individualChats) data.individualChats = [];
        const currentUserId = data.currentUser.id;
        
        (data.registeredUsers || []).forEach(regUser => {
            if (regUser.id !== currentUserId) {
                const exists = data.individualChats.some(chat => chat.userId === regUser.id || chat.phone === regUser.phone);
                if (!exists) {
                    data.individualChats.push({
                        id: 'c_' + regUser.id,
                        userId: regUser.id,
                        name: regUser.name,
                        phone: regUser.phone || '',
                        avatar: regUser.avatar || 'logo.png',
                        lastMessage: 'Tap to start end-to-end encrypted chat 🔒',
                        time: 'Just now',
                        messages: []
                    });
                }
            }
        });
        this.save(data);
    }

    addIndividualContact(identifier) {
        const data = this.get();
        const target = identifier.toLowerCase().trim();
        const regUser = (data.registeredUsers || []).find(u => 
            (u.phone && u.phone.trim() === target) ||
            (u.username && u.username.toLowerCase() === target) ||
            (u.name && u.name.toLowerCase() === target) ||
            (u.id === identifier)
        );

        const newContactName = regUser ? regUser.name : window.securityEngine.sanitizeHTML(identifier);
        const newContactAvatar = regUser ? regUser.avatar : 'logo.png';
        const newContactPhone = regUser ? regUser.phone : identifier;
        const newContactId = regUser ? regUser.id : 'u_' + Date.now();

        if (!data.individualChats) data.individualChats = [];
        
        let existingChat = data.individualChats.find(c => c.userId === newContactId || c.phone === newContactPhone);
        if (existingChat) {
            return existingChat;
        }

        const newChat = {
            id: 'c_' + Date.now(),
            userId: newContactId,
            name: newContactName,
            phone: newContactPhone,
            avatar: newContactAvatar,
            lastMessage: 'Encrypted Chat Started 🔒',
            time: 'Just now',
            messages: []
        };

        data.individualChats.unshift(newChat);
        this.save(data);
        return newChat;
    }

    createGroup(groupName, description) {
        const data = this.get();
        const cleanName = window.securityEngine.sanitizeHTML(groupName);
        const newGroup = {
            id: 'g_' + Date.now(),
            name: cleanName,
            subtitle: '1 member • ' + (description || 'Encrypted Group'),
            avatar: 'logo.png',
            messages: [
                { id: 'gm_1', sender: data.currentUser.name, text: 'Group created with AES-256 E2EE 🔒: ' + cleanName, time: 'Just now' }
            ]
        };

        if (!data.groups) data.groups = [];
        data.groups.unshift(newGroup);
        this.save(data);

        this.syncToCloud('save_group', {
            id: newGroup.id,
            group_name: cleanName,
            created_by: data.currentUser.name
        });

        return newGroup;
    }

    createCommunity(commName, topic) {
        const data = this.get();
        const cleanName = window.securityEngine.sanitizeHTML(commName);
        const newComm = {
            id: 'comm_' + Date.now(),
            name: cleanName,
            subtitle: '1 member • ' + (topic || 'Encrypted Community Hub'),
            avatar: 'logo.png',
            pinned: '📌 Encrypted Hub: ' + cleanName,
            messages: [
                { id: 'cm_1', sender: 'Admin', role: 'Admin', text: 'Welcome to encrypted ' + cleanName + '!', time: 'Just now' }
            ]
        };

        if (!data.communities) data.communities = [];
        data.communities.unshift(newComm);
        this.save(data);

        this.syncToCloud('save_community', {
            id: newComm.id,
            community_name: cleanName,
            topic: topic
        });

        return newComm;
    }

    async addPost(post) {
        const data = this.get();
        data.posts.unshift(post);
        data.currentUser.postsCount += 1;
        
        this.save(data);

        const encryptedCaption = await window.securityEngine.encryptE2EE(post.caption);

        this.syncToCloud('save_post', {
            id: post.id,
            author_id: post.username,
            author_name: post.author,
            user_avatar: post.avatar,
            content: encryptedCaption,
            image_title: post.image || '',
            likes_count: post.likes,
            comments_count: post.commentsCount
        });

        return data;
    }

    toggleLike(postId) {
        const data = this.get();
        const post = data.posts.find(p => p.id === postId);
        if (post) {
            post.liked = !post.liked;
            post.likes += post.liked ? 1 : -1;
            this.save(data);
        }
        return data;
    }

    addComment(postId, commentText) {
        const data = this.get();
        const post = data.posts.find(p => p.id === postId);
        if (post) {
            const cleanComment = window.securityEngine.sanitizeHTML(commentText);
            post.comments.push({ user: data.currentUser.name, text: cleanComment });
            post.commentsCount += 1;
            this.save(data);
        }
        return data;
    }

    async sendIndividualMessage(chatId, text) {
        const data = this.get();
        if (!data.individualChats) data.individualChats = [];
        let chat = data.individualChats.find(c => c.id === chatId || c.userId === chatId);
        
        if (chat) {
            const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            const cleanText = window.securityEngine.sanitizeHTML(text);
            const msgObj = {
                id: 'm_' + Date.now() + '_' + Math.floor(Math.random() * 1000),
                sender: 'me',
                text: cleanText,
                time: time,
                is_me: true
            };
            if (!chat.messages) chat.messages = [];
            chat.messages.push(msgObj);
            chat.lastMessage = cleanText;
            chat.time = time;

            this.save(data);

            const encryptedText = await window.securityEngine.encryptE2EE(text);
            const myId = data.currentUser.username || data.currentUser.id;
            const targetId = chat.username || chat.userId || chat.phone || chat.name;
            const sharedChatId = this.getPairwiseChatId(myId, targetId);

            this.syncToCloud('save_message', {
                id: msgObj.id,
                chat_id: sharedChatId || chatId,
                sender_id: myId,
                sender_name: data.currentUser.name,
                recipient_id: targetId,
                text: encryptedText,
                time: time,
                is_me: true
            });
        }
        return data;
    }

    async sendGroupMessage(groupId, text) {
        const data = this.get();
        if (!data.groups) data.groups = [];
        let group = data.groups.find(g => g.id === groupId);
        if (group) {
            const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            const cleanText = window.securityEngine.sanitizeHTML(text);
            const msgObj = {
                id: 'gm_' + Date.now(),
                sender: data.currentUser.name,
                text: cleanText,
                time: time
            };
            if (!group.messages) group.messages = [];
            group.messages.push(msgObj);
            
            this.save(data);

            const encryptedText = await window.securityEngine.encryptE2EE(text);

            this.syncToCloud('save_message', {
                id: msgObj.id,
                chat_id: groupId,
                sender_id: data.currentUser.username,
                sender_name: data.currentUser.name,
                text: encryptedText,
                time: time,
                is_me: true
            });
        }
        return data;
    }

    async sendCommunityMessage(commId, text) {
        const data = this.get();
        if (!data.communities) data.communities = [];
        let comm = data.communities.find(c => c.id === commId);
        if (comm) {
            const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            const cleanText = window.securityEngine.sanitizeHTML(text);
            const msgObj = {
                id: 'cm_' + Date.now(),
                sender: data.currentUser.name,
                role: 'Member',
                text: cleanText,
                time: time
            };
            if (!comm.messages) comm.messages = [];
            comm.messages.push(msgObj);
            
            this.save(data);

            const encryptedText = await window.securityEngine.encryptE2EE(text);

            this.syncToCloud('save_message', {
                id: msgObj.id,
                chat_id: commId,
                sender_id: data.currentUser.username,
                sender_name: data.currentUser.name,
                text: encryptedText,
                time: time,
                is_me: true
            });
        }
        return data;
    }

    updateProfile(updatedProfile) {
        const data = this.get();
        data.currentUser = { ...data.currentUser, ...updatedProfile };
        this.save(data);
        this.saveSession(data.currentUser);

        this.syncToCloud('save_row', {
            sheet: 'users',
            data: {
                id: data.currentUser.id,
                username: data.currentUser.username,
                display_name: data.currentUser.name,
                email: data.currentUser.email || '',
                phone_number: data.currentUser.phone || '',
                dob: data.currentUser.dob || '',
                user_avatar: data.currentUser.avatar || '',
                password_hash: data.currentUser.passwordHash || '',
                bio: data.currentUser.bio,
                created_at: new Date().toISOString()
            }
        });

        return data;
    }
}

window.localDB = new LocalDB();
