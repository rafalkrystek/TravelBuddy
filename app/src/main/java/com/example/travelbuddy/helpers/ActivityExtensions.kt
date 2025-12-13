package com.example.travelbuddy.helpers

import android.app.Activity
import android.widget.ImageButton
import com.example.travelbuddy.R

fun Activity.setupBackButton() {
    findViewById<ImageButton>(R.id.backButton)?.setOnClickListener { finish() }
}

