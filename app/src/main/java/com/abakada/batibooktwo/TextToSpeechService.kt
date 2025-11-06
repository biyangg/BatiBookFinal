package com.abakada.batibooktwo

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

/**
 * Android Text-to-Speech API Service
 * 
 * This service provides text-to-speech functionality to automatically narrate
 * story content. It uses Android's built-in TTS engine to convert text into
 * spoken audio.
 * 
 * INTEGRATION PROCESS:
 * 1. No API key required - uses Android's built-in TTS engine
 * 2. TTS engine is available on all Android devices
 * 3. Users may need to install TTS data if not pre-installed
 * 4. Supports multiple languages and voices
 * 
 * HOW IT WORKS:
 * - Android TTS engine converts text strings into spoken audio
 * - Supports multiple languages (configured in LanguageManager)
 * - Can pause, resume, and stop narration
 * - Provides callbacks for speech events (start, done, error)
 * - Automatically handles voice selection based on language
 * 
 * FEATURES:
 * - Automatic story narration
 * - Multiple language support
 * - Play/pause/stop controls
 * - Speed and pitch adjustment
 * - Progress tracking during narration
 * - Error handling for missing TTS data
 * 
 * PERMISSIONS:
 * - No special permissions required
 * - TTS engine is part of Android system
 * 
 * INITIALIZATION:
 * - Must be initialized before use
 * - Check TTS availability before speaking
 * - Handle initialization errors gracefully
 */
class TextToSpeechService private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "TextToSpeechService"
        
        @Volatile
        private var INSTANCE: TextToSpeechService? = null
        
        fun getInstance(context: Context): TextToSpeechService {
            return INSTANCE ?: synchronized(this) {
                val instance = TextToSpeechService(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var currentLanguage: String = "en" // Default to English
    private var speechRate: Float = 1.0f // Normal speed
    private var speechPitch: Float = 1.0f // Normal pitch
    
    // Current utterance ID for tracking
    private var currentUtteranceId: String? = null
    
    /**
     * Initialize TTS engine
     * 
     * @param onInitListener Callback when initialization completes
     * 
     * Process:
     * 1. Creates TextToSpeech instance
     * 2. Sets up language and voice
     * 3. Configures speech parameters
     * 4. Notifies listener when ready
     */
    fun initialize(onInitListener: (Boolean) -> Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Set default language
                val result = textToSpeech?.setLanguage(Locale(currentLanguage))
                
                if (result == TextToSpeech.LANG_MISSING_DATA || 
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS Language not supported: $currentLanguage")
                    isInitialized = false
                    onInitListener(false)
                } else {
                    // Configure speech parameters
                    textToSpeech?.setSpeechRate(speechRate)
                    textToSpeech?.setPitch(speechPitch)
                    
                    // Set utterance progress listener
                    textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            Log.d(TAG, "Speech started: $utteranceId")
                            onSpeechStart?.invoke()
                        }
                        
                        override fun onDone(utteranceId: String?) {
                            Log.d(TAG, "Speech completed: $utteranceId")
                            onSpeechDone?.invoke()
                            currentUtteranceId = null
                        }
                        
                        override fun onError(utteranceId: String?) {
                            Log.e(TAG, "Speech error: $utteranceId")
                            onSpeechError?.invoke()
                            currentUtteranceId = null
                        }
                    })
                    
                    isInitialized = true
                    onInitListener(true)
                }
            } else {
                Log.e(TAG, "TTS initialization failed")
                isInitialized = false
                onInitListener(false)
            }
        }
    }
    
    /**
     * Speak text with automatic narration
     * 
     * @param text Text to be spoken
     * @param language Language code (e.g., "en", "fil" for Filipino)
     * 
     * Process:
     * 1. Checks if TTS is initialized
     * 2. Sets language if different from current
     * 3. Speaks the text using TTS engine
     * 4. Tracks progress through callbacks
     */
    fun speak(text: String, language: String = currentLanguage) {
        if (!isInitialized || textToSpeech == null) {
            Log.e(TAG, "TTS not initialized")
            return
        }
        
        // Update language if needed
        if (language != currentLanguage) {
            setLanguage(language)
        }
        
        // Stop any ongoing speech
        stop()
        
        // Generate unique utterance ID
        currentUtteranceId = UUID.randomUUID().toString()
        
        // Speak the text
        // FLAG_QUEUE ensures smooth narration of long texts
        val result = textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            currentUtteranceId
        )
        
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "Error speaking text")
            onSpeechError?.invoke()
        }
    }
    
    /**
     * Queue text for narration (continues from current)
     * 
     * @param text Text to be queued
     */
    fun queue(text: String) {
        if (!isInitialized || textToSpeech == null) {
            Log.e(TAG, "TTS not initialized")
            return
        }
        
        val utteranceId = UUID.randomUUID().toString()
        textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_ADD,
            null,
            utteranceId
        )
    }
    
    /**
     * Stop current speech
     */
    fun stop() {
        textToSpeech?.stop()
        currentUtteranceId = null
    }
    
    /**
     * Pause speech (if supported)
     */
    fun pause() {
        // Note: Pause is not directly supported in TTS
        // We'll stop and track position for resume
        stop()
    }
    
    /**
     * Set language for TTS
     * 
     * @param language Language code (e.g., "en", "fil")
     */
    fun setLanguage(language: String) {
        currentLanguage = language
        val locale = when (language) {
            "fil" -> Locale("fil", "PH") // Filipino
            "en" -> Locale.ENGLISH
            else -> Locale.ENGLISH
        }
        
        val result = textToSpeech?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || 
            result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "Language not supported: $language")
        }
    }
    
    /**
     * Set speech rate (speed)
     * 
     * @param rate 1.0 = normal, 0.5 = half speed, 2.0 = double speed
     */
    fun setSpeechRate(rate: Float) {
        speechRate = rate
        textToSpeech?.setSpeechRate(rate)
    }
    
    /**
     * Set speech pitch
     * 
     * @param pitch 1.0 = normal, 0.5 = lower, 2.0 = higher
     */
    fun setSpeechPitch(pitch: Float) {
        speechPitch = pitch
        textToSpeech?.setPitch(pitch)
    }
    
    /**
     * Check if TTS is currently speaking
     */
    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking ?: false
    }
    
    /**
     * Release TTS resources
     * Call this when done with TTS to free resources
     */
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
    }
    
    // Callbacks for speech events
    var onSpeechStart: (() -> Unit)? = null
    var onSpeechDone: (() -> Unit)? = null
    var onSpeechError: (() -> Unit)? = null
}


