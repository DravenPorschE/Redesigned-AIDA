package com.hexacore.aidaapplications

object SudokuGenerator {
    // Difficulty values: 0 = Easy, 1 = Medium, 2 = Hard
    // Set number of blanks for each
    private val blanksPerDifficulty = intArrayOf(35, 45, 55)

    fun generate(difficulty: Int): Pair<Array<IntArray>, Array<IntArray>> {
        val board = Array(9) { IntArray(9) }
        fillBoard(board)
        val solution = Array(9) { board[it].clone() }
        val puzzle = removeNumbers(board, blanksPerDifficulty.getOrElse(difficulty) { 35 })
        return Pair(puzzle, solution)
    }

    private fun fillBoard(board: Array<IntArray>): Boolean {
        for (i in 0..8) {
            for (j in 0..8) {
                if (board[i][j] == 0) {
                    val nums = (1..9).shuffled()
                    for (num in nums) {
                        if (isValid(board, i, j, num)) {
                            board[i][j] = num
                            if (fillBoard(board)) return true
                            board[i][j] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    private fun isValid(board: Array<IntArray>, row: Int, col: Int, n: Int): Boolean {
        for (i in 0..8) if (board[row][i] == n || board[i][col] == n) return false
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (i in 0..2) for (j in 0..2)
            if (board[boxRow + i][boxCol + j] == n) return false
        return true
    }

    private fun removeNumbers(board: Array<IntArray>, blanks: Int): Array<IntArray> {
        val puzzle = Array(9) { board[it].clone() }
        var removed = 0
        val allCells = mutableListOf<Pair<Int, Int>>()
        for (i in 0..8) for (j in 0..8) allCells.add(i to j)
        allCells.shuffle()
        for ((i, j) in allCells) {
            if (removed >= blanks) break
            val backup = puzzle[i][j]
            puzzle[i][j] = 0
            removed++
        }
        return puzzle
    }
}