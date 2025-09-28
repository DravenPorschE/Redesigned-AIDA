package com.hexacore.aidaapplications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {

    private lateinit var scrollSidePanel: ScrollView
    private lateinit var draggableButton: Button
    private lateinit var draggableContainer: LinearLayout
    private lateinit var mainContentContainer: LinearLayout

    // 🔔 Banner views
    private lateinit var alarmBanner: LinearLayout
    private lateinit var alarmBannerText: TextView
    private lateinit var alarmBannerDismiss: Button

    private var sidePanelMinWidth = 130 // dp → converted later
    private var sidePanelMaxWidth = 400 // dp → you can change this
    private var lastX = 0f

    // 🔔 Receiver to listen when alarm is triggered
    private val alarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            showAlarmBanner("Alarm is ringing!")
        }
    }

    // 🔔 Receiver to hide banner when alarm is dismissed
    private val alarmDismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            hideAlarmBanner()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 🔥 Load saved theme BEFORE calling super.onCreate / setContentView
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        when (prefs.getString("selected_theme", "Light")) {
            "Light" -> setTheme(R.style.Theme_App_Light)
            "Dark" -> setTheme(R.style.Theme_App_Dark)
            "Blue" -> setTheme(R.style.Theme_App_Blue)
            "Green" -> setTheme(R.style.Theme_App_Green)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Hide system bars
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        scrollSidePanel = findViewById(R.id.scroll_side_panel)
        draggableButton = findViewById(R.id.draggable_button)
        draggableContainer = findViewById(R.id.draggable_button_container)
        mainContentContainer = findViewById(R.id.main_content_container)

        // 🔔 Banner init
        alarmBanner = findViewById(R.id.alarm_banner)
        alarmBannerText = findViewById(R.id.alarm_banner_text)
        alarmBannerDismiss = findViewById(R.id.alarm_banner_dismiss)

        alarmBannerDismiss.setOnClickListener {
            // Stop alarm service
            stopService(Intent(this, AlarmService::class.java))

            // 🔔 Also dismiss system notification
            val bannerIntent = Intent("com.hexacore.aidaapplications.ALARM_DISMISSED")
            LocalBroadcastManager.getInstance(this).sendBroadcast(bannerIntent)
        }

        // convert dp to pixels
        sidePanelMinWidth = dpToPx(sidePanelMinWidth)
        sidePanelMaxWidth = dpToPx(sidePanelMaxWidth)

        draggableButton.setOnTouchListener { _, event ->
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

        // Default screen
        if (savedInstanceState == null) {
            replaceFragment(DefaultScreen(), "Default")
        }

        // Side panel button listeners
        configureButton.setOnClickListener {
            replaceFragment(ConfigureAppScreen(), "Config")
        }

        alarmButton.setOnClickListener {
            replaceFragment(AlarmAppScreen(), "Alarm")
        }

        // ✅ Handle back press behavior
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fm = supportFragmentManager
                if (fm.backStackEntryCount > 1) {
                    // Pop back stack normally
                    fm.popBackStack()
                } else {
                    // If only DefaultScreen is left → finish app
                    finish()
                }
            }
        })

        // ✅ Ask for POST_NOTIFICATIONS permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), 100)
            }
        }

        // 🔔 Register receivers using LocalBroadcastManager
        LocalBroadcastManager.getInstance(this).registerReceiver(
            alarmReceiver,
            IntentFilter("com.hexacore.aidaapplications.ALARM_RINGING")
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            alarmDismissReceiver,
            IntentFilter("com.hexacore.aidaapplications.ALARM_DISMISSED")
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(alarmReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(alarmDismissReceiver)
    }

    // ✅ Show banner with animation (fixed to reset position before showing)
    private fun showAlarmBanner(message: String) {
        alarmBannerText.text = message
        alarmBanner.visibility = View.VISIBLE
        alarmBanner.translationY = -alarmBanner.height.toFloat() // reset above screen
        alarmBanner.animate()
            .translationY(0f)
            .setDuration(300)
            .start()
    }

    // ✅ Hide banner with animation
    private fun hideAlarmBanner() {
        alarmBanner.animate()
            .translationY(-alarmBanner.height.toFloat())
            .setDuration(300)
            .withEndAction {
                alarmBanner.visibility = View.GONE
            }
            .start()
    }

    // ✅ Improved replaceFragment with backstack + no duplicates
    private fun replaceFragment(fragment: Fragment, tag: String) {
        val fm = supportFragmentManager
        val existingFragment = fm.findFragmentByTag(tag)

        if (existingFragment != null) {
            fm.popBackStack(tag, 0) // jump back to it
        } else {
            fm.beginTransaction()
                .replace(R.id.main_content, fragment, tag)
                .addToBackStack(tag)
                .commit()
        }
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
