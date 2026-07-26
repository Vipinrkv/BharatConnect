import React, { useState } from 'react';
import { X, Shield, Bell, Moon, Sun, Lock, Globe, Eye } from 'lucide-react';

export default function SettingsModal({ currentUser, theme, onToggleTheme, onClose }) {
  const [readReceipts, setReadReceipts] = useState(true);
  const [presenceVisibility, setPresenceVisibility] = useState('EVERYONE');
  const [language, setLanguage] = useState('English');

  return (
    <div className="modal-overlay animate-fade-in">
      <div className="modal-card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3 style={{ fontSize: '18px', fontWeight: '700' }}>Settings & Preferences</h3>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {/* Appearance */}
          <div>
            <h4 style={{ fontSize: '13px', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-muted)', marginBottom: '10px' }}>
              Appearance
            </h4>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'var(--bg-tertiary)', padding: '12px', borderRadius: 'var(--radius-md)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                {theme === 'dark' ? <Moon size={18} /> : <Sun size={18} />}
                <div>
                  <div style={{ fontWeight: '600', fontSize: '14px' }}>Theme Mode</div>
                  <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>Currently using {theme} theme</div>
                </div>
              </div>
              <button className="btn-primary" style={{ padding: '6px 12px', fontSize: '12px' }} onClick={onToggleTheme}>
                Switch to {theme === 'dark' ? 'Light' : 'Dark'}
              </button>
            </div>
          </div>

          {/* Privacy & Presence */}
          <div>
            <h4 style={{ fontSize: '13px', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-muted)', marginBottom: '10px' }}>
              Privacy & Security
            </h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'var(--bg-tertiary)', padding: '12px', borderRadius: 'var(--radius-md)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <Eye size={18} />
                  <div>
                    <div style={{ fontWeight: '600', fontSize: '14px' }}>Read Receipts</div>
                    <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>Send blue ticks when opening messages</div>
                  </div>
                </div>
                <input
                  type="checkbox"
                  checked={readReceipts}
                  onChange={(e) => setReadReceipts(e.target.checked)}
                  style={{ width: '18px', height: '18px', cursor: 'pointer' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'var(--bg-tertiary)', padding: '12px', borderRadius: 'var(--radius-md)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <Shield size={18} />
                  <div>
                    <div style={{ fontWeight: '600', fontSize: '14px' }}>Who can see your online presence?</div>
                    <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>Presence visibility privacy level</div>
                  </div>
                </div>
                <select
                  value={presenceVisibility}
                  onChange={(e) => setPresenceVisibility(e.target.value)}
                  style={{ background: 'var(--bg-secondary)', color: 'var(--text-primary)', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-sm)', padding: '4px 8px', fontSize: '12px' }}
                >
                  <option value="EVERYONE">Everyone</option>
                  <option value="CONTACTS">Contacts Only</option>
                  <option value="NOBODY">Nobody (Invisible)</option>
                </select>
              </div>
            </div>
          </div>

          {/* Regional & Language */}
          <div>
            <h4 style={{ fontSize: '13px', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-muted)', marginBottom: '10px' }}>
              Language & Regional
            </h4>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'var(--bg-tertiary)', padding: '12px', borderRadius: 'var(--radius-md)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Globe size={18} />
                <div>
                  <div style={{ fontWeight: '600', fontSize: '14px' }}>Display Language</div>
                  <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>App interface language</div>
                </div>
              </div>
              <select
                value={language}
                onChange={(e) => setLanguage(e.target.value)}
                style={{ background: 'var(--bg-secondary)', color: 'var(--text-primary)', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-sm)', padding: '4px 8px', fontSize: '12px' }}
              >
                <option value="English">English</option>
                <option value="Hindi">हिंदी (Hindi)</option>
                <option value="Bengali">বাংলা (Bengali)</option>
                <option value="Telugu">తెలుగు (Telugu)</option>
                <option value="Tamil">தமிழ் (Tamil)</option>
              </select>
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '10px' }}>
            <button className="btn-primary" onClick={onClose}>Done</button>
          </div>
        </div>
      </div>
    </div>
  );
}
