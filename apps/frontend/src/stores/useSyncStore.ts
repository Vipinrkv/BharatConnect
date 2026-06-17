import { create } from 'zustand';

interface SyncItem {
  id: string;      // local_id UUID
  type: 'message_send' | 'help_request' | 'marketplace_bid';
  payload: any;
  createdAt: string;
}

interface SyncState {
  isOnline: boolean;
  syncQueue: SyncItem[];
  isSyncing: boolean;

  // Actions
  setOnlineStatus: (status: boolean) => void;
  addToQueue: (item: SyncItem) => void;
  removeFromQueue: (id: string) => void;
  setSyncing: (syncing: boolean) => void;
  clearQueue: () => void;
}

export const useSyncStore = create<SyncState>((set) => ({
  isOnline: navigator.onLine,
  syncQueue: [],
  isSyncing: false,

  setOnlineStatus: (isOnline) => set({ isOnline }),
  addToQueue: (item) => set((state) => ({ syncQueue: [...state.syncQueue, item] })),
  removeFromQueue: (id) => set((state) => ({
    syncQueue: state.syncQueue.filter((item) => item.id !== id)
  })),
  setSyncing: (isSyncing) => set({ isSyncing }),
  clearQueue: () => set({ syncQueue: [] })
}));
