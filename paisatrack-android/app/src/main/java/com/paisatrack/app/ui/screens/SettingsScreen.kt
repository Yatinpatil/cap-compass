package com.paisatrack.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paisatrack.app.BuildConfig
import com.paisatrack.app.domain.model.FinancePreferences
import com.paisatrack.app.ui.components.InfoBanner
import com.paisatrack.app.ui.components.PaisaCard
import com.paisatrack.app.ui.components.ScreenHorizontalPadding
import com.paisatrack.app.ui.components.SectionTitle
import com.paisatrack.app.ui.components.SoftIcon
import com.paisatrack.app.ui.theme.Border
import com.paisatrack.app.ui.theme.Coral
import com.paisatrack.app.ui.theme.Forest
import com.paisatrack.app.ui.theme.SecondaryText
import com.paisatrack.app.ui.theme.SoftCoral
import com.paisatrack.app.ui.theme.Teal

@Composable
fun SettingsScreen(
    preferences: FinancePreferences,
    onPermissionStatus: () -> Unit,
    onEnableSms: () -> Unit,
    onDisableSms: () -> Unit,
    onRequestNotifications: () -> Unit,
    onPolicy: () -> Unit,
    onErrorState: () -> Unit,
    onExport: ((String) -> Unit) -> Unit,
    onDeleteAllData: () -> Unit,
) {
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var csvToExport by remember { mutableStateOf<String?>(null) }
    var exportMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val csv = csvToExport
        if (uri != null && csv != null) {
            runCatching {
                val stream = context.contentResolver.openOutputStream(uri) ?: error("Cannot write to the selected location.")
                stream.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
            }
                .onSuccess { exportMessage = "CSV exported to the location you selected." }
                .onFailure { exportMessage = "Could not write the CSV file." }
        } else if (uri == null) exportMessage = "Export cancelled."
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete all PaisaTrack data?") },
            text = { Text("This permanently removes saved transactions, bills, preferences and import checkpoints from this device. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirmation = false; onDeleteAllData() }) {
                    Text("Delete all data", color = Coral)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") } },
        )
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = ScreenHorizontalPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        item {
            Column {
                Text("Your app, your choices", color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
                Text("Privacy & controls", color = Forest, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        item {
            SectionTitle("Tracking")
            PaisaCard {
                Column {
                    val smsStatus = when {
                        !BuildConfig.SMS_IMPORT_AVAILABLE -> "Unavailable in the manual-only build"
                        preferences.smsTrackingEnabled -> "On · locally screened for bank alerts"
                        preferences.lastSmsPermissionState == "revoked" -> "Paused · permission was revoked"
                        preferences.lastSmsPermissionState == "denied" -> "Off · permission was denied"
                        else -> "Off · no SMS is read"
                    }
                    SettingRow(
                        Icons.Outlined.Sms,
                        "SMS transaction tracking",
                        smsStatus,
                        onClick = if (BuildConfig.SMS_IMPORT_AVAILABLE && !preferences.smsTrackingEnabled) onEnableSms else onPermissionStatus,
                        trailing = if (preferences.smsTrackingEnabled) "On" else "Off",
                    )
                    if (BuildConfig.SMS_IMPORT_AVAILABLE && preferences.smsTrackingEnabled) {
                        TextButton(onClick = onDisableSms, modifier = Modifier.fillMaxWidth().padding(start = 56.dp)) { Text("Turn off SMS tracking", color = Teal) }
                    }
                    androidx.compose.material3.HorizontalDivider(color = Border, modifier = Modifier.padding(start = 68.dp))
                    SettingRow(
                        Icons.Outlined.NotificationsActive,
                        "Bill reminders",
                        if (preferences.notificationPermissionGranted) "Notifications allowed · WorkManager timing is approximate" else "Notifications off · saved bills remain available",
                        onClick = onRequestNotifications,
                        trailing = if (preferences.notificationPermissionGranted) "On" else "Off",
                    )
                }
            }
        }
        item {
            SectionTitle("Privacy & data")
            PaisaCard {
                Column {
                    SettingRow(Icons.Outlined.Lock, "Privacy choices", "Review SMS and notification access", onClick = onPermissionStatus)
                    androidx.compose.material3.HorizontalDivider(color = Border, modifier = Modifier.padding(start = 68.dp))
                    SettingRow(Icons.Outlined.Storage, "On-device storage", "Sensitive finance fields are encrypted with Android Keystore", onClick = onPermissionStatus)
                    androidx.compose.material3.HorizontalDivider(color = Border, modifier = Modifier.padding(start = 68.dp))
                    SettingRow(Icons.Outlined.Policy, "Privacy policy", "How local processing and data controls work", onClick = onPolicy)
                }
            }
        }
        item {
            SectionTitle("Your data")
            PaisaCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { onExport { csv -> csvToExport = csv; createDocument.launch("PaisaTrack-export.csv") } },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp),
                    ) {
                        androidx.compose.material3.Icon(Icons.Outlined.Download, null)
                        Text("  Export transactions and bills as CSV")
                    }
                    Button(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftCoral, contentColor = Coral),
                    ) {
                        androidx.compose.material3.Icon(Icons.Outlined.DeleteOutline, null)
                        Text("  Delete all data")
                    }
                    exportMessage?.let { Text(it, color = SecondaryText, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        item {
            SectionTitle("Support")
            PaisaCard {
                SettingRow(Icons.AutoMirrored.Outlined.HelpOutline, "Show an error state", "Preview the recovery screen", onClick = onErrorState)
            }
        }
        item {
            InfoBanner("Privacy-first", "Eligible messages are screened on-device. Message bodies are not saved or uploaded; financial records stay local.")
            Spacer(Modifier.height(5.dp))
            Text("PaisaTrack · 1.0.0", color = SecondaryText, style = MaterialTheme.typography.labelSmall, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp))
        }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: String? = null,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 74.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SoftIcon(icon, null)
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Text(title, color = Forest, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = SecondaryText, style = MaterialTheme.typography.bodySmall, lineHeight = MaterialTheme.typography.bodySmall.lineHeight)
            }
            if (trailing != null) Text(trailing, color = Teal, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}
