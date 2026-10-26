package com.paisatrack.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paisatrack.app.ui.components.EmptyState
import com.paisatrack.app.ui.components.PaisaCard
import com.paisatrack.app.ui.components.PreviewDataLabel
import com.paisatrack.app.ui.components.ScreenHorizontalPadding
import com.paisatrack.app.ui.components.SectionTitle
import com.paisatrack.app.ui.model.TransactionUiModel
import com.paisatrack.app.ui.theme.Forest
import com.paisatrack.app.ui.theme.SecondaryText

@Composable
fun TransactionsScreen(
    transactions: List<TransactionUiModel>,
    demoMode: Boolean = false,
    onAdd: () -> Unit = {},
    onTransactionClick: (String) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    val filters = listOf("All", "Expenses", "Income", "Needs review")
    val filteredTransactions = remember(transactions, query, selectedFilter) {
        transactions.filter { transaction ->
            val matchesQuery = query.isBlank() || listOf(transaction.title, transaction.subtitle, transaction.category, transaction.date)
                .any { it.contains(query.trim(), ignoreCase = true) }
            val matchesFilter = when (selectedFilter) {
                "Expenses" -> transaction.transactionKind == com.paisatrack.app.domain.model.TransactionKind.EXPENSE
                "Income" -> transaction.isIncome
                "Needs review" -> transaction.pendingReview
                else -> true
            }
            matchesQuery && matchesFilter
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
                Text("Everyday activity", color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
                Text("Your activity", color = Forest, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search transactions") },
                leadingIcon = { androidx.compose.material3.Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = { androidx.compose.material3.Icon(Icons.Outlined.Tune, contentDescription = "Filters") },
                singleLine = true,
                shape = RoundedCornerShape(15.dp),
            )
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                    )
                }
            }
        }
        item { SectionTitle("This month") }
        if (filteredTransactions.isEmpty()) {
            item {
                PaisaCard {
                    EmptyState(
                        title = if (transactions.isEmpty()) "No transactions yet" else "No transactions found",
                        message = if (transactions.isEmpty()) "Add a transaction manually to start your private money timeline." else "Try another search, or clear your filters.",
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    )
                }
            }
        } else {
            items(filteredTransactions, key = { it.id }) { transaction ->
                PaisaCard {
                    TransactionRow(transaction, onClick = { onTransactionClick(transaction.id) })
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
                Text("Add transaction", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
