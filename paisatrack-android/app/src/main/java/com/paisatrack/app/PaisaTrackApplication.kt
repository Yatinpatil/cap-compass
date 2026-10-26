package com.paisatrack.app

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.paisatrack.app.data.local.PaisaTrackDatabase
import com.paisatrack.app.data.repository.FinanceRepository
import com.paisatrack.app.data.sms.SmsInboxImporter
import com.paisatrack.app.data.sms.SmsIngestionService
import com.paisatrack.app.domain.category.TransactionCategorizer
import com.paisatrack.app.ui.viewmodel.PaisaTrackViewModel
import com.paisatrack.app.work.ReminderScheduler

class PaisaTrackApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    val database = PaisaTrackDatabase.create(application)
    val financeRepository = FinanceRepository(database, TransactionCategorizer())
    val smsIngestion = SmsIngestionService(financeRepository)
    val smsInboxImporter = SmsInboxImporter(application, financeRepository, smsIngestion)
    val reminderScheduler = ReminderScheduler(application, database)
    val viewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PaisaTrackViewModel::class.java))
            return PaisaTrackViewModel(financeRepository, smsInboxImporter, reminderScheduler) as T
        }
    }
}
