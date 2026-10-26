package com.paisatrack.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SmsFailed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.paisatrack.app.ui.components.PaisaCard
import com.paisatrack.app.ui.components.ScreenHorizontalPadding
import com.paisatrack.app.ui.components.SoftIcon
import com.paisatrack.app.ui.theme.Forest
import com.paisatrack.app.ui.theme.SecondaryText

enum class FeedbackKind { Loading, Error, PermissionDenied }

@Composable
fun FeedbackStateScreen(
    kind: FeedbackKind,
    permissionAvailable: Boolean = false,
    permissionStatus: String = "not_requested",
    onRequestPermission: (() -> Unit)? = null,
    onOpenSystemSettings: () -> Unit = {},
    onRetry: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenHorizontalPadding, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        PaisaCard {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                when (kind) {
                    FeedbackKind.Loading -> CircularProgressIndicator(color = Forest)
                    FeedbackKind.Error -> SoftIcon(Icons.Outlined.CloudOff, null, modifier = Modifier.padding(4.dp))
                    FeedbackKind.PermissionDenied -> SoftIcon(Icons.Outlined.SmsFailed, null, modifier = Modifier.padding(4.dp))
                }
                Text(
                    when (kind) {
                        FeedbackKind.Loading -> "Getting your local records"
                        FeedbackKind.Error -> "We couldn't show your records"
                        FeedbackKind.PermissionDenied -> "SMS access is off"
                    },
                    color = Forest,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    when (kind) {
                        FeedbackKind.Loading -> "Your information stays on this device. This should only take a moment."
                        FeedbackKind.Error -> "Your saved information is kept on this device. Try again or continue with manual tracking."
                        FeedbackKind.PermissionDenied -> if (!permissionAvailable) "PaisaTrack can still be used manually. This build does not include SMS import." else "PaisaTrack remains useful with manual entry. You can review SMS access again in Android settings. Current status: $permissionStatus."
                    },
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                if (kind == FeedbackKind.PermissionDenied) {
                    SoftIcon(Icons.Outlined.Lock, null)
                    Text(if (permissionAvailable) "SMS bodies are not saved or uploaded. Parsed transaction candidates require your review." else "No SMS permission is declared by this build.", color = SecondaryText, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    if (permissionAvailable && onRequestPermission != null) TextButton(onClick = onRequestPermission) { Text("Review SMS access") }
                    if (permissionAvailable) OutlinedButton(onClick = onOpenSystemSettings) { Text("Open app settings") }
                }
                if (kind == FeedbackKind.Error) Button(onClick = onRetry) { Text("Try again") }
                OutlinedButton(onClick = onBack) { Text("Back to the app") }
            }
        }
    }
}
