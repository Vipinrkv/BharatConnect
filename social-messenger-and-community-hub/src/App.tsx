import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  MessageSquare, Compass, HeartHandshake, Zap, User, 
  Settings as SettingsIcon, ShieldCheck, Battery, Wifi, 
  Clock, Plus, X, Menu, Search, Filter 
} from 'lucide-react';

// Imports
import { Chat, UserProfile, AppSettings, HelpRequest } from './types';
import SplashView from './components/SplashView';
import LoginView from './components/LoginView';
import OtpView from './components/OtpView';
import ChatListView from './components/ChatListView';
import ChatWindowView from './components/ChatWindowView';
import NearbyFeedView from './components/NearbyFeedView';
import VerifiedHelpView from './components/VerifiedHelpView';
import NeedItNowView from './components/NeedItNowView';
import ProfileView from './components/ProfileView';
import SettingsView from './components/SettingsView';

import { 
  CURRENT_USER, 
  INITIAL_CHATS, 
  INITIAL_NEARBY_POSTS, 
  INITIAL_HELP_REQUESTS, 
  MOCK_USERS 
} from './data/mockData';

export default function App() {
  // Global View Navigation State
  const [view, setView] = useState<'splash' | 'login' | 'otp' | 'main'>('splash');
  const [phoneInput, setPhoneInput] = useState('');
  
  // Dashboard navigation tab selection
  const [activeTab, setActiveTab] = useState<'chats' | 'nearby' | 'verified' | 'need-now' | 'profile' | 'settings'>('chats');
  
  // Active selected private/group conversation
  const [activeChat, setActiveChat] = useState<Chat | null>(null);

  // States
  const [currentUser, setCurrentUser] = useState<UserProfile>(CURRENT_USER);
  const [chats, setChats] = useState<Chat[]>(INITIAL_CHATS);
  const [showNewChatModal, setShowNewChatModal] = useState(false);

  // Accessibility & Interface preference options
  const [settings, setSettings] = useState<AppSettings>({
    theme: 'dark',
    notificationsEnabled: true,
    accessibilityTextSize: 'medium',
    hapticFeedback: true,
    soundEffects: true,
    locationSharing: true,
  });

  const isDarkMode = settings.theme === 'dark';

  // Live dynamic clock bar status (reflects current system time)
  const [currentTime, setCurrentTime] = useState('01:08 AM');
  useEffect(() => {
    // Synchronize to clock trigger
    const updateTime = () => {
      const now = new Date();
      let hours = now.getHours();
      const minutes = String(now.getMinutes()).padStart(2, '0');
      const ampm = hours >= 12 ? 'PM' : 'AM';
      hours = hours % 12;
      hours = hours ? hours : 12; // direct 12 hours check
      setCurrentTime(`${String(hours).padStart(2, '0')}:${minutes} ${ampm}`);
    };

    updateTime();
    const interval = setInterval(updateTime, 15000);
    return () => clearInterval(interval);
  }, []);

  // Update user profile bio
  const handleUpdateCurrentUser = (updatedUser: UserProfile) => {
    setCurrentUser(updatedUser);
  };

  // Push user typed message to the specific chat list state
  const handleSendMessage = (chatId: string, text: string) => {
    setChats((prevChats) =>
      prevChats.map((chat) => {
        if (chat.id === chatId) {
          const newMsg = {
            id: `msg_${Date.now()}`,
            senderId: 'me',
            text,
            timestamp: 'Just Now',
            status: 'sent' as const
          };
          return {
            ...chat,
            lastMessage: text,
            timestamp: 'Just Now',
            messages: [...chat.messages, newMsg]
          };
        }
        return chat;
      })
    );
  };

  // Start direct message thread with chosen contact in modal
  const handleStartNewChat = (recipient: UserProfile) => {
    // Check if chat already exists
    const existing = chats.find((c) => c.user.id === recipient.id);
    if (existing) {
      setActiveChat(existing);
      setShowNewChatModal(false);
      return;
    }

    // Create fresh Direct Message
    const newChat: Chat = {
      id: `chat_new_${recipient.id}`,
      user: recipient,
      lastMessage: 'Encryption keys generated.',
      unreadCount: 0,
      timestamp: 'Just Now',
      isGroup: false,
      isChannel: false,
      category: 'direct',
      messages: [
        {
          id: 'init_secure',
          senderId: recipient.id,
          text: `🤝 Active session started with ${recipient.name}. Enter a message to begin.`,
          timestamp: 'Just Now',
          status: 'read'
        }
      ]
    };

    setChats([newChat, ...chats]);
    setActiveChat(newChat);
    setShowNewChatModal(false);
  };

  // Logout reset routine
  const handleLogout = () => {
    setView('splash');
    setActiveChat(null);
    setActiveTab('chats');
  };

  // Determine container dimensions based on accessibility choices
  const getTextSizeClass = () => {
    if (settings.accessibilityTextSize === 'small') return 'text-[11px]';
    if (settings.accessibilityTextSize === 'large') return 'text-[14px]';
    return 'text-[12.5px]'; // medium standard
  };

  return (
    <div 
      className={`min-h-screen w-full flex items-center justify-center p-0 md:p-6 transition-colors duration-300 ${
        isDarkMode ? 'bg-[#040507] text-[#f3f4f6]' : 'bg-[#faf9f5] text-[#2c2824]'
      }`}
      style={{ fontFamily: '"Inter", sans-serif' }}
    >
      {/* Decorative large backdrop blurred circles for premium visual aesthetic */}
      <div className="absolute top-20 left-10 w-[30vw] h-[30vw] rounded-full bg-elegant-gold/5 blur-[120px] pointer-events-none select-none" />
      <div className="absolute bottom-20 right-10 w-[30vw] h-[30vw] rounded-full bg-elegant-accent/5 blur-[120px] pointer-events-none select-none" />

      {/* Main smartphone frame device container */}
      <div 
        id="applet_phone_frame"
        className={`w-full max-w-sm h-full md:h-[720px] md:rounded-[42px] border shadow-2xl overflow-hidden flex flex-col relative transition-all duration-300 ${
          isDarkMode 
            ? 'bg-elegant-bg border-elegant-border shadow-black/90 hover:border-elegant-gold/25' 
            : 'bg-[#fbfaf6] border-[#dfd5c6] shadow-[#dacfb9]/30 hover:border-[#c5a880]/40'
        }`}
      >
        {/* Dynamic Mobile Status Bar inside frame block */}
        <div 
          className={`px-6 py-2 flex items-center justify-between text-[11px] font-semibold tracking-wide select-none z-30 shrink-0 ${
            isDarkMode 
              ? 'bg-elegant-bg text-elegant-gold/90 border-b border-elegant-border/10' 
              : 'bg-[#fbfaf6] text-[#3c3730] border-b border-[#dfd5c6]/30'
          }`}
        >
          {/* Left Hour Clock, synchronized to realtime UTC bar */}
          <div className="flex items-center gap-1">
            <Clock className={`w-3.5 h-3.5 ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`} />
            <span id="statusBar_clock" className="font-mono">{currentTime}</span>
          </div>

          {/* Notch Spacer block */}
          <div className="hidden md:block w-28 h-4.5 bg-slate-950 dark:bg-[#12141c] border border-white/5 rounded-full z-10 select-none shadow-inner" />

          {/* Right Status utilities */}
          <div className="flex items-center gap-2">
            <span className={`text-[10px] font-mono tracking-tighter font-bold ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`}>5G</span>
            <Wifi className={`w-4 h-4 ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`} />
            <div className="flex items-center gap-0.5" title="Battery 100%">
              <Battery className={`w-4.5 h-4.5 ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`} />
              <span className="text-[10px] font-mono">100%</span>
            </div>
          </div>
        </div>

        {/* Dynamic Core Screen Router View rendering */}
        <div className="flex-1 overflow-hidden relative">
          <AnimatePresence mode="wait">
            {view === 'splash' && (
              <motion.div 
                key="splash"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="absolute inset-0 z-10"
              >
                <SplashView onComplete={() => setView('login')} />
              </motion.div>
            )}

            {view === 'login' && (
              <motion.div 
                key="login"
                initial={{ opacity: 0, x: 100 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -100 }}
                className="absolute inset-0 z-10"
              >
                <LoginView 
                  onNext={(phone) => {
                    setPhoneInput(phone);
                    setView('otp');
                  }} 
                  isDarkMode={isDarkMode}
                />
              </motion.div>
            )}

            {view === 'otp' && (
              <motion.div 
                key="otp"
                initial={{ opacity: 0, x: 100 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -100 }}
                className="absolute inset-0 z-10"
              >
                <OtpView 
                  phone={phoneInput}
                  onVerified={() => setView('main')}
                  onBack={() => setView('login')}
                  isDarkMode={isDarkMode}
                />
              </motion.div>
            )}

            {view === 'main' && (
              <motion.div 
                key="main"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="absolute inset-0 z-0 flex flex-col h-full"
              >
                {/* Immersive Top App Bar (Material 3 Top App Bar inspired) */}
                <div className={`px-4 py-3 border-b flex items-center justify-between shrink-0 z-10 transition-colors ${
                  isDarkMode ? 'bg-elegant-card border-elegant-border/20' : 'bg-[#e9e4d9] border-[#dfd5c6] shadow-sm'
                }`}>
                  <div className="flex items-center gap-2">
                    {/* Active Brand visual marker */}
                    <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-elegant-gold-dark to-elegant-gold flex items-center justify-center font-extrabold text-sm text-elegant-bg shadow-md">
                      N
                    </div>
                    <div className="flex flex-col">
                      <h3 className={`text-sm font-extrabold tracking-tight capitalize ${isDarkMode ? 'text-[#f3f4f6]' : 'text-[#3c3730]'}`}>
                        {activeTab === 'chats' ? 'Direct Messages' :
                         activeTab === 'nearby' ? 'Nearby Radar' :
                         activeTab === 'verified' ? 'Unified Aid Board' :
                         activeTab === 'need-now' ? 'Need It Now' :
                         activeTab === 'profile' ? 'My Security ID' : 'Client Settings'}
                      </h3>
                      <span className={`text-[9px] font-mono tracking-wider uppercase font-semibold leading-none ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`}>
                        Active Sandbox Node
                      </span>
                    </div>
                  </div>

                  {/* Actions right panel */}
                  <div className="flex items-center gap-1.5">
                    {activeTab === 'chats' && (
                      <button 
                        onClick={() => setShowNewChatModal(true)}
                        className={`p-2 rounded-xl text-slate-400 transition-colors ${isDarkMode ? 'hover:bg-elegant-card-hover' : 'hover:bg-[#dfd5c6]/30'}`}
                        title="New Secured Dialogue"
                      >
                        <Plus className="w-4 h-4" />
                      </button>
                    )}
                    <span className={`w-1.5 h-1.5 rounded-full animate-pulse shadow-[0_0_6px_rgba(197,168,128,0.6)] ${isDarkMode ? 'bg-elegant-gold' : 'bg-elegant-gold-dark'}`} title="System Status: Connected" />
                  </div>
                </div>

                {/* Unified Tab Area Switch */}
                <div className={`flex-1 overflow-hidden relative ${getTextSizeClass()}`}>
                  {activeTab === 'chats' && (
                    <div className="absolute inset-0">
                      <ChatListView 
                        chats={chats} 
                        onSelectChat={(chat) => setActiveChat(chat)} 
                        isDarkMode={isDarkMode}
                        onOpenNewChatModal={() => setShowNewChatModal(true)}
                      />
                    </div>
                  )}

                  {activeTab === 'nearby' && (
                    <div className="absolute inset-0">
                      <NearbyFeedView 
                        initialPosts={INITIAL_NEARBY_POSTS} 
                        currentUser={currentUser} 
                        isDarkMode={isDarkMode}
                      />
                    </div>
                  )}

                  {activeTab === 'verified' && (
                    <div className="absolute inset-0">
                      <VerifiedHelpView 
                        initialRequests={INITIAL_HELP_REQUESTS} 
                        currentUser={currentUser} 
                        isDarkMode={isDarkMode}
                      />
                    </div>
                  )}

                  {activeTab === 'need-now' && (
                    <div className="absolute inset-0">
                      <NeedItNowView isDarkMode={isDarkMode} />
                    </div>
                  )}

                  {activeTab === 'profile' && (
                    <div className="absolute inset-0">
                      <ProfileView 
                        user={currentUser} 
                        onUpdate={handleUpdateCurrentUser} 
                        isDarkMode={isDarkMode}
                      />
                    </div>
                  )}

                  {activeTab === 'settings' && (
                    <div className="absolute inset-0">
                      <SettingsView 
                        settings={settings} 
                        onUpdateSettings={(set) => setSettings(set)} 
                        onLogout={handleLogout}
                        isDarkMode={isDarkMode}
                      />
                    </div>
                  )}
                </div>

                {/* Bottom Navigation Panel Bar (Inspired by Material 3 Bottom Nav & Discord visual alignment) */}
                <div className={`px-2 py-1 border-t shrink-0 z-10 flex justify-around transition-colors duration-250 ${
                  isDarkMode ? 'bg-elegant-card border-elegant-border/10' : 'bg-[#e9e4d9] border-[#dfd5c6] shadow-lg'
                }`}>
                  {[
                    { id: 'chats', label: 'Chats', icon: <MessageSquare className="w-5 h-5" />, unread: chats.reduce((a, b) => a + b.unreadCount, 0) },
                    { id: 'nearby', label: 'Nearby', icon: <Compass className="w-5 h-5" /> },
                    { id: 'verified', label: 'Verified', icon: <HeartHandshake className="w-5 h-5 animate-pulse" style={{ animationDuration: '4s' }} /> },
                    { id: 'need-now', label: 'Crisis', icon: <Zap className="w-5 h-5" /> },
                    { id: 'profile', label: 'Profile', icon: <User className="w-5 h-5" /> },
                    { id: 'settings', label: 'Settings', icon: <SettingsIcon className="w-5 h-5" /> },
                  ].map((tab) => {
                    const isSelected = activeTab === tab.id;
                    return (
                      <button
                        key={tab.id}
                        id={`bottom_nav_btn_${tab.id}`}
                        onClick={() => {
                          setActiveTab(tab.id as any);
                          setActiveChat(null); // clears chat overlay when browsing tabs
                        }}
                        className="flex flex-col items-center justify-center p-2 relative rounded-2xl cursor-pointer grow transition-all select-none group"
                      >
                        {/* Selector indicator bubble container */}
                        <div className={`p-1 px-3 rounded-xl transition-all ${
                          isSelected 
                            ? (isDarkMode ? 'bg-elegant-gold/10 text-elegant-gold scale-105 font-bold' : 'bg-[#dfd5c6]/40 text-elegant-gold-dark font-bold scale-105') 
                            : (isDarkMode ? 'text-slate-500 hover:text-elegant-gold/60' : 'text-slate-600 hover:text-elegant-gold-dark/60')
                        }`}>
                          {tab.icon}
                        </div>
                        
                        {/* Unread system alert count bubble */}
                        {tab.unread && tab.unread > 0 ? (
                          <span className={`absolute top-1.5 right-4 font-mono text-[9px] font-extrabold px-1 rounded-full shrink-0 min-w-4 h-4 flex items-center justify-center border ${
                            isDarkMode ? 'bg-elegant-gold text-elegant-bg border-elegant-bg' : 'bg-elegant-gold-dark text-white border-[#fbfaf6]'
                          }`}>
                            {tab.unread}
                          </span>
                        ) : null}

                        <span className={`text-[9.5px] mt-0.5 font-mono ${
                          isSelected 
                            ? (isDarkMode ? 'text-elegant-gold font-extrabold' : 'text-elegant-gold-dark font-extrabold') 
                            : 'text-slate-500'
                        }`}>
                          {tab.label}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Immersive Chat Window Overlay Overlay Screen */}
          <AnimatePresence>
            {activeChat && (
              <motion.div
                initial={{ y: '100%' }}
                animate={{ y: 0 }}
                exit={{ y: '100%' }}
                transition={{ type: 'spring', stiffness: 220, damping: 25 }}
                className={`absolute inset-0 z-20 flex flex-col h-full ${isDarkMode ? 'bg-elegant-bg' : 'bg-[#faf9f5]'}`}
              >
                <ChatWindowView 
                  chat={activeChat} 
                  onBack={() => {
                    // Reset unread count on reading
                    activeChat.unreadCount = 0;
                    setActiveChat(null);
                  }} 
                  onSendMessage={handleSendMessage}
                  isDarkMode={isDarkMode}
                />
              </motion.div>
            )}
          </AnimatePresence>

          {/* New Chat recipient chooser overlay Modal */}
          <AnimatePresence>
            {showNewChatModal && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="absolute inset-0 bg-elegant-bg/85 backdrop-blur-md z-30 p-4 flex flex-col justify-center select-none"
              >
                <div className={`p-5 rounded-3xl border flex flex-col gap-4 max-w-sm mx-auto w-full max-h-[460px] ${
                  isDarkMode ? 'bg-elegant-card border-elegant-border text-white' : 'bg-[#fbfaf6] border-[#dfd5c6] text-[#3c3730]'
                }`}>
                  <div className="flex justify-between items-center pb-2 border-b border-elegant-border/10">
                    <span className={`text-xs font-bold font-mono uppercase ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`}>
                      🔑 Secure Contacts Index
                    </span>
                    <button
                      id="btn_close_new_dialog_modal"
                      onClick={() => setShowNewChatModal(false)}
                      className={`p-1 px-2.5 rounded-lg text-xs font-mono cursor-pointer transition-colors ${
                        isDarkMode ? 'text-slate-400 hover:text-white hover:bg-slate-800/50' : 'text-slate-650 hover:text-[#3c3730] hover:bg-slate-200/50'
                      }`}
                    >
                      Close
                    </button>
                  </div>

                  <p className={`text-[10.5px] font-mono leading-relaxed ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`}>
                    Choose a verified nearby responder or broadcast grid node to initiate secure dialogue.
                  </p>

                  <div className="flex-1 overflow-y-auto space-y-2 max-h-[250px] pr-1 scrollbar-thin">
                    {MOCK_USERS.map((usr) => (
                      <div
                        key={usr.id}
                        id={`contact_select_${usr.id}`}
                        onClick={() => handleStartNewChat(usr)}
                        className={`flex items-center gap-3 p-2.5 rounded-2xl cursor-pointer border transition-all ${
                          isDarkMode 
                            ? 'bg-elegant-bg/40 border-elegant-border/10 hover:border-elegant-gold/30 hover:bg-elegant-card-hover' 
                            : 'bg-[#f5f2eb]/70 border-[#dfd5c6]/40 hover:border-[#c5a880]/40 hover:bg-[#e9e4d9]'
                        }`}
                      >
                        <div className={`w-9 h-9 rounded-full ${usr.avatarBg} flex items-center justify-center font-bold text-xs text-white`}>
                          {usr.avatar}
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className={`text-[12px] font-bold truncate ${isDarkMode ? 'text-slate-100' : 'text-slate-800'}`}>{usr.name}</p>
                          <p className="text-[9.5px] font-mono text-slate-400 leading-none truncate">{usr.username} • {usr.distance || 'Online'}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* Elegant simulated Android Bottom gesture lock notch pill */}
        <div className={`hidden md:block w-36 h-1 rounded-full mx-auto my-3 pointer-events-none select-none ${
          isDarkMode ? 'bg-elegant-gold/15' : 'bg-[#dfd5c6]/60'
        }`} />
      </div>
    </div>
  );
}
