package com.hexacore.aidaapplications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import java.util.Calendar
import java.util.Locale

class OutputAction(private val activity: MainActivity) {

    var gameResponses = arrayOf("Okay. Lets play a game", "Okay. I Accept your challenge", "Sure, here are the games available")

    fun outputActionBasedOnIntent(intent: String, speech: String) {
        Log.d("AIDA_INTENT", "Predicted intent: $intent | Speech: $speech")
        when (intent) {
            "introduce_yourself" -> {
                TTSManager.speak(
                    "Hi, I am eye the. an Artificial Intelligence designed to assist for common or redundant tasks such as note taking. alarm creation. creation of calendar event. transcribing a meeting. summarizing the meeting. and searching online. I am created and developed by these wonderful developers. Their names are. Mercado. Ah Gil Yon. Knee Pie. Pa see no. Ferrer. and Villareal. Finally. if you want me to assist you. don't forget to say"
                )
            }

            "open_games" -> {
                TTSManager.speak(gameResponses.random())

                activity.
                openScreen(GameMenuScreen())
            }

            "play_tic_tac_toe" -> {
                TTSManager.speak(gameResponses.random())
                activity.openScreen(TicTacToeScreen())
            }

            "play_sudoku" -> {
                TTSManager.speak(gameResponses.random())
                activity.openScreen(SudokuScreen())
            }

            "play_memory_game" -> {
                TTSManager.speak(gameResponses.random())
                activity.openScreen(MemoryGameScreen())
            }

            "play_random_game" -> {
                val randomGame = listOf("tic_tac_toe", "sudoku", "memory_game").random()

                TTSManager.speak("Okay. Here is $randomGame for you to play")

                when (randomGame) {
                    "tic_tac_toe" -> activity.openScreen(TicTacToeScreen())
                    "sudoku" -> activity.openScreen(SudokuScreen())
                    "memory_game" -> activity.openScreen(MemoryGameScreen())
                }
            }

            "open_configuration" -> {
                activity.resetSidePanel()

                TTSManager.speak("Okay. Here is the configuration menu")

                activity.openScreen(ConfigureAppScreen())
            }

            "open_alarm" -> {
                activity.resetSidePanel()

                TTSManager.speak("Okay. Here is the alarm app")

                activity.openScreen(AlarmAppScreen())
            }

            "set_alarm" -> {
                print("Start alarm")

                // 🔹 Regex to extract time
                val timeRegex = Regex(
                    """(?<hour>\d{1,2})(?:[:.\s](?<minute>\d{2}))?\s*(?<ampm>am|pm|a\.m\.|p\.m\.|morning|night|evening)?""",
                    RegexOption.IGNORE_CASE
                )

                val timeMatch = timeRegex.find(speech)

                var hour: Int? = null
                var minute: Int? = null
                var ampm: String? = null

                if (timeMatch != null) {
                    hour = timeMatch.groups["hour"]?.value?.toIntOrNull()
                    minute = timeMatch.groups["minute"]?.value?.toIntOrNull() ?: 0

                    val ampmRaw = timeMatch.groups["ampm"]?.value?.lowercase()
                    ampm = when {
                        ampmRaw == null -> null
                        "a" in ampmRaw || "morning" in ampmRaw -> "AM"
                        "p" in ampmRaw || "night" in ampmRaw || "evening" in ampmRaw -> "PM"
                        else -> null
                    }
                }

                println("[DEBUG] Set Alarm → Time: ${hour ?: "--"}:${minute?.toString()?.padStart(2, '0') ?: "--"} ${ampm ?: ""}")

                val alarmData = mutableMapOf<String, Any>()
                hour?.let { alarmData["hour"] = it }
                minute?.let { alarmData["minute"] = it }
                ampm?.let { alarmData["ampm"] = it }

                try {
                    val hour = hour
                    val minute = minute
                    val ampm = ampm

                    var isAmPm = false
                    if(ampm == "AM") {
                        isAmPm = true
                    }

                    if (hour != null && minute != null) {
                        createAlarmDirectly(hour, minute, isAmPm)
                        TTSManager.speak("Alarm set for $hour:$minute $ampm")
                    } else {
                        //speakText("Sorry, I couldn’t read the time")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            "create_note" -> {
                // 🔹 Regex to extract note title
                val titleRegex = Regex(
                    """(?:called|titled|title\s+is|with\s+(?:a|the)\s+title|about|named)\s+(.+?)(?=\s+(?:and\s+content\s+is|content\s+is)|$)""",
                    RegexOption.IGNORE_CASE
                )

                // 🔹 Regex to extract note content
                val contentRegex = Regex(
                    """(?:content\s+is|with\s+content|body\s+is)\s+([^\n]+)""",
                    RegexOption.IGNORE_CASE
                )

                val titleMatch = titleRegex.find(speech)
                val contentMatch = contentRegex.find(speech)

                val title = titleMatch?.groups?.get(1)?.value?.trim() ?: "Untitled Note"
                val content = contentMatch?.groups?.get(1)?.value?.trim() ?: ""

                println("[DEBUG] Extracted Title: '$title', Content: '$content' from speech: '$speech'")

                val noteData = mutableMapOf<String, Any>()
                noteData["title"] = title
                if (content.isNotEmpty()) noteData["content"] = content

                try {
                    val title = title
                    val content = content

                    aiCreateNewNote(title, content)

                    TTSManager.speak("Note created with title '$title'")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun createAlarmDirectly(hour: Int, minute: Int, isAm: Boolean) {
        AlarmScheduler.scheduleAlarm(activity, hour, minute, isAm)
    }

    fun aiCreateNewNote(title: String, info: String) {
        val notes = NoteStorage.loadNotes(activity)
        notes.add(Note(title, info))
        NoteStorage.saveNotes(activity, notes)

        // 🔹 Reload notes to confirm in logs
        val updatedNotes = NoteStorage.loadNotes(activity)
        Log.d("AIDA_DEBUG", "Notes reloaded → ${updatedNotes.size} total notes")

        // 🔹 If NoteAppScreen is currently visible, refresh it
        val fragment = activity.supportFragmentManager.findFragmentById(R.id.main_content)
        if (fragment is NoteAppScreen && fragment.isVisible) {
            fragment.reloadNotes()
            Log.d("AIDA_DEBUG", "NoteAppScreen UI reloaded after saving note")
        }
    }

}
