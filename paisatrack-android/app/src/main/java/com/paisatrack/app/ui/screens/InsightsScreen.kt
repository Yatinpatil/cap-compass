package com.paisatrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paisatrack.app.ui.components.EmptyState
import com.paisatrack.app.ui.components.PaisaCard
import com.paisatrack.app.ui.components.PreviewDataLabel
import com.paisatrack.app.ui.components.ScreenHorizontalPadding
import com.paisatrack.app.ui.components.SectionTitle
import com.paisatrack.app.ui.model.CategorySpendUiModel
import com.paisatrack.app.ui.theme.Forest
import com.paisatrack.app.ui.theme.Mint
import com.paisatrack.app.ui.theme.SecondaryText
import com.paisatrack.app.ui.theme.SoftTeal
import com.paisatrack.app.ui.theme.Teal
import com.paisatrack.app.domain.money.MoneyParser

@Composable
fun InsightsScreen(
    categories: List<CategorySpendUiModel>,
    monthlyTotals: List<Pair<String, Long>> = emptyList(),
    demoMode: Boolean = false,
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = ScreenHorizontalPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Column {
            if (demoMode) PreviewDataLabel()
            Text("Patterns, made clear", color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
            Text("Spending with context", color = Forest, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        if (categories.isEmpty() && monthlyTotals.isEmpty()) {
            PaisaCard {
                EmptyState(
                    title = "Your insights are taking shape",
                    message = "Add a few transactions to see spending by category and how each month compares.",
                    icon = Icons.Outlined.AutoGraph,
                )
            }
            PaisaCard {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("Monthly overview")
                    Spacer(Modifier.height(8.dp))
                    EmptyState("No monthly activity yet", "There is not enough activity to compare months.", icon = Icons.Outlined.CalendarMonth)
                }
            }
        } else {
            PaisaCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionTitle("Spending by category")
                    categories.forEach { CategoryBar(it) }
                }
            }
            PaisaCard {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("Monthly expenses")
                    Spacer(Modifier.height(14.dp))
                    MonthlyBars(monthlyTotals)
                }
            }
            Text("Insights use transactions saved on this device.", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CategoryBar(category: CategorySpendUiModel) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(category.name, color = Forest, style = MaterialTheme.typography.bodyMedium)
            Text(category.amount, color = Forest, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(7.dp))
        androidx.compose.material3.LinearProgressIndicator(
            progress = { category.sharePercent.coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth().height(7.dp).semantics { contentDescription = "${category.sharePercent}% of spending" },
            color = if (category.colorIndex % 2 == 0) Teal else Forest,
            trackColor = SoftTeal,
        )
    }
}

@Composable
private fun MonthlyBars(values: List<Pair<String, Long>>) {
    if (values.isEmpty()) return
    val peak = values.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
    Row(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { index, (month, amount) ->
            Column(
                modifier = Modifier.weight(1f).semantics { contentDescription = "$month: ${MoneyParser.formatPaise(amount)}" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(MoneyParser.formatPaise(amount), color = SecondaryText, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Spacer(Modifier.height(5.dp))
                Spacer(
                    Modifier
                        .fillMaxWidth(0.48f)
                        .height((90f * amount.coerceIn(0L, peak).toFloat() / peak.toFloat()).coerceAtLeast(7f).dp)
                        .background(if (index == values.lastIndex) Forest else Mint, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                )
                Spacer(Modifier.height(7.dp))
                Text(month, color = SecondaryText, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
