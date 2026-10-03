package be.matchreview.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import be.matchreview.app.data.GameMatch
import be.matchreview.app.data.MatchLineupPlacement
import be.matchreview.app.data.Player
import be.matchreview.app.domain.FormationLayout
import be.matchreview.app.domain.FormationSlot
import be.matchreview.app.domain.LineupDragDropRules
import be.matchreview.app.domain.SubstitutionDrop
import be.matchreview.app.domain.SubstitutionPlanRules
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private data class LineupDragState(
    val playerId: Long,
    val fromPitch: Boolean,
    val pointerInRoot: Offset
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineupBuilderScreen(
    matchId: Long,
    vm: MainViewModel,
    nav: NavHostController
) {
    val match by vm.match(matchId).collectAsStateWithLifecycle(initialValue = null)
    val allPlayers by vm.players.collectAsStateWithLifecycle()
    val squad by vm.squad(matchId).collectAsStateWithLifecycle(initialValue = emptyList())
    val placements by vm.lineup(matchId).collectAsStateWithLifecycle(initialValue = emptyList())
    val current = match

    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LaunchedEffect(matchId) { vm.ensureLineup(matchId) }

    val selectedIds = remember(squad) {
        squad.filter { it.selected }.mapTo(mutableSetOf()) { it.playerId }
    }
    val selectedPlayers = remember(allPlayers, selectedIds) {
        allPlayers.filter { it.id in selectedIds }
            .sortedWith(compareBy<Player>({ it.shirtNumber <= 0 }, { it.shirtNumber }, { it.name }))
    }
    val playersById = remember(selectedPlayers) { selectedPlayers.associateBy { it.id } }
    val activePlacements = remember(placements, selectedIds) {
        placements.filter { it.playerId in selectedIds }
    }
    val placementByPlayer = remember(activePlacements) { activePlacements.associateBy { it.playerId } }
    val onPitch = activePlacements.filter { it.onPitch }
    val bench = selectedPlayers.filter { placementByPlayer[it.id]?.onPitch != true }
    val slots = remember(current.formation, current.playersOnPitch) {
        FormationLayout.slots(current.formation, current.playersOnPitch)
    }

    var selectedPlayerId by remember { mutableStateOf<Long?>(null) }
    var showUnderfilledConfirmation by remember { mutableStateOf(false) }
    var showAutoPlaceConfirmation by remember { mutableStateOf(false) }
    var dragState by remember { mutableStateOf<LineupDragState?>(null) }
    var pitchBounds by remember { mutableStateOf<Rect?>(null) }
    var benchBounds by remember { mutableStateOf<Rect?>(null) }
    var editorOrigin by remember { mutableStateOf(Offset.Zero) }
    // A drop can be followed by a click on the same item; it must not open the player sheet.
    var lastDropAt by remember { mutableLongStateOf(0L) }
    fun openPlayer(playerId: Long) {
        if (android.os.SystemClock.uptimeMillis() - lastDropAt > 400L) selectedPlayerId = playerId
    }

    val haptics = LocalHapticFeedback.current
    val playerHitRadiusPx = with(LocalDensity.current) { 40.dp.toPx() }

    /**
     * Where [playerId] lands if released at [pointer], using the same rules as substitution
     * mode: on another player swaps them, a full pitch swaps with the nearest player, and
     * otherwise the player snaps to a free slot nearby. Null when not over the pitch.
     */
    fun dropAt(playerId: Long, pointer: Offset): SubstitutionDrop? {
        val pitch = pitchBounds ?: return null
        if (!pitch.contains(pointer)) return null
        val (x, y) = LineupDragDropRules.normalize(
            pointer.x, pointer.y, pitch.left, pitch.top, pitch.width, pitch.height
        )
        val existing = placementByPlayer[playerId]
            ?: MatchLineupPlacement(matchId = matchId, playerId = playerId)
        return SubstitutionPlanRules.resolveDrop(
            placementByPlayer + (playerId to existing), playerId, x, y, slots,
            current.playersOnPitch, pitch.width, pitch.height, playerHitRadiusPx
        )
    }

    // Moves are confirmed by the lineup itself and a haptic tick. No snackbars: they cover
    // the bench and block the next long-press for several seconds.
    fun finishDrag(playerId: Long, pointer: Offset) {
        lastDropAt = android.os.SystemClock.uptimeMillis()
        dragState = null
        val existing = placementByPlayer[playerId]
            ?: MatchLineupPlacement(matchId = matchId, playerId = playerId)
        val benchArea = benchBounds
        when (val drop = dropAt(playerId, pointer)) {
            is SubstitutionDrop.Swap -> placementByPlayer[drop.targetPlayerId]?.let { target ->
                vm.swapLineupPlacements(existing, target)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            is SubstitutionDrop.Place -> {
                vm.setLineupPlacement(
                    existing.copy(
                        normalizedX = drop.drop.normalizedX,
                        normalizedY = drop.drop.normalizedY,
                        formationSlot = drop.drop.formationSlot,
                        role = drop.drop.role.ifBlank { existing.role },
                        onPitch = true
                    )
                )
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            null -> if (benchArea != null && benchArea.contains(pointer) && existing.onPitch) {
                vm.setLineupPlacement(existing.copy(onPitch = false, formationSlot = "", role = ""))
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } else {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    val dragPointer = dragState?.pointerInRoot
    val pitchIsDropTarget = dragPointer?.let { pitchBounds?.contains(it) } == true
    val benchIsDropTarget = dragPointer?.let { benchBounds?.contains(it) } == true
    val dropPreview = dragState?.let { dropAt(it.playerId, it.pointerInRoot) }
    val hoveredSlotId = (dropPreview as? SubstitutionDrop.Place)?.drop?.takeIf { it.snapped }?.formationSlot
    val swapTargetId = (dropPreview as? SubstitutionDrop.Swap)?.targetPlayerId

    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LineupHeader(current, onPitch.size, selectedPlayers.size)

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { editorOrigin = it.boundsInRoot().topLeft }
            ) {
                Column(Modifier.fillMaxSize()) {
                    PitchView(
                        slots = slots,
                        placements = onPitch,
                        playersById = playersById,
                        draggingPlayerId = dragState?.playerId,
                        highlightedSlotId = hoveredSlotId,
                        isDropTarget = pitchIsDropTarget,
                        onBoundsChanged = { pitchBounds = it },
                        onPlayerClick = ::openPlayer,
                        onNudge = { playerId, dx, dy ->
                            placementByPlayer[playerId]?.let { existing ->
                                vm.setLineupPlacement(
                                    existing.copy(
                                        normalizedX = (existing.normalizedX + dx).coerceIn(0.08f, 0.92f),
                                        normalizedY = (existing.normalizedY + dy).coerceIn(0.08f, 0.92f),
                                        formationSlot = ""
                                    )
                                )
                            }
                        },
                        onDragStart = { playerId, pointer ->
                            dragState = LineupDragState(playerId, true, pointer)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragMove = { pointer ->
                            dragState = dragState?.copy(pointerInRoot = pointer)
                        },
                        onDragEnd = ::finishDrag,
                        onDragCancel = { dragState = null },
                        highlightedPlayerId = swapTargetId,
                        dropHint = swapTargetId?.let { "Swap with ${playersById[it]?.name ?: "player"}" },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    BenchPanel(
                        players = bench,
                        draggingPlayerId = dragState?.playerId,
                        isDropTarget = benchIsDropTarget,
                        onBoundsChanged = { benchBounds = it },
                        onPlayerClick = ::openPlayer,
                        onDragStart = { playerId, pointer ->
                            dragState = LineupDragState(playerId, false, pointer)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragMove = { pointer ->
                            dragState = dragState?.copy(pointerInRoot = pointer)
                        },
                        onDragEnd = ::finishDrag,
                        onDragCancel = { dragState = null }
                    )
                }

                dragState?.let { drag ->
                    playersById[drag.playerId]?.let { player ->
                        PlayerDragGhost(
                            player = player,
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (drag.pointerInRoot.x - editorOrigin.x - 29.dp.toPx()).roundToInt(),
                                        (drag.pointerInRoot.y - editorOrigin.y - 29.dp.toPx()).roundToInt()
                                    )
                                }
                                .zIndex(20f)
                        )
                    }
                }
            }

            Surface(shadowElevation = 8.dp) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showAutoPlaceConfirmation = true },
                            enabled = selectedPlayers.isNotEmpty() && dragState == null,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.weight(0.9f).heightIn(min = 48.dp)
) { Text("Auto-place", maxLines = 1) }
                        OutlinedButton(
                            onClick = { nav.navigate("squad/$matchId") },
                            enabled = dragState == null,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.weight(0.9f).heightIn(min = 48.dp)
) { Text("Squad", maxLines = 1) }
                        Button(
                            enabled = onPitch.isNotEmpty() && dragState == null,
                            onClick = {
                                if (onPitch.size < current.playersOnPitch) {
                                    showUnderfilledConfirmation = true
                                } else {
                                    vm.markLineupReady(matchId) {
                                        nav.navigate("ready/$matchId") {
                                            popUpTo("lineup/$matchId") { inclusive = true }
                                        }
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1.35f).heightIn(min = 48.dp)
                        ) { Text("Match day", maxLines = 1) }
                    }
                    Text(
                        if (onPitch.size == current.playersOnPitch) {
                            "Lineup complete • tap a player to move or swap; long-press to drag"
                        } else {
                            "${onPitch.size}/${current.playersOnPitch} on pitch • tap a bench player and choose a slot"
                        },
                        color = if (onPitch.size == current.playersOnPitch) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }

    val selectedPlayer = playersById[selectedPlayerId]
    if (selectedPlayer != null && dragState == null) {
        val existing = placementByPlayer[selectedPlayer.id]
            ?: MatchLineupPlacement(matchId = matchId, playerId = selectedPlayer.id)
        PlayerPlacementSheet(
            player = selectedPlayer,
            placement = existing,
            pitchFull = onPitch.size >= current.playersOnPitch && !existing.onPitch,
            slots = slots,
            occupiedPlacements = onPitch.filterNot { it.playerId == selectedPlayer.id },
            playersById = playersById,
            onDismiss = { selectedPlayerId = null },
            onMoveToPitch = { slot ->
                if (onPitch.size < current.playersOnPitch || existing.onPitch) {
                    vm.setLineupPlacement(
                        existing.copy(
                            normalizedX = slot.normalizedX,
                            normalizedY = slot.normalizedY,
                            role = slot.label,
                            formationSlot = slot.id,
                            onPitch = true
                        )
                    )
                    selectedPlayerId = null
                }
            },
            onMoveToBench = {
                vm.setLineupPlacement(existing.copy(onPitch = false, formationSlot = "", role = ""))
                selectedPlayerId = null
            },
            onSwapWith = { occupied ->
                vm.swapLineupPlacements(existing, occupied)
                selectedPlayerId = null
            },
            onNudge = { dx, dy ->
                vm.setLineupPlacement(
                    existing.copy(
                        normalizedX = (existing.normalizedX + dx).coerceIn(0.08f, 0.92f),
                        normalizedY = (existing.normalizedY + dy).coerceIn(0.08f, 0.92f),
                        formationSlot = "",
                        onPitch = true
                    )
                )
            }
        )
    }

    if (showAutoPlaceConfirmation) {
        AlertDialog(
            onDismissRequest = { showAutoPlaceConfirmation = false },
            title = { Text("Auto-place lineup?") },
            text = {
                Text(
                    if (onPitch.isEmpty()) {
                        "Players will be placed into the available ${current.formation} formation slots."
                    } else {
                        "This will replace the current manual placement with an automatic ${current.formation} lineup."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAutoPlaceConfirmation = false
                        vm.autoPlaceLineup(matchId, selectedPlayers, slots)
                    }
                ) { Text("Auto-place") }
            },
            dismissButton = {
                TextButton(onClick = { showAutoPlaceConfirmation = false }) { Text("Cancel") }
            }
        )
    }

    if (showUnderfilledConfirmation) {
        AlertDialog(
            onDismissRequest = { showUnderfilledConfirmation = false },
            title = { Text("Save a smaller starting lineup?") },
            text = {
                Text(
                    "The match is configured for ${current.playersOnPitch} players, but ${onPitch.size} are currently on the pitch."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showUnderfilledConfirmation = false
                    vm.markLineupReady(matchId) {
                        nav.navigate("ready/$matchId") {
                            popUpTo("lineup/$matchId") { inclusive = true }
                        }
                    }
                }) { Text("Save anyway") }
            },
            dismissButton = {
                TextButton(onClick = { showUnderfilledConfirmation = false }) {
                    Text("Keep editing")
                }
            }
        )
    }
}

@Composable
private fun LineupHeader(match: GameMatch, onPitch: Int, selected: Int) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Starting lineup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("vs ${match.opponent} • ${match.formation}", style = MaterialTheme.typography.bodySmall)
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            ) {
                Text(
                    "$onPitch / ${match.playersOnPitch} on pitch\n$selected selected",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
internal fun PitchView(
    slots: List<FormationSlot>,
    placements: List<MatchLineupPlacement>,
    playersById: Map<Long, Player>,
    draggingPlayerId: Long?,
    highlightedSlotId: String?,
    isDropTarget: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    onPlayerClick: (Long) -> Unit,
    onNudge: (Long, Float, Float) -> Unit,
    onDragStart: (Long, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: (Long, Offset) -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
    subtitleFor: (Long) -> String? = { null },
    highlightedPlayerId: Long? = null,
    dropHint: String? = null
) {
    BoxWithConstraints(
        modifier = modifier
            .testTag(LineupTestTags.PITCH)
            .onGloballyPositioned { onBoundsChanged(it.boundsInRoot()) }
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isDropTarget) {
                    Brush.verticalGradient(listOf(Color(0xFF8BCB56), Color(0xFF69B442), Color(0xFF55A23A)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFF79B94A), Color(0xFF5FA63B), Color(0xFF4A9134)))
                }
            )
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val markerPx = with(density) { 54.dp.toPx() }
        val slotRadiusPx = with(density) { 13.dp.toPx() }

        PitchLines(Modifier.fillMaxSize())

        slots.forEach { slot ->
            Box(
                Modifier
                    .offset {
                        IntOffset(
                            (slot.normalizedX * widthPx - slotRadiusPx).roundToInt(),
                            (slot.normalizedY * heightPx - slotRadiusPx).roundToInt()
                        )
                    }
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        if (slot.id == highlightedSlotId) Color(0xFFFFD54F).copy(alpha = 0.9f)
                        else Color.White.copy(alpha = 0.18f)
                    )
            )
        }

        placements.forEach { placement ->
            val player = playersById[placement.playerId] ?: return@forEach
            // Keyed by player, not list position: when another player leaves the pitch the
            // list shifts, and a positional slot would hand this marker (and its running drag
            // gesture) to a different player, cancelling the drag mid-way.
            key(player.id) {
                PlayerMarker(
                    player = player,
                    role = placement.role,
                    dragging = draggingPlayerId == player.id,
                    subtitle = subtitleFor(player.id),
                    highlighted = highlightedPlayerId == player.id,
                    onClick = { onPlayerClick(player.id) },
                    onNudge = { dx, dy -> onNudge(player.id, dx, dy) },
                    onDragStart = { pointer -> onDragStart(player.id, pointer) },
                    onDragMove = onDragMove,
                    onDragEnd = { pointer -> onDragEnd(player.id, pointer) },
                    onDragCancel = onDragCancel,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (placement.normalizedX * widthPx - markerPx / 2).roundToInt(),
                                (placement.normalizedY * heightPx - markerPx / 2).roundToInt()
                            )
                        }
                        .testTag(LineupTestTags.pitchPlayer(player.id))
                )
            }
        }

        if (isDropTarget) {
            Text(
                dropHint ?: if (highlightedSlotId != null) "Release to snap into position" else "Release to place on pitch",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
internal fun PitchLines(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val line = Color.White.copy(alpha = 0.72f)
        val stroke = 3f
        drawRect(line, style = Stroke(stroke))
        drawLine(line, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), stroke, StrokeCap.Round)
        drawCircle(line, radius = size.width * 0.13f, center = center, style = Stroke(stroke))
        drawCircle(line, radius = 5f, center = center)

        val boxWidth = size.width * 0.58f
        val boxHeight = size.height * 0.16f
        drawRect(
            line,
            topLeft = Offset((size.width - boxWidth) / 2, 0f),
            size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
            style = Stroke(stroke)
        )
        drawRect(
            line,
            topLeft = Offset((size.width - boxWidth) / 2, size.height - boxHeight),
            size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
            style = Stroke(stroke)
        )
        val goalWidth = size.width * 0.28f
        val goalDepth = size.height * 0.025f
        drawRect(
            line,
            topLeft = Offset((size.width - goalWidth) / 2, 0f),
            size = androidx.compose.ui.geometry.Size(goalWidth, goalDepth),
            style = Stroke(stroke)
        )
        drawRect(
            line,
            topLeft = Offset((size.width - goalWidth) / 2, size.height - goalDepth),
            size = androidx.compose.ui.geometry.Size(goalWidth, goalDepth),
            style = Stroke(stroke)
        )
    }
}

@Composable
private fun PlayerMarker(
    player: Player,
    role: String,
    dragging: Boolean,
    subtitle: String?,
    highlighted: Boolean,
    onClick: () -> Unit,
    onNudge: (Float, Float) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(54.dp)
            .alpha(if (dragging) 0.18f else 1f)
            .longPressPlayerDrag(
                playerId = player.id,
                onStart = onDragStart,
                onMove = onDragMove,
                onEnd = onDragEnd,
                onCancel = onDragCancel
            )
            .semantics {
                contentDescription =
                    "${player.name}, number ${player.shirtNumber}, ${role.ifBlank { "on pitch" }}. Drag to move."
                customActions = listOf(
                    CustomAccessibilityAction("Move left") { onNudge(-0.05f, 0f); true },
                    CustomAccessibilityAction("Move right") { onNudge(0.05f, 0f); true },
                    CustomAccessibilityAction("Move forward") { onNudge(0f, -0.05f); true },
                    CustomAccessibilityAction("Move back") { onNudge(0f, 0.05f); true }
                )
            }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayerBadge(
            player,
            if (highlighted) Modifier.border(3.dp, Color(0xFFFFD54F), CircleShape) else Modifier
        )
        Text(
            player.name.substringBefore(" ").uppercase(),
            color = Color.Black,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        subtitle?.let {
            Text(
                it,
                color = Color(0xFF16330E),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PlayerBadge(player: Player, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(46.dp)
            .shadow(5.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xFFF5F7FA)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (player.shirtNumber > 0) player.shirtNumber.toString()
            else player.name.take(1).uppercase(),
            color = Color(0xFF1C3D6E),
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
internal fun PlayerDragGhost(player: Player, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(58.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayerBadge(player, Modifier.size(52.dp))
        Text(
            player.name.substringBefore(" ").uppercase(),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.58f), RoundedCornerShape(7.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
internal fun BenchPanel(
    players: List<Player>,
    draggingPlayerId: Long?,
    isDropTarget: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    onPlayerClick: (Long) -> Unit,
    onDragStart: (Long, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: (Long, Offset) -> Unit,
    onDragCancel: () -> Unit,
    subtitleFor: (Long) -> String? = { null },
    canDrag: (Long) -> Boolean = { true },
    /** Marks players who should get time on the pitch next. */
    highlightFor: (Long) -> Boolean = { false }
) {
    Surface(
        color = if (isDropTarget) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .testTag(LineupTestTags.BENCH)
            .onGloballyPositioned { onBoundsChanged(it.boundsInRoot()) }
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Text(
                if (isDropTarget) "RELEASE TO MOVE TO BENCH" else "BENCH • ${players.size}",
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            if (players.isEmpty()) {
                Text(
                    if (draggingPlayerId != null) "Drop the player here." else "All selected players are on the pitch.",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(players, key = { it.id }) { player ->
                        Column(
                            Modifier
                                .testTag(LineupTestTags.benchPlayer(player.id))
                                .width(60.dp)
                                .alpha(
                                    when {
                                        draggingPlayerId == player.id -> 0.18f
                                        !canDrag(player.id) -> 0.45f
                                        else -> 1f
                                    }
                                )
                                .then(
                                    if (canDrag(player.id)) {
                                        Modifier.longPressPlayerDrag(
                                            playerId = player.id,
                                            onStart = { onDragStart(player.id, it) },
                                            onMove = onDragMove,
                                            onEnd = { onDragEnd(player.id, it) },
                                            onCancel = onDragCancel
                                        )
                                    } else Modifier
                                )
                                .semantics {
                                    contentDescription =
                                        "${player.name}, substitute. Tap for placement options or drag onto the pitch."
                                    customActions = listOf(
                                        CustomAccessibilityAction("Open placement options") {
                                            onPlayerClick(player.id)
                                            true
                                        }
                                    )
                                }
                                .clickable { onPlayerClick(player.id) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape)
                                    .background(
                                        if (highlightFor(player.id)) Color(0xFFFFC247)
                                        else MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (player.shirtNumber > 0) player.shirtNumber.toString()
                                    else player.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                player.name.substringBefore(" "),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            subtitleFor(player.id)?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (highlightFor(player.id)) FontWeight.Bold else null,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Long-press, then drag a player. Reports the pointer in root coordinates.
 *
 * The gesture detector is keyed only on the player: callbacks are read through
 * [rememberUpdatedState], so recompositions caused by the drag itself (the lifted
 * ghost, the faded source marker, drop-target highlights) never restart the
 * detector and cancel the gesture half-way.
 */
private fun Modifier.longPressPlayerDrag(
    playerId: Long,
    onStart: (Offset) -> Unit,
    onMove: (Offset) -> Unit,
    onEnd: (Offset) -> Unit,
    onCancel: () -> Unit
): Modifier = composed {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnEnd by rememberUpdatedState(onEnd)
    val currentOnCancel by rememberUpdatedState(onCancel)

    fun toRoot(local: Offset): Offset =
        coordinates?.takeIf { it.isAttached }?.localToRoot(local) ?: local

    this
        .onGloballyPositioned { coordinates = it }
        .pointerInput(playerId) {
            var pointerInRoot = Offset.Zero
            var dragging = false
            try {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localPointer ->
                        dragging = true
                        pointerInRoot = toRoot(localPointer)
                        currentOnStart(pointerInRoot)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        // Use the pointer's absolute position rather than summed deltas so
                        // the drop point stays exact even if the source item moves.
                        pointerInRoot = toRoot(change.position)
                        currentOnMove(pointerInRoot)
                    },
                    onDragEnd = {
                        dragging = false
                        currentOnEnd(pointerInRoot)
                    },
                    onDragCancel = {
                        dragging = false
                        currentOnCancel()
                    }
                )
            } catch (cancelled: CancellationException) {
                // The item left composition mid-drag; never leave the screen stuck in drag mode.
                if (dragging) currentOnCancel()
                throw cancelled
            }
        }
}

internal object LineupTestTags {
    const val PITCH = "lineup-pitch"
    const val BENCH = "lineup-bench"
    fun pitchPlayer(playerId: Long) = "lineup-pitch-player-$playerId"
    fun benchPlayer(playerId: Long) = "lineup-bench-player-$playerId"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerPlacementSheet(
    player: Player,
    placement: MatchLineupPlacement,
    pitchFull: Boolean,
    slots: List<FormationSlot>,
    occupiedPlacements: List<MatchLineupPlacement>,
    playersById: Map<Long, Player>,
    onDismiss: () -> Unit,
    onMoveToPitch: (FormationSlot) -> Unit,
    onMoveToBench: () -> Unit,
    onSwapWith: (MatchLineupPlacement) -> Unit,
    onNudge: (Float, Float) -> Unit
) {
    val occupiedSlotIds = LineupDragDropRules.occupiedSlotIds(slots, occupiedPlacements)
    val nearestAvailable = slots.firstOrNull { it.id !in occupiedSlotIds } ?: slots.first()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(player.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                listOfNotNull(
                    player.shirtNumber.takeIf { it > 0 }?.let { "#$it" },
                    player.position.takeIf { it.isNotBlank() },
                    placement.role.takeIf { placement.onPitch && it.isNotBlank() }
                ).joinToString(" • ")
            )

            if (!placement.onPitch) {
                Button(
                    onClick = { onMoveToPitch(nearestAvailable) },
                    enabled = !pitchFull,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text(if (pitchFull) "Pitch is full" else "Place in next free formation slot") }
            } else {
                Text("Fine position", style = MaterialTheme.typography.titleMedium)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(onClick = { onNudge(0f, -0.05f) }) { Text("↑ Forward") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { onNudge(-0.05f, 0f) }) { Text("← Left") }
                        FilledTonalButton(onClick = { onNudge(0.05f, 0f) }) { Text("Right →") }
                    }
                    FilledTonalButton(onClick = { onNudge(0f, 0.05f) }) { Text("↓ Back") }
                }
                OutlinedButton(onClick = onMoveToBench, modifier = Modifier.fillMaxWidth()) {
                    Text("Move to bench")
                }
            }

            if (occupiedPlacements.isNotEmpty()) {
                Text("Swap with a player", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(occupiedPlacements, key = { it.playerId }) { occupied ->
                        val other = playersById[occupied.playerId]
                        AssistChip(
                            onClick = { onSwapWith(occupied) },
                            label = {
                                Text(
                                    listOfNotNull(
                                        other?.shirtNumber?.takeIf { it > 0 }?.let { "#$it" },
                                        other?.name
                                    ).joinToString(" ").ifBlank { "Player" }
                                )
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }

            Text("Choose a formation slot", style = MaterialTheme.typography.titleMedium)
            Text(
                "Tap a free slot below. Long-press dragging remains available on the lineup screen.",
                style = MaterialTheme.typography.bodySmall
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(slots, key = { it.id }) { slot ->
                    val available = slot.id !in occupiedSlotIds
                    FilterChip(
                        selected = placement.onPitch && placement.formationSlot == slot.id,
                        onClick = { if (available || placement.formationSlot == slot.id) onMoveToPitch(slot) },
                        enabled = (available || placement.formationSlot == slot.id) &&
                            (!pitchFull || placement.onPitch),
                        label = { Text("${slot.label} ${slot.id.substringAfterLast('-')}") },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
        }
    }
}
