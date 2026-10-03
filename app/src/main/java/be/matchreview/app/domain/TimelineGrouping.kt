package be.matchreview.app.domain

import be.matchreview.app.data.MatchEvent

sealed interface TimelineItem {
    val key: String
    val timestampMs: Long

    data class Single(val event: MatchEvent) : TimelineItem {
        override val key: String get() = "event-${event.id}"
        override val timestampMs: Long get() = event.timestampMs
    }

    /** All changes made in one substitution round, shown as one picture of the pitch. */
    data class LineupChange(val events: List<MatchEvent>) : TimelineItem {
        override val key: String get() = "lineup-${events.first().id}"
        override val timestampMs: Long get() = events.first().timestampMs
        val snapshot: List<SnapshotPosition> get() = LineupSnapshot.decode(events.first().lineupSnapshot)

        val incomingPlayerIds: List<Long>
            get() = events.mapNotNull {
                when (it.type) {
                    "SUBSTITUTION" -> it.relatedPlayerId
                    "PLAYER_ON" -> it.playerId
                    else -> null
                }
            }

        val outgoingPlayerIds: List<Long>
            get() = events.mapNotNull {
                when (it.type) {
                    "SUBSTITUTION", "PLAYER_OFF", "INJURY_OFF", "DISMISSAL" -> it.playerId
                    else -> null
                }
            }
    }
}

object TimelineGrouping {
    val LINEUP_TYPES = setOf("SUBSTITUTION", "PLAYER_ON", "PLAYER_OFF", "INJURY_OFF", "DISMISSAL", "POSITION_CHANGE")

    /**
     * Groups adjacent lineup events that share a time and a lineup picture. Events
     * recorded before lineup pictures existed stay as single rows. Keeps input order.
     */
    fun group(events: List<MatchEvent>): List<TimelineItem> {
        val result = mutableListOf<TimelineItem>()
        var pending = mutableListOf<MatchEvent>()
        fun flush() {
            if (pending.isNotEmpty()) {
                result += TimelineItem.LineupChange(pending.sortedBy { it.id })
                pending = mutableListOf()
            }
        }
        events.forEach { event ->
            val groupable = event.type in LINEUP_TYPES && !event.lineupSnapshot.isNullOrBlank()
            if (!groupable) {
                flush()
                result += TimelineItem.Single(event)
                return@forEach
            }
            val first = pending.firstOrNull()
            if (first != null &&
                (first.timestampMs != event.timestampMs || first.lineupSnapshot != event.lineupSnapshot)
            ) flush()
            pending += event
        }
        flush()
        return result
    }
}
