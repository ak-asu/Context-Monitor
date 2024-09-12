package com.akheparasu.contextmonitor.ui
//
//import android.Manifest
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.view.SurfaceHolder
//import android.view.SurfaceView
//import android.hardware.Camera
//import android.provider.MediaStore
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import com.akheparasu.contextmonitor.R
//import com.akheparasu.contextmonitor.utils.heartRateCalculator
//
//class HeartRateMeasurement : AppCompatActivity() {
//
//    private lateinit var camera: Camera
//    private lateinit var surfaceView: SurfaceView
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_heart_rate)
//
//        surfaceView = findViewById(R.id.surfaceView)
//    }
//
//    private fun startCamera() {
//        camera = Camera.open()
//        val holder: SurfaceHolder = surfaceView.holder
//        camera.setPreviewDisplay(holder)
//        camera.startPreview()
//        // TODO: Implement the video recording and processing to derive heart rate.
//        val videoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
//        videoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 45) // Limit video to 45 seconds
//        if (videoIntent.resolveActivity(packageManager) != null) {
//            startActivityForResult(videoIntent, VIDEO_CAPTURE_REQUEST_CODE)
//        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        camera.release() // Release camera when not in use
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == VIDEO_CAPTURE_REQUEST_CODE && resultCode == RESULT_OK) {
//            val videoUri = data?.data ?: return
//
//            // Call your heart rate calculator with the video URI
//            val heartRate = heartRateCalculator(videoUri, contentResolver)
//            val heartRateTextView: TextView = findViewById(R.id.heartRateTextView)
//            heartRateTextView.text = "Heart Rate: $heartRate bpm"
//        }
//    }
//}
