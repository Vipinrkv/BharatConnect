import React, { useState } from 'react';

// Common Types/Interfaces
interface ScreenProps {
  onNavigate: (screen: string) => void;
  isOnline: boolean;
  stateMode: 'normal' | 'loading' | 'empty' | 'error';
}

// Global Loading Spinner Component
export const LoadingState: React.FC = () => (
  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', gap: '16px' }}>
    <div style={{ width: '48px', height: '48px', borderRadius: '50%', border: '4px solid var(--md-surface-variant)', borderTopColor: 'var(--md-primary)', animation: 'spin 1s linear infinite' }} />
    <p style={{ color: 'var(--md-on-surface-variant)', fontWeight: 500 }}>Syncing databases...</p>
    <style>{`
      @keyframes spin { to { transform: rotate(360deg); } }
    `}</style>
  </div>
);

// Global Error Card Component
export const ErrorState: React.FC<{ message?: string; onRetry?: () => void }> = ({ message, onRetry }) => (
  <div style={{ padding: '24px', backgroundColor: 'rgba(186, 26, 26, 0.1)', borderRadius: 'var(--md-radius-md)', border: '1px solid var(--md-error)', display: 'flex', flexDirection: 'column', gap: '12px', textAlign: 'center', margin: 'auto' }}>
    <div style={{ fontSize: '36px', color: 'var(--md-error)' }}>⚠️</div>
    <h3 style={{ color: 'var(--md-error)', fontWeight: 600 }}>Connection Error</h3>
    <p style={{ fontSize: '13px', color: 'var(--md-on-surface)' }}>{message || 'Unable to establish secure WebSocket handshake. Retrying in background...'}</p>
    <button className="btn btn-tonal" onClick={onRetry} style={{ padding: '8px 16px', color: 'var(--md-error)' }}>Retry Connection</button>
  </div>
);

// Global Empty State Component
export const EmptyState: React.FC<{ title: string; description: string }> = ({ title, description }) => (
  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', gap: '12px', textAlign: 'center', color: 'var(--md-on-surface-variant)', padding: '24px' }}>
    <div style={{ fontSize: '48px', opacity: 0.5 }}>📭</div>
    <h3 style={{ fontWeight: 600, color: 'var(--md-on-surface)' }}>{title}</h3>
    <p style={{ fontSize: '13px' }}>{description}</p>
  </div>
);

/* ==========================================================================
   AUTHENTICATION SCREENS
   ========================================================================== */

// 1. Splash Screen
export const SplashScreen: React.FC<ScreenProps> = ({ onNavigate }) => {
  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', backgroundColor: 'var(--md-primary)', color: 'var(--md-on-primary)', textAlign: 'center' }}>
      <div style={{ fontSize: '72px', animation: 'pulse 2s infinite' }}>🤝</div>
      <h1 style={{ color: '#ffffff', fontSize: '32px', letterSpacing: '1px', marginTop: '16px' }}>BharatConnect</h1>
      <p style={{ color: 'rgba(255, 255, 255, 0.7)', fontSize: '14px' }}>Android-First Secure Messenger</p>
      
      <button className="btn btn-tonal" onClick={() => onNavigate('onboarding')} style={{ width: 'auto', position: 'absolute', bottom: '64px', backgroundColor: '#ffffff', color: 'var(--md-primary)' }}>
        Get Started
      </button>

      <style>{`
        @keyframes pulse {
          0% { transform: scale(1); }
          50% { transform: scale(1.08); }
          100% { transform: scale(1); }
        }
      `}</style>
    </div>
  );
};

// 2. Onboarding Screen
export const OnboardingScreen: React.FC<ScreenProps> = ({ onNavigate }) => {
  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', height: '100%', justifyContent: 'space-between', padding: '32px 16px' }}>
      <div style={{ textAlign: 'center', marginTop: '48px' }}>
        <div style={{ fontSize: '80px', marginBottom: '24px' }}>📡</div>
        <h2>Hyper-local Connections</h2>
        <p>Chat with contacts, discover volunteers nearby, request immediate home services, and stay connected fully offline.</p>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginBottom: '16px' }}>
          <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: 'var(--md-primary)' }} />
          <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: 'var(--md-surface-variant)' }} />
          <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: 'var(--md-surface-variant)' }} />
        </div>
        <button className="btn btn-primary" onClick={() => onNavigate('login')}>Next</button>
      </div>
    </div>
  );
};

// 3. Login Screen (OTP Request)
export const LoginScreen: React.FC<ScreenProps> = ({ onNavigate }) => {
  const [phone, setPhone] = useState('');
  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', height: '100%', justifyContent: 'space-between', padding: '32px 16px' }}>
      <div>
        <h2 style={{ marginTop: '24px' }}>Verify your Phone Number</h2>
        <p>BharatConnect sends a secure SMS OTP verification code to log you into your local database.</p>
        
        <div style={{ display: 'flex', gap: '8px', marginTop: '32px' }}>
          <input className="text-field" value="+91" style={{ width: '64px', textAlign: 'center' }} readOnly />
          <input className="text-field" placeholder="10-digit mobile number" type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} />
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <button className="btn btn-primary" onClick={() => onNavigate('otp')} disabled={phone.length < 10}>
          Send Verification SMS
        </button>
        <button className="btn btn-outline" onClick={() => onNavigate('google_login')}>
          Sign In with Google
        </button>
      </div>
    </div>
  );
};

// 4. OTP Screen
export const OtpScreen: React.FC<ScreenProps> = ({ onNavigate }) => {
  const [code, setCode] = useState('');
  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', height: '100%', justifyContent: 'space-between', padding: '32px 16px' }}>
      <div>
        <h2 style={{ marginTop: '24px' }}>Enter verification code</h2>
        <p>We've sent a 6-digit verification code to +91 98765 43210.</p>
        
        <input className="text-field" placeholder="######" type="text" maxLength={6} style={{ letterSpacing: '8px', textAlign: 'center', fontSize: '24px', marginTop: '32px' }} value={code} onChange={(e) => setCode(e.target.value)} />
        <p style={{ textAlign: 'center', fontSize: '12px', marginTop: '8px', cursor: 'pointer', color: 'var(--md-primary)', fontWeight: 500 }}>Resend SMS in 54s</p>
      </div>

      <button className="btn btn-primary" onClick={() => onNavigate('chat_list')} disabled={code.length < 6}>
        Verify & Register
      </button>
    </div>
  );
};

// 5. Google Login Screen
export const GoogleLoginScreen: React.FC<ScreenProps> = ({ onNavigate }) => {
  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', height: '100%', justifyContent: 'space-between', padding: '32px 16px' }}>
      <div>
        <h2 style={{ marginTop: '24px' }}>Google Secure Auth</h2>
        <p>Link your Gmail address for simple recovery and helper profile verification synchronization.</p>
        
        <div style={{ border: '1px solid var(--md-outline)', borderRadius: 'var(--md-radius-md)', padding: '16px', display: 'flex', alignItems: 'center', gap: '16px', marginTop: '48px', cursor: 'pointer', backgroundColor: 'var(--md-surface)' }} onClick={() => onNavigate('chat_list')}>
          <div style={{ fontSize: '24px' }}>🌐</div>
          <div>
            <div style={{ fontWeight: 600, fontSize: '15px' }}>vipin.dev@gmail.com</div>
            <div style={{ fontSize: '12px', color: 'var(--md-on-surface-variant)' }}>Log in as Vipin Dev</div>
          </div>
        </div>
      </div>

      <button className="btn btn-outline" onClick={() => onNavigate('login')}>Use Phone Number instead</button>
    </div>
  );
};

/* ==========================================================================
   MAIN APPLICATION MODULES
   ========================================================================== */

// 6. Chat List Screen (Inbox)
export const ChatListScreen: React.FC<ScreenProps> = ({ onNavigate, isOnline, stateMode }) => {
  if (stateMode === 'loading') return <LoadingState />;
  if (stateMode === 'error') return <ErrorState />;
  if (stateMode === 'empty') return <EmptyState title="No Chats Available" description="Tap the compose icon below to start a secure, encrypted conversation." />;

  const dummyChats = [
    { id: '1', name: 'Arjun Sharma', text: 'Hey, did you look at the design system?', time: '14:02', unread: 2, online: true, pending: false },
    { id: '2', name: 'Delhi Tech Volunteer Group', text: 'Volunteer Ritu has accepted the SOS help request.', time: '11:45', unread: 0, online: false, pending: false },
    { id: '3', name: 'Pooja Varma', text: 'Sent you an encrypted photo 🔒', time: 'Yesterday', unread: 0, online: true, pending: true },
  ];

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', height: '100%', gap: '8px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
        <h2>Conversations</h2>
        <div style={{ display: 'flex', gap: '12px' }}>
          <span style={{ cursor: 'pointer' }} onClick={() => onNavigate('notifications')}>🔔</span>
          <span style={{ cursor: 'pointer' }} onClick={() => onNavigate('settings')}>⚙️</span>
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
        {dummyChats.map(chat => (
          <div key={chat.id} onClick={() => onNavigate(chat.id === '2' ? 'group_chat' : 'chat_window')} style={{ display: 'flex', gap: '12px', padding: '12px', borderBottom: '1px solid var(--md-surface-variant)', cursor: 'pointer', position: 'relative' }}>
            <div style={{ width: '48px', height: '48px', borderRadius: '50%', backgroundColor: 'var(--md-primary-container)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '20px' }}>
              👤
              {chat.online && <span style={{ position: 'absolute', width: '12px', height: '12px', borderRadius: '50%', backgroundColor: 'var(--md-secondary)', border: '2px solid var(--md-bg)', bottom: '12px', left: '48px' }} />}
            </div>
            
            <div style={{ flex: 1 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ fontWeight: 600, fontSize: '15px' }}>{chat.name}</span>
                <span style={{ fontSize: '11px', color: 'var(--md-on-surface-variant)' }}>{chat.time}</span>
              </div>
              <p style={{ fontSize: '13px', margin: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '240px' }}>
                {chat.pending && !isOnline && <span style={{ color: 'var(--md-error)', marginRight: '4px' }}>🕒</span>}
                {chat.text}
              </p>
            </div>

            {chat.unread > 0 && (
              <span style={{ minWidth: '18px', height: '18px', borderRadius: '9px', backgroundColor: 'var(--md-primary)', color: 'var(--md-on-primary)', fontSize: '10px', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '0 4px', fontWeight: 'bold', alignSelf: 'center' }}>
                {chat.unread}
              </span>
            )}
          </div>
        ))}
      </div>
      
      {/* Floating Action Button (FAB) */}
      <button style={{ position: 'absolute', bottom: '24px', right: '24px', width: '56px', height: '56px', borderRadius: '16px', backgroundColor: 'var(--md-primary-container)', color: 'var(--md-on-primary-container)', border: 'none', fontSize: '24px', boxShadow: '0 4px 12px rgba(0,0,0,0.2)', cursor: 'pointer' }}>
        +
      </button>
    </div>
  );
};

// 7. Chat Window Screen (Direct Thread)
export const ChatWindowScreen: React.FC<ScreenProps> = ({ onNavigate, isOnline, stateMode }) => {
  const [messages, setMessages] = useState([
    { id: 1, text: 'Hello, are you available?', sent: false, time: '13:58' },
    { id: 2, text: 'Yes, online now. Did the files upload correctly?', sent: true, time: '14:00' },
  ]);
  const [inputVal, setInputVal] = useState('');

  const handleSend = () => {
    if (!inputVal.trim()) return;
    setMessages([...messages, { id: Date.now(), text: inputVal, sent: true, time: '14:02' }]);
    setInputVal('');
  };

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', paddingBottom: '12px', borderBottom: '1px solid var(--md-surface-variant)' }}>
        <span style={{ cursor: 'pointer', fontSize: '20px' }} onClick={() => onNavigate('chat_list')}>⬅️</span>
        <div style={{ width: '40px', height: '40px', borderRadius: '50%', backgroundColor: 'var(--md-primary-container)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>👤</div>
        <div style={{ flex: 1 }}>
          <div style={{ fontWeight: 600 }}>Arjun Sharma</div>
          <div style={{ fontSize: '11px', color: 'var(--md-on-surface-variant)' }}>{isOnline ? 'online' : 'offline (messages sync later)'}</div>
        </div>
      </div>

      {/* Messages area */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflowY: 'auto', padding: '12px 0', gap: '8px' }}>
        {messages.map(msg => (
          <div key={msg.id} className={`chat-bubble ${msg.sent ? 'sent' : 'received'}`}>
            <div>{msg.text}</div>
            <span className="chat-bubble-time">{msg.time} {msg.sent && (isOnline ? '✓✓' : '🕒')}</span>
          </div>
        ))}
      </div>

      {/* Input area */}
      <div style={{ display: 'flex', gap: '8px', padding: '8px 0', alignItems: 'center' }}>
        <input className="text-field" placeholder="Message..." style={{ margin: 0, borderRadius: '24px' }} value={inputVal} onChange={(e) => setInputVal(e.target.value)} />
        <button style={{ width: '44px', height: '44px', borderRadius: '50%', backgroundColor: 'var(--md-primary)', color: 'white', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }} onClick={handleSend}>
          🚀
        </button>
      </div>
    </div>
  );
};

// 8. Group Chat Screen
export const GroupChatScreen: React.FC<ScreenProps> = ({ onNavigate, isOnline }) => {
  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', paddingBottom: '12px', borderBottom: '1px solid var(--md-surface-variant)' }}>
        <span style={{ cursor: 'pointer', fontSize: '20px' }} onClick={() => onNavigate('chat_list')}>⬅️</span>
        <div style={{ width: '40px', height: '40px', borderRadius: '50%', backgroundColor: 'var(--md-primary-container)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>👥</div>
        <div style={{ flex: 1 }}>
          <div style={{ fontWeight: 600 }}>Delhi Tech Volunteer Group</div>
          <div style={{ fontSize: '11px', color: 'var(--md-on-surface-variant)' }}>18 Members</div>
        </div>
      </div>

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflowY: 'auto', padding: '12px 0', gap: '8px' }}>
        <div className="chat-bubble received">
          <div style={{ fontWeight: 'bold', fontSize: '11px', color: 'var(--md-primary)', marginBottom: '2px' }}>Volunteer Ritu</div>
          <div>I have picked up the emergency medicine dispatch request. Navigating to recipient location now.</div>
          <span className="chat-bubble-time">11:43</span>
        </div>
        <div className="chat-bubble received">
          <div style={{ fontWeight: 'bold', fontSize: '11px', color: 'var(--md-tertiary)', marginBottom: '2px' }}>Admin Amit</div>
          <div>Excellent, Ritu. Keep us posted. Coordination is fully stored in local SQLite cache.</div>
          <span className="chat-bubble-time">11:45</span>
        </div>
      </div>

      <div style={{ display: 'flex', gap: '8px', padding: '8px 0', alignItems: 'center' }}>
        <input className="text-field" placeholder="Message group..." style={{ margin: 0, borderRadius: '24px' }} />
        <button style={{ width: '44px', height: '44px', borderRadius: '50%', backgroundColor: 'var(--md-primary)', color: 'white', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
          🚀
        </button>
      </div>
    </div>
  );
};

// 9. Nearby Right Now Screen (Geopresence list)
export const NearbyRightNowScreen: React.FC<ScreenProps> = ({ isOnline, stateMode }) => {
  if (stateMode === 'loading') return <LoadingState />;
  if (stateMode === 'error') return <ErrorState />;
  if (stateMode === 'empty') return <EmptyState title="No Nearby Users" description="We couldn't detect any active nodes in your immediate Geohash cell." />;

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', height: '100%', gap: '12px' }}>
      <h2>Nearby Right Now</h2>
      <p style={{ margin: 0 }}>Discover active mesh nodes in your area. Your coordinates are blurred by 100m to protect your privacy.</p>
      
      <div style={{ padding: '12px', borderRadius: 'var(--md-radius-md)', backgroundColor: 'var(--md-surface-variant)', color: 'var(--md-on-surface-variant)', fontSize: '12px', display: 'flex', gap: '8px', alignItems: 'center' }}>
        <span>🔒</span>
        <span>Geohash cell precision limited to 1.2km width. Triangulation blocked.</span>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        {[
          { id: 1, name: 'Sanjay Dutt', distance: '120m away', helper: true, score: '4.9★' },
          { id: 2, name: 'Megha Sen', distance: '450m away', helper: false, score: '' },
          { id: 3, name: 'Karan Malhotra', distance: '850m away', helper: true, score: '4.2★' },
        ].map(user => (
          <div key={user.id} style={{ padding: '12px', backgroundColor: 'var(--md-surface)', borderRadius: 'var(--md-radius-md)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', border: '1px solid var(--md-surface-variant)' }}>
            <div>
              <div style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: '6px' }}>
                {user.name}
                {user.helper && <span style={{ fontSize: '12px', padding: '2px 6px', backgroundColor: 'var(--md-primary-container)', color: 'var(--md-on-primary-container)', borderRadius: '4px' }}>Helper {user.score}</span>}
              </div>
              <div style={{ fontSize: '12px', color: 'var(--md-on-surface-variant)', marginTop: '2px' }}>{user.distance}</div>
            </div>
            <button className="btn btn-tonal" style={{ width: 'auto', padding: '6px 12px', borderRadius: '8px' }}>Chat</button>
          </div>
        ))}
      </div>
    </div>
  );
};

// 10. Verified Help Screen (SOS Boards)
export const VerifiedHelpScreen: React.FC<ScreenProps> = ({ isOnline, stateMode }) => {
  if (stateMode === 'loading') return <LoadingState />;
  if (stateMode === 'error') return <ErrorState />;
  if (stateMode === 'empty') return <EmptyState title="No Urgent Calls" description="There are no active emergencies in your region. Thank you for staying ready!" />;

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', height: '100%', gap: '12px' }}>
      <h2>Verified Help</h2>
      <p style={{ margin: 0 }}>SOS emergency broadcasts verified by local trust networks.</p>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginTop: '8px' }}>
        <div style={{ padding: '16px', backgroundColor: 'var(--md-surface)', borderRadius: 'var(--md-radius-md)', borderLeft: '4px solid var(--md-error)', boxShadow: '0 2px 4px var(--md-shadow)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span style={{ fontWeight: 'bold', color: 'var(--md-error)' }}>🔴 EMERGENCY: Medical Dispatch</span>
            <span style={{ fontSize: '11px', color: 'var(--md-on-surface-variant)' }}>10m ago</span>
          </div>
          <p style={{ margin: '8px 0', fontSize: '13px' }}>Need insulin delivery for senior citizen at Pocket A-4, Janakpuri. Requires trust score 4.5+.</p>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '12px' }}>
            <span style={{ fontSize: '11px', color: 'var(--md-on-surface-variant)' }}>Min Trust Score: 4.5</span>
            <button className="btn btn-primary" style={{ width: 'auto', padding: '6px 12px', borderRadius: '8px', backgroundColor: 'var(--md-error)' }}>Volunteer Now</button>
          </div>
        </div>

        <div style={{ padding: '16px', backgroundColor: 'var(--md-surface)', borderRadius: 'var(--md-radius-md)', borderLeft: '4px solid var(--md-primary)', boxShadow: '0 2px 4px var(--md-shadow)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span style={{ fontWeight: 'bold', color: 'var(--md-primary)' }}>Water Logger Rescue</span>
            <span style={{ fontSize: '11px', color: 'var(--md-on-surface-variant)' }}>2h ago</span>
          </div>
          <p style={{ margin: '8px 0', fontSize: '13px' }}>Need volunteers with towing vehicle to clear main junction block after heavy rains.</p>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '12px' }}>
            <span style={{ fontSize: '11px', color: 'var(--md-on-surface-variant)' }}>Min Trust Score: 3.0</span>
            <button className="btn btn-primary" style={{ width: 'auto', padding: '6px 12px', borderRadius: '8px' }}>Offer Support</button>
          </div>
        </div>
      </div>
    </div>
  );
};

// 11. Need It Now Screen (Gig marketplace)
export const NeedItNowScreen: React.FC<ScreenProps> = ({ isOnline, stateMode }) => {
  if (stateMode === 'loading') return <LoadingState />;
  if (stateMode === 'error') return <ErrorState />;
  if (stateMode === 'empty') return <EmptyState title="No Active Gigs" description="Create a demand gig request below to request immediate services." />;

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', height: '100%', gap: '12px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>Need It Now</h2>
        <button className="btn btn-primary" style={{ width: 'auto', padding: '6px 12px', borderRadius: '8px' }}>Post Gig</button>
      </div>
      <p style={{ margin: 0 }}>On-demand local bids. Pay directly, sync bids dynamically.</p>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <div style={{ padding: '16px', backgroundColor: 'var(--md-surface)', borderRadius: 'var(--md-radius-md)', border: '1px solid var(--md-surface-variant)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span style={{ fontWeight: 600 }}>🔧 Plumber Needed Immediately</span>
            <span style={{ fontWeight: 'bold', color: 'var(--md-primary)' }}>₹500 Est</span>
          </div>
          <p style={{ margin: '8px 0', fontSize: '13px' }}>Kitchen sink pipe leaking heavily. Water damage starting. Need someone who can visit within 1 hour.</p>
          <div style={{ borderTop: '1px solid var(--md-surface-variant)', paddingTop: '12px', marginTop: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '12px', color: 'var(--md-primary)', fontWeight: 500 }}>3 Active Bids</span>
            <button className="btn btn-tonal" style={{ width: 'auto', padding: '6px 12px', borderRadius: '8px' }}>Place Bid</button>
          </div>
        </div>
      </div>
    </div>
  );
};

/* ==========================================================================
   SUPPORTING CONFIG/MANAGEMENT SCREENS
   ========================================================================== */

// 12. Profile Screen
export const ProfileScreen: React.FC<ScreenProps> = ({ onNavigate }) => {
  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', height: '100%', gap: '24px', alignItems: 'center', paddingTop: '24px' }}>
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
        <div style={{ width: '96px', height: '96px', borderRadius: '50%', backgroundColor: 'var(--md-primary-container)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '48px' }}>👤</div>
        <h2 style={{ margin: 0 }}>Vipin Dev</h2>
        <p style={{ margin: 0, fontSize: '13px' }}>+91 98765 43210</p>
      </div>

      <div style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: '8px' }}>
        <div style={{ padding: '12px 16px', backgroundColor: 'var(--md-surface)', borderRadius: 'var(--md-radius-md)', display: 'flex', justifyContent: 'space-between' }}>
          <span>Helper Certification</span>
          <span style={{ color: 'var(--md-primary)', fontWeight: 600 }}>Verified ✅</span>
        </div>
        <div style={{ padding: '12px 16px', backgroundColor: 'var(--md-surface)', borderRadius: 'var(--md-radius-md)', display: 'flex', justifyContent: 'space-between' }}>
          <span>Volunteer Trust Score</span>
          <span style={{ fontWeight: 600 }}>4.8 ★</span>
        </div>
      </div>

      <button className="btn btn-outline" onClick={() => onNavigate('admin')} style={{ marginTop: 'auto' }}>
        Open Admin Console
      </button>
    </div>
  );
};

// 13. Settings Screen
export const SettingsScreen: React.FC<ScreenProps & { onToggleTheme: () => void; isDark: boolean }> = ({ onNavigate, onToggleTheme, isDark }) => {
  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', height: '100%', gap: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', paddingBottom: '12px', borderBottom: '1px solid var(--md-surface-variant)' }}>
        <span style={{ cursor: 'pointer', fontSize: '20px' }} onClick={() => onNavigate('chat_list')}>⬅️</span>
        <h2 style={{ margin: 0 }}>Settings</h2>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <div style={{ padding: '16px', backgroundColor: 'var(--md-surface)', borderRadius: 'var(--md-radius-md)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <div style={{ fontWeight: 600 }}>Dark Theme</div>
            <div style={{ fontSize: '11px', color: 'var(--md-on-surface-variant)' }}>Toggle background brightness</div>
          </div>
          <input type="checkbox" checked={isDark} onChange={onToggleTheme} style={{ width: '20px', height: '20px' }} />
        </div>

        <div style={{ padding: '16px', backgroundColor: 'var(--md-surface)', borderRadius: 'var(--md-radius-md)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <div style={{ fontWeight: 600 }}>Offline Caching Mode</div>
            <div style={{ fontSize: '11px', color: 'var(--md-on-surface-variant)' }}>Cache messages in IndexedDB</div>
          </div>
          <span style={{ color: 'var(--md-primary)', fontWeight: 'bold', fontSize: '13px' }}>ACTIVE</span>
        </div>

        <div style={{ padding: '16px', backgroundColor: 'var(--md-surface)', borderRadius: 'var(--md-radius-md)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer' }} onClick={() => onNavigate('profile')}>
          <div>
            <div style={{ fontWeight: 600 }}>Account & Privacy</div>
            <div style={{ fontSize: '11px', color: 'var(--md-on-surface-variant)' }}>Edit phone number or trust settings</div>
          </div>
          <span>➡️</span>
        </div>
      </div>
    </div>
  );
};

// 14. Notifications Screen
export const NotificationsScreen: React.FC<ScreenProps> = ({ onNavigate }) => {
  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', height: '100%', gap: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', paddingBottom: '12px', borderBottom: '1px solid var(--md-surface-variant)' }}>
        <span style={{ cursor: 'pointer', fontSize: '20px' }} onClick={() => onNavigate('chat_list')}>⬅️</span>
        <h2 style={{ margin: 0 }}>Notifications</h2>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        {[
          { id: 1, title: 'New Help Request', body: 'Volunteer needed for medical dispatch nearby.', time: '10m ago' },
          { id: 2, title: 'Bid Received', body: 'Plumber Sanjay Dutt placed a bid on your Gig.', time: '1h ago' },
        ].map(item => (
          <div key={item.id} style={{ padding: '12px', backgroundColor: 'var(--md-surface)', borderRadius: 'var(--md-radius-md)', border: '1px solid var(--md-surface-variant)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <div style={{ fontWeight: 600, fontSize: '14px' }}>{item.title}</div>
              <span style={{ fontSize: '10px', color: 'var(--md-on-surface-variant)' }}>{item.time}</span>
            </div>
            <div style={{ fontSize: '12px', color: 'var(--md-on-surface-variant)', marginTop: '4px' }}>{item.body}</div>
          </div>
        ))}
      </div>
    </div>
  );
};

// 15. Admin Console Screen
export const AdminScreen: React.FC<ScreenProps> = ({ onNavigate }) => {
  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', height: '100%', gap: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', paddingBottom: '12px', borderBottom: '1px solid var(--md-surface-variant)' }}>
        <span style={{ cursor: 'pointer', fontSize: '20px' }} onClick={() => onNavigate('profile')}>⬅️</span>
        <h2 style={{ margin: 0 }}>Admin Console</h2>
      </div>
      <p style={{ margin: 0 }}>Verify local helpers, manage emergency categories, and evaluate trust score metrics.</p>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        <h3 style={{ fontSize: '14px', fontWeight: 600 }}>Pending Helper Certifications</h3>
        {[
          { id: 1, name: 'Sonia Kapoor', cert: 'Medical First Responder' },
        ].map(req => (
          <div key={req.id} style={{ padding: '12px', backgroundColor: 'var(--md-surface)', borderRadius: 'var(--md-radius-md)', border: '1px solid var(--md-surface-variant)' }}>
            <div style={{ fontWeight: 600, fontSize: '13px' }}>{req.name}</div>
            <div style={{ fontSize: '11px', color: 'var(--md-on-surface-variant)', margin: '4px 0' }}>Requesting: {req.cert}</div>
            <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
              <button className="btn btn-primary" style={{ padding: '4px 8px', borderRadius: '4px', fontSize: '11px' }}>Approve</button>
              <button className="btn btn-tonal" style={{ padding: '4px 8px', borderRadius: '4px', fontSize: '11px' }}>Reject</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
