package be.matchreview.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import be.matchreview.app.data.GameMatch
import be.matchreview.app.data.MatchLineupPlacement
import be.matchreview.app.data.Player
import be.matchreview.app.domain.FormationLayout
import be.matchreview.app.domain.LineupDragDropRules
import be.matchreview.app.domain.SubstitutionPlanRules
import kotlin.math.roundToInt

private data class SubstitutionDrag(val playerId: Long, val pointerInRoot: Offset)

/**
 * Substitution mode: the live lineup is edited with the same long-press drag and drop as
 * the starting lineup. Changes stay local until the coach confirms, so the players on the
 * pitch keep collecting minutes until the whole round is applied at one match time.
 *
 * Drop a substitute onto a pitch player to swap them, onto free grass when the pitch is
 * not full, or drag a pitch player to the bench. Tapping two players swaps them too.
 */
@Composable
internal fun SubstitutionModePanel(
    match: GameMatch,
    livePlacements: List<MatchLineupPlacement>,
    playersById: Map<Long, Player>,
    eligibleBenchIds: Set<Long>,
    minutesLabel: (Long) -> String,
    onMessage: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: (List<MatchLineupPlacement>) -> Unit,
    modifier: Modifier = Modifier
) {
    val original = remember(match.id) { livePlacements }
    val originalById = remember(original) { original.associateBy { it.playerId } }
    var plan by remember(match.id) { mutableStateOf(SubstitutionPlanRules.planOf(original)) }
    var drag by remember { mutableStateOf<SubstitutionDrag?>(null) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var pitchBounds by remember { mutableStateOf<Rect?>(null) }
    var benchBounds by remember { mutableStateOf<Rect?>(null) }
    var panelOrigin by remember { mutableStateOf(Offset.Zero) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val markerHitRadiusPx = with(density) { 30.dp.toPx() }

    val slots = remember(match.formation, match.playersOnPitch) {
        FormationLayout.slots(match.formation, match.playersOnPitch)
    }
    val onPitch = plan.values.filter { it.onPitch }
    val bench = plan.values.filterNot { it.onPitch }
        .mapNotNull { playersById[it.playerId] }
        .sortedWith(compareBy<Player>({ it.shirtNumber <= 0 }, { it.shirtNumber }, { it.name }))
    val changes = SubstitutionPlanRules.changes(original, plan)

    fun canMove(playerId: Long): Boolean =
        playerId in eligibleBenchIds || originalById[playerId]?.onPitch == true

    fun playerUnder(pointer: Offset, excluding: Long): Long? {
        val pitch = pitchBounds ?: return null
        return onPitch
            .filterNot { it.playerId == excluding }
            .map { placement ->
                val center = Offset(
                    pitch.left + pitch.width * placement.normalizedX,
                    pitch.top + pitch.height * placement.normalizedY
                )
                placement.playerId to (center - pointer).getDistance()
            }
            .filter { it.second <= markerHitRadiusPx }
            .minByOrNull { it.second }
            ?.first
    }

    fun swap(first: Long, second: Long) {
        if (!canMove(first) || !canMove(second)) {
            onMessage("That player cannot return to the pitch")
            return
        }
        plan = SubstitutionPlanRules.swap(plan, first, second)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun finishDrag(playerId: Long, pointer: Offset) {
        drag = null
        selectedId = null
        val pitch = pitchBounds
        val benchArea = benchBounds
        when {
            pitch != null && pitch.contains(pointer) -> {
                val target = playerUnder(pointer, playerId)
                if (target != null) {
                    swap(playerId, target)
                    return
                }
                val (x, y) = LineupDragDropRules.normalize(
                    pointer.x, pointer.y, pitch.left, pitch.top, pitch.width, pitch.height
                )
                val occupied = onPitch
                    .filterNot { it.playerId == playerId }
                    .mapNotNullTo(mutableSetOf()) { it.formationSlot.takeIf(String::isNotBlank) }
                val drop = LineupDragDropRules.resolvePitchDrop(x, y, slots, occupied)
                val updated = SubstitutionPlanRules.moveToPitch(plan, playerId, drop, match.playersOnPitch)
                if (updated == null) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMessage("The pitch is full. Drop the substitute onto the player who comes off.")
                } else {
                    plan = updated
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            benchArea != null && benchArea.contains(pointer) -> {
                plan = SubstitutionPlanRules.moveToBench(plan, playerId)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            else -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun tapPlayer(playerId: Long) {
        val selected = selectedId
        when {
            selected == null -> {
                if (!canMove(playerId)) {
                    onMessage("${playersById[playerId]?.name ?: "This player"} cannot return to the pitch")
                } else {
                    selectedId = playerId
                }
            }
            selected == playerId -> selectedId = null
            else -> {
                swap(selected, playerId)
                selectedId = null
            }
        }
    }

    val hoveredPlayerId = drag?.let { playerUnder(it.pointerInRoot, it.playerId) }
    val dragPointer = drag?.pointerInRoot
    val pitchIsTarget = dragPointer?.let { pitchBounds?.contains(it) } == true
    val benchIsTarget = dragPointer?.let { benchBounds?.contains(it) } == true

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(
                    "Substitution mode • clock and minutes keep running",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    selectedId?.let { "Selected ${playersById[it]?.name ?: "player"} • tap another player to swap" }
                        ?: "Long-press and drag a substitute onto the player who comes off, or tap two players.",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { panelOrigin = it.boundsInRoot().topLeft }
        ) {
            Column(Modifier.fillMaxSize()) {
                PitchView(
                    slots = slots,
                    placements = onPitch,
                    playersById = playersById,
                    draggingPlayerId = drag?.playerId,
                    highlightedSlotId = null,
                    isDropTarget = pitchIsTarget,
                    onBoundsChanged = { pitchBounds = it },
                    onPlayerClick = ::tapPlayer,
                    onNudge = { playerId, dx, dy ->
                        plan[playerId]?.let { existing ->
                            plan = plan + (playerId to existing.copy(
                                normalizedX = (existing.normalizedX + dx).coerceIn(0.08f, 0.92f),
                                normalizedY = (existing.normalizedY + dy).coerceIn(0.08f, 0.92f),
                                formationSlot = ""
                            ))
                        }
                    },
                    onDragStart = { playerId, pointer ->
                        selectedId = null
                        drag = SubstitutionDrag(playerId, pointer)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragMove = { pointer -> drag = drag?.copy(pointerInRoot = pointer) },
                    onDragEnd = ::finishDrag,
                    onDragCancel = { drag = null },
                    subtitleFor = minutesLabel,
                    highlightedPlayerId = hoveredPlayerId ?: selectedId,
                    dropHint = hoveredPlayerId?.let { "Release to swap with ${playersById[it]?.name ?: "player"}" },
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp)
                )
                BenchPanel(
                    players = bench,
                    draggingPlayerId = drag?.playerId,
                    isDropTarget = benchIsTarget,
                    onBoundsChanged = { benchBounds = it },
                    onPlayerClick = ::tapPlayer,
                    onDragStart = { playerId, pointer ->
                        selectedId = null
                        drag = SubstitutionDrag(playerId, pointer)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragMove = { pointer -> drag = drag?.copy(pointerInRoot = pointer) },
                    onDragEnd = ::finishDrag,
                    onDragCancel = { drag = null },
                    subtitleFor = { id ->
                        if (canMove(id)) minutesLabel(id) else "Out"
                    },
                    canDrag = ::canMove
                )
            }

            drag?.let { current ->
                playersById[current.playerId]?.let { player ->
                    PlayerDragGhost(
                        player = player,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (current.pointerInRoot.x - panelOrigin.x - 29.dp.toPx()).roundToInt(),
                                    (current.pointerInRoot.y - panelOrigin.y - 29.dp.toPx()).roundToInt()
                                )
                            }
                            .zIndex(20f)
                    )
                }
            }
        }

        if (!changes.isEmpty) {
            Text(
                listOfNotNull(
                    changes.incoming.takeIf { it.isNotEmpty() }
                        ?.joinToString(prefix = "▲ ") { playersById[it]?.name ?: "Player" },
                    changes.outgoing.takeIf { it.isNotEmpty() }
                        ?.joinToString(prefix = "▼ ") { playersById[it]?.name ?: "Player" },
                    changes.moved.takeIf { it.isNotEmpty() }?.let { "${it.size} moved" }
                ).joinToString("   "),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onCancel,
                enabled = drag == null,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) { Text(if (changes.isEmpty) "Close" else "Discard") }
            Button(
                onClick = { if (changes.isEmpty) onCancel() else onConfirm(plan.values.toList()) },
                enabled = drag == null,
                modifier = Modifier.weight(1.4f).heightIn(min = 48.dp)
            ) {
                Text(
                    when {
                        changes.isEmpty -> "Done"
                        changes.hasSubstitutions ->
                            "Confirm ${maxOf(changes.incoming.size, changes.outgoing.size)} change(s)"
                        else -> "Confirm positions"
                    },
                    maxLines = 1
                )
            }
        }
    }
}
