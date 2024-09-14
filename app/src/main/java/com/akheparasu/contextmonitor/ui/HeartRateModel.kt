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
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HeartRateModel(application: Application) : AndroidViewModel(application) {
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var camera: Camera? = null

    fun startVideoCapture(
        contentResolver: ContentResolver,
        surfaceProvider: SurfaceProvider,
        lifecycleOwner: LifecycleOwner,
        onVideoRecorded: (Uri?) -> Unit
    ) {
        val context = getApplication<Application>().applicationContext
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(surfaceProvider)
            }
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            // Bind the camera and preview to the lifecycle
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, videoCapture
                )
                startRecording(contentResolver, onVideoRecorded)
            } catch (exc: Exception) {
                Log.e("VideoCapture", "Camera binding failed: ${exc.message}", exc)
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
            )
                .setContentValues(contentValues)
                .build()
            camera?.cameraControl?.enableTorch(true)
            // Start recording
            recording = videoCapture.output
                .prepareRecording(context, outputOptions)
                .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            Log.e("VideoCapture", "started recording")
                        }

                        is VideoRecordEvent.Finalize -> {
                            onVideoRecorded(
                                if (recordEvent.outputResults.outputUri != Uri.EMPTY) {
                                    recordEvent.outputResults.outputUri
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }
            viewModelScope.launch(Dispatchers.Main) {
                delay(45_000)
                stopRecording()
            }
        }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
        camera?.cameraControl?.enableTorch(false)
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
