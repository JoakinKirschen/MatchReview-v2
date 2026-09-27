package be.matchreview.app

import be.matchreview.app.data.GameMatch
import be.matchreview.app.data.MatchEvent
import be.matchreview.app.data.Player
import be.matchreview.app.domain.ExportPrivacyOptions
import be.matchreview.app.domain.MatchExportFormatter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacySafeExportTest {
    @Test fun redactedCsvDoesNotContainNameNoteOrUri() {
        val csv = MatchExportFormatter.toCsv(
            match = GameMatch(teamId=1, opponent="Visitors", matchDate="2026-09-27"),
            players = listOf(Player(id=7, teamId=1, name="Sensitive Name")),
            events = listOf(MatchEvent(matchId=1, playerId=7, timestampMs=1, type="NOTE", note="Private note")),
            participations = emptyList(),
            recordings = emptyList(),
            privacy = ExportPrivacyOptions(false, false, false)
        )
        assertFalse(csv.contains("Sensitive Name"))
        assertFalse(csv.contains("Private note"))
        assertTrue(csv.contains("Player 7"))
    }
}
