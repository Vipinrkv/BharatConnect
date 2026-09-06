/**
 * BharatConnect — Single Page Web Experience & Interactive Simulator
 */

document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  initAudio();
  initMockupSwitcher();
  initPlayground();
  initQRCode();
  initFAQ();
  initScrollSpyAndNav();
  initScrollAnimations();
  initStatsCounter();
  initApkDownload();
});

/* ==========================================================================
   1. THEME TOGGLE & PERSISTENCE
   ========================================================================== */
function initTheme() {
  const themeToggleBtn = document.getElementById('themeToggleBtn');
  if (!themeToggleBtn) return;

  const currentTheme = localStorage.getItem('bc-theme') || 'dark';
  document.documentElement.setAttribute('data-theme', currentTheme);
  updateThemeIcon(currentTheme);

  themeToggleBtn.addEventListener('click', () => {
    playHapticSound('click');
    const newTheme = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('bc-theme', newTheme);
    updateThemeIcon(newTheme);
    showToast(`Switched to ${newTheme === 'dark' ? 'Midnight Dark' : 'Clean Light'} theme`);
  });
}

function updateThemeIcon(theme) {
  const sunIcon = document.getElementById('themeIconSun');
  const moonIcon = document.getElementById('themeIconMoon');
  if (!sunIcon || !moonIcon) return;

  if (theme === 'light') {
    sunIcon.style.display = 'none';
    moonIcon.style.display = 'block';
  } else {
    sunIcon.style.display = 'block';
    moonIcon.style.display = 'none';
  }
}

/* ==========================================================================
   2. AUDIO & HAPTIC SYNTHESIZER (WEB AUDIO API)
   ========================================================================== */
let audioCtx = null;
let soundEnabled = false;

function initAudio() {
  const soundToggleBtn = document.getElementById('soundToggleBtn');
  if (!soundToggleBtn) return;

  soundToggleBtn.addEventListener('click', () => {
    if (!audioCtx) {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      audioCtx = new AudioContext();
    }
    soundEnabled = !soundEnabled;
    const soundOnIcon = document.getElementById('soundOnIcon');
    const soundOffIcon = document.getElementById('soundOffIcon');
    
    if (soundEnabled) {
      if (soundOnIcon) soundOnIcon.style.display = 'block';
      if (soundOffIcon) soundOffIcon.style.display = 'none';
      playHapticSound('pop');
      showToast('Sound effects enabled 🔊');
    } else {
      if (soundOnIcon) soundOnIcon.style.display = 'none';
      if (soundOffIcon) soundOffIcon.style.display = 'block';
      showToast('Sound effects muted 🔇');
    }
  });
}

function playHapticSound(type = 'click') {
  if (!soundEnabled || !audioCtx) return;
  if (audioCtx.state === 'suspended') {
    audioCtx.resume();
  }

  const osc = audioCtx.createOscillator();
  const gain = audioCtx.createGain();
  osc.connect(gain);
  gain.connect(audioCtx.destination);

  const now = audioCtx.currentTime;

  if (type === 'click') {
    osc.type = 'sine';
    osc.frequency.setValueAtTime(800, now);
    osc.frequency.exponentialRampToValueAtTime(400, now + 0.04);
    gain.gain.setValueAtTime(0.12, now);
    gain.gain.exponentialRampToValueAtTime(0.01, now + 0.04);
    osc.start(now);
    osc.stop(now + 0.04);
  } else if (type === 'pop') {
    osc.type = 'triangle';
    osc.frequency.setValueAtTime(440, now);
    osc.frequency.exponentialRampToValueAtTime(880, now + 0.08);
    gain.gain.setValueAtTime(0.15, now);
    gain.gain.exponentialRampToValueAtTime(0.01, now + 0.08);
    osc.start(now);
    osc.stop(now + 0.08);
  } else if (type === 'send') {
    osc.type = 'sine';
    osc.frequency.setValueAtTime(520, now);
    osc.frequency.exponentialRampToValueAtTime(1040, now + 0.12);
    gain.gain.setValueAtTime(0.18, now);
    gain.gain.exponentialRampToValueAtTime(0.01, now + 0.12);
    osc.start(now);
    osc.stop(now + 0.12);
  }
}

/* ==========================================================================
   3. HERO PHONE MOCKUP INTERACTION
   ========================================================================== */
function initMockupSwitcher() {
  const tabBtns = document.querySelectorAll('.mockup-tab-btn');
  const screenViews = document.querySelectorAll('.screen-view');
  const bottomNavBtns = document.querySelectorAll('.screen-nav-btn');

  function switchScreen(targetId) {
    playHapticSound('click');
    
    tabBtns.forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-target') === targetId);
    });

    bottomNavBtns.forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-target') === targetId);
    });

    screenViews.forEach(view => {
      view.classList.toggle('active', view.id === targetId);
    });
  }

  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetId = btn.getAttribute('data-target');
      switchScreen(targetId);
    });
  });

  bottomNavBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetId = btn.getAttribute('data-target');
      switchScreen(targetId);
    });
  });
}

/* ==========================================================================
   4. INTERACTIVE PLAYGROUND / SIMULATOR
   ========================================================================== */
function initPlayground() {
  // Tabs switcher
  const simTabBtns = document.querySelectorAll('.sim-tab-btn');
  const simPanels = document.querySelectorAll('.sim-panel');

  simTabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      playHapticSound('click');
      const targetId = btn.getAttribute('data-tab');

      simTabBtns.forEach(b => b.classList.toggle('active', b === btn));
      simPanels.forEach(p => p.classList.toggle('active', p.id === targetId));
    });
  });

  // Radar Interactive Simulator
  initRadarPlayground();

  // Chat Interactive Simulator
  initChatPlayground();

  // Marketplace Interactive Simulator
  initMarketPlayground();
}

function initRadarPlayground() {
  const radiusSlider = document.getElementById('radarRadiusSlider');
  const radiusValueText = document.getElementById('radarRadiusVal');
  const blips = document.querySelectorAll('.radar-interactive-blip');
  const userCards = document.querySelectorAll('.nearby-user-card');

  if (radiusSlider && radiusValueText) {
    radiusSlider.addEventListener('input', (e) => {
      const val = e.target.value;
      radiusValueText.textContent = `${val} km`;

      blips.forEach(blip => {
        const dist = parseFloat(blip.getAttribute('data-dist') || '1');
        if (dist <= val) {
          blip.style.opacity = '1';
          blip.style.pointerEvents = 'auto';
        } else {
          blip.style.opacity = '0.2';
          blip.style.pointerEvents = 'none';
        }
      });

      userCards.forEach(card => {
        const dist = parseFloat(card.getAttribute('data-dist') || '1');
        if (dist <= val) {
          card.style.display = 'flex';
        } else {
          card.style.display = 'none';
        }
      });
    });
  }

  // Click on radar blip
  blips.forEach(blip => {
    blip.addEventListener('click', () => {
      playHapticSound('pop');
      const name = blip.getAttribute('data-name');
      const dist = blip.getAttribute('data-dist');
      showToast(`📍 Found ${name} • ${dist} km away`);
    });
  });

  // Click on nearby card
  userCards.forEach(card => {
    card.addEventListener('click', () => {
      playHapticSound('click');
      const name = card.getAttribute('data-name');
      showToast(`Initiating encrypted connection with ${name}...`);
    });
  });
}

function initChatPlayground() {
  const chatInput = document.getElementById('simChatInput');
  const chatSendBtn = document.getElementById('simChatSendBtn');
  const messagesBox = document.getElementById('simChatMessages');

  if (!chatInput || !chatSendBtn || !messagesBox) return;

  function sendMessage() {
    const text = chatInput.value.trim();
    if (!text) return;

    playHapticSound('send');
    const timeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    // Append sent message
    const msgDiv = document.createElement('div');
    msgDiv.className = 'chat-bubble bubble-sent';
    msgDiv.innerHTML = `
      <p>${escapeHTML(text)}</p>
      <span class="chat-time">${timeStr} · 🔐 Sentinel E2E ✓✓</span>
    `;
    messagesBox.appendChild(msgDiv);
    chatInput.value = '';
    messagesBox.scrollTop = messagesBox.scrollHeight;

    // Simulate smart auto reply
    setTimeout(() => {
      playHapticSound('pop');
      const replyDiv = document.createElement('div');
      replyDiv.className = 'chat-bubble bubble-received';
      
      const smartReplies = [
        "Namaste! 🙏 The connection is ultra fast with 0 lag on Supabase Realtime.",
        "Got your message! Sentinel 7-layer encryption is fully active.",
        "That sounds wonderful! Syncing instantly across offline SQLite & Cloud.",
        "Yes! BharatConnect works 100% offline first too!"
      ];
      const randomReply = smartReplies[Math.floor(Math.random() * smartReplies.length)];

      replyDiv.innerHTML = `
        <p>${randomReply}</p>
        <span class="chat-time">${new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
      `;
      messagesBox.appendChild(replyDiv);
      messagesBox.scrollTop = messagesBox.scrollHeight;
    }, 900);
  }

  chatSendBtn.addEventListener('click', sendMessage);
  chatInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      sendMessage();
    }
  });
}

function initMarketPlayground() {
  const filterBtns = document.querySelectorAll('.market-filter-btn');
  const marketCards = document.querySelectorAll('.market-card');

  filterBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      playHapticSound('click');
      const filter = btn.getAttribute('data-filter');

      filterBtns.forEach(b => b.classList.toggle('active', b === btn));

      marketCards.forEach(card => {
        const cat = card.getAttribute('data-cat');
        if (filter === 'all' || cat === filter) {
          card.style.display = 'block';
        } else {
          card.style.display = 'none';
        }
      });
    });
  });
}

/* ==========================================================================
   5. DYNAMIC QR CODE GENERATOR (OFFLINE CANVAS)
   ========================================================================== */
function initQRCode() {
  const canvas = document.getElementById('qrCodeCanvas');
  if (!canvas) return;

  const ctx = canvas.getContext('2d');
  const size = 180;
  canvas.width = size;
  canvas.height = size;

  // Render stylized QR pattern
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, size, size);

  ctx.fillStyle = '#0f172a';

  // Fixed corner squares
  drawQRFinder(ctx, 10, 10, 36);
  drawQRFinder(ctx, size - 46, 10, 36);
  drawQRFinder(ctx, 10, size - 46, 36);

  // Dynamic grid simulation
  const blockSize = 6;
  const cols = Math.floor((size - 20) / blockSize);

  // Seeded pseudo-random pattern for realistic QR look
  let seed = 42;
  function random() {
    seed = (seed * 9301 + 49297) % 233280;
    return seed / 233280;
  }

  for (let r = 0; r < cols; r++) {
    for (let c = 0; c < cols; c++) {
      const x = 10 + c * blockSize;
      const y = 10 + r * blockSize;

      // Avoid corner targets
      if (
        (x < 55 && y < 55) ||
        (x > size - 55 && y < 55) ||
        (x < 55 && y > size - 55)
      ) {
        continue;
      }

      if (random() > 0.45) {
        ctx.fillRect(x, y, blockSize - 1, blockSize - 1);
      }
    }
  }

  // Draw miniature center emblem
  const centerSize = 28;
  const centerX = (size - centerSize) / 2;
  const centerY = (size - centerSize) / 2;
  ctx.fillStyle = '#3b82f6';
  ctx.beginPath();
  ctx.roundRect ? ctx.roundRect(centerX, centerY, centerSize, centerSize, 6) : ctx.rect(centerX, centerY, centerSize, centerSize);
  ctx.fill();

  ctx.fillStyle = '#ffffff';
  ctx.font = 'bold 12px Plus Jakarta Sans, sans-serif';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText('🇮🇳', size / 2, size / 2);
}

function drawQRFinder(ctx, x, y, size) {
  ctx.fillStyle = '#0f172a';
  ctx.fillRect(x, y, size, size);
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(x + 6, y + 6, size - 12, size - 12);
  ctx.fillStyle = '#0f172a';
  ctx.fillRect(x + 12, y + 12, size - 24, size - 24);
}

/* ==========================================================================
   6. FAQ ACCORDION
   ========================================================================== */
function initFAQ() {
  const faqItems = document.querySelectorAll('.faq-item');

  faqItems.forEach(item => {
    const question = item.querySelector('.faq-question');
    const answer = item.querySelector('.faq-answer');

    question.addEventListener('click', () => {
      playHapticSound('click');
      const isOpen = item.classList.contains('open');

      // Close other open items
      faqItems.forEach(other => {
        if (other !== item && other.classList.contains('open')) {
          other.classList.remove('open');
          other.querySelector('.faq-answer').style.maxHeight = null;
        }
      });

      if (!isOpen) {
        item.classList.add('open');
        answer.style.maxHeight = answer.scrollHeight + 30 + 'px';
      } else {
        item.classList.remove('open');
        answer.style.maxHeight = null;
      }
    });
  });
}

/* ==========================================================================
   7. SCROLL SPY & NAVBAR BLUR
   ========================================================================== */
function initScrollSpyAndNav() {
  const navbar = document.querySelector('.navbar');
  const sections = document.querySelectorAll('section[id]');
  const navLinks = document.querySelectorAll('.nav-link');

  window.addEventListener('scroll', () => {
    const scrollY = window.scrollY;

    if (scrollY > 50) {
      navbar.classList.add('nav-scrolled');
    } else {
      navbar.classList.remove('nav-scrolled');
    }

    // ScrollSpy active link detection
    let current = '';
    sections.forEach(section => {
      const sectionTop = section.offsetTop - 120;
      const sectionHeight = section.offsetHeight;
      if (scrollY >= sectionTop && scrollY < sectionTop + sectionHeight) {
        current = section.getAttribute('id');
      }
    });

    navLinks.forEach(link => {
      link.classList.toggle('active', link.getAttribute('href') === `#${current}`);
    });
  });
}

/* ==========================================================================
   8. SCROLL ANIMATIONS (INTERSECTION OBSERVER)
   ========================================================================== */
function initScrollAnimations() {
  const elements = document.querySelectorAll('.reveal-on-scroll');

  const observer = new IntersectionObserver((entries, obs) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('revealed');
        obs.unobserve(entry.target);
      }
    });
  }, {
    threshold: 0.12,
    rootMargin: '0px 0px -40px 0px'
  });

  elements.forEach(el => observer.observe(el));
}

/* ==========================================================================
   9. LIVE STATS COUNTER ANIMATION
   ========================================================================== */
function initStatsCounter() {
  const statNumbers = document.querySelectorAll('.stat-number[data-count]');
  let counted = false;

  const statsSection = document.querySelector('.hero-stats');
  if (!statsSection) return;

  const observer = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting && !counted) {
      counted = true;
      statNumbers.forEach(el => {
        const target = parseFloat(el.getAttribute('data-count'));
        const suffix = el.getAttribute('data-suffix') || '';
        let count = 0;
        const duration = 1200;
        const stepTime = 25;
        const stepCount = duration / stepTime;
        const increment = target / stepCount;

        const timer = setInterval(() => {
          count += increment;
          if (count >= target) {
            el.textContent = `${target}${suffix}`;
            clearInterval(timer);
          } else {
            el.textContent = `${Math.floor(count)}${suffix}`;
          }
        }, stepTime);
      });
    }
  }, { threshold: 0.5 });

  observer.observe(statsSection);
}

/* ==========================================================================
   10. APK DOWNLOAD INTERACTION & INTEGRITY HASH COPY
   ========================================================================== */
function initApkDownload() {
  const downloadBtns = document.querySelectorAll('.trigger-apk-download');
  const copyHashBtn = document.getElementById('copyHashBtn');

  downloadBtns.forEach(btn => {
    btn.addEventListener('click', (e) => {
      playHapticSound('pop');
      showToast('🚀 Downloading BharatConnect-Native.apk (24.7 MB)...');
    });
  });

  if (copyHashBtn) {
    copyHashBtn.addEventListener('click', () => {
      playHapticSound('click');
      const hash = "d301848742f0fb79c016dab631ae6f25f4f4dd56f0753f2432a123e398b8e3e9";
      navigator.clipboard.writeText(hash).then(() => {
        showToast('✅ SHA-256 Checksum copied to clipboard!');
      }).catch(() => {
        showToast('SHA-256: d3018487...e3e9');
      });
    });
  }
}

/* ==========================================================================
   11. TOAST NOTIFICATIONS HELPER
   ========================================================================== */
function showToast(message) {
  let toastBox = document.getElementById('toastContainer');
  if (!toastBox) {
    toastBox = document.createElement('div');
    toastBox.id = 'toastContainer';
    toastBox.className = 'toast-container';
    document.body.appendChild(toastBox);
  }

  const toast = document.createElement('div');
  toast.className = 'toast';
  toast.innerHTML = `<span>${escapeHTML(message)}</span>`;
  toastBox.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 3200);
}

function escapeHTML(str) {
  return str.replace(/[&<>'"]/g, 
    tag => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      "'": '&#39;',
      '"': '&quot;'
    }[tag] || tag)
  );
}
