package com.akheparasu.contextmonitor

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
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
        // AndroidViewModel is a ViewModel subclass that provides access to the application
        // context, useful for context-dependent operations and dependency injection.
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
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
                            } else {
                                Snackbar(
                                    modifier = Modifier.padding(16.dp),
                                    content = { Text("Incomplete data provided.") }
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
    val scrollState = rememberScrollState()
    val addSymptomsScreenPopped = navController
        .currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("add_symptoms_screen_popped", false)
        ?.collectAsState()

    // rememberSaveable retains state across configuration changes and process death, while
    // remember only retains state during recompositions.
    var heartRate by rememberSaveable { mutableStateOf<Int?>(null) }
    var respiratoryRate by rememberSaveable { mutableStateOf<Int?>(null) }
    addSymptomsScreenPopped?.value?.let { hasPopped ->
        // When database entry has been added, new request for heart rate and
        // respiratory rate should be shown
        if (hasPopped) {
            heartRate = null
            respiratoryRate = null
            // Reset the value to avoid multiple triggers
            navController
                .currentBackStackEntry
                ?.savedStateHandle
                ?.set("add_symptoms_screen_popped", false)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            Toast.makeText(context, "Permissions granted. Try again", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Required permissions are not granted", Toast.LENGTH_LONG)
                .show()
        }
    }

    var isCalculatingRR by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val isCollectingRR by respiratoryRateModel.isCollecting
    if (isCollectingRR) {
        isCalculatingRR = false
    }
    if (!isCollectingRR && isCalculatingRR == false) {
        isCalculatingRR = true
        respiratoryRate = respiratoryRateCalculator(respiratoryRateModel.accelerometerValues)
        respiratoryRateModel.clearValues()
        isCalculatingRR = null
    }

    var isCollectingHR by rememberSaveable { mutableStateOf(false) }
    var isCalculatingHR by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var videoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    LaunchedEffect(videoUri) {
        if (videoUri != null && isCalculatingHR == false) {
            isCalculatingHR = true
            heartRate = heartRateCalculator(videoUri!!, context.contentResolver)
            isCalculatingHR = null
        }
    }

    val progressRR by respiratoryRateModel.progress
    val progressHR by heartRateModel.progress
    val progress = if (isCollectingHR) {
        progressHR
    } else if (isCollectingRR) {
        progressRR
    } else {
        0
    }

    // For consistent calculations, orientation is fixed when collecting data
    val activity = LocalContext.current as Activity
    if (isCollectingHR || isCollectingRR) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
    } else {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = Color.DarkGray,
            trackColor = Color.Gray,
            progress = { progress.toFloat() / MAX_PROGRESS },
            modifier = Modifier
                .size(80.dp)
                .padding(vertical = 1.dp),
            strokeWidth = 8.dp,
        )

        Spacer(modifier = Modifier.height(SPACER_HEIGHT.dp))

        Box(
            modifier = Modifier
                .size(192.dp)
                .aspectRatio(1f)
                .padding(vertical = 1.dp)
        ) {
            if (isCollectingHR) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                if (isCollectingHR) {
                                    heartRateModel.startVideoCapture(
                                        contentResolver = context.contentResolver,
                                        lifecycleOwner = lifecycleOwner,
                                        surfaceProvider = surfaceProvider
                                    ) { uri ->
                                        videoUri = uri
                                        isCollectingHR = false
                                        isCalculatingHR = false
                                        Toast.makeText(
                                            context,
                                            "Video saved to: $uri",
                                            Toast.LENGTH_LONG
                                        )
                                            .show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize() // Set the size you want for the box
                        .background(Color.Black) // Set the background color to black
                )
            }
        }
        Text(
            text = "Heart Rate: " +
                    if (isCollectingHR) {
                        "Recording..."
                    } else if (isCalculatingHR == true) {
                        "Calculating..."
                    } else {
                        heartRate?.toString() ?: "Not measured"
                    },
            modifier = Modifier.padding(vertical = 1.dp)
        )
        Button(
            onClick = {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED) {
                    permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                } else {
                    isCollectingHR = true
                    videoUri = null
                    heartRate = null
                }
            },
            enabled = !isCollectingHR && isCalculatingHR == null
                    && !isCollectingRR && isCalculatingRR == null,
            modifier = Modifier.padding(vertical = 1.dp)
        ) {
            Text("MEASURE HEART RATE")
        }

        Spacer(modifier = Modifier.height(SPACER_HEIGHT.dp))

        Text(
            text = "Respiratory Rate: " +
                    if (isCollectingRR) {
                        "Collecting..."
                    } else if (isCalculatingRR == true) {
                        "Calculating..."
                    } else {
                        respiratoryRate?.toString() ?: "Not measured"
                    },
            modifier = Modifier.padding(vertical = 1.dp)
        )
        Button(
            onClick = {
                respiratoryRate = null
                respiratoryRateModel.startAccelerometerDataCapture()
            },
            enabled = !isCollectingHR && isCalculatingHR == null
                    && !isCollectingRR && isCalculatingRR == null,
            modifier = Modifier.padding(vertical = 1.dp)
        ) {
            Text("MEASURE RESPIRATORY RATE")
        }

        Spacer(modifier = Modifier.height(SPACER_HEIGHT.dp))

        Button(
            onClick = { navController.navigate("second/$heartRate/$respiratoryRate") },
            modifier = Modifier.padding(vertical = 1.dp),
            enabled = heartRate != null && respiratoryRate != null,
        ) {
            Text("UPLOAD SIGNS")
        }

        Spacer(modifier = Modifier.height(SPACER_HEIGHT.dp))

        Button(
            onClick = { navController.navigate("third") },
            modifier = Modifier.padding(vertical = 1.dp),
            enabled = !isCollectingHR && isCalculatingHR == null
                    && !isCollectingRR && isCalculatingRR == null,
        ) {
            Text("VIEW PAST RECORDS")
        }
    }
}
