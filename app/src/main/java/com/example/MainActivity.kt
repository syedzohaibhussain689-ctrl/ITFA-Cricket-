package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CricketDatabase
import com.example.data.CricketRepository
import com.example.ui.CricketViewModel
import com.example.ui.CricketViewModelFactory
import com.example.ui.FullScorecardView
import com.example.ui.MatchScorerScreen
import com.example.ui.RulesAndDismissalDialog
import com.example.ui.SavedMatchesScreen
import com.example.ui.TeamsScreen
import com.example.ui.theme.MyApplicationTheme

// MainActivity is the front door of our Android app.
// When you tap the app icon on the phone's home screen, Android runs onCreate() right here.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Open our local SQLite database on the phone
        val database = CricketDatabase.getDatabase(applicationContext)
        // 2. Create the Repository that talks to the database
        val repository = CricketRepository(database.cricketDao())
        // 3. Create the factory that gives the Repository to our ViewModel
        val factory = CricketViewModelFactory(repository)

        // 4. Draw the app screen using Jetpack Compose
        setContent {
            MyApplicationTheme {
                val viewModel: CricketViewModel = viewModel(factory = factory)
                CricketScorerApp(viewModel = viewModel)
            }
        }
    }
}

// This composable holds the top green header bar, the 3 bottom tabs, and switches between screens.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CricketScorerApp(viewModel: CricketViewModel) {
    // Watch the live state from the ViewModel so the screen updates automatically whenever data changes
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Keeps track of which bottom tab is selected:
    // 0 = Teams, 1 = Live Match, 2 = Saved Matches
    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var showRulesDialog by rememberSaveable { mutableStateOf(false) }

    // If the user presses the phone's Back button while on Tab 1 or Tab 2, go back to Tab 0 (Teams)
    BackHandler(enabled = uiState.selectedScorecardMatch == null && currentTab != 0) {
        currentTab = 0
    }

    // Whenever there is a status message (like "Team saved"), show it in a small bar at the bottom
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            // Green bar at the top of the screen with the app title and Rules Info ("i") button
            TopAppBar(
                title = {
                    Text(
                        text = "EF101 Cricket Scorer",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(
                        onClick = { showRulesDialog = true },
                        modifier = Modifier.testTag("open_rules_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "View Cricket & Dismissal Rules"
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Show the 3 navigation tabs at the bottom (unless we are viewing a full-screen scorecard)
            if (uiState.selectedScorecardMatch == null) {
                NavigationBar {
                    NavigationBarItem(
                        selected = (currentTab == 0),
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Group, contentDescription = "Teams Tab") },
                        label = { Text("Teams") },
                        modifier = Modifier.testTag("nav_tab_teams")
                    )
                    NavigationBarItem(
                        selected = (currentTab == 1),
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.SportsCricket, contentDescription = "Live Match Tab") },
                        label = { Text("Live Match") },
                        modifier = Modifier.testTag("nav_tab_match")
                    )
                    NavigationBarItem(
                        selected = (currentTab == 2),
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.History, contentDescription = "Saved Matches Tab") },
                        label = { Text("Saved Matches") },
                        modifier = Modifier.testTag("nav_tab_history")
                    )
                }
            }
        }
    ) { innerPadding ->
        // If the user clicked "Full Scorecard", show the scorecard screen.
        // Otherwise, show whichever tab (0, 1, or 2) is currently selected.
        val scorecardDetail = uiState.selectedScorecardMatch
        if (scorecardDetail != null) {
            FullScorecardView(
                matchDetail = scorecardDetail,
                onClose = { viewModel.openScorecard(null) },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            when (currentTab) {
                0 -> TeamsScreen(
                    teams = uiState.teams,
                    playersByTeam = uiState.playersByTeam,
                    onCreateTeam = { viewModel.createTeam(it) },
                    onAddPlayer = { teamId, playerName ->
                        viewModel.addPlayerToTeam(teamId, playerName)
                    },
                    onGoToNewMatch = { currentTab = 1 },
                    modifier = Modifier.padding(innerPadding)
                )
                1 -> MatchScorerScreen(
                    teams = uiState.teams,
                    playersByTeam = uiState.playersByTeam,
                    activeMatch = uiState.activeMatch,
                    onStartNewMatch = { aId, bId, name, venue, date, overs, batFirst, sId, nsId, bwId, onCreated ->
                        viewModel.startNewMatch(
                            teamAId = aId,
                            teamBId = bId,
                            matchName = name,
                            venue = venue,
                            date = date,
                            totalOvers = overs,
                            battingFirstTeamId = batFirst,
                            openingStrikerId = sId,
                            openingNonStrikerId = nsId,
                            openingBowlerId = bwId,
                            onMatchCreated = onCreated
                        )
                    },
                    onStartSecondInnings = { sId, nsId, bwId ->
                        viewModel.startSecondInnings(sId, nsId, bwId)
                    },
                    onRecordDelivery = { runs, extraType, extraRuns, isWicket, dismissalType, dismissedId, nextBatterId ->
                        viewModel.recordDelivery(
                            runsOffBat = runs,
                            extraType = extraType,
                            extraRuns = extraRuns,
                            isWicket = isWicket,
                            dismissalType = dismissalType,
                            dismissedPlayerId = dismissedId,
                            nextBatterId = nextBatterId
                        )
                    },
                    onSelectNextOverBowler = { nextBowlerId ->
                        viewModel.selectNextOverBowler(nextBowlerId)
                    },
                    onUndoLastDelivery = { viewModel.undoLastDelivery() },
                    onViewFullScorecard = { matchId -> viewModel.openScorecard(matchId) },
                    modifier = Modifier.padding(innerPadding)
                )
                2 -> SavedMatchesScreen(
                    matches = uiState.matches,
                    teams = uiState.teams,
                    onOpenScorecard = { matchId -> viewModel.openScorecard(matchId) },
                    onResumeMatch = { matchId ->
                        viewModel.selectActiveMatch(matchId)
                        currentTab = 1
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    // Show the Rules popup window if the user tapped the "i" icon in the top bar
    if (showRulesDialog) {
        RulesAndDismissalDialog(onDismiss = { showRulesDialog = false })
    }
}
