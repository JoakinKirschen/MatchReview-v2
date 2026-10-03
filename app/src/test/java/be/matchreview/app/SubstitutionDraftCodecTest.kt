package be.matchreview.app

import be.matchreview.app.data.MatchLineupPlacement
import be.matchreview.app.domain.SubstitutionDraftCodec
import org.junit.Assert.*
import org.junit.Test

class SubstitutionDraftCodecTest {
    private val plan = listOf(
        MatchLineupPlacement(4, 12, 0.25f, 0.5f, "DEF", "DEF-1", onPitch = true),
        MatchLineupPlacement(4, 3, 0.5f, 0.9f, "GK", "GK", onPitch = true),
        MatchLineupPlacement(4, 20, 0.5f, 0.85f, "", "", onPitch = false)
    )

    @Test
    fun draftRoundTripsEveryPlacementField() {
        val decoded = SubstitutionDraftCodec.decode(4, SubstitutionDraftCodec.encode(plan))!!

        assertEquals(plan.sortedBy { it.playerId }, decoded)
    }

    @Test
    fun damagedDraftsAreIgnored() {
        assertNull(SubstitutionDraftCodec.decode(4, null))
        assertNull(SubstitutionDraftCodec.decode(4, "1,1,0.5"))
        assertNull(SubstitutionDraftCodec.decode(4, "x,1,0.5,0.5,,"))
        assertNull(SubstitutionDraftCodec.decode(4, "1,1,0.5,0.5,,;1,0,0.5,0.5,,"))
    }

    @Test
    fun separatorsInLabelsCannotBreakTheDraft() {
        val odd = listOf(MatchLineupPlacement(4, 1, role = "A,B;C", formationSlot = "S;1", onPitch = true))

        val decoded = SubstitutionDraftCodec.decode(4, SubstitutionDraftCodec.encode(odd))!!.single()

        assertEquals("A_B_C", decoded.role)
        assertEquals("S_1", decoded.formationSlot)
    }

    @Test
    fun draftOnlyFitsTheSamePlayers() {
        assertTrue(SubstitutionDraftCodec.fitsLineup(plan, plan.reversed()))
        assertFalse(SubstitutionDraftCodec.fitsLineup(plan, plan.drop(1)))
    }
}
