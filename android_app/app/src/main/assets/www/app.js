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

function showInAppToast(notif) {
    const container = document.getElementById('in-app-toast-container');
    if (!container) return;

    const notifTitle = notif.title || 'New Notification';
    const notifMsg = notif.message || '';
    const notifAvatar = notif.avatar || 'logo.png';

    container.innerHTML = `
        <img src="${notifAvatar}" class="in-app-toast-avatar">
        <div class="in-app-toast-content">
            <div class="in-app-toast-title">${notifTitle}</div>
            <div class="in-app-toast-body">${notifMsg}</div>
        </div>
        <button class="in-app-toast-close" onclick="hideInAppToast()">×</button>
    `;

    if (notif.chatId) {
        container.onclick = function(e) {
            if (e.target.classList.contains('in-app-toast-close')) return;
            hideInAppToast();
            openIndividualChatRoom(notif.chatId);
        };
    } else {
        container.onclick = null;
    }

    container.classList.add('show');
    clearTimeout(window.toastTimer);
    window.toastTimer = setTimeout(() => {
        hideInAppToast();
    }, 4500);
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
    const session = window.localDB ? window.localDB.getSession() : null;
    if (session && session.isLoggedIn && session.user) {
        // Auto-login active session
        const data = window.localDB.get();
        data.currentUser = session.user;
        window.localDB.save(data);
        showScreen('screen-home');
    } else {
        showScreen('screen-splash');
    }
    if (window.renderAll) renderAll();
});

function handleGetStarted() {
    const session = window.localDB ? window.localDB.getSession() : null;
    if (session && session.isLoggedIn && session.user) {
        showScreen('screen-home');
    } else {
        showScreen('screen-login');
    }
}

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
    document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));
    if (screenId === 'screen-home') document.querySelectorAll('.nav-item')[0].classList.add('active');
    if (screenId.includes('chat')) document.querySelectorAll('.nav-item')[1].classList.add('active');
    if (screenId === 'screen-marketplace') document.querySelectorAll('.nav-item')[3].classList.add('active');
    if (screenId === 'screen-profile') document.querySelectorAll('.nav-item')[4].classList.add('active');

    if (screenId === 'screen-notifications') {
        renderNotifications();
    }

    // Refresh contents
    renderAll();
}

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
        showCustomAlert('An error occurred during login verification: ' + (err.message || err), 'System Error');
    } finally {
        if (loginBtn) {
            loginBtn.disabled = false;
            loginBtn.innerText = 'Login';
        }
    }
}

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
        showCustomAlert('An error occurred during registration: ' + (err.message || err), 'System Error');
    } finally {
        if (regBtn) {
            regBtn.disabled = false;
            regBtn.innerText = 'Register';
        }
    }
}


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
                    ${p.liked ? '❤️' : '🤍'} <span>${p.likes || 0}</span>
                </button>
                <button class="action-btn" onclick="promptComment('${p.id}')">
                    💬 <span>${p.commentsCount || 0}</span>
                </button>
                <button class="action-btn">🔗 Share</button>
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

function renderModalContactList() {
    const q = (document.getElementById('contact-modal-search') ? document.getElementById('contact-modal-search').value : '').toLowerCase().trim();
    const data = window.localDB.get();
    const container = document.getElementById('modal-contact-list');
    if (!container) return;

    const currentUserId = (data.currentUser.id || '').toLowerCase();
    const currentUsername = (data.currentUser.username || '').toLowerCase();
    const currentPhone = (data.currentUser.phone || '').toLowerCase().replace(/[^0-9]/g, '');

    const deviceContacts = getDeviceContactsList();
    const registeredUsers = data.registeredUsers || [];
    const matchedContacts = [];

    // 1. Cross-reference Device Contacts
    deviceContacts.forEach(dc => {
        if (!dc.phone) return;
        const cleanDcPhone = String(dc.phone).replace(/[^0-9]/g, '');
        if (!cleanDcPhone) return;

        if (currentPhone && (cleanDcPhone.endsWith(currentPhone) || currentPhone.endsWith(cleanDcPhone))) return;

        const matchedSystemUser = registeredUsers.find(u => {
            const uphone = String(u.phone || '').replace(/[^0-9]/g, '');
            return uphone && (uphone.endsWith(cleanDcPhone) || cleanDcPhone.endsWith(uphone));
        });

        const alreadyAdded = matchedContacts.some(m => m.cleanPhone === cleanDcPhone);
        if (!alreadyAdded) {
            matchedContacts.push({
                id: matchedSystemUser ? matchedSystemUser.id : cleanDcPhone,
                name: dc.name || (matchedSystemUser ? matchedSystemUser.name : 'Contact (' + dc.phone + ')'),
                phone: dc.rawPhone || dc.phone,
                cleanPhone: cleanDcPhone,
                avatar: matchedSystemUser ? (matchedSystemUser.avatar || 'logo.png') : 'logo.png',
                isRegistered: !!matchedSystemUser
            });
        }
    });

    // 2. Add all other System Registered Users
    registeredUsers.forEach(u => {
        const uphone = String(u.phone || '').replace(/[^0-9]/g, '');
        const uid = String(u.id || '').toLowerCase();
        const uname = String(u.username || '').toLowerCase();

        if (uid === currentUserId || uname === currentUsername || (currentPhone && uphone && (uphone.endsWith(currentPhone) || currentPhone.endsWith(uphone)))) {
            return;
        }

        const alreadyAdded = matchedContacts.some(m => m.id === u.id || (m.cleanPhone && uphone && (m.cleanPhone.endsWith(uphone) || uphone.endsWith(m.cleanPhone))));
        if (!alreadyAdded) {
            matchedContacts.push({
                id: u.id,
                name: u.name,
                phone: u.phone || '@' + u.username,
                cleanPhone: uphone,
                avatar: u.avatar || 'logo.png',
                isRegistered: true
            });
        }
    });

    const filtered = matchedContacts.filter(c => {
        if (!q) return true;
        return (c.name && c.name.toLowerCase().includes(q)) || (c.phone && c.phone.toLowerCase().includes(q));
    });

    if (filtered.length === 0) {
        container.innerHTML = `
            <div style="text-align:center; padding:20px; color:var(--text-muted); font-size:13px;">
                No matching contacts found.<br>Type a phone number below to start chat directly!
            </div>
        `;
        return;
    }

    container.innerHTML = filtered.map(c => `
        <div class="profile-link-card" style="padding:10px 14px;" onclick="selectModalContact('${c.id || c.cleanPhone || c.phone}')">
            <div style="display:flex; align-items:center; gap:10px;">
                <img src="${c.avatar}" style="width:40px; height:40px; border-radius:50%; object-fit:cover; border:2px solid var(--primary-indigo);">
                <div>
                    <div style="font-weight:600; font-size:14px; color:var(--text-main);">${c.name}</div>
                    <div style="font-size:11px; color:var(--accent-lavender);">${c.phone} • ${c.isRegistered ? '<span style="color:#4CAF50; font-weight:700;">✓ System Registered</span>' : '<span style="color:#B388FF;">📱 Device Contact</span>'}</div>
                </div>
            </div>
            <button class="btn-primary" style="padding:5px 14px; font-size:12px; width:auto; margin:0;">Chat</button>
        </div>
    `).join('');
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
                <div style="font-size:32px; margin-bottom:8px;">👤</div>
                <div style="font-weight:600; color:white;">No Individual Chats</div>
                <div style="font-size:12px; margin-top:4px;">Tap + in bottom right to open contacts and start chat!</div>
            </div>
        `;
        return;
    }

    container.innerHTML = chats.map(c => `
        <div class="profile-link-card" onclick="openIndividualChatRoom('${c.id}')">
            <div style="display:flex; align-items:center; gap:12px;">
                <img src="${c.avatar || 'logo.png'}" style="width:44px; height:44px; border-radius:50%; object-fit:cover;">
                <div>
                    <div style="font-weight:600;">${c.name}</div>
                    <div style="font-size:12px; color:var(--text-muted);">${c.lastMessage}</div>
                </div>
            </div>
            <div style="font-size:11px; color:var(--accent-lavender);">${c.time}</div>
        </div>
    `).join('');
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

function openIndividualChatRoom(chatId) {
    activeOpenChat = { type: 'individual', id: chatId };
    const data = window.localDB.get();
    const chat = (data.individualChats || []).find(c => c.id === chatId);
    if (!chat) return;

    document.querySelector('#screen-chat-indiv .chat-avatar').src = chat.avatar || 'logo.png';
    document.querySelector('#screen-chat-indiv div[style*="font-weight:600"]').innerText = chat.name;
    
    renderIndividualMessages(chat.messages || []);
    showScreen('screen-chat-indiv');
}

function renderIndividualMessages(messages) {
    const indivContainer = document.getElementById('indiv-messages');
    if (!indivContainer) return;

    const data = window.localDB.get();
    const myUsername = String(data.currentUser.username || '').toLowerCase().trim();
    const myId = String(data.currentUser.id || '').toLowerCase().trim();
    const myPhone = String(data.currentUser.phone || '').replace(/[^0-9]/g, '');

    if (!messages || messages.length === 0) {
        indivContainer.innerHTML = `<div style="text-align:center; color:var(--text-muted); font-size:13px; margin:auto;">No messages yet. Type a message below to start chatting!</div>`;
    } else {
        indivContainer.innerHTML = messages.map(m => {
            const mSender = String(m.sender || '').toLowerCase().trim();
            const isSentByMe = (mSender === 'me' || m.is_me === true || mSender === myUsername || mSender === myId || (myPhone && mSender.endsWith(myPhone)));

            let timeDisplay = m.time || 'Just now';
            if (timeDisplay.includes('T') || timeDisplay.length > 10) {
                try {
                    const dt = new Date(timeDisplay);
                    if (!isNaN(dt.getTime())) {
                        timeDisplay = dt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                    }
                } catch(e) {}
            }

            return `
                <div class="message-bubble ${isSentByMe ? 'sent' : 'received'}">
                    <div>${m.text}</div>
                    <div class="message-time">${timeDisplay}</div>
                </div>
            `;
        }).join('');
    }
    indivContainer.scrollTop = indivContainer.scrollHeight;
}

function openGroupChatRoom(groupId) {
    activeOpenChat = { type: 'group', id: groupId };
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
        groupContainer.innerHTML = messages.map(m => `
            <div class="message-bubble received">
                <div style="font-size:11px; font-weight:bold; color:var(--accent-lavender);">${m.sender}</div>
                <div>${m.text}</div>
                <div class="message-time">${m.time}</div>
            </div>
        `).join('');
    }
    groupContainer.scrollTop = groupContainer.scrollHeight;
}

function openCommunityChatRoom(commId) {
    activeOpenChat = { type: 'community', id: commId };
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
        commContainer.innerHTML = messages.map(m => `
            <div class="message-bubble received">
                <div style="font-size:11px; font-weight:bold; color:var(--primary-indigo);">${m.sender} ${m.role ? `[${m.role}]` : ''}</div>
                <div>${m.text}</div>
                <div class="message-time">${m.time}</div>
            </div>
        `).join('');
    }
    commContainer.scrollTop = commContainer.scrollHeight;
}

async function sendChatMessage(chatType) {
    const inputId = `${chatType === 'individual' ? 'indiv' : chatType}-input`;
    const input = document.getElementById(inputId);
    if (!input || !input.value.trim()) return;

    const text = input.value.trim();

    if (chatType === 'individual' && activeOpenChat.id) {
        const data = await window.localDB.sendIndividualMessage(activeOpenChat.id, text);
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

    input.value = '';
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

