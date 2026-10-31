package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// DAO stands for "Data Access Object".
// Think of this interface as the menu of commands we use to talk to our phone's SQLite database.
// Room reads these @Query, @Insert, and @Update tags and writes the SQL code for us automatically.
@Dao
interface CricketDao {

    // --- TEAMS ---
    // Get all saved teams. Returning a "Flow" means the screen updates automatically whenever a team is added.
    @Query("SELECT * FROM teams ORDER BY id ASC")
    fun getAllTeams(): Flow<List<TeamEntity>>

    // Count how many teams are in the database so we know if we should add the 2 starter teams on first launch.
    @Query("SELECT COUNT(*) FROM teams")
    suspend fun getTeamCount(): Int

    // Save a new team and return its new ID number.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamEntity): Long

    // --- PLAYERS ---
    // Get all players from all teams.
    @Query("SELECT * FROM players ORDER BY id ASC")
    fun getAllPlayers(): Flow<List<PlayerEntity>>

    // Save a new player to a team.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    // --- MATCHES ---
    // Get all matches, newest first (DESC = descending order).
    @Query("SELECT * FROM matches ORDER BY id DESC")
    fun getAllMatches(): Flow<List<MatchEntity>>

    // Save a brand new match.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    // Update an existing match (like when Innings 2 starts or when the match finishes).
    @Update
    suspend fun updateMatch(match: MatchEntity)

    // --- DELIVERIES (EVERY BALL BOWLED) ---
    // Get all balls bowled in order from first ball to last ball.
    @Query("SELECT * FROM deliveries ORDER BY sequenceNumber ASC")
    fun getAllDeliveries(): Flow<List<DeliveryEntity>>

    // Save a new ball delivery.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: DeliveryEntity): Long

    // Update a ball delivery (used when we pick the next bowler at the end of an over).
    @Update
    suspend fun updateDelivery(delivery: DeliveryEntity)

    // Delete a specific ball (used when the scorer taps the "Undo Ball" button).
    @Query("DELETE FROM deliveries WHERE id = :deliveryId")
    suspend fun deleteDeliveryById(deliveryId: Long)
}
