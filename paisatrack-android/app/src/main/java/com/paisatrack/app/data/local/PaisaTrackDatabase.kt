package com.paisatrack.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        RecurringPaymentEntity::class,
        ReminderEntity::class,
        AppSettingEntity::class,
        SmsImportCheckpointEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(FinanceConverters::class)
abstract class PaisaTrackDatabase : RoomDatabase() {
    abstract fun transactions(): TransactionDao
    abstract fun categories(): CategoryDao
    abstract fun recurringPayments(): RecurringPaymentDao
    abstract fun reminders(): ReminderDao
    abstract fun settings(): AppSettingDao
    abstract fun smsCheckpoints(): SmsImportCheckpointDao

    companion object {
        fun create(context: Context): PaisaTrackDatabase = Room.databaseBuilder(
            context.applicationContext,
            PaisaTrackDatabase::class.java,
            "paisa_track.db",
        ).build()
    }
}
