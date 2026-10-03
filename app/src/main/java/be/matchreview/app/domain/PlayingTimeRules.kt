package be.matchreview.app.domain

/** Helps share playing time: which available substitutes have played the least. */
object PlayingTimeRules {
    /**
     * Candidates whose whole minutes equal the lowest among them. Nothing is marked when
     * everyone is level (for example at kick-off), because then nobody stands out.
     */
    fun leastPlayed(candidateIds: Collection<Long>, playedMs: (Long) -> Long): Set<Long> {
        if (candidateIds.size < 2) return emptySet()
        val minutes = candidateIds.associateWith { playedMs(it) / 60_000L }
        val lowest = minutes.values.min()
        if (minutes.values.all { it == lowest }) return emptySet()
        return minutes.filterValues { it == lowest }.keys
    }
}
