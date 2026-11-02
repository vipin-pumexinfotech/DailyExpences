package com.example.ui.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyInfo
import com.example.ui.CategorySummary
import com.example.ui.MonthlyTrend
import com.example.ui.analytics.charts.BalanceLineChart
import com.example.ui.analytics.charts.DonutChart
import com.example.ui.analytics.charts.IncomeExpenseBarChart
import com.example.ui.util.CategoryIconHelper
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    timeframe: String,
    onTimeframeChange: (String) -> Unit,
    selectedMonth: Int,
    selectedYear: Int,
    onMonthYearChange: (month: Int, year: Int) -> Unit,
    onNavigateMonth: (delta: Int) -> Unit,
    onResetToCurrentMonth: () -> Unit,
    currency: CurrencyInfo,
    expenseBreakdown: List<CategorySummary>,
    monthlyTrends: List<MonthlyTrend>,
    modifier: Modifier = Modifier
) {
    var showMonthYearPicker by remember { mutableStateOf(false) }

    val fullMonthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val currentCalendar = remember { Calendar.getInstance() }
    val currentActualMonth = currentCalendar.get(Calendar.MONTH)
    val currentActualYear = currentCalendar.get(Calendar.YEAR)

    val isCurrentSelection = selectedMonth == currentActualMonth && selectedYear == currentActualYear

    if (showMonthYearPicker) {
        MonthYearPickerDialog(
            initialMonth = selectedMonth,
            initialYear = selectedYear,
            timeframe = timeframe,
            onDismiss = { showMonthYearPicker = false },
            onConfirm = { m, y ->
                onMonthYearChange(m, y)
                showMonthYearPicker = false
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Top Header
        item {
            Text(
                text = "Financial Visual Insights",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Timeframe Selector Tabs ("Monthly" / "Yearly")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                listOf("Monthly", "Yearly").forEach { tf ->
                    val isSelected = timeframe == tf
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onTimeframeChange(tf) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tf,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Month & Year Selector Navigation Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (timeframe == "Yearly") {
                                onMonthYearChange(selectedMonth, selectedYear - 1)
                            } else {
                                onNavigateMonth(-1)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Surface(
                        onClick = { showMonthYearPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select Month & Year",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (timeframe == "Yearly") "Year $selectedYear" else "${fullMonthNames[selectedMonth]} $selectedYear",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (timeframe == "Yearly") {
                                onMonthYearChange(selectedMonth, selectedYear + 1)
                            } else {
                                onNavigateMonth(1)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (!isCurrentSelection) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onResetToCurrentMonth,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset to Current Month", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Section 1: Expense by Category (Donut Chart)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Expense Breakdown by Category",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (timeframe == "Yearly") "$selectedYear" else "${fullMonthNames[selectedMonth].take(3)} $selectedYear",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    DonutChart(
                        breakdown = expenseBreakdown,
                        currencySymbol = currency.symbol
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Category Progress Bar List
        item {
            Text(
                text = "Top Spending Categories",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (expenseBreakdown.isEmpty()) {
            item {
                Text(
                    text = "No category data available for ${if (timeframe == "Yearly") "$selectedYear" else "${fullMonthNames[selectedMonth]} $selectedYear"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        } else {
            items(expenseBreakdown) { cat ->
                val color = CategoryIconHelper.parseColor(cat.colorHex)
                val icon = CategoryIconHelper.getIcon(cat.iconName)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = cat.categoryName,
                                        tint = color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = cat.categoryName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = "${currency.symbol}${String.format("%,.2f", cat.totalAmount)} (${String.format("%.1f", cat.percentage)}%)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { (cat.percentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // Section 2: Income vs Expense Comparison Chart
        item {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (timeframe == "Yearly") "Income vs Expense ($selectedYear)" else "Income vs Expense Trend",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    IncomeExpenseBarChart(
                        trends = monthlyTrends,
                        currencySymbol = currency.symbol
                    )
                }
            }
        }

        // Section 3: Net Balance Trend
        item {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Net Balance Trend Over Time",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    BalanceLineChart(
                        trends = monthlyTrends,
                        currencySymbol = currency.symbol
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MonthYearPickerDialog(
    initialMonth: Int,
    initialYear: Int,
    timeframe: String,
    onDismiss: () -> Unit,
    onConfirm: (month: Int, year: Int) -> Unit
) {
    var tempMonth by remember { mutableIntStateOf(initialMonth) }
    var tempYear by remember { mutableIntStateOf(initialYear) }

    val shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    val currentCal = remember { Calendar.getInstance() }
    val nowMonth = currentCal.get(Calendar.MONTH)
    val nowYear = currentCal.get(Calendar.YEAR)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Month & Year", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = {
                    tempMonth = nowMonth
                    tempYear = nowYear
                }) {
                    Icon(Icons.Default.Today, contentDescription = "Current Month", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Year Switcher Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { tempYear -= 1 }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Year")
                    }
                    Text(
                        text = "$tempYear",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { tempYear += 1 }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Year")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Month Grid Selection
                Text(
                    text = "Select Month",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 3x4 Grid of Months
                Column {
                    for (row in 0..3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0..2) {
                                val monthIndex = row * 3 + col
                                val isSelected = monthIndex == tempMonth
                                val isActualCurrent = monthIndex == nowMonth && tempYear == nowYear

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isActualCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    },
                                    border = if (isActualCurrent && !isSelected) {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    } else null,
                                    onClick = { tempMonth = monthIndex }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = shortMonths[monthIndex],
                                            fontWeight = if (isSelected || isActualCurrent) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = when {
                                                isSelected -> Color.White
                                                isActualCurrent -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tempMonth, tempYear) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Apply", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
