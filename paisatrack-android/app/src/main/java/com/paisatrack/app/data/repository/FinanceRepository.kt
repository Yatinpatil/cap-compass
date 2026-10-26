package com.paisatrack.app.data.repository

import androidx.room.withTransaction
import com.paisatrack.app.data.local.AppSettingEntity
import com.paisatrack.app.data.local.CategoryEntity
import com.paisatrack.app.data.local.MoneyPaise
import com.paisatrack.app.data.local.PaisaTrackDatabase
import com.paisatrack.app.data.local.RecurringPaymentEntity
import com.paisatrack.app.data.local.SensitiveLong
import com.paisatrack.app.data.local.SensitiveText
import com.paisatrack.app.data.local.SmsImportCheckpointEntity
import com.paisatrack.app.data.local.TransactionEntity
import com.paisatrack.app.data.privacy.KeystoreCipher
import com.paisatrack.app.domain.category.CategoryIds
import com.paisatrack.app.domain.category.TransactionCategorizer
import com.paisatrack.app.domain.deduplication.DuplicateDetector
import com.paisatrack.app.domain.deduplication.TransactionFingerprintInput
import com.paisatrack.app.domain.model.BillDraft
import com.paisatrack.app.domain.model.DueDateSource
import com.paisatrack.app.domain.model.FinancePreferences
import com.paisatrack.app.domain.model.FinanceCategory
import com.paisatrack.app.domain.model.FinanceTransaction
import com.paisatrack.app.domain.model.RecurringPayment
import com.paisatrack.app.domain.model.TransactionDraft
import com.paisatrack.app.domain.model.TransactionKind
import com.paisatrack.app.domain.model.TransactionSource
import com.paisatrack.app.domain.model.TransactionStatus
import com.paisatrack.app.domain.spending.MonthlySpending
import com.paisatrack.app.domain.spending.SpendingCalculator
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class FinanceSnapshot(
    val transactions: List<FinanceTransaction>,
    val categories: List<FinanceCategory>,
    val recurringPayments: List<RecurringPayment>,
    val monthlySpending: MonthlySpending,
)

class FinanceRepository(
    private val database: PaisaTrackDatabase,
    private val categorizer: TransactionCategorizer = TransactionCategorizer(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    private val transactionDao = database.transactions()
    private val categoryDao = database.categories()
    private val paymentDao = database.recurringPayments()
    private val settingsDao = database.settings()
    private val checkpointDao = database.smsCheckpoints()
    private val duplicateDetector = DuplicateDetector(com.paisatrack.app.data.privacy.KeystoreFingerprintHasher())

    suspend fun ensureDefaultCategories() {
        categoryDao.insertDefaults(DEFAULT_CATEGORIES)
    }

    fun observeSnapshot(month: YearMonth = YearMonth.now(zoneId)): Flow<FinanceSnapshot> = combine(
        transactionDao.observeAll(),
        categoryDao.observeAll(),
        paymentDao.observeAll(),
    ) { transactionRows, categoryRows, paymentRows ->
        val transactions = transactionRows.map { it.toDomain() }.sortedByDescending { it.occurredAtEpochMillis }
        val categories = categoryRows.map { it.toDomain() }
        val payments = paymentRows.map { it.toDomain() }
        FinanceSnapshot(
            transactions = transactions,
            categories = categories,
            recurringPayments = payments,
            monthlySpending = SpendingCalculator.calculate(transactions, month, zoneId),
        )
    }.flowOn(Dispatchers.Default)

    fun observePreferences(): Flow<FinancePreferences> = settingsDao.observeAll().map { rows ->
        val values = rows.associate { it.key to it.value }
        FinancePreferences(
            onboardingComplete = values[KEY_ONBOARDING] == "true",
            smsDisclosureAccepted = values[KEY_SMS_CONSENT] == "true",
            smsTrackingEnabled = values[KEY_SMS_TRACKING] == "true",
            notificationPermissionGranted = values[KEY_NOTIFICATIONS] == "true",
            lastSmsPermissionState = values[KEY_SMS_PERMISSION] ?: "not_requested",
        )
    }

    suspend fun savePreference(key: String, value: String) = settingsDao.put(AppSettingEntity(key, value))

    fun defaultCategoryFor(merchant: String, note: String?, kind: TransactionKind): String =
        categorizer.categorize(merchant, note, kind)

    suspend fun smsImportEnabled(): Boolean = settingsDao.get(KEY_SMS_CONSENT) == "true" && settingsDao.get(KEY_SMS_TRACKING) == "true"

    suspend fun saveManualTransaction(draft: TransactionDraft): String {
        require(draft.amountPaise > 0L)
        val categoryId = draft.categoryId.ifBlank { categorizer.categorize(draft.merchant, draft.note, draft.kind) }
        val id = UUID.randomUUID().toString()
        transactionDao.upsert(
            FinanceTransaction(
                id = id,
                amountPaise = draft.amountPaise,
                kind = draft.kind,
                status = TransactionStatus.CONFIRMED,
                merchant = draft.merchant.trim(),
                note = draft.note?.trim()?.takeIf(String::isNotEmpty),
                categoryId = categoryId,
                occurredAtEpochMillis = draft.occurredAtEpochMillis,
                source = TransactionSource.MANUAL,
                reversesKind = draft.reversesKind,
            ).toEntity(),
        )
        return id
    }

    suspend fun updateTransaction(id: String, draft: TransactionDraft) {
        val existing = transactionDao.findById(id) ?: throw IllegalArgumentException("Transaction no longer exists.")
        transactionDao.upsert(
            existing.copy(
                amountPaise = MoneyPaise(draft.amountPaise),
                kind = draft.kind,
                merchant = SensitiveText(draft.merchant.trim()),
                note = draft.note?.trim()?.takeIf(String::isNotEmpty)?.let(::SensitiveText),
                categoryId = draft.categoryId,
                occurrenceTime = SensitiveLong(draft.occurredAtEpochMillis),
                reversesKind = draft.reversesKind,
                status = TransactionStatus.CONFIRMED,
                isCategoryUserEdited = true,
            ),
        )
    }

    suspend fun confirmCandidate(id: String, merchant: String, amountPaise: Long, kind: TransactionKind, categoryId: String) {
        require(amountPaise > 0L)
        val candidate = transactionDao.findById(id) ?: throw IllegalArgumentException("Candidate no longer exists.")
        require(candidate.status == TransactionStatus.PENDING_REVIEW) { "This candidate was already reviewed." }
        transactionDao.upsert(
            candidate.copy(
                amountPaise = MoneyPaise(amountPaise),
                merchant = SensitiveText(merchant.trim()),
                kind = kind,
                categoryId = categoryId,
                status = TransactionStatus.CONFIRMED,
                isCategoryUserEdited = true,
            ),
        )
    }

    suspend fun setTransactionCategory(id: String, categoryId: String) {
        val transaction = transactionDao.findById(id) ?: return
        transactionDao.upsert(transaction.copy(categoryId = categoryId, isCategoryUserEdited = true))
    }

    suspend fun deleteTransaction(id: String) = transactionDao.deleteById(id)

    /** Stores only parsed fields and a keyed fingerprint. The SMS body is never passed to Room. */
    suspend fun storeSmsCandidate(
        candidate: FinanceTransaction,
        fingerprintInput: TransactionFingerprintInput,
        providerMessageDate: Long? = null,
        providerMessageId: Long? = null,
    ): Boolean {
        require(candidate.source == TransactionSource.SMS && candidate.status == TransactionStatus.PENDING_REVIEW)
        val fingerprint = duplicateDetector.fingerprint(fingerprintInput)
        return database.withTransaction {
            if (transactionDao.findBySmsDeduplicationKey(fingerprint) != null) {
                if (providerMessageDate != null && providerMessageId != null) {
                    checkpointDao.put(SmsImportCheckpointEntity("inbox", SensitiveLong(providerMessageDate), SensitiveLong(providerMessageId)))
                }
                false
            } else {
                transactionDao.upsert(candidate.copy(deduplicationKey = fingerprint).toEntity())
                if (providerMessageDate != null && providerMessageId != null) {
                    checkpointDao.put(SmsImportCheckpointEntity("inbox", SensitiveLong(providerMessageDate), SensitiveLong(providerMessageId)))
                }
                true
            }
        }
    }

    suspend fun advanceInboxCheckpoint(messageDate: Long, providerMessageId: Long) {
        checkpointDao.put(SmsImportCheckpointEntity("inbox", SensitiveLong(messageDate), SensitiveLong(providerMessageId)))
    }

    suspend fun inboxCheckpoint(): SmsImportCheckpointEntity? = checkpointDao.get("inbox")

    suspend fun saveRecurringPayment(draft: BillDraft, id: String? = null): String {
        require(draft.name.isNotBlank()) { "Enter a name for this payment." }
        require(draft.amountPaise == null || draft.amountPaise > 0L)
        require(draft.daysBeforeReminder in 0..30)
        if (draft.dueDateSource == DueDateSource.INFERRED_UNCONFIRMED) {
            require(draft.dueDateEpochMillis != null) { "An inferred due date must be shown for confirmation." }
        }
        val paymentId = id ?: UUID.randomUUID().toString()
        val existing = id?.let { paymentDao.findById(it) }
        paymentDao.upsert(
            RecurringPaymentEntity(
                id = paymentId,
                name = SensitiveText(draft.name.trim()),
                amountPaise = draft.amountPaise?.let(::MoneyPaise),
                categoryId = draft.categoryId,
                frequency = draft.frequency,
                dueDate = draft.dueDateEpochMillis?.let(::SensitiveLong),
                dueDateSource = draft.dueDateSource,
                daysBeforeReminder = draft.daysBeforeReminder,
                enabled = draft.dueDateEpochMillis != null && draft.dueDateSource != DueDateSource.INFERRED_UNCONFIRMED,
                createdAt = existing?.createdAt ?: SensitiveLong(System.currentTimeMillis()),
            ),
        )
        return paymentId
    }

    suspend fun setRecurringPaymentEnabled(id: String, enabled: Boolean) {
        val current = paymentDao.findById(id) ?: return
        paymentDao.upsert(current.copy(enabled = enabled && current.dueDate != null && current.dueDateSource != DueDateSource.INFERRED_UNCONFIRMED))
    }

    suspend fun confirmInferredDueDate(id: String, userConfirmedDateEpochMillis: Long) {
        val current = paymentDao.findById(id) ?: throw IllegalArgumentException("Payment no longer exists.")
        paymentDao.upsert(current.copy(dueDate = SensitiveLong(userConfirmedDateEpochMillis), dueDateSource = DueDateSource.USER_ENTERED, enabled = true))
    }

    suspend fun deleteRecurringPayment(id: String) = database.withTransaction {
        database.reminders().deleteForPayment(id)
        paymentDao.deleteById(id)
    }

    suspend fun exportCsv(): String {
        val transactions = transactionDao.observeAll().first()
            .map { it.toDomain() }
            .filter { it.status == TransactionStatus.CONFIRMED }
            .sortedBy { it.occurredAtEpochMillis }
        val payments = paymentDao.observeAll().first().map { it.toDomain() }
        val categories = categoryDao.observeAll().first().associate { it.id to it.name }
        return buildString {
            appendLine("record_type,id,date,description,amount_paise,transaction_type,category,source,due_date,frequency")
            transactions.forEach { tx ->
                appendLine(listOf("transaction", tx.id, Instant.ofEpochMilli(tx.occurredAtEpochMillis).toString(), tx.merchant, tx.amountPaise.toString(), tx.kind.name, categories[tx.categoryId].orEmpty(), tx.source.name, "", "").csvRow())
            }
            payments.forEach { payment ->
                appendLine(listOf("bill", payment.id, "", payment.name, payment.amountPaise?.toString().orEmpty(), "", categories[payment.categoryId].orEmpty(), "", payment.dueDateEpochMillis?.let(Instant::ofEpochMilli)?.toString().orEmpty(), payment.frequency.name).csvRow())
            }
        }
    }

    suspend fun deleteAllUserData() {
        database.withTransaction {
            transactionDao.deleteAll()
            paymentDao.deleteAll()
            database.reminders().deleteAll()
            categoryDao.deleteCustom()
            settingsDao.deleteAll()
            checkpointDao.deleteAll()
        }
        // Delete keys only after the database transaction commits; failure must not strand encrypted rows.
        KeystoreCipher.deleteKeys()
    }

    private fun FinanceTransaction.toEntity() = TransactionEntity(
        id = id,
        amountPaise = MoneyPaise(amountPaise),
        kind = kind,
        status = status,
        merchant = SensitiveText(merchant),
        note = note?.let(::SensitiveText),
        categoryId = categoryId,
        occurrenceTime = SensitiveLong(occurredAtEpochMillis),
        source = source,
        smsDeduplicationKey = deduplicationKey,
        referenceFingerprint = referenceFingerprint,
        reversesKind = reversesKind,
        isCategoryUserEdited = isCategoryUserEdited,
        insertedAt = SensitiveLong(System.currentTimeMillis()),
    )

    private fun TransactionEntity.toDomain() = FinanceTransaction(
        id = id,
        amountPaise = amountPaise.value,
        kind = kind,
        status = status,
        merchant = merchant.value,
        note = note?.value,
        categoryId = categoryId,
        occurredAtEpochMillis = occurrenceTime.value,
        source = source,
        deduplicationKey = smsDeduplicationKey,
        referenceFingerprint = referenceFingerprint,
        reversesKind = reversesKind,
        isCategoryUserEdited = isCategoryUserEdited,
    )

    private fun CategoryEntity.toDomain() = FinanceCategory(id, name, iconKey, colorKey, isDefault)

    private fun RecurringPaymentEntity.toDomain() = RecurringPayment(
        id = id,
        name = name.value,
        amountPaise = amountPaise?.value,
        categoryId = categoryId,
        frequency = frequency,
        dueDateEpochMillis = dueDate?.value,
        dueDateSource = dueDateSource,
        daysBeforeReminder = daysBeforeReminder,
        enabled = enabled,
        createdAtEpochMillis = createdAt.value,
    )

    private fun List<String>.csvRow(): String = joinToString(",") { value -> "\"${value.replace("\"", "\"\"")}\"" }

    companion object {
        const val KEY_ONBOARDING = "onboarding_complete"
        const val KEY_SMS_CONSENT = "sms_disclosure_accepted"
        const val KEY_SMS_TRACKING = "sms_tracking_enabled"
        const val KEY_NOTIFICATIONS = "notification_permission_granted"
        const val KEY_SMS_PERMISSION = "sms_permission_state"

        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(CategoryIds.FOOD, "Food & dining", "restaurant", "teal", true),
            CategoryEntity(CategoryIds.GROCERIES, "Groceries", "basket", "mint", true),
            CategoryEntity(CategoryIds.TRANSPORT, "Transport", "commute", "blue", true),
            CategoryEntity(CategoryIds.BILLS, "Bills & utilities", "bolt", "amber", true),
            CategoryEntity(CategoryIds.SHOPPING, "Shopping", "shopping", "purple", true),
            CategoryEntity(CategoryIds.HEALTH, "Health", "health", "coral", true),
            CategoryEntity(CategoryIds.INCOME, "Income", "income", "green", true),
            CategoryEntity(CategoryIds.TRANSFERS, "Own-account transfer", "transfer", "gray", true),
            CategoryEntity(CategoryIds.CARD_REPAYMENTS, "Card repayment", "card", "gray", true),
            CategoryEntity(CategoryIds.OTHER, "Other", "category", "gray", true),
        )
    }
}
