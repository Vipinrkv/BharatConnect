import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  Search, MessageSquare, Users, Megaphone, ShieldAlert, Sparkles, 
  Plus, ShieldAlert as SecretIcon, Check, CheckCheck, UserCheck2, Loader2, Info
} from 'lucide-react';
import { Chat, UserProfile } from '../types';

interface ChatListProps {
  chats: Chat[];
  onSelectChat: (chat: Chat) => void;
  isDarkMode: boolean;
  onOpenNewChatModal: () => void;
}

export default function ChatListView({ chats, onSelectChat, isDarkMode, onOpenNewChatModal }: ChatListProps) {
  const [activeTab, setActiveTab] = useState<'all' | 'direct' | 'group' | 'channel' | 'secret'>('all');
  const [searchQuery, setSearchQuery] = useState('');

  // Filtering based on tab & search query
  const filteredChats = chats.filter((c) => {
    const matchesTab = activeTab === 'all' || c.category === activeTab;
    const matchesSearch = c.user.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          c.lastMessage.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          c.user.username.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesTab && matchesSearch;
  });

  const getCategoryIcon = (category: string) => {
    switch (category) {
      case 'group': return <Users className="w-3.5 h-3.5 text-blue-400" />;
      case 'channel': return <Megaphone className="w-3.5 h-3.5 text-amber-500" />;
      case 'secret': return <SecretIcon className="w-3.5 h-3.5 text-elegant-gold" />;
      default: return <MessageSquare className="w-3.5 h-3.5 text-slate-400" />;
    }
  };

  const getStatusIcon = (status: 'sent' | 'delivered' | 'read') => {
    switch (status) {
      case 'sent': return <Check className="w-3.5 h-3.5 text-slate-500" />;
      case 'delivered': return <CheckCheck className="w-3.5 h-3.5 text-slate-500" />;
      case 'read': return <CheckCheck className={`w-3.5 h-3.5 ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`} />;
    }
  };

  return (
    <div className="flex flex-col h-full w-full relative">
      {/* Top Banner & Premium Filters Row */}
      <div className={`p-4 pb-2 border-b transition-colors ${isDarkMode ? 'border-elegant-border/10 bg-elegant-card/50' : 'border-[#dfd5c6]/30 bg-[#f5f2eb]/40'}`}>
        {/* Modern Search */}
        <div className="relative mb-3">
          <span className="absolute left-3 top-2.5 text-slate-400">
            <Search className="w-4 h-4" />
          </span>
          <input
            id="chat_search"
            type="text"
            placeholder="Search Directs, Groups, Channels..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className={`w-full pl-9 pr-4 py-2 text-xs rounded-full border outline-none duration-150 ${
              isDarkMode 
                ? 'bg-elegant-bg border-elegant-border/20 text-white placeholder-slate-600 focus:ring-1 focus:ring-elegant-gold/45' 
                : 'bg-[#fbfaf6] border-[#dfd5c6] text-[#3c3730] placeholder-slate-400 focus:ring-1 focus:ring-elegant-gold/45'
            }`}
          />
        </div>

        {/* Categories Tab Selector (WhatsApp/Telegram hybrid) */}
        <div className="flex gap-1.5 overflow-x-auto pb-1 scrollbar-none">
          {(['all', 'direct', 'group', 'channel', 'secret'] as const).map((tab) => {
            const isSelected = activeTab === tab;
            return (
              <button
                key={tab}
                id={`tab_filter_${tab}`}
                onClick={() => setActiveTab(tab)}
                className={`px-3.5 py-1.5 rounded-full text-[11px] font-medium tracking-wide border capitalize transition-all shrink-0 cursor-pointer ${
                  isSelected
                    ? 'bg-gradient-to-r from-elegant-gold-dark to-elegant-gold text-elegant-bg font-bold border-transparent shadow-sm'
                    : isDarkMode
                    ? 'bg-elegant-card border-elegant-border/25 text-slate-400 hover:text-white hover:bg-elegant-card-hover'
                    : 'bg-[#f5f2eb] border-[#dfd5c6]/60 text-[#5c5346] hover:text-[#1c1a17] hover:bg-[#dfd5c6]/30'
                }`}
              >
                {tab === 'all' ? 'All Chats' : tab}
              </button>
            );
          })}
        </div>
      </div>

      {/* Main chats row listing */}
      <div className="flex-1 overflow-y-auto p-2 space-y-1 scrollbar-thin">
        <AnimatePresence>
          {filteredChats.length > 0 ? (
            filteredChats.map((chat) => {
              const latestMessageObj = chat.messages[chat.messages.length - 1];
              return (
                <motion.div
                  key={chat.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                  onClick={() => onSelectChat(chat)}
                  className={`flex items-center gap-3 p-3 rounded-2xl cursor-pointer transition-all duration-200 relative group border border-transparent ${
                    isDarkMode 
                      ? 'hover:bg-elegant-card hover:border-elegant-border/15 active:bg-elegant-card' 
                      : 'hover:bg-[#fbfaf6] hover:border-[#dfd5c6]/40 active:bg-[#e9e4d9] shadow-sm'
                  }`}
                >
                  {/* Left Avatar with status notch */}
                  <div className="relative">
                    <div className={`w-11 h-11 rounded-full ${chat.user.avatarBg} flex items-center justify-center font-bold text-sm text-white select-none relative shadow-inner`}>
                      {chat.user.avatar}

                      {/* Role specific premium badge overlay */}
                      {chat.user.role === 'verified_responder' && (
                        <span className="absolute -bottom-1 -right-1 bg-rose-500 text-white rounded-full p-0.5 border border-slate-950 text-[8px]" title="First Responder">
                          🚑
                        </span>
                      )}
                      {chat.user.role === 'moderator' && (
                        <span className="absolute -bottom-1 -right-1 bg-yellow-500 text-black rounded-full p-0.5 border border-slate-950 text-[8px]" title="Moderator">
                          🛡️
                        </span>
                      )}
                    </div>

                    {/* Discord-style status badge widget */}
                    <span 
                      className={`absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 ${
                        isDarkMode ? 'border-elegant-bg' : 'border-[#fbfaf6]'
                      } ${
                        chat.user.status === 'online' ? 'bg-emerald-500' :
                        chat.user.status === 'idle' ? 'bg-amber-400' :
                        chat.user.status === 'dnd' ? 'bg-rose-500' : 'bg-slate-400'
                      }`}
                    />
                  </div>

                  {/* Mid Text Section */}
                  <div className="flex-1 min-w-0">
                    <div className="flex justify-between items-baseline mb-0.5">
                      <div className="flex items-center gap-1.5">
                        <span className={`text-[13px] font-semibold truncate ${isDarkMode ? 'text-slate-100' : 'text-[#3c3730]'}`}>
                          {chat.user.name}
                        </span>
                        {/* Chat Type descriptor Badge */}
                        <span className="shrink-0 flex items-center justify-center bg-slate-800/10 dark:bg-elegant-bg/40 p-0.5 rounded-md">
                          {getCategoryIcon(chat.category)}
                        </span>
                        {chat.category === 'secret' && (
                          <span className={`text-[9px] font-mono tracking-wider font-semibold uppercase px-1 rounded ${
                            isDarkMode ? 'bg-elegant-gold/10 text-elegant-gold' : 'bg-[#dfd5c6] text-elegant-gold-dark'
                          }`}>
                            E2E
                          </span>
                        )}
                      </div>
                      <span className={`text-[10px] font-mono shrink-0 ${isDarkMode ? 'text-slate-500' : 'text-slate-400'}`}>
                        {chat.timestamp}
                      </span>
                    </div>

                    <div className="flex items-center justify-between gap-2">
                      <p className={`text-[11.5px] truncate flex-1 ${
                        chat.unreadCount > 0 
                          ? (isDarkMode ? 'font-medium text-slate-200' : 'font-semibold text-slate-800') 
                          : 'text-slate-400'
                      }`}>
                        {/* Sent status check matching WhatsApp */}
                        {latestMessageObj && latestMessageObj.senderId === 'me' && (
                          <span className="inline-block mr-1 align-middle">
                            {getStatusIcon(latestMessageObj.status)}
                          </span>
                        )}
                        {chat.lastMessage}
                      </p>

                      {/* Unread Message Count Bubble */}
                      {chat.unreadCount > 0 && (
                        <span className={`shrink-0 text-[10px] font-extrabold w-5 h-5 rounded-full flex items-center justify-center font-mono ${
                          isDarkMode ? 'bg-elegant-gold text-elegant-bg' : 'bg-elegant-gold-dark text-white'
                        }`}>
                          {chat.unreadCount}
                        </span>
                      )}
                    </div>
                  </div>
                </motion.div>
              );
            })
          ) : (
            <div className="flex flex-col items-center justify-center py-20 text-center px-4">
              <MessageSquare className="w-10 h-10 text-slate-600 mb-3 stroke-1" />
              <p className={`text-sm ${isDarkMode ? 'text-slate-400' : 'text-slate-600'}`}>No active conversations match filter.</p>
              <p className="text-xs text-slate-500 mt-1">Try toggling filter types or start a new broadcast channel.</p>
            </div>
          )}
        </AnimatePresence>
      </div>

      {/* WhatsApp style Floating Action Button for prompt interactions */}
      <div className="absolute bottom-4 right-4 z-20">
        <motion.button
          id="btn_fab_new_dialog"
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
          onClick={onOpenNewChatModal}
          className="bg-gradient-to-tr from-elegant-gold-dark to-elegant-gold text-elegant-bg rounded-2xl p-4 shadow-xl flex items-center justify-center cursor-pointer shadow-elegant-gold/20 hover:brightness-110 border border-white/10"
        >
          <Plus className="w-6 h-6 stroke-[3]" />
        </motion.button>
      </div>
    </div>
  );
}
