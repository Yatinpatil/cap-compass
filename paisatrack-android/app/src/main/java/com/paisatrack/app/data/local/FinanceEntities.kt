package com.paisatrack.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.paisatrack.app.data.privacy.KeystoreCipher
import com.paisatrack.app.domain.model.BillingFrequency
import com.paisatrack.app.domain.model.DueDateSource
import com.paisatrack.app.domain.model.ReminderStatus
import com.paisatrack.app.domain.model.TransactionKind
import com.paisatrack.app.domain.model.TransactionSource
import com.paisatrack.app.domain.model.TransactionStatus

data class MoneyPaise(val value: Long) {
    init { require(value > 0L) }
}

data class SensitiveText(val value: String)
data class SensitiveLong(val value: Long)

class FinanceConverters {
    @TypeConverter fun moneyToStorage(value: MoneyPaise): String = KeystoreCipher.encrypt(value.value.toString())
    @TypeConverter fun moneyFromStorage(value: String): MoneyPaise = MoneyPaise(KeystoreCipher.decrypt(value).toLong())
    @TypeConverter fun textToStorage(value: SensitiveText): String = KeystoreCipher.encrypt(value.value)
    @TypeConverter fun textFromStorage(value: String): SensitiveText = SensitiveText(KeystoreCipher.decrypt(value))
    @TypeConverter fun longToStorage(value: SensitiveLong): String = KeystoreCipher.encrypt(value.value.toString())
    @TypeConverter fun longFromStorage(value: String): SensitiveLong = SensitiveLong(KeystoreCipher.decrypt(value).toLong())

    @TypeConverter fun kindToStorage(value: TransactionKind): String = value.name
    @TypeConverter fun kindFromStorage(value: String): TransactionKind = TransactionKind.valueOf(value)
    @TypeConverter fun statusToStorage(value: TransactionStatus): String = value.name
    @TypeConverter fun statusFromStorage(value: String): TransactionStatus = TransactionStatus.valueOf(value)
    @TypeConverter fun sourceToStorage(value: TransactionSource): String = value.name
    @TypeConverter fun sourceFromStorage(value: String): TransactionSource = TransactionSource.valueOf(value)
    @TypeConverter fun frequencyToStorage(value: BillingFrequency): String = value.name
    @TypeConverter fun frequencyFromStorage(value: String): BillingFrequency = BillingFrequency.valueOf(value)
    @TypeConverter fun dueSourceToStorage(value: DueDateSource): String = value.name
    @TypeConverter fun dueSourceFromStorage(value: String): DueDateSource = DueDateSource.valueOf(value)
    @TypeConverter fun reminderStatusToStorage(value: ReminderStatus): String = value.name
    @TypeConverter fun reminderStatusFromStorage(value: String): ReminderStatus = ReminderStatus.valueOf(value)
}

@Entity(
    tableName = "finance_transactions",
    indices = [Index(value = ["status", "occurrenceTime"]), Index(value = ["categoryId"]), Index(value = ["smsDeduplicationKey"], unique = true)],
)
@TypeConverters(FinanceConverters::class)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amountPaise: MoneyPaise,
    val kind: TransactionKind,
    val status: TransactionStatus,
    val merchant: SensitiveText,
    val note: SensitiveText?,
    val categoryId: String,
    val occurrenceTime: SensitiveLong,
    val source: TransactionSource,
    val smsDeduplicationKey: String?,
    val referenceFingerprint: String?,
    val reversesKind: TransactionKind?,
    val isCategoryUserEdited: Boolean,
    val insertedAt: SensitiveLong,
)

@Entity(tableName = "finance_categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconKey: String,
    val colorKey: String,
    val isDefault: Boolean,
)

@Entity(tableName = "recurring_payments", indices = [Index(value = ["enabled"])])
@TypeConverters(FinanceConverters::class)
data class RecurringPaymentEntity(
    @PrimaryKey val id: String,
    val name: SensitiveText,
    val amountPaise: MoneyPaise?,
    val categoryId: String,
    val frequency: BillingFrequency,
    val dueDate: SensitiveLong?,
    val dueDateSource: DueDateSource,
    val daysBeforeReminder: Int,
    val enabled: Boolean,
    val createdAt: SensitiveLong,
)

@Entity(tableName = "payment_reminders", indices = [Index(value = ["recurringPaymentId"]), Index(value = ["status"])])
@TypeConverters(FinanceConverters::class)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val recurringPaymentId: String,
    val scheduledAt: SensitiveLong,
    val status: ReminderStatus,
    val workName: String,
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(@PrimaryKey val key: String, val value: String)

@Entity(tableName = "sms_import_checkpoints")
@TypeConverters(FinanceConverters::class)
data class SmsImportCheckpointEntity(
    @PrimaryKey val source: String,
    val lastMessageDate: SensitiveLong,
    val lastProviderId: SensitiveLong,
)
