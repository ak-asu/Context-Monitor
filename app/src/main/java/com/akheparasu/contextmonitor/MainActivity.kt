package com.akheparasu.contextmonitor

import android.Manifest
import android.content.Context
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.akheparasu.contextmonitor.ui.theme.ContextMonitorTheme
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.akheparasu.contextmonitor.ui.AddSymptomsScreen
import com.akheparasu.contextmonitor.ui.SymptomAddModel
import com.akheparasu.contextmonitor.ui.SymptomViewModel
import com.akheparasu.contextmonitor.ui.ViewSymptomsScreen
import com.akheparasu.contextmonitor.utils.heartRateCalculator
import com.akheparasu.contextmonitor.utils.respiratoryRateCalculator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: MainViewModel by viewModels()
        val addSymptomModel: SymptomAddModel by viewModels()
        val viewSymptomModel: SymptomViewModel by viewModels()
        enableEdgeToEdge()
        setContent {
            ContextMonitorTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "first") {
                    composable("first") { ContextMonitorApp(navController, viewModel) }
                    composable("second/{heartRate}/{respiratoryRate}") { backStackEntry ->
                        val heartRate = backStackEntry.arguments?.getFloat("heartRate")
                        val respiratoryRate = backStackEntry.arguments?.getFloat("respiratoryRate")
                        if (heartRate != null && respiratoryRate != null) {
                            AddSymptomsScreen(
                                addSymptomModel,
                                heartRate,
                                respiratoryRate
                            )
                        }
                    }
                    composable("third") { ViewSymptomsScreen(viewSymptomModel) }
                }
            }
        }
    }
}

@Composable
fun ContextMonitorApp(navController: NavHostController, viewModel: MainViewModel) {
    val context = LocalContext.current

    var heartRate by remember { mutableStateOf<Float?>(null) }
    var respiratoryRate by remember { mutableStateOf<Float?>(null) }

    val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            heartRate = viewModel.measureHeartRate()
        }
    }

    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val sensorEventListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]
                    // Handle sensor data
                    println("Accelerometer values: x=$x, y=$y, z=$z")
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Handle changes in sensor accuracy
            }
        }
    }
    // Register and unregister sensor listener
    DisposableEffect(sensorManager) {
        accelerometer?.let { sensor ->
            sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        onDispose {
            sensorManager?.unregisterListener(sensorEventListener)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Heart Rate: ${heartRate ?: "Not measured"}")
        Button(onClick = {
            if (!hasCameraPermission) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }) {
            Text("Measure Heart Rate")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Respiratory Rate: ${respiratoryRate ?: "Not measured"}")
        Button(onClick = {
            respiratoryRate = viewModel.measureRespiratoryRate()
        }) {
            Text("Measure Respiratory Rate")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            navController.navigate("second/$heartRate/$respiratoryRate")
        }) {
            Text("Record Symptoms")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            navController.navigate("third")
        }) {
            Text("See Symptoms")
        }
    }
}

class MainViewModel : ViewModel() {

    fun measureHeartRate(): Float {
        return "0".toFloat() //heartRateCalculator()
    }

    fun measureRespiratoryRate(): Float {
        return "0".toFloat() //respiratoryRateCalculator()
    }
}

@Preview(showBackground = true)
@Composable
fun ContextMonitorAppPreview() {
    ContextMonitorTheme {
        ContextMonitorApp(viewModel = MainViewModel(), navController = rememberNavController())
    }
}
