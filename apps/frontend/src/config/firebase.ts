import { initializeApp, getApps, getApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getMessaging, isSupported as isMessagingSupported } from 'firebase/messaging';
import { getAnalytics, isSupported as isAnalyticsSupported, Analytics } from 'firebase/analytics';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID,
};

// Initialize Firebase App (Singleton check)
const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApp();

// Initialize Firebase Auth
const auth = getAuth(app);

// Initialize Firebase Messaging (safe-guard for SSR/Private Browsing environments)
let messaging: any = null;
isMessagingSupported().then((supported) => {
  if (supported) {
    messaging = getMessaging(app);
  }
});

// Initialize Firebase Analytics (safe-guard for browser blockages / private sessions)
let analytics: Analytics | null = null;
isAnalyticsSupported().then((supported) => {
  if (supported) {
    analytics = getAnalytics(app);
  }
});

export { app, auth, messaging, analytics };
