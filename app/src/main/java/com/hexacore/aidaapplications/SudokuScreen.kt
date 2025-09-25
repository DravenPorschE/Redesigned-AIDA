package com.hexacore.aidaapplications

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.Editable
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.gridlayout.widget.GridLayout

class SudokuScreen : Fragment() {

    private lateinit var sudokuGrid: GridLayout
    private lateinit var numberSelector: LinearLayout
    private lateinit var buttonCheck: Button

    private lateinit var layoutResult: LinearLayout
    private lateinit var textResult: TextView
    private lateinit var btnPlayAgain: Button
    private lateinit var btnChooseOffline: Button
    private lateinit var btnMainMenu: Button

    private var board = Array(9) { IntArray(9) }
    private var solution = Array(9) { IntArray(9) }
    private val cellViews = Array(9) { arrayOfNulls<EditText>(9) }
    private var currentDifficulty = 0 // 0: easy, 1: medium, 2: hard

    // Color palette
    private val prefilledColor = 0xFF212121.toInt()
    private val userInputColor = 0xFF00E7A2.toInt()
    private val highlightColor = 0xFFB2F1E4.toInt()
    private val matchNumberColor = 0xFF26B39E.toInt()
    private val errorColor = 0xFFFFA726.toInt()
    private val selectedNumberBg = 0xFFB2F1E4.toInt()
    private val cellBg1 = 0xFFFFFFFF.toInt()
    private val cellBg2 = 0xFFF2F2F2.toInt()

    private val errorCells = mutableSetOf<Pair<Int, Int>>()
    private var highlightNumber: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.sudoku_screen, container, false)
        sudokuGrid = view.findViewById(R.id.sudoku_grid)
        numberSelector = view.findViewById(R.id.number_selector)
        buttonCheck = view.findViewById(R.id.button_check)
        layoutResult = view.findViewById(R.id.layout_result)
        textResult = view.findViewById(R.id.text_result)
        btnPlayAgain = view.findViewById(R.id.button_play_again)
        btnChooseOffline = view.findViewById(R.id.button_choose_offline)
        btnMainMenu = view.findViewById(R.id.button_main_menu)

        sudokuGrid.visibility = View.GONE
        numberSelector.visibility = View.GONE
        buttonCheck.visibility = View.GONE

        showDifficultyDialog()

        setupNumberSelector()
        buttonCheck.setOnClickListener { checkSolution() }
        btnPlayAgain.setOnClickListener {
            layoutResult.visibility = View.GONE
            showDifficultyDialog()
        }
        btnChooseOffline.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        btnMainMenu.setOnClickListener {
            parentFragmentManager.popBackStack(null, 1)
        }

        return view
    }

    private fun showDifficultyDialog() {
        val difficulties = arrayOf("Easy", "Medium", "Hard")
        AlertDialog.Builder(requireContext())
            .setTitle("Choose Difficulty")
            .setItems(difficulties) { _, which ->
                currentDifficulty = which
                sudokuGrid.visibility = View.VISIBLE
                numberSelector.visibility = View.VISIBLE
                buttonCheck.visibility = View.VISIBLE
                layoutResult.visibility = View.GONE
                generatePuzzle()
            }
            .setCancelable(false)
            .show()
    }

    private fun generatePuzzle() {
        val (puzzle, solved) = SudokuGenerator.generate(currentDifficulty)
        board = puzzle
        solution = solved
        errorCells.clear()
        highlightNumber = null
        renderBoard()
        highlightNumberSelector(-1)
    }

    private fun renderBoard() {
        sudokuGrid.removeAllViews()
        sudokuGrid.rowCount = 9
        sudokuGrid.columnCount = 9

        for (i in 0..8) {
            for (j in 0..8) {
                val params = GridLayout.LayoutParams()
                params.width = dp(38)
                params.height = dp(38)
                params.setMargins(
                    if (j % 3 == 0) dp(4) else dp(1),
                    if (i % 3 == 0) dp(4) else dp(1),
                    if (j == 8) dp(4) else 0,
                    if (i == 8) dp(4) else 0
                )

                val et = EditText(requireContext())
                et.gravity = Gravity.CENTER
                et.textSize = 18f
                et.layoutParams = params
                et.filters = arrayOf(InputFilter.LengthFilter(1))
                et.inputType = InputType.TYPE_CLASS_NUMBER

                if (board[i][j] != 0) {
                    et.setText(board[i][j].toString())
                    et.isFocusable = false
                    et.isFocusableInTouchMode = false
                    et.isClickable = false
                    et.setTextColor(prefilledColor)
                    et.setBackgroundColor(getCellBackgroundColor(i, j, false))
                } else {
                    et.setText("")
                    et.setTextColor(userInputColor)
                    et.setBackgroundColor(getCellBackgroundColor(i, j, false))
                    et.isFocusable = true
                    et.isFocusableInTouchMode = true
                    et.isClickable = true

                    et.setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus && !et.text.isNullOrEmpty()) {
                            highlightNumbers(et.text.toString().toIntOrNull())
                        } else if (hasFocus) {
                            clearHighlights()
                        }
                    }
                    et.addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable?) {
                            val num = s?.toString()?.toIntOrNull()
                            highlightNumbers(num)
                            clearErrors()
                        }
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    })
                }
                cellViews[i][j] = et
                sudokuGrid.addView(et)
            }
        }
        clearHighlights()
    }

    private fun setupNumberSelector() {
        numberSelector.removeAllViews()
        for (n in 1..9) {
            val btn = Button(requireContext())
            btn.text = n.toString()
            btn.setTextColor(userInputColor)
            btn.textSize = 16f
            btn.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.white)
            btn.setOnClickListener {
                highlightNumber = n
                highlightNumberSelector(n)
                highlightNumbers(n)
            }
            btn.layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                setMargins(dp(4), 0, dp(4), 0)
            }
            btn.tag = n
            numberSelector.addView(btn)
        }
    }

    private fun highlightNumberSelector(selected: Int) {
        for (i in 0 until numberSelector.childCount) {
            val btn = numberSelector.getChildAt(i) as Button
            val num = btn.tag as? Int
            if (selected != -1 && num == selected) {
                btn.setBackgroundColor(selectedNumberBg)
            } else {
                btn.setBackgroundColor(cellBg1)
            }
        }
    }

    private fun highlightNumbers(number: Int?) {
        for (i in 0..8) for (j in 0..8) {
            val et = cellViews[i][j] ?: continue
            if (number != null && et.text?.toString() == number.toString() && et.text.isNotEmpty()) {
                et.setBackgroundColor(matchNumberColor)
            } else if (errorCells.contains(i to j)) {
                et.setBackgroundColor(errorColor)
            } else {
                et.setBackgroundColor(getCellBackgroundColor(i, j, false))
            }
        }
    }

    private fun clearHighlights() {
        highlightNumber = null
        highlightNumberSelector(-1)
        for (i in 0..8) for (j in 0..8) {
            val et = cellViews[i][j] ?: continue
            if (errorCells.contains(i to j)) {
                et.setBackgroundColor(errorColor)
            } else {
                et.setBackgroundColor(getCellBackgroundColor(i, j, false))
            }
        }
    }

    private fun clearErrors() {
        errorCells.clear()
        clearHighlights()
    }

    private fun checkSolution() {
        errorCells.clear()
        val userBoard = Array(9) { IntArray(9) }
        var complete = true
        for (i in 0..8) for (j in 0..8) {
            val value = cellViews[i][j]?.text?.toString()?.toIntOrNull() ?: 0
            userBoard[i][j] = value
            if (board[i][j] == 0 && value == 0) complete = false
        }

        if (!complete) {
            textResult.text = "Please fill in all the cells before checking."
            textResult.setTextColor(errorColor)
            layoutResult.visibility = View.VISIBLE
            clearHighlights()
            return
        }

        var hasError = false
        for (i in 0..8) for (j in 0..8) {
            if (board[i][j] == 0 && userBoard[i][j] != solution[i][j]) {
                errorCells.add(i to j)
                hasError = true
            }
        }

        if (!hasError) {
            textResult.text = "Congratulations! 🎉"
            textResult.setTextColor(userInputColor)
        } else {
            textResult.text = "Some numbers are incorrect! Check highlighted cells."
            textResult.setTextColor(errorColor)
        }
        layoutResult.visibility = View.VISIBLE
        clearHighlights()
    }

    private fun getCellBackgroundColor(i: Int, j: Int, selected: Boolean): Int {
        val block = (i / 3 + j / 3) % 2
        return when {
            selected -> highlightColor
            block == 0 -> cellBg1
            else -> cellBg2
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}