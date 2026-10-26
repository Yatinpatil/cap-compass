package com.paisatrack.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paisatrack.app.ui.components.InfoBanner
import com.paisatrack.app.ui.components.PaisaCard
import com.paisatrack.app.ui.components.ScreenHorizontalPadding
import com.paisatrack.app.ui.theme.Forest
import com.paisatrack.app.ui.theme.SecondaryText

@Composable
fun PrivacyPolicyScreen() {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = ScreenHorizontalPadding, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Your privacy matters", color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
        Text("How your data is handled", color = Forest, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        InfoBanner("Local processing", "PaisaTrack does not send finance records or SMS content to a server. The app has no account or cloud sync feature.")
        PolicySection("Finance records", "Transactions, categories, bills, reminders and app preferences are stored in the app's local Room database. Sensitive amounts, names, notes and dates are encrypted using keys held by Android Keystore. Android app data may be removed when you uninstall the app or clear its storage.")
        PolicySection("Optional SMS tracking", "The manual-only Play build does not declare SMS permissions. A separate SMS-review build can request READ_SMS and RECEIVE_SMS only after the user sees this disclosure and consents. Those permissions are restricted by Google Play and require policy review; availability is not guaranteed.")
        PolicySection("Message handling", "When SMS tracking is available and enabled, the app screens inbox and incoming messages locally for eligible transaction alerts. Message bodies are transient parser input and are not written to the database or sent anywhere. OTP and unrelated messages are excluded. A parsed transaction is kept as a pending candidate until you review it. Inbox import is limited to recent messages.")
        PolicySection("Reminders", "Bill reminders are scheduled on this device with WorkManager. Android may delay them, and exact delivery is not promised. Notifications are optional and use generic text that does not show the bill name or amount.")
        PolicySection("Export and deletion", "CSV export is initiated only when you choose it and uses Android's document picker to save a file to the location you select. Delete all data removes saved records, custom categories, settings and import checkpoints from this app and deletes encryption keys. Exported files are outside PaisaTrack's control.")
        PolicySection("Permissions and controls", "You can use manual transaction and bill entry without SMS access. Turn off SMS tracking in Settings or revoke access in Android settings. Notification denial pauses reminders; it does not remove bills. If permission is revoked, tracking stops and the app explains the status.")
        PolicySection("Publisher and contact", "Publisher: Yatin Patil. The public policy provides a privacy contact channel. Google Play Data safety and store disclosures should match the build submitted.")
        androidx.compose.material3.TextButton(onClick = { uriHandler.openUri("https://github.com/Yatinpatil/cap-compass/blob/main/paisatrack-android/PRIVACY_POLICY.md") }) {
            Text("Open the full privacy policy", color = Forest)
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    PaisaCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Forest, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(body, color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
