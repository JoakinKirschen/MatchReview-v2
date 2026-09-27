package be.matchreview.app

import be.matchreview.app.data.RecordingSegment
import be.matchreview.app.data.RecordingStatus
import be.matchreview.app.domain.MediaIntegrityRules
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaIntegrityRulesTest {
    @Test fun findsStaleAndInvalidCompletedSegments() {
        val issues = MediaIntegrityRules.inspect(listOf(
            RecordingSegment(id=1, matchId=1, status=RecordingStatus.RECORDING, matchClockStartMs=0),
            RecordingSegment(id=2, matchId=1, status=RecordingStatus.COMPLETED, matchClockStartMs=0, uri=null)
        ))
        assertEquals(2, issues.size)
    }
}
