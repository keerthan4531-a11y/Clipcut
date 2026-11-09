package com.clipcut.editor.engine

import androidx.media3.common.OverlaySettings
import androidx.media3.common.VideoCompositorSettings
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.StaticOverlaySettings
import com.clipcut.editor.model.BlendMode

@UnstableApi
internal data class ClipCutCompositorLayer(
    val inputId: Int,
    val trackId: String,
    val trackIndex: Int,
    val opacity: Float,
    val blendMode: BlendMode
)

/**
 * Carries ClipCut timeline-layer intent into Media3's multi-sequence compositor.
 *
 * Media3 1.10's public compositor API exposes output size plus per-input
 * overlay alpha/transform settings. It does not expose a programmable blend
 * function, so non-normal track blend modes are still guarded in VideoEngine.
 */
@UnstableApi
internal class ClipCutVideoCompositorSettings(
    outputWidth: Int,
    outputHeight: Int,
    layers: List<ClipCutCompositorLayer>
) : VideoCompositorSettings {
    private val outputSize = Size(outputWidth.coerceAtLeast(1), outputHeight.coerceAtLeast(1))
    private val layersByInputId = layers.associateBy { it.inputId }
    private val overlaySettingsByInputId = layers.associate { layer ->
        layer.inputId to StaticOverlaySettings.Builder()
            .setAlphaScale(layer.opacity.safeOpacity())
            .build()
    }

    override fun getOutputSize(inputSizes: MutableList<Size>): Size = outputSize

    override fun getOverlaySettings(inputId: Int, presentationTimeUs: Long): OverlaySettings {
        return overlaySettingsByInputId[inputId] ?: DEFAULT_OVERLAY_SETTINGS
    }

    fun layerForInput(inputId: Int): ClipCutCompositorLayer? = layersByInputId[inputId]

    private fun Float.safeOpacity(): Float {
        return if (isFinite()) coerceIn(0f, 1f) else 1f
    }

    private companion object {
        val DEFAULT_OVERLAY_SETTINGS: OverlaySettings = StaticOverlaySettings.Builder().build()
    }
}
