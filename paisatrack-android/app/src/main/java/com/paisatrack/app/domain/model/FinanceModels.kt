package com.paisatrack.app.domain.model

enum class TransactionKind {
    EXPENSE,
    INCOME,
    REFUND,
    REVERSAL,
    OWN_ACCOUNT_TRANSFER,
    CREDIT_CARD_REPAYMENT,
}

enum class TransactionStatus { PENDING_REVIEW, CONFIRMED }
enum class TransactionSource { MANUAL, SMS }
enum class BillingFrequency { WEEKLY, MONTHLY, QUARTERLY, YEARLY, ONE_TIME }
enum class DueDateSource { USER_ENTERED, IMPORTED_CONFIRMED, INFERRED_UNCONFIRMED, UNKNOWN }
enum class ReminderStatus { SCHEDULED, DELIVERED, CANCELLED, PAUSED_PERMISSION }

data class FinanceTransaction(
    val id: String,
    val amountPaise: Long,
    val kind: TransactionKind,
    val status: TransactionStatus,
    val merchant: String,
    val note: String? = null,
    val categoryId: String,
    val occurredAtEpochMillis: Long,
    val source: TransactionSource,
    val deduplicationKey: String? = null,
    val referenceFingerprint: String? = null,
    val reversesKind: TransactionKind? = null,
    val isCategoryUserEdited: Boolean = false,
) {
    init {
        require(amountPaise > 0) { "Transaction amounts must be positive integer paise." }
        require(kind == TransactionKind.REVERSAL || reversesKind == null) {
            "Only a reversal may contain the type of the original transaction."
        }
    }
}

data class FinanceCategory(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorKey: String,
    val isDefault: Boolean,
)

data class RecurringPayment(
    val id: String,
    val name: String,
    val amountPaise: Long?,
    val categoryId: String,
    val frequency: BillingFrequency,
    val dueDateEpochMillis: Long?,
    val dueDateSource: DueDateSource,
    val daysBeforeReminder: Int,
    val enabled: Boolean,
    val createdAtEpochMillis: Long,
) {
    val dueDateConfirmed: Boolean
        get() = dueDateEpochMillis != null && dueDateSource != DueDateSource.INFERRED_UNCONFIRMED && dueDateSource != DueDateSource.UNKNOWN

    init {
        require(amountPaise == null || amountPaise > 0)
        require(daysBeforeReminder in 0..30)
    }
}

data class FinancePreferences(
    val onboardingComplete: Boolean = false,
    val smsDisclosureAccepted: Boolean = false,
    val smsTrackingEnabled: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val lastSmsPermissionState: String = "not_requested",
)

data class TransactionDraft(
    val amountPaise: Long,
    val kind: TransactionKind,
    val merchant: String,
    val note: String?,
    val categoryId: String,
    val occurredAtEpochMillis: Long,
    val reversesKind: TransactionKind? = null,
)

data class BillDraft(
    val name: String,
    val amountPaise: Long?,
    val categoryId: String,
    val frequency: BillingFrequency,
    val dueDateEpochMillis: Long?,
    val dueDateSource: DueDateSource,
    val daysBeforeReminder: Int,
)
