import { analytics } from '../config/firebase';
import { logEvent as fbLogEvent, setUserProperties as fbSetUserProperties, setUserId as fbSetUserId } from 'firebase/analytics';

export class AnalyticsService {
  /**
   * Log custom event to Firebase Analytics
   */
  logEvent(eventName: string, eventParams?: Record<string, any>) {
    if (!analytics) {
      console.log(`[Mock Analytics Event] Name: ${eventName}, Params:`, eventParams);
      return;
    }
    fbLogEvent(analytics, eventName, eventParams);
  }

  /**
   * Identify user by ID
   */
  setUserId(userId: string | null) {
    if (!analytics) {
      console.log(`[Mock Analytics User ID] ID: ${userId}`);
      return;
    }
    fbSetUserId(analytics, userId);
  }

  /**
   * Set custom user properties
   */
  setUserProperties(properties: Record<string, any>) {
    if (!analytics) {
      console.log('[Mock Analytics Properties] Properties:', properties);
      return;
    }
    fbSetUserProperties(analytics, properties);
  }

  /**
   * Track page views manually
   */
  trackPageView(pageName: string) {
    this.logEvent('page_view', { page_path: pageName });
  }
}

export const analyticsService = new AnalyticsService();
