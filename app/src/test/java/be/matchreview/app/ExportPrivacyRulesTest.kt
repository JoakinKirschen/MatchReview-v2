package be.matchreview.app

import be.matchreview.app.domain.ExportPrivacyOptions
import be.matchreview.app.domain.ExportPrivacyRules
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportPrivacyRulesTest {
    @Test fun redactionRemovesNamesNotesAndUris() {
        val options = ExportPrivacyOptions(false, false, false)
        assertEquals("Player 7", ExportPrivacyRules.playerLabel("Alex", 7, options))
        assertEquals("", ExportPrivacyRules.note("private", options))
        assertEquals("", ExportPrivacyRules.mediaUri("content://video", options))
    }
}
