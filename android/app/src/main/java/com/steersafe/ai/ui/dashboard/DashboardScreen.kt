package com.steersafe.ai.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steersafe.ai.ai.RiskLevel
import com.steersafe.ai.data.sensor.DrivingTelemetry

@Composable
fun DashboardScreen(
    telemetryFlow: kotlinx.coroutines.flow.StateFlow<DrivingTelemetry>,
    riskScore: Int,
    riskLevel: RiskLevel,
    isDriving: Boolean,
    onStartDrive: () -> Unit,
    onEndDrive: () -> Unit
) {
    val telemetry by telemetryFlow.collectAsState()

    // Dashboard background matching dark mode, HUD visuals
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        // Subtle blur glow elements in top-right
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-50).dp)
                .blur(80.dp)
                .background(
                    when (riskLevel) {
                        RiskLevel.SAFE -> Color(0x1A10B981)
                        RiskLevel.MODERATE -> Color(0x1AF59E0B)
                        RiskLevel.HIGH_RISK -> Color(0x1AEF4444)
                    }
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "SteerSafe AI",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isDriving) "DRIVING TELEMETRY ACTIVE" else "DASHBOARD STANDBY",
                    color = if (isDriving) Color(0xFF10B981) else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Risk Gauge Card
            RiskGaugeCard(score = riskScore, level = riskLevel)

            Spacer(modifier = Modifier.height(16.dp))

            // Grid displaying real-time parameters (Speeds & Violations)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TelemetryCard(title = "Speed", value = "${telemetry.currentSpeed.toInt()} km/h", modifier = Modifier.weight(1f))
                    TelemetryCard(title = "Max Speed", value = "${telemetry.maxSpeed.toInt()} km/h", modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TelemetryCard(title = "Distance", value = String.format("%.2f km", telemetry.distanceTravelled), modifier = Modifier.weight(1f))
                    TelemetryCard(
                        title = "Duration",
                        value = "${telemetry.drivingDuration / 60}m ${telemetry.drivingDuration % 60}s",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Safety Event Counters Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x0FFFFFFF))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    EventCounter(label = "Harsh Brakes", count = telemetry.harshBraking, color = Color(0xFFEF4444))
                    EventCounter(label = "Sharp Turns", count = telemetry.sharpTurns, color = Color(0xFFF59E0B))
                    EventCounter(label = "Overspeeds", count = telemetry.overspeedEvents, color = Color(0xFFFF9800))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Control Button (START / END DRIVE)
            Button(
                onClick = { if (isDriving) onEndDrive() else onStartDrive() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDriving) Color(0xFFEF4444) else Color(0xFF6366F1)
                )
            ) {
                Text(
                    text = if (isDriving) "END DRIVE" else "START DRIVE",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RiskGaugeCard(score: Int, level: RiskLevel) {
    val animatedScore by animateFloatAsState(targetValue = score.toFloat(), animationSpec = tween(1000))
    val gaugeColor by animateColorAsState(
        targetValue = when (level) {
            RiskLevel.SAFE -> Color(0xFF10B981)
            RiskLevel.MODERATE -> Color(0xFFF59E0B)
            RiskLevel.HIGH_RISK -> Color(0xFFEF4444)
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x12FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            // Draw background arc
            drawArc(
                color = Color(0x1AFFFFFF),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
            // Draw active risk score arc
            drawArc(
                color = gaugeColor,
                startAngle = 135f,
                sweepAngle = (animatedScore / 100f) * 270f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = level.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = gaugeColor
            )
            Text(
                text = "AI RISK RATING",
                fontSize = 9.sp,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TelemetryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x0BFFFFFF))
            .border(1.dp, Color(0x12FFFFFF), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(text = title, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EventCounter(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "$count", color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
