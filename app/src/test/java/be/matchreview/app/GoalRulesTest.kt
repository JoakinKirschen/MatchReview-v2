package be.matchreview.app

import be.matchreview.app.data.AvailabilityStatus
import be.matchreview.app.data.MatchLineupPlacement
import be.matchreview.app.data.MatchSquadPlayer
import be.matchreview.app.data.Player
import be.matchreview.app.data.PlayerMatchState
import be.matchreview.app.domain.GoalMouthGeometry
import be.matchreview.app.domain.GoalkeeperRules
import org.junit.Assert.*
import org.junit.Test

class GoalRulesTest {
    @Test
    fun tapsMapIntoTheGoalFrameAndBack() {
        val width = 260f
        val height = 100f
        val (x, y) = GoalMouthGeometry.toCanvas(0.25f, 0.75f, width, height)
        val (goalX, goalY) = GoalMouthGeometry.normalize(x, y, width, height)

        assertEquals(0.25f, goalX, 0.0001f)
        assertEquals(0.75f, goalY, 0.0001f)
        assertEquals(0f to 0f, GoalMouthGeometry.normalize(-50f, -50f, width, height))
    }

    @Test
    fun describesGoalZones() {
        assertEquals("top left", GoalMouthGeometry.describe(0.1f, 0.1f))
        assertEquals("centre", GoalMouthGeometry.describe(0.5f, 0.5f))
        assertEquals("bottom right", GoalMouthGeometry.describe(0.9f, 0.95f))
        assertNull(GoalMouthGeometry.describe(null, 0.5f))
    }

    @Test
    fun goalkeeperIsFoundFromSlotThenSquadThenPosition() {
        val players = listOf(
            Player(id = 1, teamId = 1, name = "Keeper", position = "Goalkeeper"),
            Player(id = 2, teamId = 1, name = "Striker")
        )
        val squad = listOf(MatchSquadPlayer(1, 2, AvailabilityStatus.AVAILABLE, true, PlayerMatchState.ON_PITCH, isGoalkeeper = true))

        val bySlot = listOf(
            MatchLineupPlacement(1, 1, formationSlot = "DEF-1", onPitch = true),
            MatchLineupPlacement(1, 2, formationSlot = "GK", role = "GK", onPitch = true)
        )
        assertEquals(listOf(2L), GoalkeeperRules.onPitchGoalkeepers(bySlot, emptyList(), players))

        val free = bySlot.map { it.copy(formationSlot = "", role = "") }
        assertEquals(listOf(2L), GoalkeeperRules.onPitchGoalkeepers(free, squad, players))
        assertEquals(listOf(1L), GoalkeeperRules.onPitchGoalkeepers(free, emptyList(), players))
    }
}
