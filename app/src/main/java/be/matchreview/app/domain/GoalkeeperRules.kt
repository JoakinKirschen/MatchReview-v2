package be.matchreview.app.domain

import be.matchreview.app.data.MatchLineupPlacement
import be.matchreview.app.data.MatchSquadPlayer
import be.matchreview.app.data.Player

/** Finds who is keeping goal so a save can be recorded with one tap. */
object GoalkeeperRules {
    fun onPitchGoalkeepers(
        placements: List<MatchLineupPlacement>,
        squad: List<MatchSquadPlayer>,
        players: List<Player>
    ): List<Long> {
        val onPitch = placements.filter { it.onPitch }
        val bySlot = onPitch.filter { isGoalkeeperLabel(it.formationSlot) || isGoalkeeperLabel(it.role) }
        if (bySlot.isNotEmpty()) return bySlot.map { it.playerId }
        val onPitchIds = onPitch.mapTo(mutableSetOf()) { it.playerId }
        val bySquad = squad.filter { it.isGoalkeeper && it.playerId in onPitchIds }
        if (bySquad.isNotEmpty()) return bySquad.map { it.playerId }
        return players
            .filter { it.id in onPitchIds && isGoalkeeperLabel(it.position) }
            .map { it.id }
    }

    fun isGoalkeeperLabel(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.equals("GK", ignoreCase = true) ||
            trimmed.contains("goal", ignoreCase = true) ||
            trimmed.contains("keeper", ignoreCase = true)
    }
}
