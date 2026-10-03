package be.matchreview.app.domain

import be.matchreview.app.data.GameMatch

/** The settings a coach usually repeats from match to match, such as 8v8, 2-4-1, 4 × 15 min. */
data class MatchFormat(
    val playersOnPitch: Int,
    val formation: String,
    val periodCount: Int,
    val periodDurationMinutes: Int,
    val rollingSubstitutions: Boolean,
    val competition: String = "",
    val teamId: Long = 0L
)

object MatchFormatMemory {
    /** Keeps a remembered format usable: unsupported sizes and formations fall back to defaults. */
    fun sanitize(format: MatchFormat): MatchFormat {
        val size = format.playersOnPitch.takeIf { it in MatchSetupRules.supportedMatchSizes } ?: 11
        return format.copy(
            playersOnPitch = size,
            formation = MatchSetupRules.legalFormationOrDefault(size, format.formation),
            periodCount = format.periodCount.coerceIn(1, 8),
            periodDurationMinutes = format.periodDurationMinutes.coerceIn(1, 120)
        )
    }

    /**
     * Fallback when nothing was remembered yet: the most recently created real match.
     * Practice matches are ignored, since they always use their own fixed format.
     */
    fun fromLatestMatch(matches: List<GameMatch>, practiceTeamIds: Set<Long>): MatchFormat? =
        matches.filterNot { it.teamId in practiceTeamIds }
            .maxByOrNull { it.id }
            ?.let {
                sanitize(
                    MatchFormat(
                        playersOnPitch = it.playersOnPitch,
                        formation = it.formation,
                        periodCount = it.periodCount,
                        periodDurationMinutes = it.periodDurationMinutes,
                        rollingSubstitutions = it.rollingSubstitutions,
                        competition = it.competition,
                        teamId = it.teamId
                    )
                )
            }
}
