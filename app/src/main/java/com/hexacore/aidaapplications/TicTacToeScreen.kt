package com.hexacore.aidaapplications

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class TicTacToeScreen : Fragment() {

    private lateinit var board: Array<Array<Button>>
    private lateinit var turnText: TextView
    private lateinit var winnerLayout: LinearLayout
    private lateinit var winnerText: TextView
    private lateinit var score1Text: TextView
    private lateinit var score2Text: TextView
    private lateinit var player1Label: TextView
    private lateinit var player2Label: TextView

    private lateinit var btnContinue: Button
    private lateinit var btnGiveUp: Button

    // For match winner section
    private lateinit var matchEndLayout: LinearLayout
    private lateinit var matchWinnerText: TextView
    private lateinit var btnPlayAgain: Button
    private lateinit var btnChooseOffline: Button
    private lateinit var btnMainMenu: Button

    private var currentPlayer = "X"
    private var roundWinner: String? = null
    private var player1Score = 0
    private var player2Score = 0
    private var roundsToWin = 3

    private val player1Color = 0xFF00E7A2.toInt()
    private val player2Color = 0xFF26B39E.toInt()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.tic_tac_toe_screen, container, false)

        turnText = view.findViewById(R.id.text_turn)
        winnerLayout = view.findViewById(R.id.layout_winner)
        winnerText = view.findViewById(R.id.text_winner)
        score1Text = view.findViewById(R.id.text_score1)
        score2Text = view.findViewById(R.id.text_score2)
        player1Label = view.findViewById(R.id.text_player1)
        player2Label = view.findViewById(R.id.text_player2)
        btnContinue = view.findViewById(R.id.button_continue)
        btnGiveUp = view.findViewById(R.id.button_giveup)

        matchEndLayout = view.findViewById(R.id.layout_match_end)
        matchWinnerText = view.findViewById(R.id.text_match_winner)
        btnPlayAgain = view.findViewById(R.id.button_play_again)
        btnChooseOffline = view.findViewById(R.id.button_choose_offline)
        btnMainMenu = view.findViewById(R.id.button_main_menu)

        board = Array(3) { row ->
            Array(3) { col ->
                val buttonId = resources.getIdentifier("button$row$col", "id", requireContext().packageName)
                val btn: Button = view.findViewById(buttonId)
                btn.setOnClickListener { onCellClicked(row, col) }
                btn
            }
        }

        view.findViewById<Button>(R.id.button_restart_round).setOnClickListener {
            resetBoard()
        }

        btnContinue.setOnClickListener {
            winnerLayout.visibility = View.GONE
            resetBoard()
        }
        btnGiveUp.setOnClickListener {
            player1Score = 0
            player2Score = 0
            winnerLayout.visibility = View.GONE
            resetBoard()
        }

        btnPlayAgain.setOnClickListener {
            matchEndLayout.visibility = View.GONE
            winnerLayout.visibility = View.GONE
            player1Score = 0
            player2Score = 0
            resetBoard()
        }
        btnChooseOffline.setOnClickListener {
            parentFragmentManager.popBackStack() // to your offline games menu fragment
        }
        btnMainMenu.setOnClickListener {
            parentFragmentManager.popBackStack(null, 1)
        }

        updateTurnIndicator()
        updateScore()
        return view
    }

    private fun onCellClicked(row: Int, col: Int) {
        val button = board[row][col]
        if (button.text.isNotEmpty() || roundWinner != null) return

        button.text = currentPlayer
        button.setTextColor(if (currentPlayer == "X") player1Color else player2Color)
        button.animate().rotationYBy(360f).setDuration(200).start()

        if (checkWin()) {
            roundWinner = currentPlayer
            val winnerIsPlayer1 = (currentPlayer == "X")
            if (winnerIsPlayer1) player1Score++ else player2Score++
            updateScore()
            lockBoard()
            if (player1Score == roundsToWin || player2Score == roundsToWin) {
                // Show match winner dialog only, don't show round winner dialog
                showMatchWinner()
            } else {
                // Show round winner dialog
                showWinner(
                    "Player ${if (winnerIsPlayer1) "1" else "2"} Wins!",
                    if (winnerIsPlayer1) player1Color else player2Color
                )
            }
        } else if (isBoardFull()) {
            showWinner("It's a draw!", 0xFF757575.toInt())
            lockBoard()
        } else {
            currentPlayer = if (currentPlayer == "X") "O" else "X"
            updateTurnIndicator()
        }
    }

    private fun showWinner(message: String, color: Int) {
        winnerText.text = message
        winnerText.setTextColor(color)
        winnerLayout.visibility = View.VISIBLE
        matchEndLayout.visibility = View.GONE
        btnContinue.visibility = View.VISIBLE
        btnGiveUp.visibility = View.VISIBLE
    }

    private fun showMatchWinner() {
        matchWinnerText.text = "Player ${if (player1Score == roundsToWin) "1" else "2"} Wins the Match!"
        matchWinnerText.setTextColor(if (player1Score == roundsToWin) player1Color else player2Color)
        winnerLayout.visibility = View.VISIBLE
        matchEndLayout.visibility = View.VISIBLE
        btnContinue.visibility = View.GONE
        btnGiveUp.visibility = View.GONE
    }

    private fun checkWin(): Boolean {
        for (i in 0..2) {
            if (board[i][0].text == currentPlayer &&
                board[i][1].text == currentPlayer &&
                board[i][2].text == currentPlayer
            ) return true

            if (board[0][i].text == currentPlayer &&
                board[1][i].text == currentPlayer &&
                board[2][i].text == currentPlayer
            ) return true
        }
        if (board[0][0].text == currentPlayer &&
            board[1][1].text == currentPlayer &&
            board[2][2].text == currentPlayer
        ) return true

        if (board[0][2].text == currentPlayer &&
            board[1][1].text == currentPlayer &&
            board[2][0].text == currentPlayer
        ) return true

        return false
    }

    private fun isBoardFull(): Boolean {
        return board.all { row -> row.all { it.text.isNotEmpty() } }
    }

    private fun resetBoard() {
        for (row in board) for (cell in row) {
            cell.text = ""
            cell.isEnabled = true
            cell.setTextColor(player1Color)
            cell.rotationY = 0f
        }
        roundWinner = null
        currentPlayer = "X"
        updateTurnIndicator()
        winnerLayout.visibility = View.GONE
        matchEndLayout.visibility = View.GONE
    }

    private fun lockBoard() {
        for (row in board) for (cell in row) cell.isEnabled = false
    }

    private fun updateScore() {
        score1Text.text = " $player1Score"
        score2Text.text = " $player2Score"
    }

    private fun updateTurnIndicator() {
        turnText.text = "Player ${if (currentPlayer == "X") "1" else "2"}'s turn ($currentPlayer)"
        turnText.setTextColor(if (currentPlayer == "X") player1Color else player2Color)
        player1Label.setTextColor(if (currentPlayer == "X") player1Color else 0xFF757575.toInt())
        player2Label.setTextColor(if (currentPlayer == "O") player2Color else 0xFF757575.toInt())
    }
}