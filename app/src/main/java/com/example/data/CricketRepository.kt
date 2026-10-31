package com.example.data

import kotlinx.coroutines.flow.Flow

// The Repository is a simple helper class that sits between our database (DAO) and our screen logic (ViewModel).
// Having this class keeps our code neat: the ViewModel just asks the Repository to save or load data.
class CricketRepository(private val dao: CricketDao) {

    // Live lists of teams, players, matches, and balls from the phone's database
    val allTeams: Flow<List<TeamEntity>> = dao.getAllTeams()
    val allPlayers: Flow<List<PlayerEntity>> = dao.getAllPlayers()
    val allMatches: Flow<List<MatchEntity>> = dao.getAllMatches()
    val allDeliveries: Flow<List<DeliveryEntity>> = dao.getAllDeliveries()

    // Save a new team name (like "Lahore Lions")
    suspend fun createTeam(name: String): Long {
        return dao.insertTeam(TeamEntity(name = name.trim()))
    }

    // Add a player to a specific team using that team's ID number
    suspend fun addPlayer(teamId: Long, playerName: String): Long {
        return dao.insertPlayer(PlayerEntity(teamId = teamId, name = playerName.trim()))
    }

    // Create a new match in the database
    suspend fun createMatch(match: MatchEntity): Long {
        return dao.insertMatch(match)
    }

    // Save changes to an existing match
    suspend fun updateMatch(match: MatchEntity) {
        dao.updateMatch(match)
    }

    // Save a single ball event
    suspend fun insertDelivery(delivery: DeliveryEntity): Long {
        return dao.insertDelivery(delivery)
    }

    // Update a ball event
    suspend fun updateDelivery(delivery: DeliveryEntity) {
        dao.updateDelivery(delivery)
    }

    // Remove a ball event when the user clicks "Undo Ball"
    suspend fun deleteDelivery(deliveryId: Long) {
        dao.deleteDeliveryById(deliveryId)
    }

    // When the app is opened for the very first time and there are 0 teams saved,
    // we add 2 sample teams with 4 players each so you can test a match right away.
    suspend fun seedDemoDataIfEmpty() {
        if (dao.getTeamCount() == 0) {
            val teamAId = dao.insertTeam(TeamEntity(name = "Lahore Lions"))
            dao.insertPlayer(PlayerEntity(teamId = teamAId, name = "Babar Azam"))
            dao.insertPlayer(PlayerEntity(teamId = teamAId, name = "Shaheen Afridi"))
            dao.insertPlayer(PlayerEntity(teamId = teamAId, name = "Fakhar Zaman"))
            dao.insertPlayer(PlayerEntity(teamId = teamAId, name = "Haris Rauf"))

            val teamBId = dao.insertTeam(TeamEntity(name = "Karachi Kings"))
            dao.insertPlayer(PlayerEntity(teamId = teamBId, name = "Mohammad Rizwan"))
            dao.insertPlayer(PlayerEntity(teamId = teamBId, name = "Naseem Shah"))
            dao.insertPlayer(PlayerEntity(teamId = teamBId, name = "Shadab Khan"))
            dao.insertPlayer(PlayerEntity(teamId = teamBId, name = "Iftikhar Ahmed"))
        }
    }
}
