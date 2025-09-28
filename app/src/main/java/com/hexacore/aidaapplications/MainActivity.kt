package com.hexacore.aidaapplications

import android.content.BroadcastReceiver
import android.content.Context
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
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
import androidx.fragment.app.Fragment
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel

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

    private lateinit var tflite: Interpreter
    private lateinit var words: List<String>
    private lateinit var classes: List<String>

    private var aidaResponses = arrayOf("What can i do for you today?", "How can i help you?", "What can i do for you?", "How can i assist you?", "What can i do today?", "How can i help you today?", "What can i do for today?", "How can i assist you today?", "What can i do today?")

    @SuppressLint("ClickableViewAccessibility")
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

        noteButton.setOnClickListener {
            resetSidePanel()

            supportFragmentManager.beginTransaction()
                .replace(R.id.main_content, NoteAppScreen())
                .commit()
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
        yung code nung dalawang file is copy mo na lang yung sa may DefaultScreen at default_screen.xml

        kasi similar functions lang din naman ang mangyayari, at yung mismong functionality ng note app
        is ilalagay mo sa loob ng NoteAppScreen na file and yung magiging itsura ng Note app is nandun
        sa may note_app.xml


         */



        // Initialize WakeWordManager with the wake word callback
        wakeWordManager = WakeWordManager(this) {
            runOnUiThread {
                speakAndThenListen(aidaResponses.random())
            }
        }
        wakeWordManager.initWakeWord()
        wakeWordManager.start()

        initializeModel(this)
        // Initialize Text-to-Speech once when the app starts
        TTSManager.init(this) {
            Log.d("TTS", "TextToSpeech initialized and ready")
        }

        // Request microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_REQUEST
            )
        } else {
            fm.beginTransaction()
                .replace(R.id.main_content, fragment, tag)
                .addToBackStack(tag)
                .commit()
        }
    }

            wakeWordManager.initWakeWord()
        }
    }
    // Resize panel in landscape
    private fun resizeSidePanel(dx: Float) {
        val layoutParams = scrollSidePanel.layoutParams as LinearLayout.LayoutParams
        var newWidth = layoutParams.width + dx.toInt()

        // clamp between min and max
        if (newWidth < sidePanelMinWidth) newWidth = sidePanelMinWidth
        if (newWidth > sidePanelMaxWidth) newWidth = sidePanelMaxWidth

        layoutParams.width = newWidth
        scrollSidePanel.layoutParams = layoutParams
        scrollSidePanel.requestLayout()

        // Update draggable button position
        val buttonParams = draggableButton.layoutParams as FrameLayout.LayoutParams
        buttonParams.marginStart = newWidth
        draggableButton.layoutParams = buttonParams
    }

    // Reset side panel
    fun resetSidePanel() {
        if (isPortrait()) {
            val screenWidth = resources.displayMetrics.widthPixels.toFloat()
            val buttonWidth = draggableButton.width.toFloat()
            val panelWidth = scrollSidePanel.width.toFloat()

            // Reset panel fully hidden off-screen
            scrollSidePanel.x = screenWidth

            // Reset button just at edge of panel
            draggableButton.x = scrollSidePanel.x - buttonWidth

        } else {
            // Landscape logic (resize panel + margin)
            val layoutParams = scrollSidePanel.layoutParams
            layoutParams.width = sidePanelMinWidth
            scrollSidePanel.layoutParams = layoutParams
            scrollSidePanel.requestLayout()

            val buttonParams = draggableButton.layoutParams as FrameLayout.LayoutParams
            buttonParams.marginStart = sidePanelMinWidth
            draggableButton.layoutParams = buttonParams
            draggableButton.translationX = 0f
        }
    }



    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}

    private fun isPortrait(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    override fun onResume() {
        super.onResume()
        wakeWordManager.start()
    }

    override fun onPause() {
        super.onPause()
        wakeWordManager.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeWordManager.release()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            wakeWordManager.initWakeWord()
        }
    }

    private fun speakAndThenListen(text: String) {
        openGoogleSTT = true
        if (::tts.isInitialized) {
            tts.setPitch(1.0f)
            tts.setSpeechRate(1.0f)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { isSpeaking = true }
                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    runOnUiThread { startSpeechToText() }
                }
                override fun onError(utteranceId: String?) { isSpeaking = false }
            })
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")
        }
    }


    // MainActivity.kt
    private fun startSpeechToText() {
        GoogleSTT(this).askSpeechInput()
    }

    fun loadJsonArray(context: Context, fileName: String): List<String> {
        val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        return org.json.JSONArray(jsonString).let { array ->
            List(array.length()) { i -> array.getString(i) }
        }
    }

    fun initializeModel(context: Context) {
        // Load model
        val model = context.assets.open("intent_detection_model.tflite").use { input ->
            input.readBytes()
        }
        val assetFileDescriptor = context.assets.openFd("intent_detection_model.tflite")
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        tflite = Interpreter(modelBuffer)

        // Load words and classes
        words = loadJsonArray(context, "words.json")
        classes = loadJsonArray(context, "classes.json")
    }

    fun predictIntent(sentence: String): Pair<String, Float> {
        val input = preprocessInput(sentence)
        val inputArray = arrayOf(input)
        val output = Array(1) { FloatArray(classes.size) }

        tflite.run(inputArray, output)

        val maxIndex = output[0].indices.maxByOrNull { output[0][it] } ?: 0
        val confidence = output[0][maxIndex]

        return classes[maxIndex] to confidence
    }

    fun preprocessInput(sentence: String): FloatArray {
        val tokens = sentence
            .lowercase(Locale.getDefault())
            .replace("[!?.,]".toRegex(), "") // remove punctuation
            .split(" ")

        val bag = FloatArray(words.size) { 0f }
        for ((i, word) in words.withIndex()) {
            if (tokens.contains(word)) {
                bag[i] = 1f
            }
        }

        return bag
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 102 && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            //Toast.makeText(this, "You said: ${result?.get(0)}", Toast.LENGTH_SHORT).show()

            val (intent, confidence) = predictIntent(result?.get(0) ?: "")

            val outputAction = OutputAction(this)
            outputAction.outputActionBasedOnIntent(intent, result?.get(0) ?: "")
        }
    }

    fun openScreen(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_content, fragment)
            .commit()
    }

}