package com.hexacore.aidaapplications

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class OutputAction(private val context: Context) {
    fun outputActionBasedOnIntent(intent: String, speech: String) {
        Log.d("AIDA_INTENT", "Predicted intent: $intent | Speech: $speech")
        when (intent) {
            "introduce_yourself" -> {
                TTSManager.speak(
                    "Hi, I am eye the. an Artificial Intelligence designed to assist for common or redundant tasks such as note taking. alarm creation. creation of calendar event. transcribing a meeting. summarizing the meeting. and searching online. I am created and developed by these wonderful developers. Their names are. Mercado. Ah Gil Yon. Knee Pie. Pa see no. Ferrer. and Villareal. Finally. if you want me to assist you. don't forget to say"
                )
            }
        }
    }
}
