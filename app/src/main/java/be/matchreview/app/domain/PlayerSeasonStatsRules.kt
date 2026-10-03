package be.matchreview.app.domain

import be.matchreview.app.data.GameMatch
import be.matchreview.app.data.MatchEvent
import be.matchreview.app.data.MatchStatus
import be.matchreview.app.data.Player
import be.matchreview.app.data.PlayerParticipation

data class PlayerSeasonStats(
    val player: Player,
    val matchesPlayed: Int,
    val minutesPlayedMs: Long,
    val goals: Int,
    val assists: Int,
    val saves: Int
) {
    val wholeMinutes: Long get() = minutesPlayedMs / 60_000L
}

/** Per-player totals across a team's matches, for sharing playing time fairly. */
object PlayerSeasonStatsRules {
    /** Matches that have been played or are being played; drafts and cancelled ones are left out. */
    private val countedStatuses = setOf(
        MatchStatus.LIVE, MatchStatus.PAUSED, MatchStatus.PERIOD_ENDED, MatchStatus.FINISHED
    )

    fun compute(
        players: List<Player>,
        matches: List<GameMatch>,
        participations: List<PlayerParticipation>,
        events: List<MatchEvent>
    ): List<PlayerSeasonStats> {
        val counted = matches.filter { it.status in countedStatuses }.associateBy { it.id }
        val intervals = participations.filter { it.matchId in counted }
        val countedEvents = events.filter { it.matchId in counted }
        return players.map { player ->
            val own = intervals.filter { it.playerId == player.id }
            PlayerSeasonStats(
                player = player,
                matchesPlayed = own.map { it.matchId }.distinct().size,
                minutesPlayedMs = own.sumOf { interval ->
                    // An open interval belongs to a match in progress; count up to its clock.
                    val end = interval.endMatchTimeMs
                        ?: counted[interval.matchId]?.accumulatedMatchTimeMs
                        ?: interval.startMatchTimeMs
                    (end - interval.startMatchTimeMs).coerceAtLeast(0L)
                },
                goals = countedEvents.count { it.type == "OUR_GOAL" && it.playerId == player.id },
                assists = countedEvents.count { it.type == "OUR_GOAL" && it.relatedPlayerId == player.id },
                saves = countedEvents.count { it.type == "KEEPER_SAVE" && it.playerId == player.id }
            )
        }.sortedWith(
            compareByDescending<PlayerSeasonStats> { it.minutesPlayedMs }
                .thenBy { it.player.shirtNumber.takeIf { number -> number > 0 } ?: Int.MAX_VALUE }
                .thenBy { it.player.name }
        )
    }
}
