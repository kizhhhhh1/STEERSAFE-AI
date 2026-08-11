package com.steersafe.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.steersafe.ai.ai.RiskLevel
import com.steersafe.ai.data.sensor.DrivingTelemetry
import com.steersafe.ai.ui.dashboard.DashboardScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    // Simple mocked flow for demonstration
    private val _telemetryFlow = MutableStateFlow(
        DrivingTelemetry(0f, 0f, 0f, 0f, 0f, 0, 0, 0, 0, 0)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var isDriving by remember { mutableStateOf(false) }
            var riskScore by remember { mutableStateOf(0) }
            var riskLevel by remember { mutableStateOf(RiskLevel.SAFE) }

            // Mock telemetry updates while driving
            LaunchedEffect(isDriving) {
                if (isDriving) {
                    while (true) {
                        delay(1000)
                        val speed = (40..100).random().toFloat()
                        val newScore = (speed / 120 * 50).toInt()
                        riskScore = newScore
                        riskLevel = when {
                            newScore <= 30 -> RiskLevel.SAFE
                            newScore <= 65 -> RiskLevel.MODERATE
                            else -> RiskLevel.HIGH_RISK
                        }
                        _telemetryFlow.value = _telemetryFlow.value.copy(
                            currentSpeed = speed,
                            maxSpeed = maxOf(speed, _telemetryFlow.value.maxSpeed)
                        )
                    }
                } else {
                    riskScore = 0
                    riskLevel = RiskLevel.SAFE
                    _telemetryFlow.value = DrivingTelemetry(0f, 0f, 0f, 0f, 0f, 0, 0, 0, 0, 0)
                }
            }

            DashboardScreen(
                telemetryFlow = _telemetryFlow,
                riskScore = riskScore,
                riskLevel = riskLevel,
                isDriving = isDriving,
                onStartDrive = { isDriving = true },
                onEndDrive = { isDriving = false }
            )
        }
    }
}
