package com.example.gymtracker.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymtracker.data.MeasurementDataPoint

@Composable
fun MeasurementChart(
    dataPoints: List<MeasurementDataPoint>,
    title: String,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "No data available",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val primaryColor = MaterialTheme.colorScheme.primary
            val gridColor = Color.Gray.copy(alpha = 0.3f)
            // Chart container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val width = size.width
                    val height = size.height
                    val padding = 40f

                    // Find min and max values
                    val values = dataPoints.map { it.value }
                    val minValue = values.minOrNull() ?: 0f
                    val maxValue = values.maxOrNull() ?: 100f
                    val valueRange = maxValue - minValue

                    // Calculate scale factors
                    val xScale = (width - 2 * padding) / (dataPoints.size - 1).coerceAtLeast(1)

                    // Draw grid lines
                    val strokeWidth = 1f

                    // Horizontal grid lines
                    for (i in 0..4) {
                        val y = padding + (height - 2 * padding) * i / 4
                        drawLine(
                            color = gridColor,
                            start = Offset(padding, y),
                            end = Offset(width - padding, y),
                            strokeWidth = strokeWidth
                        )
                    }

                    // Vertical grid lines
                    for (i in dataPoints.indices) {
                        val x = padding + i * xScale
                        drawLine(
                            color = gridColor,
                            start = Offset(x, padding),
                            end = Offset(x, height - padding),
                            strokeWidth = strokeWidth
                        )
                    }

                    // Draw data line
                    if (dataPoints.size > 1) {
                        val path = Path()
                        var isFirst = true

                        dataPoints.forEachIndexed { index: Int, dataPoint: MeasurementDataPoint ->
                            val x = padding + index * xScale
                            val normalizedValue = (dataPoint.value - minValue) / valueRange.coerceAtLeast(1f)
                            val y = height - padding - normalizedValue * (height - 2 * padding)

                            if (isFirst) {
                                path.moveTo(x, y)
                                isFirst = false
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        // Draw the line
                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(width = 3f)
                        )

                        // Draw data points
                        dataPoints.forEachIndexed { index: Int, dataPoint: MeasurementDataPoint ->
                            val x = padding + index * xScale
                            val normalizedValue = (dataPoint.value - minValue) / valueRange.coerceAtLeast(1f)
                            val y = height - padding - normalizedValue * (height - 2 * padding)

                            drawCircle(
                                color = primaryColor,
                                radius = 4f,
                                center = Offset(x, y)
                            )
                        }
                    }
                }
            }
        }
    }
} 