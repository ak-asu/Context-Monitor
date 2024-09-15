package com.akheparasu.contextmonitor.ui

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RespiratoryRateModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val sensorManager: SensorManager =
        application.getSystemService(Application.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val accelerometerValues = mutableStateListOf<FloatArray>() // Stores the accelerometer values
    val isCollecting = mutableStateOf(false)
    val progress = mutableStateOf(0)// Tracks if data is being collected

    init {
        if (accelerometer == null) {
            throw IllegalStateException("Accelerometer sensor not available on this device")
        }
    }

    // Starts capturing accelerometer data
    fun startAccelerometerDataCapture() {
        if (isCollecting.value) {
            stopAccelerometerDataCapture()
        }
        clearValues()
        progress.value = 0
        if (!isCollecting.value) {
            isCollecting.value = true
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            viewModelScope.launch(Dispatchers.IO) {
                while (progress.value < 5) {
                    delay(1000)
                    progress.value += 1
                }
                stopAccelerometerDataCapture()
            }
        }
    }

    // Stops capturing accelerometer data
    private fun stopAccelerometerDataCapture() {
        if (isCollecting.value) {
            isCollecting.value = false
            sensorManager.unregisterListener(this)
        }
    }

    // Clears the stored accelerometer values
    fun clearValues() {
        accelerometerValues.clear()
    }

    // Captures the sensor data when the sensor value changes
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                // Capture the x, y, z values and store in the list
                accelerometerValues.add(it.values.clone())
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for now
    }

    // Ensures proper cleanup of sensor listener when ViewModel is destroyed
    override fun onCleared() {
        super.onCleared()
        stopAccelerometerDataCapture()
    }
}

class RespiratoryRateModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RespiratoryRateModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RespiratoryRateModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
