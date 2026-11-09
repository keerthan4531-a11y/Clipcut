package com.clipcut.editor.engine

import com.clipcut.editor.model.Clip
import com.clipcut.editor.model.Track
import com.clipcut.editor.model.TrackType
import com.clipcut.editor.model.effectiveTimelineEndMs
import com.clipcut.editor.model.effectiveTimelineStartMs

/** Pure, testable selection contract shared by live preview and export ordering. */
internal data class PreviewCompositionPlan(
    val visualTracks: List<Track>,
    val audioTracks: List<Track>,
    val soloTrackIds: Set<String>,
    val durationMs: Long,
) {
    fun primaryClipAt(positionMs: Long): Clip? {
        val clampedPosition = positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L))
        return visualTracks.firstNotNullOfOrNull { track ->
            track.clips.firstOrNull { clip ->
                val startMs = track.effectiveTimelineStartMs(clip)
                val endMs = track.effectiveTimelineEndMs(clip)
                clampedPosition >= startMs &&
                    (clampedPosition < endMs ||
                        (clampedPosition == durationMs && endMs == durationMs))
            }
        }
    }

    companion object {
        fun create(tracks: List<Track>): PreviewCompositionPlan {
            val plan = CompositionPlanBuilder.build(tracks)
            return PreviewCompositionPlan(
                visualTracks = plan.visualTracks,
                audioTracks = plan.audioTracks,
                soloTrackIds = plan.soloTrackIds,
                durationMs = plan.durationMs,
            )
        }
    }
}
