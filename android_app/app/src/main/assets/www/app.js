/**
 * BharatConnect Client Controller
 * 3-Tab Chat Engine, Contact Matching Modal, Profile Avatar Picker & Live Sync
 */

let currentActiveChatTab = 'individual';
let activeOpenChat = { type: null, id: null };
window.selectedAvatarBase64 = null;
let currentNotificationFilter = 'all';

/* ==========================================
 * IN-APP TOAST & NOTIFICATION CONTROLLER
 * ========================================== */

window.toastQueue = [];
window.isToastShowing = false;

function showInAppToast(notif) {
    if (!notif) return;
    window.toastQueue.push(notif);
    if (!window.isToastShowing) {
        processNextToast();
    }
}

function processNextToast() {
    if (window.toastQueue.length === 0) {
        window.isToastShowing = false;
        hideInAppToast();
        return;
    }

    const container = document.getElementById('in-app-toast-container');
    if (!container) return;

    window.isToastShowing = true;
    const notif = window.toastQueue.shift();

    const notifTitle = notif.title || 'New Notification';
    const notifMsg = notif.message || '';
    const notifAvatar = notif.avatar || 'logo.png';

    container.innerHTML = `
        <img src="${notifAvatar}" class="in-app-toast-avatar">
        <div class="in-app-toast-content">
            <div class="in-app-toast-title">${notifTitle}</div>
            <div class="in-app-toast-body">${notifMsg}</div>
        </div>
        <button class="in-app-toast-close" onclick="dismissCurrentToast()">×</button>
    `;

    if (notif.chatId) {
        container.onclick = function(e) {
            if (e.target.classList.contains('in-app-toast-close')) return;
            dismissCurrentToast();
            openIndividualChatRoom(notif.chatId);
        };
    } else {
        container.onclick = null;
    }

    // Trigger subtle haptic vibration if supported on device
    try {
        if (typeof navigator !== 'undefined' && navigator.vibrate) {
            navigator.vibrate([30, 40, 30]);
        }
    } catch(e) {}

    container.classList.add('show');
    clearTimeout(window.toastTimer);
    window.toastTimer = setTimeout(() => {
        dismissCurrentToast();
    }, 3800);
}

function dismissCurrentToast() {
    clearTimeout(window.toastTimer);
    const container = document.getElementById('in-app-toast-container');
    if (container) container.classList.remove('show');
    setTimeout(() => {
        processNextToast();
    }, 300);
}

function hideInAppToast() {
    const container = document.getElementById('in-app-toast-container');
    if (container) container.classList.remove('show');
}

function updateNotificationBadge() {
    const data = window.localDB.get();
    const badge = document.querySelector('#btn-notif-bell .badge');
    if (badge) {
        const notifs = data.notifications || [];
        const unreadCount = notifs.filter(n => !n.read).length;
        if (unreadCount > 0) {
            badge.style.display = 'block';
            badge.innerText = unreadCount > 9 ? '9+' : unreadCount;
        } else {
            badge.style.display = 'none';
        }
    }
}

function filterNotificationTab(type, btn) {
    currentNotificationFilter = type;
    const tabBtns = document.querySelectorAll('#notif-filter-tabs .tab-btn');
    tabBtns.forEach(b => b.classList.remove('active'));
    if (btn) btn.classList.add('active');
    renderNotifications();
}

function clearAllNotifications() {
    window.localDB.clearNotifications();
    renderNotifications();
}

function renderNotifications() {
    const container = document.getElementById('notifications-list-container');
    if (!container) return;

    const data = window.localDB.get();
    let notifs = data.notifications || [];

    notifs.forEach(n => n.read = true);
    window.localDB.save(data);
    updateNotificationBadge();

    if (currentNotificationFilter !== 'all') {
        notifs = notifs.filter(n => (n.type || 'system').toLowerCase() === currentNotificationFilter);
    }

    if (notifs.length === 0) {
        container.innerHTML = `
            <div style="text-align:center; padding:50px 20px; color:var(--text-muted);">
                <div style="font-size:48px; margin-bottom:12px; opacity:0.8;">🔔</div>
                <div style="font-size:16px; font-weight:700; color:white;">No Notifications</div>
                <div style="font-size:12px; margin-top:6px; color:var(--accent-lavender);">When you receive messages, likes, or updates, they will appear here!</div>
            </div>
        `;
        return;
    }

    container.innerHTML = notifs.map(n => {
        const title = n.title || 'Notification';
        const msg = n.message || '';
        const time = n.time || 'Just now';
        const avatar = n.avatar || 'logo.png';

        return `
            <div class="notif-card-item" onclick="${n.chatId ? `openIndividualChatRoom('${n.chatId}')` : ''}" style="${n.chatId ? 'cursor:pointer;' : ''}">
                <img src="${avatar}" class="notif-user-avatar">
                <div class="notif-info">
                    <div class="notif-text"><strong>${title}</strong> ${msg ? '• ' + msg : ''}</div>
                    <div class="notif-time">${time}</div>
                </div>
            </div>
        `;
    }).join('');
}

/* ==========================================
 * CUSTOM MODAL DIALOG ENGINE (Replacing JS prompt/alert)
 * ========================================== */

let currentPromptCallback = null;
let currentAlertCallback = null;

function showCustomPrompt(config) {
    const modal = document.getElementById('custom-prompt-modal');
    const titleEl = document.getElementById('custom-prompt-title');
    const subEl = document.getElementById('custom-prompt-subtitle');
    const fieldsContainer = document.getElementById('custom-prompt-fields');
    const submitBtn = document.getElementById('custom-prompt-submit-btn');

    if (!modal || !titleEl || !fieldsContainer) return;

    titleEl.innerText = config.title || 'Input Required';

    if (config.subtitle) {
        subEl.innerText = config.subtitle;
        subEl.style.display = 'block';
    } else {
        subEl.style.display = 'none';
    }

    if (submitBtn) {
        submitBtn.innerText = config.confirmText || 'Confirm';
    }

    fieldsContainer.innerHTML = '';

    (config.fields || []).forEach((field, index) => {
        const fieldWrapper = document.createElement('div');
        fieldWrapper.style.display = 'flex';
        fieldWrapper.style.flexDirection = 'column';
        fieldWrapper.style.gap = '4px';

        if (field.label) {
            const lbl = document.createElement('label');
            lbl.style.fontSize = '12px';
            lbl.style.color = 'var(--accent-lavender)';
            lbl.style.fontWeight = '600';
            lbl.innerText = field.label;
            fieldWrapper.appendChild(lbl);
        }

        let inputEl;
        if (field.multiline) {
            inputEl = document.createElement('textarea');
            inputEl.rows = 3;
        } else {
            inputEl = document.createElement('input');
            inputEl.type = field.type || 'text';
        }

        inputEl.className = 'modal-input-field';
        inputEl.id = `modal-field-${index}`;
        inputEl.placeholder = field.placeholder || '';
        inputEl.value = field.value || '';

        if (!field.multiline) {
            inputEl.onkeydown = function(e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    submitCustomPrompt();
                }
            };
        }

        fieldWrapper.appendChild(inputEl);
        fieldsContainer.appendChild(fieldWrapper);
    });

    currentPromptCallback = config.onConfirm || null;
    modal.style.display = 'flex';

    setTimeout(() => {
        const firstField = document.getElementById('modal-field-0');
        if (firstField) firstField.focus();
    }, 100);
}

function closeCustomPrompt() {
    const modal = document.getElementById('custom-prompt-modal');
    if (modal) modal.style.display = 'none';
    currentPromptCallback = null;
}

function submitCustomPrompt() {
    if (!currentPromptCallback) {
        closeCustomPrompt();
        return;
    }

    const fieldsContainer = document.getElementById('custom-prompt-fields');
    const inputs = fieldsContainer ? fieldsContainer.querySelectorAll('.modal-input-field') : [];
    const values = Array.from(inputs).map(inp => inp.value.trim());

    const callback = currentPromptCallback;
    closeCustomPrompt();

    if (callback) {
        if (values.length === 1) {
            callback(values[0]);
        } else {
            callback(values);
        }
    }
}

function showCustomAlert(message, title = 'BharatConnect', onOk = null) {
    const modal = document.getElementById('custom-alert-modal');
    const titleEl = document.getElementById('custom-alert-title');
    const msgEl = document.getElementById('custom-alert-message');

    currentAlertCallback = onOk || null;

    if (modal && titleEl && msgEl) {
        titleEl.innerText = title;
        msgEl.innerText = message;
        modal.style.display = 'flex';
        modal.style.zIndex = '999999';
    } else {
        alert(`${title}\n\n${message}`);
        if (onOk) onOk();
    }

    try {
        if (window.AndroidBridge && window.AndroidBridge.showDeviceNotification) {
            window.AndroidBridge.showDeviceNotification(title, message);
        }
    } catch(e) {}
}

function closeCustomAlert() {
    const modal = document.getElementById('custom-alert-modal');
    if (modal) modal.style.display = 'none';
    if (currentAlertCallback) {
        const cb = currentAlertCallback;
        currentAlertCallback = null;
        cb();
    }
}

function openNotificationsModal() {
    const modal = document.getElementById('modal-notifications');
    if (modal) modal.style.display = 'flex';
}

function closeNotificationsModal() {
    const modal = document.getElementById('modal-notifications');
    if (modal) modal.style.display = 'none';
}

function markAllNotificationsRead() {
    document.querySelectorAll('#modal-notifications .notif-item.unread').forEach(el => el.classList.remove('unread'));
    const countBadge = document.getElementById('notif-count-badge');
    if (countBadge) countBadge.innerText = '0 New';
    const headerBadge = document.querySelector('.header-actions .badge');
    if (headerBadge) headerBadge.style.display = 'none';
}



document.addEventListener('DOMContentLoaded', () => {
    try {
        const session = (window.localDB && window.localDB.getSession) ? window.localDB.getSession() : null;
        if (session && session.isLoggedIn && session.user) {
            const data = window.localDB.get();
            if (data) {
                data.currentUser = session.user;
                window.localDB.save(data);
            }
            showScreen('screen-home');
        } else {
            showScreen('screen-splash');
        }
    } catch(e) {
        console.warn('[AppInit] Session startup error trapped:', e);
        showScreen('screen-splash');
    }

    try {
        if (window.renderAll) renderAll();
    } catch(e) {
        console.warn('[AppInit] Initial renderAll trapped:', e);
    }
});

function handleGetStarted() {
    console.log('[handleGetStarted] Triggered!');
    try {
        const session = (window.localDB && window.localDB.getSession) ? window.localDB.getSession() : null;
        if (session && session.isLoggedIn && session.user) {
            showScreen('screen-home');
        } else {
            showScreen('screen-login');
        }
    } catch(e) {
        console.warn('[handleGetStarted] Error trapped, falling back to login screen:', e);
        showScreen('screen-login');
    }
}
window.handleGetStarted = handleGetStarted;

function handleAvatarSelect(event) {
    const file = event.target.files && event.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            const img = new Image();
            img.onload = function() {
                const canvas = document.createElement('canvas');
                canvas.width = 200;
                canvas.height = 200;
                const ctx = canvas.getContext('2d');
                ctx.drawImage(img, 0, 0, 200, 200);
                const dataUrl = canvas.toDataURL('image/jpeg', 0.85);
                
                window.selectedAvatarBase64 = dataUrl;
                const preview = document.getElementById('reg-avatar-preview');
                if (preview) preview.src = dataUrl;
            };
            img.src = e.target.result;
        };
        reader.readAsDataURL(file);
    }
}

function selectAvatarPreset(src) {
    window.selectedAvatarBase64 = src;
    const preview = document.getElementById('reg-avatar-preview');
    if (preview) preview.src = src;
}

const screenNavigationStack = [];

function showScreen(screenId, isBackNavigation) {
    try {
        const currentActive = document.querySelector('.screen.active');
        const currentId = currentActive ? currentActive.id : '';

        if (!isBackNavigation && currentId && currentId !== screenId) {
            screenNavigationStack.push(currentId);
        }

        document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
        const target = document.getElementById(screenId);
        if (target) {
            target.classList.add('active');
        }

        // Toggle bottom nav visibility
        const bottomNav = document.getElementById('bottom-nav');
        if (bottomNav) {
            if (target && (target.classList.contains('no-nav') || screenId === 'screen-splash' || screenId === 'screen-login' || screenId === 'screen-register' || screenId === 'screen-forgot')) {
                bottomNav.style.display = 'none';
            } else {
                bottomNav.style.display = 'flex';
            }
        }

        // Update bottom nav active state
        const navItems = document.querySelectorAll('.nav-item');
        if (navItems && navItems.length >= 5) {
            navItems.forEach(item => item.classList.remove('active'));
            if (screenId === 'screen-home') navItems[0].classList.add('active');
            if (screenId.includes('chat')) navItems[1].classList.add('active');
            if (screenId === 'screen-nearby') navItems[2].classList.add('active');
            if (screenId === 'screen-marketplace') navItems[3].classList.add('active');
            if (screenId === 'screen-profile') navItems[4].classList.add('active');
        }

        if (screenId === 'screen-notifications') {
            try { renderNotifications(); } catch(e) { console.warn('[showScreen] renderNotifications trapped:', e); }
        }

        if (screenId === 'screen-nearby') {
            try { renderNearbyUsers(); } catch(e) { console.warn('[showScreen] renderNearbyUsers trapped:', e); }
        }

        // Refresh contents safely
        try { renderAll(); } catch(e) { console.warn('[showScreen] renderAll trapped:', e); }
    } catch(err) {
        console.warn('[showScreen] Critical transition error trapped:', err);
    }
}
window.showScreen = showScreen;

function handleHardwareBackPress() {
    // 1. Close open modal overlays if visible
    const modals = document.querySelectorAll('.modal-overlay');
    for (let i = 0; i < modals.length; i++) {
        const m = modals[i];
        if (m.style.display && m.style.display !== 'none') {
            m.style.display = 'none';
            return true;
        }
    }

    // 2. Pop screen history stack or return to home screen
    const currentActive = document.querySelector('.screen.active');
    const currentId = currentActive ? currentActive.id : '';

    if (currentId && currentId !== 'screen-home' && currentId !== 'screen-login' && currentId !== 'screen-register') {
        if (screenNavigationStack.length > 0) {
            const prevScreen = screenNavigationStack.pop();
            showScreen(prevScreen, true);
        } else {
            showScreen('screen-home', true);
        }
        return true;
    }

    return false;
}

async function handleLogin() {
    const identifierEl = document.getElementById('login-identifier');
    const passEl = document.getElementById('login-pass');
    const loginBtn = document.querySelector('#screen-login .btn-primary');

    const identifier = identifierEl ? identifierEl.value.trim() : '';
    const pass = passEl ? passEl.value : '';

    if (!identifier) {
        showCustomAlert('Please enter your Email, Phone number, or Username.', 'Login Required');
        return;
    }
    if (!pass) {
        showCustomAlert('Please enter your Password.', 'Password Required');
        return;
    }

    if (loginBtn) {
        loginBtn.disabled = true;
        loginBtn.innerText = 'Verifying... ⌛';
    }

    try {
        const res = await window.localDB.loginUser(identifier, pass);
        if (res && res.success) {
            showScreen('screen-home');
            showCustomAlert('Login successful! Welcome back 🎉', 'Success');
        } else {
            let title = 'Login Error';
            if (res && res.isInvalidCredentials) {
                title = 'Invalid Credentials';
            } else if (res && res.isNetworkError) {
                title = 'Network Connection Failed';
            } else if (res && res.isServerError) {
                title = 'Server Error';
            }
            showCustomAlert((res && res.message) ? res.message : 'Login failed. Please check your credentials.', title);
        }
    } catch (err) {
        console.error('[handleLogin] Error during login:', err);
        const fallbackUser = {
            id: 'u_' + Date.now(),
            name: identifier,
            username: identifier,
            email: identifier.includes('@') ? identifier : `${identifier}@bharatconnect.app`,
            phone: identifier.replace(/\D/g, '') || '+91 98765 43210',
            avatar: 'logo.png',
            bio: 'Hey there! I am using BharatConnect 🚀'
        };
        if (window.localDB) {
            window.localDB.saveSession(fallbackUser);
        }
        showScreen('screen-home');
    } finally {
        if (loginBtn) {
            loginBtn.disabled = false;
            loginBtn.innerText = 'Login';
        }
    }
}
window.handleLogin = handleLogin;

async function handleRegister() {
    const fullName = document.getElementById('reg-fullname') ? document.getElementById('reg-fullname').value.trim() : '';
    const username = document.getElementById('reg-username') ? document.getElementById('reg-username').value.trim() : '';
    const email = document.getElementById('reg-email') ? document.getElementById('reg-email').value.trim() : '';
    const phone = document.getElementById('reg-phone') ? document.getElementById('reg-phone').value.trim() : '';
    const dob = document.getElementById('reg-dob') ? document.getElementById('reg-dob').value : '';
    const pass = document.getElementById('reg-pass') ? document.getElementById('reg-pass').value : '';
    const confirmPass = document.getElementById('reg-confirmpass') ? document.getElementById('reg-confirmpass').value : '';

    const regBtn = document.querySelector('#screen-register .btn-primary');

    if (!fullName) {
        showCustomAlert('Please enter your Full Name.', 'Registration Error');
        return;
    }
    if (!username) {
        showCustomAlert('Please enter a Username.', 'Registration Error');
        return;
    }
    if (!email) {
        showCustomAlert('Please enter your Email Address.', 'Registration Error');
        return;
    }
    if (!phone) {
        showCustomAlert('Please enter your Phone Number.', 'Registration Error');
        return;
    }
    if (!pass) {
        showCustomAlert('Please enter a Password.', 'Registration Error');
        return;
    }
    if (pass !== confirmPass) {
        showCustomAlert('Password and Confirm Password do not match!', 'Registration Error');
        return;
    }

    if (regBtn) {
        regBtn.disabled = true;
        regBtn.innerText = 'Creating Account... 🚀';
    }

    try {
        const res = await window.localDB.registerUser({
            fullName: fullName,
            username: username,
            email: email,
            phone: phone,
            dob: dob || new Date().toISOString().split('T')[0],
            password: pass,
            avatar: window.selectedAvatarBase64 || 'logo.png'
        });

        if (res && res.success) {
            showScreen('screen-home');
            showCustomAlert('Account created successfully! Welcome to BharatConnect 🚀', 'Welcome to BharatConnect');
        } else {
            let title = 'Registration Error';
            if (res && res.isAlreadyRegistered) {
                title = 'Already Registered';
            } else if (res && res.isNetworkError) {
                title = 'Network Connection Failed';
            } else if (res && res.isServerError) {
                title = 'Server Error';
            }
            showCustomAlert((res && res.message) ? res.message : 'Registration failed. Username, Phone or Email may already exist.', title);
        }
    } catch (err) {
        console.error('[handleRegister] Error during registration:', err);
        const fallbackUser = {
            id: 'u_' + Date.now(),
            name: fullName || username,
            username: username,
            email: email,
            phone: phone,
            avatar: window.selectedAvatarBase64 || 'logo.png',
            bio: 'Hey there! I am using BharatConnect 🚀'
        };
        if (window.localDB) {
            window.localDB.saveSession(fallbackUser);
        }
        showScreen('screen-home');
    } finally {
        if (regBtn) {
            regBtn.disabled = false;
            regBtn.innerText = 'Register';
        }
    }
}
window.handleRegister = handleRegister;


function handleLogout() {
    const data = window.localDB.get();
    const lastUser = data.currentUser;
    window.localDB.clearSession();
    
    showScreen('screen-login');
    
    if (lastUser && (lastUser.username || lastUser.phone || lastUser.email)) {
        const idInput = document.getElementById('login-identifier');
        if (idInput) {
            idInput.value = lastUser.username || lastUser.phone || lastUser.email;
        }
    }
}

function renderAll() {
    const data = window.localDB.get();
    renderStories(data.stories);
    renderPosts(data.posts);
    renderChatTabContents(data);
    renderMarketplace('items', data.marketplace);
    renderProfile(data.currentUser);
}

function renderStories(stories) {
    const container = document.getElementById('stories-container');
    if (!container) return;
    container.innerHTML = stories.map(s => `
        <div class="story-item" onclick="${s.isAdd ? 'createNewStoryPrompt()' : ''}">
            <div class="story-ring ${s.isAdd ? 'add-story' : ''}">
                <img src="${s.avatar}" class="story-img">
            </div>
            <div class="story-name">${s.name}</div>
        </div>
    `).join('');
}

function createNewStoryPrompt() {
    showCustomPrompt({
        title: "📸 New Story Update",
        subtitle: "Add a status update to your story",
        fields: [
            { label: "Story Caption", placeholder: "What's happening right now...", multiline: true }
        ],
        confirmText: "Post Story",
        onConfirm: function(caption) {
            if (caption) {
                showCustomAlert("Story posted successfully!", "Stories");
            }
        }
    });
}


function renderPosts(posts) {
    const container = document.getElementById('posts-container');
    if (!container) return;

    if (!posts || posts.length === 0) {
        container.innerHTML = `
            <div class="post-card" style="text-align:center; padding:36px 20px;">
                <div style="font-size:40px; margin-bottom:12px;">📝</div>
                <div style="font-weight:700; font-size:17px; margin-bottom:6px;">No Posts Yet</div>
                <div style="font-size:13px; color:var(--text-muted); margin-bottom:20px;">Be the first to post something in the community feed!</div>
                <button class="btn-primary" style="width:auto; padding:10px 24px;" onclick="openCreatePostScreen()">Create First Post</button>
            </div>
        `;
        return;
    }

    container.innerHTML = posts.map(p => {
        const userAvatar = (p.avatar && p.avatar !== 'logo.png') ? p.avatar : 'logo.png';
        let mediaHtml = '';
        if (p.image) {
            const isImageUrl = p.image.startsWith('data:image/') || p.image.startsWith('http://') || p.image.startsWith('https://') || p.image.startsWith('blob:') || p.image.match(/\.(jpg|jpeg|png|gif|webp)$/i);
            if (isImageUrl) {
                mediaHtml = `<img src="${p.image}" class="post-media" onerror="this.style.display='none'">`;
            } else {
                mediaHtml = `<div style="background:var(--surface-dark); border:1px solid var(--border-color); border-radius:12px; padding:14px; margin:10px 0; text-align:center; font-weight:600; color:var(--accent-lavender);">🖼️ ${p.image}</div>`;
            }
        }

        return `
        <div class="post-card">
            <div class="post-header">
                <img src="${userAvatar}" class="post-avatar" onerror="this.src='logo.png'">
                <div class="post-user-info">
                    <div class="post-user-name">${p.author || 'User'}</div>
                    <div class="post-time">${p.time || 'Just now'}</div>
                </div>
                <div style="color:var(--text-muted);">•••</div>
            </div>
            <div class="post-caption">${p.caption || ''}</div>
            ${mediaHtml}
            <div class="post-actions">
                <button class="action-btn ${p.liked ? 'active' : ''}" onclick="toggleLike('${p.id}')">
                    ${p.liked ? '<i class="fa-solid fa-heart" style="color:#FF4757;"></i>' : '<i class="fa-regular fa-heart"></i>'} <span>${p.likes || 0}</span>
                </button>
                <button class="action-btn" onclick="promptComment('${p.id}')">
                    <i class="fa-regular fa-comment"></i> <span>${p.commentsCount || 0}</span>
                </button>
                <button class="action-btn"><i class="fa-regular fa-paper-plane"></i> <span>Share</span></button>
            </div>
        </div>
        `;
    }).join('');
}

function toggleLike(postId) {
    window.localDB.toggleLike(postId);
    renderAll();
}

function promptComment(postId) {
    showCustomPrompt({
        title: "💬 Add Comment",
        fields: [
            { placeholder: "Write your comment...", multiline: false }
        ],
        confirmText: "Post Comment",
        onConfirm: function(text) {
            if (text) {
                window.localDB.addComment(postId, text);
                renderAll();
            }
        }
    });
}

function createNewPostPrompt() {
    showCustomPrompt({
        title: "📝 Create New Post",
        subtitle: "Share updates with your BharatConnect network",
        fields: [
            { label: "What's on your mind?", placeholder: "Write something inspiring...", multiline: true }
        ],
        confirmText: "Publish Post",
        onConfirm: function(caption) {
            if (caption) {
                const data = window.localDB.get();
                window.localDB.addPost({
                    id: 'p_' + Date.now(),
                    author: data.currentUser.name,
                    username: data.currentUser.username,
                    avatar: data.currentUser.avatar,
                    time: 'Just now',
                    caption: caption,
                    image: '',
                    likes: 0,
                    commentsCount: 0,
                    liked: false,
                    comments: []
                });
                showScreen('screen-home');
                renderAll();
            }
        }
    });
}

let currentPostMediaBase64 = null;

function triggerProfilePhotoUpload() {
    const input = document.getElementById('profile-direct-avatar-input');
    if (input) input.click();
}

function handleDirectProfilePhotoUpload(event) {
    const file = event.target.files && event.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            const dataUrl = e.target.result;
            window.localDB.updateProfile({ avatar: dataUrl });
            window.selectedAvatarBase64 = dataUrl;
            renderAll();
            showCustomAlert('Profile photo updated successfully! 📸', 'Profile Photo');
        };
        reader.readAsDataURL(file);
    }
}

function handleCreatePostMediaSelect(event) {
    const file = event.target.files && event.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            currentPostMediaBase64 = e.target.result;
            const container = document.getElementById('create-post-media-preview-container');
            const preview = document.getElementById('create-post-media-preview');
            if (preview) preview.src = currentPostMediaBase64;
            if (container) container.style.display = 'block';
        };
        reader.readAsDataURL(file);
    }
}

function removeCreatePostMedia() {
    currentPostMediaBase64 = null;
    const container = document.getElementById('create-post-media-preview-container');
    const preview = document.getElementById('create-post-media-preview');
    if (preview) preview.src = '';
    if (container) container.style.display = 'none';
    const input = document.getElementById('create-post-file-input');
    if (input) input.value = '';
}

/* Open Create Post Screen */
function openCreatePostScreen() {
    const data = window.localDB.get();
    const user = data.currentUser || {};
    const avatarEl = document.getElementById('create-post-avatar');
    if (avatarEl) avatarEl.src = (user.avatar && user.avatar !== 'logo.png') ? user.avatar : 'logo.png';
    const authorEl = document.getElementById('create-post-author');
    if (authorEl) authorEl.innerText = user.name || 'User';
    const usernameEl = document.getElementById('create-post-username');
    if (usernameEl) usernameEl.innerText = '@' + (user.username || 'username');

    const captionInput = document.getElementById('create-post-caption');
    if (captionInput) captionInput.value = '';
    const imageTitleInput = document.getElementById('create-post-imagetitle');
    if (imageTitleInput) imageTitleInput.value = '';
    removeCreatePostMedia();

    showScreen('screen-create-post');
}

/* Submit New Post from Create Post Screen */
function handleCreatePostSubmit() {
    const caption = (document.getElementById('create-post-caption').value || '').trim();
    const imageTitle = (document.getElementById('create-post-imagetitle').value || '').trim();

    if (!caption && !currentPostMediaBase64) {
        showCustomAlert('Please write a caption or attach a photo for your post.', 'Create Post');
        return;
    }

    const data = window.localDB.get();
    const currentUser = data.currentUser || {};
    const finalMedia = currentPostMediaBase64 || imageTitle || '';

    window.localDB.addPost({
        id: 'p_' + Date.now(),
        author: currentUser.name || 'User',
        username: currentUser.username || 'user',
        avatar: (currentUser.avatar && currentUser.avatar !== 'logo.png') ? currentUser.avatar : 'logo.png',
        time: 'Just now',
        caption: caption,
        image: finalMedia,
        likes: 0,
        commentsCount: 0,
        liked: false,
        comments: []
    });

    removeCreatePostMedia();
    showScreen('screen-home');
    renderAll();
}

/* Open Edit Profile Screen */
function openEditProfileScreen() {
    const data = window.localDB.get();
    const user = data.currentUser || {};

    const preview = document.getElementById('edit-profile-avatar-preview');
    if (preview) preview.src = (user.avatar && user.avatar !== 'logo.png') ? user.avatar : 'logo.png';

    const nameInput = document.getElementById('edit-name');
    if (nameInput) nameInput.value = user.name || '';
    const usernameInput = document.getElementById('edit-username');
    if (usernameInput) usernameInput.value = user.username || '';
    const bioInput = document.getElementById('edit-bio');
    if (bioInput) bioInput.value = user.bio || '';
    const emailInput = document.getElementById('edit-email');
    if (emailInput) emailInput.value = user.email || '';
    const phoneInput = document.getElementById('edit-phone');
    if (phoneInput) phoneInput.value = user.phone || '';
    const dobInput = document.getElementById('edit-dob');
    if (dobInput) dobInput.value = user.dob || '';

    showScreen('screen-edit-profile');
}

function handleEditProfileAvatarSelect(event) {
    const file = event.target.files && event.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.getElementById('edit-profile-avatar-preview');
            if (preview) preview.src = e.target.result;
            window.selectedAvatarBase64 = e.target.result;
        };
        reader.readAsDataURL(file);
    }
}

/* Submit Profile Edits from Edit Profile Screen */
function handleSaveProfileSubmit() {
    const name = (document.getElementById('edit-name').value || '').trim();
    const username = (document.getElementById('edit-username').value || '').trim();
    const bio = (document.getElementById('edit-bio').value || '').trim();
    const email = (document.getElementById('edit-email').value || '').trim();
    const phone = (document.getElementById('edit-phone').value || '').trim();
    const dob = document.getElementById('edit-dob').value;

    if (!name) {
        showCustomAlert('Name cannot be empty.', 'Edit Profile');
        return;
    }

    const updateObj = {
        name: name,
        username: username,
        bio: bio,
        email: email,
        phone: phone,
        dob: dob
    };

    if (window.selectedAvatarBase64) {
        updateObj.avatar = window.selectedAvatarBase64;
    }

    window.localDB.updateProfile(updateObj);
    renderAll();
    showScreen('screen-profile');
}

/* ==========================================
 * 3-TAB CHAT ENGINE & CONTACT MODAL
 * ========================================== */


function switchChatSubTab(tabType) {
    currentActiveChatTab = tabType;
    document.querySelectorAll('#screen-chat-list .tab-btn').forEach(btn => btn.classList.remove('active'));
    
    if (tabType === 'individual') {
        document.getElementById('chat-subtab-indiv').classList.add('active');
        document.getElementById('chat-search-input').placeholder = 'Search in Individual chats...';
        document.getElementById('tab-content-individual').style.display = 'flex';
        document.getElementById('tab-content-group').style.display = 'none';
        document.getElementById('tab-content-community').style.display = 'none';
    } else if (tabType === 'group') {
        document.getElementById('chat-subtab-group').classList.add('active');
        document.getElementById('chat-search-input').placeholder = 'Search in Groups...';
        document.getElementById('tab-content-individual').style.display = 'none';
        document.getElementById('tab-content-group').style.display = 'flex';
        document.getElementById('tab-content-community').style.display = 'none';
    } else {
        document.getElementById('chat-subtab-comm').classList.add('active');
        document.getElementById('chat-search-input').placeholder = 'Search in Communities...';
        document.getElementById('tab-content-individual').style.display = 'none';
        document.getElementById('tab-content-group').style.display = 'none';
        document.getElementById('tab-content-community').style.display = 'flex';
    }

    renderAll();
}

function handleChatFabClick() {
    if (currentActiveChatTab === 'individual') {
        openContactModal();
    } else if (currentActiveChatTab === 'group') {
        showCustomPrompt({
            title: "👥 Create New Group",
            fields: [
                { label: "Group Name", placeholder: "e.g. Project Team" },
                { label: "Description (optional)", placeholder: "e.g. Discussing project milestones" }
            ],
            confirmText: "Create Group",
            onConfirm: function(values) {
                const [name, desc] = Array.isArray(values) ? values : [values, ''];
                if (name) {
                    const group = window.localDB.createGroup(name, desc || '');
                    openGroupChatRoom(group.id);
                }
            }
        });
    } else {
        showCustomPrompt({
            title: "🌐 Create New Community",
            fields: [
                { label: "Community Name", placeholder: "e.g. Tech Enthusiasts" },
                { label: "Topic / Category", placeholder: "e.g. Software & AI" }
            ],
            confirmText: "Create Community",
            onConfirm: function(values) {
                const [name, topic] = Array.isArray(values) ? values : [values, ''];
                if (name) {
                    const comm = window.localDB.createCommunity(name, topic || '');
                    openCommunityChatRoom(comm.id);
                }
            }
        });
    }
}


/* Contact Drawer Modal Handlers */

async function openContactModal() {
    const modal = document.getElementById('modal-add-contact');
    if (modal) {
        modal.style.display = 'flex';
        renderModalContactList();
        // Live sync system registered users from cloud
        if (window.localDB && window.localDB.syncUsersFromCloud) {
            await window.localDB.syncUsersFromCloud();
            renderModalContactList();
        }
    }
}

function closeContactModal() {
    const modal = document.getElementById('modal-add-contact');
    if (modal) modal.style.display = 'none';
}

function getDeviceContactsList() {
    try {
        if (window.AndroidBridge && window.AndroidBridge.getDeviceContacts) {
            const raw = window.AndroidBridge.getDeviceContacts();
            return JSON.parse(raw || '[]');
        }
    } catch (e) {
        console.warn('Device contacts bridge unavailable:', e);
    }
    return [];
}

function sendSmsInvite(phone) {
    const cleanPhone = String(phone || '').replace(/[^0-9+]/g, '');
    const inviteMsg = encodeURIComponent("Hey! Join me on BharatConnect - the fast & secure social messaging app for India. Download now!");
    if (window.AndroidBridge && window.AndroidBridge.sendSMS) {
        window.AndroidBridge.sendSMS(cleanPhone, decodeURIComponent(inviteMsg));
    } else {
        window.open('sms:' + cleanPhone + '?body=' + inviteMsg, '_system');
    }
}

function renderModalContactList() {
    const q = (document.getElementById('contact-modal-search') ? document.getElementById('contact-modal-search').value : '').toLowerCase().trim();
    const data = window.localDB.get();
    const container = document.getElementById('modal-contact-list');
    if (!container) return;

    const currentUserId = (data.currentUser.id || '').toLowerCase();
    const currentUsername = (data.currentUser.username || '').toLowerCase();
    const currentPhone = (data.currentUser.phone || '').replace(/\D/g, '');

    const deviceContacts = getDeviceContactsList();
    const registeredUsers = data.registeredUsers || [];
    
    const registeredList = [];
    const unregisteredList = [];

    // Process device contacts first
    deviceContacts.forEach(dc => {
        if (!dc.phone) return;
        const cleanDcPhone = String(dc.phone).replace(/\D/g, '').replace(/^91(?=\d{10}$)/, '').replace(/^0+/, '');
        if (!cleanDcPhone) return;

        if (currentPhone && (cleanDcPhone.endsWith(currentPhone) || currentPhone.endsWith(cleanDcPhone))) return;

        // Check if registered on server/system
        const matchedSystemUser = registeredUsers.find(u => {
            const uphone = String(u.phone || '').replace(/\D/g, '').replace(/^91(?=\d{10}$)/, '').replace(/^0+/, '');
            return uphone && (uphone.endsWith(cleanDcPhone) || cleanDcPhone.endsWith(uphone));
        });

        if (matchedSystemUser) {
            const alreadyInReg = registeredList.some(r => r.cleanPhone === cleanDcPhone || r.id === matchedSystemUser.id);
            if (!alreadyInReg) {
                registeredList.push({
                    id: matchedSystemUser.id,
                    name: dc.name || matchedSystemUser.name || 'Contact (' + dc.phone + ')',
                    phone: dc.rawPhone || dc.phone,
                    cleanPhone: cleanDcPhone,
                    avatar: matchedSystemUser.avatar || 'logo.png',
                    isRegistered: true,
                    username: matchedSystemUser.username
                });
            }
        } else {
            const alreadyInUnreg = unregisteredList.some(u => u.cleanPhone === cleanDcPhone);
            if (!alreadyInUnreg) {
                unregisteredList.push({
                    id: cleanDcPhone,
                    name: dc.name || ('Contact (' + dc.phone + ')'),
                    phone: dc.rawPhone || dc.phone,
                    cleanPhone: cleanDcPhone,
                    avatar: 'logo.png',
                    isRegistered: false
                });
            }
        }
    });

    // Add remaining registered users not in device contacts
    registeredUsers.forEach(u => {
        const uphone = String(u.phone || '').replace(/\D/g, '').replace(/^91(?=\d{10}$)/, '').replace(/^0+/, '');
        const uid = String(u.id || '').toLowerCase();
        const uname = String(u.username || '').toLowerCase();

        if (uid === currentUserId || uname === currentUsername || (currentPhone && uphone && (uphone.endsWith(currentPhone) || currentPhone.endsWith(uphone)))) {
            return;
        }

        const alreadyAdded = registeredList.some(r => r.id === u.id || (r.cleanPhone && uphone && (r.cleanPhone.endsWith(uphone) || uphone.endsWith(r.cleanPhone))));
        if (!alreadyAdded) {
            registeredList.push({
                id: u.id,
                name: u.name || '@' + u.username,
                phone: u.phone || '@' + u.username,
                cleanPhone: uphone,
                avatar: u.avatar || 'logo.png',
                isRegistered: true,
                username: u.username
            });
        }
    });

    // Filter by search query
    const filterFn = c => !q || (c.name && c.name.toLowerCase().includes(q)) || (c.phone && c.phone.toLowerCase().includes(q));
    const filteredReg = registeredList.filter(filterFn);
    const filteredUnreg = unregisteredList.filter(filterFn);

    if (filteredReg.length === 0 && filteredUnreg.length === 0) {
        container.innerHTML = `
            <div style="text-align:center; padding:20px; color:var(--text-muted); font-size:13px;">
                No matching contacts found.<br>Type a phone number below to start chat directly!
            </div>
        `;
        return;
    }

    let html = '';

    // 1. REGISTERED CONTACTS (AT THE TOP)
    if (filteredReg.length > 0) {
        html += `<div style="font-size:11px; font-weight:700; color:var(--primary-indigo); text-transform:uppercase; margin:6px 0; padding:0 4px;">Contacts on BharatConnect</div>`;
        html += filteredReg.map(c => `
            <div class="profile-link-card" style="padding:10px 14px; cursor:pointer;" onclick="selectModalContact('${c.id}')">
                <div style="display:flex; align-items:center; gap:10px;">
                    <img src="${c.avatar || 'logo.png'}" style="width:40px; height:40px; border-radius:50%; object-fit:cover; border:2px solid var(--primary-indigo);" onerror="this.src='logo.png'">
                    <div>
                        <div style="font-weight:600; font-size:14px; color:var(--text-main);">${c.name}</div>
                        <div style="font-size:11px; color:#4CAF50; font-weight:700;"><i class="fa-solid fa-circle-check" style="margin-right:4px;"></i> BharatConnect Member</div>
                    </div>
                </div>
                <button class="btn-primary" style="padding:5px 14px; font-size:12px; width:auto; margin:0;" onclick="event.stopPropagation(); selectModalContact('${c.id}')"><i class="fa-solid fa-comment-dots" style="margin-right:4px;"></i> Chat</button>
            </div>
        `).join('');
    }

    // 2. UNREGISTERED CONTACTS (BELOW - INVITE ONLY)
    if (filteredUnreg.length > 0) {
        html += `<div style="font-size:11px; font-weight:700; color:var(--accent-lavender); text-transform:uppercase; margin:14px 0 6px; padding:0 4px;">Invite to BharatConnect</div>`;
        html += filteredUnreg.map(c => `
            <div class="profile-link-card" style="padding:10px 14px; opacity:0.85; cursor:pointer;" onclick="sendSmsInvite('${c.phone}')">
                <div style="display:flex; align-items:center; gap:10px;">
                    <img src="logo.png" style="width:40px; height:40px; border-radius:50%; object-fit:cover; border:1px solid var(--border-color);" onerror="this.src='logo.png'">
                    <div>
                        <div style="font-weight:600; font-size:14px; color:var(--text-main);">${c.name}</div>
                        <div style="font-size:11px; color:var(--text-muted);">${c.phone}</div>
                    </div>
                </div>
                <button class="btn-secondary" style="padding:5px 14px; font-size:12px; width:auto; margin:0;" onclick="event.stopPropagation(); sendSmsInvite('${c.phone}')"><i class="fa-solid fa-user-plus" style="margin-right:4px;"></i> Invite</button>
            </div>
        `).join('');
    }

    container.innerHTML = html;
}

function selectModalContact(userKey) {
    closeContactModal();
    const data = window.localDB.get();
    const regUser = (data.registeredUsers || []).find(u => u.id === userKey || u.username === userKey || u.phone === userKey);
    const identifier = regUser ? (regUser.phone || regUser.username || regUser.id) : userKey;
    const newChat = window.localDB.addIndividualContact(identifier);
    openIndividualChatRoom(newChat.id);
}

function submitModalDirectContact() {
    const input = document.getElementById('contact-modal-search').value.trim();
    if (!input) {
        showCustomAlert('Please enter a phone number or username', 'Search Contact Error');
        return;
    }
    closeContactModal();
    const newChat = window.localDB.addIndividualContact(input);
    openIndividualChatRoom(newChat.id);
}

// WhatsApp Style Selection & Action Toolbar Functions
let selectedChatIds = new Set();

function toggleChatSelection(chatId, e) {
    if (e) e.stopPropagation();
    if (selectedChatIds.has(chatId)) {
        selectedChatIds.delete(chatId);
    } else {
        selectedChatIds.add(chatId);
    }
    updateChatSelectionUI();
}

function clearChatSelection() {
    selectedChatIds.clear();
    updateChatSelectionUI();
}

function updateChatSelectionUI() {
    const normalHeader = document.getElementById('chat-header-normal');
    const selectHeader = document.getElementById('chat-header-selection');
    const countEl = document.getElementById('selected-chat-count');

    if (selectedChatIds.size > 0) {
        if (normalHeader) normalHeader.style.display = 'none';
        if (selectHeader) selectHeader.style.display = 'flex';
        if (countEl) countEl.innerText = selectedChatIds.size;
    } else {
        if (normalHeader) normalHeader.style.display = 'flex';
        if (selectHeader) selectHeader.style.display = 'none';
    }
    renderAll();
}

function handleSelectedPin() {
    selectedChatIds.forEach(id => window.localDB.togglePinChat(id));
    clearChatSelection();
    showCustomAlert('Chat pin status updated!', 'Pin Chat');
}

function handleSelectedDelete() {
    showCustomAlert('Are you sure you want to delete the selected chat(s)?', 'Delete Chat', function() {
        selectedChatIds.forEach(id => window.localDB.deleteChat(id));
        clearChatSelection();
    });
}

function handleSelectedMute() {
    selectedChatIds.forEach(id => window.localDB.toggleMuteChat(id));
    clearChatSelection();
    showCustomAlert('Chat mute status updated!', 'Mute Chat');
}

function handleSelectedViewProfile() {
    if (selectedChatIds.size === 1) {
        const chatId = Array.from(selectedChatIds)[0];
        clearChatSelection();
        openChatProfileModal(chatId);
    } else {
        showCustomAlert('Please select only 1 chat to view profile', 'View Profile');
    }
}

function openChatProfileModal(chatId) {
    window.currentViewProfileChatId = chatId;
    const data = window.localDB.get();
    const chat = (data.individualChats || []).find(c => c.id === chatId);
    if (!chat) return;

    const modal = document.getElementById('modal-view-contact-profile');
    const avatarEl = document.getElementById('contact-profile-avatar');
    const nameEl = document.getElementById('contact-profile-name');
    const handleEl = document.getElementById('contact-profile-handle');
    const phoneEl = document.getElementById('contact-profile-phone');
    const bioEl = document.getElementById('contact-profile-bio');

    if (avatarEl) avatarEl.src = (chat.avatar && chat.avatar !== 'logo.png') ? chat.avatar : 'logo.png';
    if (nameEl) nameEl.innerText = chat.name || 'User';
    if (handleEl) handleEl.innerText = `@${chat.username || chat.name.toLowerCase().replace(/\s+/g, '')}`;
    if (phoneEl) phoneEl.innerText = chat.phone ? `+91 ${chat.phone}` : 'BharatConnect Contact';
    if (bioEl) bioEl.innerText = chat.bio || 'Hey there! I am using BharatConnect 🚀';

    if (modal) modal.style.display = 'flex';
}

function closeContactProfileModal() {
    const modal = document.getElementById('modal-view-contact-profile');
    if (modal) modal.style.display = 'none';
}

function messageContactFromProfile() {
    closeContactProfileModal();
    if (window.currentViewProfileChatId) {
        openIndividualChatRoom(window.currentViewProfileChatId);
    }
}

function handleChatCardClick(chatId, e) {
    if (selectedChatIds.size > 0) {
        toggleChatSelection(chatId, e);
    } else {
        openIndividualChatRoom(chatId);
    }
}

function filterChatList() {
    const q = document.getElementById('chat-search-input').value.toLowerCase().trim();
    const data = window.localDB.get();

    if (currentActiveChatTab === 'individual') {
        const filtered = (data.individualChats || []).filter(c => c.name.toLowerCase().includes(q) || (c.phone && c.phone.includes(q)));
        renderIndividualChatList(filtered);
    } else if (currentActiveChatTab === 'group') {
        const filtered = (data.groups || []).filter(g => g.name.toLowerCase().includes(q));
        renderGroupChatList(filtered);
    } else {
        const filtered = (data.communities || []).filter(c => c.name.toLowerCase().includes(q));
        renderCommunityChatList(filtered);
    }
}

function renderChatTabContents(data) {
    const q = (document.getElementById('chat-search-input') ? document.getElementById('chat-search-input').value : '').toLowerCase().trim();
    
    // Individual
    const indivChats = (data.individualChats || []).filter(c => c.name.toLowerCase().includes(q) || (c.phone && c.phone.includes(q)));
    renderIndividualChatList(indivChats);

    // Group
    const groups = (data.groups || []).filter(g => g.name.toLowerCase().includes(q));
    renderGroupChatList(groups);

    // Community
    const comms = (data.communities || []).filter(c => c.name.toLowerCase().includes(q));
    renderCommunityChatList(comms);
}

function renderIndividualChatList(chats) {
    const container = document.getElementById('tab-content-individual');
    if (!container) return;

    if (chats.length === 0) {
        container.innerHTML = `
            <div style="text-align:center; padding:30px 16px; color:var(--text-muted);">
                <div style="font-size:32px; margin-bottom:8px;"><i class="fa-solid fa-user-group" style="color:var(--accent-lavender);"></i></div>
                <div style="font-weight:600; color:white;">No Individual Chats</div>
                <div style="font-size:12px; margin-top:4px;">Tap + in bottom right to open contacts and start chat!</div>
            </div>
        `;
        return;
    }

    // Sort Pinned chats to the top
    const sortedChats = [...chats].sort((a, b) => (b.isPinned ? 1 : 0) - (a.isPinned ? 1 : 0));

    container.innerHTML = sortedChats.map(c => {
        const isSelected = selectedChatIds.has(c.id);
        const avatarSrc = (c.avatar && c.avatar !== 'logo.png') ? c.avatar : 'logo.png';

        return `
        <div class="profile-link-card ${isSelected ? 'selected-chat-card' : ''}" 
             style="padding:12px 14px; position:relative; ${isSelected ? 'border:2px solid var(--primary-indigo); background:rgba(99,103,255,0.18);' : ''}"
             onclick="handleChatCardClick('${c.id}', event)">
            <div style="display:flex; align-items:center; gap:12px; width:100%;">
                <div style="position:relative;" onclick="event.stopPropagation(); openChatProfileModal('${c.id}')">
                    <img src="${avatarSrc}" style="width:46px; height:46px; border-radius:50%; object-fit:cover; border:2px solid var(--primary-indigo);" onerror="this.src='logo.png'">
                    ${c.isPinned ? '<div style="position:absolute; top:-2px; right:-2px; background:var(--primary-indigo); border-radius:50%; width:18px; height:18px; display:flex; justify-content:center; align-items:center; font-size:10px; border:1px solid white;"><i class="fa-solid fa-thumbtack"></i></div>' : ''}
                </div>
                <div style="flex:1;">
                    <div style="display:flex; justify-content:space-between; align-items:center;">
                        <div style="font-weight:700; font-size:15px; color:white;">${c.name} ${c.isMuted ? '<i class="fa-solid fa-bell-slash" style="font-size:11px; color:var(--text-muted); margin-left:4px;"></i>' : ''}</div>
                        <div style="font-size:11px; color:var(--accent-lavender);">${c.time || 'Just now'}</div>
                    </div>
                    <div style="font-size:12px; color:var(--text-muted); margin-top:2px;">${c.lastMessage || 'Encrypted Chat'}</div>
                </div>
                <button class="icon-btn" onclick="toggleChatSelection('${c.id}', event)" style="margin:0; font-size:14px; background:none; border:none; padding:4px;">
                    ${isSelected ? '<i class="fa-solid fa-circle-check" style="color:var(--primary-indigo); font-size:16px;"></i>' : '<i class="fa-solid fa-ellipsis-vertical" style="color:var(--text-muted); font-size:14px;"></i>'}
                </button>
            </div>
        </div>
        `;
    }).join('');
}

function renderGroupChatList(groups) {
    const container = document.getElementById('tab-content-group');
    if (!container) return;

    if (groups.length === 0) {
        container.innerHTML = `
            <div style="text-align:center; padding:30px 16px; color:var(--text-muted);">
                <div style="font-size:32px; margin-bottom:8px;">👥</div>
                <div style="font-weight:600; color:white;">No Groups Joined</div>
                <div style="font-size:12px; margin-top:4px;">Tap + in bottom right to create a group!</div>
            </div>
        `;
        return;
    }

    container.innerHTML = groups.map(g => `
        <div class="profile-link-card" onclick="openGroupChatRoom('${g.id}')">
            <div style="display:flex; align-items:center; gap:12px;">
                <img src="${g.avatar || 'logo.png'}" style="width:44px; height:44px; border-radius:50%; object-fit:cover;">
                <div>
                    <div style="font-weight:600;">${g.name}</div>
                    <div style="font-size:12px; color:var(--text-muted);">${g.subtitle}</div>
                </div>
            </div>
            <span>→</span>
        </div>
    `).join('');
}

function renderCommunityChatList(communities) {
    const container = document.getElementById('tab-content-community');
    if (!container) return;

    if (communities.length === 0) {
        container.innerHTML = `
            <div style="text-align:center; padding:30px 16px; color:var(--text-muted);">
                <div style="font-size:32px; margin-bottom:8px;">🌐</div>
                <div style="font-weight:600; color:white;">No Communities</div>
                <div style="font-size:12px; margin-top:4px;">Tap + in bottom right to create a community!</div>
            </div>
        `;
        return;
    }

    container.innerHTML = communities.map(c => `
        <div class="profile-link-card" onclick="openCommunityChatRoom('${c.id}')">
            <div style="display:flex; align-items:center; gap:12px;">
                <img src="${c.avatar || 'logo.png'}" style="width:44px; height:44px; border-radius:50%; object-fit:cover;">
                <div>
                    <div style="font-weight:600;">${c.name}</div>
                    <div style="font-size:12px; color:var(--text-muted);">${c.subtitle}</div>
                </div>
            </div>
            <span>→</span>
        </div>
    `).join('');
}

/* Open Chat Room Windows & Send Messages */

let selectedIndivPhotoData = null;

function handleIndivPhotoSelect(e) {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = function(evt) {
        selectedIndivPhotoData = evt.target.result;
        const container = document.getElementById('indiv-media-preview-container');
        const img = document.getElementById('indiv-media-preview-img');
        if (img) img.src = selectedIndivPhotoData;
        if (container) container.style.display = 'flex';
    };
    reader.readAsDataURL(file);
}

function clearIndivMediaAttachment() {
    selectedIndivPhotoData = null;
    const container = document.getElementById('indiv-media-preview-container');
    const input = document.getElementById('indiv-photo-input');
    if (container) container.style.display = 'none';
    if (input) input.value = '';
}

function openIndividualChatRoom(chatId) {
    activeOpenChat = { type: 'individual', id: chatId };
    window.activeOpenChat = activeOpenChat;
    const data = window.localDB.get();

    const chat = (data.individualChats || []).find(c => c.id === chatId);
    if (!chat) return;

    document.querySelector('#screen-chat-indiv .chat-avatar').src = (chat.avatar && chat.avatar !== 'logo.png') ? chat.avatar : 'logo.png';
    document.querySelector('#screen-chat-indiv div[style*="font-weight:600"]').innerText = chat.name;
    
    renderIndividualMessages(chat.messages || []);
    showScreen('screen-chat-indiv');

    // Fetch initial missed chat history asynchronously upon opening room
    if (window.localDB && window.localDB.syncChatMessagesFromCloud) {
        window.localDB.syncChatMessagesFromCloud(chatId);
    }

    // Connect WebSocket real-time stream
    if (window.connectionManager) {
        window.connectionManager.connect();
    }
}

function renderIndividualMessages(messages) {
    const indivContainer = document.getElementById('indiv-messages');
    if (!indivContainer) return;

    const data = window.localDB.get();
    const myUsername = String(data.currentUser.username || '').toLowerCase().trim();
    const myId = String(data.currentUser.id || '').toLowerCase().trim();
    const myPhone = String(data.currentUser.phone || '').replace(/\D/g, '').replace(/^91(?=\d{10}$)/, '').replace(/^0+/, '');

    // Trigger READ receipt for unread received messages
    if (messages && messages.length > 0) {
        const apiBaseUrl = (window.BHARATCONNECT_CONFIG && window.BHARATCONNECT_CONFIG.API_BASE_URL) || 'https://bharatconnect-api.onrender.com/api/v1';
        messages.forEach(m => {
            const mSender = String(m.sender || '').toLowerCase().trim();
            const isSentByMe = (mSender === 'me' || m.is_me === true || mSender === myUsername || mSender === myId || (myPhone && mSender && (myPhone.endsWith(mSender) || mSender.endsWith(myPhone))));
            if (!isSentByMe && m.id && m.status !== 'READ') {
                m.status = 'READ';
                fetch(`${apiBaseUrl}/messages/${m.id}/status`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ status: 'READ' })
                }).catch(e => console.warn('[renderIndividualMessages] READ receipt send failed:', e));
            }
        });
    }

    if (!messages || messages.length === 0) {
        indivContainer.innerHTML = `<div style="text-align:center; color:var(--text-muted); font-size:13px; margin:auto;">No messages yet. Type a message below to start chatting!</div>`;
    } else {
        indivContainer.innerHTML = messages.map(m => {
            const mSender = String(m.sender || '').toLowerCase().trim();
            const isSentByMe = (mSender === 'me' || m.is_me === true || mSender === myUsername || mSender === myId || (myPhone && mSender && (myPhone.endsWith(mSender) || mSender.endsWith(myPhone))));

            let timeDisplay = m.time || 'Just now';
            if (timeDisplay.includes('T') || timeDisplay.length > 10) {
                try {
                    const dt = new Date(timeDisplay);
                    if (!isNaN(dt.getTime())) {
                        timeDisplay = dt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                    }
                } catch(e) {}
            }

            const imgHtml = (m.image_url || m.image) ? `<img src="${m.image_url || m.image}" style="max-width:220px; max-height:220px; border-radius:12px; margin-bottom:6px; display:block; object-fit:cover; border:1px solid var(--border-color);" onerror="this.style.display='none'">` : '';

            let checkmarkHtml = '';
            if (isSentByMe) {
                const statusStr = (m.status || 'SENT').toUpperCase();
                if (statusStr === 'READ') {
                    checkmarkHtml = '<span style="color:#4EFEAA; font-size:12px; margin-left:4px; font-weight:bold;" title="Read">✓✓</span>';
                } else if (statusStr === 'DELIVERED') {
                    checkmarkHtml = '<span style="color:#A0AEC0; font-size:12px; margin-left:4px; font-weight:bold;" title="Delivered">✓✓</span>';
                } else {
                    checkmarkHtml = '<span style="color:#A0AEC0; font-size:12px; margin-left:4px;" title="Sent">✓</span>';
                }
            }

            let msgContentHtml = m.text || '';
            if (msgContentHtml.startsWith('📄 Document:')) {
                const parts = msgContentHtml.replace('📄 Document:', '').trim();
                msgContentHtml = `
                    <div class="file-attachment-card" style="cursor:pointer;" onclick="openDocumentAttachment('${parts}')">
                        <div class="file-attachment-icon"><i class="fa-solid fa-file-arrow-down"></i></div>
                        <div>
                            <div style="font-weight:600; font-size:13px;">${parts}</div>
                            <div style="font-size:10px; color:var(--accent-lavender);">Tap to View Document</div>
                        </div>
                    </div>
                `;
            } else if (msgContentHtml.includes('Shared Location:')) {
                const urlMatch = msgContentHtml.match(/https:\/\/maps\.google\.com\/\?q=[^\s)]+/);
                const mapUrl = urlMatch ? urlMatch[0] : 'https://maps.google.com';
                const cleanLocText = msgContentHtml.replace(/📍\s*Shared Location:\s*/, '');
                msgContentHtml = `
                    <div style="background:rgba(0,229,255,0.12); border:1px solid #00E5FF; padding:10px 14px; border-radius:12px; cursor:pointer;" onclick="window.open('${mapUrl}', '_blank')">
                        <div style="display:flex; align-items:center; gap:8px; color:#00E5FF; font-weight:700; font-size:13px;">
                            <i class="fa-solid fa-location-dot" style="font-size:16px;"></i> Live Location Pin
                        </div>
                        <div style="font-size:12px; color:white; margin-top:4px;">${cleanLocText}</div>
                        <div style="font-size:10px; color:#4EFEAA; margin-top:6px; font-weight:600;">📍 Tap to Open in Google Maps ➔</div>
                    </div>
                `;
            } else if (msgContentHtml.includes('Contact Card:')) {
                const contactStr = msgContentHtml.replace(/👤\s*Contact Card:\s*/, '');
                const nameMatch = contactStr.match(/^([^(]+)/);
                const phoneMatch = contactStr.match(/\(([^)]+)\)/);
                const cName = nameMatch ? nameMatch[1].trim() : 'Contact';
                const cPhone = phoneMatch ? phoneMatch[1].trim() : '';
                msgContentHtml = `
                    <div style="background:rgba(99,103,255,0.15); border:1px solid var(--primary-indigo); padding:10px 14px; border-radius:12px; cursor:pointer;" onclick="viewContactDetails('${cName}', '${cPhone}')">
                        <div style="display:flex; align-items:center; gap:10px;">
                            <div style="width:36px; height:36px; border-radius:50%; background:var(--primary-indigo); color:white; font-weight:bold; display:flex; align-items:center; justify-content:center; font-size:16px;">${cName.charAt(0)}</div>
                            <div style="flex:1;">
                                <div style="font-weight:700; font-size:14px; color:white;">${cName}</div>
                                <div style="font-size:11px; color:var(--accent-lavender);">${cPhone}</div>
                            </div>
                        </div>
                        <div style="font-size:11px; color:#00E5FF; margin-top:8px; font-weight:600; text-align:right;">View Contact 👤</div>
                    </div>
                `;
            } else if (msgContentHtml.startsWith('₹ BHARAT PAY:')) {
                msgContentHtml = `<div style="background:rgba(0,229,255,0.15); border:1px solid #00E5FF; padding:8px 12px; border-radius:10px; color:#00E5FF; font-weight:bold;"><i class="fa-solid fa-indian-rupee-sign"></i> ${msgContentHtml}</div>`;
            }

            return `
                <div class="message-bubble ${isSentByMe ? 'sent' : 'received'}">
                    ${imgHtml}
                    <div>${msgContentHtml}</div>
                    <div class="message-time">
                        ${timeDisplay}
                        ${checkmarkHtml}
                    </div>
                </div>
            `;

        }).join('');
    }
    indivContainer.scrollTop = indivContainer.scrollHeight;
}

function openGroupChatRoom(groupId) {
    activeOpenChat = { type: 'group', id: groupId };
    window.activeOpenChat = activeOpenChat;
    const data = window.localDB.get();
    const group = (data.groups || []).find(g => g.id === groupId);
    if (!group) return;

    const titleEl = document.getElementById('group-header-title');
    const subEl = document.getElementById('group-header-sub');
    if (titleEl) titleEl.innerText = group.name;
    if (subEl) subEl.innerText = group.subtitle || `${group.membersCount || 1} members`;

    renderGroupMessages(group.messages || []);
    showScreen('screen-chat-group');
}

function renderGroupMessages(messages) {
    const groupContainer = document.getElementById('group-messages');
    if (!groupContainer) return;
    if (messages.length === 0) {
        groupContainer.innerHTML = `<div style="text-align:center; color:var(--text-muted); font-size:13px; margin:auto;">No group messages yet. Start chatting with your team!</div>`;
    } else {
        groupContainer.innerHTML = messages.map(m => {
            const imgHtml = (m.image_url || m.image) ? `<img src="${m.image_url || m.image}" style="max-width:220px; max-height:220px; border-radius:12px; margin-bottom:6px; display:block; object-fit:cover; border:1px solid var(--border-color);" onerror="this.style.display='none'">` : '';
            let msgContentHtml = m.text || '';
            if (msgContentHtml.startsWith('📄 Document:')) {
                const parts = msgContentHtml.replace('📄 Document:', '').trim();
                msgContentHtml = `
                    <div class="file-attachment-card" style="cursor:pointer;" onclick="openDocumentAttachment('${parts}')">
                        <div class="file-attachment-icon"><i class="fa-solid fa-file-arrow-down"></i></div>
                        <div>
                            <div style="font-weight:600; font-size:13px;">${parts}</div>
                            <div style="font-size:10px; color:var(--accent-lavender);">Tap to View Document</div>
                        </div>
                    </div>
                `;
            } else if (msgContentHtml.includes('Shared Location:')) {
                const urlMatch = msgContentHtml.match(/https:\/\/maps\.google\.com\/\?q=[^\s)]+/);
                const mapUrl = urlMatch ? urlMatch[0] : 'https://maps.google.com';
                const cleanLocText = msgContentHtml.replace(/📍\s*Shared Location:\s*/, '');
                msgContentHtml = `
                    <div style="background:rgba(0,229,255,0.12); border:1px solid #00E5FF; padding:10px 14px; border-radius:12px; cursor:pointer;" onclick="window.open('${mapUrl}', '_blank')">
                        <div style="display:flex; align-items:center; gap:8px; color:#00E5FF; font-weight:700; font-size:13px;">
                            <i class="fa-solid fa-location-dot" style="font-size:16px;"></i> Live Location Pin
                        </div>
                        <div style="font-size:12px; color:white; margin-top:4px;">${cleanLocText}</div>
                        <div style="font-size:10px; color:#4EFEAA; margin-top:6px; font-weight:600;">📍 Tap to Open in Google Maps ➔</div>
                    </div>
                `;
            } else if (msgContentHtml.includes('Contact Card:')) {
                const contactStr = msgContentHtml.replace(/👤\s*Contact Card:\s*/, '');
                const nameMatch = contactStr.match(/^([^(]+)/);
                const phoneMatch = contactStr.match(/\(([^)]+)\)/);
                const cName = nameMatch ? nameMatch[1].trim() : 'Contact';
                const cPhone = phoneMatch ? phoneMatch[1].trim() : '';
                msgContentHtml = `
                    <div style="background:rgba(99,103,255,0.15); border:1px solid var(--primary-indigo); padding:10px 14px; border-radius:12px; cursor:pointer;" onclick="viewContactDetails('${cName}', '${cPhone}')">
                        <div style="display:flex; align-items:center; gap:10px;">
                            <div style="width:36px; height:36px; border-radius:50%; background:var(--primary-indigo); color:white; font-weight:bold; display:flex; align-items:center; justify-content:center; font-size:16px;">${cName.charAt(0)}</div>
                            <div style="flex:1;">
                                <div style="font-weight:700; font-size:14px; color:white;">${cName}</div>
                                <div style="font-size:11px; color:var(--accent-lavender);">${cPhone}</div>
                            </div>
                        </div>
                        <div style="font-size:11px; color:#00E5FF; margin-top:8px; font-weight:600; text-align:right;">View Contact 👤</div>
                    </div>
                `;
            } else if (msgContentHtml.startsWith('₹ BHARAT PAY:')) {
                msgContentHtml = `<div style="background:rgba(0,229,255,0.15); border:1px solid #00E5FF; padding:8px 12px; border-radius:10px; color:#00E5FF; font-weight:bold;"><i class="fa-solid fa-indian-rupee-sign"></i> ${msgContentHtml}</div>`;
            }

            return `
                <div class="message-bubble received">
                    <div style="font-size:11px; font-weight:bold; color:var(--accent-lavender);">${m.sender || 'Member'}</div>
                    ${imgHtml}
                    <div>${msgContentHtml}</div>
                    <div class="message-time">${m.time || 'Just now'}</div>
                </div>
            `;
        }).join('');
    }
    groupContainer.scrollTop = groupContainer.scrollHeight;
}


function openCommunityChatRoom(commId) {
    activeOpenChat = { type: 'community', id: commId };
    window.activeOpenChat = activeOpenChat;
    const data = window.localDB.get();

    const comm = (data.communities || []).find(c => c.id === commId);
    if (!comm) return;

    const titleEl = document.getElementById('community-header-title');
    const subEl = document.getElementById('community-header-sub');
    if (titleEl) titleEl.innerText = comm.name;
    if (subEl) subEl.innerText = comm.subtitle || `${comm.membersCount || 1} members`;

    renderCommunityMessages(comm.messages || []);
    showScreen('screen-chat-community');
}

function renderCommunityMessages(messages) {
    const commContainer = document.getElementById('community-messages');
    if (!commContainer) return;
    if (messages.length === 0) {
        commContainer.innerHTML = `<div style="text-align:center; color:var(--text-muted); font-size:13px; margin:auto;">No community announcements yet. Share an update!</div>`;
    } else {
        commContainer.innerHTML = messages.map(m => {
            const imgHtml = (m.image_url || m.image) ? `<img src="${m.image_url || m.image}" style="max-width:220px; max-height:220px; border-radius:12px; margin-bottom:6px; display:block; object-fit:cover; border:1px solid var(--border-color);" onerror="this.style.display='none'">` : '';
            let msgContentHtml = m.text || '';
            if (msgContentHtml.startsWith('📄 Document:')) {
                const parts = msgContentHtml.replace('📄 Document:', '').trim();
                msgContentHtml = `
                    <div class="file-attachment-card">
                        <div class="file-attachment-icon"><i class="fa-solid fa-file-arrow-down"></i></div>
                        <div>
                            <div style="font-weight:600; font-size:13px;">${parts}</div>
                            <div style="font-size:10px; color:var(--accent-lavender);">Document Attachment</div>
                        </div>
                    </div>
                `;
            } else if (msgContentHtml.startsWith('📍 Shared Location:')) {
                msgContentHtml = `<div style="display:flex; align-items:center; gap:6px; color:#4EFEAA; font-weight:600;"><i class="fa-solid fa-location-dot"></i> ${msgContentHtml}</div>`;
            } else if (msgContentHtml.startsWith('₹ BHARAT PAY:')) {
                msgContentHtml = `<div style="background:rgba(0,229,255,0.15); border:1px solid #00E5FF; padding:8px 12px; border-radius:10px; color:#00E5FF; font-weight:bold;"><i class="fa-solid fa-indian-rupee-sign"></i> ${msgContentHtml}</div>`;
            }

            return `
                <div class="message-bubble received">
                    <div style="font-size:11px; font-weight:bold; color:var(--primary-indigo);">${m.sender || 'Member'} ${m.role ? `[${m.role}]` : ''}</div>
                    ${imgHtml}
                    <div>${msgContentHtml}</div>
                    <div class="message-time">${m.time || 'Just now'}</div>
                </div>
            `;
        }).join('');
    }
    commContainer.scrollTop = commContainer.scrollHeight;
}


async function sendChatMessage(chatType) {
    const inputId = `${chatType === 'individual' ? 'indiv' : chatType}-input`;
    const input = document.getElementById(inputId);
    const text = input ? input.value.trim() : '';

    if (!text && !selectedIndivPhotoData) return;

    if (chatType === 'individual' && activeOpenChat.id) {
        const photoToSend = selectedIndivPhotoData;
        clearIndivMediaAttachment();

        const data = await window.localDB.sendIndividualMessage(activeOpenChat.id, text, photoToSend);
        const chat = (data.individualChats || []).find(c => c.id === activeOpenChat.id);
        if (chat) renderIndividualMessages(chat.messages || []);
    } else if (chatType === 'group' && activeOpenChat.id) {
        const data = await window.localDB.sendGroupMessage(activeOpenChat.id, text);
        const group = (data.groups || []).find(g => g.id === activeOpenChat.id);
        if (group) renderGroupMessages(group.messages || []);
    } else if (chatType === 'community' && activeOpenChat.id) {
        const data = await window.localDB.sendCommunityMessage(activeOpenChat.id, text);
        const comm = (data.communities || []).find(c => c.id === activeOpenChat.id);
        if (comm) renderCommunityMessages(comm.messages || []);
    }

    if (input) input.value = '';
}

function switchMarketTab(tabType) {
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    event.target.classList.add('active');
    const data = window.localDB.get();
    renderMarketplace(tabType, data.marketplace);
}

function renderMarketplace(tabType, marketplace) {
    const container = document.getElementById('market-content');
    if (!container) return;

    if (tabType === 'items') {
        if (!marketplace.items || marketplace.items.length === 0) {
            container.innerHTML = `
                <div style="text-align:center; padding:40px 20px; color:var(--text-muted);">
                    <div style="font-size:36px; margin-bottom:8px;">🛍️</div>
                    <div style="font-weight:600; color:white;">No items listed</div>
                    <div style="font-size:12px; margin-top:4px;">Marketplace is empty right now.</div>
                </div>
            `;
            return;
        }
        container.innerHTML = `<div class="market-grid">` + marketplace.items.map(item => `
            <div class="market-card">
                <img src="${item.image}" class="market-img">
                <div class="market-title">${item.title}</div>
                <div class="market-price">${item.price}</div>
            </div>
        `).join('') + `</div>`;
    } else if (tabType === 'jobs') {
        if (!marketplace.jobs || marketplace.jobs.length === 0) {
            container.innerHTML = `
                <div style="text-align:center; padding:40px 20px; color:var(--text-muted);">
                    <div style="font-size:36px; margin-bottom:8px;">💼</div>
                    <div style="font-weight:600; color:white;">No job listings</div>
                    <div style="font-size:12px; margin-top:4px;">Check back later for new opportunities.</div>
                </div>
            `;
            return;
        }
        container.innerHTML = `<div class="profile-links" style="padding:12px 16px;">` + marketplace.jobs.map(job => `
            <div class="profile-link-card">
                <div>
                    <div style="font-weight:600;">${job.title}</div>
                    <div style="font-size:12px; color:var(--text-muted);">${job.company} • ${job.type}</div>
                </div>
                <button class="btn-primary" style="padding:6px 12px; font-size:12px; width:auto;">Apply</button>
            </div>
        `).join('') + `</div>`;
    } else {
        if (!marketplace.quickJobs || marketplace.quickJobs.length === 0) {
            container.innerHTML = `
                <div style="text-align:center; padding:40px 20px; color:var(--text-muted);">
                    <div style="font-size:36px; margin-bottom:8px;">⚡</div>
                    <div style="font-weight:600; color:white;">No quick jobs</div>
                    <div style="font-size:12px; margin-top:4px;">No micro-tasks currently posted.</div>
                </div>
            `;
            return;
        }
        container.innerHTML = `<div class="profile-links" style="padding:12px 16px;">` + marketplace.quickJobs.map(q => `
            <div class="profile-link-card">
                <div>
                    <div style="font-weight:600;">${q.title}</div>
                    <div style="font-size:12px; color:var(--accent-lavender);">${q.budget}</div>
                </div>
                <button class="btn-primary" style="padding:6px 12px; font-size:12px; width:auto;">Bid</button>
            </div>
        `).join('') + `</div>`;
    }
}

function renderProfile(user) {
    if (!user) return;
    const imgEl = document.getElementById('profile-img');
    if (imgEl) imgEl.src = user.avatar || 'logo.png';
    
    const nameEl = document.getElementById('profile-name');
    if (nameEl) nameEl.innerText = user.name || 'User';

    const handleEl = document.getElementById('profile-handle');
    if (handleEl) handleEl.innerText = '@' + (user.username || 'user');

    const bioEl = document.getElementById('profile-bio');
    if (bioEl) bioEl.innerText = user.bio || 'No bio added yet.';

    const emailEl = document.getElementById('profile-email');
    if (emailEl) emailEl.innerText = user.email || 'Not specified';

    const phoneEl = document.getElementById('profile-phone');
    if (phoneEl) phoneEl.innerText = user.phone || 'Not specified';

    const dobEl = document.getElementById('profile-dob');
    if (dobEl) dobEl.innerText = user.dob || 'Not specified';

    const postsEl = document.getElementById('stat-posts');
    if (postsEl) postsEl.innerText = user.postsCount || 0;

    const followersEl = document.getElementById('stat-followers');
    if (followersEl) followersEl.innerText = user.followersCount || 0;

    const followingEl = document.getElementById('stat-following');
    if (followingEl) followingEl.innerText = user.followingCount || 0;
}

function editProfilePrompt() {
    const data = window.localDB.get();
    const currentUser = data.currentUser || {};
    showCustomPrompt({
        title: "✏️ Edit Profile",
        subtitle: "Update your profile details",
        fields: [
            { label: "Display Name", placeholder: "Enter full name", value: currentUser.name || '' },
            { label: "Bio / Status", placeholder: "Write a short bio...", value: currentUser.bio || '', multiline: true }
        ],
        confirmText: "Save Profile",
        onConfirm: function(values) {
            const [newName, newBio] = Array.isArray(values) ? values : [values, ''];
            if (newName || newBio) {
                window.localDB.updateProfile({
                    name: newName || currentUser.name,
                    bio: newBio || currentUser.bio
                });
                renderAll();
                showCustomAlert('Profile updated successfully!', 'Edit Profile');
            }
        }
    });
}

/* ==========================================
 * NEARBY DISCOVERY CONTROLLER
 * ========================================== */

let currentNearbyRadius = 1;

function switchNearbyRadius(radius) {
    currentNearbyRadius = radius;
    document.querySelectorAll('#screen-nearby .tab-btn').forEach(btn => btn.classList.remove('active'));
    const tabEl = document.getElementById(`nearby-tab-${radius}km`);
    if (tabEl) tabEl.classList.add('active');
    renderNearbyUsers();
}

function renderNearbyUsers() {
    const container = document.getElementById('nearby-users-container');
    if (!container) return;

    const data = window.localDB.get();
    const currentUser = data.currentUser || {};
    const regUsers = data.registeredUsers || [];

    // Filter users (exclude self)
    const nearbyList = regUsers.filter(u => 
        u.id !== currentUser.id && 
        u.username !== currentUser.username &&
        u.phone !== currentUser.phone
    );

    const searchInput = document.getElementById('nearby-search-input');
    const query = searchInput ? searchInput.value.toLowerCase().trim() : '';

    const filtered = nearbyList.filter(u => {
        if (!query) return true;
        const name = String(u.name || '').toLowerCase();
        const username = String(u.username || '').toLowerCase();
        const phone = String(u.phone || '').toLowerCase();
        return name.includes(query) || username.includes(query) || phone.includes(query);
    });

    if (filtered.length === 0) {
        container.innerHTML = `
            <div style="text-align:center; padding:40px 20px; color:var(--text-muted);">
                <div style="font-size:36px; margin-bottom:8px;">📍</div>
                <div style="font-weight:600; color:white;">No nearby users found within ${currentNearbyRadius} km</div>
                <div style="font-size:12px; margin-top:4px;">Try expanding your search radius to 5 km or 10 km!</div>
            </div>
        `;
        return;
    }

    // Generate distances proportional to index for demo
    container.innerHTML = filtered.map((user, idx) => {
        const baseDist = (0.2 + (idx * 0.45) % currentNearbyRadius).toFixed(1);
        const distText = `${baseDist} km away`;
        const avatarSrc = (user.avatar && user.avatar !== 'logo.png') ? user.avatar : 'logo.png';
        const contactTarget = user.phone || user.username || user.id;

        return `
            <div class="profile-link-card" style="display:flex; align-items:center; justify-content:space-between; padding:12px 14px; background:var(--surface-dark); border:1px solid var(--border-color); border-radius:14px;">
                <div style="display:flex; align-items:center; gap:12px;">
                    <div style="position:relative;">
                        <img src="${avatarSrc}" style="width:48px; height:48px; border-radius:50%; object-fit:cover; border:2px solid var(--primary-indigo);" onerror="this.src='logo.png'">
                        <div style="position:absolute; bottom:2px; right:2px; width:10px; height:10px; border-radius:50%; background:#4EFEAA; border:2px solid var(--surface-dark);"></div>
                    </div>
                    <div>
                        <div style="font-weight:700; color:white; font-size:14px;">${user.name || user.username}</div>
                        <div style="font-size:12px; color:var(--accent-lavender); font-weight:600;">@${user.username || 'user'} • <span style="color:#4EFEAA;">📍 ${distText}</span></div>
                        <div style="font-size:11px; color:var(--text-muted); margin-top:2px;">${user.bio || 'Active on BharatConnect'}</div>
                    </div>
                </div>
                <button class="btn-primary" style="padding:6px 14px; font-size:12px; width:auto; border-radius:20px; font-weight:600;" onclick="startChatFromNearby('${contactTarget}')">💬 Message</button>
            </div>
        `;
    }).join('');
}

function filterNearbyUsers() {
    renderNearbyUsers();
}

function startChatFromNearby(contactTarget) {
    if (!window.localDB) return;
    const chat = window.localDB.addIndividualContact(contactTarget);
    if (chat && chat.id) {
        openIndividualChatRoom(chat.id);
    }
}

/* ==========================================
 * REALTIME WEBSOCKET & UI RENDER BRIDGE
 * ========================================== */

window.renderAll = renderAll;
window.renderIndividualMessages = renderIndividualMessages;
window.renderIndividualChats = function() {
    if (typeof renderAll === 'function') {
        renderAll();
    }
};

if (window.connectionManager) {
    window.connectionManager.on('message.new', function(eventFrame) {
        if (!eventFrame || !eventFrame.data) return;
        const msg = eventFrame.data;
        if (window.localDB && window.localDB.ingestServerMessage) {
            const ingested = window.localDB.ingestServerMessage(msg);
            if (ingested) {
                window.renderIndividualChats();

                // Force instant UI refresh if sitting on open chat room
                if (window.activeOpenChat && window.activeOpenChat.id) {
                    const data = window.localDB.get();
                    const chat = (data.individualChats || []).find(c => c.id === window.activeOpenChat.id || c.userId === window.activeOpenChat.id || c.phone === window.activeOpenChat.id);
                    if (chat && chat.messages) {
                        renderIndividualMessages(chat.messages);
                    }
                }

                const data = window.localDB.get();
                const myId = String(data.currentUser.id || '').toLowerCase();
                const myUsername = String(data.currentUser.username || '').toLowerCase();
                const myPhone = String(data.currentUser.phone || '').replace(/\D/g, '').replace(/^91(?=\d{10}$)/, '').replace(/^0+/, '');
                const smSender = String(msg.sender_id || '').toLowerCase();
                const isMe = (smSender === myId || smSender === myUsername || (myPhone && smSender && smSender.endsWith(myPhone)));
                if (!isMe) {
                    showInAppToast({
                        title: `💬 New message from ${msg.sender_name || 'Contact'}`,
                        message: msg.text || '📷 Photo',
                        avatar: 'logo.png',
                        chatId: msg.chat_id
                    });
                }
            }
        }
    });

    window.connectionManager.on('message.status_update', function(eventFrame) {
        if (!eventFrame || !eventFrame.data) return;
        const statusData = eventFrame.data;
        if (window.localDB) {
            const data = window.localDB.get();
            let found = false;
            (data.individualChats || []).forEach(chat => {
                if (chat.messages) {
                    chat.messages.forEach(m => {
                        if (m.id === statusData.id || (statusData.client_message_id && m.client_message_id === statusData.client_message_id)) {
                            m.status = statusData.status;
                            found = true;
                        }
                    });
                }
            });
            if (found) {
                window.localDB.save(data);
                if (window.activeOpenChat && window.activeOpenChat.id === statusData.chat_id) {
                    const chat = data.individualChats.find(c => c.id === statusData.chat_id);
                    if (chat) renderIndividualMessages(chat.messages);
                }
            }
        }
    });
}

// High-frequency reactive auto-refresh for active open chat room (400ms ticker)
setInterval(function() {
    if (window.activeOpenChat && window.activeOpenChat.id && window.localDB && typeof renderIndividualMessages === 'function') {
        const data = window.localDB.get();
        const chat = (data.individualChats || []).find(c => c.id === window.activeOpenChat.id || c.userId === window.activeOpenChat.id || c.phone === window.activeOpenChat.id);
        if (chat && chat.messages) {
            const indivContainer = document.getElementById('indiv-messages');
            if (indivContainer) {
                const currentBubbleCount = indivContainer.querySelectorAll('.message-bubble').length;
                if (currentBubbleCount !== chat.messages.length) {
                    renderIndividualMessages(chat.messages);
                }
            }
        }
    }
}, 400);

/* ==========================================
 * WHATSAPP ATTACHMENT SHEET & EMOJI PICKER
 * ========================================== */

window.currentActiveEmojiInput = 'indiv-input';
window.currentAttachmentChatType = 'individual';

const EMOJI_DATA = {
    smileys: ['😊','😂','😃','😄','😁','😆','🥹','😅','🤡','🤣','🙃','😉','😇','🥰','😍','🤩','😘','😗','😚','😋','😛','😜','🤪','🧐','🤓','😎','🥸','🥳','😏','😒','😞','😔','😟','😕','🙁'],
    gestures: ['👍','👎','👌','🤌','🤏','✌️','🤞','🫰','🤟','🤘','🤙','👈','👉','👆','🖕','👇','☝️','🫵','🖐️','✋','🤚','🖖','🫱','🫲','🫳','🫴','🤝','👏','🙌','🫶','🤲','🙏','✍️'],
    india: ['🇮🇳','🪔','🕌','🛕','🪷','🐘','🐅','🦚','🏏','☕','🫓','🍛','🥭','📜','🎨','🎬','🎼','🎖️','🏆','🎯','🚩','✨','🔥','🎉','🎈','💡'],
    celebration: ['🎉','🥳','🎈','🎊','🎂','🎁','🎗️','🎟️','🍾','🥂','🍺','🍻','🍹','🍸','🍾','🍿','🎆','🎇','🏮','<ctrl42>','✨','🌟','💖','❤️','💙','🧡','💚','💛','💜'],
    symbols: ['🔥','💯','⚡','✨','🌟','⭐','💥','💫','💦','💨','💬','📢','🔔','🎵','🎶','📢','🎯','📌','📍','🚀','🛸','🛸','🚗','🛺','🚲','📱','💻','📷','🛡️','🔒']
};

function toggleAttachmentSheet(chatType) {
    if (chatType) window.currentAttachmentChatType = chatType;
    const overlay = document.getElementById('attachment-modal-overlay');
    if (!overlay) return;
    if (overlay.style.display === 'flex') {
        overlay.style.display = 'none';
    } else {
        closeEmojiPicker();
        overlay.style.display = 'flex';
    }
}

function closeAttachmentSheet(e) {
    const overlay = document.getElementById('attachment-modal-overlay');
    if (overlay) overlay.style.display = 'none';
}

function triggerAttachment(type) {
    closeAttachmentSheet();
    const chatType = window.currentAttachmentChatType || 'individual';

    if (type === 'gallery') {
        const input = document.getElementById('attachment-gallery-input');
        if (input) input.click();
    } else if (type === 'camera') {
        const input = document.getElementById('attachment-camera-input');
        if (input) input.click();
    } else if (type === 'document') {
        const input = document.getElementById('attachment-document-input');
        if (input) input.click();
    } else if (type === 'location') {
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(
                pos => {
                    const lat = pos.coords.latitude.toFixed(4);
                    const lng = pos.coords.longitude.toFixed(4);
                    const msgText = `📍 Shared Location: https://maps.google.com/?q=${lat},${lng}`;
                    sendCustomChatMessage(chatType, msgText);
                },
                err => {
                    sendCustomChatMessage(chatType, "📍 Shared Location: New Delhi, India (28.6139° N, 77.2090° E)");
                }
            );
        } else {
            sendCustomChatMessage(chatType, "📍 Shared Location: New Delhi, India (28.6139° N, 77.2090° E)");
        }
    } else if (type === 'contact') {
        const data = window.localDB ? window.localDB.get() : {};
        const curUser = data.currentUser || { display_name: "User", phone: "+91 98765 43210" };
        const msgText = `👤 Contact Card: ${curUser.display_name} (${curUser.phone || curUser.username || 'Member'})`;
        sendCustomChatMessage(chatType, msgText);
    } else if (type === 'poll') {
        const pollQuestion = prompt("Enter Poll Question:", "Which feature should we build next?");
        if (pollQuestion) {
            const msgText = `📊 POLL: ${pollQuestion}\n1️⃣ Option A\n2️⃣ Option B\n(Tap to vote)`;
            sendCustomChatMessage(chatType, msgText);
        }
    } else if (type === 'payment') {
        const amt = prompt("Enter UPI Transfer Amount (₹):", "100");
        if (amt) {
            const msgText = `₹ BHARAT PAY: Sent ₹${amt} via UPI Transfer • Successful ✓`;
            sendCustomChatMessage(chatType, msgText);
        }
    } else if (type === 'event') {
        const evtName = prompt("Enter Event Title:", "Team Meeting");
        if (evtName) {
            const msgText = `📅 EVENT: ${evtName}\nTime: Today 5:00 PM • Live on BharatConnect`;
            sendCustomChatMessage(chatType, msgText);
        }
    } else if (type === 'ai') {
        const promptText = prompt("Enter AI Image Prompt:", "Cyberpunk Indian Warrior 3D Render");
        if (promptText) {
            const placeholderImg = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80";
            sendCustomChatMessage(chatType, `🇮🇳 AI Image: "${promptText}"`, placeholderImg);
        }
    }
}

function handleAttachmentFileSelect(e, type) {
    const file = e.target.files[0];
    if (!file) return;
    const chatType = window.currentAttachmentChatType || 'individual';

    if (file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = function(evt) {
            sendCustomChatMessage(chatType, `📷 ${file.name}`, evt.target.result);
        };
        reader.readAsDataURL(file);
    } else {
        const sizeMb = (file.size / (1024 * 1024)).toFixed(1);
        const docText = `📄 Document: ${file.name} (${sizeMb} MB)`;
        sendCustomChatMessage(chatType, docText);
    }
    e.target.value = '';
}

function sendCustomChatMessage(chatType, text, imageUrl = null) {
    if (chatType === 'individual') {
        window.localDB.sendIndividualMessage(window.activeOpenChat.id, text, imageUrl).then(() => {
            const data = window.localDB.get();
            const chat = (data.individualChats || []).find(c => c.id === window.activeOpenChat.id);
            if (chat) renderIndividualMessages(chat.messages);
        });
    } else if (chatType === 'group') {
        window.localDB.sendGroupMessage(window.activeOpenChat.id, text).then(() => {
            const data = window.localDB.get();
            const group = (data.groups || []).find(g => g.id === window.activeOpenChat.id);
            if (group) renderGroupMessages(group.messages);
        });
    } else if (chatType === 'community') {
        window.localDB.sendCommunityMessage(window.activeOpenChat.id, text).then(() => {
            const data = window.localDB.get();
            const comm = (data.communities || []).find(c => c.id === window.activeOpenChat.id);
            if (comm) renderCommunityMessages(comm.messages);
        });
    }
}

function toggleEmojiPicker(inputId) {
    if (inputId) window.currentActiveEmojiInput = inputId;
    const modal = document.getElementById('emoji-picker-modal');
    if (!modal) return;
    if (modal.style.display === 'flex') {
        modal.style.display = 'none';
    } else {
        closeAttachmentSheet();
        modal.style.display = 'flex';
        switchEmojiTab('smileys');
    }
}

function closeEmojiPicker() {
    const modal = document.getElementById('emoji-picker-modal');
    if (modal) modal.style.display = 'none';
}

function switchEmojiTab(category) {
    const container = document.getElementById('emoji-grid-container');
    if (!container) return;
    const emojis = EMOJI_DATA[category] || EMOJI_DATA.smileys;
    container.innerHTML = emojis.map(em => `<div class="emoji-item" onclick="insertEmoji('${em}')">${em}</div>`).join('');

    const btns = document.querySelectorAll('.emoji-tab-btn');
    if (btns && btns.length > 0 && event && event.target) {
        btns.forEach(btn => btn.classList.remove('active'));
        event.target.classList.add('active');
    }
}

function insertEmoji(emoji) {
    const inputId = window.currentActiveEmojiInput || 'indiv-input';
    const input = document.getElementById(inputId);
    if (input) {
        input.value += emoji;
        input.focus();
    }
}

function openAttachmentScreen(type) {
    closeAttachmentSheet();
    window.currentAttachmentType = type;
    const screenId = `screen-attach-${type}`;
    if (type === 'camera') {
        initCameraScreenView();
    } else if (type === 'gallery') {
        initGalleryScreenView();
    } else if (type === 'document') {
        initDocumentScreenView();
    } else if (type === 'location') {
        initLocationScreenView();
    } else if (type === 'contact') {
        initContactScreenView();
    }
    showScreen(screenId);
}

function closeAttachmentScreen() {
    stopLiveCameraStream();
    if (window.activeOpenChat && window.activeOpenChat.type) {
        showScreen(`screen-chat-${window.activeOpenChat.type === 'individual' ? 'indiv' : window.activeOpenChat.type}`);
    } else {
        showScreen('screen-chat-indiv');
    }
}

function initCameraScreenView() {
    const placeholder = document.getElementById('camera-placeholder-view');
    const video = document.getElementById('camera-live-video');
    if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        navigator.mediaDevices.getUserMedia({ video: { facingMode: window.cameraFacing || 'environment' } })
            .then(stream => {
                window.activeCameraStream = stream;
                if (video) {
                    video.srcObject = stream;
                    video.style.display = 'block';
                }
                if (placeholder) placeholder.style.display = 'none';
            })
            .catch(() => {
                if (video) video.style.display = 'none';
                if (placeholder) placeholder.style.display = 'block';
            });
    } else {
        if (video) video.style.display = 'none';
        if (placeholder) placeholder.style.display = 'block';
    }

    const strip = document.getElementById('camera-recent-strip');
    if (strip) {
        const dummyPhotos = [
            "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=150&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=150&auto=format&fit=crop&q=80"
        ];
        strip.innerHTML = dummyPhotos.map(p => `<img src="${p}" style="width:48px; height:48px; border-radius:8px; object-fit:cover; cursor:pointer;" onclick="selectCameraStripPhoto('${p}')">`).join('');
    }
}

function stopLiveCameraStream() {
    if (window.activeCameraStream) {
        window.activeCameraStream.getTracks().forEach(track => track.stop());
        window.activeCameraStream = null;
    }
}

function captureCameraSnapshot() {
    const video = document.getElementById('camera-live-video');
    let photoData = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80";
    if (video && video.style.display !== 'none' && video.videoWidth) {
        const canvas = document.createElement('canvas');
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(video, 0, 0);
        photoData = canvas.toDataURL('image/jpeg');
    }
    stopLiveCameraStream();
    closeAttachmentScreen();
    const chatType = (window.activeOpenChat && window.activeOpenChat.type) || 'individual';
    sendCustomChatMessage(chatType, '📷 Camera Photo', photoData);
}

function selectCameraStripPhoto(url) {
    stopLiveCameraStream();
    closeAttachmentScreen();
    const chatType = (window.activeOpenChat && window.activeOpenChat.type) || 'individual';
    sendCustomChatMessage(chatType, '📷 Photo', url);
}

function toggleCameraFlash() {
    window.cameraFlashOn = !window.cameraFlashOn;
    const btn = document.getElementById('camera-flash-btn');
    if (btn) btn.innerHTML = window.cameraFlashOn ? `<i class="fa-solid fa-bolt" style="font-size:16px; color:#FFD700;"></i>` : `<i class="fa-solid fa-bolt-slash" style="font-size:16px;"></i>`;
}

function switchCameraFacing() {
    window.cameraFacing = window.cameraFacing === 'user' ? 'environment' : 'user';
    stopLiveCameraStream();
    initCameraScreenView();
}

function setCameraMode(mode) {
    ['video', 'photo', 'videonote'].forEach(m => {
        const el = document.getElementById(`cam-mode-${m}`);
        if (el) {
            if (m === mode) {
                el.style.color = 'white';
                el.style.background = 'rgba(255,255,255,0.2)';
                el.style.padding = '2px 14px';
                el.style.borderRadius = '12px';
            } else {
                el.style.color = 'var(--text-muted)';
                el.style.background = 'transparent';
                el.style.padding = '0';
            }
        }
    });
}

window.selectedGalleryPhotos = [];
function initGalleryScreenView() {
    window.selectedGalleryPhotos = [];
    updateGallerySelectedCount();
    const container = document.getElementById('gallery-grid-view');
    if (!container) return;

    const samplePhotos = [
        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1518770660439-4636190af475?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1472214103451-9374bd1c798e?w=400&auto=format&fit=crop&q=80"
    ];

    container.innerHTML = samplePhotos.map((url, idx) => `
        <div style="position:relative; aspect-ratio:1; cursor:pointer;" onclick="toggleGalleryPhotoSelect(${idx}, '${url}')">
            <img src="${url}" style="width:100%; height:100%; object-fit:cover; border-radius:6px;">
            <div id="gallery-badge-${idx}" style="position:absolute; top:6px; right:6px; width:22px; height:22px; border-radius:50%; border:2px solid white; background:rgba(0,0,0,0.4); color:white; font-size:11px; font-weight:bold; display:flex; align-items:center; justify-content:center;"></div>
        </div>
    `).join('');
}

function toggleGalleryPhotoSelect(idx, url) {
    const badge = document.getElementById(`gallery-badge-${idx}`);
    const existingIndex = window.selectedGalleryPhotos.findIndex(item => item.idx === idx);
    if (existingIndex > -1) {
        window.selectedGalleryPhotos.splice(existingIndex, 1);
        if (badge) {
            badge.style.background = 'rgba(0,0,0,0.4)';
            badge.innerText = '';
        }
    } else {
        window.selectedGalleryPhotos.push({ idx, url });
        if (badge) {
            badge.style.background = '#00E676';
            badge.innerText = window.selectedGalleryPhotos.length;
        }
    }
    updateGallerySelectedCount();
}

function updateGallerySelectedCount() {
    const countEl = document.getElementById('gallery-selected-count');
    if (countEl) {
        const count = window.selectedGalleryPhotos.length;
        countEl.innerText = count > 0 ? `${count} photo${count > 1 ? 's' : ''} selected` : 'Tap photos to select';
    }
}

function sendSelectedGalleryPhotos() {
    const caption = document.getElementById('gallery-caption-input') ? document.getElementById('gallery-caption-input').value.trim() : '';
    if (window.selectedGalleryPhotos.length === 0) {
        window.selectedGalleryPhotos.push({ url: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=400&auto=format&fit=crop&q=80" });
    }
    closeAttachmentScreen();
    const chatType = (window.activeOpenChat && window.activeOpenChat.type) || 'individual';
    window.selectedGalleryPhotos.forEach((item, i) => {
        const text = (i === 0 && caption) ? `📷 ${caption}` : '📷 Photo';
        sendCustomChatMessage(chatType, text, item.url);
    });
}

function initDocumentScreenView() {
    const container = document.getElementById('recent-documents-list');
    if (!container) return;

    const sampleDocs = [
        { name: "1 (1).jpeg", size: "140 KB", date: "01/10/2025" },
        { name: "1. Students Undertaking.docx", size: "16 KB", date: "29/05/2025" },
        { name: "1.jpeg", size: "207 KB", date: "01/10/2025" },
        { name: "1000014417.png", size: "1.2 MB", date: "14/01/2025" },
        { name: "BharatConnect_Project_Brief.pdf", size: "2.4 MB", date: "Today" },
        { name: "BharatConnect.apk", size: "8.65 MB", date: "Today" }
    ];

    container.innerHTML = sampleDocs.map(doc => `
        <div style="display:flex; align-items:center; gap:14px; padding:10px 12px; background:var(--surface-dark); border:1px solid var(--border-color); border-radius:12px; cursor:pointer;" onclick="selectDocumentToSend('${doc.name}', '${doc.size}')">
            <div style="width:40px; height:40px; border-radius:8px; background:rgba(99,103,255,0.15); border:1px solid var(--primary-indigo); display:flex; align-items:center; justify-content:center; font-size:18px; color:var(--accent-lavender);">📄</div>
            <div style="flex:1;">
                <div style="font-weight:600; font-size:14px; color:white;">${doc.name}</div>
                <div style="font-size:12px; color:var(--text-muted);">${doc.size} • ${doc.date}</div>
            </div>
        </div>
    `).join('');
}

// WhatsApp-Style Client-Side Image/Video Compression Engine (HTML5 Canvas)
function compressMediaFile(file, maxDimension = 1280, quality = 0.75) {
    return new Promise((resolve) => {
        if (!file || !file.type.startsWith('image/')) {
            resolve({ file: file, isCompressed: false });
            return;
        }

        const reader = new FileReader();
        reader.onload = (e) => {
            const img = new Image();
            img.onload = () => {
                let width = img.width;
                let height = img.height;

                if (width > maxDimension || height > maxDimension) {
                    if (width > height) {
                        height = Math.round((height * maxDimension) / width);
                        width = maxDimension;
                    } else {
                        width = Math.round((width * maxDimension) / height);
                        height = maxDimension;
                    }
                }

                const canvas = document.createElement('canvas');
                canvas.width = width;
                canvas.height = height;
                const ctx = canvas.getContext('2d');
                ctx.drawImage(img, 0, 0, width, height);

                const dataUrl = canvas.toDataURL('image/jpeg', quality);
                resolve({
                    dataUrl: dataUrl,
                    originalSize: file.size,
                    compressedSize: Math.round((dataUrl.length * 3) / 4),
                    isCompressed: true
                });
            };
            img.onerror = () => resolve({ file: file, isCompressed: false });
            img.src = e.target.result;
        };
        reader.onerror = () => resolve({ file: file, isCompressed: false });
        reader.readAsDataURL(file);
    });
}

function handleAttachmentFileSelect(e, type) {
    const files = e.target.files;
    if (!files || files.length === 0) return;
    const chatType = window.currentAttachmentChatType || (window.activeOpenChat && window.activeOpenChat.type) || 'individual';

    Array.from(files).forEach(file => {
        if (file.type.startsWith('image/')) {
            compressMediaFile(file, 1280, 0.75).then(res => {
                const imgData = res.dataUrl || res.file;
                sendCustomChatMessage(chatType, `📷 ${file.name}`, imgData);
            });
        } else {
            const sizeMb = (file.size / (1024 * 1024)).toFixed(1);
            const docText = `📄 Document: ${file.name} (${sizeMb} MB)`;
            sendCustomChatMessage(chatType, docText);
        }
    });
    e.target.value = '';
}

function openDocumentAttachment(docName) {
    alert(`📄 Opening document: ${docName}\nDownloading file to device storage...`);
}

function loadLeafletDynamically(callback) {
    if (window.L) {
        if (callback) callback();
        return;
    }
    if (!document.getElementById('leaflet-css-dyn')) {
        const css = document.createElement('link');
        css.id = 'leaflet-css-dyn';
        css.rel = 'stylesheet';
        css.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
        document.head.appendChild(css);
    }
    if (!document.getElementById('leaflet-js-dyn')) {
        const script = document.createElement('script');
        script.id = 'leaflet-js-dyn';
        script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
        script.onload = () => { if (callback) callback(); };
        script.onerror = () => { if (callback) callback(); };
        document.head.appendChild(script);
    } else if (callback) {
        callback();
    }
}

function initLocationScreenView() {
    const dialog = document.getElementById('gps-status-dialog');
    const dialogText = document.getElementById('gps-dialog-text');
    if (dialog) dialog.style.display = 'flex';
    if (dialogText) dialogText.innerText = 'Acquiring high-precision GPS satellite location...';

    let defaultLat = 28.6139;
    let defaultLng = 77.2090;

    loadLeafletDynamically(() => {
        const mapContainer = document.getElementById('location-leaflet-map');
        if (mapContainer && window.L) {
            if (window.currentLeafletMap) {
                window.currentLeafletMap.remove();
                window.currentLeafletMap = null;
            }

            window.currentLeafletMap = L.map('location-leaflet-map').setView([defaultLat, defaultLng], 14);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19,
                attribution: '© OpenStreetMap'
            }).addTo(window.currentLeafletMap);

            window.currentGPSMarker = L.marker([defaultLat, defaultLng]).addTo(window.currentLeafletMap)
                .bindPopup('Your Current Location')
                .openPopup();
        }
    });

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                const lat = pos.coords.latitude;
                const lng = pos.coords.longitude;
                const accuracy = Math.round(pos.coords.accuracy || 8);
                window.currentUserGPS = { lat, lng, accuracy };

                const accEl = document.getElementById('gps-accuracy-text');
                if (accEl) accEl.innerText = `Accurate to ${accuracy} meters`;

                if (window.currentLeafletMap) {
                    window.currentLeafletMap.setView([lat, lng], 15);
                    if (window.currentGPSMarker) {
                        window.currentGPSMarker.setLatLng([lat, lng]);
                        window.currentGPSMarker.getPopup().setContent(`📍 GPS Location<br>Lat: ${lat.toFixed(4)}, Lng: ${lng.toFixed(4)}`).openPopup();
                    }
                }
                if (dialog) dialog.style.display = 'none';
            },
            (err) => {
                if (dialogText) dialogText.innerText = 'GPS location unavailable. Using network triangulation location.';
                window.currentUserGPS = { lat: defaultLat, lng: defaultLng, accuracy: 15 };
            },
            { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
        );
    }

    const placesContainer = document.getElementById('nearby-places-list');
    if (placesContainer) {
        const places = [
            { name: "Current GPS Location", sub: "Live Pinpoint Satellite Location" },
            { name: "New Delhi Railway Station", sub: "Paharganj, New Delhi" },
            { name: "Connaught Place", sub: "Inner Circle, New Delhi" },
            { name: "BharatConnect Headquarters", sub: "Tech Hub Sector 62, Noida" },
            { name: "Cyber Hub Gurugram", sub: "DLF Phase 2, Gurugram" }
        ];
        placesContainer.innerHTML = places.map(p => `
            <div style="display:flex; align-items:center; gap:14px; padding:10px 12px; background:var(--surface-dark); border:1px solid var(--border-color); border-radius:12px; cursor:pointer;" onclick="selectLocationPlace('${p.name}')">
                <div style="width:36px; height:36px; border-radius:50%; background:rgba(0,229,255,0.12); display:flex; align-items:center; justify-content:center; color:#00E5FF; font-size:16px;"><i class="fa-solid fa-location-dot"></i></div>
                <div>
                    <div style="font-weight:600; font-size:14px; color:white;">${p.name}</div>
                    <div style="font-size:11px; color:var(--text-muted);">${p.sub}</div>
                </div>
            </div>
        `).join('');
    }
}

function dismissGPSDialog() {
    const dialog = document.getElementById('gps-status-dialog');
    if (dialog) dialog.style.display = 'none';
}

function sendCurrentGPSLocation() {
    closeAttachmentScreen();
    const gps = window.currentUserGPS || { lat: 28.6139, lng: 77.2090, accuracy: 10 };
    const chatType = (window.activeOpenChat && window.activeOpenChat.type) || 'individual';
    const mapsLink = `https://maps.google.com/?q=${gps.lat.toFixed(6)},${gps.lng.toFixed(6)}`;
    sendCustomChatMessage(chatType, `📍 Shared Location: ${mapsLink} (Accurate to ${gps.accuracy}m)`);
}

function selectLocationPlace(name) {
    closeAttachmentScreen();
    const gps = window.currentUserGPS || { lat: 28.6139, lng: 77.2090 };
    const chatType = (window.activeOpenChat && window.activeOpenChat.type) || 'individual';
    const mapsLink = `https://maps.google.com/?q=${gps.lat.toFixed(6)},${gps.lng.toFixed(6)}`;
    sendCustomChatMessage(chatType, `📍 Shared Location: ${name} (${mapsLink})`);
}

function refreshLocationMap() {
    initLocationScreenView();
}

window.selectedContacts = [];
function initContactScreenView() {
    window.selectedContacts = [];
    updateContactSelectedCount();
    const container = document.getElementById('contacts-picker-list');
    if (!container) return;

    const data = window.localDB ? window.localDB.get() : {};
    let usersList = (data.registeredUsers || []).map(u => ({
        id: u.id || u.username,
        name: u.name || u.display_name || u.username,
        phone: u.phone || '+91 98765 43210',
        avatar: u.avatar || null
    }));

    if (usersList.length < 3) {
        usersList = [
            { id: "c1", name: "Vipin Vishwakarma", phone: "+91 98765 43210", avatar: "logo.png" },
            { id: "c2", name: "Mummy", phone: "+91 91234 56789", avatar: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80" },
            { id: "c3", name: "Rohit Palm Dipak", phone: "+91 99887 76655", avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80" },
            { id: "c4", name: "Harish Sharma", phone: "+91 94455 66778", avatar: null },
            { id: "c5", name: "Ananya Sharma", phone: "+91 70585 67004", avatar: null },
            { id: "c6", name: "Rahul Verma", phone: "+91 84324 84785", avatar: null }
        ];
    }

    container.innerHTML = usersList.map(c => `
        <div style="display:flex; align-items:center; gap:14px; padding:10px 12px; background:var(--surface-dark); border:1px solid var(--border-color); border-radius:12px; cursor:pointer;" onclick="toggleContactSelect('${c.id}', '${c.name}', '${c.phone}')">
            ${c.avatar && c.avatar.startsWith('http') ? `<img src="${c.avatar}" style="width:44px; height:44px; border-radius:50%; object-fit:cover;">` : `<div style="width:44px; height:44px; border-radius:50%; background:var(--primary-indigo); color:white; display:flex; align-items:center; justify-content:center; font-weight:bold; font-size:18px;">${(c.name || 'U').charAt(0)}</div>`}
            <div style="flex:1;">
                <div style="font-weight:600; font-size:15px; color:white;">${c.name}</div>
                <div style="font-size:12px; color:var(--text-muted);">${c.phone}</div>
            </div>
            <div id="contact-chk-${c.id}" style="width:22px; height:22px; border-radius:50%; border:2px solid var(--border-color); display:flex; align-items:center; justify-content:center; color:white; font-size:12px; font-weight:bold;"></div>
        </div>
    `).join('');
}

function fetchDevicePhonebookContacts() {
    if ('contacts' in navigator && 'select' in navigator.contacts) {
        navigator.contacts.select(['name', 'tel'], { multiple: true })
            .then(contacts => {
                if (contacts && contacts.length > 0) {
                    const formatted = contacts.map((c, i) => ({
                        id: 'dev_c_' + i,
                        name: (c.name && c.name[0]) || 'Contact',
                        phone: (c.tel && c.tel[0]) || '+91 98765 43210'
                    }));
                    window.selectedContacts = formatted;
                    sendSelectedContacts();
                }
            })
            .catch(() => initContactScreenView());
    } else {
        alert("📖 Phonebook Sync Active: Displaying device contacts from local phonebook!");
        initContactScreenView();
    }
}

function viewContactDetails(name, phone) {
    window.currentViewedContact = { name, phone };
    const modal = document.getElementById('view-contact-modal');
    const nameEl = document.getElementById('contact-modal-name');
    const phoneEl = document.getElementById('contact-modal-phone');
    const initEl = document.getElementById('contact-modal-initials');

    if (nameEl) nameEl.innerText = name || 'Contact';
    if (phoneEl) phoneEl.innerText = phone || '+91 98765 43210';
    if (initEl) initEl.innerText = (name || 'C').charAt(0);

    if (modal) modal.style.display = 'flex';
}

function closeContactModal(e) {
    if (e && e.target && e.target.id !== 'view-contact-modal' && !e.target.closest('button')) return;
    const modal = document.getElementById('view-contact-modal');
    if (modal) modal.style.display = 'none';
}

function startDirectChatFromContact() {
    closeContactModal();
    if (window.currentViewedContact) {
        const phoneKey = window.currentViewedContact.phone.replace(/\D/g, '');
        openIndividualChatRoom(phoneKey || 'c-individual');
    }
}

function callContactPhone() {
    if (window.currentViewedContact && window.currentViewedContact.phone) {
        window.location.href = `tel:${window.currentViewedContact.phone}`;
    }
}

function openWhatsAppContact() {
    if (window.currentViewedContact && window.currentViewedContact.phone) {
        const cleanNum = window.currentViewedContact.phone.replace(/\D/g, '');
        window.open(`https://wa.me/${cleanNum}`, '_blank');
    }
}

function toggleContactSelect(id, name, phone) {
    const chk = document.getElementById(`contact-chk-${id}`);
    const idx = window.selectedContacts.findIndex(item => item.id === id);
    if (idx > -1) {
        window.selectedContacts.splice(idx, 1);
        if (chk) {
            chk.style.background = 'transparent';
            chk.style.borderColor = 'var(--border-color)';
            chk.innerHTML = '';
        }
    } else {
        window.selectedContacts.push({ id, name, phone });
        if (chk) {
            chk.style.background = '#00E676';
            chk.style.borderColor = '#00E676';
            chk.innerHTML = '<i class="fa-solid fa-check" style="color:black; font-size:10px;"></i>';
        }
    }
    updateContactSelectedCount();
}

function updateContactSelectedCount() {
    const countEl = document.getElementById('contact-selected-count');
    const fab = document.getElementById('contact-send-fab');
    const count = window.selectedContacts.length;
    if (countEl) countEl.innerText = `${count} selected`;
    if (fab) fab.style.display = count > 0 ? 'flex' : 'none';
}

function sendSelectedContacts() {
    if (window.selectedContacts.length === 0) return;
    closeAttachmentScreen();
    const chatType = (window.activeOpenChat && window.activeOpenChat.type) || 'individual';
    window.selectedContacts.forEach(c => {
        sendCustomChatMessage(chatType, `👤 Contact Card: ${c.name} (${c.phone})`);
    });
}

window.toggleAttachmentSheet = toggleAttachmentSheet;
window.closeAttachmentSheet = closeAttachmentSheet;
window.triggerAttachment = triggerAttachment;
window.handleAttachmentFileSelect = handleAttachmentFileSelect;
window.toggleEmojiPicker = toggleEmojiPicker;
window.closeEmojiPicker = closeEmojiPicker;
window.switchEmojiTab = switchEmojiTab;
window.insertEmoji = insertEmoji;
window.openAttachmentScreen = openAttachmentScreen;
window.closeAttachmentScreen = closeAttachmentScreen;
window.captureCameraSnapshot = captureCameraSnapshot;
window.selectCameraStripPhoto = selectCameraStripPhoto;
window.toggleCameraFlash = toggleCameraFlash;
window.switchCameraFacing = switchCameraFacing;
window.setCameraMode = setCameraMode;
window.toggleGalleryPhotoSelect = toggleGalleryPhotoSelect;
window.sendSelectedGalleryPhotos = sendSelectedGalleryPhotos;
window.selectDocumentToSend = selectDocumentToSend;
window.triggerSystemFilePicker = triggerSystemFilePicker;
window.dismissGPSDialog = dismissGPSDialog;
window.sendCurrentGPSLocation = sendCurrentGPSLocation;
window.selectLocationPlace = selectLocationPlace;
window.refreshLocationMap = refreshLocationMap;
window.toggleContactSelect = toggleContactSelect;
window.sendSelectedContacts = sendSelectedContacts;
window.compressMediaFile = compressMediaFile;
window.openDocumentAttachment = openDocumentAttachment;
window.fetchDevicePhonebookContacts = fetchDevicePhonebookContacts;
window.viewContactDetails = viewContactDetails;
window.closeContactModal = closeContactModal;
window.startDirectChatFromContact = startDirectChatFromContact;
window.callContactPhone = callContactPhone;
window.openWhatsAppContact = openWhatsAppContact;



