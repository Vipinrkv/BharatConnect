import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  ArrowLeft, Phone, Video, Send, Smile, Paperclip, MoreVertical, 
  Check, CheckCheck, ShieldAlert, Sparkles, Star, Mic, ShieldAlert as LockIcon
} from 'lucide-react';
import { Chat, Message } from '../types';

interface ChatWindowProps {
  chat: Chat;
  onBack: () => void;
  onSendMessage: (chatId: string, text: string) => void;
  isDarkMode: boolean;
}

export default function ChatWindowView({ chat, onBack, onSendMessage, isDarkMode }: ChatWindowProps) {
  const [inputText, setInputText] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [showAttachPanel, setShowAttachPanel] = useState(false);
  const [callActiveModal, setCallActiveModal] = useState<'audio' | 'video' | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Auto-scroll messages to bottom
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [chat.messages, isTyping]);

  const handleSend = () => {
    if (!inputText.trim()) return;
    const pendingText = inputText;
    setInputText('');
    onSendMessage(chat.id, pendingText);

    // Simulate smart responses from different actors!
    setIsTyping(true);
    setTimeout(() => {
      setIsTyping(false);
      let replyText = 'Copy that! Let me check the grid log and get back to you immediately.';
      
      if (chat.user.id === 'user_1') {
        replyText = `Understood. I have verified your dispatch badge. Head directly to Room 4B, Pinecrest Plaza. Water & medical kits are prepped.`;
      } else if (chat.user.id === 'user_2') {
        replyText = `Great effort. The Sector 2 truck is starting up. I’ll meet you near the western gate. Live status updated.`;
      } else if (chat.user.id === 'user_5') {
        replyText = `[AUTOMATED CONSOLE ADVISORY] This is a secure channel broadcast. Your request receipt has been logged. Thank you for reporting.`;
      } else if (chat.id.includes('secret')) {
        replyText = `🔐 Key verification signature match. Your secret chat message has been signed and is set to auto-destruct in 60s.`;
      }

      // Add actual reply to chat object
      const mockReply: Message = {
        id: `reply_${Date.now()}`,
        senderId: chat.user.id,
        text: replyText,
        timestamp: 'Just Now',
        status: 'read'
      };
      
      chat.messages.push(mockReply);
      chat.lastMessage = replyText;
      scrollToBottom();
    }, 2000);
  };

  const triggerCallMock = (type: 'audio' | 'video') => {
    setCallActiveModal(type);
    setTimeout(() => {
      setCallActiveModal(null);
    }, 3000); // Toggles off automatically
  };

  return (
    <div 
      id="chat_window_container" 
      className={`flex flex-col h-full w-full relative h-[680px] overflow-hidden ${
        isDarkMode ? 'bg-elegant-bg text-white' : 'bg-[#faf9f5] text-[#2c2824]'
      }`}
    >
      {/* Immersive Header */}
      <div className={`p-3.5 flex items-center justify-between border-b transition-colors z-10 ${
        isDarkMode ? 'border-elegant-border/10 bg-elegant-card/90 backdrop-blur-md' : 'border-[#dfd5c6]/30 bg-[#fbfaf6]/95 backdrop-blur-md shadow-sm'
      }`}>
        <div className="flex items-center gap-2">
          <button 
            id="btn_chat_close"
            onClick={onBack}
            className={`p-1.5 rounded-xl cursor-pointer transition-colors ${isDarkMode ? 'hover:bg-elegant-card' : 'hover:bg-[#e9e4d9]'}`}
          >
            <ArrowLeft className={`w-5 h-5 ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`} />
          </button>

          {/* User profile details */}
          <div className="flex items-center gap-2.5">
            <div className="relative">
              <div className={`w-10 h-10 rounded-full ${chat.user.avatarBg} flex items-center justify-center font-bold text-sm text-white relative shadow-sm`}>
                {chat.user.avatar}
              </div>
              <span className={`absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full border-2 ${
                isDarkMode ? 'border-elegant-bg' : 'border-[#faf9f5]'
              } ${
                chat.user.status === 'online' ? 'bg-emerald-500' :
                chat.user.status === 'idle' ? 'bg-amber-400' : 'bg-rose-500'
              }`} />
            </div>

            <div className="flex flex-col">
              <span className="text-[12.5px] font-semibold tracking-wide truncate max-w-[120px]">
                {chat.user.name}
              </span>
              <span className="text-[9.5px] text-slate-400 font-mono tracking-tight leading-none">
                {chat.user.customStatus || chat.user.username}
              </span>
            </div>
          </div>
        </div>

        {/* Action icons */}
        <div className="flex items-center gap-1.5">
          <button 
            id="btn_call_audio"
            onClick={() => triggerCallMock('audio')}
            className={`p-2 rounded-xl transition-colors cursor-pointer ${isDarkMode ? 'hover:bg-elegant-card' : 'hover:bg-[#e9e4d9]'}`}
            title="Audio Call"
          >
            <Phone className={`w-4 h-4 ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`} />
          </button>
          <button 
            id="btn_call_video"
            onClick={() => triggerCallMock('video')}
            className={`p-2 rounded-xl transition-colors cursor-pointer ${isDarkMode ? 'hover:bg-elegant-card' : 'hover:bg-[#e9e4d9]'}`}
            title="Video Broadcast"
          >
            <Video className={`w-4 h-4 ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`} />
          </button>
          <button 
            id="btn_chat_details_panel" 
            className={`p-2 rounded-xl cursor-pointer ${isDarkMode ? 'hover:bg-elegant-card' : 'hover:bg-[#e9e4d9]'}`}
          >
            <MoreVertical className="w-4 h-4 text-slate-400" />
          </button>
        </div>
      </div>

      {/* Verification / Security keys top flag */}
      {chat.category === 'secret' && (
        <div className={`border-b px-4 py-2 flex items-center gap-2 ${
          isDarkMode ? 'bg-elegant-gold/5 border-elegant-gold/25 text-elegant-gold' : 'bg-[#dfd5c6]/20 border-[#dfd5c6]/60 text-elegant-gold-dark'
        }`}>
          <LockIcon className="w-3.5 h-3.5 animate-pulse" />
          <span className="text-[10px] font-mono leading-normal">
            End-to-end sandbox session active. Conversation is cryptographically segmented and untraceable.
          </span>
        </div>
      )}

      {/* Chat Messages flow body */}
      <div 
        className="flex-1 overflow-y-auto p-4 space-y-3"
        style={{
          backgroundImage: isDarkMode 
            ? 'radial-gradient(rgba(197, 168, 128, 0.04) 1px, transparent 0)' 
            : 'radial-gradient(rgba(145, 121, 84, 0.04) 1px, transparent 0)',
          backgroundSize: '16px 16px'
        }}
      >
        <div className="flex justify-center my-2">
          <span className={`text-[9px] font-mono px-3 py-1 rounded-full uppercase tracking-wider border ${
            isDarkMode ? 'bg-elegant-card text-slate-400 border-elegant-border/10' : 'bg-[#dfd5c6]/20 text-[#5c5346] border-[#dfd5c6]/60'
          }`}>
            🚨 Secure Sandbox Thread Established
          </span>
        </div>

        {chat.messages.map((msg) => {
          const isMe = msg.senderId === 'me';
          return (
            <div 
              key={msg.id} 
              className={`flex flex-col max-w-[80%] ${isMe ? 'ml-auto items-end' : 'mr-auto items-start'}`}
            >
              {/* Message bubble */}
              <div className={`p-3 rounded-2xl text-[12.5px] leading-relaxed relative ${
                isMe 
                  ? 'bg-gradient-to-tr from-elegant-gold-dark to-elegant-gold text-elegant-bg rounded-br-none font-bold shadow-md shadow-elegant-gold/5' 
                  : (isDarkMode ? 'bg-elegant-card text-slate-150 rounded-bl-none border border-elegant-border/10' : 'bg-[#fbfaf6] text-[#3c3730] rounded-bl-none shadow-sm border border-[#dfd5c6]/40')
              }`}>
                <p className="whitespace-pre-line">{msg.text}</p>
                
                {/* Message footer timestamp + WhatsApp checks */}
                <div className={`flex items-center justify-end gap-1 mt-1 text-[8.5px] select-none font-mono tracking-tighter opacity-70 ${
                  isMe ? 'text-elegant-bg/85' : 'text-slate-400'
                }`}>
                  <span>{msg.timestamp}</span>
                  {isMe && (
                    <span>
                      {msg.status === 'read' ? (
                        <CheckCheck className="w-3 h-3 text-elegant-bg" />
                      ) : (
                        <Check className="w-3 h-3 text-elegant-bg/60" />
                      )}
                    </span>
                  )}
                </div>
              </div>
            </div>
          );
        })}

        {/* Live typing indicator matching Telegram */}
        {isTyping && (
          <div className="flex items-center gap-2 max-w-[80%] mr-auto">
            <div className={`p-3.5 rounded-2xl rounded-bl-none text-xs flex items-center gap-1.5 ${
              isDarkMode ? 'bg-elegant-card text-slate-400 border border-elegant-border/10' : 'bg-[#f5f2eb] text-slate-505 border border-[#dfd5c6]/60 shadow-sm'
            }`}>
              <div className="flex gap-1">
                <span className={`w-1.5 h-1.5 rounded-full animate-bounce delay-100 ${isDarkMode ? 'bg-elegant-gold' : 'bg-elegant-gold-dark'}`} />
                <span className={`w-1.5 h-1.5 rounded-full animate-bounce delay-200 ${isDarkMode ? 'bg-elegant-gold' : 'bg-elegant-gold-dark'}`} />
                <span className={`w-1.5 h-1.5 rounded-full animate-bounce delay-300 ${isDarkMode ? 'bg-emerald-500 bg-elegant-gold' : 'bg-elegant-gold-dark'}`} />
              </div>
              <span className="font-mono text-[10px] tracking-tight">{chat.user.name} is typing...</span>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input bar section */}
      <div className={`p-3 relative z-10 ${isDarkMode ? 'bg-elegant-bg' : 'bg-[#faf9f5]'}`}>
        {/* Attachment Options Drawer */}
        <AnimatePresence>
          {showAttachPanel && (
            <motion.div
              initial={{ opacity: 0, y: 15 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 15 }}
              className={`absolute bottom-full left-3 right-3 mb-2 p-3 rounded-2xl border flex justify-around shadow-2xl z-20 ${
                isDarkMode ? 'bg-elegant-card border-elegant-border text-white' : 'bg-[#fbfaf6] border-[#dfd5c6]/80 text-[#3c3730]'
              }`}
            >
              {[
                { label: 'Document', icon: '📄', bg: 'bg-blue-500/10 text-blue-400' },
                { label: 'Camera', icon: '📸', bg: 'bg-rose-500/10 text-rose-400' },
                { label: 'Vitals Log', icon: '📝', bg: 'bg-elegant-gold/15 text-elegant-gold-dark font-bold' },
                { label: 'Audio', icon: '🎵', bg: 'bg-amber-500/10 text-amber-400' },
                { label: 'Coordinates', icon: '📍', bg: 'bg-purple-500/10 text-purple-400' },
              ].map((item, idx) => (
                <button
                  key={idx}
                  onClick={() => {
                    setInputText((prev) => prev ? `${prev} [Attachment: ${item.label}] ` : `[Attachment: ${item.label}] `);
                    setShowAttachPanel(false);
                  }}
                  className="flex flex-col items-center gap-1 cursor-pointer group"
                >
                  <div className={`w-10 h-10 rounded-xl flex items-center justify-center font-bold text-lg transition-transform group-hover:scale-110 ${item.bg}`}>
                    {item.icon}
                  </div>
                  <span className="text-[10px] text-slate-400">{item.label}</span>
                </button>
              ))}
            </motion.div>
          )}
        </AnimatePresence>

        <div className="flex items-center gap-2">
          {/* Main Input Text Field */}
          <div className={`flex items-center flex-1 rounded-2xl border px-3 py-1.5 ${
            isDarkMode ? 'bg-elegant-card border-elegant-border/10' : 'bg-white border-[#dfd5c6] shadow-sm'
          }`}>
            <button
              id="btn_chat_attach"
              onClick={() => setShowAttachPanel(!showAttachPanel)}
              className="p-1.5 rounded-full hover:bg-slate-500/10 text-slate-400 cursor-pointer"
            >
              <Paperclip className="w-4.5 h-4.5 text-slate-400 transition-transform hover:rotate-45" />
            </button>

            <input
              id="input_chat_text"
              type="text"
              placeholder="Message..."
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleSend();
              }}
              className="flex-1 text-xs bg-transparent border-none outline-none px-3 py-1.5 focus:ring-0 text-slate-150"
            />

            <button
               id="btn_chat_emoji_picker"
               onClick={() => setInputText((prev) => `${prev} 🤝`)}
               className="p-1 px-2.5 rounded hover:bg-slate-500/15 text-slate-400 cursor-pointer"
            >
              <Smile className="w-4.5 h-4.5 text-slate-400 hover:text-amber-450" />
            </button>
          </div>

          {/* Action icon */}
          {inputText.trim() ? (
            <motion.button
              id="btn_chat_send" 
              onClick={handleSend}
              whileTap={{ scale: 0.9 }}
              className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-elegant-gold-dark to-elegant-gold text-elegant-bg flex items-center justify-center font-bold shadow-lg transition-transform cursor-pointer"
            >
              <Send className="w-4 h-4 stroke-[3]" />
            </motion.button>
          ) : (
            <motion.button
              id="btn_chat_voice"
              whileTap={{ scale: 0.9 }}
              onClick={() => setInputText('🎙️ [Recording audio message...]')}
              className={`w-10 h-10 rounded-2xl flex items-center justify-center cursor-pointer transition-colors ${
                isDarkMode ? 'bg-elegant-card text-elegant-gold border border-elegant-border/20' : 'bg-white text-elegant-gold-dark border-[#dfd5c6] shadow-sm'
              }`}
            >
              <Mic className="w-4 h-4" />
            </motion.button>
          )}
        </div>
      </div>

      {/* Trigger alert panel for calling simulation in preview */}
      <AnimatePresence>
        {callActiveModal && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            className={`absolute inset-x-0 bottom-16 mx-4 p-4 rounded-3xl border text-white shadow-2xl z-50 flex items-center justify-between ${
              isDarkMode ? 'bg-elegant-card border-elegant-gold text-white' : 'bg-white border-[#dfd5c6] text-[#3c3730]'
            }`}
          >
            <div className="flex items-center gap-3">
              <div className={`w-2.5 h-2.5 rounded-full animate-ping ${isDarkMode ? 'bg-elegant-gold' : 'bg-elegant-gold-dark'}`} />
              <div className="flex flex-col">
                <span className={`text-xs font-bold uppercase tracking-wider font-mono ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`}>
                  Incoming {callActiveModal === 'video' ? 'Video Feed' : 'Secure Call'}
                </span>
                <span className="text-[10px] text-slate-400 font-mono">Connecting with {chat.user.name}...</span>
              </div>
            </div>
            
            <button
              id="btn_cancel_mock_call"
              onClick={() => setCallActiveModal(null)}
              className="bg-rose-500 hover:bg-rose-600 text-[10px] font-mono font-bold px-3 py-1.5 rounded-lg text-white text-center cursor-pointer"
            >
              Disconnect
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
