package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.FairDispatchEngine
import com.example.data.engine.WageEngine
import com.example.data.repository.CoopRepository
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FairnessScreen(
    repository: CoopRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Run live comparison between Rating-Greedy and Equitable
    val (ratingGreedy, equitable) = remember {
        repository.runFairDispatchComparison()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Fair Dispatch Engine", fontWeight = FontWeight.Bold)
                        Text(
                            "Greedy vs Cooperative Equity Comparison",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        modifier = modifier.fillMaxSize().testTag("fairness_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Intro summary
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Balance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Algorithmic Justice in Gig Work",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rating-greedy algorithms create winner-take-all dynamics where high-rated workers starve others. Sahayog's Equitable Dispatch balances skill matching with progressive earnings distribution, ensuring baseline livelihoods for all members.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Key Metrics Side-by-Side
            item {
                Text(
                    text = "Side-by-Side Comparison Metrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Rating-Greedy Card
                    AlgorithmComparisonCard(
                        title = "Rating-Greedy (Corporate)",
                        subtitle = "Highest rating prioritized",
                        gini = ratingGreedy.giniCoefficient,
                        top20Share = ratingGreedy.top20SharePercent,
                        assignedCount = ratingGreedy.assignments.size,
                        dispatchedPaise = ratingGreedy.totalDispatchedPaise,
                        accentColor = Color(0xFFDC2626),
                        modifier = Modifier.weight(1f)
                    )

                    // Equitable Cooperative Card
                    AlgorithmComparisonCard(
                        title = "Sahayog Equitable",
                        subtitle = "Earnings-equalized dispatch",
                        gini = equitable.giniCoefficient,
                        top20Share = equitable.top20SharePercent,
                        assignedCount = equitable.assignments.size,
                        dispatchedPaise = equitable.totalDispatchedPaise,
                        accentColor = Color(0xFF059669),
                        isWinner = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Canvas Lorenz / Inequality Bar Comparison
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Top-20% Concentration of Wealth",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Share of dispatched wages captured by the top 20% of workers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Visual bar comparison
                        WealthConcentrationChart(
                            greedyTop20 = ratingGreedy.top20SharePercent / 100.0,
                            equitableTop20 = equitable.top20SharePercent / 100.0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFDC2626)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Greedy: ${String.format(Locale.US, "%.1f", ratingGreedy.top20SharePercent)}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF059669)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Equitable: ${String.format(Locale.US, "%.1f", equitable.top20SharePercent)}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Gini explanation card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mathematical Explanation", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val giniReduction = ((ratingGreedy.giniCoefficient - equitable.giniCoefficient) / ratingGreedy.giniCoefficient.coerceAtLeast(0.001) * 100).coerceAtLeast(0.0)
                        Text(
                            text = "• Gini coefficient dropped by ${String.format(Locale.US, "%.1f", giniReduction)}% (closer to 0.0 means near-perfect equality).\n" +
                                   "• Cooperative dispatch prioritizes qualified workers with fewer hours this week.\n" +
                                   "• Eliminates arbitrary rating penalties and protects senior & junior cooperative members alike.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlgorithmComparisonCard(
    title: String,
    subtitle: String,
    gini: Double,
    top20Share: Double,
    assignedCount: Int,
    dispatchedPaise: Long,
    accentColor: Color,
    isWinner: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(accentColor))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Gini Metric
            Text("Gini Inequality Index", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(Locale.US, "%.3f", gini),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                if (isWinner) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.TrendingDown, contentDescription = "Lower is better", tint = accentColor, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Top 20%
            Text("Top 20% Wage Share", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(
                text = "${String.format(Locale.US, "%.1f", top20Share * 100)}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Dispatched & Assigned
            Text("Assigned: $assignedCount jobs", style = MaterialTheme.typography.bodySmall)
            Text(
                text = "Total: ${WageEngine.formatPaiseCompact(dispatchedPaise)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun WealthConcentrationChart(
    greedyTop20: Double,
    equitableTop20: Double,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barHeight = 22.dp.toPx()
        val corner = 8.dp.toPx()
        val maxW = size.width

        // Greedy bar
        drawRoundRect(
            color = Color(0xFFF3F4F6),
            topLeft = Offset(0f, 10f),
            size = Size(maxW, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
        )
        drawRoundRect(
            color = Color(0xFFDC2626),
            topLeft = Offset(0f, 10f),
            size = Size((maxW * greedyTop20.toFloat()).coerceIn(20f, maxW), barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
        )

        // Equitable bar
        drawRoundRect(
            color = Color(0xFFF3F4F6),
            topLeft = Offset(0f, 55f),
            size = Size(maxW, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
        )
        drawRoundRect(
            color = Color(0xFF059669),
            topLeft = Offset(0f, 55f),
            size = Size((maxW * equitableTop20.toFloat()).coerceIn(20f, maxW), barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
        )
    }
}
