package com.hexacore.aidaapplications

import android.util.Log

class OutputAction(private val activity: MainActivity) {

    private val gameResponses = arrayOf(
        "Okay. Let's play a game",
        "Okay. I accept your challenge",
        "Sure, here are the games available"
    )

    fun outputActionBasedOnIntent(intent: String, speech: String) {
        Log.d("AIDA_INTENT", "Predicted intent: $intent | Speech: $speech")

        when (intent) {
            "introduce_yourself" -> {
                TTSManager.speak(
                    "Hi, I am AIDA. An Artificial Intelligence designed to assist with tasks such as note taking, alarm creation, creating calendar events, transcribing meetings, summarizing meetings, and searching online. I was created and developed by Mercado, Gil Yon, Knee Pie, Pasino, Ferrer, and Villareal. If you want me to assist you, don't forget to say my wake word."
                )
            }

            "open_games" -> {
                TTSManager.speak(gameResponses.random())
                activity.openScreen(GameMenuScreen())
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
