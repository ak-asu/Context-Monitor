package com.akheparasu.contextmonitor

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.akheparasu.contextmonitor.ui.*
import com.akheparasu.contextmonitor.utils.*
import com.akheparasu.contextmonitor.ui.theme.ContextMonitorTheme

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
        val addSymptomModel: AddSymptomsViewModel by viewModels {
            AddSymptomsViewModelFactory(application)
        }
        val viewSymptomModel: ViewSymptomsModel by viewModels {
            ViewSymptomsModelFactory(application)
        }
        enableEdgeToEdge()
        setContent {
            ContextMonitorTheme {
                val navController = rememberNavController()
                Scaffold(
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
                                padding = padding
                            )
                        }
                        composable("second/{heartRate}/{respiratoryRate}") { backStackEntry ->
                            val heartRate = backStackEntry.arguments?.getInt("heartRate")
                            val respiratoryRate =
                                backStackEntry.arguments?.getInt("respiratoryRate")
                            if (heartRate != null && respiratoryRate != null) {
                                AddSymptomsScreen(
                                    navController = navController,
                                    viewModel = addSymptomModel,
                                    padding = padding,
                                    heartRate = heartRate,
                                    respiratoryRate = respiratoryRate
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
    val lifecycleOwner = LocalLifecycleOwner.current

    var heartRate by rememberSaveable { mutableStateOf<Int?>(null) }
    var respiratoryRate by rememberSaveable { mutableStateOf<Int?>(null) }

    var isCalculatingRR by rememberSaveable { mutableStateOf(false) }
    var isCollectingRR by rememberSaveable { mutableStateOf(false) }

    if (!isCollectingRR
        && respiratoryRateModel.accelerometerValues.isNotEmpty()
        && respiratoryRate == null
    ) {
        respiratoryRate = respiratoryRateCalculator(respiratoryRateModel.accelerometerValues)
    }

    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {

        }
    }
    var isRecordingHR by rememberSaveable { mutableStateOf(false) }
    var isCalculatingHR by rememberSaveable { mutableStateOf(false) }
    var videoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    LaunchedEffect(videoUri) {
        if (videoUri != null) {
            heartRate = heartRateCalculator(videoUri!!, context.contentResolver)
            isCalculatingHR = false
        }
    }
    // Register and unregister sensor listener
//    DisposableEffect(SensorManager) {
//        accelerometer?.let { sensor ->
//            sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
//        }
//        onDispose {
//            respiratoryRateModelsensorManager?.unregisterListener(sensorEventListener)
//        }
//    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    // Start the camera capture when the view is created
                    if (isRecordingHR) {
                    heartRateModel.startVideoCapture(
                        contentResolver = context.contentResolver,
                        lifecycleOwner = lifecycleOwner,
                        surfaceProvider = surfaceProvider
                    ) { uri ->
                        videoUri = uri
                        isRecordingHR = false
                        isCalculatingHR = true
                        Toast.makeText(context, "Video saved to: $uri", Toast.LENGTH_LONG).show()
                    }
                    }
                }
            },
            modifier = Modifier
                .size(64.dp)
                .padding(vertical = 4.dp)
        )
        Text(
            text = "Heart Rate: " +
                    if (isRecordingHR) {
                        "Recording..."
                    } else if (isCalculatingHR) {
                        "Calculating..."
                    } else {
                        heartRate?.toString() ?: "Not measured"
                    },
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Button(
            onClick = {
                if (!hasCameraPermission) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    if (!isRecordingHR) {
                        isRecordingHR = true
                    }
                }
            },
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text("MEASURE HEART RATE")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Respiratory Rate: " +
                    if (isCollectingRR) {
                        "Collecting..."
                    } else if (isCalculatingRR) {
                        "Calculating..."
                    } else {
                        respiratoryRate?.toString() ?: "Not measured"
                    }
        )
        Button(onClick = {
            if (isCollectingRR) {
                respiratoryRateModel.clearValues()
            }
            respiratoryRateModel.startAccelerometerDataCapture()
        }) {
            Text("MEASURE RESPIRATORY RATE")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            navController.navigate("second/$heartRate/$respiratoryRate")
        }) {
            Text("UPLOAD SIGNS")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            navController.navigate("third")
        }) {
            Text("SYMPTOMS")
        }
    }
}
