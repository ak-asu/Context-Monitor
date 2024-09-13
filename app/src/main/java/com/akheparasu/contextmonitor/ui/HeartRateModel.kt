package com.akheparasu.contextmonitor.ui

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
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

    // Start video capture
    fun startVideoCapture(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication<Application>().applicationContext)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(surfaceProvider)
            }

            // Create a video recorder without audio
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Bind the camera provider to the lifecycle
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, videoCapture
                )
            } catch (exc: Exception) {
                // Handle exceptions
            }
        }, ContextCompat.getMainExecutor(getApplication<Application>().applicationContext))
    }

    // Start video recording for 45 seconds
    fun startRecording() {
        videoCapture?.let { videoCapture ->
            val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
                }
            }

            val outputOptions = MediaStoreOutputOptions.Builder(
                getApplication<Application>().contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
                .setContentValues(contentValues)
                .build()

            // Start recording
            recording = videoCapture.output
                .prepareRecording(getApplication<Application>().applicationContext, outputOptions)
                .start(ContextCompat.getMainExecutor(getApplication<Application>().applicationContext)) {
                    // Handle video capture events
                }

            // Stop recording after 45 seconds
            viewModelScope.launch(Dispatchers.Main) {
                delay(45_000)
                stopRecording()
            }
        }
    }

    // Stop video recording
    private fun stopRecording() {
        recording?.stop()
        recording = null
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
