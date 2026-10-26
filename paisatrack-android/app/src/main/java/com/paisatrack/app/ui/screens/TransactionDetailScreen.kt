package com.paisatrack.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paisatrack.app.ui.components.InfoBanner
import com.paisatrack.app.ui.components.PaisaCard
import com.paisatrack.app.ui.components.PreviewDataLabel
import com.paisatrack.app.ui.components.ScreenHorizontalPadding
import com.paisatrack.app.ui.components.SoftIcon
import com.paisatrack.app.ui.model.TransactionUiModel
import com.paisatrack.app.ui.model.CategoryChoiceUiModel
import com.paisatrack.app.ui.theme.Coral
import com.paisatrack.app.ui.theme.Forest
import com.paisatrack.app.ui.theme.SecondaryText
import com.paisatrack.app.ui.theme.SoftCoral

@Composable
fun TransactionDetailScreen(
    transaction: TransactionUiModel?,
    categories: List<CategoryChoiceUiModel> = emptyList(),
    demoMode: Boolean = false,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onCategoryChanged: (String) -> Unit = {},
) {
    var category by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.categoryId.orEmpty()) }
    var categoryMenu by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete transaction?") },
            text = { Text("This transaction will be removed from your records.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete", color = Coral) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenHorizontalPadding, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        if (demoMode) PreviewDataLabel()
        if (transaction == null) {
            PaisaCard {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth()) {
                com.paisatrack.app.ui.components.EmptyState("Transaction unavailable", "This transaction may have been removed or is no longer available.", icon = Icons.AutoMirrored.Outlined.ReceiptLong)
                }
            }
            return@Column
        }

        PaisaCard {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                SoftIcon(Icons.Outlined.AccountBalanceWallet, null, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text(transaction.title, color = Forest, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(transaction.subtitle, color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Text(transaction.amount, color = Forest, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(if (transaction.pendingReview) "Needs your review" else transaction.transactionKind.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase), color = SecondaryText, style = MaterialTheme.typography.labelLarge)
            }
        }
        PaisaCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                DetailRow("Date", transaction.date)
                DetailRow("Payment method", transaction.paymentMethod)
                DetailRow("Reference", transaction.reference)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Category", color = SecondaryText)
                    androidx.compose.foundation.layout.Box {
                        TextButton(onClick = { categoryMenu = true }) {
                            Text(categories.firstOrNull { it.id == category }?.name ?: transaction.category, color = Forest, fontWeight = FontWeight.SemiBold)
                            androidx.compose.material3.Icon(Icons.Outlined.Edit, null, modifier = Modifier.padding(start = 6.dp))
                        }
                        DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                            categories.forEach { option ->
                                DropdownMenuItem(text = { Text(option.name) }, onClick = { category = option.id; categoryMenu = false; onCategoryChanged(option.id) })
                            }
                        }
                    }
                }
            }
        }
        InfoBanner(if (transaction.pendingReview) "Review before counting" else "Private record", if (transaction.pendingReview) "This candidate is not included in confirmed totals until you review and save it." else "This transaction is stored locally and the sensitive fields are protected with Android Keystore.")
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
        ) { androidx.compose.material3.Icon(Icons.Outlined.Edit, null); Text(if (transaction.pendingReview) "  Review and confirm" else "  Edit details") }
        Button(
            onClick = { confirmDelete = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SoftCoral, contentColor = Coral),
        ) { androidx.compose.material3.Icon(Icons.Outlined.DeleteOutline, null); Text("  Delete transaction") }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SecondaryText)
        Text(value, color = Forest, fontWeight = FontWeight.Medium)
    }
}
