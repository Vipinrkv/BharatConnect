package com.bharatconnect.app.core.contacts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.bharatconnect.app.core.network.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.bharatconnect.app.data.remote.dto.ProfileDto
import io.github.jan.supabase.gotrue.auth

data class PhoneContact(
    val id: String,
    val name: String,
    val rawPhone: String,
    val normalizedPhone: String,
    val isRegistered: Boolean = false,
    val registeredUserId: String? = null,
    val avatarUrl: String? = null,
    val username: String? = null
)

object ContactsManager {

    /**
     * Reads all contacts from the Android device phonebook.
     * Preserves the authoritative contact name from the user's phonebook.
     */
    suspend fun getDeviceContacts(context: Context): List<PhoneContact> = withContext(Dispatchers.IO) {
        val contactsMap = LinkedHashMap<String, PhoneContact>()
        val contentResolver = context.contentResolver

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID
        )

        try {
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val id = if (idIndex >= 0) it.getString(idIndex) ?: "" else ""
                    val name = if (nameIndex >= 0) it.getString(nameIndex) ?: "Contact" else "Contact"
                    val rawNumber = if (numberIndex >= 0) it.getString(numberIndex) ?: "" else ""
                    val normalized = normalizePhoneNumber(rawNumber)

                    val key = if (normalized.isNotBlank()) normalized else rawNumber.trim()
                    if (key.isNotBlank() && !contactsMap.containsKey(key)) {
                        contactsMap[key] = PhoneContact(
                            id = id,
                            name = name,
                            rawPhone = rawNumber,
                            normalizedPhone = normalized,
                            isRegistered = false
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        contactsMap.values.toList()
    }

    /**
     * Cross-references device contacts with Supabase registered users.
     * Registered users are marked with isRegistered = true, registeredUserId, and avatarUrl.
     * Always sorts registered users to the top with direct chat capability.
     */
    suspend fun matchRegisteredContacts(deviceContacts: List<PhoneContact>): List<PhoneContact> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClient.client
            val currentUserId = supabase.auth.currentUserOrNull()?.id

            val remoteProfiles = try {
                supabase.postgrest["profiles"]
                    .select()
                    .decodeList<ProfileDto>()
            } catch (e: Exception) {
                android.util.Log.e("ContactsManager", "Failed to decode remote profiles", e)
                emptyList()
            }

            val registeredPhoneMap = mutableMapOf<String, ProfileDto>()
            val registeredIdMap = mutableSetOf<String>()
            for (p in remoteProfiles) {
                if (p.id == currentUserId) continue // Skip self
                p.phoneNumber?.let { num ->
                    val norm = normalizePhoneNumber(num)
                    val fullDigits = num.filter { it.isDigit() }
                    if (norm.length >= 10) {
                        registeredPhoneMap[norm] = p
                    }
                    if (fullDigits.isNotEmpty()) {
                        registeredPhoneMap[fullDigits] = p
                    }
                }
            }

            val matchedDevicePhones = mutableSetOf<String>()
            val updatedContacts = deviceContacts.map { contact ->
                val norm = normalizePhoneNumber(contact.rawPhone)
                val fullDigits = contact.rawPhone.filter { it.isDigit() }
                val match = registeredPhoneMap[norm] ?: registeredPhoneMap[fullDigits]
                if (match != null) {
                    matchedDevicePhones.add(norm)
                    if (fullDigits.isNotEmpty()) matchedDevicePhones.add(fullDigits)
                    registeredIdMap.add(match.id)
                    contact.copy(
                        isRegistered = true,
                        registeredUserId = match.id,
                        avatarUrl = match.avatarUrl,
                        username = match.username
                    )
                } else {
                    contact.copy(isRegistered = false)
                }
            }.toMutableList()

            // Also include ALL registered BharatConnect members not in device phonebook
            for (p in remoteProfiles) {
                if (p.id == currentUserId) continue
                if (!registeredIdMap.contains(p.id)) {
                    val displayName = p.fullName?.takeIf { it.isNotBlank() }
                        ?: p.username?.takeIf { it.isNotBlank() }
                        ?: "BharatConnect Member"
                    val displaySubtitle = if (!p.username.isNullOrBlank()) {
                        "@${p.username}"
                    } else if (!p.phoneNumber.isNullOrBlank()) {
                        p.phoneNumber
                    } else {
                        "Registered Member"
                    }
                    val norm = p.phoneNumber?.let { normalizePhoneNumber(it) }.orEmpty()
                    updatedContacts.add(
                        PhoneContact(
                            id = p.id,
                            name = displayName,
                            rawPhone = displaySubtitle,
                            normalizedPhone = norm,
                            isRegistered = true,
                            registeredUserId = p.id,
                            avatarUrl = p.avatarUrl,
                            username = p.username
                        )
                    )
                    registeredIdMap.add(p.id)
                }
            }

            updatedContacts.sortedWith(
                compareByDescending<PhoneContact> { it.isRegistered }
                    .thenBy { it.name.lowercase() }
            )
        } catch (e: Exception) {
            android.util.Log.e("ContactsManager", "Error in matchRegisteredContacts", e)
            deviceContacts.sortedBy { it.name.lowercase() }
        }
    }

    /**
     * Normalizes phone number to 10-digit Indian standard / international format.
     * Takes the last 10 digits to cleanly bridge +91, 0, and un-prefixed numbers.
     */
    fun normalizePhoneNumber(phone: String): String {
        val digitsOnly = phone.filter { it.isDigit() }
        return if (digitsOnly.length >= 10) {
            digitsOnly.takeLast(10)
        } else {
            digitsOnly
        }
    }

    /**
     * Launches the native Android default SMS app with pre-filled BharatConnect invitation.
     */
    fun sendSmsInvite(context: Context, phoneNumber: String) {
        try {
            val inviteMessage = "Hey! Let's connect on BharatConnect, India's own secure social & messaging app. Download it now: https://bharatconnect.app"
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", inviteMessage)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback general share if no SMS handler exists
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("sms:$phoneNumber?body=Hey! Let's connect on BharatConnect: https://bharatconnect.app")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }
}
