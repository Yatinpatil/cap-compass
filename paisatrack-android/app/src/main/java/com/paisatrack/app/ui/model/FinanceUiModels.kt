package com.paisatrack.app.ui.model

import com.paisatrack.app.domain.model.BillingFrequency
import com.paisatrack.app.domain.model.FinancePreferences
import com.paisatrack.app.domain.model.TransactionKind

data class DashboardUiState(
    val monthlyIncome: String? = null,
    val monthlyExpenses: String? = null,
    val netCashFlow: String? = null,
    val categories: List<CategorySpendUiModel> = emptyList(),
    val recentTransactions: List<TransactionUiModel> = emptyList(),
    val upcomingPayments: List<BillUiModel> = emptyList(),
    val pendingReviewCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class CategorySpendUiModel(
    val name: String,
    val amount: String,
    val sharePercent: Int,
    val colorIndex: Int,
)

data class TransactionUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: String,
    val amountInput: String,
    val isIncome: Boolean,
    val category: String,
    val date: String,
    val paymentMethod: String,
    val reference: String,
    val amountPaise: Long = 0L,
    val transactionKind: TransactionKind = if (isIncome) TransactionKind.INCOME else TransactionKind.EXPENSE,
    val pendingReview: Boolean = false,
    val occurredAtEpochMillis: Long = 0L,
    val note: String = "",
    val dateInput: String = "",
    val categoryId: String = "",
    val reversesKind: TransactionKind? = null,
)

data class BillUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: String,
    val dueDate: String,
    val amountPaise: Long? = null,
    val dueDateEpochMillis: Long? = null,
    val dueDateConfirmed: Boolean = true,
    val frequency: BillingFrequency = BillingFrequency.MONTHLY,
    val daysBeforeReminder: Int = 1,
    val enabled: Boolean = true,
)

data class PaisaTrackUiState(
    val dashboard: DashboardUiState = DashboardUiState(),
    val transactions: List<TransactionUiModel> = emptyList(),
    val categories: List<CategoryChoiceUiModel> = emptyList(),
    val bills: List<BillUiModel> = emptyList(),
    val monthlyTotals: List<Pair<String, Long>> = emptyList(),
    val preferences: FinancePreferences = FinancePreferences(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

data class CategoryChoiceUiModel(val id: String, val name: String)

data class TransactionFormUiModel(
    val amountRupees: String,
    val kind: TransactionKind,
    val merchant: String,
    val note: String,
    val categoryId: String,
    val dateInput: String,
    val reversesKind: TransactionKind? = null,
)

data class BillFormUiModel(
    val name: String,
    val amountRupees: String,
    val categoryId: String,
    val frequency: BillingFrequency,
    val dueDateInput: String,
    val daysBeforeReminder: Int,
    val dueDateWasInferred: Boolean = false,
)
