package be.matchreview.app

import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import be.matchreview.app.data.MatchLineupPlacement
import be.matchreview.app.domain.FormationLayout
import be.matchreview.app.ui.MatchReviewTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Drives real long-press drag gestures on the lineup builder and checks the saved
 * placements, so bench-to-pitch, pitch-to-bench and snapping keep working.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
class LineupDragAndDropTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: MatchReviewApplication
        get() = ApplicationProvider.getApplicationContext()

    private data class Fixture(val matchId: Long, val playerIds: List<Long>)

    /** A 5-a-side match (formation 2-2) with six selected players, all on the bench. */
    private fun showLineup(): Fixture {
        val fixture = runBlocking {
            val repository = app.repository
            val teamId = repository.addTeam("U12", "", "", "")
            val playerIds = (1..6).map { repository.addPlayer(teamId, "Player$it", it, "") }
            val matchId = repository.addMatch(
                teamId, "Rivals", "2026-09-26", "", "", true, "2-2",
                periodCount = 2, periodDurationMinutes = 25, playersOnPitch = 5, rollingSubstitutions = true
            )
            repository.ensureMatchSquad(matchId, teamId)
            playerIds.forEach { repository.setSquadSelected(matchId, it, true) }
            repository.ensureLineup(matchId)
            Fixture(matchId, playerIds)
        }
        compose.setContent {
            val vm = remember { MainViewModel(app) }
            MatchReviewTheme {
                LineupBuilderScreen(fixture.matchId, vm, rememberNavController())
            }
        }
        waitForTag(LineupTestTags.benchPlayer(fixture.playerIds.first()))
        return fixture
    }

    /**
     * An 8v8 match (2-4-1) with ten selected players that was auto-placed: players 1-8
     * fill GK, DEF-1, DEF-2, MID-1..4 and FWD-1; players 9 and 10 are on the bench.
     */
    private fun showAutoPlaced8v8(): Fixture {
        val fixture = runBlocking {
            val repository = app.repository
            val teamId = repository.addTeam("U11", "", "", "")
            val playerIds = (1..10).map { repository.addPlayer(teamId, "Player$it", it, "") }
            val matchId = repository.addMatch(
                teamId, "Rivals", "2026-10-03", "", "", true, "2-4-1",
                periodCount = 4, periodDurationMinutes = 15, playersOnPitch = 8, rollingSubstitutions = true
            )
            repository.ensureMatchSquad(matchId, teamId)
            playerIds.forEach { repository.setSquadSelected(matchId, it, true) }
            repository.ensureLineup(matchId)
            val players = app.database.matchDao().getPlayersForTeam(teamId)
            repository.autoPlaceLineup(matchId, players, FormationLayout.slots("2-4-1", 8))
            Fixture(matchId, playerIds)
        }
        compose.setContent {
            val vm = remember { MainViewModel(app) }
            MatchReviewTheme {
                LineupBuilderScreen(fixture.matchId, vm, rememberNavController())
            }
        }
        waitForTag(LineupTestTags.benchPlayer(fixture.playerIds[8]))
        waitForTag(LineupTestTags.pitchPlayer(fixture.playerIds[0]))
        return fixture
    }

    private fun benchCenter(): Offset =
        compose.onNodeWithTag(LineupTestTags.BENCH).fetchSemanticsNode().boundsInRoot.center

    @Test
    fun benchPlayerCanFillAFreedSlotRightAfterOthersWentToTheBench() {
        val fixture = showAutoPlaced8v8()
        val (def1, def2) = fixture.playerIds.subList(1, 3)

        longPressDrag(LineupTestTags.pitchPlayer(def1), benchCenter())
        waitForPlacement(fixture, def1) { !it.onPitch }
        longPressDrag(LineupTestTags.pitchPlayer(def2), benchCenter())
        waitForPlacement(fixture, def2) { !it.onPitch }
        waitForTag(LineupTestTags.benchPlayer(def2))

        // 6/8 on the pitch: a bench player dropped on the free DEF-1 spot takes it at once.
        val substitute = fixture.playerIds[8]
        longPressDrag(LineupTestTags.benchPlayer(substitute), pitchPoint(1f / 3f, 0.72f))

        waitForPlacement(fixture, substitute) { it.onPitch }
        assertEquals("DEF-1", placement(fixture, substitute)!!.formationSlot)
    }

    @Test
    fun benchPlayerDroppedOnPitchPlayerOfFullPitchSwapsWithThem() {
        val fixture = showAutoPlaced8v8()
        val midfielder = fixture.playerIds[4] // MID-2
        val substitute = fixture.playerIds[9]
        val target = compose.onNodeWithTag(LineupTestTags.pitchPlayer(midfielder))
            .fetchSemanticsNode().boundsInRoot.center

        longPressDrag(LineupTestTags.benchPlayer(substitute), target)

        waitForPlacement(fixture, substitute) { it.onPitch }
        waitForPlacement(fixture, midfielder) { !it.onPitch }
        assertEquals("MID-2", placement(fixture, substitute)!!.formationSlot)
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun placement(fixture: Fixture, playerId: Long): MatchLineupPlacement? = runBlocking {
        app.database.matchDao().getLineupPlacementOnce(fixture.matchId, playerId)
    }

    private fun waitForPlacement(fixture: Fixture, playerId: Long, condition: (MatchLineupPlacement) -> Boolean) {
        compose.waitUntil(10_000) { placement(fixture, playerId)?.let(condition) == true }
    }

    /** Long-presses the node, drags it in small steps to [targetInRoot] and releases. */
    private fun longPressDrag(tag: String, targetInRoot: Offset) {
        val node = compose.onNodeWithTag(tag)
        val topLeft = node.fetchSemanticsNode().boundsInRoot.topLeft
        node.performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            moveBy(Offset(1f, 1f))
            val target = targetInRoot - topLeft
            val start = center + Offset(1f, 1f)
            val steps = 20
            for (step in 1..steps) {
                moveTo(start + (target - start) * (step / steps.toFloat()))
            }
            up()
        }
    }

    private fun pitchPoint(normalizedX: Float, normalizedY: Float): Offset {
        val pitch = compose.onNodeWithTag(LineupTestTags.PITCH).fetchSemanticsNode().boundsInRoot
        return Offset(pitch.left + pitch.width * normalizedX, pitch.top + pitch.height * normalizedY)
    }

    @Test
    fun dragFromBenchPlacesPlayerOnPitchWhereDropped() {
        val fixture = showLineup()
        val player = fixture.playerIds[0]

        longPressDrag(LineupTestTags.benchPlayer(player), pitchPoint(0.5f, 0.5f))

        waitForPlacement(fixture, player) { it.onPitch }
        val saved = placement(fixture, player)!!
        assertEquals(0.5f, saved.normalizedX, 0.05f)
        assertEquals(0.5f, saved.normalizedY, 0.05f)
        waitForTag(LineupTestTags.pitchPlayer(player))
        // The drag finished cleanly: controls that are disabled mid-drag are enabled again.
        compose.onNodeWithText("Auto-place").assertIsEnabled()
        compose.onNodeWithText("Match day").assertIsEnabled()
    }

    @Test
    fun dropNearFreeSlotSnapsToIt() {
        val fixture = showLineup()
        val player = fixture.playerIds[1]

        longPressDrag(LineupTestTags.benchPlayer(player), pitchPoint(0.52f, 0.87f))

        waitForPlacement(fixture, player) { it.onPitch }
        assertEquals("GK", placement(fixture, player)!!.formationSlot)
    }

    @Test
    fun dragFromPitchToBenchMovesPlayerOff() {
        val fixture = showLineup()
        val player = fixture.playerIds[2]
        longPressDrag(LineupTestTags.benchPlayer(player), pitchPoint(0.5f, 0.5f))
        waitForPlacement(fixture, player) { it.onPitch }
        waitForTag(LineupTestTags.pitchPlayer(player))

        val bench = compose.onNodeWithTag(LineupTestTags.BENCH).fetchSemanticsNode().boundsInRoot
        longPressDrag(LineupTestTags.pitchPlayer(player), bench.center)

        waitForPlacement(fixture, player) { !it.onPitch }
        waitForTag(LineupTestTags.benchPlayer(player))
        compose.onNodeWithText("Auto-place").assertIsEnabled()
    }

    @Test
    fun severalPlayersCanBeDraggedOneAfterAnother() {
        val fixture = showLineup()
        val targets = listOf(0.3f to 0.3f, 0.7f to 0.3f, 0.5f to 0.6f)

        fixture.playerIds.take(3).zip(targets).forEach { (player, target) ->
            longPressDrag(LineupTestTags.benchPlayer(player), pitchPoint(target.first, target.second))
            waitForPlacement(fixture, player) { it.onPitch }
        }

        fixture.playerIds.take(3).forEach { waitForTag(LineupTestTags.pitchPlayer(it)) }
    }
}
