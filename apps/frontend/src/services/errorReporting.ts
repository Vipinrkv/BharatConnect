import { Capacitor } from '@capacitor/core';

export class ErrorReportingService {
  private isNative = Capacitor.isNativePlatform();

  /**
   * Log non-fatal error boundary exceptions (equivalent to Crashlytics logException)
   */
  logError(error: Error, customMetadata?: Record<string, any>) {
    if (this.isNative) {
      // Production Android build integration point:
      // Calls Capacitor Community Firebase Crashlytics or similar bridge
      // Example:
      // import { FirebaseCrashlytics } from '@capacitor-community/firebase-crashlytics';
      // FirebaseCrashlytics.recordException({ message: error.message, stacktrace: error.stack || '' });
      console.log('[Native Crashlytics Error Logged]', error, customMetadata);
    } else {
      console.error('[Web Crashlytics Mock] Error:', error.message, '\nMetadata:', customMetadata, '\nStack:', error.stack);
    }
  }

  /**
   * Log custom breadcrumb details to help trace context before a potential crash
   */
  logBreadcrumb(message: string) {
    if (this.isNative) {
      // Example: FirebaseCrashlytics.log({ message });
      console.log(`[Native Crashlytics Breadcrumb]: ${message}`);
    } else {
      console.log(`[Web Breadcrumb]: ${message}`);
    }
  }

  /**
   * Set identifiers to pair errors with specific user accounts
   */
  setUserId(userId: string) {
    if (this.isNative) {
      // Example: FirebaseCrashlytics.setUserId({ userId });
      console.log(`[Native Crashlytics User ID]: ${userId}`);
    } else {
      console.log(`[Web User ID Set]: ${userId}`);
    }
  }
}

export const errorReportingService = new ErrorReportingService();
