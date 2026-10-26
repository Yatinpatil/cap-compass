package com.paisatrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisatrack.app.ui.components.EmptyState
import com.paisatrack.app.ui.components.PaisaCard
import com.paisatrack.app.ui.components.PreviewDataLabel
import com.paisatrack.app.ui.components.ScreenHorizontalPadding
import com.paisatrack.app.ui.components.SectionTitle
import com.paisatrack.app.ui.components.SoftIcon
import com.paisatrack.app.ui.screens.FeedbackKind
import com.paisatrack.app.ui.model.DashboardUiState
import com.paisatrack.app.ui.model.TransactionUiModel
import com.paisatrack.app.ui.theme.Forest
import com.paisatrack.app.ui.theme.Mint
import com.paisatrack.app.ui.theme.SecondaryText
import com.paisatrack.app.ui.theme.SoftTeal
import com.paisatrack.app.ui.theme.Teal

@Composable
fun HomeScreen(
    state: DashboardUiState,
    demoMode: Boolean = false,
    onAddTransaction: () -> Unit = {},
    onTransactions: () -> Unit = {},
    onInsights: () -> Unit = {},
    onBills: () -> Unit = {},
    onTransactionClick: (String) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    if (state.isLoading || state.errorMessage != null) {
        FeedbackStateScreen(
            kind = if (state.isLoading) FeedbackKind.Loading else FeedbackKind.Error,
            onRetry = onRetry,
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = ScreenHorizontalPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column {
                if (demoMode) PreviewDataLabel()
                Text("Private money overview", color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
                Text("Your money at a glance", color = Forest, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        item {
            PaisaCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Forest, RoundedCornerShape(16.dp))
                        .padding(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftIcon(Icons.Outlined.CalendarMonth, null, background = Color.White.copy(alpha = 0.13f), tint = Mint)
                        Text("  This month", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(17.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SummaryAmount("Income", state.monthlyIncome, true)
                        SummaryAmount("Expenses", state.monthlyExpenses, false)
                    }
                    Spacer(Modifier.height(17.dp))
                    androidx.compose.material3.HorizontalDivider(color = Color.White.copy(alpha = 0.16f))
                    Spacer(Modifier.height(13.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Net cash flow", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
                        Text(state.netCashFlow ?: "—", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (state.pendingReviewCount > 0) {
            item {
                PaisaCard {
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        SoftIcon(Icons.AutoMirrored.Outlined.ReceiptLong, null, background = Mint, tint = Forest)
                        Column(Modifier.weight(1f).padding(start = 11.dp)) {
                            Text("${state.pendingReviewCount} transaction${if (state.pendingReviewCount == 1) "" else "s"} to review", color = Forest, fontWeight = FontWeight.SemiBold)
                            Text("SMS suggestions are not counted until you confirm them.", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = onTransactions) { Text("Review", color = Teal) }
                    }
                }
            }
        }
        item {
            SectionTitle("Spending categories", action = {
                TextButton(onClick = onInsights) { Text("See insights", color = Teal) }
            })
            if (state.categories.isEmpty()) {
                PaisaCard { EmptyState("Your categories will appear here", "Add or review transactions to see how your spending is grouped.") }
            } else {
                PaisaCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        state.categories.take(3).forEach { category ->
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(category.name, color = Forest, style = MaterialTheme.typography.bodyMedium)
                                    Text(category.amount, color = Forest, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(7.dp))
                                LinearProgressIndicator(
                                    progress = { (category.sharePercent.coerceIn(0, 100) / 100f) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = if (category.colorIndex % 2 == 0) Teal else Forest,
                                    trackColor = SoftTeal,
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionTitle("Recent transactions", action = { TextButton(onClick = onTransactions) { Text("View all", color = Teal) } })
            if (state.recentTransactions.isEmpty()) {
                    PaisaCard { EmptyState("No transactions yet", "Your activity will show up here once you add a transaction.", icon = Icons.AutoMirrored.Outlined.ReceiptLong) }
            } else {
                PaisaCard {
                    Column {
                        state.recentTransactions.take(3).forEachIndexed { index, transaction ->
                            TransactionRow(transaction, onClick = { onTransactionClick(transaction.id) })
                            if (index < minOf(2, state.recentTransactions.lastIndex)) {
                                androidx.compose.material3.HorizontalDivider(color = com.paisatrack.app.ui.theme.Border, modifier = Modifier.padding(start = 70.dp))
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionTitle("Upcoming payments", action = { TextButton(onClick = onBills) { Text("View bills", color = Teal) } })
            if (state.upcomingPayments.isEmpty()) {
                PaisaCard { EmptyState("Nothing due soon", "Add a bill or renewal to get a reminder before it is due.", icon = Icons.Outlined.CalendarMonth) }
            } else {
                PaisaCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        state.upcomingPayments.take(2).forEach { bill ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SoftIcon(Icons.Outlined.CalendarMonth, null, background = Mint, tint = Forest)
                                Column(Modifier.weight(1f).padding(start = 11.dp)) {
                                    Text(bill.title, color = Forest, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text("Due ${bill.dueDate}", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(bill.amount, color = Forest, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = onAddTransaction,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest),
            ) { Text("Add a transaction", fontWeight = FontWeight.SemiBold); Spacer(Modifier.padding(4.dp)); androidx.compose.material3.Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null) }
        }
    }
}

@Composable
private fun SummaryAmount(label: String, value: String?, income: Boolean) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(5.dp))
        Text(value ?: "—", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TransactionRow(transaction: TransactionUiModel, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(74.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SoftIcon(Icons.AutoMirrored.Outlined.ReceiptLong, null, background = if (transaction.isIncome) Mint else SoftTeal, tint = Forest)
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Text(transaction.title, color = Forest, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text("${transaction.category} · ${transaction.subtitle}", color = SecondaryText, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(transaction.amount, color = if (transaction.isIncome) Teal else Forest, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(transaction.date, color = SecondaryText, style = MaterialTheme.typography.labelSmall)
            }
            androidx.compose.material3.Icon(Icons.Outlined.ChevronRight, null, tint = SecondaryText, modifier = Modifier.padding(start = 4.dp))
        }
    }
}
