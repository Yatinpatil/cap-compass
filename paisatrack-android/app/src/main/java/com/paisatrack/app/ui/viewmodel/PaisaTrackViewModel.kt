package com.paisatrack.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paisatrack.app.data.repository.FinanceRepository
import com.paisatrack.app.data.sms.SmsInboxImporter
import com.paisatrack.app.BuildConfig
import com.paisatrack.app.domain.model.BillDraft
import com.paisatrack.app.domain.model.BillingFrequency
import com.paisatrack.app.domain.model.DueDateSource
import com.paisatrack.app.domain.model.FinanceCategory
import com.paisatrack.app.domain.model.FinanceTransaction
import com.paisatrack.app.domain.model.TransactionDraft
import com.paisatrack.app.domain.model.TransactionKind
import com.paisatrack.app.domain.model.TransactionStatus
import com.paisatrack.app.domain.money.FinanceDateParser
import com.paisatrack.app.domain.money.MoneyParser
import com.paisatrack.app.domain.spending.SpendingCalculator
import com.paisatrack.app.ui.model.BillUiModel
import com.paisatrack.app.ui.model.CategoryChoiceUiModel
import com.paisatrack.app.ui.model.CategorySpendUiModel
import com.paisatrack.app.ui.model.DashboardUiState
import com.paisatrack.app.ui.model.PaisaTrackUiState
import com.paisatrack.app.ui.model.TransactionFormUiModel
import com.paisatrack.app.ui.model.TransactionUiModel
import com.paisatrack.app.work.ReminderScheduler
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PaisaTrackViewModel(
    private val repository: FinanceRepository,
    private val inboxImporter: SmsInboxImporter,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val _uiState = MutableStateFlow(PaisaTrackUiState())
    val uiState: StateFlow<PaisaTrackUiState> = _uiState.asStateFlow()

    val dashboard: StateFlow<DashboardUiState> = uiState
        .map { state -> state.dashboard }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState(isLoading = true))

    init {
        viewModelScope.launch {
            repository.ensureDefaultCategories()
            combine(repository.observeSnapshot(), repository.observePreferences()) { snapshot, preferences ->
                val transactions = snapshot.transactions.map { it.toUi(snapshot.categories) }
                val now = System.currentTimeMillis()
                val bills = snapshot.recurringPayments.sortedBy { it.dueDateEpochMillis ?: Long.MAX_VALUE }.map { payment ->
                    BillUiModel(
                        id = payment.id,
                        title = payment.name,
                        subtitle = "${payment.frequency.name.lowercase().replaceFirstChar(Char::uppercase)} · ${if (payment.enabled) "Reminder on" else "Reminder paused"}",
                        amount = payment.amountPaise?.let(MoneyParser::formatPaise) ?: "Amount not set",
                        dueDate = payment.dueDateEpochMillis?.let { FinanceDateParser.formatDate(it, zoneId = zone) } ?: "Date not set",
                        amountPaise = payment.amountPaise,
                        dueDateEpochMillis = payment.dueDateEpochMillis,
                        dueDateConfirmed = payment.dueDateConfirmed,
                        frequency = payment.frequency,
                        daysBeforeReminder = payment.daysBeforeReminder,
                        enabled = payment.enabled,
                    )
                }
                val categories = spendingCategories(snapshot.categories, snapshot.monthlySpending.categoryNetPaise)
                val totalIncome = snapshot.monthlySpending.incomePaise
                val totalSpend = snapshot.monthlySpending.netSpendingPaise
                val dashboard = DashboardUiState(
                    monthlyIncome = MoneyParser.formatPaise(totalIncome),
                    monthlyExpenses = MoneyParser.formatPaise(totalSpend),
                    netCashFlow = MoneyParser.formatPaise(snapshot.monthlySpending.netCashFlowPaise),
                    categories = categories,
                    recentTransactions = transactions.filter { !it.pendingReview }.take(8),
                    upcomingPayments = bills.filter { it.dueDateEpochMillis?.let { date -> date >= now } == true || it.dueDateEpochMillis == null }.take(3),
                    pendingReviewCount = transactions.count { it.pendingReview },
                    isLoading = false,
                )
                PaisaTrackUiState(
                    dashboard = dashboard,
                    transactions = transactions,
                    categories = snapshot.categories.map { CategoryChoiceUiModel(it.id, it.name) },
                    bills = bills,
                    monthlyTotals = if (snapshot.transactions.any { it.status == TransactionStatus.CONFIRMED }) {
                        (5L downTo 0L).map { offset ->
                            val month = YearMonth.now(zone).minusMonths(offset)
                            val total = SpendingCalculator.calculate(snapshot.transactions, month, zone).netSpendingPaise.coerceAtLeast(0L)
                        month.format(DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("en-IN"))) to total
                        }
                    } else emptyList(),
                    preferences = preferences,
                    isLoading = false,
                    errorMessage = _uiState.value.errorMessage,
                )
            }.collect { _uiState.value = it }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(errorMessage = null) }

    fun completeOnboarding(onSaved: () -> Unit = {}) = launchAction {
        repository.savePreference(FinanceRepository.KEY_ONBOARDING, "true")
        onSaved()
    }

    fun acceptSmsDisclosureAndEnableTracking(onSaved: () -> Unit = {}) = launchAction {
        repository.savePreference(FinanceRepository.KEY_SMS_CONSENT, "true")
        repository.savePreference(FinanceRepository.KEY_SMS_TRACKING, "true")
        onSaved()
    }

    fun acceptSmsDisclosure(onSaved: () -> Unit = {}) = launchAction {
        repository.savePreference(FinanceRepository.KEY_SMS_CONSENT, "true")
        onSaved()
    }

    fun disableSmsTracking() = launchAction {
        repository.savePreference(FinanceRepository.KEY_SMS_TRACKING, "false")
    }

    fun updateSmsPermissionState(grantedRead: Boolean, grantedReceive: Boolean) = launchAction {
        val available = grantedRead || grantedReceive
        repository.savePreference(FinanceRepository.KEY_SMS_PERMISSION, when {
            grantedRead && grantedReceive -> "granted"
            grantedRead -> "read_only"
            grantedReceive -> "receive_only"
            else -> "denied"
        })
        repository.savePreference(FinanceRepository.KEY_SMS_TRACKING, (available && repository.smsImportEnabled()).toString())
        if (grantedRead && repository.smsImportEnabled()) inboxImporter.importExistingMessages()
    }

    fun updateNotificationPermission(granted: Boolean) = launchAction {
        repository.savePreference(FinanceRepository.KEY_NOTIFICATIONS, granted.toString())
        if (!granted) {
            _uiState.value.bills.forEach { reminderScheduler.cancel(it.id) }
        } else {
            repository.observeSnapshot().first().recurringPayments.filter { it.enabled && it.dueDateConfirmed }.forEach {
                reminderScheduler.schedule(it.id, it.dueDateEpochMillis, it.daysBeforeReminder, true)
            }
        }
    }

    fun refreshSystemPermissions(readSmsGranted: Boolean, receiveSmsGranted: Boolean, notificationsGranted: Boolean) = launchAction {
        val preferences = repository.observePreferences().first()
        if (BuildConfig.SMS_IMPORT_AVAILABLE && preferences.smsTrackingEnabled && !readSmsGranted && !receiveSmsGranted) {
            repository.savePreference(FinanceRepository.KEY_SMS_PERMISSION, "revoked")
            repository.savePreference(FinanceRepository.KEY_SMS_TRACKING, "false")
        }
        if (BuildConfig.SMS_IMPORT_AVAILABLE && (readSmsGranted || receiveSmsGranted) && preferences.smsDisclosureAccepted) {
            repository.savePreference(FinanceRepository.KEY_SMS_PERMISSION, when {
                readSmsGranted && receiveSmsGranted -> "granted"
                readSmsGranted -> "read_only"
                else -> "receive_only"
            })
        }
        val before = preferences.notificationPermissionGranted
        if (before != notificationsGranted) {
            repository.savePreference(FinanceRepository.KEY_NOTIFICATIONS, notificationsGranted.toString())
            if (notificationsGranted) {
                repository.observeSnapshot().first().recurringPayments.filter { it.enabled && it.dueDateConfirmed }.forEach {
                    reminderScheduler.schedule(it.id, it.dueDateEpochMillis, it.daysBeforeReminder, true)
                }
            } else {
                _uiState.value.bills.forEach { reminderScheduler.cancel(it.id) }
            }
        }
    }

    fun addOrUpdateTransaction(form: TransactionFormUiModel, id: String? = null, onSaved: () -> Unit = {}) = launchAction {
        val amount = MoneyParser.rupeesToPaise(form.amountRupees)
        val date = FinanceDateParser.parseDateAtStartOfDay(form.dateInput, zone)
        val category = form.categoryId.ifBlank { repository.defaultCategoryFor(form.merchant, form.note, form.kind) }
        val draft = TransactionDraft(amount, form.kind, form.merchant.trim(), form.note.trim().ifBlank { null }, category, date, form.reversesKind)
        if (id == null) repository.saveManualTransaction(draft) else repository.updateTransaction(id, draft)
        onSaved()
    }

    fun confirmSmsCandidate(id: String, merchant: String, amount: String, kind: TransactionKind, categoryId: String, onSaved: () -> Unit = {}) = launchAction {
        repository.confirmCandidate(id, merchant, MoneyParser.rupeesToPaise(amount), kind, categoryId)
        onSaved()
    }

    fun changeCategory(transactionId: String, categoryId: String) = launchAction { repository.setTransactionCategory(transactionId, categoryId) }

    fun deleteTransaction(id: String, onDeleted: () -> Unit = {}) = launchAction {
        repository.deleteTransaction(id)
        onDeleted()
    }

    fun addBill(name: String, amount: String, categoryId: String, frequency: BillingFrequency, dueDate: String, daysBefore: Int, onSaved: () -> Unit = {}) = launchAction {
        val due = dueDate.trim().takeIf(String::isNotEmpty)?.let { FinanceDateParser.parseDateAtStartOfDay(it, zone) }
        val parsedAmount = amount.trim().takeIf(String::isNotEmpty)?.let(MoneyParser::rupeesToPaise)
        val id = repository.saveRecurringPayment(
            BillDraft(name, parsedAmount, categoryId.ifBlank { com.paisatrack.app.domain.category.CategoryIds.BILLS }, frequency, due, if (due == null) DueDateSource.UNKNOWN else DueDateSource.USER_ENTERED, daysBefore),
        )
        scheduleBill(id)
        onSaved()
    }

    fun toggleBill(id: String, enabled: Boolean) = launchAction {
        repository.setRecurringPaymentEnabled(id, enabled)
        val bill = _uiState.value.bills.firstOrNull { it.id == id }
        if (enabled && bill?.dueDateConfirmed == true) reminderScheduler.schedule(id, bill.dueDateEpochMillis, bill.daysBeforeReminder, true)
        else reminderScheduler.cancel(id)
    }

    fun confirmBillDate(id: String, dateInput: String) = launchAction {
        val date = FinanceDateParser.parseDateAtStartOfDay(dateInput, zone)
        repository.confirmInferredDueDate(id, date)
        scheduleBill(id)
    }

    fun deleteBill(id: String) = launchAction {
        reminderScheduler.cancel(id)
        repository.deleteRecurringPayment(id)
    }

    fun exportCsv(onReady: (String) -> Unit) = launchAction { onReady(repository.exportCsv()) }

    fun deleteAllData(onDeleted: () -> Unit = {}) = launchAction {
        reminderScheduler.cancelAll()
        repository.deleteAllUserData()
        repository.ensureDefaultCategories()
        onDeleted()
    }

    private suspend fun scheduleBill(id: String) {
        val bill = repository.observeSnapshot().let { flow -> flow.first() }
            .recurringPayments.firstOrNull { it.id == id } ?: return
        val notificationsAllowed = _uiState.value.preferences.notificationPermissionGranted
        val enabled = bill.enabled && notificationsAllowed
        if (enabled) reminderScheduler.schedule(id, bill.dueDateEpochMillis, bill.daysBeforeReminder, bill.dueDateConfirmed)
        else reminderScheduler.cancel(id)
    }

    private fun launchAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                if (_uiState.value.errorMessage != null) _uiState.value = _uiState.value.copy(errorMessage = null)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Something went wrong. Please try again.")
            }
        }
    }

    private fun FinanceTransaction.toUi(categories: List<FinanceCategory>): TransactionUiModel {
        val categoryName = categories.firstOrNull { it.id == categoryId }?.name ?: "Other"
        val signed = when (kind) {
            TransactionKind.INCOME, TransactionKind.REFUND -> "+${MoneyParser.formatPaise(amountPaise)}"
            TransactionKind.REVERSAL -> if (reversesKind == TransactionKind.EXPENSE) "+${MoneyParser.formatPaise(amountPaise)}" else "−${MoneyParser.formatPaise(amountPaise)}"
            TransactionKind.OWN_ACCOUNT_TRANSFER, TransactionKind.CREDIT_CARD_REPAYMENT -> MoneyParser.formatPaise(amountPaise)
            TransactionKind.EXPENSE -> "−${MoneyParser.formatPaise(amountPaise)}"
        }
        return TransactionUiModel(
            id = id,
            title = merchant,
            subtitle = note ?: if (source.name == "SMS") "Imported from SMS · review" else "Manual entry",
            amount = signed,
            amountInput = BigDecimal.valueOf(amountPaise, 2).stripTrailingZeros().toPlainString(),
            isIncome = kind == TransactionKind.INCOME || kind == TransactionKind.REFUND || (kind == TransactionKind.REVERSAL && reversesKind == TransactionKind.EXPENSE),
            category = categoryName,
            date = FinanceDateParser.formatDate(occurredAtEpochMillis, zoneId = zone),
            dateInput = Instant.ofEpochMilli(occurredAtEpochMillis).atZone(zone).format(DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ROOT)),
            categoryId = categoryId,
            reversesKind = reversesKind,
            paymentMethod = if (source.name == "SMS") "SMS alert" else "Manual entry",
            reference = "Not stored",
            amountPaise = amountPaise,
            transactionKind = kind,
            pendingReview = status == TransactionStatus.PENDING_REVIEW,
            occurredAtEpochMillis = occurredAtEpochMillis,
            note = note.orEmpty(),
        )
    }

    private fun spendingCategories(categories: List<FinanceCategory>, amounts: Map<String, Long>): List<CategorySpendUiModel> {
        val positive = amounts.filterValues { it > 0L }
        val total = positive.values.sum().coerceAtLeast(1L)
        return positive.entries.sortedByDescending { it.value }.mapIndexed { index, entry ->
            CategorySpendUiModel(
                name = categories.firstOrNull { it.id == entry.key }?.name ?: "Other",
                amount = MoneyParser.formatPaise(entry.value),
                sharePercent = ((entry.value * 100L) / total).toInt(),
                colorIndex = index,
            )
        }
    }
}
