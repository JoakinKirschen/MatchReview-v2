package be.matchreview.app.domain

import be.matchreview.app.data.MatchLineupPlacement
import kotlin.math.abs

/** Lineup differences between the live lineup and a planned substitution round. */
data class SubstitutionChanges(
    val outgoing: List<Long>,
    val incoming: List<Long>,
    val moved: List<Long>
) {
    val hasSubstitutions: Boolean get() = outgoing.isNotEmpty() || incoming.isNotEmpty()
    val isEmpty: Boolean get() = !hasSubstitutions && moved.isEmpty()
}

/** What happens when a dragged player is released on the pitch. */
sealed interface SubstitutionDrop {
    /** Exchange places with this player (a substitution when one of them is on the bench). */
    data class Swap(val targetPlayerId: Long) : SubstitutionDrop
    /** Take this spot on the pitch, snapped to a free formation slot when close to one. */
    data class Place(val drop: PitchDrop) : SubstitutionDrop
}

/** A player off and the player who replaced them; either side may be missing. */
data class SubstitutionPair(val outgoingId: Long?, val incomingId: Long?)

/**
 * Pure rules for substitution mode. The plan is edited with drag and drop while the
 * match continues; nothing is written until the coach confirms the round, so the
 * minutes of the players on the pitch keep counting until then.
 */
object SubstitutionPlanRules {
    fun planOf(placements: List<MatchLineupPlacement>): Map<Long, MatchLineupPlacement> =
        placements.associateBy { it.playerId }

    /**
     * Dropping a player onto another player exchanges their places. Dropping a bench
     * player onto a pitch player is a substitution: the substitute takes the exact
     * position and the replaced player goes to the bench.
     */
    fun swap(
        plan: Map<Long, MatchLineupPlacement>,
        draggedId: Long,
        targetId: Long
    ): Map<Long, MatchLineupPlacement> {
        if (draggedId == targetId) return plan
        val dragged = plan[draggedId] ?: return plan
        val target = plan[targetId] ?: return plan
        val newDragged = dragged.copy(
            normalizedX = target.normalizedX,
            normalizedY = target.normalizedY,
            formationSlot = if (target.onPitch) target.formationSlot else "",
            role = if (target.onPitch) target.role else "",
            onPitch = target.onPitch
        )
        val newTarget = target.copy(
            normalizedX = dragged.normalizedX,
            normalizedY = dragged.normalizedY,
            formationSlot = if (dragged.onPitch) dragged.formationSlot else "",
            role = if (dragged.onPitch) dragged.role else "",
            onPitch = dragged.onPitch
        )
        return plan + (draggedId to newDragged) + (targetId to newTarget)
    }

    /** Returns null when a bench player would exceed the configured match size. */
    fun moveToPitch(
        plan: Map<Long, MatchLineupPlacement>,
        playerId: Long,
        drop: PitchDrop,
        maximumOnPitch: Int
    ): Map<Long, MatchLineupPlacement>? {
        val existing = plan[playerId] ?: return plan
        if (!existing.onPitch && plan.values.count { it.onPitch } >= maximumOnPitch) return null
        return plan + (playerId to existing.copy(
            normalizedX = drop.normalizedX,
            normalizedY = drop.normalizedY,
            formationSlot = drop.formationSlot,
            role = drop.role.ifBlank { existing.role },
            onPitch = true
        ))
    }

    fun moveToBench(
        plan: Map<Long, MatchLineupPlacement>,
        playerId: Long
    ): Map<Long, MatchLineupPlacement> {
        val existing = plan[playerId] ?: return plan
        if (!existing.onPitch) return plan
        return plan + (playerId to existing.copy(onPitch = false, formationSlot = "", role = ""))
    }

    /**
     * Decides where a released player goes. Distances are measured in pixels so the hit
     * area is round on screen whatever the pitch's aspect ratio.
     *
     * - Released on (or near) another pitch player: swap with them.
     * - A substitute released while the pitch is full: replace the nearest player, so the
     *   drop always snaps to someone instead of failing.
     * - Otherwise: place it, snapping to a free formation slot when one is near.
     */
    fun resolveDrop(
        plan: Map<Long, MatchLineupPlacement>,
        draggedId: Long,
        normalizedX: Float,
        normalizedY: Float,
        slots: List<FormationSlot>,
        maximumOnPitch: Int,
        pitchWidthPx: Float,
        pitchHeightPx: Float,
        playerHitRadiusPx: Float
    ): SubstitutionDrop? {
        val dragged = plan[draggedId] ?: return null
        val others = plan.values.filter { it.onPitch && it.playerId != draggedId }
        val nearest = others.minByOrNull { other ->
            val dx = (other.normalizedX - normalizedX) * pitchWidthPx
            val dy = (other.normalizedY - normalizedY) * pitchHeightPx
            dx * dx + dy * dy
        }
        if (nearest != null) {
            val dx = (nearest.normalizedX - normalizedX) * pitchWidthPx
            val dy = (nearest.normalizedY - normalizedY) * pitchHeightPx
            if (dx * dx + dy * dy <= playerHitRadiusPx * playerHitRadiusPx) {
                return SubstitutionDrop.Swap(nearest.playerId)
            }
        }
        if (!dragged.onPitch && plan.values.count { it.onPitch } >= maximumOnPitch) {
            return nearest?.let { SubstitutionDrop.Swap(it.playerId) }
        }
        val occupied = LineupDragDropRules.occupiedSlotIds(slots, others)
        return SubstitutionDrop.Place(
            LineupDragDropRules.resolvePitchDrop(normalizedX, normalizedY, slots, occupied)
        )
    }

    fun changes(
        original: List<MatchLineupPlacement>,
        plan: Map<Long, MatchLineupPlacement>
    ): SubstitutionChanges {
        val before = original.associateBy { it.playerId }
        val outgoing = original
            .filter { it.onPitch && plan[it.playerId]?.onPitch == false }
            .map { it.playerId }
        val incoming = plan.values
            .filter { it.onPitch && before[it.playerId]?.onPitch != true }
            .map { it.playerId }
            .sorted()
        val moved = plan.values
            .filter { planned ->
                val old = before[planned.playerId]
                planned.onPitch && old?.onPitch == true &&
                    (abs(old.normalizedX - planned.normalizedX) > 0.001f ||
                        abs(old.normalizedY - planned.normalizedY) > 0.001f)
            }
            .map { it.playerId }
            .sorted()
        return SubstitutionChanges(outgoing, incoming, moved)
    }

    /**
     * Pairs every substitute with the outgoing player whose former position is closest
     * to where the substitute now plays. Leftovers become plain "off" or "on" entries.
     */
    fun pairs(
        original: List<MatchLineupPlacement>,
        plan: Map<Long, MatchLineupPlacement>
    ): List<SubstitutionPair> {
        val changes = changes(original, plan)
        val before = original.associateBy { it.playerId }
        val candidates = changes.incoming.flatMap { incomingId ->
            val newSpot = plan.getValue(incomingId)
            changes.outgoing.map { outgoingId ->
                val oldSpot = before.getValue(outgoingId)
                val dx = oldSpot.normalizedX - newSpot.normalizedX
                val dy = oldSpot.normalizedY - newSpot.normalizedY
                Triple(outgoingId, incomingId, dx * dx + dy * dy)
            }
        }.sortedWith(compareBy<Triple<Long, Long, Float>> { it.third }.thenBy { it.first }.thenBy { it.second })

        val usedOut = mutableSetOf<Long>()
        val usedIn = mutableSetOf<Long>()
        val result = mutableListOf<SubstitutionPair>()
        candidates.forEach { (outgoingId, incomingId, _) ->
            if (outgoingId !in usedOut && incomingId !in usedIn) {
                usedOut += outgoingId
                usedIn += incomingId
                result += SubstitutionPair(outgoingId, incomingId)
            }
        }
        changes.outgoing.filterNot { it in usedOut }.forEach { result += SubstitutionPair(it, null) }
        changes.incoming.filterNot { it in usedIn }.forEach { result += SubstitutionPair(null, it) }
        return result
    }
}
