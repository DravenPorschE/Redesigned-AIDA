package com.hexacore.aidaapplications

import android.content.Context
import android.widget.Toast
import ai.picovoice.porcupine.PorcupineManager
import java.io.File

class WakeWordManager(
    private val context: Context,
    private val onWakeWordDetectedCallback: () -> Unit
) {
    private var porcupineManager: PorcupineManager? = null

    fun initWakeWord() {
        try {
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
                    onWakeWordDetectedCallback() // call lambda
                }

        } catch (e: Exception) {
            e.printStackTrace()
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