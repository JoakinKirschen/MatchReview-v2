package be.matchreview.app

import be.matchreview.app.data.GameMatch
import be.matchreview.app.data.MatchEvent
import be.matchreview.app.data.MatchStatus
import be.matchreview.app.data.Player
import be.matchreview.app.domain.PlayerSeasonStats
import be.matchreview.app.domain.SeasonReportRules
import be.matchreview.app.domain.StatLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonReportRulesTest {
    private val sam = Player(id = 1, teamId = 1, name = "Sam", shirtNumber = 9)
    private val eve = Player(id = 2, teamId = 1, name = "=Eve", shirtNumber = 0)

    private val matches = listOf(
        GameMatch(id = 11, teamId = 1, opponent = "B, United", matchDate = "2026-09-08", status = MatchStatus.FINISHED,
            ourScore = 1, opponentScore = 1, isHome = false),
        GameMatch(id = 10, teamId = 1, opponent = "A", matchDate = "2026-09-01", status = MatchStatus.FINISHED,
            ourScore = 3, opponentScore = 1, competition = "League"),
        GameMatch(id = 12, teamId = 1, opponent = "C", matchDate = "2026-09-15", status = MatchStatus.DRAFT)
    )
    private val stats = listOf(
        PlayerSeasonStats(sam, StatLine(2, 80, 3, 1, 0), StatLine(2, 80, 3, 1, 0)),
        PlayerSeasonStats(eve, StatLine(1, 20, 0, 0, 4), StatLine(1, 20, 0, 0, 4))
    )
    private val events = listOf(
        MatchEvent(matchId = 10, playerId = 1, timestampMs = 1, type = "YELLOW_CARD"),
        MatchEvent(matchId = 11, playerId = 1, timestampMs = 1, type = "DISMISSAL"),
        // A card in a match that was never played does not count.
        MatchEvent(matchId = 12, playerId = 1, timestampMs = 1, type = "YELLOW_CARD")
    )

    @Test
    fun reportListsFinishedMatchesInDateOrderWithCards() {
        val report = SeasonReportRules.build("U11", matches, stats, events)

        assertEquals(listOf("A", "B, United"), report.matches.map { it.opponent })
        assertEquals(listOf("W", "D"), report.matches.map { it.result })
        assertEquals(2, report.summary.played)
        assertEquals(4, report.summary.goalsFor)
        val samLine = report.players.first()
        assertEquals(1, samLine.yellowCards)
        assertEquals(1, samLine.redCards)
    }

    @Test
    fun csvQuotesCellsAndNeutralisesFormulas() {
        val csv = SeasonReportRules.toCsv(SeasonReportRules.build("U11", matches, stats, events))
        val lines = csv.split("\r\n")

        assertEquals("\"Season report\",\"U11\"", lines.first())
        assertTrue(lines.contains("\"2026-09-08\",\"B, United\",\"Away\",\"\",\"1-1\",\"D\""))
        assertTrue(lines.contains("\"9\",\"Sam\",\"2\",\"80\",\"3\",\"1\",\"0\",\"1\",\"1\""))
        assertTrue(lines.contains("\"\",\"'=Eve\",\"1\",\"20\",\"0\",\"0\",\"4\",\"0\",\"0\""))
        assertEquals("matchreview-season-u11-boys.csv", SeasonReportRules.fileName("U11 Boys!", "csv"))
    }
}
