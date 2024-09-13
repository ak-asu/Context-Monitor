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
import kotlinx.coroutines.launch

class RespiratoryRateModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val sensorManager: SensorManager =
        application.getSystemService(Application.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val accelerometerValues = mutableStateListOf<FloatArray>() // Stores the accelerometer values
    val isCollecting = mutableStateOf(false) // Tracks if data is being collected

    // Starts capturing accelerometer data
    fun startAccelerometerDataCapture() {
        isCollecting.value = true
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        viewModelScope.launch(Dispatchers.IO) {
            // Stop after 45 seconds
            kotlinx.coroutines.delay(45_000)
            stopAccelerometerDataCapture()
        }
    }

    // Stops capturing accelerometer data
    private fun stopAccelerometerDataCapture() {
        isCollecting.value = false
        sensorManager.unregisterListener(this)
    }

    // Starts capturing accelerometer data
    fun clearValues() {
        accelerometerValues.clear()
    }

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
