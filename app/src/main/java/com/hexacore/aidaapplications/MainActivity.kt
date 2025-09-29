package com.hexacore.aidaapplications

import android.app.Activity
import android.Manifest
import android.annotation.SuppressLint
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
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var scrollSidePanel: ScrollView
    private lateinit var draggableButton: Button
    private lateinit var tts: TextToSpeech
    private lateinit var mainContent: LinearLayout

    private var sidePanelMinWidthDp = 130
    private var sidePanelMaxWidthDp = 400
    private var lastX = 0f
    private var sidePanelMinWidth = 0
    private var sidePanelMaxWidth = 0
    private val RECORD_AUDIO_REQUEST = 100
    private var openGoogleSTT = false

    private var isSpeaking = false
    private lateinit var wakeWordManager: WakeWordManager

    private lateinit var tflite: Interpreter
    private lateinit var words: List<String>
    private lateinit var classes: List<String>

    private var aidaResponses = arrayOf(
        "What can I do for you today?", "How can I help you?", "What can I do for you?",
        "How can I assist you?", "What can I do today?", "How can I help you today?",
        "What can I do for today?", "How can I assist you today?", "What can I do today?"
    )

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

        // immersive fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        scrollSidePanel = findViewById(R.id.scroll_side_panel)
        draggableButton = findViewById(R.id.draggable_button)
        mainContent = findViewById(R.id.main_content_container)

        sidePanelMinWidth = dpToPx(sidePanelMinWidthDp)
        sidePanelMaxWidth = dpToPx(sidePanelMaxWidthDp)

        // 🗣️ TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) { isSpeaking = true }
                        override fun onDone(utteranceId: String?) { isSpeaking = false }
                        override fun onError(utteranceId: String?) { isSpeaking = false }
                    })
                }
            }
        }

        // 🎛 draggable panel
        draggableButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { lastX = event.rawX; true }
                MotionEvent.ACTION_MOVE -> {
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
                MotionEvent.ACTION_UP -> {
                    if (isPortrait()) {
                        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
                        val panelWidth = scrollSidePanel.width.toFloat()
                        val fullyShownX = screenWidth - panelWidth - draggableButton.width
                        val fullyHiddenX = screenWidth - draggableButton.width
                        val mid = (fullyShownX + fullyHiddenX) / 2f

                        val targetButtonX = if (draggableButton.x <= mid) {
                            isPanelOpen = true
                            fullyShownX
                        } else {
                            isPanelOpen = false
                            fullyHiddenX
                        }
                        draggableButton.animate().x(targetButtonX).setDuration(150).start()
                        scrollSidePanel.animate().x(targetButtonX + draggableButton.width).setDuration(150).start()
                    }
                    true
                }
                else -> false
            }
        }

        draggableButton.setOnClickListener {
            if (isPortrait()) {
                val screenWidth = resources.displayMetrics.widthPixels.toFloat()
                val panelWidth = scrollSidePanel.width.toFloat()
                val fullyShownX = screenWidth - panelWidth - draggableButton.width
                val fullyHiddenX = screenWidth - draggableButton.width
                val targetButtonX = if (isPanelOpen) fullyHiddenX else fullyShownX
                isPanelOpen = !isPanelOpen
                draggableButton.animate().x(targetButtonX).setDuration(200).start()
                scrollSidePanel.animate().x(targetButtonX + draggableButton.width).setDuration(200).start()
            }
        }

        // 👉 Initial placement
        if (savedInstanceState == null) {
            draggableButton.post { placeButtonAndPanel() }
        }

        // 🔘 Side panel buttons
        val noteButton: ImageButton = findViewById(R.id.note_app_button)
        val alarmButton: ImageButton = findViewById(R.id.alarm_app_button)
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

        // 🎤 wake word
        wakeWordManager = WakeWordManager(this) {
            runOnUiThread { speakAndThenListen(aidaResponses.random()) }
        }
        wakeWordManager.initWakeWord()
        wakeWordManager.start()

        initializeModel(this)
        TTSManager.init(this) { Log.d("TTS", "TextToSpeech initialized") }

        // mic permission
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

    // 🔄 Rotation handler
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        draggableButton.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    placeButtonAndPanel()
                    draggableButton.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        )
    }

    // ✅ Placement logic keeps button visible
    private fun placeButtonAndPanel() {
        if (draggableButton.width == 0 || draggableButton.height == 0) {
            draggableButton.post { placeButtonAndPanel() }
            return
        }

        draggableButton.translationX = 0f
        draggableButton.translationY = 0f
        scrollSidePanel.translationX = 0f

        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val buttonWidth = draggableButton.width.toFloat()
        val buttonHeight = draggableButton.height.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()

        if (isPanelOpen) {
            // Panel open → button sits beside panel
            scrollSidePanel.x = screenWidth - scrollSidePanel.width
            draggableButton.x = scrollSidePanel.x - buttonWidth
        } else {
            // Panel closed → button sticks to right edge
            scrollSidePanel.x = screenWidth
            draggableButton.x = screenWidth - buttonWidth
        }

        draggableButton.y = (screenHeight - buttonHeight) / 2
    }

    private fun resizeSidePanel(dx: Float) {
        val layoutParams = scrollSidePanel.layoutParams
        var newWidth = layoutParams.width + dx.toInt()
        if (newWidth < sidePanelMinWidth) newWidth = sidePanelMinWidth
        if (newWidth > sidePanelMaxWidth) newWidth = sidePanelMaxWidth
        layoutParams.width = newWidth
        scrollSidePanel.layoutParams = layoutParams
        scrollSidePanel.requestLayout()

        // ✅ Button sits just outside panel edge
        draggableButton.x = scrollSidePanel.x - draggableButton.width
    }


    // ✅ Fixed reset
    fun resetSidePanel() {
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val buttonWidth = draggableButton.width.toFloat()

        // Always hide panel off-screen
        scrollSidePanel.x = screenWidth

        // Always put button on right edge
        draggableButton.x = screenWidth - buttonWidth

        isPanelOpen = false
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
    private fun isPortrait(): Boolean = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    override fun onDestroy() {
        super.onDestroy()
        wakeWordManager.release()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(alarmRingReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(alarmDismissReceiver)
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
                override fun onDone(utteranceId: String?) { isSpeaking = false; runOnUiThread { startSpeechToText() } }
                override fun onError(utteranceId: String?) { isSpeaking = false }
            })
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")
        }
    }

    private fun startSpeechToText() {
        GoogleSTT(this).askSpeechInput()
    }

    fun loadJsonArray(context: Context, fileName: String): List<String> {
        val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        return org.json.JSONArray(jsonString).let { array -> List(array.length()) { i -> array.getString(i) } }
    }

    fun initializeModel(context: Context) {
        val assetFileDescriptor = context.assets.openFd("intent_detection_model.tflite")
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        tflite = Interpreter(modelBuffer)
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
        val tokens = sentence.lowercase(Locale.getDefault())
            .replace("[!?.,]".toRegex(), "")
            .split(" ")
        val bag = FloatArray(words.size) { 0f }
        for ((i, word) in words.withIndex()) {
            if (tokens.contains(word)) bag[i] = 1f
        }
        return bag
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 102 && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
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

    fun openScreen(fragment: Fragment, tag: String = fragment.javaClass.simpleName) {
        replaceFragment(fragment, tag)
    }
}
