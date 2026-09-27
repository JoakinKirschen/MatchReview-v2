package be.matchreview.app

import be.matchreview.app.domain.PracticeMatchRules
import org.junit.Assert.*
import org.junit.Test

class PracticeMatchRulesTest {
    @Test fun templateSupportsEightASidePractice() {
        assertEquals(8, PracticeMatchRules.players.size)
        assertEquals("GK", PracticeMatchRules.players.first().position)
        assertEquals(8, PracticeMatchRules.players.map { it.shirtNumber }.distinct().size)
    }
}
