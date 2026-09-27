package be.matchreview.app

import be.matchreview.app.data.GameMatch
import be.matchreview.app.data.MatchEvent
import be.matchreview.app.domain.MatchIntegrityRules
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchIntegrityRulesTest {
    @Test fun detectsScoreThatDoesNotMatchEvents() {
        val match = GameMatch(teamId=1, opponent="A", matchDate="2026-01-01", ourScore=2)
        val events = listOf(MatchEvent(matchId=1, timestampMs=1, type="OUR_GOAL"))
        val issues = MatchIntegrityRules.inspect(match, events, emptyList(), emptyList(), emptyList())
        assertTrue(issues.any { it.code == "SCORE_MISMATCH" })
    }
}
