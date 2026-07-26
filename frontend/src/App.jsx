import React, { useState, useEffect, useRef } from 'react';
import Sidebar from './components/Sidebar';
import ChatView from './components/ChatView';
import UserDiscoveryModal from './components/UserDiscoveryModal';
import GroupModal from './components/GroupModal';
import SettingsModal from './components/SettingsModal';

const API_BASE = 'http://localhost:5000/api/v1';
const WS_BASE = 'ws://localhost:5000';

export default function App() {
  const [theme, setTheme] = useState('dark');
  const [currentUser, setCurrentUser] = useState({
    user_id: 'u-101',
    username: 'vipin_k',
    display_name: 'Vipin Kumar',
    email: 'vipin@bharatconnect.com',
    status_message: 'Building BharatConnect 🚀',
    presence: 'ONLINE'
  });

  const [usersList, setUsersList] = useState([]);
  const [usersMap, setUsersMap] = useState({});
  const [chats, setChats] = useState([]);
  const [activeChat, setActiveChat] = useState(null);
  const [messages, setMessages] = useState([]);
  const [typingUsersMap, setTypingUsersMap] = useState({}); // chat_id -> Set of user_ids

  // Modals
  const [showDiscovery, setShowDiscovery] = useState(false);
  const [showGroupModal, setShowGroupModal] = useState(false);
  const [showSettings, setShowSettings] = useState(false);

  const wsRef = useRef(null);

  // Toggle Theme
  const handleToggleTheme = () => {
    const nextTheme = theme === 'dark' ? 'light' : 'dark';
    setTheme(nextTheme);
    document.documentElement.setAttribute('data-theme', nextTheme);
  };

  // Switch Active User (Identity Simulator)
  const handleSwitchUser = async (userId) => {
    try {
      const res = await fetch(`${API_BASE}/users/me`, {
        headers: { 'x-user-id': userId }
      });
      const user = await res.json();
      setCurrentUser(user);
      setActiveChat(null);
    } catch (err) {
      console.error('Error switching user:', err);
    }
  };

  // Fetch Users & Chats upon mount or user change
  useEffect(() => {
    const loadInitialData = async () => {
      try {
        const [usersRes, chatsRes] = await Promise.all([
          fetch(`${API_BASE}/users/search?q=`),
          fetch(`${API_BASE}/chats`, { headers: { 'x-user-id': currentUser.user_id } })
        ]);

        const usersData = await usersRes.json();
        const chatsData = await chatsRes.json();

        setUsersList(usersData);
        const map = {};
        usersData.forEach(u => map[u.user_id] = u);
        setUsersMap(map);

        setChats(chatsData);
        if (chatsData.length > 0 && !activeChat) {
          setActiveChat(chatsData[0]);
        }
      } catch (err) {
        console.error('Failed loading initial API data:', err);
      }
    };

    loadInitialData();
  }, [currentUser]);

  // Fetch Messages when activeChat changes
  useEffect(() => {
    if (!activeChat) return;

    const fetchMessages = async () => {
      try {
        const res = await fetch(`${API_BASE}/chats/${activeChat.chat_id}/messages`);
        const msgs = await res.json();
        setMessages(msgs);

        // Send Read Receipt via WebSocket
        if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
          wsRef.current.send(JSON.stringify({
            event: 'message.read',
            payload: { chat_id: activeChat.chat_id }
          }));
        }
      } catch (err) {
        console.error('Error fetching messages:', err);
      }
    };

    fetchMessages();
  }, [activeChat]);

  // WebSocket Connection Management
  useEffect(() => {
    const ws = new WebSocket(WS_BASE);
    wsRef.current = ws;

    ws.onopen = () => {
      ws.send(JSON.stringify({
        event: 'auth',
        payload: { user_id: currentUser.user_id }
      }));
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        const { event: evtType, payload } = data;

        if (evtType === 'message.receive' || evtType === 'message.ack') {
          const newMsg = payload;
          setMessages(prev => {
            if (prev.some(m => m.message_id === newMsg.message_id || m.client_message_id === newMsg.client_message_id)) {
              return prev.map(m => (m.client_message_id === newMsg.client_message_id ? newMsg : m));
            }
            return [...prev, newMsg];
          });

          // Refresh chat list preview
          setChats(prev => prev.map(c => {
            if (c.chat_id === newMsg.chat_id) {
              return { ...c, last_message: newMsg };
            }
            return c;
          }));
        }

        if (evtType === 'message.updated') {
          const updatedMsg = payload;
          setMessages(prev => prev.map(m => m.message_id === updatedMsg.message_id ? updatedMsg : m));
        }

        if (evtType === 'user.presence') {
          const { user_id, presence } = payload;
          setUsersMap(prev => ({
            ...prev,
            [user_id]: { ...prev[user_id], presence }
          }));
        }

        if (evtType === 'typing.start') {
          const { chat_id, user_id } = payload;
          setTypingUsersMap(prev => ({
            ...prev,
            [chat_id]: Array.from(new Set([...(prev[chat_id] || []), user_id]))
          }));
        }

        if (evtType === 'typing.stop') {
          const { chat_id, user_id } = payload;
          setTypingUsersMap(prev => ({
            ...prev,
            [chat_id]: (prev[chat_id] || []).filter(id => id !== user_id)
          }));
        }
      } catch (err) {
        console.error('WS client message error:', err);
      }
    };

    return () => {
      ws.close();
    };
  }, [currentUser]);

  // Actions
  const handleSendMessage = (msgPayload) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({
        event: 'message.send',
        payload: msgPayload
      }));
    }
  };

  const handleEditMessage = (message_id, content) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({
        event: 'message.edit',
        payload: { message_id, content }
      }));
    }
  };

  const handleDeleteMessage = (message_id, delete_type) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({
        event: 'message.delete',
        payload: { message_id, delete_type }
      }));
    }
  };

  const handleTypingStart = (chat_id) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({
        event: 'typing.start',
        payload: { chat_id }
      }));
    }
  };

  const handleTypingStop = (chat_id) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({
        event: 'typing.stop',
        payload: { chat_id }
      }));
    }
  };

  const handleStartDirectChat = async (target_user_id) => {
    try {
      const res = await fetch(`${API_BASE}/chats/direct`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-user-id': currentUser.user_id
        },
        body: JSON.stringify({ target_user_id })
      });
      const chat = await res.json();
      setChats(prev => {
        if (prev.some(c => c.chat_id === chat.chat_id)) return prev;
        return [chat, ...prev];
      });
      setActiveChat(chat);
    } catch (err) {
      console.error('Error starting direct chat:', err);
    }
  };

  const handleCreateGroup = async (groupPayload) => {
    try {
      const res = await fetch(`${API_BASE}/chats/group`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-user-id': currentUser.user_id
        },
        body: JSON.stringify(groupPayload)
      });
      const groupChat = await res.json();
      setChats(prev => [groupChat, ...prev]);
      setActiveChat(groupChat);
    } catch (err) {
      console.error('Error creating group chat:', err);
    }
  };

  const handleToggleMuteChat = (chat_id) => {
    setChats(prev => prev.map(c => c.chat_id === chat_id ? { ...c, is_muted: !c.is_muted } : c));
  };

  const handleTogglePinChat = (chat_id) => {
    setChats(prev => prev.map(c => c.chat_id === chat_id ? { ...c, is_pinned: !c.is_pinned } : c));
  };

  const handlePinMessage = (chat_id, message_id) => {
    setMessages(prev => prev.map(m => m.message_id === message_id ? { ...m, is_pinned: !m.is_pinned } : m));
  };

  return (
    <div className="app-container">
      <Sidebar
        currentUser={currentUser}
        usersList={usersList}
        chats={chats}
        activeChat={activeChat}
        onSelectChat={setActiveChat}
        onOpenNewGroup={() => setShowGroupModal(true)}
        onOpenDiscovery={() => setShowDiscovery(true)}
        onOpenSettings={() => setShowSettings(true)}
        onSwitchUser={handleSwitchUser}
        theme={theme}
        onToggleTheme={handleToggleTheme}
      />

      {activeChat ? (
        <ChatView
          currentUser={currentUser}
          usersMap={usersMap}
          chat={activeChat}
          messages={messages}
          typingUsers={typingUsersMap[activeChat.chat_id] || []}
          onSendMessage={handleSendMessage}
          onEditMessage={handleEditMessage}
          onDeleteMessage={handleDeleteMessage}
          onPinMessage={handlePinMessage}
          onTypingStart={handleTypingStart}
          onTypingStop={handleTypingStop}
          onToggleMuteChat={handleToggleMuteChat}
          onTogglePinChat={handleTogglePinChat}
        />
      ) : (
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)' }}>
          Select a conversation to start chatting
        </div>
      )}

      {/* Modals */}
      {showDiscovery && (
        <UserDiscoveryModal
          usersList={usersList}
          currentUser={currentUser}
          onStartDirectChat={handleStartDirectChat}
          onClose={() => setShowDiscovery(false)}
        />
      )}

      {showGroupModal && (
        <GroupModal
          usersList={usersList}
          currentUser={currentUser}
          onCreateGroup={handleCreateGroup}
          onClose={() => setShowGroupModal(false)}
        />
      )}

      {showSettings && (
        <SettingsModal
          currentUser={currentUser}
          theme={theme}
          onToggleTheme={handleToggleTheme}
          onClose={() => setShowSettings(false)}
        />
      )}
    </div>
  );
}
