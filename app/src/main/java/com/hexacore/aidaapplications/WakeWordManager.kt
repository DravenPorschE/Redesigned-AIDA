package com.hexacore.aidaapplications

import android.content.Context
import android.widget.Toast
import ai.picovoice.porcupine.PorcupineManager
import java.io.File

class WakeWordManager(private val context: Context) {
    private var porcupineManager: PorcupineManager? = null

    fun initWakeWord() {
        try {
            // Copy keyword file from assets to internal storage
            val keywordFile = File(context.filesDir, "Hey-Aida_en_android_v3_0_0.ppn")
            if (!keywordFile.exists()) {
                context.assets.open("Hey-Aida_en_android_v3_0_0.ppn").use { input ->
                    keywordFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            porcupineManager = PorcupineManager.Builder()
                .setAccessKey("VM996Z/2j8ghpUlIqlqxMmVAOxOHHCMujdWtGLAZ3i43Q0vTinykmg==")
                .setKeywordPath(keywordFile.absolutePath)
                .setSensitivity(0.7f)
                .build(context) {
                    // Wake word detected → do logic here instead of MainActivity
                    onWakeWordDetected()
                }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onWakeWordDetected() {
        // This is where you add the functions you want to trigger
        // Example: Toast, open fragment, run logic, etc.
        (context as? MainActivity)?.runOnUiThread {
            Toast.makeText(context, "Wake word detected!", Toast.LENGTH_SHORT).show()

        }
    }

    fun start() {
        porcupineManager?.start()
    }

    fun stop() {
        porcupineManager?.stop()
    }

    fun release() {
        porcupineManager?.delete()
    }
}
