package com.bharatconnect.app.presentation.chat

import android.Manifest
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bharatconnect.app.core.contacts.ContactsManager
import com.bharatconnect.app.core.contacts.PhoneContact
import com.bharatconnect.app.core.theme.ColorPrimary6367FF
import com.bharatconnect.app.presentation.components.ContactItemSkeleton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectContactBottomSheet(
    phoneContacts: List<PhoneContact>,
    isLoadingContacts: Boolean,
    hasContactPermission: Boolean,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    onDismiss: () -> Unit,
    onContactSelected: (PhoneContact) -> Unit
) {
    val context = LocalContext.current
    var contactSearchQuery by remember { mutableStateOf("") }
    val queryDigits = remember(contactSearchQuery) { contactSearchQuery.filter { it.isDigit() } }

    val registeredPhonebookContacts = remember(phoneContacts, contactSearchQuery, queryDigits) {
        phoneContacts.filter {
            it.isRegistered && it.isPhonebookContact && (
                contactSearchQuery.isBlank() ||
                it.name.contains(contactSearchQuery, ignoreCase = true) ||
                it.rawPhone.contains(contactSearchQuery) ||
                (it.username != null && it.username.contains(contactSearchQuery, ignoreCase = true)) ||
                (queryDigits.isNotEmpty() && (it.normalizedPhone.contains(queryDigits) || it.rawPhone.filter { c -> c.isDigit() }.contains(queryDigits)))
            )
        }
    }

    val otherRegisteredContacts = remember(phoneContacts, contactSearchQuery, queryDigits) {
        phoneContacts.filter {
            it.isRegistered && !it.isPhonebookContact && (
                contactSearchQuery.isBlank() ||
                it.name.contains(contactSearchQuery, ignoreCase = true) ||
                it.rawPhone.contains(contactSearchQuery) ||
                (it.username != null && it.username.contains(contactSearchQuery, ignoreCase = true)) ||
                (queryDigits.isNotEmpty() && (it.normalizedPhone.contains(queryDigits) || it.rawPhone.filter { c -> c.isDigit() }.contains(queryDigits)))
            )
        }
    }

    val unregisteredContacts = remember(phoneContacts, contactSearchQuery, queryDigits) {
        phoneContacts.filter {
            !it.isRegistered && (
                contactSearchQuery.isBlank() ||
                it.name.contains(contactSearchQuery, ignoreCase = true) ||
                it.rawPhone.contains(contactSearchQuery) ||
                (it.username != null && it.username.contains(contactSearchQuery, ignoreCase = true)) ||
                (queryDigits.isNotEmpty() && (it.normalizedPhone.contains(queryDigits) || it.rawPhone.filter { c -> c.isDigit() }.contains(queryDigits)))
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!hasContactPermission) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    val totalRegisteredCount = registeredPhonebookContacts.size + otherRegisteredContacts.size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF120F2A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF332F63)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Select Contact", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        text = if (hasContactPermission) "$totalRegisteredCount registered members available" else "Grant contact permission to find friends",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                }
            }

            // Search Bar
            OutlinedTextField(
                value = contactSearchQuery,
                onValueChange = { contactSearchQuery = it },
                placeholder = { Text("Search name, @username, or phone number...", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF16142E),
                    unfocusedContainerColor = Color(0xFF16142E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = ColorPrimary6367FF,
                    unfocusedBorderColor = Color(0xFF2C2856)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            if (!hasContactPermission) {
                // Phonebook Sync Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1840)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = null, tint = ColorPrimary6367FF, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Sync Device Contacts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Find phonebook friends & invite via SMS", color = Color.LightGray, fontSize = 11.sp)
                            }
                        }
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Allow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (isLoadingContacts) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(6) {
                        ContactItemSkeleton()
                    }
                }
            } else if (phoneContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (!hasContactPermission) "Permission needed to load device contacts." else "No contacts found in device phonebook.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        if (!hasContactPermission) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // 1. Registered Phonebook Contacts Section (PINNED ON TOP)
                    if (registeredPhonebookContacts.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CONTACTS ON BHARATCONNECT (${registeredPhonebookContacts.size})",
                                    color = Color(0xFF4EFEAA),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF142E1F),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Phonebook",
                                        color = Color(0xFF4EFEAA),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        items(registeredPhonebookContacts) { contact ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1640)),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFF322A6B)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onContactSelected(contact) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(ColorPrimary6367FF),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = contact.name.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = contact.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF4EFEAA))
                                            )
                                        }
                                        Text(contact.rawPhone, color = Color.LightGray, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { onContactSelected(contact) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary6367FF),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.ChatBubble, contentDescription = "Chat", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Other Registered BharatConnect Members
                    if (otherRegisteredContacts.isNotEmpty()) {
                        item {
                            Text(
                                text = "OTHER BHARATCONNECT MEMBERS (${otherRegisteredContacts.size})",
                                color = Color(0xFF818CF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(otherRegisteredContacts) { contact ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF16142E)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onContactSelected(contact) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2C2856)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = contact.name.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = contact.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                            contact.username?.let { u ->
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("@$u", color = Color.Gray, fontSize = 12.sp)
                                            }
                                        }
                                        Text(contact.rawPhone, color = Color.Gray, fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { onContactSelected(contact) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF818CF8)),
                                        border = BorderStroke(1.dp, Color(0xFF818CF8)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.ChatBubble, contentDescription = "Chat", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Unregistered Contacts Section (Invite via Native SMS)
                    if (unregisteredContacts.isNotEmpty()) {
                        item {
                            Text(
                                text = "INVITE TO BHARATCONNECT (${unregisteredContacts.size})",
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                            )
                        }
                        items(unregisteredContacts) { contact ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF14112E)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF25214E)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = contact.name.take(1),
                                            color = Color.LightGray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = contact.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(contact.rawPhone, color = Color.Gray, fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            ContactsManager.sendSmsInvite(context, contact.rawPhone)
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4EFEAA)),
                                        border = BorderStroke(1.dp, Color(0xFF4EFEAA)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Sms, contentDescription = "Invite", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Invite", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
