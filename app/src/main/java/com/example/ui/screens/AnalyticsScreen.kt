package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.EmotionEntity
import com.example.ui.viewmodel.EmotionViewModel

@Composable
fun AnalyticsScreen(
    viewModel: EmotionViewModel,
    modifier: Modifier = Modifier
) {
    val historyLogs by viewModel.historyLogs.collectAsState()

    // Calculate distributions based on logs
    val totalRecords = historyLogs.size
    val emotionCounts = remember(historyLogs) {
        val counts = mutableMapOf<String, Int>()
        historyLogs.forEach { log ->
            counts[log.primaryEmotion] = (counts[log.primaryEmotion] ?: 0) + 1
        }
        counts
    }

    val sortedEmotions = remember(emotionCounts) {
        emotionCounts.entries.sortedByDescending { it.value }
    }

    val primaryMood = remember(sortedEmotions) {
        sortedEmotions.firstOrNull()?.key ?: "Neutral"
    }

    val primaryMoodPercentage = remember(sortedEmotions, totalRecords) {
        if (totalRecords > 0) {
            val count = sortedEmotions.firstOrNull()?.value ?: 0
            (count * 100) / totalRecords
        } else {
            0
        }
    }

    val averageConfidence = remember(historyLogs) {
        if (historyLogs.isNotEmpty()) {
            historyLogs.map { it.confidence }.average()
        } else {
            0.0
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFFDF8F6)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column {
                Text(
                    text = "EMOTION METRICS CENTER",
                    color = Color(0xFF6750A4),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Mood Analytics",
                    color = Color(0xFF1C1B1F),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
            }

            if (historyLogs.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0x1F79747E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "No Data",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "AWAITING METRICS GENERATION",
                            color = Color(0xFF1C1B1F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Create historical entries via camera scan or gallery imports to view emotional curves.",
                            color = Color(0xFF49454F),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Circular Donut Distribution Chart
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0x1F79747E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "EMOTION WEIGHT MATRIX",
                            color = Color(0xFF49454F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Custom Donut Canvas
                        Box(
                            modifier = Modifier.size(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmotionDonutChart(emotionCounts = emotionCounts, total = totalRecords)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$totalRecords",
                                    color = Color(0xFF1C1B1F),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "TOTAL SCANS",
                                    color = Color(0xFF49454F),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Legend
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sortedEmotions.forEach { entry ->
                                val emotion = entry.key
                                val count = entry.value
                                val percent = (count * 100) / totalRecords

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(getEmotionColor(emotion), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = emotion,
                                            color = Color(0xFF1C1B1F),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = "$percent% ($count Scans)",
                                        color = getEmotionColor(emotion),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Core Statistics Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0x1F79747E)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "DOMINANT MOOD",
                                color = Color(0xFF49454F),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = primaryMood,
                                color = getEmotionColor(primaryMood),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Represents $primaryMoodPercentage% of total records.",
                                color = Color(0xFF49454F),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0x1F79747E)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "AVG CONFIDENCE",
                                color = Color(0xFF49454F),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format("%.1f", averageConfidence * 100)}%",
                                color = Color(0xFF6750A4),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Across all detection sessions.",
                                color = Color(0xFF49454F),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // AI Behavioral Analysis Report
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0x1F79747E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Behavior Analysis",
                                tint = Color(0xFF21005D),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI BEHAVIORAL PROFILE",
                                color = Color(0xFF21005D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        val report = when (primaryMood) {
                            "Happy" -> "Your mental trajectory is showing high cognitive stability. Dominant happiness registers active positive reinforcements and reduces physical stress markers. Maintain this wonderful momentum by journaling your joyful events!"
                            "Sad" -> "We've noticed a slightly higher percentage of Sad indicators. This is fully natural and highly useful to process. Focus on mild soothing activities today. Practice deep box-breathing exercises and prioritize comfortable environments."
                            "Angry" -> "Active stress, irritability, or frustration patterns detected. High-tension landmarks are registered around the brow orbits. Try a 5-minute desk break or step outside for a glass of cool water to clear focus."
                            "Neutral" -> "You are registering a highly centered, balanced, and serene emotional framework. Neutral configurations indicate high mental resilience, focus, and a superb baseline for deep studying or productive focus."
                            else -> "You exhibit a highly dynamic emotional variance. This represents rich expressive capabilities and high adaptability. Embrace your diverse expressions as part of a healthy, communicative mental landscape!"
                        }

                        Text(
                            text = report,
                            color = Color(0xFF1C1B1F),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmotionDonutChart(
    emotionCounts: Map<String, Int>,
    total: Int
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val sizeMin = size.minDimension
        val strokeWidth = 14.dp.toPx()
        val rectSize = sizeMin - strokeWidth

        val topLeft = Offset(
            x = (size.width - rectSize) / 2f,
            y = (size.height - rectSize) / 2f
        )
        val arcSize = Size(rectSize, rectSize)

        var startAngle = -90f

        if (total == 0) {
            drawArc(
                color = Color.Gray,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = arcSize
            )
        } else {
            emotionCounts.forEach { entry ->
                val emotion = entry.key
                val count = entry.value
                val sweepAngle = (count.toFloat() / total) * 360f

                drawArc(
                    color = getEmotionColor(emotion),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = topLeft,
                    size = arcSize
                )

                startAngle += sweepAngle
            }
        }
    }
}
