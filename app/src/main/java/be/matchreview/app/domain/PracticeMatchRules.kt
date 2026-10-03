package be.matchreview.app.domain

import be.matchreview.app.data.Team

data class PracticePlayerTemplate(val name: String, val shirtNumber: Int, val position: String)

object PracticeMatchRules {
    const val TEAM_NAME = "Practice team"
    const val SEASON = "Practice"

    fun isPracticeTeam(team: Team): Boolean = team.name == TEAM_NAME && team.season == SEASON

    val players: List<PracticePlayerTemplate> = listOf(
        PracticePlayerTemplate("Practice goalkeeper", 1, "GK"),
        PracticePlayerTemplate("Practice defender 1", 2, "DF"),
        PracticePlayerTemplate("Practice defender 2", 3, "DF"),
        PracticePlayerTemplate("Practice midfielder 1", 4, "MF"),
        PracticePlayerTemplate("Practice midfielder 2", 5, "MF"),
        PracticePlayerTemplate("Practice winger 1", 6, "MF"),
        PracticePlayerTemplate("Practice winger 2", 7, "MF"),
        PracticePlayerTemplate("Practice striker", 9, "FW")
    )
}
