package com.hexacore.aidaapplications

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.gridlayout.widget.GridLayout
import androidx.fragment.app.Fragment
import android.graphics.Typeface
import androidx.appcompat.app.AlertDialog

class MemoryGameScreen : Fragment() {

    private lateinit var grid: GridLayout
    private lateinit var movesText: TextView
    private lateinit var chooseDifficultyButton: Button
    private lateinit var restartButton: Button
    private lateinit var backButton: Button

    private var numPairs = 7
    private var numColumns = 4

    private var cards: List<MemoryCard> = listOf()
    private var cardButtons: List<Button> = listOf()
    private var firstSelected: Int? = null
    private var moves = 0
    private var matches = 0
    private var isBusy = false

    private val symbols = listOf(
        "🍎","🍌","🍇","🍉","🍓","🍒","🍍","🍑","🍋","🍊",
        "🍈","🥝","🍐","🥥","🍅","🥑","🍆","🥔","🥕","🌽",
        "🥦","🧄","🧅","🥬","🌶","🥒","🍄","🥜","🌰","🍞"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.memory_game_screen, container, false)
        grid = view.findViewById(R.id.memory_grid)
        movesText = view.findViewById(R.id.memory_moves)
        chooseDifficultyButton = view.findViewById(R.id.memory_choose_difficulty)
        restartButton = view.findViewById(R.id.memory_restart)
        backButton = view.findViewById(R.id.memory_back)

        chooseDifficultyButton.setOnClickListener { showDifficultyDialog() }
        restartButton.setOnClickListener { setupGame(numPairs) }
        backButton.setOnClickListener { parentFragmentManager.popBackStack() }

        setupGame(numPairs)
        return view
    }

    private fun showDifficultyDialog() {
        val difficulties = arrayOf("Easy (7 pairs)", "Medium (13 pairs)", "Hard (20 pairs)")
        AlertDialog.Builder(requireContext())
            .setTitle("Choose Difficulty")
            .setItems(difficulties) { _, which ->
                numPairs = when (which) {
                    0 -> 7
                    1 -> 13
                    2 -> 20
                    else -> 7
                }
                setupGame(numPairs)
            }.show()
    }

    private fun setupGame(pairs: Int) {
        moves = 0
        matches = 0
        movesText.text = "Moves: 0"

        // Prepare card data
        val pickedSymbols = symbols.shuffled().take(pairs)
        val allSymbols = (pickedSymbols + pickedSymbols).shuffled()
        cards = allSymbols.mapIndexed { idx, sym -> MemoryCard(idx, sym) }

        // Setup grid
        grid.removeAllViews()
        val totalCards = cards.size
        val columns = if (pairs > 13) 5 else 4 // hard mode: 5 columns
        grid.columnCount = columns
        grid.rowCount = (totalCards + columns - 1) / columns

        // Dynamically create buttons for cards
        cardButtons = List(totalCards) { idx ->
            Button(requireContext()).apply {
                text = ""
                textSize = 28f
                setBackgroundColor(Color.parseColor("#2962FF")) // Card Back: blue
                setTextColor(Color.WHITE)
                isAllCaps = false
                setOnClickListener { onCardClicked(idx) }
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
                layoutParams = params
            }
        }

        // Add buttons to grid
        cardButtons.forEach { grid.addView(it) }
        updateViews()
    }

    private fun onCardClicked(idx: Int) {
        if (isBusy) return
        val card = cards[idx]
        if (card.isFaceUp || card.isMatched) return

        card.isFaceUp = true
        updateViews()

        if (firstSelected == null) {
            firstSelected = idx
        } else {
            moves++
            movesText.text = "Moves: $moves"
            val firstIdx = firstSelected!!
            val firstCard = cards[firstIdx]
            if (firstCard.symbol == card.symbol) {
                // Match!
                firstCard.isMatched = true
                card.isMatched = true
                matches++
                firstSelected = null
                if (matches == cards.size / 2) {
                    showWinDialog()
                }
            } else {
                isBusy = true
                Handler(Looper.getMainLooper()).postDelayed({
                    firstCard.isFaceUp = false
                    card.isFaceUp = false
                    updateViews()
                    firstSelected = null
                    isBusy = false
                }, 800)
            }
        }
        updateViews()
    }

    private fun updateViews() {
        for (i in cards.indices) {
            val card = cards[i]
            val btn = cardButtons[i]
            if (card.isMatched) {
                btn.text = card.symbol
                btn.setBackgroundColor(Color.parseColor("#43A047")) // Green for matched
                btn.isEnabled = false
            } else if (card.isFaceUp) {
                btn.text = card.symbol
                btn.setBackgroundColor(Color.parseColor("#FFFFFF")) // Face up: white
                btn.setTextColor(Color.parseColor("#2962FF"))
            } else {
                btn.text = ""
                btn.setBackgroundColor(Color.parseColor("#2962FF")) // Card back: blue
                btn.setTextColor(Color.WHITE)
                btn.isEnabled = true
            }
        }
    }

    private fun showWinDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .create()
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(48, 48, 48, 48)
        layout.setBackgroundColor(Color.parseColor("#222222"))

        val message = TextView(requireContext())
        message.text = "🎉 Great memory! 🎉\nYou finished in $moves moves."
        message.textSize = 26f
        message.setTextColor(Color.parseColor("#FFEB3B"))
        message.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        message.setPadding(0, 0, 0, 32)
        message.setTypeface(null, Typeface.BOLD)

        val playAgainBtn = Button(requireContext())
        playAgainBtn.text = "Play Again"
        playAgainBtn.setOnClickListener {
            dialog.dismiss()
            showDifficultyDialog()
        }

        val backBtn = Button(requireContext())
        backBtn.text = "Back to Main"
        backBtn.setOnClickListener {
            dialog.dismiss()
            parentFragmentManager.popBackStack()
        }

        layout.addView(message)
        layout.addView(playAgainBtn)
        layout.addView(backBtn)
        layout.gravity = android.view.Gravity.CENTER

        dialog.setView(layout)
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}