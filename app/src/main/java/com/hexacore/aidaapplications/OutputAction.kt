package com.hexacore.aidaapplications

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
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
        }
    }
}
