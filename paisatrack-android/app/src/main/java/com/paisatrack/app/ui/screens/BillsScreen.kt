package com.paisatrack.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paisatrack.app.ui.components.EmptyState
import com.paisatrack.app.ui.components.InfoBanner
import com.paisatrack.app.ui.components.PaisaCard
import com.paisatrack.app.ui.components.PreviewDataLabel
import com.paisatrack.app.ui.components.ScreenHorizontalPadding
import com.paisatrack.app.ui.components.SectionTitle
import com.paisatrack.app.ui.components.SoftIcon
import com.paisatrack.app.ui.model.BillUiModel
import com.paisatrack.app.ui.theme.Coral
import com.paisatrack.app.ui.theme.Forest
import com.paisatrack.app.ui.theme.Mint
import com.paisatrack.app.ui.theme.SecondaryText
import com.paisatrack.app.ui.theme.Teal

@Composable
fun BillsScreen(
    bills: List<BillUiModel>,
    demoMode: Boolean = false,
    onAdd: () -> Unit = {},
    onToggleReminder: (String, Boolean) -> Unit = { _, _ -> },
    onDelete: (String) -> Unit = {},
    onConfirmDate: (String, String) -> Unit = { _, _ -> },
) {
    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    var deleteBill by rememberSaveable { mutableStateOf<BillUiModel?>(null) }
    var confirmDateBill by rememberSaveable { mutableStateOf<BillUiModel?>(null) }
    var confirmedDate by rememberSaveable { mutableStateOf("") }
    deleteBill?.let { bill ->
        AlertDialog(
            onDismissRequest = { deleteBill = null },
            title = { Text("Delete ${bill.title}?") },
            text = { Text("Its local reminder will also be cancelled.") },
            confirmButton = { TextButton(onClick = { onDelete(bill.id); deleteBill = null }) { Text("Delete", color = Coral) } },
            dismissButton = { TextButton(onClick = { deleteBill = null }) { Text("Cancel") } },
        )
    }
    confirmDateBill?.let { bill ->
        AlertDialog(
            onDismissRequest = { confirmDateBill = null },
            title = { Text("Confirm the due date") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A due date for ${bill.title} needs your confirmation before a reminder can be scheduled.")
                    OutlinedTextField(value = confirmedDate, onValueChange = { confirmedDate = it }, label = { Text("Day/month/year") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = { onConfirmDate(bill.id, confirmedDate); confirmedDate = ""; confirmDateBill = null }) { Text("Confirm date") }
            },
            dismissButton = { TextButton(onClick = { confirmDateBill = null; confirmedDate = "" }) { Text("Cancel") } },
        )
    }
    val visibleBills = bills.filter { bill ->
        when (selectedFilter) {
            "Upcoming" -> bill.dueDateEpochMillis?.let { it >= System.currentTimeMillis() } == true
            "Past" -> bill.dueDateEpochMillis?.let { it < System.currentTimeMillis() } == true
            else -> true
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = ScreenHorizontalPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                if (demoMode) PreviewDataLabel()
                Text("Stay ahead of due dates", color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
                Text("Upcoming payments", color = Forest, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        item {
            PaisaCard {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    SoftIcon(Icons.Outlined.NotificationsActive, null)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("Gentle reminders", color = Forest, fontWeight = FontWeight.SemiBold)
                        Text("Local, approximate timing. Notifications can arrive later.", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("On device", color = Teal, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Upcoming", "Past").forEach { filter ->
                    FilterChip(selected = selectedFilter == filter, onClick = { selectedFilter = filter }, label = { Text(filter) })
                }
            }
        }
        item { SectionTitle("Your payments") }
        if (visibleBills.isEmpty()) {
            item {
                PaisaCard {
                    EmptyState(
                        title = if (bills.isEmpty()) "No bills or renewals yet" else "No payments in this filter",
                        message = "Add rent, utilities, subscriptions or other repeat payments to keep their due dates in one place.",
                        icon = Icons.Outlined.CalendarMonth,
                    )
                }
            }
        } else {
            items(visibleBills, key = { it.id }) { bill ->
                PaisaCard {
                    Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SoftIcon(Icons.Outlined.CalendarMonth, null, background = Mint)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(bill.title, color = Forest, fontWeight = FontWeight.SemiBold)
                                Text(bill.subtitle, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                                Text(if (bill.dueDateConfirmed) "Due ${bill.dueDate}" else if (bill.dueDateEpochMillis != null) "Date to confirm · ${bill.dueDate}" else "Due date not set", color = Teal, style = MaterialTheme.typography.labelMedium)
                            }
                            Text(bill.amount, color = Forest, fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { deleteBill = bill }) {
                                androidx.compose.material3.Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete ${bill.title}", tint = Coral)
                            }
                        }
                        androidx.compose.material3.HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Reminder", modifier = Modifier.weight(1f), color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = bill.enabled,
                                onCheckedChange = { onToggleReminder(bill.id, it) },
                                enabled = bill.dueDateConfirmed,
                            )
                        }
                        if (bill.dueDateEpochMillis != null && !bill.dueDateConfirmed) {
                            TextButton(onClick = { confirmDateBill = bill }, modifier = Modifier.align(Alignment.End)) { Text("Confirm due date") }
                        }
                        if (bill.dueDateEpochMillis == null) InfoBanner("No date set", "Add a confirmed due date to enable a reminder.")
                    }
                }
            }
        }
        item {
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest),
            ) {
                androidx.compose.material3.Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 3.dp))
                Text("Add a bill or renewal", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
