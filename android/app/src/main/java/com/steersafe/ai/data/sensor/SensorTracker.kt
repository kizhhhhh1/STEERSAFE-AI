package com.steersafe.ai.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.sqrt

data class DrivingTelemetry(
    val currentSpeed: Float = 0f,
    val avgSpeed: Float = 0f,
    val maxSpeed: Float = 0f,
    val distanceTravelled: Float = 0f, // in km
    val drivingDuration: Long = 0, // in seconds
    val suddenAccelerations: Int = 0,
    val harshBraking: Int = 0,
    val sharpTurns: Int = 0,
    val overspeedEvents: Int = 0
)

class SensorTracker(private val context: Context) : SensorEventListener, LocationListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _telemetry = MutableStateFlow(DrivingTelemetry())
    val telemetry: StateFlow<DrivingTelemetry> = _telemetry

    // Tracking states
    private var speedSum = 0f
    private var speedCount = 0
    private var lastLocation: Location? = null
    private var startTime: Long = 0

    // Threshold constants for safety event detection
    private val HARSH_ACCEL_THRESHOLD = 3.0f // m/s^2 change
    private val HARSH_BRAKE_THRESHOLD = -3.0f // m/s^2 change
    private val SHARP_TURN_THRESHOLD = 2.5f // rad/s rotation
    private val SPEED_LIMIT = 22.2f // ~80 km/h in m/s

    fun startTracking() {
        startTime = System.currentTimeMillis()
        speedSum = 0f
        speedCount = 0
        lastLocation = null

        _telemetry.value = DrivingTelemetry()

        // Register sensors
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Register GPS updates
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L, // 1 second interval
                1f,    // 1 meter changes
                this
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun stopTracking(): DrivingTelemetry {
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)
        return _telemetry.value
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val current = _telemetry.value

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]
                
                // Exclude gravity from acceleration change estimation (simplified linear acceleration)
                val magnitude = sqrt(ax*ax + ay*ay + az*az) - 9.8f

                if (magnitude > HARSH_ACCEL_THRESHOLD) {
                    _telemetry.value = current.copy(
                        suddenAccelerations = current.suddenAccelerations + 1
                    )
                } else if (magnitude < HARSH_BRAKE_THRESHOLD) {
                    _telemetry.value = current.copy(
                        harshBraking = current.harshBraking + 1
                    )
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                val gz = event.values[2] // Z axis rotation rate (yaw/turn rate)
                if (abs(gz) > SHARP_TURN_THRESHOLD) {
                    _telemetry.value = current.copy(
                        sharpTurns = current.sharpTurns + 1
                    )
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // GPS Location listener callbacks
    override fun onLocationChanged(location: Location) {
        val current = _telemetry.value
        val speedMps = location.speed // speed in meters per second
        val speedKmh = speedMps * 3.6f

        // Calculate distance incremental change
        var incrementalDistance = 0f
        lastLocation?.let {
            incrementalDistance = it.distanceTo(location) / 1000f // meters to km
        }
        lastLocation = location

        // Update speeds
        speedSum += speedKmh
        speedCount++
        val newAvgSpeed = speedSum / speedCount
        val newMaxSpeed = maxOf(current.maxSpeed, speedKmh)

        // Verify overspeeding
        val overspeedIncrement = if (speedMps > SPEED_LIMIT) 1 else 0

        val durationSec = (System.currentTimeMillis() - startTime) / 1000

        _telemetry.value = current.copy(
            currentSpeed = speedKmh,
            avgSpeed = newAvgSpeed,
            maxSpeed = newMaxSpeed,
            distanceTravelled = current.distanceTravelled + incrementalDistance,
            drivingDuration = durationSec,
            overspeedEvents = current.overspeedEvents + overspeedIncrement
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
