package com.paisatrack.app.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.paisatrack.app.ui.model.BillUiModel
import com.paisatrack.app.ui.model.CategorySpendUiModel
import com.paisatrack.app.ui.model.DashboardUiState
import com.paisatrack.app.ui.model.TransactionUiModel
import com.paisatrack.app.ui.screens.BillsScreen
import com.paisatrack.app.ui.screens.FeedbackKind
import com.paisatrack.app.ui.screens.FeedbackStateScreen
import com.paisatrack.app.ui.screens.HomeScreen
import com.paisatrack.app.ui.screens.InsightsScreen
import com.paisatrack.app.ui.screens.OnboardingScreen
import com.paisatrack.app.ui.screens.SettingsScreen
import com.paisatrack.app.ui.screens.TransactionDetailScreen
import com.paisatrack.app.ui.screens.TransactionsScreen
import com.paisatrack.app.ui.theme.PaisaTrackTheme
import com.paisatrack.app.domain.model.FinancePreferences

private val previewTransactions = listOf(
    TransactionUiModel("demo-1", "Cafe Coffee Day", "UPI · HDFC Bank", "−₹380", "380", false, "Food & dining", "Today", "UPI", "DEMO-001"),
    TransactionUiModel("demo-2", "Monthly salary", "NEFT · HDFC Bank", "+₹82,000", "82000", true, "Income", "1 Oct", "Bank transfer", "DEMO-002"),
    TransactionUiModel("demo-3", "Metro recharge", "UPI · SBI", "−₹600", "600", false, "Transport", "2 Oct", "UPI", "DEMO-003"),
)

private val previewCategories = listOf(
    CategorySpendUiModel("Home & bills", "₹12,400", 40, 0),
    CategorySpendUiModel("Food & dining", "₹7,820", 25, 1),
    CategorySpendUiModel("Transport", "₹4,650", 15, 2),
    CategorySpendUiModel("Shopping", "₹3,100", 10, 3),
)

private val previewBills = listOf(
    BillUiModel("demo-bill-1", "Electricity", "BESCOM · Monthly", "₹1,840", "12 Oct"),
    BillUiModel("demo-bill-2", "Music subscription", "StreamWave · Yearly", "₹1,299", "18 Oct"),
)

private val previewDashboard = DashboardUiState(
    monthlyIncome = "₹82,000",
    monthlyExpenses = "₹31,260",
    netCashFlow = "+₹50,740",
    categories = previewCategories,
    recentTransactions = previewTransactions,
    upcomingPayments = previewBills,
)

@Preview(showBackground = true, name = "Home · demo preview")
@Composable
private fun HomeDemoPreview() = PaisaTrackTheme { HomeScreen(previewDashboard, demoMode = true) }

@Preview(showBackground = true, name = "Transactions · demo preview")
@Composable
private fun TransactionsDemoPreview() = PaisaTrackTheme { TransactionsScreen(previewTransactions, demoMode = true) }

@Preview(showBackground = true, name = "Detail · demo preview")
@Composable
private fun DetailDemoPreview() = PaisaTrackTheme { TransactionDetailScreen(previewTransactions.first(), demoMode = true) }

@Preview(showBackground = true, name = "Bills · demo preview")
@Composable
private fun BillsDemoPreview() = PaisaTrackTheme { BillsScreen(previewBills, demoMode = true) }

@Preview(showBackground = true, name = "Insights · demo preview")
@Composable
private fun InsightsDemoPreview() = PaisaTrackTheme { InsightsScreen(previewCategories, listOf("Jul" to 4_800_00L, "Aug" to 6_800_00L, "Sep" to 5_500_00L, "Oct" to 4_200_00L), demoMode = true) }

@Preview(showBackground = true, name = "Privacy onboarding")
@Composable
private fun OnboardingPreview() = PaisaTrackTheme { OnboardingScreen(onContinue = {}, onPermissionInfo = {}) }

@Preview(showBackground = true, name = "Permission denied")
@Composable
private fun PermissionDeniedPreview() = PaisaTrackTheme { FeedbackStateScreen(FeedbackKind.PermissionDenied) }

@Preview(showBackground = true, name = "Loading state")
@Composable
private fun LoadingPreview() = PaisaTrackTheme { FeedbackStateScreen(FeedbackKind.Loading) }

@Preview(showBackground = true, name = "Error state")
@Composable
private fun ErrorPreview() = PaisaTrackTheme { FeedbackStateScreen(FeedbackKind.Error) }

@Preview(showBackground = true, name = "Settings")
@Composable
private fun SettingsPreview() = PaisaTrackTheme {
    SettingsScreen(
        preferences = FinancePreferences(),
        onPermissionStatus = {},
        onEnableSms = {},
        onDisableSms = {},
        onRequestNotifications = {},
        onPolicy = {},
        onErrorState = {},
        onExport = {},
        onDeleteAllData = {},
    )
}
