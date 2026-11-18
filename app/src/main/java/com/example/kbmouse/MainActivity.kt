package com.example.kbmouse

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var tv: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        tv = TextView(this).apply {
            textSize = 18f
            text = "Tap layar sekali, lalu gerakkan mouse / tekan tombol"
            isFocusable = true
            isFocusableInTouchMode = true
        }
        setContentView(tv)
        tv.setOnClickListener { tv.requestPointerCapture() }
        tv.setOnCapturedPointerListener { _, e ->
            if (e.actionMasked == MotionEvent.ACTION_MOVE) {
                tv.text = "Mouse dx=${e.x} dy=${e.y}"
            }
            true
        }
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val aksi = if (e.action == KeyEvent.ACTION_DOWN) "TEKAN" else "LEPAS"
        tv.text = "Tombol ${KeyEvent.keyCodeToString(e.keyCode)} $aksi"
        return true
    }
}
