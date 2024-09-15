package com.akheparasu.contextmonitor.ui

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akheparasu.contextmonitor.utils.MAX_PROGRESS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RespiratoryRateModel(application: Application) : AndroidViewModel(application),
    SensorEventListener {

    private val sensorManager: SensorManager =
        application.getSystemService(Application.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val accelerometerValues = mutableStateListOf<FloatArray>()
    val isCollecting = mutableStateOf(false)
    val progress = mutableIntStateOf(0)

    init {
        if (accelerometer == null) {
            Log.e("RespiratoryRateCapture", "Accelerometer sensor not available on this device")
            // throw IllegalStateException("Accelerometer sensor not available on this device")
        }
    }

    fun startAccelerometerDataCapture() {
        if (accelerometer == null) {
            Log.e("RespiratoryRateCapture", "Accelerometer sensor not available on this device")
            return
        }
        if (isCollecting.value) {
            stopAccelerometerDataCapture()
        }
        clearValues()
        progress.intValue = 0
        if (!isCollecting.value) {
            isCollecting.value = true
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            viewModelScope.launch(Dispatchers.IO) {
                while (progress.intValue < MAX_PROGRESS) {
                    delay(1000)
                    progress.intValue += 1
                }
                stopAccelerometerDataCapture()
            }
        }
    }

    private fun stopAccelerometerDataCapture() {
        if (isCollecting.value) {
            isCollecting.value = false
            sensorManager.unregisterListener(this)
        }
    }

    fun clearValues() {
        accelerometerValues.clear()
    }

    // Captures the sensor data when the sensor value changes
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                accelerometerValues.add(it.values.clone())
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    // Proper cleanup of resources when ViewModel is destroyed
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
