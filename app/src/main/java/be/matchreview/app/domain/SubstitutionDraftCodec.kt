package be.matchreview.app.domain

import be.matchreview.app.data.MatchLineupPlacement
import java.util.Locale

/**
 * Stores an unconfirmed substitution round so it survives the app being closed or the
 * screen rotating. One entry per player: `playerId,onPitch,x,y,slot,role`, joined by `;`.
 */
object SubstitutionDraftCodec {
    fun encode(plan: List<MatchLineupPlacement>): String =
        plan.sortedBy { it.playerId }.joinToString(";") {
            listOf(
                it.playerId.toString(),
                if (it.onPitch) "1" else "0",
                String.format(Locale.ROOT, "%.4f", it.normalizedX),
                String.format(Locale.ROOT, "%.4f", it.normalizedY),
                clean(it.formationSlot),
                clean(it.role)
            ).joinToString(",")
        }

    fun decode(matchId: Long, value: String?): List<MatchLineupPlacement>? {
        if (value.isNullOrBlank()) return null
        val result = value.split(';').map { entry ->
            val parts = entry.split(',')
            if (parts.size != 6) return null
            MatchLineupPlacement(
                matchId = matchId,
                playerId = parts[0].toLongOrNull() ?: return null,
                normalizedX = parts[2].toFloatOrNull()?.coerceIn(0f, 1f) ?: return null,
                normalizedY = parts[3].toFloatOrNull()?.coerceIn(0f, 1f) ?: return null,
                formationSlot = parts[4],
                role = parts[5],
                onPitch = parts[1] == "1"
            )
        }
        return result.takeIf { list -> list.map { it.playerId }.distinct().size == list.size }
    }

    /**
     * A draft is only restored onto the same set of players it was made for; any other
     * lineup change in the meantime makes it stale.
     */
    fun fitsLineup(draft: List<MatchLineupPlacement>, live: List<MatchLineupPlacement>): Boolean =
        draft.mapTo(mutableSetOf()) { it.playerId } == live.mapTo(mutableSetOf()) { it.playerId }

    private fun clean(value: String): String = value.replace(Regex("[,;]"), "_")
}
