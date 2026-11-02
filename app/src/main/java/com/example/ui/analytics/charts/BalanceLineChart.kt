package com.example.ui.analytics.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MonthlyTrend
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentIndigo

@Composable
fun BalanceLineChart(
    trends: List<MonthlyTrend>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (trends.isEmpty()) return

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(trends) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(1200))
    }

    val balances = trends.map { it.balance }
    val minBal = balances.minOrNull() ?: 0.0
    val maxBal = balances.maxOrNull()?.coerceAtLeast(100.0) ?: 100.0
    val range = (maxBal - minBal).coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val width = size.width
            val height = size.height - 24.dp.toPx()
            val stepX = width / (trends.size - 1).coerceAtLeast(1)

            val points = trends.mapIndexed { i, trend ->
                val x = i * stepX
                val normalizedY = ((trend.balance - minBal) / range).toFloat()
                val y = height - (normalizedY * height * animatedProgress.value)
                Offset(x, y)
            }

            if (points.isNotEmpty()) {
                val strokePath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2, p1.y)
                        val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2, p2.y)
                        cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p2.x, p2.y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(points.last().x, height)
                    lineTo(points.first().x, height)
                    close()
                }

                // Draw gradient fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AccentIndigo.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )

                // Draw curve line
                drawPath(
                    path = strokePath,
                    color = AccentCyan,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Draw data points
                points.forEach { point ->
                    drawCircle(
                        color = AccentIndigo,
                        radius = 5.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = AccentCyan,
                        radius = 3.dp.toPx(),
                        center = point
                    )
                }
            }
        }

        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
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
