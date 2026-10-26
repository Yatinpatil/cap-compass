package com.paisatrack.app

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.viewModels
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.paisatrack.app.PaisaTrackApplication
import com.paisatrack.app.BuildConfig
import com.paisatrack.app.navigation.PaisaTrackApp
import com.paisatrack.app.ui.theme.PaisaTrackTheme
import com.paisatrack.app.ui.viewmodel.PaisaTrackViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: PaisaTrackViewModel by viewModels {
        (application as PaisaTrackApplication).container.viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaisaTrackTheme {
                PaisaTrackApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val smsRead = BuildConfig.SMS_IMPORT_AVAILABLE && checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val smsReceive = BuildConfig.SMS_IMPORT_AVAILABLE && checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val notifications = android.os.Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        viewModel.refreshSystemPermissions(smsRead, smsReceive, notifications)
    }
}
