import { create } from 'zustand';
import { UserProfile } from '@bharatconnect/shared';

interface UserState {
  profile: UserProfile | null;
  isAuthenticated: boolean;
  currentLocation: { latitude: number; longitude: number } | null;
  nearbyUsers: UserProfile[];

  // Actions
  setProfile: (profile: UserProfile | null) => void;
  setAuthenticated: (auth: boolean) => void;
  updateLocation: (lat: number, lng: number) => void;
  setNearbyUsers: (users: UserProfile[]) => void;
}

export const useUserStore = create<UserState>((set) => ({
  profile: null,
  isAuthenticated: false,
  currentLocation: null,
  nearbyUsers: [],

  setProfile: (profile) => set({ profile, isAuthenticated: !!profile }),
  setAuthenticated: (isAuthenticated) => set({ isAuthenticated }),
  updateLocation: (latitude, longitude) => set({ currentLocation: { latitude, longitude } }),
  setNearbyUsers: (nearbyUsers) => set({ nearbyUsers }),
}));
