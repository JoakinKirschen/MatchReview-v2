package be.matchreview.app

import be.matchreview.app.data.MatchLineupPlacement
import be.matchreview.app.domain.LineupSwapRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LineupSwapRulesTest {
    @Test fun benchPlayerTakesOccupiedSlotAndOtherPlayerMovesToBench() {
        val bench = MatchLineupPlacement(matchId = 8, playerId = 1, onPitch = false)
        val pitch = MatchLineupPlacement(
            matchId = 8,
            playerId = 2,
            normalizedX = .2f,
            normalizedY = .4f,
            role = "Left",
            formationSlot = "midfield-1",
            onPitch = true
        )

        val (newSelected, newOccupied) = LineupSwapRules.swap(bench, pitch)

        assertEquals(1L, newSelected.playerId)
        assertTrue(newSelected.onPitch)
        assertEquals("midfield-1", newSelected.formationSlot)
        assertEquals(2L, newOccupied.playerId)
        assertFalse(newOccupied.onPitch)
    }
}
