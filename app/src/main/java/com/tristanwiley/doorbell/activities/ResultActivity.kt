package com.tristanwiley.doorbell.activities

import android.app.Activity
import android.os.Bundle
import com.tristanwiley.doorbell.R
import kotlinx.android.synthetic.main.activity_result.*

class ResultActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        val faceName = intent.getStringExtra("faceName")
        textView.setText(faceName)
    }
}
