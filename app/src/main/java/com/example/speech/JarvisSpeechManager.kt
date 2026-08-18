package com.example.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.JarvisApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class JarvisSpeechManager private constructor(private val context: Context) : RecognitionListener {

    companion object {
        private const val TAG = "JarvisSpeechManager"

        @Volatile
        private var INSTANCE: JarvisSpeechManager? = null

        fun getInstance(context: Context): JarvisSpeechManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: JarvisSpeechManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var toneGenerator: ToneGenerator? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _lastDetectedPhrase = MutableStateFlow<String?>(null)
    val lastDetectedPhrase: StateFlow<String?> = _lastDetectedPhrase.asStateFlow()

    private var onWakeWordDetectedListener: ((String) -> Unit)? = null

    init {
        initTTS()
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ToneGenerator: ${e.message}")
        }
    }

    private fun initTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "TTS Language US not supported, falling back to default")
                    tts?.setLanguage(Locale.getDefault())
                }
                isTtsReady = true
                applyTtsPreferences()
                Log.d(TAG, "TextToSpeech initialized successfully")
            } else {
                Log.e(TAG, "TextToSpeech initialization failed with status: $status")
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "TTS Utterance started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "TTS Utterance finished: $utteranceId")
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "TTS Utterance error: $utteranceId")
            }
        })
    }

    fun applyTtsPreferences() {
        if (!isTtsReady) return
        val prefs = (context as? JarvisApplication)?.preferences ?: JarvisApplication.instance.preferences
        tts?.setPitch(prefs.getTtsPitchSync())
        tts?.setSpeechRate(prefs.getTtsSpeedSync())
    }

    /**
     * Audibly announce text using Text-to-Speech
     */
    fun speak(text: String, flush: Boolean = true, utteranceId: String = "jarvis_announcement_${System.currentTimeMillis()}") {
        if (!isTtsReady) {
            Log.w(TAG, "TTS not ready yet. Retrying in 500ms...")
            mainHandler.postDelayed({ speak(text, flush, utteranceId) }, 500)
            return
        }
        applyTtsPreferences()
        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, queueMode, null, utteranceId)
    }

    /**
     * Play high-tech confirmation tone
     */
    fun playWakeConfirmationTone() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing tone: ${e.message}")
        }
    }

    fun playAlertTone() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 250)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alert tone: ${e.message}")
        }
    }

    fun setOnWakeWordDetectedListener(listener: (String) -> Unit) {
        this.onWakeWordDetectedListener = listener
    }

    /**
     * Start continuous listening for trigger phrase "Jarvis"
     */
    fun startWakeWordListening() {
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAudioPermission) {
            Log.w(TAG, "Cannot start wake word listening: RECORD_AUDIO permission not granted.")
            _isListening.value = false
            return
        }

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                        Log.e(TAG, "Speech recognition is not available on this device")
                        return@post
                    }
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(this@JarvisSpeechManager)
                    }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                speechRecognizer?.startListening(intent)
                _isListening.value = true
                Log.d(TAG, "SpeechRecognizer started listening for wake word")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognition: ${e.message}")
                _isListening.value = false
            }
        }
    }

    fun stopWakeWordListening() {
        mainHandler.post {
            try {
                _isListening.value = false
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
                Log.d(TAG, "SpeechRecognizer stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognizer: ${e.message}")
            }
        }
    }

    private fun restartListeningWithDelay(delayMs: Long = 1000) {
        val prefs = JarvisApplication.instance.preferences
        if (!prefs.isServiceEnabledSync() || !prefs.isVoiceWakeEnabledSync()) {
            _isListening.value = false
            return
        }

        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasAudio) {
            _isListening.value = false
            return
        }

        mainHandler.postDelayed({
            if (prefs.isServiceEnabledSync() && prefs.isVoiceWakeEnabledSync()) {
                startWakeWordListening()
            }
        }, delayMs)
    }

    // Speech Recognition Listener Callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
    }

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {
        _rmsLevel.value = rmsdB.coerceAtLeast(0f)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
    }

    override fun onError(error: Int) {
        _isListening.value = false
        Log.w(TAG, "SpeechRecognizer error: $error")
        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                // Restart listening after normal timeout
                restartListeningWithDelay(500)
            }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                stopWakeWordListening()
                restartListeningWithDelay(1500)
            }
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                restartListeningWithDelay(3000)
            }
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                Log.e(TAG, "Insufficient audio permissions for speech recognizer")
            }
            else -> {
                restartListeningWithDelay(2000)
            }
        }
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        processSpeechMatches(matches)
        restartListeningWithDelay(500)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val detected = matches[0]
            _lastDetectedPhrase.value = detected
            checkWakeWord(detected)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun processSpeechMatches(matches: List<String>?) {
        if (matches.isNullOrEmpty()) return
        for (phrase in matches) {
            _lastDetectedPhrase.value = phrase
            if (checkWakeWord(phrase)) {
                break
            }
        }
    }

    private fun checkWakeWord(text: String): Boolean {
        val prefs = JarvisApplication.instance.preferences
        val targetWakeWord = prefs.getWakeWordSync().lowercase()
        val normalizedText = text.lowercase()

        if (normalizedText.contains(targetWakeWord) || normalizedText.contains("jarvis") || normalizedText.contains("hey jarvis")) {
            playWakeConfirmationTone()
            onWakeWordDetectedListener?.invoke(text)
            return true
        }
        return false
    }

    fun shutdown() {
        stopWakeWordListening()
        tts?.stop()
        tts?.shutdown()
        tts = null
        toneGenerator?.release()
        toneGenerator = null
    }
}
