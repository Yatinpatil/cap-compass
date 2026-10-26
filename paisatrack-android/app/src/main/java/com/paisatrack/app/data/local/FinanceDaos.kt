package com.paisatrack.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.paisatrack.app.domain.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM finance_transactions")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM finance_transactions WHERE status = :status")
    fun observeByStatus(status: TransactionStatus): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM finance_transactions WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TransactionEntity?

    @Query("SELECT * FROM finance_transactions WHERE smsDeduplicationKey = :key LIMIT 1")
    suspend fun findBySmsDeduplicationKey(key: String): TransactionEntity?

    @Upsert suspend fun upsert(transaction: TransactionEntity)

    @Query("DELETE FROM finance_transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM finance_transactions")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM finance_categories ORDER BY isDefault DESC, name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM finance_categories WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaults(categories: List<CategoryEntity>)

    @Upsert suspend fun upsert(category: CategoryEntity)

    @Query("DELETE FROM finance_categories WHERE isDefault = 0")
    suspend fun deleteCustom()
}

@Dao
interface RecurringPaymentDao {
    @Query("SELECT * FROM recurring_payments ORDER BY enabled DESC")
    fun observeAll(): Flow<List<RecurringPaymentEntity>>

    @Query("SELECT * FROM recurring_payments WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): RecurringPaymentEntity?

    @Upsert suspend fun upsert(payment: RecurringPaymentEntity)

    @Query("DELETE FROM recurring_payments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recurring_payments")
    suspend fun deleteAll()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM payment_reminders WHERE recurringPaymentId = :paymentId")
    fun observeForPayment(paymentId: String): Flow<List<ReminderEntity>>

    @Upsert suspend fun upsert(reminder: ReminderEntity)

    @Query("DELETE FROM payment_reminders WHERE recurringPaymentId = :paymentId")
    suspend fun deleteForPayment(paymentId: String)

    @Query("DELETE FROM payment_reminders")
    suspend fun deleteAll()
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings")
    fun observeAll(): Flow<List<AppSettingEntity>>

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Upsert suspend fun put(setting: AppSettingEntity)

    @Query("DELETE FROM app_settings")
    suspend fun deleteAll()
}

@Dao
interface SmsImportCheckpointDao {
    @Query("SELECT * FROM sms_import_checkpoints WHERE source = :source LIMIT 1")
    suspend fun get(source: String): SmsImportCheckpointEntity?

    @Upsert suspend fun put(checkpoint: SmsImportCheckpointEntity)

    @Query("DELETE FROM sms_import_checkpoints")
    suspend fun deleteAll()
}
