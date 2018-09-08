package com.tristanwiley.doorbell

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.tristanwiley.doorbell.activities.CaptureActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dingDong.setOnClickListener {
            startActivity(Intent(this, CaptureActivity::class.java))
        }
    }
}
