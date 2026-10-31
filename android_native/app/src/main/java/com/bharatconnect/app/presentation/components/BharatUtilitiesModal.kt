package com.bharatconnect.app.presentation.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bharatconnect.app.core.theme.ColorPrimary6367FF

data class HelplineItem(
    val title: String,
    val number: String,
    val subtitle: String,
    val icon: ImageVector,
    val badgeColor: Color
)

data class UtilityItem(
    val title: String,
    val subtitle: String,
    val webUrl: String,
    val icon: ImageVector,
    val badgeColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BharatUtilitiesModal(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val emergencyHelplines = listOf(
        HelplineItem("National Emergency SOS", "112", "Police, Fire & Medical all-in-one", Icons.Default.Emergency, Color(0xFFFF4B4B)),
        HelplineItem("Cyber Crime & Fraud", "1930", "UPI, banking & online fraud reporting", Icons.Default.Security, Color(0xFFFF9933)),
        HelplineItem("Women Safety Helpline", "1091", "24x7 Women in distress assistance", Icons.Default.Shield, Color(0xFFE91E63)),
        HelplineItem("Police Control Room", "100", "Direct police dispatch", Icons.Default.LocalPolice, Color(0xFF2196F3)),
        HelplineItem("Ambulance & Healthcare", "108", "Free medical emergency transport", Icons.Default.LocalHospital, Color(0xFF4CAF50)),
        HelplineItem("Indian Railways Help", "139", "PNR enquiry & passenger security", Icons.Default.Train, Color(0xFF9C27B0))
    )

    val publicUtilities = listOf(
        UtilityItem("IRCTC PNR Live Tracker", "Check live Indian Railways train status", "https://www.irctc.co.in", Icons.Default.DirectionsTransit, Color(0xFF00ACC1)),
        UtilityItem("DigiLocker India", "Access official Aadhaar, PAN & driving licence", "https://www.digilocker.gov.in", Icons.Default.FolderShared, Color(0xFF3F51B5)),
        UtilityItem("UIDAI Aadhaar Portal", "Aadhaar download & address update services", "https://myaadhaar.uidai.gov.in", Icons.Default.Fingerprint, Color(0xFFFF9800)),
        UtilityItem("India Post Tracker", "Speed post tracking & PIN code locator", "https://www.indiapost.gov.in", Icons.Default.LocalPostOffice, Color(0xFFD32F2F))
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF100D28),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF373160)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            // Header with Indian Tiranga Accent
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFF9933), Color(0xFFFFFFFF), Color(0xFF138808))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🇮🇳", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Bharat Citizen Services",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "1-Tap emergency helplines & official Indian portals",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Text(
                        text = "EMERGENCY HELPLINES (TOLL-FREE 24x7)",
                        color = Color(0xFFFF9933),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                items(emergencyHelplines) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF171336)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(0.8.dp, Color(0xFF2E275E)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                launchDialer(context, item.number)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(item.badgeColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.icon, contentDescription = null, tint = item.badgeColor, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(item.subtitle, color = Color.Gray, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { launchDialer(context, item.number) },
                                colors = ButtonDefaults.buttonColors(containerColor = item.badgeColor),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(item.number, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "INDIAN PUBLIC UTILITIES & SCHEMES",
                        color = Color(0xFF4EFEAA),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                items(publicUtilities) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141030)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(0.8.dp, Color(0xFF282255)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                launchWebUrl(context, item.webUrl)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(item.badgeColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.icon, contentDescription = null, tint = item.badgeColor, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(item.subtitle, color = Color.Gray, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { launchWebUrl(context, item.webUrl) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4EFEAA)),
                                border = BorderStroke(1.dp, Color(0xFF4EFEAA)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Open", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun launchDialer(context: Context, number: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun launchWebUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
