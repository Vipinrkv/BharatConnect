import React, { useState } from 'react';
import { X, Search, UserPlus, Check } from 'lucide-react';

export default function UserDiscoveryModal({ usersList, currentUser, onStartDirectChat, onClose }) {
  const [searchQuery, setSearchQuery] = useState('');

  const filteredUsers = usersList.filter(u =>
    u.user_id !== currentUser.user_id &&
    (u.username.toLowerCase().includes(searchQuery.toLowerCase()) ||
     u.display_name.toLowerCase().includes(searchQuery.toLowerCase()) ||
     u.email.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  return (
    <div className="modal-overlay animate-fade-in">
      <div className="modal-card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3 style={{ fontSize: '18px', fontWeight: '700' }}>User Discovery</h3>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>

        <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '16px' }}>
          Find users by `@username`, display name, or registered email.
        </p>

        <div style={{ position: 'relative', marginBottom: '16px' }}>
          <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            type="text"
            className="input-field"
            placeholder="Type handle e.g. @rahul_dev..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ paddingLeft: '36px' }}
            autoFocus
          />
        </div>

        <div style={{ maxHeight: '280px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {filteredUsers.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '20px', color: 'var(--text-muted)', fontSize: '13px' }}>
              No matching users found
            </div>
          ) : (
            filteredUsers.map(user => (
              <div
                key={user.user_id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '10px 12px',
                  borderRadius: 'var(--radius-md)',
                  background: 'var(--bg-tertiary)'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <div className="avatar">
                    {user.display_name.substring(0, 2).toUpperCase()}
                  </div>
                  <div>
                    <div style={{ fontWeight: '600', fontSize: '14px' }}>{user.display_name}</div>
                    <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>@{user.username}</div>
                  </div>
                </div>

                <button
                  className="btn-primary"
                  style={{ padding: '6px 12px', fontSize: '12px' }}
                  onClick={() => {
                    onStartDirectChat(user.user_id);
                    onClose();
                  }}
                >
                  <UserPlus size={14} /> Message
                </button>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
