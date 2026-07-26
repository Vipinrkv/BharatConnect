import React, { useState } from 'react';
import {
  MessageSquare,
  Users,
  Search,
  Settings,
  Plus,
  Pin,
  VolumeX,
  Archive,
  UserPlus,
  Circle,
  Moon,
  Sun
} from 'lucide-react';

export default function Sidebar({
  currentUser,
  usersList,
  chats,
  activeChat,
  onSelectChat,
  onOpenNewGroup,
  onOpenDiscovery,
  onOpenSettings,
  onSwitchUser,
  theme,
  onToggleTheme
}) {
  const [searchQuery, setSearchQuery] = useState('');
  const [filterTab, setFilterTab] = useState('ALL'); // ALL, DIRECT, GROUPS, UNREAD, PINNED

  // Filtered Chats Logic
  const filteredChats = chats.filter(chat => {
    const title = chat.title || chat.targetUser?.display_name || chat.targetUser?.username || '';
    const matchesQuery = title.toLowerCase().includes(searchQuery.toLowerCase());

    if (!matchesQuery) return false;
    if (filterTab === 'DIRECT') return chat.chat_type === 'DIRECT';
    if (filterTab === 'GROUPS') return chat.chat_type === 'GROUP';
    if (filterTab === 'UNREAD') return chat.unread_count > 0;
    if (filterTab === 'PINNED') return chat.is_pinned;
    return true;
  });

  const getInitials = (name) => {
    if (!name) return 'BC';
    return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
  };

  return (
    <aside className="glass-panel" style={{ width: '360px', display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Top Header & User Identity */}
      <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div className="avatar" style={{ background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)' }}>
            {getInitials(currentUser.display_name)}
            <span className={`status-badge ${currentUser.presence.toLowerCase()}`} />
          </div>
          <div>
            <div style={{ fontWeight: '700', fontSize: '15px', display: 'flex', alignItems: 'center', gap: '6px' }}>
              {currentUser.display_name}
            </div>
            <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
              @{currentUser.username}
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
          <button className="btn-icon" onClick={onToggleTheme} title="Toggle Dark/Light Mode">
            {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
          </button>
          <button className="btn-icon" onClick={onOpenSettings} title="Settings & Preferences">
            <Settings size={18} />
          </button>
        </div>
      </div>

      {/* Identity Tester Switcher Dropdown (To test multi-user chat easily!) */}
      <div style={{ padding: '8px 20px', background: 'var(--bg-tertiary)', borderBottom: '1px solid var(--border-color)', fontSize: '12px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span style={{ color: 'var(--text-muted)' }}>Simulate Active Account:</span>
        <select
          value={currentUser.user_id}
          onChange={(e) => onSwitchUser(e.target.value)}
          style={{ background: 'var(--bg-secondary)', color: 'var(--text-primary)', border: '1px solid var(--border-color)', borderRadius: '4px', padding: '2px 8px', fontSize: '12px', outline: 'none', cursor: 'pointer' }}
        >
          {usersList.map(u => (
            <option key={u.user_id} value={u.user_id}>
              {u.display_name} (@{u.username})
            </option>
          ))}
        </select>
      </div>

      {/* Action Buttons & Quick Search */}
      <div style={{ padding: '12px 20px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button className="btn-primary" style={{ flex: 1, justifyContent: 'center' }} onClick={onOpenDiscovery}>
            <UserPlus size={16} /> New Chat
          </button>
          <button className="btn-primary" style={{ background: 'var(--bg-tertiary)', color: 'var(--text-primary)' }} onClick={onOpenNewGroup} title="Create Group Chat">
            <Users size={16} /> Group
          </button>
        </div>

        <div style={{ position: 'relative' }}>
          <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            type="text"
            className="input-field"
            placeholder="Search chats, contacts or handle..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ paddingLeft: '36px' }}
          />
        </div>

        {/* Filter Pills */}
        <div style={{ display: 'flex', gap: '6px', overflowX: 'auto', paddingBottom: '4px' }}>
          {['ALL', 'DIRECT', 'GROUPS', 'UNREAD', 'PINNED'].map(tab => (
            <button
              key={tab}
              onClick={() => setFilterTab(tab)}
              style={{
                background: filterTab === tab ? 'var(--accent-primary)' : 'var(--bg-tertiary)',
                color: filterTab === tab ? '#ffffff' : 'var(--text-secondary)',
                border: 'none',
                padding: '4px 10px',
                borderRadius: 'var(--radius-full)',
                fontSize: '11px',
                fontWeight: '600',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
                whiteSpace: 'nowrap'
              }}
            >
              {tab}
            </button>
          ))}
        </div>
      </div>

      {/* Chat List */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '0 12px 12px 12px' }}>
        {filteredChats.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px 20px', color: 'var(--text-muted)', fontSize: '13px' }}>
            No chats found
          </div>
        ) : (
          filteredChats.map(chat => {
            const isActive = activeChat && activeChat.chat_id === chat.chat_id;
            const isGroup = chat.chat_type === 'GROUP';
            const title = isGroup ? chat.title : (chat.targetUser?.display_name || 'Unknown User');
            const presence = isGroup ? null : (chat.targetUser?.presence || 'OFFLINE');
            const lastMsg = chat.last_message;

            return (
              <div
                key={chat.chat_id}
                onClick={() => onSelectChat(chat)}
                style={{
                  padding: '12px',
                  borderRadius: 'var(--radius-md)',
                  background: isActive ? 'var(--accent-subtle)' : 'transparent',
                  borderLeft: isActive ? '3px solid var(--accent-primary)' : '3px solid transparent',
                  cursor: 'pointer',
                  marginBottom: '4px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  transition: 'all 0.15s ease'
                }}
                className="chat-item-row"
              >
                <div className="avatar" style={{ background: isGroup ? 'linear-gradient(135deg, #8b5cf6 0%, #ec4899 100%)' : undefined }}>
                  {isGroup ? <Users size={20} /> : getInitials(title)}
                  {!isGroup && presence && <span className={`status-badge ${presence.toLowerCase()}`} />}
                </div>

                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2px' }}>
                    <span style={{ fontWeight: '600', fontSize: '14px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', color: isActive ? 'var(--text-primary)' : 'var(--text-primary)' }}>
                      {title}
                    </span>
                    {lastMsg && (
                      <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                        {new Date(lastMsg.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    )}
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontSize: '13px', color: 'var(--text-secondary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '180px' }}>
                      {lastMsg ? (lastMsg.is_deleted ? 'This message was deleted' : lastMsg.content) : 'No messages yet'}
                    </span>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      {chat.is_pinned && <Pin size={12} style={{ color: 'var(--accent-primary)' }} />}
                      {chat.is_muted && <VolumeX size={12} style={{ color: 'var(--text-muted)' }} />}
                      {chat.unread_count > 0 && (
                        <span style={{ background: 'var(--accent-primary)', color: '#ffffff', fontSize: '10px', fontWeight: '700', borderRadius: 'var(--radius-full)', padding: '2px 6px', minWidth: '18px', textAlign: 'center' }}>
                          {chat.unread_count}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </aside>
  );
}
