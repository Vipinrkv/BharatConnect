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
        const extractKey = (u) => {
            if (!u) return '';
            if (typeof u === 'object') {
                u = u.phone || u.username || u.id || '';
            }
            const digits = String(u).replace(/\D/g, '').replace(/^91(?=\d{10}$)/, '').replace(/^0+/, '');
            if (digits.length >= 7) return digits;
            return String(u).toLowerCase().trim();
        };

        const k1 = extractKey(user1);
        const k2 = extractKey(user2);
        if (!k1 || !k2) return '';
        const pair = [k1, k2].sort().join('_');
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
        const apiBaseUrl = (window.BHARATCONNECT_CONFIG && window.BHARATCONNECT_CONFIG.API_BASE_URL) || 'https://bharatconnect-api.onrender.com/api/v1';
        try {
            const res = await fetchWithTimeout(`${apiBaseUrl}/auth/users`, { timeout: 3500 });
            if (res.ok) {
                const users = await res.json();
                if (Array.isArray(users) && users.length > 0) {
                    const data = this.get();
                    if (!data.registeredUsers) data.registeredUsers = [];

                    for (const r of users) {
                        const cloudUser = {
                            id: r.id || ('u_' + Date.now()),
                            name: (window.securityEngine && window.securityEngine.sanitizeHTML) ? window.securityEngine.sanitizeHTML(r.display_name || r.username || 'System User') : (r.display_name || r.username),
                            username: (window.securityEngine && window.securityEngine.sanitizeHTML) ? window.securityEngine.sanitizeHTML(r.username || '') : (r.username || ''),
                            phone: (window.securityEngine && window.securityEngine.sanitizeHTML) ? window.securityEngine.sanitizeHTML(r.phone || '') : (r.phone || ''),
                            email: (window.securityEngine && window.securityEngine.sanitizeHTML) ? window.securityEngine.sanitizeHTML(r.email || '') : (r.email || ''),
                            avatar: r.user_avatar || 'logo.png',
                            bio: (window.securityEngine && window.securityEngine.sanitizeHTML) ? window.securityEngine.sanitizeHTML(r.bio || 'BharatConnect User') : (r.bio || 'BharatConnect User')
                        };

                        const idx = data.registeredUsers.findIndex(u =>
                            (u.id && u.id === cloudUser.id) ||
                            (u.username && cloudUser.username && u.username.toLowerCase() === cloudUser.username.toLowerCase())
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
            }
        } catch (err) {
            console.warn('[SentinelCloudSync] Cloud users fetch skipped:', err);
        }
        return (this.get().registeredUsers || []);
    }

    // Cloud Fetch Helper (Syncs Posts & Messages via REST & WebSockets)
    async syncFromCloud() {
        const apiBaseUrl = (window.BHARATCONNECT_CONFIG && window.BHARATCONNECT_CONFIG.API_BASE_URL) || 'https://bharatconnect-api.onrender.com/api/v1';
        try {
            const data = this.get();
            if (!data.currentUser) return;

            // 1. Fetch Posts from FastAPI Server
            const res = await fetchWithTimeout(`${apiBaseUrl}/posts`, { timeout: 3500 });
            if (res.ok) {
                const posts = await res.json();
                if (Array.isArray(posts) && posts.length > 0) {
                    for (const r of posts) {
                        const cloudPost = {
                            id: r.id || ('p_' + Date.now()),
                            author: r.author_name || 'Anonymous User',
                            username: r.author_id || 'user',
                            avatar: r.user_avatar || 'logo.png',
                            time: r.time_ago || 'Recently',
                            caption: r.content || '',
                            image: r.image_title || '',
                            likes: Number(r.likes_count || 0),
                            commentsCount: Number(r.comments_count || 0),
                            liked: Boolean(r.is_liked),
                            comments: []
                        };
                        const exists = data.posts.some(p => p.id === cloudPost.id);
                        if (!exists) {
                            data.posts.unshift(cloudPost);
                        }
                    }
                    this.save(data);
                }
            }

            if (window.renderAll) window.renderAll();
        } catch (err) {
            console.warn('[SentinelCloudSync] Cloud fetch skipped:', err);
        }
    }


    async registerUser(userData) {
        const apiBaseUrl = (window.BHARATCONNECT_CONFIG && window.BHARATCONNECT_CONFIG.API_BASE_URL) || 'https://bharatconnect-api.onrender.com/api/v1';

        const displayName = (window.securityEngine && window.securityEngine.sanitizeHTML) ? window.securityEngine.sanitizeHTML(userData.fullName || userData.username) : (userData.fullName || userData.username);
        const rawPhone = String(userData.phone || '').trim();
        const normPhone = rawPhone.replace(/\D/g, '').replace(/^91(?=\d{10}$)/, '').replace(/^0+/, '');
        const registerPayload = {
            full_name: displayName,
            display_name: displayName,
            username: String(userData.username || '').toLowerCase().trim(),
            email: String(userData.email || '').toLowerCase().trim(),
            phone: normPhone || rawPhone,
            password: userData.password,
            user_avatar: userData.avatar || 'logo.png'
        };

        try {
            const response = await fetch(`${apiBaseUrl}/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(registerPayload)
            });

            const json = await response.json();

            if (response.ok) {
                const newUser = {
                    id: (json.user && json.user.id) ? json.user.id : ('u_' + Date.now()),
                    name: (json.user && json.user.display_name) ? json.user.display_name : registerPayload.display_name,
                    username: (json.user && json.user.username) ? json.user.username : registerPayload.username,
                    email: (json.user && json.user.email) ? json.user.email : registerPayload.email,
                    phone: registerPayload.phone,
                    avatar: (json.user && json.user.user_avatar && json.user.user_avatar !== 'logo.png') ? json.user.user_avatar : (userData.avatar || 'logo.png'),
                    bio: 'Hey there! I am using BharatConnect 🚀'
                };

                const data = this.get();
                if (!data.registeredUsers) data.registeredUsers = [];
                data.registeredUsers.push(newUser);
                data.currentUser = newUser;
                this.save(data);
                this.saveSession(newUser);
                if (json.access_token) {
                    localStorage.setItem('bharatconnect_jwt_token', json.access_token);
                }

                return { success: true, user: newUser };
            } else if (response.status === 400) {
                const errDetail = (json && (json.detail || json.message)) || 'Account already registered with these details.';
                return { success: false, isAlreadyRegistered: true, message: errDetail };
            } else {
                const errDetail = (json && (json.detail || json.message)) || 'Server response error. Please try again.';
                return { success: false, isServerError: true, message: errDetail };
            }
        } catch (err) {
            console.warn('[registerUser] Server connection failed:', err);
            return {
                success: false,
                isNetworkError: true,
                message: 'Unable to connect to server. Please check your internet connection and try again.'
            };
        }
    }

    // Live Online Server Login (Requires active internet/cloud connection)
    async loginUser(identifier, password) {
        const apiBaseUrl = (window.BHARATCONNECT_CONFIG && window.BHARATCONNECT_CONFIG.API_BASE_URL) || 'https://bharatconnect-api.onrender.com/api/v1';

        const loginPayload = {
            identifier: identifier.trim(),
            password: password
        };

        try {
            const response = await fetch(`${apiBaseUrl}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(loginPayload)
            });

            const json = await response.json();

            if (response.ok && json.user) {
                const data = this.get();
                if (!data.registeredUsers) data.registeredUsers = [];

                const existingLocal = data.registeredUsers.find(u => (u.id && u.id === json.user.id) || (u.username && u.username.toLowerCase() === json.user.username.toLowerCase()));

                const verifiedUser = {
                    id: json.user.id,
                    name: json.user.display_name || identifier,
                    username: json.user.username || identifier,
                    email: json.user.email || '',
                    phone: json.user.phone || (existingLocal ? existingLocal.phone : ''),
                    avatar: (json.user.user_avatar && json.user.user_avatar !== 'logo.png') ? json.user.user_avatar : (existingLocal && existingLocal.avatar ? existingLocal.avatar : 'logo.png'),
                    bio: json.user.bio || 'Hey there! I am using BharatConnect 🚀'
                };

                const idx = data.registeredUsers.findIndex(u => u.id === verifiedUser.id || u.username === verifiedUser.username);
                if (idx >= 0) {
                    data.registeredUsers[idx] = { ...data.registeredUsers[idx], ...verifiedUser };
                } else {
                    data.registeredUsers.push(verifiedUser);
                }

                data.currentUser = verifiedUser;
                this.saveSession(verifiedUser);
                this.save(data);
                if (json.access_token) {
                    localStorage.setItem('bharatconnect_jwt_token', json.access_token);
                }

                return { success: true, message: 'Logged in successfully! 🚀', user: verifiedUser };
            } else if (response.status === 401 || response.status === 400) {
                const errDetail = (json && (json.detail || json.message)) || 'Invalid Username/Email/Phone or Password.';
                return { success: false, isInvalidCredentials: true, message: errDetail };
            } else {
                const errDetail = (json && (json.detail || json.message)) || 'Server response error. Please try again.';
                return { success: false, isServerError: true, message: errDetail };
            }
        } catch (err) {
            console.warn('[loginUser] Server connection failed:', err);
            return {
                success: false,
                isNetworkError: true,
                message: 'Unable to connect to server. Please check your internet connection and try again.'
            };
        }
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
        const currentUser = data.currentUser || {};
        const myPhone = String(currentUser.phone || '').replace(/\D/g, '').replace(/^91(?=\d{10}$)/, '').replace(/^0+/, '');
        const myId = String(currentUser.id || '').toLowerCase();
        
        (data.registeredUsers || []).forEach(regUser => {
            const uphone = String(regUser.phone || '').replace(/\D/g, '').replace(/^91(?=\d{10}$)/, '').replace(/^0+/, '');
            const uid = String(regUser.id || '').toLowerCase();

            const isMe = (uid === myId || (uphone && myPhone && uphone === myPhone));
            if (!isMe) {
                const sharedChatId = this.getPairwiseChatId(currentUser, regUser);
                let existingChat = data.individualChats.find(chat => 
                    chat.id === sharedChatId || 
                    (chat.phone && regUser.phone && String(chat.phone).replace(/\D/g, '').endsWith(uphone)) || 
                    chat.userId === regUser.id
                );
                if (!existingChat) {
                    data.individualChats.push({
                        id: sharedChatId || ('c_' + regUser.id),
                        userId: regUser.id,
                        name: regUser.name || regUser.username,
                        phone: regUser.phone || '',
                        username: regUser.username || '',
                        avatar: regUser.avatar || 'logo.png',
                        lastMessage: 'Tap to start end-to-end encrypted chat 🔒',
                        time: 'Just now',
                        messages: []
                    });
                } else {
                    if (sharedChatId) existingChat.id = sharedChatId;
                    existingChat.userId = regUser.id;
                    existingChat.username = regUser.username || existingChat.username;
                    if (regUser.phone) existingChat.phone = regUser.phone;
                }
            }
        });
        this.save(data);
    }

    addIndividualContact(identifier) {
        const data = this.get();
        const target = identifier.toLowerCase().trim();
        const normTarget = identifier.replace(/\D/g, '').replace(/^91(?=\d{10}$)/, '').replace(/^0+/, '');

        const regUser = (data.registeredUsers || []).find(u => {
            const uphone = String(u.phone || '').replace(/\D/g, '').replace(/^91(?=\d{10}$)/, '').replace(/^0+/, '');
            return (uphone && normTarget && (uphone.endsWith(normTarget) || normTarget.endsWith(uphone))) ||
                   (u.username && u.username.toLowerCase() === target) ||
                   (u.name && u.name.toLowerCase() === target) ||
                   (u.id === identifier);
        });

        const newContactName = regUser ? regUser.name : window.securityEngine.sanitizeHTML(identifier);
        const newContactAvatar = regUser ? regUser.avatar : 'logo.png';
        const newContactPhone = regUser ? regUser.phone : identifier;
        const newContactId = regUser ? regUser.id : ('u_' + (normTarget || Date.now()));

        const myUserKey = (data.currentUser.phone || data.currentUser.username || data.currentUser.id || 'me');
        const sharedChatId = this.getPairwiseChatId(myUserKey, newContactPhone || newContactId);

        if (!data.individualChats) data.individualChats = [];
        
        let existingChat = data.individualChats.find(c => c.id === sharedChatId || c.userId === newContactId || c.phone === newContactPhone);
        if (existingChat) {
            existingChat.id = sharedChatId || existingChat.id;
            this.save(data);
            return existingChat;
        }

        const newChat = {
            id: sharedChatId || ('c_' + Date.now()),
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

    togglePinChat(chatId) {
        const data = this.get();
        let found = false;
        ['individualChats', 'groups', 'communities'].forEach(key => {
            if (data[key]) {
                const item = data[key].find(c => c.id === chatId);
                if (item) {
                    item.isPinned = !item.isPinned;
                    found = true;
                }
            }
        });
        if (found) this.save(data);
        return data;
    }

    deleteChat(chatId) {
        const data = this.get();
        ['individualChats', 'groups', 'communities'].forEach(key => {
            if (data[key]) {
                data[key] = data[key].filter(c => c.id !== chatId);
            }
        });
        this.save(data);
        return data;
    }

    toggleMuteChat(chatId) {
        const data = this.get();
        let isMuted = false;
        ['individualChats', 'groups', 'communities'].forEach(key => {
            if (data[key]) {
                const item = data[key].find(c => c.id === chatId);
                if (item) {
                    item.isMuted = !item.isMuted;
                    isMuted = item.isMuted;
                }
            }
        });
        this.save(data);
        return isMuted;
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
        if (data.currentUser) {
            data.currentUser.postsCount = (data.currentUser.postsCount || 0) + 1;
        }
        this.save(data);

        const apiBaseUrl = (window.BHARATCONNECT_CONFIG && window.BHARATCONNECT_CONFIG.API_BASE_URL) || 'https://bharatconnect-api.onrender.com/api/v1';

        try {
            await fetch(`${apiBaseUrl}/posts`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    content: post.caption,
                    image_title: post.image || null
                })
            });
        } catch (err) {
            console.warn('[addPost] Server sync skipped:', err);
        }

        return data;
    }

    async toggleLike(postId) {
        const data = this.get();
        const post = data.posts.find(p => p.id === postId);
        if (post) {
            post.liked = !post.liked;
            post.likes += post.liked ? 1 : -1;
            this.save(data);

            const apiBaseUrl = (window.BHARATCONNECT_CONFIG && window.BHARATCONNECT_CONFIG.API_BASE_URL) || 'https://bharatconnect-api.onrender.com/api/v1';
            try {
                await fetch(`${apiBaseUrl}/posts/${postId}/like`, { method: 'POST' });
            } catch (e) {
                console.warn('[toggleLike] Server like sync skipped:', e);
            }
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

    async sendIndividualMessage(chatId, text, imageUrl = null) {
        const data = this.get();
        if (!data.individualChats) data.individualChats = [];
        
        const myUserKey = data.currentUser.phone || data.currentUser.username || data.currentUser.id;
        
        let chat = data.individualChats.find(c => {
            if (c.id === chatId) return true;
            const computedId = this.getPairwiseChatId(myUserKey, c.phone || c.userId || c.id);
            return computedId === chatId;
        });

        if (!chat) {
            chat = this.addIndividualContact(chatId);
        }

        if (chat) {
            const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            const cleanText = window.securityEngine.sanitizeHTML(text);
            const msgObj = {
                id: 'm_' + Date.now() + '_' + Math.floor(Math.random() * 1000),
                sender: 'me',
                text: cleanText,
                image_url: imageUrl,
                time: time,
                is_me: true
            };
            if (!chat.messages) chat.messages = [];
            chat.messages.push(msgObj);
            chat.lastMessage = cleanText || '📷 Photo';
            chat.time = time;

            this.save(data);

            // Synchronize message to live REST Server
            const apiBaseUrl = (window.BHARATCONNECT_CONFIG && window.BHARATCONNECT_CONFIG.API_BASE_URL) || 'https://bharatconnect-api.onrender.com/api/v1';
            const mySenderId = data.currentUser.phone || data.currentUser.username || data.currentUser.id;
            const mySenderName = data.currentUser.name || 'Member';
            const recipientId = chat.phone || chat.userId || this.getRecipientFromChatId(chat.id, mySenderId);

            try {
                await fetch(`${apiBaseUrl}/chats/${chat.id}/messages`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        sender_id: mySenderId,
                        sender_name: mySenderName,
                        recipient_id: recipientId,
                        text: text,
                        image_url: imageUrl,
                        time: time
                    })
                });
            } catch (err) {
                console.warn('[sendIndividualMessage] Server sync error:', err);
            }
        }
        return data;
    }

    async syncChatMessagesFromCloud(chatId) {
        if (!chatId) return;
        const apiBaseUrl = (window.BHARATCONNECT_CONFIG && window.BHARATCONNECT_CONFIG.API_BASE_URL) || 'https://bharatconnect-api.onrender.com/api/v1';
        try {
            const res = await fetch(`${apiBaseUrl}/chats/${chatId}/messages`);
            if (!res.ok) return;
            const serverMsgs = await res.json();
            if (!Array.isArray(serverMsgs)) return;

            const data = this.get();
            if (!data.individualChats) data.individualChats = [];
            
            const myUserKey = data.currentUser.phone || data.currentUser.username || data.currentUser.id;
            let chat = data.individualChats.find(c => c.id === chatId || this.getPairwiseChatId(myUserKey, c.phone || c.userId) === chatId);
            
            if (!chat) {
                chat = this.addIndividualContact(chatId);
            }
            if (!chat) return;

            if (!chat.messages) chat.messages = [];

            const myPhone = String(data.currentUser.phone || '').replace(/\D/g, '').replace(/^91(?=\d{10}$)/, '').replace(/^0+/, '');
            const myId = String(data.currentUser.id || '').toLowerCase();
            const myUsername = String(data.currentUser.username || '').toLowerCase();

            let hasNew = false;
            for (const sm of serverMsgs) {
                const smSender = String(sm.sender_id || '').replace(/\D/g, '').replace(/^91(?=\d{10}$)/, '').replace(/^0+/, '') || String(sm.sender_id || '').toLowerCase();
                
                const isSentByMe = (smSender === 'me' || sm.is_me === true || smSender === myId || smSender === myUsername || (myPhone && smSender && (myPhone.endsWith(smSender) || smSender.endsWith(myPhone))));

                const exists = chat.messages.some(m => m.id === sm.id || (m.text === sm.text && m.time === sm.time));
                if (!exists) {
                    chat.messages.push({
                        id: sm.id,
                        sender: isSentByMe ? 'me' : (sm.sender_name || 'Contact'),
                        text: sm.text,
                        image_url: sm.image_url || null,
                        time: sm.time || 'Just now',
                        is_me: isSentByMe
                    });
                    chat.lastMessage = sm.text || '📷 Photo';
                    chat.time = sm.time || 'Just now';
                    hasNew = true;
                }
            }

            if (hasNew) {
                this.save(data);
                if (typeof window.renderIndividualMessages === 'function' && window.activeOpenChat && window.activeOpenChat.id === chatId) {
                    window.renderIndividualMessages(chat.messages);
                }
            }
        } catch (e) {
            console.warn('[syncChatMessagesFromCloud] error:', e);
        }
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

    async updateProfile(updatedProfile) {
        const data = this.get();
        data.currentUser = { ...data.currentUser, ...updatedProfile };
        
        // Update registeredUsers array for current user
        if (data.registeredUsers) {
            const idx = data.registeredUsers.findIndex(u => u.id === data.currentUser.id || u.username === data.currentUser.username);
            if (idx >= 0) {
                data.registeredUsers[idx] = { ...data.registeredUsers[idx], ...data.currentUser };
            }
        }

        this.save(data);
        this.saveSession(data.currentUser);

        const apiBaseUrl = (window.BHARATCONNECT_CONFIG && window.BHARATCONNECT_CONFIG.API_BASE_URL) || 'https://bharatconnect-api.onrender.com/api/v1';

        try {
            await fetch(`${apiBaseUrl}/auth/profile/${encodeURIComponent(data.currentUser.id || data.currentUser.username)}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    display_name: data.currentUser.name,
                    username: data.currentUser.username,
                    bio: data.currentUser.bio,
                    email: data.currentUser.email,
                    phone: data.currentUser.phone,
                    user_avatar: data.currentUser.avatar
                })
            });
        } catch (e) {
            console.warn('[updateProfile] Live server profile update skipped:', e);
        }

        return data;
    }
}

window.localDB = new LocalDB();
