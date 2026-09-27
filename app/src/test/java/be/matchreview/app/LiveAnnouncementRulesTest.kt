package be.matchreview.app

import be.matchreview.app.domain.LiveAnnouncementRules
import org.junit.Assert.*
import org.junit.Test

class LiveAnnouncementRulesTest {
    @Test fun clockOnlyAnnouncesMinuteBoundaries() {
        assertFalse(LiveAnnouncementRules.shouldAnnounceClock(10_000, 11_000))
        assertTrue(LiveAnnouncementRules.shouldAnnounceClock(59_000, 60_000))
        assertEquals("Score 2 to 1", LiveAnnouncementRules.scoreAnnouncement(2, 1))
    }
}
