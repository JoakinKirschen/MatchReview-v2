package be.matchreview.app.domain

import be.matchreview.app.data.MatchEvent
import be.matchreview.app.data.MatchLineupPlacement

/** What undoing the latest lineup change has to restore. */
data class LineupUndoPlan(
    /** The events written by the change; they are deleted. */
    val roundEvents: List<MatchEvent>,
    val roundTimeMs: Long,
    val periodNumber: Int,
    /** Who stood where on the pitch just before the change. */
    val before: List<SnapshotPosition>
)

object LineupUndoRules {
    /** Changes made in substitution mode or with the injury / sent-off actions. */
    val UNDOABLE_TYPES = setOf(
        "SUBSTITUTION", "PLAYER_ON", "PLAYER_OFF", "POSITION_CHANGE", "INJURY_OFF", "DISMISSAL"
    )

    /**
     * Plans undoing the latest lineup change, or null when there is nothing to undo.
     * The lineup before it is the picture stored with the previous change (or at kick-off).
     * Matches without such a picture are rebuilt from the change itself: substitutes go back
     * to the bench, the players they replaced return to the same spot, and players that went
     * off without a replacement return to their last known spot.
     */
    fun plan(events: List<MatchEvent>, placements: List<MatchLineupPlacement>): LineupUndoPlan? {
        val changes = TimelineGrouping
            .group(events.sortedWith(compareBy<MatchEvent> { it.timestampMs }.thenBy { it.id }))
            .filterIsInstance<TimelineItem.LineupChange>()
        val latest = changes.lastOrNull() ?: return null
        if (latest.isStartingLineup || latest.events.any { it.type !in UNDOABLE_TYPES }) return null
        val previous = changes.dropLast(1).lastOrNull()
        val before = previous?.snapshot?.takeIf { it.isNotEmpty() } ?: rebuild(latest, placements)
        return LineupUndoPlan(
            roundEvents = latest.events,
            roundTimeMs = latest.timestampMs,
            periodNumber = latest.events.first().periodNumber,
            before = before
        )
    }

    private fun rebuild(
        change: TimelineItem.LineupChange,
        placements: List<MatchLineupPlacement>
    ): List<SnapshotPosition> {
        val replacedBy = change.events
            .filter { it.type == "SUBSTITUTION" && it.playerId != null && it.relatedPlayerId != null }
            .associate { it.relatedPlayerId!! to it.playerId!! }
        val unpairedIncoming = change.incomingPlayerIds.toSet() - replacedBy.keys
        val result = change.snapshot
            .filterNot { it.playerId in unpairedIncoming }
            .map { position -> replacedBy[position.playerId]?.let { position.copy(playerId = it) } ?: position }
            .associateBy { it.playerId }
            .toMutableMap()
        val lastKnown = placements.associateBy { it.playerId }
        change.outgoingPlayerIds
            .filterNot { it in result || it in replacedBy.values }
            .forEach { id ->
                lastKnown[id]?.let { result[id] = SnapshotPosition(id, it.normalizedX, it.normalizedY) }
            }
        return result.values.sortedBy { it.playerId }
    }
}
