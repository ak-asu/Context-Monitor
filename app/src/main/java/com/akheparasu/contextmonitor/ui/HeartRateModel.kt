package com.akheparasu.contextmonitor.ui

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akheparasu.contextmonitor.utils.MAX_PROGRESS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HeartRateModel(application: Application) : AndroidViewModel(application) {
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    val progress = mutableIntStateOf(0)

    fun startVideoCapture(
        contentResolver: ContentResolver,
        surfaceProvider: SurfaceProvider,
        lifecycleOwner: LifecycleOwner,
        onVideoRecorded: (Uri?) -> Unit
    ) {
        progress.intValue = 0
        val context = getApplication<Application>().applicationContext
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                cameraProvider?.unbindAll() // Unbind before rebinding
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(surfaceProvider)
                }
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, videoCapture
                )
                startRecording(contentResolver, onVideoRecorded)
            } catch (exc: Exception) {
                Log.e("HeartRateCapture", "Failed to capture: ${exc.message}", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun startRecording(contentResolver: ContentResolver, onVideoRecorded: (Uri?) -> Unit) {
        val context = getApplication<Application>().applicationContext
        videoCapture?.let { videoCapture ->
            val name = "heart_rate_" + SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
            val outputOptions = MediaStoreOutputOptions.Builder(
                contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).setContentValues(contentValues).build()
            camera?.cameraControl?.enableTorch(true)
            recording = videoCapture.output
                .prepareRecording(context, outputOptions)
                .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    handleVideoRecordEvent(recordEvent, onVideoRecorded)
                }
            viewModelScope.launch(Dispatchers.Main) {
                while (progress.intValue < MAX_PROGRESS) {
                    delay(1000)
                    progress.intValue += 1
                }
                stopRecording()
            }
        } ?: Log.e("HeartRateCapture", "videoCapture is null, cannot start recording")
    }

    private fun handleVideoRecordEvent(
        recordEvent: VideoRecordEvent,
        onVideoRecorded: (Uri?) -> Unit
    ) {
        when (recordEvent) {
            is VideoRecordEvent.Start -> {
                Log.d("HeartRateCapture", "Recording started")
            }
            is VideoRecordEvent.Finalize -> {
                val outputUri = if (recordEvent.outputResults.outputUri != Uri.EMPTY) {
                    recordEvent.outputResults.outputUri
                } else {
                    null
                }
                onVideoRecorded(outputUri)
                stopCamera()
            }
        }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
        camera?.cameraControl?.enableTorch(false)
        stopCamera()
    }

    private fun stopCamera() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        camera = null
    }

    override fun onCleared() {
        super.onCleared()
        stopRecording()
        stopCamera()
    }
}

class HeartRateModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HeartRateModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HeartRateModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
