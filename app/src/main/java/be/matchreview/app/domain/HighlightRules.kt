package be.matchreview.app.domain

import be.matchreview.app.data.MatchEvent
import be.matchreview.app.data.RecordingSegment

/** A part of one recorded clip that goes into the highlights video. */
data class HighlightClip(
    val recordingId: Long,
    val uri: String,
    val startMs: Long,
    val endMs: Long,
    val eventIds: List<Long>
) {
    val durationMs: Long get() = endMs - startMs
}

object HighlightRules {
    /** The build-up matters more than the celebration. */
    const val BEFORE_MS = 8_000L
    const val AFTER_MS = 4_000L

    val OUR_GOALS = setOf("OUR_GOAL")
    val ALL_GOALS = setOf("OUR_GOAL", "OPPONENT_GOAL")

    /**
     * One window around each recorded event of [types], in match order. Windows that touch
     * in the same recording are merged so no moment is shown twice.
     */
    fun clips(
        events: List<MatchEvent>,
        recordings: List<RecordingSegment>,
        types: Set<String> = ALL_GOALS
    ): List<HighlightClip> {
        val byId = recordings.associateBy { it.id }
        val windows = events
            .filter { it.type in types && VideoEventRules.isPlayable(it, recordings) }
            .sortedWith(compareBy<MatchEvent> { it.timestampMs }.thenBy { it.id })
            .mapNotNull { event ->
                val recording = byId[event.recordingSegmentId] ?: return@mapNotNull null
                val offset = event.recordingOffsetMs ?: return@mapNotNull null
                val length = recording.recordingDurationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
                HighlightClip(
                    recordingId = recording.id,
                    uri = recording.uri ?: return@mapNotNull null,
                    startMs = (offset - BEFORE_MS).coerceAtLeast(0L),
                    endMs = (offset + AFTER_MS).coerceAtMost(length),
                    eventIds = listOf(event.id)
                )
            }
            .filter { it.endMs > it.startMs }
        return windows.fold(mutableListOf()) { merged, clip ->
            val last = merged.lastOrNull()
            if (last != null && last.recordingId == clip.recordingId && clip.startMs <= last.endMs) {
                merged[merged.lastIndex] = last.copy(
                    endMs = maxOf(last.endMs, clip.endMs),
                    eventIds = last.eventIds + clip.eventIds
                )
            } else {
                merged += clip
            }
            merged
        }
    }
}
