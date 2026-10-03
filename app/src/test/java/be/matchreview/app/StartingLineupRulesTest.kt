package be.matchreview.app

import be.matchreview.app.data.MatchEvent
import be.matchreview.app.data.MatchLineupPlacement
import be.matchreview.app.data.ParticipationReason
import be.matchreview.app.data.PlayerParticipation
import be.matchreview.app.domain.LineupSnapshot
import be.matchreview.app.domain.SnapshotPosition
import be.matchreview.app.domain.StartingLineupRules
import be.matchreview.app.domain.TimelineGrouping
import be.matchreview.app.domain.TimelineItem
import org.junit.Assert.*
import org.junit.Test

class StartingLineupRulesTest {
    private val starters = listOf(1L, 2L, 3L).map {
        PlayerParticipation(matchId = 1, periodId = 1, playerId = it, startMatchTimeMs = 0, entryReason = ParticipationReason.STARTER)
    }

    @Test
    fun storedKickOffPictureIsUsedAsIs() {
        val events = listOf(
            MatchEvent(id = 5, matchId = 1, timestampMs = 0, type = "KICK_OFF", lineupSnapshot = "1:0.500:0.900")
        )
        assertSame(events, StartingLineupRules.withStartingLineup(1, events, starters, emptyList()))
    }

    @Test
    fun olderMatchesGetAnApproximateStartingLineup() {
        // First round: player 4 replaced player 2 at (0.3, 0.6); player 3 went to the bench.
        val snapshot = "1:0.500:0.900;4:0.300:0.600"
        val events = listOf(
            MatchEvent(id = 10, matchId = 1, playerId = 2, relatedPlayerId = 4, timestampMs = 600_000,
                type = "SUBSTITUTION", lineupSnapshot = snapshot),
            MatchEvent(id = 11, matchId = 1, playerId = 3, timestampMs = 600_000,
                type = "PLAYER_OFF", lineupSnapshot = snapshot)
        )
        val placements = listOf(
            MatchLineupPlacement(1, 3, 0.7f, 0.3f, onPitch = false),
            MatchLineupPlacement(1, 2, 0.3f, 0.6f, onPitch = false)
        )

        val result = StartingLineupRules.withStartingLineup(1, events, starters, placements)

        val kickOff = result.first()
        assertTrue(StartingLineupRules.isReconstructed(kickOff))
        assertEquals(
            setOf(SnapshotPosition(1, 0.5f, 0.9f), SnapshotPosition(2, 0.3f, 0.6f), SnapshotPosition(3, 0.7f, 0.3f)),
            LineupSnapshot.decode(kickOff.lineupSnapshot).toSet()
        )
        val first = TimelineGrouping.group(result).first() as TimelineItem.LineupChange
        assertTrue(first.isStartingLineup)
        assertTrue(first.isApproximate)
    }

    @Test
    fun matchesThatNeverKickedOffGetNoStartingLineup() {
        assertTrue(StartingLineupRules.withStartingLineup(1, emptyList(), emptyList(), emptyList()).isEmpty())
    }
}
