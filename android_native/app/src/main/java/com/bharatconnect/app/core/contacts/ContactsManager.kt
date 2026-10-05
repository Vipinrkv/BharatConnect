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

/**
 * Data representation of a phonebook or platform contact.
 *
 * @param id Unique contact identifier (from Android ContactsContract or Supabase UUID).
 * @param name Display name from device phonebook or profile full name.
 * @param rawPhone Raw phone number string as typed by user or stored in contacts.
 * @param normalizedPhone Standardized 10-digit Indian phone number without country prefixes.
 * @param isRegistered True if this phone number or account is registered on BharatConnect.
 * @param registeredUserId Remote Supabase user UUID if registered.
 * @param avatarUrl Remote profile picture URL if available.
 * @param username Public unique handle (e.g. "@rahul").
 * @param isPhonebookContact True if found in local device address book; false if external platform member.
 */
data class PhoneContact(
    val id: String,
    val name: String,
    val rawPhone: String,
    val normalizedPhone: String,
    val isRegistered: Boolean = false,
    val registeredUserId: String? = null,
    val avatarUrl: String? = null,
    val username: String? = null,
    val isPhonebookContact: Boolean = true
)

/**
 * =========================================================================================
 * CONTACTS MANAGER: PHONEBOOK SYNC, NORMALIZATION & REGISTRATION MATCHING
 * =========================================================================================
 *
 * Implements WhatsApp-style contact discovery, address book synchronization, and name resolution.
 *
 * Architectural Features:
 * 1. Offline-First Address Book: Reads Android ContentResolver and caches names in a concurrent map.
 * 2. 10-Digit Normalization: Strips country codes (+91, 0091) and leading zeroes so numbers like
 *    "+91 98765 43210", "09876543210", and "9876543210" map cleanly to the same user.
 * 3. 3-Tier Categorization:
 *    - Tier 1 (Pinned Top): Saved contacts from phonebook registered on BharatConnect (Chat button).
 *    - Tier 2 (Middle): Other registered BharatConnect members (Chat button).
 *    - Tier 3 (Bottom): Saved contacts not yet on BharatConnect (Native SMS Invite button).
 * 4. Authoritative Name Resolution: WhatsApp privacy standard where your phonebook contact name
 *    always takes precedence over the user's remote public profile name.
 */
object ContactsManager {

    // Fast in-memory cache mapping normalized phone numbers to device address book names
    private val phoneToNameMap = java.util.concurrent.ConcurrentHashMap<String, String>()

    /**
     * Reads all contacts from the Android device address book via ContactsContract.
     * Preserves the authoritative contact name saved locally by the user.
     *
     * @param context Android context for ContentResolver queries.
     * @return List of deduplicated phone contacts with normalized numbers.
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

                    if (normalized.isNotBlank()) {
                        phoneToNameMap[normalized] = name
                        val fullDigits = rawNumber.filter { char -> char.isDigit() }
                        if (fullDigits.isNotBlank()) {
                            phoneToNameMap[fullDigits] = name
                        }
                    }

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
     * Cross-references device contacts with Supabase & Room DB registered users.
     * Registered phonebook contacts are identified with isRegistered = true, isPhonebookContact = true,
     * and placed at the top of the contact drawer with direct 1-tap chat capability.
     */
    suspend fun matchRegisteredContacts(deviceContacts: List<PhoneContact>): List<PhoneContact> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClient.client
            val currentUserId = supabase.auth.currentUserOrNull()?.id
                ?: com.bharatconnect.app.core.session.SessionManager.getCachedUserProfile()?.id

            // 1. Fetch from local Room DB first for instant offline matching
            val localUsers = try {
                com.bharatconnect.app.core.database.DatabaseProvider.getDatabase().userDao().getAllUsers()
            } catch (_: Exception) {
                emptyList()
            }

            // 2. Fetch remote profiles from Supabase with timeout guard
            val remoteProfiles = try {
                kotlinx.coroutines.withTimeoutOrNull(4000L) {
                    supabase.postgrest["profiles"]
                        .select()
                        .decodeList<ProfileDto>()
                } ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("ContactsManager", "Failed to decode remote profiles", e)
                emptyList()
            }

            // 3. Cache fresh remote profiles into local Room DB for future offline usage
            if (remoteProfiles.isNotEmpty()) {
                try {
                    val entities = remoteProfiles.map {
                        com.bharatconnect.app.data.local.room.entity.UserEntity.fromDomain(it.toDomain())
                    }
                    com.bharatconnect.app.core.database.DatabaseProvider.getDatabase().userDao().insertUsers(entities)
                } catch (_: Exception) {}
            }

            // 4. Build registered index mapping phone, username, and email to profile
            data class MatchableProfile(
                val id: String,
                val username: String?,
                val fullName: String?,
                val phoneNumber: String?,
                val email: String?,
                val avatarUrl: String?
            )

            val profilePool = mutableMapOf<String, MatchableProfile>()

            // Add local users
            for (u in localUsers) {
                if (u.id == currentUserId) continue
                profilePool[u.id] = MatchableProfile(
                    id = u.id,
                    username = u.username,
                    fullName = u.fullName,
                    phoneNumber = u.phoneNumber,
                    email = u.email,
                    avatarUrl = u.avatarUrl
                )
            }

            // Overlay remote profiles (more up-to-date)
            for (p in remoteProfiles) {
                if (p.id == currentUserId) continue
                profilePool[p.id] = MatchableProfile(
                    id = p.id,
                    username = p.username,
                    fullName = p.fullName,
                    phoneNumber = p.phoneNumber,
                    email = p.email,
                    avatarUrl = p.avatarUrl
                )
            }

            val phoneIndex = mutableMapOf<String, MatchableProfile>()
            val usernameIndex = mutableMapOf<String, MatchableProfile>()
            val emailIndex = mutableMapOf<String, MatchableProfile>()

            for (p in profilePool.values) {
                p.phoneNumber?.let { num ->
                    val norm = normalizePhoneNumber(num)
                    val fullDigits = num.filter { it.isDigit() }
                    if (norm.length >= 10) phoneIndex[norm] = p
                    if (fullDigits.isNotEmpty()) phoneIndex[fullDigits] = p
                }
                p.username?.let { u ->
                    if (u.isNotBlank()) usernameIndex[u.trim().lowercase().removePrefix("@")] = p
                }
                p.email?.let { e ->
                    if (e.isNotBlank()) emailIndex[e.trim().lowercase()] = p
                }
            }

            val matchedRegisteredIds = mutableSetOf<String>()

            // 5. Match device phonebook contacts by phone or exact username
            val updatedDeviceContacts = deviceContacts.map { contact ->
                val norm = normalizePhoneNumber(contact.rawPhone)
                val fullDigits = contact.rawPhone.filter { it.isDigit() }
                val cleanName = contact.name.trim().lowercase().removePrefix("@")

                val matched = phoneIndex[norm]
                    ?: phoneIndex[fullDigits]
                    ?: usernameIndex[cleanName]

                if (matched != null) {
                    matchedRegisteredIds.add(matched.id)
                    contact.copy(
                        isRegistered = true,
                        registeredUserId = matched.id,
                        avatarUrl = matched.avatarUrl,
                        username = matched.username,
                        isPhonebookContact = true
                    )
                } else {
                    contact.copy(isRegistered = false, isPhonebookContact = true)
                }
            }.toMutableList()

            // 6. Include other registered BharatConnect members not present in phonebook
            for (p in profilePool.values) {
                if (!matchedRegisteredIds.contains(p.id)) {
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
                    updatedDeviceContacts.add(
                        PhoneContact(
                            id = p.id,
                            name = displayName,
                            rawPhone = displaySubtitle,
                            normalizedPhone = norm,
                            isRegistered = true,
                            registeredUserId = p.id,
                            avatarUrl = p.avatarUrl,
                            username = p.username,
                            isPhonebookContact = false
                        )
                    )
                    matchedRegisteredIds.add(p.id)
                }
            }

            // 6b. Always provide official verified BharatConnect Support channel
            val supportBotId = "bharatconnect_support_bot"
            if (!matchedRegisteredIds.contains(supportBotId) && currentUserId != supportBotId) {
                updatedDeviceContacts.add(
                    PhoneContact(
                        id = supportBotId,
                        name = "BharatConnect Support",
                        rawPhone = "Official Help & Community Desk",
                        normalizedPhone = "0000000000",
                        isRegistered = true,
                        registeredUserId = supportBotId,
                        avatarUrl = null,
                        username = "bharatconnect",
                        isPhonebookContact = false
                    )
                )
                matchedRegisteredIds.add(supportBotId)
                try {
                    val supportUser = com.bharatconnect.app.data.local.room.entity.UserEntity(
                        id = supportBotId,
                        username = "bharatconnect",
                        fullName = "BharatConnect Support",
                        avatarUrl = null,
                        bio = "Official BharatConnect support & community guide",
                        phoneNumber = "+91 00000 00000",
                        isOnline = true
                    )
                    com.bharatconnect.app.core.database.DatabaseProvider.getDatabase().userDao().insertOrUpdateUser(supportUser)
                } catch (_: Exception) {}
            }

            // 7. Sort:
            // 1st: Registered phonebook contacts (Saved contacts on BharatConnect)
            // 2nd: Other registered members on BharatConnect
            // 3rd: Unregistered phonebook contacts (for SMS invite)
            updatedDeviceContacts.sortedWith(
                compareByDescending<PhoneContact> { it.isRegistered && it.isPhonebookContact }
                    .thenByDescending { it.isRegistered }
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
     * Looks up whether a phone number exists in the device phonebook.
     * Returns the device contact name if saved, or null if unsaved.
     */
    fun getContactNameByPhone(context: Context, rawPhone: String?): String? {
        if (rawPhone.isNullOrBlank()) return null
        val norm = normalizePhoneNumber(rawPhone)
        if (norm.isNotBlank() && phoneToNameMap.containsKey(norm)) {
            return phoneToNameMap[norm]
        }
        val fullDigits = rawPhone.filter { it.isDigit() }
        if (fullDigits.isNotBlank() && phoneToNameMap.containsKey(fullDigits)) {
            return phoneToNameMap[fullDigits]
        }

        // Direct ContentResolver query if cache is cold
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(rawPhone))
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIdx >= 0) {
                        val name = cursor.getString(nameIdx)
                        if (!name.isNullOrBlank()) {
                            if (norm.isNotBlank()) phoneToNameMap[norm] = name
                            return@use name
                        }
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Formats a phone number for clean WhatsApp-style display (e.g. "+91 98765 43210").
     */
    fun formatDisplayPhoneNumber(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return if (digits.length >= 10) {
            val last10 = digits.takeLast(10)
            "+91 ${last10.substring(0, 5)} ${last10.substring(5)}"
        } else {
            phone.ifBlank { "BharatConnect Member" }
        }
    }

    /**
     * WhatsApp-style authoritative name resolution:
     * 1. If saved in device phonebook -> displays device phonebook name (e.g. "Rahul Work")
     * 2. If NOT saved in device phonebook -> displays phone number (e.g. "+91 98765 43210")
     * 3. Fallback -> full name, username, or fallback title
     */
    fun resolveCounterpartDisplayName(
        context: Context,
        phoneNumber: String?,
        fullName: String?,
        username: String?,
        fallbackTitle: String? = null
    ): String {
        if (!phoneNumber.isNullOrBlank()) {
            val savedName = getContactNameByPhone(context, phoneNumber)
            if (!savedName.isNullOrBlank()) {
                return savedName
            }
            // Contact is NOT saved in phonebook -> display phone number, just like WhatsApp!
            return formatDisplayPhoneNumber(phoneNumber)
        }

        // If no phone number is present on profile, fallback to profile full name or username
        return fullName?.takeIf { it.isNotBlank() }
            ?: username?.takeIf { it.isNotBlank() }
            ?: fallbackTitle?.takeIf { it.isNotBlank() }
            ?: "BharatConnect Member"
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
