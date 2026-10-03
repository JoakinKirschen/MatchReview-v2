package be.matchreview.app

import be.matchreview.app.ui.AppButton
import be.matchreview.app.ui.AppOutlinedButton
import android.os.StatFs
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import be.matchreview.app.domain.GoalkeeperRules

@Composable
fun MatchReadinessScreen(
    matchId: Long,
    vm: MainViewModel,
    nav: NavHostController
) {
    val match by vm.match(matchId).collectAsStateWithLifecycle(initialValue = null)
    val squad by vm.squad(matchId).collectAsStateWithLifecycle(initialValue = emptyList())
    val lineup by vm.lineup(matchId).collectAsStateWithLifecycle(initialValue = emptyList())
    val players by vm.players.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val current = match
    var showDetailsEditor by remember { mutableStateOf(false) }

    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val selectedIds = squad.filter { it.selected }.mapTo(mutableSetOf()) { it.playerId }
    val starters = lineup.filter { it.onPitch && it.playerId in selectedIds }
    val selectedPlayers = players.filter { it.id in selectedIds }
    // Formation slots label the keeper "GK", which a plain "goal" text check never matched.
    val goalkeeperReady = GoalkeeperRules.onPitchGoalkeepers(starters, squad, players).isNotEmpty()
    val freeBytes = remember {
        runCatching { StatFs(context.filesDir.absolutePath).availableBytes }.getOrDefault(0L)
    }
    val freeStorageLabel = when {
        freeBytes >= 1024L * 1024L * 1024L ->
            "${freeBytes / (1024L * 1024L * 1024L)} GB free"
        else -> "${freeBytes / (1024L * 1024L)} MB free"
    }
    val fullLineup = starters.size == current.playersOnPitch

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Match-day check", style = MaterialTheme.typography.headlineMedium)
        Text(
            "${selectedPlayers.size} selected • ${starters.size}/${current.playersOnPitch} starting",
            style = MaterialTheme.typography.titleMedium
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReadinessRow("Opponent", current.opponent)
                ReadinessRow("Format", "${current.playersOnPitch}v${current.playersOnPitch}")
                ReadinessRow("Formation", current.formation)
                ReadinessRow("Timing", "${current.periodCount} × ${current.periodDurationMinutes} min")
                ReadinessRow("Venue", current.venue.ifBlank { "Not specified" })
                ReadinessRow("Date", current.matchDate)
                ReadinessRow("Home or away", if (current.isHome) "Home" else "Away")
                ReadinessRow("Competition", current.competition.ifBlank { "Not specified" })
                TextButton(onClick = { showDetailsEditor = true }) { Text("Edit match details") }
                ReadinessRow("Storage", freeStorageLabel)
            }
        }

        if (!fullLineup) {
            AssistChip(
                onClick = { nav.navigate("lineup/$matchId") },
                label = { Text("Starting lineup is underfilled — review") }
            )
        }
        if (!goalkeeperReady) {
            AssistChip(
                onClick = { nav.navigate("lineup/$matchId") },
                label = { Text("No goalkeeper role detected — review") }
            )
        }

        Text(
            "Camera audio starts off. You can enable it from the Camera tab if the club has permission to record sound.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(8.dp))
        AppOutlinedButton(
            onClick = { nav.navigate("lineup/$matchId") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) { Text("Review lineup") }
        AppButton(
            onClick = {
                nav.navigate("live/$matchId") {
                    popUpTo("ready/$matchId") { inclusive = true }
                }
            },
            enabled = starters.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) { Text("Open match day") }
    }

    if (showDetailsEditor) {
        MatchDetailsDialog(
            match = current,
            onDismiss = { showDetailsEditor = false },
            onSave = { opponent, date, venue, competition, home ->
                vm.updateMatchDetails(matchId, opponent, date, venue, competition, home) {
                    showDetailsEditor = false
                }
            }
        )
    }
}

@Composable
private fun ReadinessRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
