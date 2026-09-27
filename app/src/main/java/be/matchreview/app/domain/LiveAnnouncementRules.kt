package be.matchreview.app.domain

/** Avoids announcing a ticking clock every second to screen-reader users. */
object LiveAnnouncementRules {
    fun shouldAnnounceClock(previousMs: Long, currentMs: Long): Boolean {
        if (currentMs < previousMs) return true
        return previousMs / 60_000L != currentMs / 60_000L
    }

    fun scoreAnnouncement(ourScore: Int, opponentScore: Int): String =
        "Score $ourScore to $opponentScore"
}
