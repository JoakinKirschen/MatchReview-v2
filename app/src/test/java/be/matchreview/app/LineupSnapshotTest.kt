package be.matchreview.app

import be.matchreview.app.data.MatchEvent
import be.matchreview.app.data.MatchLineupPlacement
import be.matchreview.app.domain.LineupSnapshot
import be.matchreview.app.domain.SnapshotPosition
import be.matchreview.app.domain.TimelineGrouping
import be.matchreview.app.domain.TimelineItem
import org.junit.Assert.*
import org.junit.Test

class LineupSnapshotTest {
    @Test
    fun encodesOnlyPitchPlayersAndDecodesThem() {
        val encoded = LineupSnapshot.encode(
            listOf(
                MatchLineupPlacement(1, 7, 0.25f, 0.5f, onPitch = true),
                MatchLineupPlacement(1, 3, 0.5f, 0.9f, onPitch = true),
                MatchLineupPlacement(1, 9, onPitch = false)
            )
        )

        assertEquals("3:0.500:0.900;7:0.250:0.500", encoded)
        assertEquals(
            listOf(SnapshotPosition(3, 0.5f, 0.9f), SnapshotPosition(7, 0.25f, 0.5f)),
            LineupSnapshot.decode(encoded)
        )
    }

    @Test
    fun ignoresDamagedEntries() {
        assertEquals(listOf(SnapshotPosition(4, 1f, 0f)), LineupSnapshot.decode("x:1:2;4:1.5:-1;5:0.1"))
        assertTrue(LineupSnapshot.decode(null).isEmpty())
    }

    @Test
    fun roundEventsAreGroupedIntoOneLineupPicture() {
        val snapshot = "1:0.500:0.900"
        val events = listOf(
            MatchEvent(id = 1, matchId = 1, timestampMs = 10_000, type = "OUR_GOAL"),
            MatchEvent(id = 2, matchId = 1, playerId = 5, relatedPlayerId = 6, timestampMs = 60_000, type = "SUBSTITUTION", lineupSnapshot = snapshot),
            MatchEvent(id = 3, matchId = 1, playerId = 7, timestampMs = 60_000, type = "PLAYER_ON", lineupSnapshot = snapshot),
            MatchEvent(id = 4, matchId = 1, playerId = 8, relatedPlayerId = 9, timestampMs = 90_000, type = "SUBSTITUTION")
        )

        val items = TimelineGrouping.group(events)

        assertEquals(3, items.size)
        val change = items[1] as TimelineItem.LineupChange
        assertEquals(listOf(6L, 7L), change.incomingPlayerIds)
        assertEquals(listOf(5L), change.outgoingPlayerIds)
        // Legacy substitutions without a picture remain ordinary rows.
        assertTrue(items[2] is TimelineItem.Single)
    }
}
