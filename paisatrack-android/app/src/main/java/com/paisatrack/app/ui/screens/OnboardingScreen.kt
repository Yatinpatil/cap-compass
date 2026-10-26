package com.paisatrack.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisatrack.app.ui.components.InfoBanner
import com.paisatrack.app.ui.components.PaisaCard
import com.paisatrack.app.ui.components.PaisaLogo
import com.paisatrack.app.ui.components.ScreenHorizontalPadding
import com.paisatrack.app.ui.components.SoftIcon
import com.paisatrack.app.ui.theme.Forest
import com.paisatrack.app.ui.theme.Mint
import com.paisatrack.app.ui.theme.SecondaryText
import com.paisatrack.app.ui.theme.SoftTeal

@Composable
fun OnboardingScreen(
    smsImportAvailable: Boolean = false,
    onContinue: () -> Unit,
    onEnableSms: () -> Unit = {},
    onPermissionInfo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenHorizontalPadding, vertical = 26.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        PaisaLogo()
        Spacer(Modifier.height(30.dp))
        Surface(color = Mint, shape = RoundedCornerShape(50)) {
            Text(
                "PRIVATE BY DESIGN",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                color = Forest,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Know where your money goes.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Forest,
            lineHeight = 39.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "A calmer way to keep track of spending, bills and renewals.",
            style = MaterialTheme.typography.bodyLarge,
            color = SecondaryText,
            lineHeight = 25.sp,
        )
        Spacer(Modifier.height(22.dp))

        PaisaCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
                PrivacyBenefit(Icons.Outlined.PhoneAndroid, "Your data stays on this device", "Your finances are designed to remain private and offline-first.")
                PrivacyBenefit(Icons.Outlined.CloudOff, "No message uploads", "SMS content is never sent to a server. Unrelated messages and OTPs are not retained.")
                PrivacyBenefit(Icons.Outlined.Lock, "You stay in control", "SMS tracking is optional. You can continue with manual tracking.")
            }
        }
        Spacer(Modifier.height(16.dp))
        InfoBanner(
            title = "About SMS access",
            message = if (smsImportAvailable) "Optional SMS tracking screens eligible financial alerts on this device. It requires your consent and restricted Android permissions. Manual entry always remains available." else "This manual-only build does not request restricted SMS access. Manual tracking remains available.",
        )
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Forest),
        ) {
            Text("Continue with manual tracking", fontWeight = FontWeight.SemiBold)
        }
        if (smsImportAvailable) {
            OutlinedButton(
                onClick = onEnableSms,
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 4.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text("Review optional SMS tracking") }
        }
        TextButton(onClick = onPermissionInfo, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SoftIcon(Icons.Outlined.Sms, null, modifier = Modifier.padding(end = 8.dp).then(Modifier))
                Text("Why is SMS access restricted?", color = Forest)
            }
        }
        Text(
            "No permission is requested on this screen.",
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PrivacyBenefit(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        SoftIcon(icon, contentDescription = null, background = SoftTeal)
        Column(Modifier.padding(start = 12.dp, top = 2.dp)) {
            Text(title, color = Forest, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(body, color = SecondaryText, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
        }
    }
}
