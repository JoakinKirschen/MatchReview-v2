package be.matchreview.app.domain

import be.matchreview.app.data.GameMatch
import be.matchreview.app.data.MatchStatus

data class SeasonSummary(
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val goalsFor: Int,
    val goalsAgainst: Int
)

object SeasonSummaryRules {
    fun summarize(matches: List<GameMatch>): SeasonSummary {
        val completed = matches.filter { it.status == MatchStatus.FINISHED }
        return SeasonSummary(
            played = completed.size,
            won = completed.count { it.ourScore > it.opponentScore },
            drawn = completed.count { it.ourScore == it.opponentScore },
            lost = completed.count { it.ourScore < it.opponentScore },
            goalsFor = completed.sumOf { it.ourScore },
            goalsAgainst = completed.sumOf { it.opponentScore }
        )
    }
}
