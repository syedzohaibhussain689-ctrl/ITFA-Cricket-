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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.PlayerEntity
import com.example.data.TeamEntity

// This screen is the first tab ("Teams").
// It lets you create new teams, view your saved teams, and add players to any team.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeamsScreen(
    teams: List<TeamEntity>,
    playersByTeam: Map<Long, List<PlayerEntity>>,
    onCreateTeam: (String) -> Unit,
    onAddPlayer: (Long, String) -> Unit,
    onGoToNewMatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Holds the text typed into the "New Team Name" box
    var newTeamName by remember { mutableStateOf("") }

    // Holds the text typed into the "Add Player" box for each team card
    val playerInputs = remember { mutableStateMapOf<Long, String>() }

    // LazyColumn is a scrollable list that only draws the items currently visible on screen
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top banner explaining how teams and players work
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
                            imageVector = Icons.Default.Group,
                            contentDescription = "Teams Icon",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Local Teams & Players (SQLite)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create teams and add players below. Everything is saved offline on your phone. Each team needs at least $MIN_PLAYERS_PER_TEAM players to start a match.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Box to type a new team name and save it
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Create New Team",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTeamName,
                            onValueChange = { newTeamName = it },
                            label = { Text("Team Name (e.g. Islamabad United)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_team_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newTeamName.isNotBlank()) {
                                    onCreateTeam(newTeamName)
                                    newTeamName = "" // Clear the text box after saving
                                }
                            },
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("create_team_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Create Team")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                }
            }
        }

        // Heading showing how many teams are saved
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Teams (${teams.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onGoToNewMatch,
                    modifier = Modifier.testTag("go_to_match_setup_button")
                ) {
                    Text("Go to Match Scorer →")
                }
            }
        }

        // Draw one card for each saved team
        items(teams, key = { it.id }) { team ->
            val teamPlayers = playersByTeam[team.id].orEmpty()
            // Check if this team has at least 2 players so it's ready for a match
            val isReady = teamPlayers.size >= MIN_PLAYERS_PER_TEAM
            val currentInput = playerInputs[team.id] ?: ""

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("team_card_${team.id}"),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = team.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        // Small green or red badge showing if the team has enough players
                        Surface(
                            color = if (isReady) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = "Team Readiness Status",
                                    tint = if (isReady) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isReady) {
                                        "${teamPlayers.size} Players (Ready)"
                                    } else {
                                        "${teamPlayers.size}/$MIN_PLAYERS_PER_TEAM Players (Incomplete)"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isReady) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // List all players in this team as small tags/chips
                    if (teamPlayers.isEmpty()) {
                        Text(
                            text = "No players added yet. Add at least $MIN_PLAYERS_PER_TEAM players to use this team in a match.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            teamPlayers.forEachIndexed { index, player ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text("${index + 1}. ${player.name}") }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Text box and button to add a new player to this team
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = currentInput,
                            onValueChange = { playerInputs[team.id] = it },
                            label = { Text("Add Player to ${team.name}") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_player_input_${team.id}")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (currentInput.isNotBlank()) {
                                    onAddPlayer(team.id, currentInput)
                                    playerInputs[team.id] = "" // Clear input box after adding player
                                }
                            },
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("add_player_button_${team.id}")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Player")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Popup window that opens when you tap the "i" (Info) button in the top bar.
// It explains our group's dismissal rules and cricket rules in plain English.
@Composable
fun RulesAndDismissalDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "EF101-P02 Cricket & Dismissal Rules",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. Documented Dismissal Rules:",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "• Bowled, Caught, LBW, Stumped: The striker is marked OUT and replaced by the next available batter. The bowler IS credited with +1 wicket.\n" +
                        "• Run Out: Either the Striker or Non-Striker can be dismissed (with 0–3 completed runs added). The bowler is NOT credited with a wicket.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "2. Legal Balls, Overs & Extras:",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "• Normal runs (0, 1, 2, 3, 4, 6), Byes, and Leg-Byes count as 1 legal ball.\n" +
                        "• Wide and No-Ball add +1 penalty extra run (plus any additional runs) and do NOT count as a legal ball in the over.\n" +
                        "• 6 legal balls = 1 completed over (0.1 → 0.5 → 1.0).",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "3. Strike Rotation & Delivery Records:",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "• Striker and Non-Striker swap ends automatically on odd runs (1 or 3) and at the end of every completed over.\n" +
                        "• Every ball is saved as a DeliveryEntity row in Room SQLite, allowing full innings reconstruction offline.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_rules_dialog_button")
            ) {
                Text("Got It")
            }
        }
    )
}
