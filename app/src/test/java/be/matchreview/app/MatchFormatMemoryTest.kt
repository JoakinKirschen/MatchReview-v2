package be.matchreview.app

import be.matchreview.app.data.GameMatch
import be.matchreview.app.domain.MatchFormat
import be.matchreview.app.domain.MatchFormatMemory
import org.junit.Assert.*
import org.junit.Test

class MatchFormatMemoryTest {
    @Test
    fun latestRealMatchIsUsedAndPracticeMatchesAreIgnored() {
        val matches = listOf(
            GameMatch(id = 1, teamId = 1, opponent = "A", matchDate = "2026-09-01", playersOnPitch = 8,
                formation = "2-4-1", periodCount = 4, periodDurationMinutes = 15, competition = "League"),
            GameMatch(id = 2, teamId = 9, opponent = "Practice", matchDate = "2026-09-02", playersOnPitch = 8,
                formation = "2-4-1", periodCount = 2, periodDurationMinutes = 10)
        )

        val format = MatchFormatMemory.fromLatestMatch(matches, practiceTeamIds = setOf(9L))!!

        assertEquals(8, format.playersOnPitch)
        assertEquals("2-4-1", format.formation)
        assertEquals(4, format.periodCount)
        assertEquals(15, format.periodDurationMinutes)
        assertEquals("League", format.competition)
        assertEquals(1L, format.teamId)
    }

    @Test
    fun noRealMatchMeansNoDefaults() {
        val practice = GameMatch(id = 2, teamId = 9, opponent = "Practice", matchDate = "2026-09-02")
        assertNull(MatchFormatMemory.fromLatestMatch(listOf(practice), setOf(9L)))
    }

    @Test
    fun unsupportedRememberedValuesFallBackToLegalOnes() {
        val format = MatchFormatMemory.sanitize(MatchFormat(7, "9-9-9", 0, 500, true))

        assertEquals(11, format.playersOnPitch)
        assertTrue(format.formation.isNotBlank())
        assertEquals(1, format.periodCount)
        assertEquals(120, format.periodDurationMinutes)
    }
}
