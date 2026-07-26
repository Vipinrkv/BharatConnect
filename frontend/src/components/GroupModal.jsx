import React, { useState } from 'react';
import { X, Users, Check } from 'lucide-react';

export default function GroupModal({ usersList, currentUser, onCreateGroup, onClose }) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [selectedUserIds, setSelectedUserIds] = useState([]);

  const candidates = usersList.filter(u => u.user_id !== currentUser.user_id);

  const toggleSelectUser = (id) => {
    if (selectedUserIds.includes(id)) {
      setSelectedUserIds(selectedUserIds.filter(i => i !== id));
    } else {
      setSelectedUserIds([...selectedUserIds, id]);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!title.trim()) return;
    onCreateGroup({ title: title.trim(), description: description.trim(), participant_ids: selectedUserIds });
    onClose();
  };

  return (
    <div className="modal-overlay animate-fade-in">
      <div className="modal-card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3 style={{ fontSize: '18px', fontWeight: '700' }}>Create Group Chat</h3>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div>
            <label style={{ fontSize: '12px', fontWeight: '600', color: 'var(--text-secondary)', display: 'block', marginBottom: '6px' }}>
              Group Title *
            </label>
            <input
              type="text"
              className="input-field"
              placeholder="e.g. Engineering Leadership"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
            />
          </div>

          <div>
            <label style={{ fontSize: '12px', fontWeight: '600', color: 'var(--text-secondary)', display: 'block', marginBottom: '6px' }}>
              Description
            </label>
            <input
              type="text"
              className="input-field"
              placeholder="Brief topic or rules..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <div>
            <label style={{ fontSize: '12px', fontWeight: '600', color: 'var(--text-secondary)', display: 'block', marginBottom: '6px' }}>
              Select Participants ({selectedUserIds.length})
            </label>

            <div style={{ maxHeight: '180px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '6px', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)', padding: '8px' }}>
              {candidates.map(user => {
                const isSelected = selectedUserIds.includes(user.user_id);
                return (
                  <div
                    key={user.user_id}
                    onClick={() => toggleSelectUser(user.user_id)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '8px 12px',
                      borderRadius: 'var(--radius-sm)',
                      background: isSelected ? 'var(--accent-subtle)' : 'transparent',
                      cursor: 'pointer'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <div className="avatar" style={{ width: '32px', height: '32px', fontSize: '12px' }}>
                        {user.display_name.substring(0, 2).toUpperCase()}
                      </div>
                      <div>
                        <div style={{ fontWeight: '600', fontSize: '13px' }}>{user.display_name}</div>
                        <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>@{user.username}</div>
                      </div>
                    </div>

                    {isSelected && <Check size={16} style={{ color: 'var(--accent-primary)' }} />}
                  </div>
                );
              })}
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
            <button type="button" className="btn-icon" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn-primary" disabled={!title.trim()}>
              <Users size={16} /> Create Group
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
