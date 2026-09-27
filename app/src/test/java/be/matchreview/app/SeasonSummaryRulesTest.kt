package be.matchreview.app

import be.matchreview.app.data.GameMatch
import be.matchreview.app.data.MatchStatus
import be.matchreview.app.domain.SeasonSummaryRules
import org.junit.Assert.assertEquals
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
}
