package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CricketRepository
import com.example.data.DeliveryEntity
import com.example.data.Innings
import com.example.data.MatchEntity
import com.example.data.PlayerEntity
import com.example.data.TeamEntity
import com.example.data.reconstructInnings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Every team must have at least 2 players before a match is allowed to start
const val MIN_PLAYERS_PER_TEAM = 2

// Bundles everything about one match (teams, players, both innings, and all balls) into one neat package
data class ActiveMatchDetail(
    val match: MatchEntity,
    val teamA: TeamEntity,
    val teamB: TeamEntity,
    val battingFirstTeam: TeamEntity,
    val battingSecondTeam: TeamEntity,
    val battingFirstPlayers: List<PlayerEntity>,
    val battingSecondPlayers: List<PlayerEntity>,
    val innings1: Innings,
    val innings2: Innings?,
    val deliveriesInnings1: List<DeliveryEntity>,
    val deliveriesInnings2: List<DeliveryEntity>
)

// Holds all the data our screens need to draw themselves at any moment
data class CricketUiState(
    val teams: List<TeamEntity> = emptyList(),
    val playersByTeam: Map<Long, List<PlayerEntity>> = emptyMap(),
    val matches: List<MatchEntity> = emptyList(),
    val activeMatch: ActiveMatchDetail? = null,
    val selectedScorecardMatch: ActiveMatchDetail? = null,
    val statusMessage: String? = null
)

// The ViewModel is the "brain" of our screens.
// When you tap a button on the screen (like "Add Player" or "4 Runs"), the screen calls a function here,
// and the ViewModel updates the database and refreshes the screen.
class CricketViewModel(private val repository: CricketRepository) : ViewModel() {

    // Tracks which match we are currently scoring, which scorecard is open, and any popup message
    private val selectedActiveMatchId = MutableStateFlow<Long?>(null)
    private val selectedScorecardMatchId = MutableStateFlow<Long?>(null)
    private val statusMessageFlow = MutableStateFlow<String?>(null)

    init {
        // When the app starts, add our 2 starter teams if the database is completely empty
        viewModelScope.launch {
            repository.seedDemoDataIfEmpty()
        }
    }

    // Combines the database tables into a single live state (uiState) that our screens watch
    val uiState: StateFlow<CricketUiState> = combine(
        repository.allTeams,
        repository.allPlayers,
        repository.allMatches,
        repository.allDeliveries,
        combine(selectedActiveMatchId, selectedScorecardMatchId, statusMessageFlow) { a, s, m ->
            Triple(a, s, m)
        }
    ) { teams, players, matches, deliveries, selectionTriple ->
        val (activeId, scorecardId, message) = selectionTriple
        // Group players by their team ID so we can quickly find a team's roster
        val playersByTeam = players.groupBy { it.teamId }

        // Pick which match to show on the Live Match tab
        val currentMatchEntity = matches.find { it.id == activeId }
            ?: matches.firstOrNull { !it.isCompleted }
            ?: matches.firstOrNull()

        val activeDetail = currentMatchEntity?.let {
            buildMatchDetail(it, teams, playersByTeam, deliveries)
        }

        val scorecardDetail = matches.find { it.id == scorecardId }?.let {
            buildMatchDetail(it, teams, playersByTeam, deliveries)
        }

        CricketUiState(
            teams = teams,
            playersByTeam = playersByTeam,
            matches = matches,
            activeMatch = activeDetail,
            selectedScorecardMatch = scorecardDetail,
            statusMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CricketUiState()
    )

    // Helper function that takes a saved match and its balls and builds the Innings 1 and Innings 2 scoreboards
    private fun buildMatchDetail(
        match: MatchEntity,
        teams: List<TeamEntity>,
        playersByTeam: Map<Long, List<PlayerEntity>>,
        allDeliveries: List<DeliveryEntity>
    ): ActiveMatchDetail? {
        val teamA = teams.find { it.id == match.teamAId } ?: return null
        val teamB = teams.find { it.id == match.teamBId } ?: return null

        // Figure out which team bats first and which team bats second
        val battingFirstTeam = if (match.battingFirstTeamId == teamA.id) teamA else teamB
        val battingSecondTeam = if (match.battingFirstTeamId == teamA.id) teamB else teamA

        val battingFirstPlayers = playersByTeam[battingFirstTeam.id].orEmpty()
        val battingSecondPlayers = playersByTeam[battingSecondTeam.id].orEmpty()

        // Filter the balls for this match into 1st Innings balls and 2nd Innings balls
        val matchDeliveries = allDeliveries.filter { it.matchId == match.id }
        val in1Deliveries = matchDeliveries.filter { it.inningsNumber == 1 }
        val in2Deliveries = matchDeliveries.filter { it.inningsNumber == 2 }

        // Build the 1st Innings scoreboard from its balls
        val innings1 = reconstructInnings(
            inningsNumber = 1,
            battingTeamName = battingFirstTeam.name,
            bowlingTeamName = battingSecondTeam.name,
            battingPlayers = battingFirstPlayers,
            bowlingPlayers = battingSecondPlayers,
            openingStrikerId = match.in1OpeningStrikerId,
            openingNonStrikerId = match.in1OpeningNonStrikerId,
            openingBowlerId = match.in1OpeningBowlerId,
            maxOvers = match.totalOvers,
            targetRuns = null,
            deliveries = in1Deliveries
        )

        // If the 2nd Innings has started, build the 2nd Innings scoreboard with Target = Innings 1 Runs + 1
        val innings2 = if (match.currentInnings == 2 && match.in2OpeningStrikerId != 0L) {
            reconstructInnings(
                inningsNumber = 2,
                battingTeamName = battingSecondTeam.name,
                bowlingTeamName = battingFirstTeam.name,
                battingPlayers = battingSecondPlayers,
                bowlingPlayers = battingFirstPlayers,
                openingStrikerId = match.in2OpeningStrikerId,
                openingNonStrikerId = match.in2OpeningNonStrikerId,
                openingBowlerId = match.in2OpeningBowlerId,
                maxOvers = match.totalOvers,
                targetRuns = innings1.totalRuns + 1,
                deliveries = in2Deliveries
            )
        } else {
            null
        }

        return ActiveMatchDetail(
            match = match,
            teamA = teamA,
            teamB = teamB,
            battingFirstTeam = battingFirstTeam,
            battingSecondTeam = battingSecondTeam,
            battingFirstPlayers = battingFirstPlayers,
            battingSecondPlayers = battingSecondPlayers,
            innings1 = innings1,
            innings2 = innings2,
            deliveriesInnings1 = in1Deliveries,
            deliveriesInnings2 = in2Deliveries
        )
    }

    // Clears the small snackbar message at the bottom of the screen after it has been shown
    fun clearMessage() {
        statusMessageFlow.value = null
    }

    // Called when the user types a team name and taps "Save"
    fun createTeam(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            statusMessageFlow.value = "Please enter a team name."
            return
        }
        viewModelScope.launch {
            repository.createTeam(trimmed)
            statusMessageFlow.value = "Team '$trimmed' saved locally."
        }
    }

    // Called when the user adds a player to a team
    fun addPlayerToTeam(teamId: Long, playerName: String) {
        val trimmed = playerName.trim()
        if (trimmed.isEmpty()) {
            statusMessageFlow.value = "Please enter a player name."
            return
        }
        viewModelScope.launch {
            repository.addPlayer(teamId, trimmed)
            statusMessageFlow.value = "Player '$trimmed' added."
        }
    }

    // Checks that both teams have enough players and starts a brand new match
    fun startNewMatch(
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
        onMatchCreated: () -> Unit
    ) {
        val state = uiState.value
        val teamAPlayers = state.playersByTeam[teamAId].orEmpty()
        val teamBPlayers = state.playersByTeam[teamBId].orEmpty()

        if (teamAId == teamBId) {
            statusMessageFlow.value = "Please select two different teams."
            return
        }
        if (teamAPlayers.size < MIN_PLAYERS_PER_TEAM || teamBPlayers.size < MIN_PLAYERS_PER_TEAM) {
            statusMessageFlow.value =
                "Incomplete team! Each team must have at least $MIN_PLAYERS_PER_TEAM players."
            return
        }
        if (openingStrikerId == openingNonStrikerId) {
            statusMessageFlow.value = "Striker and Non-Striker must be two different players."
            return
        }

        viewModelScope.launch {
            val newMatch = MatchEntity(
                teamAId = teamAId,
                teamBId = teamBId,
                matchName = matchName.ifBlank { "Friendly T20/Short Match" },
                venue = venue.ifBlank { "University Cricket Ground" },
                date = date.ifBlank { "2026-11-09" },
                totalOvers = totalOvers.coerceAtLeast(1),
                battingFirstTeamId = battingFirstTeamId,
                currentInnings = 1,
                in1OpeningStrikerId = openingStrikerId,
                in1OpeningNonStrikerId = openingNonStrikerId,
                in1OpeningBowlerId = openingBowlerId
            )
            val newMatchId = repository.createMatch(newMatch)
            selectedActiveMatchId.value = newMatchId
            onMatchCreated()
        }
    }

    // Switches the match from Innings 1 to Innings 2 once the user picks the new opening players
    fun startSecondInnings(
        openingStrikerId: Long,
        openingNonStrikerId: Long,
        openingBowlerId: Long
    ) {
        val active = uiState.value.activeMatch ?: return
        if (openingStrikerId == openingNonStrikerId) {
            statusMessageFlow.value = "Striker and Non-Striker must be two different players."
            return
        }
        viewModelScope.launch {
            val updated = active.match.copy(
                currentInnings = 2,
                in2OpeningStrikerId = openingStrikerId,
                in2OpeningNonStrikerId = openingNonStrikerId,
                in2OpeningBowlerId = openingBowlerId
            )
            repository.updateMatch(updated)
        }
    }

    // Saves a single ball when the user taps 0, 1, 2, 3, 4, 6, Wide, No-Ball, Bye, Leg-Bye, or Wicket
    fun recordDelivery(
        runsOffBat: Int,
        extraType: String = "NONE",
        extraRuns: Int = 0,
        isWicket: Boolean = false,
        dismissalType: String = "NONE",
        dismissedPlayerId: Long = 0L,
        nextBatterId: Long = 0L
    ) {
        val active = uiState.value.activeMatch ?: return
        val match = active.match
        if (match.isCompleted) return

        val currentInningsObj = if (match.currentInnings == 1) active.innings1 else active.innings2
        if (currentInningsObj == null || currentInningsObj.isCompleted) return
        if (currentInningsObj.needsNextBowler) {
            statusMessageFlow.value = "Please select the next bowler for the new over first."
            return
        }

        val currentDeliveries = if (match.currentInnings == 1) {
            active.deliveriesInnings1
        } else {
            active.deliveriesInnings2
        }

        // Give this ball the next number in line (1, 2, 3...)
        val nextSequence = (currentDeliveries.maxOfOrNull { it.sequenceNumber } ?: 0) + 1

        val newDelivery = DeliveryEntity(
            matchId = match.id,
            inningsNumber = match.currentInnings,
            sequenceNumber = nextSequence,
            strikerId = currentInningsObj.currentStrikerId,
            nonStrikerId = currentInningsObj.currentNonStrikerId,
            bowlerId = currentInningsObj.currentBowlerId,
            runsOffBat = runsOffBat,
            extraType = extraType,
            extraRuns = extraRuns,
            isWicket = isWicket,
            dismissalType = dismissalType,
            dismissedPlayerId = dismissedPlayerId,
            nextBatterId = nextBatterId
        )

        viewModelScope.launch {
            repository.insertDelivery(newDelivery)
            checkAndSaveMatchCompletion(
                active = active,
                updatedInningsDeliveries = currentDeliveries + newDelivery
            )
        }
    }

    // Saves who will bowl the next over when an over of 6 legal balls finishes
    fun selectNextOverBowler(nextBowlerId: Long) {
        val active = uiState.value.activeMatch ?: return
        val match = active.match
        val currentDeliveries = if (match.currentInnings == 1) {
            active.deliveriesInnings1
        } else {
            active.deliveriesInnings2
        }
        val lastDelivery = currentDeliveries.maxByOrNull { it.sequenceNumber } ?: return
        viewModelScope.launch {
            repository.updateDelivery(lastDelivery.copy(nextOverBowlerId = nextBowlerId))
        }
    }

    // Deletes the last ball bowled in case the scorer tapped the wrong button by mistake
    fun undoLastDelivery() {
        val active = uiState.value.activeMatch ?: return
        val match = active.match
        val currentDeliveries = if (match.currentInnings == 1) {
            active.deliveriesInnings1
        } else {
            active.deliveriesInnings2
        }
        val lastDelivery = currentDeliveries.maxByOrNull { it.sequenceNumber } ?: return
        viewModelScope.launch {
            repository.deleteDelivery(lastDelivery.id)
            if (match.isCompleted) {
                repository.updateMatch(match.copy(isCompleted = false, resultSummary = ""))
            }
            statusMessageFlow.value = "Last delivery undone."
        }
    }

    // Checks if the 2nd Innings just ended, and if so, calculates who won the match and saves the result
    private suspend fun checkAndSaveMatchCompletion(
        active: ActiveMatchDetail,
        updatedInningsDeliveries: List<DeliveryEntity>
    ) {
        val match = active.match
        if (match.currentInnings == 2) {
            val target = active.innings1.totalRuns + 1
            val updatedInnings2 = reconstructInnings(
                inningsNumber = 2,
                battingTeamName = active.battingSecondTeam.name,
                bowlingTeamName = active.battingFirstTeam.name,
                battingPlayers = active.battingSecondPlayers,
                bowlingPlayers = active.battingFirstPlayers,
                openingStrikerId = match.in2OpeningStrikerId,
                openingNonStrikerId = match.in2OpeningNonStrikerId,
                openingBowlerId = match.in2OpeningBowlerId,
                maxOvers = match.totalOvers,
                targetRuns = target,
                deliveries = updatedInningsDeliveries
            )

            if (updatedInnings2.isCompleted) {
                val maxWickets = (active.battingSecondPlayers.size - 1).coerceAtLeast(1)
                val wicketsRemaining = (maxWickets - updatedInnings2.wickets).coerceAtLeast(1)
                val resultText = when {
                    updatedInnings2.totalRuns >= target ->
                        "${active.battingSecondTeam.name} won by $wicketsRemaining wicket(s)!"
                    updatedInnings2.totalRuns == active.innings1.totalRuns ->
                        "Match Tied (${updatedInnings2.totalRuns} runs each)!"
                    else -> {
                        val runMargin = active.innings1.totalRuns - updatedInnings2.totalRuns
                        "${active.battingFirstTeam.name} won by $runMargin run(s)!"
                    }
                }
                repository.updateMatch(
                    match.copy(isCompleted = true, resultSummary = resultText)
                )
            }
        }
    }

    // Switches which match is loaded in the Live Match tab
    fun selectActiveMatch(matchId: Long?) {
        selectedActiveMatchId.value = matchId
    }

    // Opens or closes the full scorecard screen for a specific match
    fun openScorecard(matchId: Long?) {
        selectedScorecardMatchId.value = matchId
    }
}

// Boilerplate helper required by Android so we can pass our CricketRepository into CricketViewModel
class CricketViewModelFactory(private val repository: CricketRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CricketViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CricketViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
