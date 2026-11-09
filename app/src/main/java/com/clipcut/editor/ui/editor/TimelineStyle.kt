package com.clipcut.editor.ui.editor

import com.clipcut.editor.ui.theme.ClipCutAccents
import androidx.compose.ui.graphics.Color
import com.clipcut.editor.model.TrackType

internal fun trackAccentColor(trackType: TrackType): Color = when (trackType) {
    TrackType.VIDEO -> ClipCutAccents.Blue
    TrackType.AUDIO -> ClipCutAccents.Green
    TrackType.OVERLAY -> ClipCutAccents.Peach
    TrackType.TEXT -> ClipCutAccents.Mauve
    TrackType.ADJUSTMENT -> ClipCutAccents.Yellow
}
