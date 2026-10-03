package be.matchreview.app.domain

import be.matchreview.app.data.GameMatch
import be.matchreview.app.data.MatchEvent
import be.matchreview.app.data.MatchStatus
import be.matchreview.app.data.Player
import be.matchreview.app.data.PlayerParticipation

/** Season numbers for one player; minutes are whole minutes. */
data class StatLine(
    val matches: Int,
    val minutes: Long,
    val goals: Int,
    val assists: Int,
    val saves: Int
)

data class PlayerSeasonStats(
    val player: Player,
    /** What the app recorded during matches. */
    val tracked: StatLine,
    /** What is shown: tracked numbers plus the coach's corrections, never below zero. */
    val total: StatLine
) {
    val matchesPlayed: Int get() = total.matches
    val wholeMinutes: Long get() = total.minutes
    val goals: Int get() = total.goals
    val assists: Int get() = total.assists
    val saves: Int get() = total.saves
    val isCorrected: Boolean get() = tracked != total
}

/** Corrections stored on a player so that tracked numbers plus corrections give the edited totals. */
data class StatAdjustments(
    val matches: Int,
    val minutes: Int,
    val goals: Int,
    val assists: Int,
    val saves: Int
)

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
            val playedMs = own.sumOf { interval ->
                // An open interval belongs to a match in progress; count up to its clock.
                val end = interval.endMatchTimeMs
                    ?: counted[interval.matchId]?.accumulatedMatchTimeMs
                    ?: interval.startMatchTimeMs
                (end - interval.startMatchTimeMs).coerceAtLeast(0L)
            }
            val tracked = StatLine(
                matches = own.map { it.matchId }.distinct().size,
                minutes = playedMs / 60_000L,
                goals = countedEvents.count { it.type == "OUR_GOAL" && it.playerId == player.id },
                assists = countedEvents.count { it.type == "OUR_GOAL" && it.relatedPlayerId == player.id },
                saves = countedEvents.count { it.type == "KEEPER_SAVE" && it.playerId == player.id }
            )
            PlayerSeasonStats(player, tracked, withCorrections(tracked, player))
        }.sortedWith(
            compareByDescending<PlayerSeasonStats> { it.total.minutes }
                .thenBy { it.player.shirtNumber.takeIf { number -> number > 0 } ?: Int.MAX_VALUE }
                .thenBy { it.player.name }
        )
    }

    fun withCorrections(tracked: StatLine, player: Player): StatLine = StatLine(
        matches = (tracked.matches + player.statMatchesAdjustment).coerceAtLeast(0),
        minutes = (tracked.minutes + player.statMinutesAdjustment).coerceAtLeast(0L),
        goals = (tracked.goals + player.statGoalsAdjustment).coerceAtLeast(0),
        assists = (tracked.assists + player.statAssistsAdjustment).coerceAtLeast(0),
        saves = (tracked.saves + player.statSavesAdjustment).coerceAtLeast(0)
    )

    /**
     * The corrections that turn [tracked] into the totals the coach typed. Later tracked
     * matches keep adding on top, so a correction for untracked matches stays right.
     */
    fun adjustmentsFor(tracked: StatLine, desired: StatLine): StatAdjustments = StatAdjustments(
        matches = desired.matches.coerceAtLeast(0) - tracked.matches,
        minutes = (desired.minutes.coerceAtLeast(0L) - tracked.minutes).toInt(),
        goals = desired.goals.coerceAtLeast(0) - tracked.goals,
        assists = desired.assists.coerceAtLeast(0) - tracked.assists,
        saves = desired.saves.coerceAtLeast(0) - tracked.saves
    )
}
