import React, { useState, useEffect, useRef } from 'react';
import {
  Send,
  Check,
  CheckCheck,
  CornerUpLeft,
  Edit2,
  Trash2,
  Copy,
  Pin,
  MoreVertical,
  Search,
  X,
  Volume2,
  VolumeX,
  UserX,
  Shield,
  Clock
} from 'lucide-react';

export default function ChatView({
  currentUser,
  usersMap,
  chat,
  messages,
  typingUsers,
  onSendMessage,
  onEditMessage,
  onDeleteMessage,
  onPinMessage,
  onTypingStart,
  onTypingStop,
  onToggleMuteChat,
  onTogglePinChat
}) {
  const [inputText, setInputText] = useState('');
  const [replyingTo, setReplyingTo] = useState(null);
  const [editingMsg, setEditingMsg] = useState(null);
  const [chatSearch, setChatSearch] = useState('');
  const [showSearchInput, setShowSearchInput] = useState(false);
  const [contextMenuMsgId, setContextMenuMsgId] = useState(null);

  const messagesEndRef = useRef(null);

  const isGroup = chat.chat_type === 'GROUP';
  const targetUser = !isGroup ? (chat.targetUser || usersMap[chat.participants.find(p => p !== currentUser.user_id)]) : null;
  const title = isGroup ? chat.title : (targetUser?.display_name || 'Chat');
  const presence = !isGroup ? (targetUser?.presence || 'OFFLINE') : null;

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, typingUsers]);

  // Handle typing debounce
  const typingTimeoutRef = useRef(null);
  const handleInputChange = (e) => {
    setInputText(e.target.value);
    onTypingStart(chat.chat_id);

    if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
    typingTimeoutRef.current = setTimeout(() => {
      onTypingStop(chat.chat_id);
    }, 2000);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!inputText.trim()) return;

    if (editingMsg) {
      onEditMessage(editingMsg.message_id, inputText.trim());
      setEditingMsg(null);
    } else {
      onSendMessage({
        chat_id: chat.chat_id,
        content: inputText.trim(),
        parent_message_id: replyingTo ? replyingTo.message_id : null
      });
      setReplyingTo(null);
    }
    setInputText('');
    onTypingStop(chat.chat_id);
  };

  const handleCopy = (text) => {
    navigator.clipboard.writeText(text);
    setContextMenuMsgId(null);
  };

  // Filter messages if search is active
  const filteredMessages = messages.filter(m => {
    if (!chatSearch.trim()) return true;
    return m.content.toLowerCase().includes(chatSearch.toLowerCase());
  });

  const pinnedMessages = messages.filter(m => m.is_pinned && !m.is_deleted);

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', height: '100%', background: 'var(--bg-primary)' }}>
      {/* Chat Top Bar Header */}
      <header className="glass-panel" style={{ padding: '14px 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', borderRight: 'none' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div className="avatar" style={{ background: isGroup ? 'linear-gradient(135deg, #8b5cf6 0%, #ec4899 100%)' : undefined }}>
            {isGroup ? 'GR' : (title.substring(0, 2).toUpperCase())}
            {!isGroup && presence && <span className={`status-badge ${presence.toLowerCase()}`} />}
          </div>

          <div>
            <div style={{ fontWeight: '700', fontSize: '16px', color: 'var(--text-primary)' }}>
              {title}
            </div>
            <div style={{ fontSize: '12px', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '6px' }}>
              {isGroup ? (
                <span>{chat.participants.length} participants</span>
              ) : (
                <>
                  <span style={{ textTransform: 'capitalize', color: presence === 'ONLINE' ? 'var(--status-online)' : 'var(--text-muted)', fontWeight: presence === 'ONLINE' ? '600' : 'normal' }}>
                    {presence || 'Offline'}
                  </span>
                  {presence !== 'ONLINE' && targetUser?.last_seen && (
                    <span>• Last seen {new Date(targetUser.last_seen).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                  )}
                </>
              )}
            </div>
          </div>
        </div>

        {/* Action Controls */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {showSearchInput ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '4px', background: 'var(--bg-tertiary)', borderRadius: 'var(--radius-md)', padding: '2px 8px' }}>
              <input
                type="text"
                placeholder="Search in chat..."
                value={chatSearch}
                onChange={(e) => setChatSearch(e.target.value)}
                style={{ background: 'transparent', border: 'none', color: 'var(--text-primary)', outline: 'none', fontSize: '13px', width: '140px' }}
                autoFocus
              />
              <button className="btn-icon" onClick={() => { setShowSearchInput(false); setChatSearch(''); }}>
                <X size={14} />
              </button>
            </div>
          ) : (
            <button className="btn-icon" onClick={() => setShowSearchInput(true)} title="Search messages">
              <Search size={18} />
            </button>
          )}

          <button className="btn-icon" onClick={() => onTogglePinChat(chat.chat_id)} title={chat.is_pinned ? 'Unpin Chat' : 'Pin Chat'}>
            <Pin size={18} style={{ color: chat.is_pinned ? 'var(--accent-primary)' : 'inherit' }} />
          </button>

          <button className="btn-icon" onClick={() => onToggleMuteChat(chat.chat_id)} title={chat.is_muted ? 'Unmute Notifications' : 'Mute Notifications'}>
            {chat.is_muted ? <VolumeX size={18} style={{ color: 'var(--text-muted)' }} /> : <Volume2 size={18} />}
          </button>
        </div>
      </header>

      {/* Pinned Messages Banner */}
      {pinnedMessages.length > 0 && (
        <div style={{ background: 'var(--accent-subtle)', padding: '8px 24px', borderBottom: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--accent-primary)', fontWeight: '600' }}>
            <Pin size={14} />
            <span>Pinned Message: "{pinnedMessages[pinnedMessages.length - 1].content}"</span>
          </div>
        </div>
      )}

      {/* Message Feed Area */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '20px 24px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {filteredMessages.length === 0 ? (
          <div style={{ margin: 'auto', textAlign: 'center', color: 'var(--text-muted)', fontSize: '13px' }}>
            No messages yet. Send a message to start communicating!
          </div>
        ) : (
          filteredMessages.map((msg, index) => {
            const isMe = msg.sender_id === currentUser.user_id;
            const senderUser = usersMap[msg.sender_id] || { display_name: 'User' };
            const parentMsg = msg.parent_message_id ? messages.find(m => m.message_id === msg.parent_message_id) : null;

            return (
              <div
                key={msg.message_id}
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: isMe ? 'flex-end' : 'flex-start',
                  position: 'relative'
                }}
                className="animate-fade-in"
              >
                {/* Sender Name in Group */}
                {isGroup && !isMe && (
                  <span style={{ fontSize: '11px', fontWeight: '600', color: 'var(--accent-primary)', marginBottom: '4px', marginLeft: '12px' }}>
                    {senderUser.display_name}
                  </span>
                )}

                {/* Message Bubble Container */}
                <div
                  style={{
                    position: 'relative',
                    maxWidth: '65%',
                    background: isMe ? 'var(--msg-sent-bg)' : 'var(--msg-received-bg)',
                    color: '#ffffff',
                    padding: '10px 14px',
                    borderRadius: isMe ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
                    boxShadow: 'var(--shadow-sm)',
                    border: msg.is_pinned ? '1px solid var(--accent-primary)' : '1px solid transparent'
                  }}
                  onMouseEnter={() => setContextMenuMsgId(msg.message_id)}
                  onMouseLeave={() => setContextMenuMsgId(null)}
                >
                  {/* Quoted Parent Reply Preview */}
                  {parentMsg && (
                    <div style={{ background: 'rgba(0,0,0,0.2)', borderLeft: '3px solid var(--accent-primary)', padding: '4px 8px', borderRadius: '4px', marginBottom: '6px', fontSize: '12px' }}>
                      <div style={{ fontWeight: '600', color: 'var(--accent-primary)' }}>
                        {parentMsg.sender_id === currentUser.user_id ? 'You' : (usersMap[parentMsg.sender_id]?.display_name || 'User')}
                      </div>
                      <div style={{ color: 'var(--text-secondary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {parentMsg.content}
                      </div>
                    </div>
                  )}

                  {/* Message Content */}
                  <div style={{ fontSize: '14px', lineHeight: '1.4', wordBreak: 'break-word' }}>
                    {msg.is_deleted ? (
                      <em style={{ color: 'var(--text-muted)' }}>This message was deleted</em>
                    ) : (
                      msg.content
                    )}
                  </div>

                  {/* Message Footer: Timestamp, Edit Tag, Status Ticks */}
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '4px', marginTop: '4px', fontSize: '10px', color: isMe ? 'rgba(255,255,255,0.7)' : 'var(--text-muted)' }}>
                    {msg.is_edited && <span>(edited)</span>}
                    <span>{new Date(msg.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>

                    {isMe && !msg.is_deleted && (
                      msg.status === 'READ' ? (
                        <CheckCheck size={14} style={{ color: '#60a5fa' }} title="Read" />
                      ) : msg.status === 'DELIVERED' ? (
                        <CheckCheck size={14} style={{ color: '#d1d5db' }} title="Delivered" />
                      ) : (
                        <Check size={14} style={{ color: '#d1d5db' }} title="Sent" />
                      )
                    )}
                  </div>

                  {/* Hover Quick Actions Context Toolbar */}
                  {contextMenuMsgId === msg.message_id && !msg.is_deleted && (
                    <div
                      style={{
                        position: 'absolute',
                        top: '-32px',
                        right: isMe ? '0' : 'auto',
                        left: isMe ? 'auto' : '0',
                        background: 'var(--bg-secondary)',
                        border: '1px solid var(--border-color)',
                        borderRadius: 'var(--radius-md)',
                        padding: '2px 6px',
                        display: 'flex',
                        gap: '4px',
                        boxShadow: 'var(--shadow-md)',
                        zIndex: 10
                      }}
                    >
                      <button className="btn-icon" onClick={() => setReplyingTo(msg)} title="Reply">
                        <CornerUpLeft size={14} />
                      </button>
                      <button className="btn-icon" onClick={() => handleCopy(msg.content)} title="Copy Text">
                        <Copy size={14} />
                      </button>
                      <button className="btn-icon" onClick={() => onPinMessage(chat.chat_id, msg.message_id)} title="Pin Message">
                        <Pin size={14} style={{ color: msg.is_pinned ? 'var(--accent-primary)' : 'inherit' }} />
                      </button>

                      {isMe && (
                        <>
                          <button className="btn-icon" onClick={() => { setEditingMsg(msg); setInputText(msg.content); }} title="Edit Message">
                            <Edit2 size={14} />
                          </button>
                          <button className="btn-icon" onClick={() => onDeleteMessage(msg.message_id, 'everyone')} title="Delete for Everyone" style={{ color: '#ef4444' }}>
                            <Trash2 size={14} />
                          </button>
                        </>
                      )}
                    </div>
                  )}
                </div>
              </div>
            );
          })
        )}

        {/* Realtime Typing Indicator */}
        {typingUsers && typingUsers.length > 0 && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '12px', color: 'var(--text-secondary)', marginLeft: '12px' }}>
            <span className="typing-dots">
              <span className="typing-dot" />
              <span className="typing-dot" />
              <span className="typing-dot" />
            </span>
            <span>{typingUsers.map(id => usersMap[id]?.display_name || 'Someone').join(', ')} is typing...</span>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Reply or Edit Banner */}
      {(replyingTo || editingMsg) && (
        <div style={{ background: 'var(--bg-tertiary)', padding: '8px 24px', borderTop: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '13px' }}>
          <div>
            <span style={{ fontWeight: '600', color: 'var(--accent-primary)' }}>
              {replyingTo ? `Replying to ${replyingTo.sender_id === currentUser.user_id ? 'yourself' : (usersMap[replyingTo.sender_id]?.display_name || 'User')}` : 'Editing message'}
            </span>
            <div style={{ color: 'var(--text-secondary)', fontSize: '12px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '400px' }}>
              {replyingTo ? replyingTo.content : editingMsg.content}
            </div>
          </div>
          <button className="btn-icon" onClick={() => { setReplyingTo(null); setEditingMsg(null); setInputText(''); }}>
            <X size={16} />
          </button>
        </div>
      )}

      {/* Message Input Form */}
      <form onSubmit={handleSubmit} style={{ padding: '16px 24px', background: 'var(--bg-secondary)', borderTop: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', gap: '12px' }}>
        <input
          type="text"
          className="input-field"
          placeholder="Write a message... (Press Enter to send)"
          value={inputText}
          onChange={handleInputChange}
          style={{ flex: 1 }}
        />
        <button type="submit" className="btn-primary" disabled={!inputText.trim()} style={{ opacity: !inputText.trim() ? 0.5 : 1 }}>
          <Send size={16} />
        </button>
      </form>
    </div>
  );
}
