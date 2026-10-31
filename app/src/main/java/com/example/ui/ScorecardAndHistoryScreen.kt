package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Innings
import com.example.data.MatchEntity
import com.example.data.TeamEntity

// This file has two parts:
// 1. SavedMatchesScreen (Tab 3): Lists all past and current matches saved in the phone's database.
// 2. FullScorecardView: Shows the complete batting, bowling, and extras tables for any match.

@Composable
fun SavedMatchesScreen(
    matches: List<MatchEntity>,
    teams: List<TeamEntity>,
    onOpenScorecard: (Long) -> Unit,
    onResumeMatch: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Quick lookup map to get a team's name from its ID number
    val teamMap = remember(teams) { teams.associateBy { it.id } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Info banner at the top of the Saved Matches screen
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Saved Matches",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Saved Matches & Scorecards (Offline SQLite)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All completed and in-progress matches are stored locally on your phone. Tap any match below to reopen its full scorecard even after closing and reopening the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // If no matches have been created yet, show a friendly message
        if (matches.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsScore,
                            contentDescription = "No Matches Yet",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Saved Matches Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Go to the Live Match tab to start a match. Your match summaries and scorecards will appear here automatically.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else {
            // Draw one summary card for each saved match
            items(matches, key = { it.id }) { match ->
                val teamAName = teamMap[match.teamAId]?.name ?: "Team A"
                val teamBName = teamMap[match.teamBId]?.name ?: "Team B"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("saved_match_card_${match.id}"),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$teamAName vs $teamBName",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = if (match.isCompleted) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = if (match.isCompleted) "Completed" else "In Progress",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "${match.matchName} • ${match.venue} • ${match.date} (${match.totalOvers} Overs)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (match.resultSummary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Result: ${match.resultSummary}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onOpenScorecard(match.id) },
                                modifier = Modifier.testTag("reopen_scorecard_btn_${match.id}")
                            ) {
                                Icon(Icons.Default.SportsScore, contentDescription = "Open Scorecard")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Scorecard")
                            }

                            // If the match isn't finished yet, let the user jump back in to keep scoring
                            if (!match.isCompleted) {
                                OutlinedButton(
                                    onClick = { onResumeMatch(match.id) },
                                    modifier = Modifier.testTag("resume_match_btn_${match.id}")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume Match")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resume Scoring")
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// Shows the complete match scorecard with both innings, batting stats, bowling stats, and extras.
@Composable
fun FullScorecardView(
    matchDetail: ActiveMatchDetail,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pressing the phone's Back button closes the scorecard and goes back to the previous screen
    BackHandler { onClose() }

    val match = matchDetail.match
    // Lets the user switch between viewing the 1st Innings card and the 2nd Innings card
    var selectedInningsTab by remember {
        mutableIntStateOf(if (matchDetail.innings2 != null) 2 else 1)
    }

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
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_scorecard_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Back")
                }
                Text(
                    text = "Full Match Scorecard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Top green summary card showing both innings scores and who won
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${matchDetail.teamA.name} vs ${matchDetail.teamB.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "${match.matchName} • ${match.venue} • ${match.date} (${match.totalOvers} Overs)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "1st Innings: ${matchDetail.innings1.battingTeamName} — ${matchDetail.innings1.totalRuns}/${matchDetail.innings1.wickets} (${matchDetail.innings1.oversNotation} ov)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    matchDetail.innings2?.let { in2 ->
                        Text(
                            text = "2nd Innings: ${in2.battingTeamName} — ${in2.totalRuns}/${in2.wickets} (${in2.oversNotation} ov) [Target: ${in2.targetRuns}]",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    if (match.resultSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Winner",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = match.resultSummary,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Buttons to switch between 1st Innings and 2nd Innings details
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = (selectedInningsTab == 1),
                    onClick = { selectedInningsTab = 1 },
                    label = {
                        Text("1st Innings (${matchDetail.innings1.battingTeamName})")
                    },
                    modifier = Modifier.testTag("scorecard_tab_in1")
                )
                if (matchDetail.innings2 != null) {
                    FilterChip(
                        selected = (selectedInningsTab == 2),
                        onClick = { selectedInningsTab = 2 },
                        label = {
                            Text("2nd Innings (${matchDetail.innings2.battingTeamName})")
                        },
                        modifier = Modifier.testTag("scorecard_tab_in2")
                    )
                }
            }
        }

        val displayedInnings = if (selectedInningsTab == 2 && matchDetail.innings2 != null) {
            matchDetail.innings2
        } else {
            matchDetail.innings1
        }

        item {
            InningsScorecardCard(innings = displayedInnings)
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// Draws the detailed Batting Table, Bowling Table, Extras breakdown, and Ball-by-Ball log for one innings.
@Composable
private fun InningsScorecardCard(innings: Innings) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Team Total, Wickets, and Overs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${innings.battingTeamName} Innings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${innings.totalRuns}/${innings.wickets} (${innings.oversNotation} / ${innings.maxOvers} ov)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Extras breakdown (Wide, No-Ball, Bye, Leg-Bye)
            Text(
                text = "Extras: ${innings.totalExtras} (Wide: ${innings.extrasWide}, No-Ball: ${innings.extrasNoBall}, Bye: ${innings.extrasBye}, Leg-Bye: ${innings.extrasLegBye})",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )

            HorizontalDivider()

            // BATTING TABLE HEADER (Runs, Balls, 1s, 2s, 3s, 4s, 6s)
            Text(
                text = "Batting Statistics",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Batter", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.2f))
                Text("R", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f))
                Text("B", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f))
                Text("1s", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f))
                Text("2s", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f))
                Text("3s", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f))
                Text("4s", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f))
                Text("6s", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f))
            }
            HorizontalDivider()

            // Draw one row for each batter on the team
            innings.batterStats.forEach { b ->
                val isAtCrease = !b.isOut &&
                    (b.playerId == innings.currentStrikerId || b.playerId == innings.currentNonStrikerId)
                val statusLabel = when {
                    b.isOut -> b.dismissalText
                    isAtCrease -> if (b.playerId == innings.currentStrikerId) "not out *" else "not out"
                    b.didBat -> "not out"
                    else -> "did not bat"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(2.2f)) {
                        Text(
                            text = b.playerName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isAtCrease) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (b.isOut) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Text("${b.runs}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f))
                    Text("${b.ballsFaced}", modifier = Modifier.weight(0.6f))
                    Text("${b.ones}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.5f))
                    Text("${b.twos}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.5f))
                    Text("${b.threes}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.5f))
                    Text("${b.fours}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.5f))
                    Text("${b.sixes}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.5f))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider()

            // BOWLING TABLE HEADER (Overs, Legal Balls, Runs Conceded, Wickets)
            Text(
                text = "Bowling Statistics (${innings.bowlingTeamName})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Bowler", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.2f))
                Text("Overs", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                Text("Balls", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                Text("Runs", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                Text("Wkts", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
            }
            HorizontalDivider()

            // Draw one row for each bowler
            innings.bowlerStats.forEach { bw ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(bw.playerName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(2.2f))
                    Text(bw.oversNotation, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.9f))
                    Text("${bw.legalBalls}", modifier = Modifier.weight(0.9f))
                    Text("${bw.runsConceded}", modifier = Modifier.weight(0.9f))
                    Text("${bw.wickets}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                }
            }

            // Show the full list of balls bowled in this innings at the bottom
            if (innings.recentBallsDisplay.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Delivery-by-Delivery Event Log (${innings.recentBallsDisplay.size} deliveries): " +
                        innings.recentBallsDisplay.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
