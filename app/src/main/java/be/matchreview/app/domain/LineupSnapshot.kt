package be.matchreview.app.domain

import be.matchreview.app.data.MatchLineupPlacement
import java.util.Locale

/** One on-pitch position stored with a lineup-change event. */
data class SnapshotPosition(
    val playerId: Long,
    val normalizedX: Float,
    val normalizedY: Float
)

/**
 * Compact text encoding of the players on the pitch after a lineup change, for example
 * `12:0.500:0.900;14:0.250:0.680`. Plain text keeps it inside the existing JSON backups
 * and readable without Android's JSON classes in pure unit tests.
 */
object LineupSnapshot {
    fun encode(placements: List<MatchLineupPlacement>): String =
        placements
            .filter { it.onPitch }
            .sortedBy { it.playerId }
            .joinToString(";") {
                "${it.playerId}:${format(it.normalizedX)}:${format(it.normalizedY)}"
            }

    fun decode(value: String?): List<SnapshotPosition> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(';').mapNotNull { entry ->
            val parts = entry.split(':')
            if (parts.size != 3) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val x = parts[1].toFloatOrNull() ?: return@mapNotNull null
            val y = parts[2].toFloatOrNull() ?: return@mapNotNull null
            SnapshotPosition(id, x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
        }
    }

    private fun format(value: Float): String =
        String.format(Locale.ROOT, "%.3f", value.coerceIn(0f, 1f))
}
