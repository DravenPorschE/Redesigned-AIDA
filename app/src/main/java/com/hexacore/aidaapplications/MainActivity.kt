package com.hexacore.aidaapplications

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
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

        // Initialize draggable logic
        draggableButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastX
                    if (isPortrait()) {
                        // Portrait: panel on right, drag left to show
                        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
                        val panelWidth = scrollSidePanel.width.toFloat()

                        // New X for draggable button
                        var newButtonX = draggableButton.x + dx
                        // Clamp button between (screenWidth - panelWidth - button width) and (screenWidth - button width)
                        val minX = screenWidth - panelWidth - draggableButton.width
                        val maxX = screenWidth - draggableButton.width
                        newButtonX = newButtonX.coerceIn(minX, maxX)
                        draggableButton.x = newButtonX

                        // Panel X is button's right edge
                        scrollSidePanel.x = newButtonX + draggableButton.width
                    } else {
                        // Landscape: resize panel width
                        resizeSidePanel(dx)
                    }
                    lastX = event.rawX
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
                val panelWidth = scrollSidePanel.width.toFloat()
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
            resetSidePanel()

            supportFragmentManager.beginTransaction()
                .replace(R.id.main_content, GameMenuScreen())
                .commit()
        }

        noteButton.setOnClickListener {
            resetSidePanel()

            supportFragmentManager.beginTransaction()
                .replace(R.id.main_content, NoteAppScreen())
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
            initWakeWord()
        }

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
    }
    // Resize panel in landscape
    private fun resizeSidePanel(dx: Float) {
        val layoutParams = scrollSidePanel.layoutParams
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
    private fun resetSidePanel() {
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
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun isPortrait(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    private fun initWakeWord() {
        // Initialize WakeWordManager with the speechHelper
        wakeWordManager = WakeWordManager(this) {
            runOnUiThread {
                speakAndThenListen("I'm listening")
            }
        }
        wakeWordManager.initWakeWord()
        wakeWordManager.start()
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
            initWakeWord()
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
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something…")
        }
        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show()
        }
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


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH && resultCode == RESULT_OK) {
            val spokenText = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            spokenText?.let {
                Toast.makeText(this, "You said: $it", Toast.LENGTH_SHORT).show()
                // run your AIDA intent prediction logic here
                val (intent, confidence) = predictIntent(it)

                val outputAction = OutputAction(this)
                outputAction.outputActionBasedOnIntent(intent, it)
            }
        }
    }
}