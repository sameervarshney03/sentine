package com.example.sentine.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sentine.data.db.RiskEventEntity
import com.example.sentine.ui.theme.SurfaceVariant
import com.example.sentine.ui.theme.RiskHigh
import com.example.sentine.ui.theme.RiskLow
import com.example.sentine.ui.theme.RiskMedium
import com.example.sentine.ui.theme.RiskSafe
import com.example.sentine.ui.theme.TextPrimary
import com.example.sentine.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AppHistoryGraph(
    events: List<RiskEventEntity>,
    onPointTapped: (Long) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayStart = todayCal.timeInMillis
    val dayMs = 24 * 60 * 60 * 1000L

    val days = (6 downTo 0).map { i -> todayStart - (i * dayMs) }
    
    val dailyScores = days.map { dayStart ->
        val dayEnd = dayStart + dayMs
        val dayEvents = events.filter { it.eventType == "DAILY_SCORE" && it.timestamp in dayStart until dayEnd }
        val maxScore = dayEvents.maxOfOrNull { it.eventDetail.toIntOrNull() ?: 0 }
        Pair(dayStart, maxScore)
    }

    if (dailyScores.all { it.second == null }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(SurfaceVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("App behavior history is collecting", color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Monitoring started today — check back tomorrow", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    val format = SimpleDateFormat("EEE", Locale.getDefault())
    val labels = days.map { format.format(it) }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    
    // Find the latest day with data and select it by default
    LaunchedEffect(dailyScores) {
        val lastIndex = dailyScores.indexOfLast { it.second != null }
        if (lastIndex != -1 && selectedIndex == null) {
            selectedIndex = lastIndex
            onPointTapped(dailyScores[lastIndex].first)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(SurfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Determine which point was tapped
                        val stepX = size.width / 6
                        val tappedIndex = (offset.x / stepX).toInt().coerceIn(0, 6)
                        if (dailyScores[tappedIndex].second != null) {
                            selectedIndex = tappedIndex
                            onPointTapped(dailyScores[tappedIndex].first)
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height - 30.dp.toPx() // Reserve space for labels
            val stepX = width / 6
            
            // Draw grid lines
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(0f, height * fraction),
                    end = Offset(width, height * fraction),
                    strokeWidth = 1f
                )
            }

            val path = Path()
            var firstPoint = true
            val points = mutableListOf<Offset>()

            dailyScores.forEachIndexed { i, (dayStart, score) ->
                val x = stepX * i
                
                // Draw X-axis label
                drawContext.canvas.nativeCanvas.drawText(
                    labels[i],
                    x,
                    size.height,
                    Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 34f
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                )

                if (score != null) {
                    // Map 0-100 to y position (inverted so 100 is top)
                    val y = height - (score / 100f * height)
                    val point = Offset(x, y)
                    points.add(point)
                    
                    if (firstPoint) {
                        path.moveTo(point.x, point.y)
                        firstPoint = false
                    } else {
                        // Smooth curve is better but straight lines requested by common graphs, let's use straight lines
                        path.lineTo(point.x, point.y)
                    }
                }
            }

            // Draw line
            if (points.isNotEmpty()) {
                // Determine gradient colors based on points using a vertical gradient
                val gradient = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(RiskHigh, RiskMedium, RiskLow, RiskSafe),
                    startY = 0f,
                    endY = height
                )
                
                drawPath(
                    path = path,
                    brush = gradient,
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Draw points
                points.forEachIndexed { i, point ->
                    // We only have points for non-null scores, need to find the actual index
                    val actualIndex = dailyScores.indexOfFirst { it.second != null && Math.abs((it.first - dailyScores.first().first) / dayMs).toInt() == 0 } // simpler way:
                    
                    val gradientColor = when {
                        point.y < height * 0.25f -> RiskHigh
                        point.y < height * 0.5f -> RiskMedium
                        point.y < height * 0.75f -> RiskLow
                        else -> RiskSafe
                    }

                    // For the tap UI
                    val dataIndex = (point.x / stepX).toInt()
                    val isSelected = dataIndex == selectedIndex

                    drawCircle(
                        color = SurfaceVariant,
                        radius = if (isSelected) 8.dp.toPx() else 5.dp.toPx(),
                        center = point
                    )
                    
                    drawCircle(
                        color = gradientColor,
                        radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                        center = point
                    )
                    
                    if (isSelected) {
                        // Draw score tooltip
                        val scoreStr = dailyScores[dataIndex].second.toString()
                        drawContext.canvas.nativeCanvas.drawText(
                            scoreStr,
                            point.x,
                            point.y - 16.dp.toPx(),
                            Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 40f
                                textAlign = Paint.Align.CENTER
                                isFakeBoldText = true
                                isAntiAlias = true
                            }
                        )
                    }
                }
            }
        }
    }
}
