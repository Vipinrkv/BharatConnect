package com.bharatconnect.app.core.contacts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.bharatconnect.app.core.network.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class PhoneContact(
    val id: String,
    val name: String,
    val rawPhone: String,
    val normalizedPhone: String,
    val isRegistered: Boolean = false,
    val registeredUserId: String? = null,
    val avatarUrl: String? = null
)

@Serializable
private data class ProfilePhoneDto(
    val id: String,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)

object ContactsManager {

    /**
     * Reads all contacts from the Android device phonebook.
     * Preserves the authoritative contact name from the user's phonebook.
     */
    suspend fun getDeviceContacts(context: Context): List<PhoneContact> = withContext(Dispatchers.IO) {
        val contactsMap = mutableMapOf<String, PhoneContact>()
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

                    if (normalized.length >= 10 && !contactsMap.containsKey(normalized)) {
                        contactsMap[normalized] = PhoneContact(
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
     */
    suspend fun matchRegisteredContacts(deviceContacts: List<PhoneContact>): List<PhoneContact> = withContext(Dispatchers.IO) {
        if (deviceContacts.isEmpty()) return@withContext emptyList()

        try {
            val supabase = SupabaseClient.client
            val remoteProfiles = supabase.postgrest["profiles"]
                .select()
                .decodeList<ProfilePhoneDto>()

            val registeredPhoneMap = mutableMapOf<String, ProfilePhoneDto>()
            for (p in remoteProfiles) {
                p.phoneNumber?.let { num ->
                    val norm = normalizePhoneNumber(num)
                    if (norm.isNotEmpty()) {
                        registeredPhoneMap[norm] = p
                    }
                }
            }

            deviceContacts.map { contact ->
                val match = registeredPhoneMap[contact.normalizedPhone]
                if (match != null) {
                    contact.copy(
                        isRegistered = true,
                        registeredUserId = match.id,
                        avatarUrl = match.avatarUrl
                    )
                } else {
                    contact.copy(isRegistered = false)
                }
            }.sortedWith(
                compareByDescending<PhoneContact> { it.isRegistered }
                    .thenBy { it.name.lowercase() }
            )
        } catch (_: Exception) {
            deviceContacts.sortedBy { it.name.lowercase() }
        }
    }

    /**
     * Normalizes phone number to 10-digit Indian standard / international format.
     * Strips +91, leading 0, spaces, dashes, and special characters.
     */
    fun normalizePhoneNumber(phone: String): String {
        var clean = phone.replace("[^0-9+]".toRegex(), "")
        if (clean.startsWith("+91")) {
            clean = clean.substring(3)
        } else if (clean.startsWith("91") && clean.length == 12) {
            clean = clean.substring(2)
        } else if (clean.startsWith("0") && clean.length == 11) {
            clean = clean.substring(1)
        }
        return clean.replace("+", "").trim()
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
