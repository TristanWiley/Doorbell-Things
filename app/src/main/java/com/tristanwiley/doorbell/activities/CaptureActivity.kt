package com.tristanwiley.doorbell.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import com.google.android.things.pio.PeripheralManager
import com.google.android.things.pio.Pwm
import com.tristanwiley.doorbell.MainActivity
import com.tristanwiley.doorbell.Note
import com.tristanwiley.doorbell.R
import com.tristanwiley.doorbell.camera.CameraHandler
import com.tristanwiley.doorbell.camera.ImagePreprocessor
import kotlinx.android.synthetic.main.activity_calling.*
import org.jetbrains.anko.doAsync


class CaptureActivity : Activity() {
    lateinit var mCameraHandler: CameraHandler
    lateinit var mImagePreprocessor: ImagePreprocessor
    /** Camera image capture size  */
    private val PREVIEW_IMAGE_WIDTH = 640
    private val PREVIEW_IMAGE_HEIGHT = 480
    /** Image dimensions required by TF model  */
    private val TF_INPUT_IMAGE_WIDTH = 224
    private val TF_INPUT_IMAGE_HEIGHT = 224
    val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calling)

        initCamera()
        val delay = 1L
        handler.postDelayed(object : Runnable {
            override fun run() {
                loadPhoto()
                handler.postDelayed(this, delay)
            }
        }, delay)
        val pwm = PeripheralManager.getInstance().openPwm("PWM2")

        playSounds(pwm, arrayListOf(Note(440.0, 1000), Note(349.228, 1000),
                Note(391.995, 1000), Note(261.626, 1000), Note(261.626, 1000),
                Note(391.995, 1000), Note(440.0, 1000), Note(349.228, 1000)))
    }

    private fun initCamera() {
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
            handler.removeCallbacksAndMessages(null)
            closeCamera()
            runOnUiThread { startActivity(Intent(this@CaptureActivity, MainActivity::class.java)) }
            pwm.setEnabled(false)
        }
    }

    private fun onPhotoReady(bitmap: Bitmap) {
        imageView.setImageBitmap(bitmap)
    }

    /**
     * Clean up resources used by the camera.
     */
    private fun closeCamera() {
        mCameraHandler.shutDown()
    }

    /**
     * Load the image that will be used in the classification process.
     * When done, the method [.onPhotoReady] must be called with the image.
     */
    private fun loadPhoto() {
        mCameraHandler.takePicture()
    }

}
