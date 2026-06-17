import {
  signInWithPopup,
  GoogleAuthProvider as FirebaseGoogleProvider,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut as firebaseSignOut,
  User as FirebaseUser
} from 'firebase/auth';
import { auth } from '../config/firebase';

export interface AuthUser {
  uid: string;
  email: string | null;
  displayName: string | null;
  photoURL: string | null;
  phoneNumber: string | null;
}

export interface AuthProvider {
  signIn(...args: any[]): Promise<AuthUser>;
  signOut(): Promise<void>;
  getCurrentUser(): AuthUser | null;
}

/**
 * Google Sign In Implementation
 */
export class GoogleAuthProvider implements AuthProvider {
  async signIn(): Promise<AuthUser> {
    const provider = new FirebaseGoogleProvider();
    const result = await signInWithPopup(auth, provider);
    return this.mapUser(result.user);
  }

  async signOut(): Promise<void> {
    await firebaseSignOut(auth);
  }

  getCurrentUser(): AuthUser | null {
    const user = auth.currentUser;
    return user ? this.mapUser(user) : null;
  }

  private mapUser(user: FirebaseUser): AuthUser {
    return {
      uid: user.uid,
      email: user.email,
      displayName: user.displayName,
      photoURL: user.photoURL,
      phoneNumber: user.phoneNumber,
    };
  }
}

/**
 * Email/Password Authentication Implementation
 */
export class EmailAuthProvider implements AuthProvider {
  async signIn(email: string, password: string, isSignUp: boolean = false): Promise<AuthUser> {
    let credential;
    if (isSignUp) {
      credential = await createUserWithEmailAndPassword(auth, email, password);
    } else {
      credential = await signInWithEmailAndPassword(auth, email, password);
    }
    return this.mapUser(credential.user);
  }

  async signOut(): Promise<void> {
    await firebaseSignOut(auth);
  }

  getCurrentUser(): AuthUser | null {
    const user = auth.currentUser;
    return user ? this.mapUser(user) : null;
  }

  private mapUser(user: FirebaseUser): AuthUser {
    return {
      uid: user.uid,
      email: user.email,
      displayName: user.displayName,
      photoURL: user.photoURL,
      phoneNumber: user.phoneNumber,
    };
  }
}

/**
 * Reserved Phone OTP Authentication Provider (Stubbed)
 */
export class PhoneOtpProvider implements AuthProvider {
  async signIn(): Promise<AuthUser> {
    throw new Error('Phone OTP Authentication is reserved and not implemented yet.');
  }

  async signOut(): Promise<void> {
    await firebaseSignOut(auth);
  }

  getCurrentUser(): AuthUser | null {
    // Return null in stub to avoid breaks during state checks
    return null;
  }
}
