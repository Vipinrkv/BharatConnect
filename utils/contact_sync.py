"""
BharatConnect Native Contact Sync Engine (utils/contact_sync.py)

Fetches device contacts via Pyjnius (Android API) or local device store,
normalizes phone numbers, and queries BharatConnect DB to find registered contacts.
"""

import re
import sys
from kivy.utils import platform
from database.db import db_engine


from utils.phone import normalize_phone


def normalize_phone_number(raw_phone):
    """Delegates to canonical E.164 phone normalization utility."""
    return normalize_phone(raw_phone)


class PhoneContactSyncEngine:
    """Native Contact Reader & Registered Member Discovery System."""

    @staticmethod
    def request_android_permissions():
        """Requests Android READ_CONTACTS permission dynamically on Android devices."""
        if platform == "android":
            try:
                from android.permissions import request_permissions, Permission
                request_permissions([Permission.READ_CONTACTS, Permission.WRITE_CONTACTS])
            except Exception as e:
                print(f"[ContactSync] Android permission request error: {e}")

    @staticmethod
    def fetch_device_contacts():
        """
        Reads native device contacts from Android ContactsContract API,
        or returns local sample contacts for Desktop/Emulator environments.
        """
        contacts_list = []

        if platform == "android":
            try:
                from jnius import autoclass
                PythonActivity = autoclass("org.kivy.android.PythonActivity")
                ContactsContract = autoclass("android.provider.ContactsContract")

                activity = PythonActivity.mActivity
                resolver = activity.getContentResolver()
                uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI

                projection = [
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                ]

                cursor = resolver.query(uri, projection, None, None, None)
                if cursor is not None:
                    name_idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    num_idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    while cursor.moveToNext():
                        name = cursor.getString(name_idx) or "Unknown Contact"
                        raw_num = cursor.getString(num_idx) or ""
                        norm_num = normalize_phone_number(raw_num)
                        if norm_num:
                            contacts_list.append({"name": name, "phone": norm_num, "raw_phone": raw_num})
                    cursor.close()
            except Exception as e:
                print(f"[ContactSync] Pyjnius Android contact fetch error: {e}")

        if not contacts_list:
            # Local fallback contacts list
            contacts_list = [
                {"name": "Priya Sharma", "phone": "9876543210", "raw_phone": "+91 98765 43210"},
                {"name": "Rohan Verma", "phone": "9876543211", "raw_phone": "+91 98765 43211"},
                {"name": "Ananya Patel", "phone": "9876543212", "raw_phone": "+91 98765 43212"},
                {"name": "Vikram Singh", "phone": "9876543213", "raw_phone": "+91 98765 43213"},
                {"name": "Kavita Rao", "phone": "9876543214", "raw_phone": "+91 98765 43214"},
            ]

        return contacts_list

    @classmethod
    def get_contact_name_for_phone(cls, raw_phone, fallback=None):
        """
        Looks up a phone number in the device contacts and returns the contact's name.
        If no contact matches, returns the fallback name or normalized phone number.
        """
        if not raw_phone:
            return fallback or "Unknown Contact"
        norm = normalize_phone_number(raw_phone)
        contacts = cls.fetch_device_contacts()
        for c in contacts:
            if c.get("phone") == norm:
                return c["name"]
        return fallback or raw_phone

    @classmethod
    def send_sms_invite(cls, raw_phone, contact_name=""):
        """
        Triggers native Android SMS intent or system URI launcher to send an invitation via SMS.
        """
        clean_phone = raw_phone.strip() if raw_phone else ""
        invite_body = "Hey! Join me on BharatConnect - the fast, secure Indian social & chat app. Download now!"
        
        if platform == "android":
            try:
                from jnius import autoclass
                PythonActivity = autoclass("org.kivy.android.PythonActivity")
                Intent = autoclass("android.content.Intent")
                Uri = autoclass("android.net.Uri")

                intent = Intent(Intent.ACTION_VIEW)
                intent.setData(Uri.parse(f"sms:{clean_phone}"))
                intent.putExtra("sms_body", invite_body)
                PythonActivity.mActivity.startActivity(intent)
                return True, f"Opening SMS app to invite {contact_name or clean_phone}..."
            except Exception as e:
                print(f"[ContactSync] Android SMS intent error: {e}")

        # Desktop / Web Fallback
        try:
            import webbrowser
            import urllib.parse
            encoded_text = urllib.parse.quote(invite_body)
            webbrowser.open(f"sms:{clean_phone}?body={encoded_text}")
        except Exception:
            pass
        return True, f"Invitation prepared for {contact_name or clean_phone}!"

    @classmethod
    def sync_and_match_contacts(cls):
        """
        Fetches phone contacts, matches against BharatConnect database,
        and returns tuple of (registered_contacts, invite_contacts).
        """
        cls.request_android_permissions()
        phone_contacts = cls.fetch_device_contacts()

        phone_map = {c["phone"]: c for c in phone_contacts if c.get("phone")}

        # Query database for registered users matching these phone numbers
        registered_users = db_engine.match_registered_phone_contacts(list(phone_map.keys()))

        registered_matches = []
        matched_phones = set()

        for user in registered_users:
            p = normalize_phone_number(user.get("phone", ""))
            matched_phones.add(p)
            contact_info = phone_map.get(p, {})
            registered_matches.append({
                "id": user.get("id"),
                "display_name": contact_info.get("name") or user.get("display_name") or user.get("username"),
                "username": user.get("username"),
                "phone": user.get("phone"),
                "avatar_initials": user.get("avatar_initials", "BC"),
                "avatar_color": user.get("avatar_color", "#6367FF"),
                "is_registered": True,
            })

        invite_matches = []
        for p, c in phone_map.items():
            if p not in matched_phones:
                invite_matches.append({
                    "name": c["name"],
                    "phone": c["raw_phone"],
                    "is_registered": False,
                })

        return registered_matches, invite_matches

