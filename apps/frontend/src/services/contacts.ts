import { useUserStore } from '../stores/useUserStore';
import { calculateSHA256 } from '../lib/mediaPipeline';

export interface LocalContact {
  name: string;
  phone: string;
}

export class ContactsDiscoveryService {
  /**
   * Request native permission to read device contacts list.
   * Leverages Capacitor features with fallback mock listings for browsers.
   */
  async requestPermission(): Promise<boolean> {
    try {
      // Mock alert prompt representing Capacitor Contacts.requestPermissions()
      const confirmed = window.confirm(
        'BharatConnect wants to access your contacts to match friends. Your phone numbers will be hashed before being sent to the server.'
      );
      return confirmed;
    } catch (e) {
      console.error('Error requesting contacts permission:', e);
      return false;
    }
  }

  /**
   * Retrieve device contacts and filter/format to E.164 phone formats
   */
  async getDeviceContacts(): Promise<LocalContact[]> {
    // In production:
    // import { Contacts } from '@capacitor-community/contacts';
    // const result = await Contacts.getContacts();
    // return result.contacts.map(c => ({ name: c.displayName, phone: c.phoneNumbers[0]?.number }));
    
    // Web Mock Fallback:
    return [
      { name: 'Arjun Sharma', phone: '+919876543210' },
      { name: 'Volunteer Ritu', phone: '+919999999999' },
      { name: 'Unknown Contact', phone: '+910000000000' },
    ];
  }

  /**
   * Clean, hash, and upload contacts to identify matches in the system
   */
  async discoverFriends(): Promise<any[]> {
    const hasPermission = await this.requestPermission();
    if (!hasPermission) return [];

    try {
      const contacts = await this.getDeviceContacts();
      const hashesMap: Record<string, LocalContact> = {};
      const hashesList: string[] = [];

      for (const contact of contacts) {
        if (!contact.phone) continue;
        
        // Normalize: remove '+', spaces, dashes, parentheses
        const cleanPhone = contact.phone.replace(/[+\s()-]/g, '');
        
        // Hash using SHA-256 helper
        const encoder = new TextEncoder();
        const data = encoder.encode(cleanPhone);
        const hashHex = await calculateSHA256(data.buffer);
        
        hashesList.push(hashHex);
        hashesMap[hashHex] = contact;
      }

      // Upload hashes list to backend router endpoint
      const response = await fetch(`${import.meta.env.VITE_API_URL || '/api/v1'}/auth/contacts/discover`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('auth_token') || ''}`,
        },
        body: JSON.stringify({ phone_hashes: hashesList }),
      });

      if (!response.ok) throw new Error('Failed to fetch matched contacts from backend service.');

      const matches = await response.json();
      console.log('Matched registered friends found:', matches);

      // Cache matching suggestions in Zustand store
      // useUserStore.getState().setNearbyUsers(matches);

      return matches;
    } catch (err) {
      console.error('Contact discovery failed:', err);
      return [];
    }
  }
}

export const contactsDiscoveryService = new ContactsDiscoveryService();
