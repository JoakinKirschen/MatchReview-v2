package be.matchreview.app

import be.matchreview.app.domain.StorageBudgetRules
import org.junit.Assert.*
import org.junit.Test

class StorageBudgetRulesTest {
    @Test fun reserveIsNeverAvailableToRecording() {
        val low = StorageBudgetRules.recordingBudget(StorageBudgetRules.SAFETY_RESERVE_BYTES)
        assertFalse(low.canStart)
        assertEquals(0, low.estimatedMinutes)
        val healthy = StorageBudgetRules.recordingBudget(
            StorageBudgetRules.SAFETY_RESERVE_BYTES + StorageBudgetRules.DEFAULT_BYTES_PER_MINUTE * 5
        )
        assertTrue(healthy.canStart)
        assertEquals(5, healthy.estimatedMinutes)
    }
}
