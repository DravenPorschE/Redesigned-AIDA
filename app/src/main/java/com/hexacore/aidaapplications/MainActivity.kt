package com.hexacore.aidaapplications

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat

import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel

class MainActivity : AppCompatActivity() {

    private lateinit var scrollSidePanel: ScrollView
    private lateinit var draggableButton: Button
    private lateinit var mainContentContainer: LinearLayout
    private lateinit var tts: TextToSpeech

    private lateinit var mainContent: FrameLayout

    private var sidePanelMinWidthDp = 130
    private var sidePanelMaxWidthDp = 400
    private var lastX = 0f
    private var sidePanelMinWidth = 0
    private var sidePanelMaxWidth = 0
    private val RECORD_AUDIO_REQUEST = 100
    private val REQUEST_CODE_SPEECH = 100
    private var openGoogleSTT = false

    private var isSpeaking = false
    private lateinit var wakeWordManager: WakeWordManager

    private lateinit var tflite: Interpreter
    private lateinit var words: List<String>
    private lateinit var classes: List<String>

    private var aidaResponses = arrayOf("What can i do for you today?", "How can i help you?", "What can i do for you?", "How can i assist you?", "What can i do today?", "How can i help you today?", "What can i do for today?", "How can i assist you today?", "What can i do today?")

    // 🔔 Alarm banner
    private lateinit var alarmBanner: LinearLayout
    private lateinit var alarmBannerText: TextView
    private lateinit var alarmBannerDismiss: Button

    // Track whether panel is open
    private var isPanelOpen = false

    // Local broadcast receivers
    private val alarmRingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra("message") ?: "⏰ Alarm is ringing!"
            runOnUiThread { showAlarmBanner(message) }
        }
    }
    private val alarmDismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            runOnUiThread { hideAlarmBanner() }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
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
        mainContent = findViewById(R.id.main_content)

        sidePanelMinWidth = dpToPx(sidePanelMinWidthDp)
        sidePanelMaxWidth = dpToPx(sidePanelMaxWidthDp)

        // 🗣️ Text-to-Speech setup
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    println("❌ TTS language not supported.")
                } else {
                    // ✅ Attach listener here
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isSpeaking = true
                        }

                        override fun onDone(utteranceId: String?) {
                            isSpeaking = false
                        }

                        override fun onError(utteranceId: String?) {
                            isSpeaking = false
                        }
                    })
                }
            } else {
                println("❌ TTS initialization failed.")
            }
        }

        var isDragging = false

        draggableButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    isDragging = false

                    // Always reset to full opacity when touched
                    draggableButton.animate().alpha(1f).setDuration(100).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    isDragging = true

                    // Keep button fully visible while dragging
                    if (draggableButton.alpha != 1f) {
                        draggableButton.alpha = 1f
                    }

                    val dx = event.rawX - lastX
                    if (isPortrait()) {
                        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
                        val panelWidth = scrollSidePanel.width.toFloat()
                        var newButtonX = draggableButton.x + dx
                        val minX = screenWidth - panelWidth - draggableButton.width
                        val maxX = screenWidth - draggableButton.width
                        newButtonX = newButtonX.coerceIn(minX, maxX)
                        draggableButton.x = newButtonX
                        scrollSidePanel.x = newButtonX + draggableButton.width
                    } else {
                        resizeSidePanel(dx)
                    }
                    lastX = event.rawX
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!isDragging) {
                        v.performClick()
                    }

                    // Fade back after 1s
                    draggableButton.animate()
                        .alpha(0.4f)
                        .setStartDelay(1000)
                        .setDuration(500)
                        .start()
                    true
                }
                else -> false
            }
        }


        // Optional click to toggle
        draggableButton.setOnClickListener {
            if (isPortrait()) {
                val screenWidth = resources.displayMetrics.widthPixels.toFloat()
                val panelWidth = scrollSidePanel.width.toFloat()
                val fullyShownX = screenWidth - panelWidth - draggableButton.width
                val fullyHiddenX = screenWidth - draggableButton.width
                val targetButtonX = if (draggableButton.x > fullyShownX + 10f) fullyShownX else fullyHiddenX
                draggableButton.animate().x(targetButtonX).setDuration(200).start()
                scrollSidePanel.animate().x(targetButtonX + draggableButton.width).setDuration(200).start()
            }
        }

        // Set initial positions
        if (isPortrait()) {
            scrollSidePanel.post {
                val screenWidth = resources.displayMetrics.widthPixels.toFloat()
                val buttonWidth = draggableButton.width.toFloat()
                val buttonHeight = draggableButton.height.toFloat()
                val screenHeight = resources.displayMetrics.heightPixels.toFloat()

                // Place panel off-screen to the right
                scrollSidePanel.x = screenWidth

                // Place button just at left edge of panel
                draggableButton.x = scrollSidePanel.x - buttonWidth

                // Center button vertically
                draggableButton.y = (screenHeight - buttonHeight) / 2
            }
        } else {
            scrollSidePanel.post {
                // Hide panel (width = 0)
                val layoutParams = scrollSidePanel.layoutParams
                layoutParams.width = 0
                scrollSidePanel.layoutParams = layoutParams
                scrollSidePanel.requestLayout()

                // Place button at the very left edge
                val buttonParams = draggableButton.layoutParams as FrameLayout.LayoutParams
                buttonParams.marginStart = 0
                draggableButton.layoutParams = buttonParams

                // Center button vertically
                val buttonHeight = draggableButton.height.toFloat()
                val screenHeight = resources.displayMetrics.heightPixels.toFloat()
                draggableButton.y = (screenHeight - buttonHeight) / 2
            }
        }



        val noteButton: ImageButton = findViewById(R.id.note_app_button)
        val alarmButton: ImageButton = findViewById(R.id.alarm_app_button)
        val calendarButton: ImageButton = findViewById(R.id.calendar_app_button)
        val meetingButton: ImageButton = findViewById(R.id.meeting_app_button)
        val gameButton: ImageButton = findViewById(R.id.game_app_button)
        val configureButton: ImageButton = findViewById(R.id.configure_app_button)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_content, DefaultScreen())
                .commit()
        }

        gameButton.setOnClickListener { resetSidePanel(); replaceFragment(GameMenuScreen(), "GameMenu") }
        noteButton.setOnClickListener { resetSidePanel(); replaceFragment(NoteAppScreen(), "Note") }
        alarmButton.setOnClickListener { resetSidePanel(); replaceFragment(AlarmAppScreen(), "Alarm") }
        configureButton.setOnClickListener { resetSidePanel(); replaceFragment(ConfigureAppScreen(), "Config") }
        calendarButton.setOnClickListener { resetSidePanel(); replaceFragment(CalendarAppScreen(), "Calendar") }

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
            wakeWordManager.initWakeWord()
        }

        // 🔔 alarm banner
        alarmBanner = findViewById(R.id.alarm_banner)
        alarmBannerText = findViewById(R.id.alarm_banner_text)
        alarmBannerDismiss = findViewById(R.id.alarm_banner_dismiss)
        alarmBanner.visibility = View.GONE

        alarmBannerDismiss.setOnClickListener {
            stopService(Intent(this, AlarmService::class.java))
            LocalBroadcastManager.getInstance(this)
                .sendBroadcast(Intent("com.hexacore.aidaapplications.ALARM_DISMISSED"))
            hideAlarmBanner()
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
            alarmRingReceiver, IntentFilter("com.hexacore.aidaapplications.ALARM_RINGING")
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            alarmDismissReceiver, IntentFilter("com.hexacore.aidaapplications.ALARM_DISMISSED")
        )
    }
    // Resize panel in landscape
    private fun resizeSidePanel(dx: Float) {
        val layoutParams = scrollSidePanel.layoutParams
        var newWidth = layoutParams.width + dx.toInt()

        // clamp between 0 and max
        if (newWidth < 0) newWidth = 0
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

            // Reset panel fully hidden off-screen
            scrollSidePanel.x = screenWidth

            // Reset button just at edge of panel
            draggableButton.x = scrollSidePanel.x - buttonWidth

        } else {
            // Landscape: completely hide
            val layoutParams = scrollSidePanel.layoutParams
            layoutParams.width = 0
            scrollSidePanel.layoutParams = layoutParams
            scrollSidePanel.requestLayout()

            val buttonParams = draggableButton.layoutParams as FrameLayout.LayoutParams
            buttonParams.marginStart = 0
            draggableButton.layoutParams = buttonParams
            draggableButton.translationX = 0f
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
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

    private fun showAlarmBanner(message: String) {
        alarmBannerText.text = message
        alarmBanner.visibility = View.VISIBLE
        alarmBanner.post {
            alarmBanner.translationY = -alarmBanner.height.toFloat()
            alarmBanner.animate().translationY(0f).setDuration(300).start()
        }
    }
    private fun hideAlarmBanner() {
        alarmBanner.post {
            alarmBanner.animate().translationY(-alarmBanner.height.toFloat()).setDuration(300).withEndAction {
                alarmBanner.visibility = View.GONE
                alarmBanner.translationY = 0f
            }.start()
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

    private fun replaceFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_content, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    fun openScreen(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_content, fragment)
            .commit()
    }

}