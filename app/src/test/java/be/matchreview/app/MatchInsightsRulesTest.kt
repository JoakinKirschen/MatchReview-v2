package be.matchreview.app

import be.matchreview.app.data.GameMatch
import be.matchreview.app.data.MatchEvent
import be.matchreview.app.data.MatchStatus
import be.matchreview.app.data.Player
import be.matchreview.app.data.PlayerParticipation
import be.matchreview.app.domain.BackupReminderRules
import be.matchreview.app.domain.GoalSummaryRules
import be.matchreview.app.domain.PeriodTimeRules
import be.matchreview.app.domain.PlayerSeasonStatsRules
import be.matchreview.app.domain.PlayingTimeRules
import be.matchreview.app.domain.StatAdjustments
import be.matchreview.app.domain.StatLine
import org.junit.Assert.*
import org.junit.Test

class MatchInsightsRulesTest {
    private val sam = Player(id = 1, teamId = 1, name = "Sam", shirtNumber = 9)
    private val lee = Player(id = 2, teamId = 1, name = "Lee", shirtNumber = 7)
    private val kim = Player(id = 3, teamId = 1, name = "Kim", shirtNumber = 1)

    @Test
    fun seasonStatsAddUpPlayedMatchesOnly() {
        val matches = listOf(
            GameMatch(id = 10, teamId = 1, opponent = "A", matchDate = "2026-09-01", status = MatchStatus.FINISHED),
            GameMatch(id = 11, teamId = 1, opponent = "B", matchDate = "2026-09-08", status = MatchStatus.LIVE,
                accumulatedMatchTimeMs = 600_000),
            GameMatch(id = 12, teamId = 1, opponent = "C", matchDate = "2026-09-15", status = MatchStatus.DRAFT)
        )
        val participations = listOf(
            PlayerParticipation(matchId = 10, periodId = 1, playerId = 1, startMatchTimeMs = 0, endMatchTimeMs = 1_800_000),
            PlayerParticipation(matchId = 11, periodId = 2, playerId = 1, startMatchTimeMs = 0),
            PlayerParticipation(matchId = 10, periodId = 1, playerId = 2, startMatchTimeMs = 900_000, endMatchTimeMs = 1_800_000),
            PlayerParticipation(matchId = 12, periodId = 3, playerId = 2, startMatchTimeMs = 0, endMatchTimeMs = 9_000_000)
        )
        val events = listOf(
            MatchEvent(matchId = 10, playerId = 1, relatedPlayerId = 2, timestampMs = 60_000, type = "OUR_GOAL"),
            MatchEvent(matchId = 11, playerId = 3, timestampMs = 90_000, type = "KEEPER_SAVE")
        )

        val stats = PlayerSeasonStatsRules.compute(listOf(lee, sam, kim), matches, participations, events)

        assertEquals(listOf("Sam", "Lee", "Kim"), stats.map { it.player.name })
        val samStats = stats.first()
        assertEquals(2, samStats.matchesPlayed)
        assertEquals(40L, samStats.wholeMinutes) // 30 finished + 10 so far in the live match
        assertEquals(1, samStats.goals)
        assertEquals(1, stats[1].assists)
        assertEquals(15L, stats[1].wholeMinutes) // the draft match does not count
        assertEquals(1, stats[2].saves)
    }

    @Test
    fun leastPlayedSubstitutesAreMarkedOnlyWhenSomebodyStandsOut() {
        val minutes = mapOf(1L to 300_000L, 2L to 59_000L, 3L to 30_000L)
        assertEquals(setOf(2L, 3L), PlayingTimeRules.leastPlayed(minutes.keys) { minutes.getValue(it) })
        assertTrue(PlayingTimeRules.leastPlayed(listOf(1L, 2L)) { 0L }.isEmpty())
        assertTrue(PlayingTimeRules.leastPlayed(listOf(1L)) { 0L }.isEmpty())
    }

    @Test
    fun goalLinesUseFootballMinutesAndAssists() {
        val events = listOf(
            MatchEvent(id = 2, matchId = 1, timestampMs = 2_000_000, type = "OPPONENT_GOAL"),
            MatchEvent(id = 1, matchId = 1, playerId = 1, relatedPlayerId = 2, timestampMs = 680_000, type = "OUR_GOAL"),
            MatchEvent(id = 3, matchId = 1, timestampMs = 2_100_000, type = "OUR_GOAL"),
            MatchEvent(id = 4, matchId = 1, playerId = 3, timestampMs = 100, type = "KEEPER_SAVE")
        )

        val lines = GoalSummaryRules.lines(events, mapOf(1L to "Sam", 2L to "Lee"), "U11", "Rivals")

        assertEquals(listOf("12' Sam (assist Lee)", "34' Rivals", "36' U11"), lines)
    }

    @Test
    fun periodAlertFiresOnceNearTheEnd() {
        val planned = 15 * 60_000L
        assertFalse(PeriodTimeRules.shouldAlert(planned - 1, planned, alreadyAlerted = false))
        assertTrue(PeriodTimeRules.shouldAlert(planned + 5_000, planned, alreadyAlerted = false))
        assertFalse(PeriodTimeRules.shouldAlert(planned + 5_000, planned, alreadyAlerted = true))
        // Reopening the screen long after the period ended does not vibrate again.
        assertFalse(PeriodTimeRules.shouldAlert(planned + 120_000, planned, alreadyAlerted = false))
        assertEquals(83_000L, PeriodTimeRules.overtimeMs(planned + 83_000, planned))
    }

    @Test
    fun backupIsDueAfterAMatchFinishedSinceTheLastBackup() {
        assertFalse(BackupReminderRules.isDue(lastBackupEpochMs = null, lastFinishedMatchEpochMs = null))
        assertTrue(BackupReminderRules.isDue(lastBackupEpochMs = null, lastFinishedMatchEpochMs = 5))
        assertTrue(BackupReminderRules.isDue(lastBackupEpochMs = 4, lastFinishedMatchEpochMs = 5))
        assertFalse(BackupReminderRules.isDue(lastBackupEpochMs = 6, lastFinishedMatchEpochMs = 5))
    }

    @Test
    fun coachCorrectionsAreAddedOnTopOfTrackedStats() {
        val tracked = StatLine(matches = 2, minutes = 40, goals = 1, assists = 0, saves = 0)
        // The coach adds a match that was not tracked: 3 matches, 60 minutes, 2 goals.
        val adjustments = PlayerSeasonStatsRules.adjustmentsFor(tracked, StatLine(3, 60, 2, 0, 0))
        assertEquals(StatAdjustments(1, 20, 1, 0, 0), adjustments)

        val corrected = sam.copy(
            statMatchesAdjustment = adjustments.matches,
            statMinutesAdjustment = adjustments.minutes,
            statGoalsAdjustment = adjustments.goals
        )
        // A later tracked match keeps counting on top of the correction.
        val laterTracked = tracked.copy(matches = 3, minutes = 70)
        assertEquals(StatLine(4, 90, 2, 0, 0), PlayerSeasonStatsRules.withCorrections(laterTracked, corrected))
        // Totals never go below zero.
        assertEquals(0, PlayerSeasonStatsRules.withCorrections(tracked, sam.copy(statGoalsAdjustment = -5)).goals)
    }
}
