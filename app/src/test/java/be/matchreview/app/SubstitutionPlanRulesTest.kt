package be.matchreview.app

import be.matchreview.app.data.MatchLineupPlacement
import be.matchreview.app.domain.PitchDrop
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
}
