package com.karcam

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.karcam.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private var capturedPhotoFile: File? = null

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.btnCapture.setOnClickListener { takePhoto() }
        binding.btnConfirm.setOnClickListener { uploadPhoto() }
        binding.btnRetake.setOnClickListener { retakePhoto() }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.camera_init_failed), Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoDir = File(cacheDir, "photos")
        photoDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val photoFile = File(photoDir, "Speiseplan_$timestamp.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    capturedPhotoFile = photoFile
                    showPreview(photoFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.capture_failed, exception.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun showPreview(photoFile: File) {
        binding.viewFinder.visibility = View.GONE
        binding.btnCapture.visibility = View.GONE

        binding.imagePreview.visibility = View.VISIBLE
        binding.previewButtons.visibility = View.VISIBLE

        val bitmap = ensurePortrait(photoFile)
        binding.imagePreview.setImageBitmap(bitmap)
    }

    private fun ensurePortrait(photoFile: File): Bitmap {
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        if (bitmap.width <= bitmap.height) return bitmap

        val matrix = Matrix().apply { postRotate(90f) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        FileOutputStream(photoFile).use { out ->
            rotated.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        return rotated
    }

    private fun retakePhoto() {
        capturedPhotoFile?.delete()
        capturedPhotoFile = null

        binding.imagePreview.visibility = View.GONE
        binding.previewButtons.visibility = View.GONE

        binding.viewFinder.visibility = View.VISIBLE
        binding.btnCapture.visibility = View.VISIBLE
    }

    private fun uploadPhoto() {
        val photoFile = capturedPhotoFile ?: return

        binding.previewButtons.visibility = View.GONE
        binding.uploadOverlay.visibility = View.VISIBLE
        binding.tvStatus.text = getString(R.string.uploading)
        binding.uploadProgress.progress = 0
        binding.tvProgressPercent.text = getString(R.string.progress_text, 0)

        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                FtpUploader.upload(photoFile) { percentage ->
                    launch(Dispatchers.Main) {
                        binding.uploadProgress.progress = percentage
                        binding.tvProgressPercent.text = getString(R.string.progress_text, percentage)
                    }
                }
            }

            if (result.success) {
                binding.uploadProgress.progress = 100
                binding.tvProgressPercent.text = getString(R.string.progress_text, 100)
                binding.tvStatus.text = getString(R.string.upload_success)
                binding.tvStatus.postDelayed({ finish() }, 2000)
            } else {
                binding.uploadProgress.visibility = View.GONE
                binding.tvProgressPercent.visibility = View.GONE
                binding.tvStatus.text = getString(R.string.upload_failed, result.errorMessage)
                binding.tvStatus.postDelayed({ retakePhoto() }, 3000)
            }
        }
    }
}
