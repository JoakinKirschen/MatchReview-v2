package be.matchreview.app.domain

/**
 * Suppresses accidental duplicate commands while allowing different match-day actions.
 * The gate is deliberately small and in-memory: Room transactions remain authoritative.
 */
class LiveCommandGate(private val duplicateWindowMs: Long = 1_200L) {
    private val acceptedAt = mutableMapOf<String, Long>()

    @Synchronized
    fun accept(key: String, nowMs: Long): Boolean {
        val previous = acceptedAt[key]
        if (previous != null && nowMs - previous in 0 until duplicateWindowMs) return false
        acceptedAt[key] = nowMs
        if (acceptedAt.size > 64) {
            val cutoff = nowMs - duplicateWindowMs * 4
            acceptedAt.entries.removeAll { it.value < cutoff }
        }
        return true
    }

    @Synchronized
    fun clear(key: String) {
        acceptedAt.remove(key)
    }
}
