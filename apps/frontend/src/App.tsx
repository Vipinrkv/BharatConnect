import React, { useState } from 'react';
import {
  SplashScreen,
  OnboardingScreen,
  LoginScreen,
  OtpScreen,
  GoogleLoginScreen,
  ChatListScreen,
  ChatWindowScreen,
  GroupChatScreen,
  NearbyRightNowScreen,
  VerifiedHelpScreen,
  NeedItNowScreen,
  ProfileScreen,
  SettingsScreen,
  NotificationsScreen,
  AdminScreen
} from './features/screens';

function App() {
  const [currentScreen, setCurrentScreen] = useState<string>('splash');
  const [isOnline, setIsOnline] = useState<boolean>(true);
  const [isDark, setIsDark] = useState<boolean>(true);
  const [stateMode, setStateMode] = useState<'normal' | 'loading' | 'empty' | 'error'>('normal');

  // Router dispatcher
  const renderScreen = () => {
    const props = { onNavigate: setCurrentScreen, isOnline, stateMode };
    switch (currentScreen) {
      case 'splash':
        return <SplashScreen {...props} />;
      case 'onboarding':
        return <OnboardingScreen {...props} />;
      case 'login':
        return <LoginScreen {...props} />;
      case 'otp':
        return <OtpScreen {...props} />;
      case 'google_login':
        return <GoogleLoginScreen {...props} />;
      case 'chat_list':
        return <ChatListScreen {...props} />;
      case 'chat_window':
        return <ChatWindowScreen {...props} />;
      case 'group_chat':
        return <GroupChatScreen {...props} />;
      case 'nearby':
        return <NearbyRightNowScreen {...props} />;
      case 'help':
        return <VerifiedHelpScreen {...props} />;
      case 'need_it_now':
        return <NeedItNowScreen {...props} />;
      case 'profile':
        return <ProfileScreen {...props} />;
      case 'settings':
        return <SettingsScreen {...props} onToggleTheme={() => setIsDark(!isDark)} isDark={isDark} />;
      case 'notifications':
        return <NotificationsScreen {...props} />;
      case 'admin':
        return <AdminScreen {...props} />;
      default:
        return <SplashScreen {...props} />;
    }
  };

  // Nav Item activation checks
  const isTabActive = (tab: string) => {
    if (tab === 'chat' && (currentScreen === 'chat_list' || currentScreen === 'chat_window' || currentScreen === 'group_chat')) return true;
    return currentScreen === tab;
  };

  const showNavbar = ['chat_list', 'chat_window', 'group_chat', 'nearby', 'help', 'need_it_now', 'profile', 'settings', 'notifications', 'admin'].includes(currentScreen);

  return (
    <div style={{ display: 'flex', gap: '32px', padding: '32px', minWidth: '960px', justifyContent: 'center', alignItems: 'flex-start' }}>
      
      {/* 1. Android Phone Simulator Frame */}
      <div className={`android-device ${isDark ? 'dark-theme' : ''}`}>
        
        {/* Device Status Bar */}
        <div className="status-bar">
          <div>14:02</div>
          <div className="status-bar-icons">
            <span style={{ fontSize: '11px' }}>{isOnline ? '📶 LTE' : '✈️ OFFLINE'}</span>
            <span>🔋 85%</span>
          </div>
        </div>

        {/* Offline Banner alert indicator */}
        {!isOnline && (
          <div className="offline-banner">
            <span>🔌</span> Running in Offline Mesh Mode (Dexie caching active)
          </div>
        )}

        {/* Page Body Viewport */}
        <div className="screen-container">
          {renderScreen()}
        </div>

        {/* Device Bottom Tab Navigation Bar */}
        {showNavbar && (
          <div className="navigation-bar">
            <div className={`nav-item ${isTabActive('chat') ? 'active' : ''}`} onClick={() => setCurrentScreen('chat_list')}>
              <div className="nav-icon-wrapper">💬</div>
              <span>Chats</span>
            </div>
            <div className={`nav-item ${isTabActive('nearby') ? 'active' : ''}`} onClick={() => setCurrentScreen('nearby')}>
              <div className="nav-icon-wrapper">📍</div>
              <span>Nearby</span>
            </div>
            <div className={`nav-item ${isTabActive('help') ? 'active' : ''}`} onClick={() => setCurrentScreen('help')}>
              <div className="nav-icon-wrapper">🚨</div>
              <span>SOS</span>
            </div>
            <div className={`nav-item ${isTabActive('need_it_now') ? 'active' : ''}`} onClick={() => setCurrentScreen('need_it_now')}>
              <div className="nav-icon-wrapper">🛒</div>
              <span>Gigs</span>
            </div>
            <div className={`nav-item ${isTabActive('profile') ? 'active' : ''}`} onClick={() => setCurrentScreen('profile')}>
              <div className="nav-icon-wrapper">👤</div>
              <span>Profile</span>
            </div>
          </div>
        )}
      </div>

      {/* 2. Designer Live Control Console Board */}
      <div style={{ width: '400px', padding: '24px', backgroundColor: '#1e1e1e', color: '#e0e0e0', borderRadius: '16px', border: '1px solid #333', display: 'flex', flexDirection: 'column', gap: '20px', boxShadow: '0 8px 32px rgba(0,0,0,0.5)' }}>
        <div>
          <h2 style={{ color: '#ffffff', margin: 0 }}>Design Console</h2>
          <p style={{ fontSize: '12px', color: '#999', margin: '4px 0 0 0' }}>Interactive testbed for the BharatConnect UI System.</p>
        </div>

        {/* Device Environment Toggles */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <label style={{ fontSize: '12px', fontWeight: 'bold', color: '#999' }}>DEVICE CONTROLS</label>
          <div style={{ display: 'flex', gap: '8px' }}>
            <button className="btn" style={{ flex: 1, backgroundColor: isDark ? '#333' : 'var(--md-primary)', color: '#fff', fontSize: '12px', padding: '8px 12px' }} onClick={() => setIsDark(!isDark)}>
              Toggle {isDark ? 'Light Mode ☀️' : 'Dark Mode 🌙'}
            </button>
            <button className="btn" style={{ flex: 1, backgroundColor: isOnline ? '#2e7d32' : '#c62828', color: '#fff', fontSize: '12px', padding: '8px 12px' }} onClick={() => setIsOnline(!isOnline)}>
              {isOnline ? 'Go Offline ✈️' : 'Go Online 📶'}
            </button>
          </div>
        </div>

        {/* Dynamic UI State simulation */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <label style={{ fontSize: '12px', fontWeight: 'bold', color: '#999' }}>SIMULATED STATE PORTS</label>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
            {(['normal', 'loading', 'empty', 'error'] as const).map(mode => (
              <button key={mode} className="btn" style={{ backgroundColor: stateMode === mode ? 'var(--md-primary)' : '#333', color: '#fff', fontSize: '12px', padding: '8px 4px', textTransform: 'capitalize' }} onClick={() => setStateMode(mode)}>
                {mode}
              </button>
            ))}
          </div>
        </div>

        {/* Direct Screen Router */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <label style={{ fontSize: '12px', fontWeight: 'bold', color: '#999' }}>DIRECT SCREEN ACCESS</label>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '6px', maxHeight: '240px', overflowY: 'auto', paddingRight: '4px' }}>
            {[
              { id: 'splash', label: '1. Splash' },
              { id: 'onboarding', label: '2. Onboarding' },
              { id: 'login', label: '3. Phone Login' },
              { id: 'otp', label: '4. OTP Code' },
              { id: 'google_login', label: '5. Google Login' },
              { id: 'chat_list', label: '6. Chat List' },
              { id: 'chat_window', label: '7. Chat Thread' },
              { id: 'group_chat', label: '8. Group Chat' },
              { id: 'nearby', label: '9. Nearby Nodes' },
              { id: 'help', label: '10. SOS Board' },
              { id: 'need_it_now', label: '11. Gig Bids' },
              { id: 'profile', label: '12. User Profile' },
              { id: 'settings', label: '13. Settings' },
              { id: 'notifications', label: '14. Alerts' },
              { id: 'admin', label: '15. Admin panel' },
            ].map(scr => (
              <button key={scr.id} className="btn" style={{ backgroundColor: currentScreen === scr.id ? '#444' : '#222', border: '1px solid #3c3c3c', color: currentScreen === scr.id ? 'var(--md-primary)' : '#ccc', fontSize: '11px', padding: '6px 4px', textAlign: 'left', justifyContent: 'flex-start' }} onClick={() => { setCurrentScreen(scr.id); setStateMode('normal'); }}>
                {scr.label}
              </button>
            ))}
          </div>
        </div>

        {/* Material 3 specification links */}
        <div style={{ fontSize: '11px', color: '#888', borderTop: '1px solid #333', paddingTop: '12px', lineHeight: '1.4' }}>
          <strong>Accessibility Metrics:</strong><br />
          • Touch targets: Minimum 48dp x 48dp sizing<br />
          • Contrast: WCAG AA compliant HSL tokens<br />
          • Off-grid: Caches views in Dexie client instantly
        </div>
      </div>

    </div>
  );
}

export default App;
