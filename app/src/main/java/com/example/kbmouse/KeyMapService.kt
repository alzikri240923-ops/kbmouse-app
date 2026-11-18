package com.example.kbmouse

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import kotlin.math.sqrt

class KeyMapService : AccessibilityService() {
    private val held = mutableSetOf<Int>()
    private var stroke: StrokeDescription? = null
    private var lastX = 0f
    private var lastY = 0f
    private var gestureId = 0

    // Posisi joystick (diatur otomatis, bisa diubah di Tahap 3)
    private var cx = 0f
    private var cy = 0f
    private var radius = 0f

    override fun onServiceConnected() {
        val m = DisplayMetrics()
        (getSystemService(WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.getRealMetrics(m)
        val w = maxOf(m.widthPixels, m.heightPixels).toFloat()
        val h = minOf(m.widthPixels, m.heightPixels).toFloat()
        cx = w * 0.15f
        cy = h * 0.70f
        radius = h * 0.12f
    }

    override fun onKeyEvent(e: KeyEvent): Boolean {
        val k = e.keyCode
        if (k != KeyEvent.KEYCODE_W && k != KeyEvent.KEYCODE_A &&
            k != KeyEvent.KEYCODE_S && k != KeyEvent.KEYCODE_D) return false

        if (e.action == KeyEvent.ACTION_DOWN) {
            if (!held.add(k)) return true // abaikan key repeat
        } else if (e.action == KeyEvent.ACTION_UP) {
            held.remove(k)
        }
        update()
        return true
    }

    private fun update() {
        var dx = 0f
        var dy = 0f
        if (KeyEvent.KEYCODE_W in held) dy -= 1f
        if (KeyEvent.KEYCODE_S in held) dy += 1f
        if (KeyEvent.KEYCODE_A in held) dx -= 1f
        if (KeyEvent.KEYCODE_D in held) dx += 1f
        if (dx == 0f && dy == 0f) { release(); return }
        val len = sqrt(dx * dx + dy * dy)
        moveTo(cx + dx / len * radius, cy + dy / len * radius)
    }

    private fun moveTo(x: Float, y: Float) {
        val p = Path()
        val prev = stroke
        stroke = if (prev == null) {
            p.moveTo(cx, cy); p.lineTo(x, y)
            StrokeDescription(p, 0, 80, true)
        } else {
            p.moveTo(lastX, lastY); p.lineTo(x, y)
            prev.continueStroke(p, 0, 80, true)
        }
        lastX = x; lastY = y
        send()
    }

    private fun send() {
        val s = stroke ?: return
        val id = ++gestureId
        dispatchGesture(
            GestureDescription.Builder().addStroke(s).build(),
            object : GestureResultCallback() {
                override fun onCompleted(g: GestureDescription?) {
                    if (id == gestureId && stroke != null) hold()
                }
            }, null
        )
    }

    // Tahan jari di posisi terakhir supaya joystick tidak lepas
    private fun hold() {
        val p = Path()
        p.moveTo(lastX, lastY)
        stroke = stroke!!.continueStroke(p, 0, 80, true)
        send()
    }

    private fun release() {
        val prev = stroke ?: return
        stroke = null
        gestureId++
        val p = Path()
        p.moveTo(lastX, lastY)
        dispatchGesture(
            GestureDescription.Builder()
                .addStroke(prev.continueStroke(p, 0, 50, false)).build(),
            null, null
        )
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() { held.clear(); stroke = null }
}
