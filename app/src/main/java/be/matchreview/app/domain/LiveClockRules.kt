package be.matchreview.app.domain

import be.matchreview.app.data.GameMatch
import be.matchreview.app.data.MatchClockSegment
import be.matchreview.app.data.MatchPeriod
import be.matchreview.app.data.MatchStatus

/** The clock of a live match at one moment, for the notification. */
data class LiveClock(
    val matchTimeMs: Long,
    val periodTimeMs: Long,
    val plannedPeriodMs: Long,
    val running: Boolean
) {
    /** Time left until the planned period length is reached; null when it is not counting down. */
    val msUntilPeriodEnd: Long?
        get() = if (running && plannedPeriodMs > 0L && periodTimeMs < plannedPeriodMs) {
            plannedPeriodMs - periodTimeMs
        } else null
}

object LiveClockRules {
    val ACTIVE_STATUSES = setOf(MatchStatus.LIVE, MatchStatus.PAUSED, MatchStatus.PERIOD_ENDED)

    fun clock(
        match: GameMatch,
        segments: List<MatchClockSegment>,
        periods: List<MatchPeriod>,
        monotonicNowMs: Long,
        wallClockNowMs: Long
    ): LiveClock {
        val open = segments.lastOrNull { it.monotonicEndMs == null }
        val running = match.clockRunning && open != null && match.status == MatchStatus.LIVE
        val matchTimeMs = if (running) {
            MatchClockCalculator.recoveredCurrentMatchTimeMs(
                open!!.startMatchTimeMs, open.monotonicStartMs, open.wallClockStartMs,
                monotonicNowMs, wallClockNowMs
            )
        } else match.accumulatedMatchTimeMs
        val period = periods.firstOrNull { it.periodNumber == match.currentPeriod }
        return LiveClock(
            matchTimeMs = matchTimeMs,
            periodTimeMs = MatchClockCalculator.periodElapsedMs(matchTimeMs, period?.startMatchTimeMs),
            plannedPeriodMs = period?.plannedDurationMs ?: (match.periodDurationMinutes * 60_000L),
            running = running
        )
    }
}
