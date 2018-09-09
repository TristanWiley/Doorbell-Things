package com.tristanwiley.doorbell.activities

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.google.android.things.pio.PeripheralManager
import com.google.android.things.pio.Pwm
import com.koushikdutta.ion.Ion
import com.tristanwiley.doorbell.MainActivity
import com.tristanwiley.doorbell.Note
import com.tristanwiley.doorbell.R
import com.tristanwiley.doorbell.camera.CameraHandler
import com.tristanwiley.doorbell.camera.ImagePreprocessor
import kotlinx.android.synthetic.main.activity_calling.*
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*


class CaptureActivity : Activity() {
    lateinit var mCameraHandler: CameraHandler
    lateinit var mImagePreprocessor: ImagePreprocessor
    /** Camera image capture size  */
    private val PREVIEW_IMAGE_WIDTH = 640
    private val PREVIEW_IMAGE_HEIGHT = 480
    /** Image dimensions required by TF model  */
    private val TF_INPUT_IMAGE_WIDTH = 224
    private val TF_INPUT_IMAGE_HEIGHT = 224
    private var cameraOpen = false
    private var isFirstPhoto = true
    val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calling)

        initCamera()
        val delay = 1L
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (cameraOpen) loadPhoto()
                handler.postDelayed(this, delay)
            }
        }, delay)
        val pwm = PeripheralManager.getInstance().openPwm("PWM2")

        playSounds(pwm, arrayListOf(Note(440.0, 1000), Note(349.228, 1000),
                Note(391.995, 1000), Note(261.626, 1000), Note(261.626, 1000),
                Note(391.995, 1000), Note(440.0, 1000), Note(349.228, 1000)))
    }

    private fun initCamera() {
        cameraOpen = true
        mImagePreprocessor = ImagePreprocessor(
                PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT,
                TF_INPUT_IMAGE_WIDTH, TF_INPUT_IMAGE_HEIGHT)
        mCameraHandler = CameraHandler.getInstance()
        mCameraHandler.initializeCamera(this,
                PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT, null
        ) { imageReader ->
            val bitmap = mImagePreprocessor.preprocessImage(imageReader.acquireNextImage())
            if (bitmap != null) {
                onPhotoReady(bitmap)
            }
        }
    }

    private fun playSounds(pwm: Pwm, list: ArrayList<Note>) {
        pwm.setPwmFrequencyHz(list[0].NOTE_FREQUENCY)
        pwm.setPwmDutyCycle(20.0)
        pwm.setEnabled(true)
        doAsync {
            for (note in list) {
                pwm.setPwmFrequencyHz(note.NOTE_FREQUENCY)
                Thread.sleep(note.NOTE_DELAY)
            }

            pwm.setEnabled(false)
        }
    }

    private fun onPhotoReady(bitmap: Bitmap) {
        if (isFirstPhoto) {
            isFirstPhoto = false
            Log.wtf("sendingPic", "This pic boutta get dabbed on")
            Ion.with(applicationContext)
                    .load("http://3bd8e6c5.ngrok.io/uploadImage")
                    .setMultipartFile("image", bitmapToFile(bitmap))
                    .asJsonObject()
                    .setCallback { _, json ->
                        Log.wtf("onPhotoReady", "SENT: $json")
                        //The JSON here is of format {"confidence":#, "faceID":String, "faceName": string}
                        handler.removeCallbacksAndMessages(null)
                        closeCamera()
                        try {
                            runOnUiThread {
                                var intent = Intent(this@CaptureActivity, ResultActivity::class.java)
                                intent.putExtra("faceName", json.get("faceName").asString)
                                startActivity(intent)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

        }
        imageView.setImageBitmap(bitmap)
    }

    private fun bitmapToFile(bitmap: Bitmap): File {
        // Get the context wrapper
        val wrapper = ContextWrapper(applicationContext)

        // Initialize a new file instance to save bitmap object
        var file = wrapper.getDir("Images", Context.MODE_PRIVATE)
        file = File(file, "${UUID.randomUUID()}.jpg")

        try {
            // Compress the bitmap and save in jpg format
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Return the saved bitmap uri
        return file
    }

    /**
     * Clean up resources used by the camera.
     */
    private fun closeCamera() {
        cameraOpen = false
        mCameraHandler.shutDown()
    }

    /**
     * Load the image that will be used in the classification process.
     * When done, the method [.onPhotoReady] must be called with the image.
     */
    private fun loadPhoto() {
        if (cameraOpen) mCameraHandler.takePicture()
    }

}
