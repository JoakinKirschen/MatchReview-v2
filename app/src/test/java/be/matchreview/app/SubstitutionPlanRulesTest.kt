package be.matchreview.app

import be.matchreview.app.data.MatchLineupPlacement
import be.matchreview.app.domain.FormationSlot
import be.matchreview.app.domain.LineupDragDropRules
import be.matchreview.app.domain.PitchDrop
import be.matchreview.app.domain.SubstitutionDrop
import be.matchreview.app.domain.SubstitutionPair
import be.matchreview.app.domain.SubstitutionPlanRules
import org.junit.Assert.*
import org.junit.Test

class SubstitutionPlanRulesTest {
    private val original = listOf(
        MatchLineupPlacement(1, 10, 0.5f, 0.9f, "GK", "GK", onPitch = true),
        MatchLineupPlacement(1, 11, 0.3f, 0.6f, "DEF", "DEF-1", onPitch = true),
        MatchLineupPlacement(1, 12, 0.7f, 0.3f, "FWD", "FWD-1", onPitch = true),
        MatchLineupPlacement(1, 20, onPitch = false),
        MatchLineupPlacement(1, 21, onPitch = false)
    )
    private val plan = SubstitutionPlanRules.planOf(original)

    @Test
    fun droppingSubstituteOnPitchPlayerSwapsThem() {
        val updated = SubstitutionPlanRules.swap(plan, 20, 11)

        val incoming = updated.getValue(20)
        assertTrue(incoming.onPitch)
        assertEquals(0.3f, incoming.normalizedX)
        assertEquals("DEF-1", incoming.formationSlot)
        val outgoing = updated.getValue(11)
        assertFalse(outgoing.onPitch)
        assertEquals("", outgoing.formationSlot)
        assertEquals(listOf(11L), SubstitutionPlanRules.changes(original, updated).outgoing)
        assertEquals(listOf(20L), SubstitutionPlanRules.changes(original, updated).incoming)
    }

    @Test
    fun swappingTwoPitchPlayersOnlyMovesThem() {
        val updated = SubstitutionPlanRules.swap(plan, 11, 12)

        val changes = SubstitutionPlanRules.changes(original, updated)
        assertFalse(changes.hasSubstitutions)
        assertEquals(listOf(11L, 12L), changes.moved)
        assertEquals("FWD-1", updated.getValue(11).formationSlot)
    }

    @Test
    fun benchPlayerCannotJoinFullPitch() {
        val drop = PitchDrop(0.5f, 0.5f)

        assertNull(SubstitutionPlanRules.moveToPitch(plan, 20, drop, maximumOnPitch = 3))
        assertNotNull(SubstitutionPlanRules.moveToPitch(plan, 20, drop, maximumOnPitch = 4))
    }

    @Test
    fun movingBackToOriginalLineupLeavesNoChanges() {
        val there = SubstitutionPlanRules.swap(plan, 20, 11)
        val back = SubstitutionPlanRules.swap(there, 11, 20)

        assertTrue(SubstitutionPlanRules.changes(original, back).isEmpty)
    }

    @Test
    fun substitutesArePairedWithNearestOutgoingPlayer() {
        var updated = SubstitutionPlanRules.swap(plan, 20, 12)
        updated = SubstitutionPlanRules.swap(updated, 21, 11)

        val pairs = SubstitutionPlanRules.pairs(original, updated)

        assertEquals(
            setOf(SubstitutionPair(12, 20), SubstitutionPair(11, 21)),
            pairs.toSet()
        )
    }

    @Test
    fun unmatchedChangesBecomeSingleOnOrOffEntries() {
        val updated = SubstitutionPlanRules.moveToBench(plan, 12)

        assertEquals(listOf(SubstitutionPair(12, null)), SubstitutionPlanRules.pairs(original, updated))
    }

    private val slots = listOf(
        FormationSlot("GK", "GK", 0.5f, 0.9f),
        FormationSlot("DEF-1", "DEF", 0.3f, 0.6f),
        FormationSlot("FWD-1", "FWD", 0.7f, 0.3f),
        FormationSlot("FWD-2", "FWD", 0.3f, 0.3f)
    )

    private fun drop(plan: Map<Long, MatchLineupPlacement>, id: Long, x: Float, y: Float, max: Int = 4) =
        SubstitutionPlanRules.resolveDrop(plan, id, x, y, slots, max, 400f, 600f, playerHitRadiusPx = 40f)

    @Test
    fun releasingNearAPlayerSwapsWithThem() {
        assertEquals(SubstitutionDrop.Swap(11), drop(plan, 20, 0.33f, 0.62f))
    }

    @Test
    fun fullPitchSnapsSubstituteToNearestPlayer() {
        // Far from everyone, but the pitch already has its 3 players.
        assertEquals(SubstitutionDrop.Swap(12), drop(plan, 20, 0.85f, 0.15f, max = 3))
    }

    @Test
    fun freeSlotNearDropIsSnappedButOccupiedSlotIsNot() {
        val result = drop(plan, 20, 0.33f, 0.35f) as SubstitutionDrop.Place
        assertEquals("FWD-2", result.drop.formationSlot)
        assertTrue(result.drop.snapped)
    }

    @Test
    fun playerStandingOnASlotOccupiesItWithoutItsSlotId() {
        val practice = listOf(MatchLineupPlacement(1, 30, 0.31f, 0.31f, "", "practice-3", onPitch = true))

        assertTrue("FWD-2" in LineupDragDropRules.occupiedSlotIds(slots, practice))
        assertTrue("practice-3" in LineupDragDropRules.occupiedSlotIds(slots, practice))
        assertFalse("FWD-1" in LineupDragDropRules.occupiedSlotIds(slots, practice))
    }
}
