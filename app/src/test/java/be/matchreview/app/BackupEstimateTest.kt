package be.matchreview.app

import be.matchreview.app.domain.BackupEstimate
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupEstimateTest {
    @Test fun excludesVideoWhenMediaIsOff() {
        assertEquals(2L * 1024L * 1024L, BackupEstimate.estimatedBytes(900_000_000L, false))
    }

    @Test fun includesRecordedBytesWhenMediaIsOn() {
        assertEquals(12L * 1024L * 1024L, BackupEstimate.estimatedBytes(10L * 1024L * 1024L, true))
    }
}
