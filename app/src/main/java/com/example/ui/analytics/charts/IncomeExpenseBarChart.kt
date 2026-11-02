package com.example.ui.analytics.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MonthlyTrend
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeEmerald

@Composable
fun IncomeExpenseBarChart(
    trends: List<MonthlyTrend>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (trends.isEmpty()) return

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(trends) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(1000))
    }

    val maxVal = trends.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(100.0) ?: 100.0

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(10.dp).background(IncomeEmerald, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Income", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier.size(10.dp).background(ExpenseCrimson, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Expense", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height - 30.dp.toPx() // Reserve space for x labels
            val groupCount = trends.size
            val groupWidth = canvasWidth / groupCount
            val barWidth = groupWidth * 0.28f

            trends.forEachIndexed { index, trend ->
                val groupCenterX = groupWidth * index + groupWidth / 2

                val incHeight = ((trend.income / maxVal) * canvasHeight * animatedProgress.value).toFloat()
                val expHeight = ((trend.expense / maxVal) * canvasHeight * animatedProgress.value).toFloat()

                val incX = groupCenterX - barWidth - 2.dp.toPx()
                val expX = groupCenterX + 2.dp.toPx()

                // Draw Income bar
                if (incHeight > 0) {
                    drawRoundRect(
                        color = IncomeEmerald,
                        topLeft = Offset(incX, canvasHeight - incHeight),
                        size = Size(barWidth, incHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }

                // Draw Expense bar
                if (expHeight > 0) {
                    drawRoundRect(
                        color = ExpenseCrimson,
                        topLeft = Offset(expX, canvasHeight - expHeight),
                        size = Size(barWidth, expHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        }

        // Month Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            trends.forEach { trend ->
                Text(
                    text = trend.monthName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
