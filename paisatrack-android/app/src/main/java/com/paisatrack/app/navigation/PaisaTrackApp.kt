package com.paisatrack.app.navigation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.paisatrack.app.BuildConfig
import com.paisatrack.app.domain.model.TransactionKind
import com.paisatrack.app.ui.screens.AddBillScreen
import com.paisatrack.app.ui.screens.AddTransactionScreen
import com.paisatrack.app.ui.screens.BillsScreen
import com.paisatrack.app.ui.screens.FeedbackKind
import com.paisatrack.app.ui.screens.FeedbackStateScreen
import com.paisatrack.app.ui.screens.HomeScreen
import com.paisatrack.app.ui.screens.InsightsScreen
import com.paisatrack.app.ui.screens.OnboardingScreen
import com.paisatrack.app.ui.screens.PrivacyPolicyScreen
import com.paisatrack.app.ui.screens.SettingsScreen
import com.paisatrack.app.ui.screens.TransactionDetailScreen
import com.paisatrack.app.ui.screens.TransactionsScreen
import com.paisatrack.app.ui.theme.AppBackground
import com.paisatrack.app.ui.theme.Forest
import com.paisatrack.app.ui.viewmodel.PaisaTrackViewModel

private data class TabDestination(val route: String, val label: String, val icon: ImageVector)

private val tabDestinations = listOf(
    TabDestination("home", "Home", Icons.Outlined.Home),
    TabDestination("transactions", "Transactions", Icons.AutoMirrored.Outlined.ReceiptLong),
    TabDestination("insights", "Insights", Icons.Outlined.Insights),
    TabDestination("bills", "Bills", Icons.Outlined.CalendarMonth),
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PaisaTrackApp(viewModel: PaisaTrackViewModel) {
    val navController = rememberNavController()
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: "onboarding"
    val isTopLevel = tabDestinations.any { it.route == route }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showSmsDisclosure by rememberSaveable { mutableStateOf(false) }
    var continueAfterSms by rememberSaveable { mutableStateOf(false) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val read = result[Manifest.permission.READ_SMS] == true
        val receive = result[Manifest.permission.RECEIVE_SMS] == true
        viewModel.updateSmsPermissionState(read, receive)
        if (continueAfterSms) {
            continueAfterSms = false
            viewModel.completeOnboarding()
            navController.navigate("home") { popUpTo("onboarding") { inclusive = true } }
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.updateNotificationPermission(granted)
    }

    if (showSmsDisclosure) {
        AlertDialog(
            onDismissRequest = { showSmsDisclosure = false; continueAfterSms = false },
            title = { Text("Optional SMS transaction tracking") },
            text = {
                Text(
                    "If you consent, PaisaTrack will use Android SMS access to inspect inbox messages and new alerts on this device for eligible bank transactions. " +
                        "The app filters messages locally, does not save SMS bodies or OTPs, and does not upload messages. Parsed items stay unconfirmed until you review them. " +
                        "You can turn tracking off or continue manually at any time. SMS permissions are restricted by Google Play and approval is not guaranteed.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSmsDisclosure = false
                    viewModel.acceptSmsDisclosureAndEnableTracking {
                        smsPermissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                    }
                }) { Text("I agree and continue") }
            },
            dismissButton = { TextButton(onClick = { showSmsDisclosure = false; continueAfterSms = false }) { Text("Not now") } },
        )
    }

    LaunchedEffect(ui.preferences.onboardingComplete, route) {
        if (ui.preferences.onboardingComplete && route == "onboarding") {
            navController.navigate("home") { popUpTo("onboarding") { inclusive = true } }
        }
    }
    LaunchedEffect(ui.errorMessage) {
        ui.errorMessage?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    val title = when (route) {
        "home" -> "PaisaTrack"
        "transactions" -> "Transactions"
        "insights" -> "Insights"
        "bills" -> "Bills & renewals"
        "settings" -> "Settings"
        "privacy" -> "Privacy overview"
        "addTransaction" -> "New transaction"
        "editTransaction/{id}" -> "Edit transaction"
        "addBill" -> "New bill"
        "transactionDetail/{id}" -> "Transaction details"
        "state/{kind}" -> when (backStackEntry?.arguments?.getString("kind")) {
            "permission" -> "Tracking status"
            "loading" -> "Loading"
            else -> "Something went wrong"
        }
        else -> "PaisaTrack"
    }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (route != "onboarding") {
                TopAppBar(
                    title = { Text(title, color = Forest, style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        if (!isTopLevel) IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Forest)
                        }
                    },
                    actions = {
                        if (isTopLevel) IconButton(onClick = { navController.navigate("settings") { launchSingleTop = true } }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Forest)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground),
                )
            }
        },
        bottomBar = {
            if (isTopLevel) NavigationBar(containerColor = androidx.compose.ui.graphics.Color.White) {
                tabDestinations.forEach { destination ->
                    val selected = backStackEntry?.destination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = "onboarding", modifier = Modifier.padding(innerPadding)) {
            composable("onboarding") {
                OnboardingScreen(
                    smsImportAvailable = BuildConfig.SMS_IMPORT_AVAILABLE,
                    onContinue = { viewModel.completeOnboarding { navController.navigate("home") { popUpTo("onboarding") { inclusive = true } } } },
                    onEnableSms = { continueAfterSms = true; showSmsDisclosure = true },
                    onPermissionInfo = { navController.navigate("state/permission") },
                )
            }
            composable("home") {
                HomeScreen(
                    state = ui.dashboard,
                    onAddTransaction = { navController.navigate("addTransaction") },
                    onTransactions = { navController.navigate("transactions") },
                    onInsights = { navController.navigate("insights") },
                    onBills = { navController.navigate("bills") },
                    onTransactionClick = { id -> navController.navigate("transactionDetail/${Uri.encode(id)}") },
                )
            }
            composable("transactions") {
                TransactionsScreen(
                    transactions = ui.transactions,
                    onAdd = { navController.navigate("addTransaction") },
                    onTransactionClick = { id -> navController.navigate("transactionDetail/${Uri.encode(id)}") },
                )
            }
            composable("insights") { InsightsScreen(categories = ui.dashboard.categories, monthlyTotals = ui.monthlyTotals) }
            composable("bills") {
                BillsScreen(
                    bills = ui.bills,
                    onAdd = { navController.navigate("addBill") },
                    onToggleReminder = viewModel::toggleBill,
                    onDelete = viewModel::deleteBill,
                    onConfirmDate = viewModel::confirmBillDate,
                )
            }
            composable("settings") {
                SettingsScreen(
                    preferences = ui.preferences,
                    onPermissionStatus = { navController.navigate("state/permission") },
                    onEnableSms = { continueAfterSms = false; showSmsDisclosure = true },
                    onDisableSms = viewModel::disableSmsTracking,
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else viewModel.updateNotificationPermission(true)
                    },
                    onPolicy = { navController.navigate("privacy") },
                    onErrorState = { navController.navigate("state/error") },
                    onExport = viewModel::exportCsv,
                    onDeleteAllData = { viewModel.deleteAllData { navController.navigate("onboarding") { popUpTo("home") { inclusive = true } } } },
                )
            }
            composable("privacy") { PrivacyPolicyScreen() }
            composable("addTransaction") {
                AddTransactionScreen(categories = ui.categories) { form ->
                    viewModel.addOrUpdateTransaction(form) { navController.popBackStack() }
                }
            }
            composable("addBill") {
                AddBillScreen(categories = ui.categories) { form ->
                    viewModel.addBill(form.name, form.amountRupees, form.categoryId, form.frequency, form.dueDateInput, form.daysBeforeReminder) {
                        navController.popBackStack()
                    }
                }
            }
            composable("transactionDetail/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("id")
                val transaction = ui.transactions.firstOrNull { it.id == id }
                TransactionDetailScreen(
                    transaction = transaction,
                    categories = ui.categories,
                    onCategoryChanged = { categoryId -> id?.let { viewModel.changeCategory(it, categoryId) } },
                    onEdit = { navController.navigate("editTransaction/${Uri.encode(id.orEmpty())}") },
                    onDelete = { id?.let { viewModel.deleteTransaction(it) { navController.popBackStack() } } },
                )
            }
            composable("editTransaction/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("id")
                AddTransactionScreen(
                    transaction = ui.transactions.firstOrNull { it.id == id },
                    categories = ui.categories,
                    onSave = { form -> id?.let { transactionId -> viewModel.addOrUpdateTransaction(form, transactionId) { navController.popBackStack() } } },
                )
            }
            composable("state/{kind}", arguments = listOf(navArgument("kind") { type = NavType.StringType })) { entry ->
                val permission = entry.arguments?.getString("kind") == "permission"
                FeedbackStateScreen(
                    kind = when {
                        permission -> FeedbackKind.PermissionDenied
                        entry.arguments?.getString("kind") == "loading" -> FeedbackKind.Loading
                        else -> FeedbackKind.Error
                    },
                    permissionAvailable = BuildConfig.SMS_IMPORT_AVAILABLE,
                    permissionStatus = ui.preferences.lastSmsPermissionState,
                    onRequestPermission = if (BuildConfig.SMS_IMPORT_AVAILABLE) {
                        { continueAfterSms = false; showSmsDisclosure = !ui.preferences.smsDisclosureAccepted; if (ui.preferences.smsDisclosureAccepted) smsPermissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)) }
                    } else null,
                    onOpenSystemSettings = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
                    },
                    onRetry = { navController.popBackStack() },
                    onBack = { navController.popBackStack("home", inclusive = false) },
                )
            }
        }
    }
}
