package be.matchreview.app

import be.matchreview.app.data.GameMatch
import be.matchreview.app.data.MatchStatus
import be.matchreview.app.data.Team
import be.matchreview.app.domain.PracticeMatchRules
import be.matchreview.app.domain.SeasonSummaryRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonSummaryRulesTest {
    @Test fun ignoresUnfinishedMatches() {
        val matches = listOf(
            GameMatch(teamId=1, opponent="A", matchDate="2026-01-01", ourScore=2, opponentScore=1, status=MatchStatus.FINISHED),
            GameMatch(teamId=1, opponent="B", matchDate="2026-01-02", ourScore=1, opponentScore=1, status=MatchStatus.FINISHED),
            GameMatch(teamId=1, opponent="C", matchDate="2026-01-03", status=MatchStatus.DRAFT)
        )
        val result = SeasonSummaryRules.summarize(matches)
        assertEquals(2, result.played)
        assertEquals(1, result.won)
        assertEquals(1, result.drawn)
        assertEquals(3, result.goalsFor)
    }

    @Test fun canBeLimitedToTeams() {
        val matches = listOf(
            GameMatch(teamId=1, opponent="A", matchDate="2026-01-01", ourScore=2, opponentScore=1, status=MatchStatus.FINISHED),
            GameMatch(teamId=2, opponent="B", matchDate="2026-01-02", ourScore=0, opponentScore=3, status=MatchStatus.FINISHED)
        )
        assertEquals(1, SeasonSummaryRules.summarize(matches, setOf(1L)).won)
        assertEquals(0, SeasonSummaryRules.summarize(matches, setOf(1L)).lost)
        assertEquals(2, SeasonSummaryRules.summarize(matches).played)
        assertEquals(0, SeasonSummaryRules.summarize(matches, emptySet()).played)
    }

    @Test fun recognisesOnlyTheDedicatedPracticeTeam() {
        assertTrue(PracticeMatchRules.isPracticeTeam(Team(name = "Practice team", season = "Practice")))
        assertFalse(PracticeMatchRules.isPracticeTeam(Team(name = "Practice team", season = "2026/27")))
    }
}
