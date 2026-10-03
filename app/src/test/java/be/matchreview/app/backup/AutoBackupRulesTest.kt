package be.matchreview.app.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoBackupRulesTest {
    @Test
    fun onlyOlderAutomaticBackupsAreDeleted() {
        val names = listOf(
            "matchreview-auto-20261001-100000.mrbak",
            "matchreview-auto-20261003-090000.mrbak",
            "MatchReview-2026-09-01-1200.mrbak", // saved by hand
            "matchreview-auto-20260920-180000.mrbak",
            "matchreview-auto-20261002-120000.mrbak",
            "notes.txt"
        )

        assertEquals(
            listOf("matchreview-auto-20261001-100000.mrbak", "matchreview-auto-20260920-180000.mrbak"),
            AutoBackupRules.toDelete(names, keep = 2)
        )
        assertTrue(AutoBackupRules.toDelete(names, keep = 5).isEmpty())
    }

    @Test
    fun namesSortInTimeOrder() {
        val earlier = AutoBackupRules.fileName(1_790_000_000_000L)
        val later = AutoBackupRules.fileName(1_790_000_060_000L)
        assertTrue(earlier.startsWith(AutoBackupRules.PREFIX) && earlier.endsWith(AutoBackupRules.EXTENSION))
        assertTrue(earlier < later)
    }
}
