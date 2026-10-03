package be.matchreview.app.domain

import be.matchreview.app.data.MatchLineupPlacement

data class PitchDrop(
    val normalizedX: Float,
    val normalizedY: Float,
    val formationSlot: String = "",
    val role: String = "",
    val snapped: Boolean = false
)

object LineupDragDropRules {
    const val EDGE_INSET = 0.08f
    const val DEFAULT_SNAP_RADIUS = 0.11f

    fun normalize(
        rootX: Float,
        rootY: Float,
        pitchLeft: Float,
        pitchTop: Float,
        pitchWidth: Float,
        pitchHeight: Float
    ): Pair<Float, Float> {
        if (pitchWidth <= 0f || pitchHeight <= 0f) return 0.5f to 0.5f
        return ((rootX - pitchLeft) / pitchWidth).coerceIn(EDGE_INSET, 1f - EDGE_INSET) to
            ((rootY - pitchTop) / pitchHeight).coerceIn(EDGE_INSET, 1f - EDGE_INSET)
    }

    /** How close a player must stand to a formation slot to count as filling it. */
    const val SLOT_OCCUPIED_RADIUS = 0.07f

    /**
     * Slots that are taken, either because a player was saved in that slot or because a
     * player stands on it. Players placed freely (or with slot ids from another layout,
     * such as practice matches) would otherwise leave their slot looking free.
     */
    fun occupiedSlotIds(
        slots: List<FormationSlot>,
        others: List<MatchLineupPlacement>
    ): Set<String> {
        val byId = others.mapNotNullTo(mutableSetOf()) { it.formationSlot.takeIf(String::isNotBlank) }
        val byPosition = slots.filter { slot ->
            others.any { other ->
                val dx = other.normalizedX - slot.normalizedX
                val dy = other.normalizedY - slot.normalizedY
                dx * dx + dy * dy <= SLOT_OCCUPIED_RADIUS * SLOT_OCCUPIED_RADIUS
            }
        }.map { it.id }
        return byId + byPosition
    }

    fun resolvePitchDrop(
        normalizedX: Float,
        normalizedY: Float,
        slots: List<FormationSlot>,
        occupiedSlotIds: Set<String>,
        snapRadius: Float = DEFAULT_SNAP_RADIUS
    ): PitchDrop {
        val x = normalizedX.coerceIn(EDGE_INSET, 1f - EDGE_INSET)
        val y = normalizedY.coerceIn(EDGE_INSET, 1f - EDGE_INSET)
        val nearest = slots
            .asSequence()
            .filterNot { it.id in occupiedSlotIds }
            .map { slot ->
                val dx = slot.normalizedX - x
                val dy = slot.normalizedY - y
                val distanceSquared = dx * dx + dy * dy
                slot to distanceSquared
            }
            .minByOrNull { it.second }

        return if (nearest != null && nearest.second <= snapRadius * snapRadius) {
            PitchDrop(
                normalizedX = nearest.first.normalizedX,
                normalizedY = nearest.first.normalizedY,
                formationSlot = nearest.first.id,
                role = nearest.first.label,
                snapped = true
            )
        } else {
            PitchDrop(normalizedX = x, normalizedY = y)
        }
    }
}
