package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BatterInnings
import com.example.data.BowlerSpell
import com.example.data.PlayerEntity
import com.example.data.TeamEntity

// This file handles Tab 2 ("Live Match").
// It has two modes:
// 1. MatchSetupSection: Where you pick the two teams, overs, and starting players before the game begins.
// 2. LiveScoringSection: Where you tap buttons for every ball bowled (0, 1, 2, 3, 4, 6, Wide, Wicket, etc.).
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatchScorerScreen(
    teams: List<TeamEntity>,
    playersByTeam: Map<Long, List<PlayerEntity>>,
    activeMatch: ActiveMatchDetail?,
    onStartNewMatch: (
        teamAId: Long,
        teamBId: Long,
        matchName: String,
        venue: String,
        date: String,
        totalOvers: Int,
        battingFirstTeamId: Long,
        openingStrikerId: Long,
        openingNonStrikerId: Long,
        openingBowlerId: Long,
        onCreated: () -> Unit
    ) -> Unit,
    onStartSecondInnings: (strikerId: Long, nonStrikerId: Long, bowlerId: Long) -> Unit,
    onRecordDelivery: (
        runsOffBat: Int,
        extraType: String,
        extraRuns: Int,
        isWicket: Boolean,
        dismissalType: String,
        dismissedPlayerId: Long,
        nextBatterId: Long
    ) -> Unit,
    onSelectNextOverBowler: (Long) -> Unit,
    onUndoLastDelivery: () -> Unit,
    onViewFullScorecard: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // True if we should show the "Create Match" form; False if we are scoring a live match
    var showSetupForm by remember { mutableStateOf(activeMatch == null) }

    LaunchedEffect(activeMatch?.match?.id) {
        if (activeMatch == null) {
            showSetupForm = true
        }
    }

    if (showSetupForm || activeMatch == null) {
        MatchSetupSection(
            teams = teams,
            playersByTeam = playersByTeam,
            hasExistingMatch = activeMatch != null,
            onCancelSetup = { showSetupForm = false },
            onStartMatch = { aId, bId, name, venue, date, overs, batFirstId, sId, nsId, bwId ->
                onStartNewMatch(aId, bId, name, venue, date, overs, batFirstId, sId, nsId, bwId) {
                    showSetupForm = false
                }
            },
            modifier = modifier
        )
    } else {
        LiveScoringSection(
            activeMatch = activeMatch,
            onNewMatchClick = { showSetupForm = true },
            onStartSecondInnings = onStartSecondInnings,
            onRecordDelivery = onRecordDelivery,
            onSelectNextOverBowler = onSelectNextOverBowler,
            onUndoLastDelivery = onUndoLastDelivery,
            onViewFullScorecard = { onViewFullScorecard(activeMatch.match.id) },
            modifier = modifier
        )
    }
}

// Form to set up a new match: pick Team A, Team B, match details, overs, and opening players.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchSetupSection(
    teams: List<TeamEntity>,
    playersByTeam: Map<Long, List<PlayerEntity>>,
    hasExistingMatch: Boolean,
    onCancelSetup: () -> Unit,
    onStartMatch: (
        teamAId: Long,
        teamBId: Long,
        matchName: String,
        venue: String,
        date: String,
        totalOvers: Int,
        battingFirstTeamId: Long,
        openingStrikerId: Long,
        openingNonStrikerId: Long,
        openingBowlerId: Long
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    // Default to the first two saved teams
    var teamAId by remember(teams) { mutableLongStateOf(teams.getOrNull(0)?.id ?: 0L) }
    var teamBId by remember(teams) { mutableLongStateOf(teams.getOrNull(1)?.id ?: 0L) }
    var matchName by remember { mutableStateOf("EF101 Championship Match") }
    var venue by remember { mutableStateOf("Main University Oval") }
    var date by remember { mutableStateOf("2026-11-09") }
    var oversInput by remember { mutableStateOf("2") }
    var battingFirstId by remember(teamAId) { mutableLongStateOf(teamAId) }

    // Make sure Team A and Team B are two different teams
    val selectedTeamsValid = teamAId != 0L && teamBId != 0L && teamAId != teamBId
    val battingTeamId = if (battingFirstId == teamBId) teamBId else teamAId
    val bowlingTeamId = if (battingTeamId == teamAId) teamBId else teamAId

    val battingPlayers = playersByTeam[battingTeamId].orEmpty()
    val bowlingPlayers = playersByTeam[bowlingTeamId].orEmpty()

    // Remember which opening Striker, Non-Striker, and Bowler the user picked
    var strikerId by remember(battingTeamId, battingPlayers) {
        mutableLongStateOf(battingPlayers.getOrNull(0)?.id ?: 0L)
    }
    var nonStrikerId by remember(battingTeamId, battingPlayers) {
        mutableLongStateOf(battingPlayers.getOrNull(1)?.id ?: 0L)
    }
    var bowlerId by remember(bowlingTeamId, bowlingPlayers) {
        mutableLongStateOf(bowlingPlayers.getOrNull(0)?.id ?: 0L)
    }

    // Check if both chosen teams have at least 2 players
    val teamAPlayersCount = playersByTeam[teamAId]?.size ?: 0
    val teamBPlayersCount = playersByTeam[teamBId]?.size ?: 0
    val bothTeamsMeetMinSize =
        teamAPlayersCount >= MIN_PLAYERS_PER_TEAM && teamBPlayersCount >= MIN_PLAYERS_PER_TEAM

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Create & Configure Match",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (hasExistingMatch) {
                    TextButton(onClick = onCancelSetup) {
                        Text("Back to Live Match")
                    }
                }
            }
        }

        // Step 1 Card: Pick Team A and Team B
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Select Two Teams (Min $MIN_PLAYERS_PER_TEAM players each)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Team A:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        teams.forEach { team ->
                            val count = playersByTeam[team.id]?.size ?: 0
                            FilterChip(
                                selected = (teamAId == team.id),
                                onClick = {
                                    teamAId = team.id
                                    if (battingFirstId != teamAId && battingFirstId != teamBId) {
                                        battingFirstId = team.id
                                    }
                                },
                                label = { Text("${team.name} ($count)") },
                                modifier = Modifier.testTag("select_team_a_${team.id}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Team B:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        teams.forEach { team ->
                            val count = playersByTeam[team.id]?.size ?: 0
                            FilterChip(
                                selected = (teamBId == team.id),
                                onClick = { teamBId = team.id },
                                label = { Text("${team.name} ($count)") },
                                modifier = Modifier.testTag("select_team_b_${team.id}")
                            )
                        }
                    }

                    // Show an error message if the same team is picked twice or if a team has fewer than 2 players
                    if (!selectedTeamsValid) {
                        Text(
                            text = "Please select two different teams.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (!bothTeamsMeetMinSize) {
                        Text(
                            text = "Cannot start match: Both teams must have at least $MIN_PLAYERS_PER_TEAM players. Go to the Teams tab to add players.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Step 2 Card: Enter Match Title, Venue, Date, Overs, and who bats first
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "2. Match Details & Overs",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = matchName,
                        onValueChange = { matchName = it },
                        label = { Text("Match Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = venue,
                            onValueChange = { venue = it },
                            label = { Text("Venue") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Date") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = oversInput,
                            onValueChange = { oversInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Overs per Innings") },
                            singleLine = true,
                            modifier = Modifier
                                .width(150.dp)
                                .testTag("overs_input")
                        )
                        // Quick buttons to pick 1, 2, 5, or 10 overs
                        listOf(1, 2, 5, 10).forEach { preset ->
                            FilterChip(
                                selected = oversInput == preset.toString(),
                                onClick = { oversInput = preset.toString() },
                                label = { Text("${preset} ov") }
                            )
                        }
                    }

                    if (selectedTeamsValid) {
                        val teamAName = teams.find { it.id == teamAId }?.name ?: "Team A"
                        val teamBName = teams.find { it.id == teamBId }?.name ?: "Team B"
                        Text("Batting First Team:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = (battingTeamId == teamAId),
                                onClick = { battingFirstId = teamAId },
                                label = { Text(teamAName) }
                            )
                            FilterChip(
                                selected = (battingTeamId == teamBId),
                                onClick = { battingFirstId = teamBId },
                                label = { Text(teamBName) }
                            )
                        }
                    }
                }
            }
        }

        // Step 3 Card: Pick the starting Striker, Non-Striker, and Bowler
        if (selectedTeamsValid && bothTeamsMeetMinSize) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "3. Select Opening Players (1st Innings)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Opening Striker:", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            battingPlayers.forEach { p ->
                                FilterChip(
                                    selected = (strikerId == p.id),
                                    onClick = {
                                        strikerId = p.id
                                        // Make sure Striker and Non-Striker aren't the exact same person
                                        if (nonStrikerId == p.id) {
                                            nonStrikerId =
                                                battingPlayers.firstOrNull { it.id != p.id }?.id ?: 0L
                                        }
                                    },
                                    label = { Text(p.name) }
                                )
                            }
                        }

                        Text("Opening Non-Striker:", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            battingPlayers.forEach { p ->
                                FilterChip(
                                    selected = (nonStrikerId == p.id),
                                    onClick = {
                                        nonStrikerId = p.id
                                        if (strikerId == p.id) {
                                            strikerId =
                                                battingPlayers.firstOrNull { it.id != p.id }?.id ?: 0L
                                        }
                                    },
                                    label = { Text(p.name) }
                                )
                            }
                        }

                        Text("Opening Bowler:", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            bowlingPlayers.forEach { p ->
                                FilterChip(
                                    selected = (bowlerId == p.id),
                                    onClick = { bowlerId = p.id },
                                    label = { Text(p.name) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val parsedOvers = oversInput.toIntOrNull()?.coerceAtLeast(1) ?: 2
                                onStartMatch(
                                    teamAId,
                                    teamBId,
                                    matchName,
                                    venue,
                                    date,
                                    parsedOvers,
                                    battingTeamId,
                                    strikerId,
                                    nonStrikerId,
                                    bowlerId
                                )
                            },
                            enabled = strikerId != 0L && nonStrikerId != 0L &&
                                strikerId != nonStrikerId && bowlerId != 0L,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_match_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start Match")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Match & Begin 1st Innings", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// Live scoring view where you enter each ball and see the scoreboard update in real time.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveScoringSection(
    activeMatch: ActiveMatchDetail,
    onNewMatchClick: () -> Unit,
    onStartSecondInnings: (strikerId: Long, nonStrikerId: Long, bowlerId: Long) -> Unit,
    onRecordDelivery: (
        runsOffBat: Int,
        extraType: String,
        extraRuns: Int,
        isWicket: Boolean,
        dismissalType: String,
        dismissedPlayerId: Long,
        nextBatterId: Long
    ) -> Unit,
    onSelectNextOverBowler: (Long) -> Unit,
    onUndoLastDelivery: () -> Unit,
    onViewFullScorecard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val match = activeMatch.match
    val currentInnings = if (match.currentInnings == 1) activeMatch.innings1 else activeMatch.innings2
    val battingPlayers = if (match.currentInnings == 1) {
        activeMatch.battingFirstPlayers
    } else {
        activeMatch.battingSecondPlayers
    }
    val bowlingPlayers = if (match.currentInnings == 1) {
        activeMatch.battingSecondPlayers
    } else {
        activeMatch.battingFirstPlayers
    }

    // Controls whether the Extra popup (Wide, No-Ball, Bye, Leg-Bye) or Wicket popup is open
    var activeExtraDialog by remember { mutableStateOf<String?>(null) }
    var showWicketDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top row of buttons: "New Match" and "Full Scorecard"
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onNewMatchClick,
                    modifier = Modifier.testTag("new_match_top_button")
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = "New Match")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Match")
                }
                Button(
                    onClick = onViewFullScorecard,
                    modifier = Modifier.testTag("open_live_scorecard_button")
                ) {
                    Icon(Icons.Default.SportsCricket, contentDescription = "Full Scorecard")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Full Scorecard")
                }
            }
        }

        // Big green Scoreboard Banner showing Runs/Wickets, Overs, Extras, and Target
        item {
            val displayInnings = currentInnings ?: activeMatch.innings1
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${match.matchName} • ${match.venue} (${match.date})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "${displayInnings.battingTeamName} (${if (displayInnings.inningsNumber == 1) "1st Innings" else "2nd Innings"})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "${displayInnings.totalRuns}/${displayInnings.wickets}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("live_score_text")
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Overs: ${displayInnings.oversNotation} / ${match.totalOvers}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("live_overs_text")
                            )
                            Text(
                                text = "Extras: ${displayInnings.totalExtras} (Wd ${displayInnings.extrasWide}, Nb ${displayInnings.extrasNoBall}, B ${displayInnings.extrasBye}, Lb ${displayInnings.extrasLegBye})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                            )
                        }
                    }

                    // If we are in the 2nd innings, show the Target score and how many runs are still needed
                    if (displayInnings.inningsNumber == 2 && displayInnings.targetRuns != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val runsNeeded = (displayInnings.targetRuns - displayInnings.totalRuns).coerceAtLeast(0)
                        val ballsRemaining = (match.totalOvers * 6 - displayInnings.legalBalls).coerceAtLeast(0)
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "1st Innings: ${activeMatch.innings1.battingTeamName} ${activeMatch.innings1.totalRuns}/${activeMatch.innings1.wickets} • Target: ${displayInnings.targetRuns} (Need $runsNeeded runs in $ballsRemaining balls)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // If the match is completely finished, show the Winner / Result card
        if (match.isCompleted) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Match Result",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Match Completed — Saved Locally",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text(
                            text = match.resultSummary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.testTag("match_result_banner")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onViewFullScorecard) {
                                Text("View Full Match Scorecard")
                            }
                            OutlinedButton(onClick = onUndoLastDelivery) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Undo Last Ball")
                            }
                        }
                    }
                }
            }
        }

        // If Innings 1 just finished, show a card to pick the starting players for Innings 2
        if (activeMatch.innings1.isCompleted && match.currentInnings == 1) {
            item {
                SecondInningsSetupCard(
                    firstInningsSummary = activeMatch.innings1.completionReason,
                    targetRuns = activeMatch.innings1.totalRuns + 1,
                    battingSecondTeamName = activeMatch.battingSecondTeam.name,
                    battingSecondPlayers = activeMatch.battingSecondPlayers,
                    bowlingSecondPlayers = activeMatch.battingFirstPlayers,
                    onStartSecondInnings = onStartSecondInnings,
                    onUndoLastBall = onUndoLastDelivery
                )
            }
        } else if (currentInnings != null) {
            // Card showing the 2 batters currently at the crease and the current bowler
            item {
                val striker = currentInnings.batterStats.find { it.playerId == currentInnings.currentStrikerId }
                val nonStriker = currentInnings.batterStats.find { it.playerId == currentInnings.currentNonStrikerId }
                val bowler = currentInnings.bowlerStats.find { it.playerId == currentInnings.currentBowlerId }

                ActivePlayersCard(
                    striker = striker,
                    nonStriker = nonStriker,
                    bowler = bowler,
                    recentBalls = currentInnings.recentBallsDisplay
                )
            }

            // If a 6-ball over just ended, ask the scorer to pick the next bowler
            if (currentInnings.needsNextBowler && !currentInnings.isCompleted) {
                item {
                    SelectNextBowlerCard(
                        completedOverNumber = currentInnings.legalBalls / 6,
                        bowlingPlayers = bowlingPlayers,
                        previousBowlerId = currentInnings.previousOverBowlerId,
                        onSelectBowler = onSelectNextOverBowler,
                        onUndoLastBall = onUndoLastDelivery
                    )
                }
            } else if (!currentInnings.isCompleted) {
                // Otherwise, show the normal scoring buttons (0, 1, 2, 3, 4, 6, Extras, Wicket, Undo)
                item {
                    BallByBallControlsCard(
                        onNormalRun = { runs ->
                            onRecordDelivery(runs, "NONE", 0, false, "NONE", 0L, 0L)
                        },
                        onExtraClick = { extraType ->
                            activeExtraDialog = extraType
                        },
                        onWicketClick = {
                            showWicketDialog = true
                        },
                        onUndoClick = onUndoLastDelivery
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // Popup dialog when the user taps Wide, No-Ball, Bye, or Leg-Bye
    activeExtraDialog?.let { extraType ->
        ExtraDeliveryDialog(
            extraType = extraType,
            onDismiss = { activeExtraDialog = null },
            onConfirmExtra = { runsOffBat, extraRuns ->
                onRecordDelivery(runsOffBat, extraType, extraRuns, false, "NONE", 0L, 0L)
                activeExtraDialog = null
            }
        )
    }

    // Popup dialog when the user taps WICKET (OUT)
    if (showWicketDialog && currentInnings != null) {
        // Find batters who haven't gotten out yet and aren't already on the field
        val availableNextBatters = battingPlayers.filter { p ->
            val stat = currentInnings.batterStats.find { it.playerId == p.id }
            val isCurrentlyAtCrease =
                p.id == currentInnings.currentStrikerId || p.id == currentInnings.currentNonStrikerId
            val isAlreadyOut = stat?.isOut == true
            !isCurrentlyAtCrease && !isAlreadyOut
        }
        val strikerName = battingPlayers.find { it.id == currentInnings.currentStrikerId }?.name ?: "Striker"
        val nonStrikerName = battingPlayers.find { it.id == currentInnings.currentNonStrikerId }?.name ?: "Non-Striker"

        WicketDialog(
            strikerId = currentInnings.currentStrikerId,
            strikerName = strikerName,
            nonStrikerId = currentInnings.currentNonStrikerId,
            nonStrikerName = nonStrikerName,
            availableNextBatters = availableNextBatters,
            onDismiss = { showWicketDialog = false },
            onConfirmWicket = { dismissalType, runsCompleted, dismissedId, nextBatterId ->
                onRecordDelivery(
                    runsCompleted,
                    "NONE",
                    0,
                    true,
                    dismissalType,
                    dismissedId,
                    nextBatterId
                )
                showWicketDialog = false
            }
        )
    }
}

// Card showing the current Striker (marked with *), Non-Striker, Current Bowler, and recent balls.
@Composable
private fun ActivePlayersCard(
    striker: BatterInnings?,
    nonStriker: BatterInnings?,
    bowler: BowlerSpell?,
    recentBalls: List<String>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Batter", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.6f))
                Text("R (B)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("1s/2s/3s", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("4s / 6s", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Striker Row (Highlighted in green with a *)
            striker?.let { s ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏏 ${s.playerName} *",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1.6f)
                    )
                    Text("${s.runs} (${s.ballsFaced})", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${s.ones}/${s.twos}/${s.threes}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text("${s.fours} / ${s.sixes}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.9f))
                }
            }

            // Non-Striker Row
            nonStriker?.let { ns ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ns.playerName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1.6f)
                    )
                    Text("${ns.runs} (${ns.ballsFaced})", modifier = Modifier.weight(1f))
                    Text("${ns.ones}/${ns.twos}/${ns.threes}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text("${ns.fours} / ${ns.sixes}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.9f))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            // Current Bowler Row
            bowler?.let { b ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚾ Bowler: ${b.playerName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Overs: ${b.oversNotation} (${b.legalBalls}b) • Runs: ${b.runsConceded} • Wkts: ${b.wickets}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Horizontal row of recent ball badges (like "1", "4", "Wd", "W")
            if (recentBalls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Text(
                            text = "Balls:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(recentBalls.takeLast(12)) { badge ->
                        Surface(
                            color = when {
                                badge.contains("W") && !badge.contains("Wd") -> MaterialTheme.colorScheme.errorContainer
                                badge.contains("4") || badge.contains("6") -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Card containing the tap buttons for scoring each ball: 0, 1, 2, 3, 4, 6, Extras, Wicket, and Undo.
@Composable
private fun BallByBallControlsCard(
    onNormalRun: (Int) -> Unit,
    onExtraClick: (String) -> Unit,
    onWicketClick: () -> Unit,
    onUndoClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Ball-by-Ball Scorer Input",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            // Row 1: Normal Runs (0, 1, 2, 3, 4, 6)
            Text("Normal Runs (Legal Ball):", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(0, 1, 2, 3, 4, 6).forEach { run ->
                    // Color 4 and 6 orange so boundaries stand out
                    val isBoundary = (run == 4 || run == 6)
                    Button(
                        onClick = { onNormalRun(run) },
                        colors = if (isBoundary) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("run_btn_$run")
                    ) {
                        Text(text = "$run", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Row 2: Extras (Wide, No-Ball, Bye, Leg-Bye)
            Text("Extras (Wide & No-Ball do NOT count as legal ball):", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "WIDE" to "Wide",
                    "NO_BALL" to "No-Ball",
                    "BYE" to "Bye",
                    "LEG_BYE" to "Leg-Bye"
                ).forEach { (type, label) ->
                    OutlinedButton(
                        onClick = { onExtraClick(type) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("extra_btn_$type")
                    ) {
                        Text(text = label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Row 3: Wicket (OUT) button in red, and Undo Ball button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onWicketClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("wicket_button")
                ) {
                    Text("WICKET (OUT)", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onUndoClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("undo_ball_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo Last Ball")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Undo Ball")
                }
            }
        }
    }
}

// Card shown at the end of every 6-ball over so the scorer can pick who bowls the next over.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectNextBowlerCard(
    completedOverNumber: Int,
    bowlingPlayers: List<PlayerEntity>,
    previousBowlerId: Long,
    onSelectBowler: (Long) -> Unit,
    onUndoLastBall: () -> Unit
) {
    // In cricket, the same bowler cannot bowl two overs in a row, so we filter out previousBowlerId
    val eligibleBowlers = bowlingPlayers.filter { it.id != previousBowlerId }
        .ifEmpty { bowlingPlayers }

    var chosenBowlerId by remember(eligibleBowlers) {
        mutableLongStateOf(eligibleBowlers.firstOrNull()?.id ?: 0L)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "End of Over $completedOverNumber — Select Next Bowler",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Strike has automatically rotated for the new over. Choose the bowler for Over ${completedOverNumber + 1}:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                eligibleBowlers.forEach { bowler ->
                    FilterChip(
                        selected = (chosenBowlerId == bowler.id),
                        onClick = { chosenBowlerId = bowler.id },
                        label = { Text(bowler.name) },
                        modifier = Modifier.testTag("next_bowler_chip_${bowler.id}")
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (chosenBowlerId != 0L) onSelectBowler(chosenBowlerId)
                    },
                    modifier = Modifier.testTag("confirm_next_bowler_button")
                ) {
                    Text("Start Next Over")
                }
                OutlinedButton(onClick = onUndoLastBall) {
                    Text("Undo Last Ball")
                }
            }
        }
    }
}

// Card shown when Innings 1 finishes so the scorer can pick the opening Striker, Non-Striker, and Bowler for Innings 2.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SecondInningsSetupCard(
    firstInningsSummary: String,
    targetRuns: Int,
    battingSecondTeamName: String,
    battingSecondPlayers: List<PlayerEntity>,
    bowlingSecondPlayers: List<PlayerEntity>,
    onStartSecondInnings: (strikerId: Long, nonStrikerId: Long, bowlerId: Long) -> Unit,
    onUndoLastBall: () -> Unit
) {
    var strikerId by remember(battingSecondPlayers) {
        mutableLongStateOf(battingSecondPlayers.getOrNull(0)?.id ?: 0L)
    }
    var nonStrikerId by remember(battingSecondPlayers) {
        mutableLongStateOf(battingSecondPlayers.getOrNull(1)?.id ?: 0L)
    }
    var bowlerId by remember(bowlingSecondPlayers) {
        mutableLongStateOf(bowlingSecondPlayers.getOrNull(0)?.id ?: 0L)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "1st Innings Complete! ($firstInningsSummary)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Target for $battingSecondTeamName: $targetRuns runs",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Text("Select Opening Striker ($battingSecondTeamName):", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                battingSecondPlayers.forEach { p ->
                    FilterChip(
                        selected = (strikerId == p.id),
                        onClick = {
                            strikerId = p.id
                            if (nonStrikerId == p.id) {
                                nonStrikerId = battingSecondPlayers.firstOrNull { it.id != p.id }?.id ?: 0L
                            }
                        },
                        label = { Text(p.name) }
                    )
                }
            }

            Text("Select Opening Non-Striker ($battingSecondTeamName):", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                battingSecondPlayers.forEach { p ->
                    FilterChip(
                        selected = (nonStrikerId == p.id),
                        onClick = {
                            nonStrikerId = p.id
                            if (strikerId == p.id) {
                                strikerId = battingSecondPlayers.firstOrNull { it.id != p.id }?.id ?: 0L
                            }
                        },
                        label = { Text(p.name) }
                    )
                }
            }

            Text("Select Opening Bowler:", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                bowlingSecondPlayers.forEach { p ->
                    FilterChip(
                        selected = (bowlerId == p.id),
                        onClick = { bowlerId = p.id },
                        label = { Text(p.name) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onStartSecondInnings(strikerId, nonStrikerId, bowlerId) },
                    enabled = strikerId != 0L && nonStrikerId != 0L && strikerId != nonStrikerId && bowlerId != 0L,
                    modifier = Modifier.testTag("start_second_innings_button")
                ) {
                    Text("Start 2nd Innings Chase")
                }
                OutlinedButton(onClick = onUndoLastBall) {
                    Text("Undo Last 1st-Innings Ball")
                }
            }
        }
    }
}

// Popup window for entering an Extra (Wide, No-Ball, Bye, or Leg-Bye).
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExtraDeliveryDialog(
    extraType: String,
    onDismiss: () -> Unit,
    onConfirmExtra: (runsOffBat: Int, extraRuns: Int) -> Unit
) {
    var selectedAdditionalRuns by remember { mutableIntStateOf(0) }
    var selectedByeRuns by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (extraType) {
                    "WIDE" -> "Record Wide Delivery (Illegal Ball)"
                    "NO_BALL" -> "Record No-Ball Delivery (Illegal Ball)"
                    "BYE" -> "Record Bye Runs (Legal Ball)"
                    else -> "Record Leg-Bye Runs (Legal Ball)"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (extraType) {
                    "WIDE" -> {
                        Text("Wide adds +1 penalty extra and does NOT count as a legal ball. Any additional runs ran by batters?")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0, 1, 2, 3, 4).forEach { r ->
                                FilterChip(
                                    selected = (selectedAdditionalRuns == r),
                                    onClick = { selectedAdditionalRuns = r },
                                    label = { Text(if (r == 0) "Wide only (1)" else "Wd + $r (${1 + r})") }
                                )
                            }
                        }
                    }
                    "NO_BALL" -> {
                        Text("No-Ball adds +1 penalty extra and does NOT count as a legal ball. Runs scored off the bat on this No-Ball?")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0, 1, 2, 3, 4, 6).forEach { r ->
                                FilterChip(
                                    selected = (selectedAdditionalRuns == r),
                                    onClick = { selectedAdditionalRuns = r },
                                    label = { Text(if (r == 0) "Nb only (1)" else "Nb + $r bat") }
                                )
                            }
                        }
                    }
                    else -> {
                        Text("Select number of ${if (extraType == "BYE") "Bye" else "Leg-Bye"} extras ran (counts as 1 legal ball):")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 3, 4).forEach { r ->
                                FilterChip(
                                    selected = (selectedByeRuns == r),
                                    onClick = { selectedByeRuns = r },
                                    label = { Text("$r") }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (extraType) {
                        "WIDE" -> onConfirmExtra(selectedAdditionalRuns, 1)
                        "NO_BALL" -> onConfirmExtra(selectedAdditionalRuns, 1)
                        else -> onConfirmExtra(0, selectedByeRuns)
                    }
                },
                modifier = Modifier.testTag("confirm_extra_button")
            ) {
                Text("Record Extra")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Popup window when a batter gets out (Bowled, Caught, LBW, Stumped, or Run Out).
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WicketDialog(
    strikerId: Long,
    strikerName: String,
    nonStrikerId: Long,
    nonStrikerName: String,
    availableNextBatters: List<PlayerEntity>,
    onDismiss: () -> Unit,
    onConfirmWicket: (
        dismissalType: String,
        runsCompleted: Int,
        dismissedPlayerId: Long,
        nextBatterId: Long
    ) -> Unit
) {
    val dismissalOptions = listOf(
        "BOWLED" to "Bowled",
        "CAUGHT" to "Caught",
        "LBW" to "LBW",
        "STUMPED" to "Stumped",
        "RUN_OUT" to "Run Out"
    )
    var dismissalType by remember { mutableStateOf("BOWLED") }
    var dismissedPlayerId by remember { mutableLongStateOf(strikerId) }
    var runOutCompletedRuns by remember { mutableIntStateOf(0) }
    var nextBatterId by remember(availableNextBatters) {
        mutableLongStateOf(availableNextBatters.firstOrNull()?.id ?: 0L)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Wicket / Dismissal", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Select Dismissal Type:", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dismissalOptions.forEach { (code, label) ->
                        FilterChip(
                            selected = (dismissalType == code),
                            onClick = {
                                dismissalType = code
                                if (code != "RUN_OUT") {
                                    dismissedPlayerId = strikerId
                                    runOutCompletedRuns = 0
                                }
                            },
                            label = { Text(label) },
                            modifier = Modifier.testTag("dismissal_type_$code")
                        )
                    }
                }

                // Helpful reminder showing whether the bowler gets credit for this type of wicket
                Text(
                    text = if (dismissalType == "RUN_OUT") {
                        "Rule: Run Out does NOT credit the bowler's wicket count."
                    } else {
                        "Rule: $dismissalType credits the bowler with +1 wicket."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // On a Run Out, either batter could be out, and they might have completed 1 or 2 runs first
                if (dismissalType == "RUN_OUT") {
                    Text("Who was Run Out?", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = (dismissedPlayerId == strikerId),
                            onClick = { dismissedPlayerId = strikerId },
                            label = { Text("Striker ($strikerName)") }
                        )
                        FilterChip(
                            selected = (dismissedPlayerId == nonStrikerId),
                            onClick = { dismissedPlayerId = nonStrikerId },
                            label = { Text("Non-Striker ($nonStrikerName)") }
                        )
                    }

                    Text("Completed runs before Run Out:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 1, 2, 3).forEach { r ->
                            FilterChip(
                                selected = (runOutCompletedRuns == r),
                                onClick = { runOutCompletedRuns = r },
                                label = { Text("$r") }
                            )
                        }
                    }
                }

                // If there are still batters waiting on the bench, pick who walks in next
                if (availableNextBatters.isNotEmpty()) {
                    Text("2. Select Next Incoming Batter:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableNextBatters.forEach { p ->
                            FilterChip(
                                selected = (nextBatterId == p.id),
                                onClick = { nextBatterId = p.id },
                                label = { Text(p.name) }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Last wicket! This dismissal will complete the innings (All Out).",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmWicket(
                        dismissalType,
                        if (dismissalType == "RUN_OUT") runOutCompletedRuns else 0,
                        dismissedPlayerId,
                        nextBatterId
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_wicket_button")
            ) {
                Text("Confirm Wicket")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
