package com.heathevens.offlinenotes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Thin wrapper around Android's built-in SpeechRecognizer.
 *
 * This intentionally uses the platform's on-device speech engine rather
 * than any cloud API. EXTRA_PREFER_OFFLINE tells the system to use an
 * on-device recognition model when the phone has one downloaded (most
 * modern Android phones ship with an offline Google speech model, or the
 * user can download one in Settings > System > Languages > On-device
 * speech recognition). No network permission is requested by this app,
 * so if no offline model is available, recognition simply won't produce
 * a result rather than silently phoning out.
 */
class SpeechTranscriber(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onListeningChange: (Boolean) -> Unit
    ) {
        stop()

        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            // Push the OS's silence auto-cutoff way out so the manual Stop button gets a real
            // chance to be tapped instead of the recognizer ending the session on its own.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 60000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 60000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 60000L)
        }

        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onListeningChange(true)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull().orEmpty()
                onListeningChange(false)
                r.destroy()
                if (recognizer === r) recognizer = null
                if (text.isNotBlank()) {
                    onResult(text)
                } else {
                    onError("No speech recognized.")
                }
            }

            override fun onError(error: Int) {
                onListeningChange(false)
                r.destroy()
                if (recognizer === r) recognizer = null
                onError("Recognition error (code $error).")
            }

            override fun onEndOfSpeech() {
                onListeningChange(false)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        r.startListening(intent)
    }

    /** Signals "I'm done talking" and lets the recognizer finish processing and return a result. */
    fun finishListening() {
        recognizer?.stopListening()
    }

    /** Hard stop with no result expected - used when resetting before a new session or on teardown. */
    fun stop() {
        recognizer?.destroy()
        recognizer = null
    }
}
