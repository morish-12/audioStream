package com.example.audiostream

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var status: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33)
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        val prefs = getSharedPreferences("p", MODE_PRIVATE)
        val pad = (24 * resources.displayMetrics.density).toInt()
        val ll = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }
        val ip = EditText(this).apply { hint = "PC IP (e.g. 192.168.1.5)"; setText(prefs.getString("ip", "")) }
        val port = EditText(this).apply {
            hint = "Port"; inputType = InputType.TYPE_CLASS_NUMBER; setText(prefs.getString("port", "5555"))
        }
        val start = Button(this).apply { text = "Connect & Play" }
        val stop = Button(this).apply { text = "Stop" }
        status = TextView(this).apply { textSize = 16f; setPadding(0, pad, 0, 0) }
        listOf(ip, port, start, stop, status).forEach { ll.addView(it) }
        setContentView(ll)

        start.setOnClickListener {
            prefs.edit().putString("ip", ip.text.toString()).putString("port", port.text.toString()).apply()
            val i = Intent(this, StreamService::class.java)
                .putExtra("host", ip.text.toString().trim())
                .putExtra("port", port.text.toString().toIntOrNull() ?: 5555)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
        }
        stop.setOnClickListener { startService(Intent(this, StreamService::class.java).setAction("STOP")) }
        poll()
    }

    private fun poll() {
        status.text = "Status: ${StreamService.status}"
        h.postDelayed({ poll() }, 700)
    }

    override fun onDestroy() { h.removeCallbacksAndMessages(null); super.onDestroy() }
}
