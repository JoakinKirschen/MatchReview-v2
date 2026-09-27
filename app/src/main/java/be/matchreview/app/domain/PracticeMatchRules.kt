package be.matchreview.app.domain

data class PracticePlayerTemplate(val name: String, val shirtNumber: Int, val position: String)

object PracticeMatchRules {
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
