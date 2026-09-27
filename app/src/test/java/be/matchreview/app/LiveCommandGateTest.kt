package be.matchreview.app

import be.matchreview.app.domain.LiveCommandGate
import org.junit.Assert.*
import org.junit.Test

class LiveCommandGateTest {
    @Test fun duplicateWithinWindowIsRejected() {
        val gate = LiveCommandGate(1_000)
        assertTrue(gate.accept("goal", 10_000))
        assertFalse(gate.accept("goal", 10_500))
        assertTrue(gate.accept("goal", 11_000))
        assertTrue(gate.accept("substitution", 10_500))
    }
}
