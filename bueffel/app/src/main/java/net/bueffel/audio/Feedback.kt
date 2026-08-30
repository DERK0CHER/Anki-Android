package net.bueffel.audio

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

/**
 * A short tone on each answer.
 *
 * Built from [ToneGenerator] rather than sound files: it needs no assets, no permission and no
 * playback library, and two tones is all this is - one that says yes and one that says no.
 */
class Feedback {
    private var generator: ToneGenerator? = null

    private fun generator(): ToneGenerator? {
        if (generator == null) {
            generator =
                runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME) }
                    .onFailure { Log.w(TAG, "no tone generator available", it) }
                    .getOrNull()
        }
        return generator
    }

    fun play(correct: Boolean) {
        val tone = if (correct) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK
        runCatching { generator()?.startTone(tone, DURATION_MS) }
            .onFailure { Log.w(TAG, "could not play the tone", it) }
    }

    /** Frees the audio resource; the generator is recreated on the next tone if needed */
    fun release() {
        runCatching { generator?.release() }
        generator = null
    }

    private companion object {
        const val TAG = "Feedback"
        const val VOLUME = 70
        const val DURATION_MS = 150
    }
}
