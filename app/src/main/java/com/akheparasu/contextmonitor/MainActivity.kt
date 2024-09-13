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
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.akheparasu.contextmonitor.ui.AddSymptomsScreen
import com.akheparasu.contextmonitor.ui.HeartRateModel
import com.akheparasu.contextmonitor.ui.HeartRateModelFactory
import com.akheparasu.contextmonitor.ui.RespiratoryRateModel
import com.akheparasu.contextmonitor.ui.RespiratoryRateModelFactory
import com.akheparasu.contextmonitor.ui.SymptomAddModel
import com.akheparasu.contextmonitor.ui.SymptomAddModelFactory
import com.akheparasu.contextmonitor.ui.SymptomViewModel
import com.akheparasu.contextmonitor.ui.SymptomViewModelFactory
import com.akheparasu.contextmonitor.ui.ViewSymptomsScreen
import com.akheparasu.contextmonitor.utils.heartRateCalculator
import com.akheparasu.contextmonitor.utils.respiratoryRateCalculator

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val heartRateModel: HeartRateModel by viewModels {
            HeartRateModelFactory(application)
        }
        val respiratoryRateModel: RespiratoryRateModel by viewModels {
            RespiratoryRateModelFactory(application)
        }
        val addSymptomModel: SymptomAddModel by viewModels {
            SymptomAddModelFactory(application)
        }
        val viewSymptomModel: SymptomViewModel by viewModels {
            SymptomViewModelFactory(application)
        }
        enableEdgeToEdge()
        setContent {
            ContextMonitorTheme {
                val navController = rememberNavController()
                Scaffold (
                    topBar = {
                        TopAppBar(
                            title = { Text(getString(R.string.app_name)) }
                        )
                    },
                ) { padding ->
                    NavHost(navController, startDestination = "first") {
                        composable("first") {
                            ContextMonitorApp(
                                navController = navController,
                                heartRateModel = heartRateModel,
                                respiratoryRateModel = respiratoryRateModel,
                                padding = padding)
                        }
                        composable("second/{heartRate}/{respiratoryRate}") { backStackEntry ->
                            val heartRat = backStackEntry.arguments?.getString("heartRate")
                            val respiratoryRate = backStackEntry.arguments?.getString("respiratoryRate")
                            val heartRate = 0f
                            if (heartRate != null && respiratoryRate != null) {
                                AddSymptomsScreen(
                                    navController = navController,
                                    viewModel = addSymptomModel,
                                    padding = padding,
                                    heartRate = heartRate.toFloat(),
                                    respiratoryRate = respiratoryRate.toFloat()
                                )
                            }
                        }
                        composable("third") { ViewSymptomsScreen(viewSymptomModel, padding) }
                    }
                }
            }
        }
    }
}

@Composable
fun ContextMonitorApp(
    navController: NavHostController,
    heartRateModel: HeartRateModel,
    respiratoryRateModel: RespiratoryRateModel,
    padding: PaddingValues
) {
    val context = LocalContext.current

    var heartRate by remember { mutableStateOf<Float?>(null) }
    var respiratoryRate by remember { mutableStateOf<Float?>(null) }

    val isCollecting by remember { respiratoryRateModel.isCollecting }
    val accelerometerValues = respiratoryRateModel.accelerometerValues

    if (!isCollecting && accelerometerValues.isNotEmpty()) {
        respiratoryRate = respiratoryRateCalculator(accelerometerValues).toFloat()
    }

//    if (!isRecording && accelerometerValues.isNotEmpty()) {
//        respiratoryRate = respiratoryRateCalculator(accelerometerValues).toFloat()
//    }

    val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // heartRate = viewModel.measureHeartRate()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isRecording by remember { mutableStateOf(false) }

    // Register and unregister sensor listener
//    DisposableEffect(sensorManager) {
//        accelerometer?.let { sensor ->
//            sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
//        }
//        onDispose {
//            sensorManager?.unregisterListener(sensorEventListener)
//        }
//    }

    Column(
        modifier = Modifier.fillMaxSize().padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Heart Rate: ${heartRate ?: "Not measured"}")
        Button(onClick = {
            if (!hasCameraPermission) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            } else {
                if (!isRecording) {
                    isRecording = true
                }
            }
        }) {
            Text("Measure Heart Rate")
        }

// Camera preview using CameraX PreviewView
        if (isRecording) {
            AndroidView(
                factory = { context ->
                    PreviewView(context).apply {
                        // Start the camera capture when the view is created
                        heartRateModel.startVideoCapture(
                            lifecycleOwner = lifecycleOwner,
                            surfaceProvider = surfaceProvider
                        )
                        heartRateModel.startRecording()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Respiratory Rate: ${respiratoryRate ?: "Not measured"}")
        Button(onClick = {
            if (isCollecting) {
                respiratoryRateModel.clearValues()
                accelerometerValues.clear()
            }
            respiratoryRateModel.startAccelerometerDataCapture()
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

//@Preview(showBackground = true)
//@Composable
//fun ContextMonitorAppPreview() {
//    ContextMonitorTheme {
//        ContextMonitorApp(
//            viewModel = MainViewModel(),
//            navController = rememberNavController(),
//            padding = PaddingValues(16.dp)
//        )
//    }
//}
