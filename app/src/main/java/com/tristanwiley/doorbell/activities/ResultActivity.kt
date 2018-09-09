package com.tristanwiley.doorbell.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.tristanwiley.doorbell.MainActivity
import com.tristanwiley.doorbell.R
import kotlinx.android.synthetic.main.activity_result.*

class ResultActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        val faceName = intent.getStringExtra("faceName")
        if (faceName == "unknown") {
            textView.text = "Not to be rude, but who even are you?"
        } else {
            textView.text = "Welcome, $faceName!"
        }

        button.setOnClickListener{
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
