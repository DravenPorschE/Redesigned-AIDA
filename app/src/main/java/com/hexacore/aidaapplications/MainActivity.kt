package com.hexacore.aidaapplications

import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat

import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var scrollSidePanel: ScrollView
    private lateinit var draggableButton: Button
    private lateinit var draggableContainer: LinearLayout
    private lateinit var mainContentContainer: LinearLayout

    private var sidePanelMinWidth = 130 // dp → converted later
    private var sidePanelMaxWidth = 400 // dp → you can change this
    private var lastX = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Get controller for window insets
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        scrollSidePanel = findViewById(R.id.scroll_side_panel)
        draggableButton = findViewById(R.id.draggable_button)
        draggableContainer = findViewById(R.id.draggable_button_container)
        mainContentContainer = findViewById(R.id.main_content_container)

        // convert dp to pixels
        sidePanelMinWidth = dpToPx(sidePanelMinWidth)
        sidePanelMaxWidth = dpToPx(sidePanelMaxWidth)

        draggableButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastX
                    resizeSidePanel(dx)
                    lastX = event.rawX
                    true
                }
                else -> false
            }
        }

        val noteButton: ImageButton = findViewById(R.id.note_app_button)
        val alarmButton: ImageButton = findViewById(R.id.alarm_app_button)
        val calendarButton: ImageButton = findViewById(R.id.calendar_app_button)
        val meetingButton: ImageButton = findViewById(R.id.meeting_app_button)
        val gameButton: ImageButton = findViewById(R.id.game_app_button)
        val configureButton: ImageButton = findViewById(R.id.configure_app_button)

        supportFragmentManager.beginTransaction()
            .replace(R.id.main_content, DefaultScreen())
            .commit()

        // ADD THIS BLOCK FOR GAME BUTTON
        gameButton.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_content, GameMenuScreen())
                .commit()
        }

        /*

        Paano mag open ng app sa may main content na screen (Yung sa right side na screen)

        for example:

        noteButton.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_content, NoteAppScreen())
                .commit()
        }

        yung "main_content" is yung papalitan ng bagong screen
        yung "NoteApp()" is yung screen na ipapalit.

        Kaya before mo gawin yan gawa ka muna ng dalawang file
        Dun sa may app/kotlin+java/con.hexacore.aidapplications

        right click ka dun and new > Kotlin Class file tapos ang name is for example NoteAppScreen

        Tapos dun sa may layout na folder, gawa ka din ng another xml file and ang ilalagay na name is
        something similar dun sa pag gagamitan mo, in this case ang name ng file is note_app.xml

        yung code nung dalawang file is copy mo na lang yung sa may DefaultScreen at default_screen.xml

        kasi similar functions lang din naman ang mangyayari, at yung mismong functionality ng note app
        is ilalagay mo sa loob ng NoteAppScreen na file and yung magiging itsura ng Note app is nandun
        sa may note_app.xml


         */
    }
    private fun resizeSidePanel(dx: Float) {
        val layoutParams = scrollSidePanel.layoutParams as LinearLayout.LayoutParams
        var newWidth = layoutParams.width + dx.toInt()

        // clamp between min and max
        if (newWidth < sidePanelMinWidth) newWidth = sidePanelMinWidth
        if (newWidth > sidePanelMaxWidth) newWidth = sidePanelMaxWidth

        layoutParams.width = newWidth
        scrollSidePanel.layoutParams = layoutParams
        scrollSidePanel.requestLayout()
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}