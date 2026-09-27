package be.matchreview.app

import be.matchreview.app.domain.ClockRecoveryConfidence
import be.matchreview.app.domain.ClockRecoveryRules
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockRecoveryRulesTest {
    @Test fun sameBootUsesMonotonicTime() {
        val result = ClockRecoveryRules.assess(5_000, 10_000, 100_000, 20_000, 110_000)
        assertEquals(15_000, result.recoveredMatchTimeMs)
        assertEquals(ClockRecoveryConfidence.SAME_BOOT, result.confidence)
    }

    @Test fun suspiciousWallClockChangeNeedsConfirmation() {
        val result = ClockRecoveryRules.assess(0, 10_000, 100_000, 20_000, 500_000)
        assertEquals(ClockRecoveryConfidence.NEEDS_CONFIRMATION, result.confidence)
    }
}
