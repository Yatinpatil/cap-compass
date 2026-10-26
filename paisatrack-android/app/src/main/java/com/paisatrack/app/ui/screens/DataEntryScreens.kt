package com.paisatrack.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.paisatrack.app.domain.model.BillingFrequency
import com.paisatrack.app.domain.model.TransactionKind
import com.paisatrack.app.ui.components.InfoBanner
import com.paisatrack.app.ui.components.PaisaCard
import com.paisatrack.app.ui.components.ScreenHorizontalPadding
import com.paisatrack.app.ui.model.BillFormUiModel
import com.paisatrack.app.ui.model.CategoryChoiceUiModel
import com.paisatrack.app.ui.model.TransactionFormUiModel
import com.paisatrack.app.ui.model.TransactionUiModel
import com.paisatrack.app.ui.theme.Forest
import com.paisatrack.app.ui.theme.SecondaryText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AddTransactionScreen(
    transaction: TransactionUiModel? = null,
    categories: List<CategoryChoiceUiModel> = emptyList(),
    onSave: (TransactionFormUiModel) -> Unit,
) {
    val kindLabels = listOf(
        "Expense" to TransactionKind.EXPENSE,
        "Income" to TransactionKind.INCOME,
        "Refund" to TransactionKind.REFUND,
        "Reversal" to TransactionKind.REVERSAL,
        "Transfer" to TransactionKind.OWN_ACCOUNT_TRANSFER,
        "Card repayment" to TransactionKind.CREDIT_CARD_REPAYMENT,
    )
    var kind by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.transactionKind ?: TransactionKind.EXPENSE) }
    var amount by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.amountInput.orEmpty()) }
    var description by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.title.orEmpty()) }
    var note by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.note.orEmpty()) }
    var categoryId by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.categoryId.orEmpty()) }
    var reversesKind by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.reversesKind ?: TransactionKind.EXPENSE) }
    var date by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.dateInput?.ifBlank { todayInput() } ?: todayInput()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenHorizontalPadding, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Text("Saved only on this device", color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
        Text(if (transaction == null) "Transaction details" else "Update the record", color = Forest, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        PaisaCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    kindLabels.forEach { (label, value) -> FilterChip(selected = kind == value, onClick = { kind = value }, label = { Text(label) }) }
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Merchant or description") }, singleLine = true)
                OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Note (optional)") }, singleLine = true)
                Text("Category", color = SecondaryText, style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { option -> FilterChip(selected = categoryId == option.id, onClick = { categoryId = option.id }, label = { Text(option.name) }) }
                }
                if (categoryId.isBlank()) Text("Leave category unselected for an automatic merchant-based suggestion.", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                if (kind == TransactionKind.REVERSAL) {
                    Text("This reverses", color = SecondaryText, style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(TransactionKind.EXPENSE, TransactionKind.REFUND, TransactionKind.INCOME).forEach { original ->
                            FilterChip(selected = reversesKind == original, onClick = { reversesKind = original }, label = { Text(original.name.lowercase().replaceFirstChar(Char::uppercase)) })
                        }
                    }
                }
                OutlinedTextField(value = date, onValueChange = { date = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Date · day/month/year") }, supportingText = { Text("For example, ${todayInput()}") }, singleLine = true)
            }
        }
        if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = {
                if (description.isBlank()) error = "Enter a merchant or description."
                else onSave(TransactionFormUiModel(amount, kind, description, note, categoryId, date, if (kind == TransactionKind.REVERSAL) reversesKind else null))
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Forest),
        ) { Text(if (transaction == null) "Save transaction" else "Save changes", fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun AddBillScreen(categories: List<CategoryChoiceUiModel> = emptyList(), onSave: (BillFormUiModel) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var dueDate by rememberSaveable { mutableStateOf("") }
    var frequency by rememberSaveable { mutableStateOf(BillingFrequency.MONTHLY) }
    var reminder by rememberSaveable { mutableIntStateOf(1) }
    var categoryId by rememberSaveable { mutableStateOf(categories.firstOrNull()?.id.orEmpty()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenHorizontalPadding, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Text("Due dates are never guessed", color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
        Text("Payment details", color = Forest, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        PaisaCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Bill or service name") }, singleLine = true)
                OutlinedTextField(value = amount, onValueChange = { amount = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Amount in ₹ (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(value = dueDate, onValueChange = { dueDate = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Next due date (optional)") }, supportingText = { Text("Day/month/year. Leave blank if not known.") }, singleLine = true)
                Text("Repeat", color = SecondaryText, style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BillingFrequency.entries.forEach { value ->
                        FilterChip(selected = frequency == value, onClick = { frequency = value }, label = { Text(value.name.lowercase().replaceFirstChar(Char::uppercase)) })
                    }
                }
                Text("Category", color = SecondaryText, style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { value -> FilterChip(selected = categoryId == value.id, onClick = { categoryId = value.id }, label = { Text(value.name) }) }
                }
                Text("Remind me", color = SecondaryText, style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "On due date", 1 to "1 day before", 3 to "3 days before").forEach { (days, label) ->
                        FilterChip(selected = reminder == days, onClick = { reminder = days }, label = { Text(label) })
                    }
                }
            }
        }
        if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        if (dueDate.isBlank()) InfoBanner("Date not set", "The payment will be saved, but no reminder can be scheduled until you add a confirmed due date.")
        Button(
            onClick = {
                when {
                    name.isBlank() -> error = "Enter a bill or service name."
                    else -> onSave(BillFormUiModel(name, amount, categoryId, frequency, dueDate, reminder))
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Forest),
        ) { Text("Save bill", fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(4.dp))
    }
}

private fun todayInput(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ROOT))
