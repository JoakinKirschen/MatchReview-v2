package be.matchreview.app.domain

/** When the planned length of a period is reached, and by how much it has run over. */
object PeriodTimeRules {
    /** Alerts only fire this close to the moment, not when the screen is reopened much later. */
    const val ALERT_WINDOW_MS = 30_000L

    fun isOver(periodTimeMs: Long, plannedMs: Long): Boolean =
        plannedMs > 0L && periodTimeMs >= plannedMs

    fun overtimeMs(periodTimeMs: Long, plannedMs: Long): Long =
        if (isOver(periodTimeMs, plannedMs)) periodTimeMs - plannedMs else 0L

    fun shouldAlert(periodTimeMs: Long, plannedMs: Long, alreadyAlerted: Boolean): Boolean =
        !alreadyAlerted &&
            isOver(periodTimeMs, plannedMs) &&
            periodTimeMs - plannedMs <= ALERT_WINDOW_MS
}
