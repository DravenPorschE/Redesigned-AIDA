
class GameMenuScreen : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.game_menu_screen, container, false)

        val offlineGamesButton: Button = view.findViewById(R.id.button_offline_games)
        val offlineGamesLayout: LinearLayout = view.findViewById(R.id.layout_offline_games)

        // Hide at start
        offlineGamesLayout.visibility = View.GONE

        offlineGamesButton.setOnClickListener {
            offlineGamesLayout.visibility =
                if (offlineGamesLayout.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        val ticTacToeButton: Button = view.findViewById(R.id.button_tictactoe)
        ticTacToeButton.setOnClickListener {
            openGame(TicTacToeScreen())
        }

        val sudokuButton: Button = view.findViewById(R.id.button_sudoku)
        sudokuButton.setOnClickListener {
            openGame(SudokuScreen())
        }

        // Memory Game button (optional)
        val memoryGameButton: Button = view.findViewById(R.id.button_memory_game)
        memoryGameButton.setOnClickListener {
            openGame(MemoryGameScreen())
        }

        return view
    }

    fun openGame(game: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_content, game)
            .addToBackStack(null)
            .commit()
    }
}