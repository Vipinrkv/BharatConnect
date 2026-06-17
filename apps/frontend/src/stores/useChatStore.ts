import { create } from 'zustand';
import { Message, Chat } from '@bharatconnect/shared';

interface ChatState {
  chats: Chat[];
  activeChatId: string | null;
  messagesByChat: Record<string, Message[]>;
  typingUsers: Record<string, string[]>; // chatId -> list of user names typing
  
  // Actions
  setChats: (chats: Chat[]) => void;
  setActiveChatId: (chatId: string | null) => void;
  addMessage: (chatId: string, message: Message) => void;
  setMessages: (chatId: string, messages: Message[]) => void;
  setUserTyping: (chatId: string, username: string, isTyping: boolean) => void;
}

export const useChatStore = create<ChatState>((set) => ({
  chats: [],
  activeChatId: null,
  messagesByChat: {},
  typingUsers: {},

  setChats: (chats) => set({ chats }),
  setActiveChatId: (activeChatId) => set({ activeChatId }),
  
  addMessage: (chatId, message) => set((state) => {
    const existing = state.messagesByChat[chatId] || [];
    // Ensure uniqueness based on id
    if (existing.some((m) => m.id === message.id)) return state;
    
    return {
      messagesByChat: {
        ...state.messagesByChat,
        [chatId]: [...existing, message].sort(
          (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
        )
      }
    };
  }),

  setMessages: (chatId, messages) => set((state) => ({
    messagesByChat: {
      ...state.messagesByChat,
      [chatId]: messages
    }
  })),

  setUserTyping: (chatId, username, isTyping) => set((state) => {
    const currentTyping = state.typingUsers[chatId] || [];
    let updatedTyping: string[];
    if (isTyping) {
      updatedTyping = currentTyping.includes(username) ? currentTyping : [...currentTyping, username];
    } else {
      updatedTyping = currentTyping.filter((u) => u !== username);
    }
    return {
      typingUsers: {
        ...state.typingUsers,
        [chatId]: updatedTyping
      }
    };
  })
}));
