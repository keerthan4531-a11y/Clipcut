package com.clipcut.editor.engine

import android.media.AudioAttributes
import android.media.AudioManager
import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioFocusPolicyTest {

    @Test
    fun previewPlayback_usesMediaMovieAttributes() {
        val attributes = ClipCutAudioFocusPolicy.buildPreviewAttributes()

        assertEquals(C.USAGE_MEDIA, ClipCutAudioFocusPolicy.PREVIEW_USAGE)
        assertEquals(C.AUDIO_CONTENT_TYPE_MOVIE, ClipCutAudioFocusPolicy.PREVIEW_CONTENT_TYPE)
        assertEquals(C.USAGE_MEDIA, attributes.usage)
        assertEquals(C.AUDIO_CONTENT_TYPE_MOVIE, attributes.contentType)
    }

    @Test
    fun ttsPreview_usesTransientDuckFocus() {
        assertEquals(
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            ClipCutAudioFocusPolicy.TTS_PREVIEW_FOCUS_GAIN,
        )
        assertEquals(
            AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY,
            ClipCutAudioFocusPolicy.TTS_PREVIEW_USAGE,
        )
        assertEquals(
            AudioAttributes.CONTENT_TYPE_SPEECH,
            ClipCutAudioFocusPolicy.TTS_PREVIEW_CONTENT_TYPE,
        )
    }

    @Test
    fun voiceover_usesExclusiveTransientFocus() {
        assertEquals(
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
            ClipCutAudioFocusPolicy.VOICEOVER_FOCUS_GAIN,
        )
        assertEquals(
            AudioAttributes.USAGE_VOICE_COMMUNICATION,
            ClipCutAudioFocusPolicy.VOICEOVER_USAGE,
        )
        assertEquals(
            AudioAttributes.CONTENT_TYPE_SPEECH,
            ClipCutAudioFocusPolicy.VOICEOVER_CONTENT_TYPE,
        )
    }
}
