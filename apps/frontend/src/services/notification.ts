import { messaging } from '../config/firebase';
import { getToken, onMessage } from 'firebase/messaging';

export class NotificationService {
  private vapidKey = import.meta.env.VITE_FCM_VAPID_KEY || '';

  /**
   * Request device permission and fetch registration token for push notifications
   */
  async requestToken(): Promise<string | null> {
    if (!messaging) {
      console.warn('Firebase Messaging is not supported or initialized in this environment.');
      return null;
    }

    try {
      const permission = await Notification.requestPermission();
      if (permission === 'granted') {
        const token = await getToken(messaging, {
          vapidKey: this.vapidKey,
        });
        if (token) {
          return token;
        }
        console.warn('No FCM registration token received.');
      } else {
        console.warn('Notification permission denied by user.');
      }
    } catch (error) {
      console.error('Error fetching FCM token:', error);
    }
    return null;
  }

  /**
   * Register foreground listener to receive payloads while client is active
   */
  onForegroundMessage(callback: (payload: any) => void) {
    if (!messaging) return;
    return onMessage(messaging, (payload) => {
      console.log('FCM Foreground payload received:', payload);
      callback(payload);
    });
  }
}

export const notificationService = new NotificationService();
