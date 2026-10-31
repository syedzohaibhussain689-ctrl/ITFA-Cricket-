package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// This file holds all our data shapes (tables) and the main cricket rule calculator.
// The @Entity tag tells Android Room to create a database table for each of these classes.

// Table 1: Stores a cricket team (like "Lahore Lions")
@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // Unique ID number created automatically
    val name: String                                   // The team's name
)

// Table 2: Stores a single player and links them to their team using teamId
@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // Unique player ID number
    val teamId: Long,                                  // Which team this player belongs to
    val name: String                                   // The player's name
)

// Table 3: Stores the setup info and final result of a match
@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teamAId: Long,                  // ID of Team A
    val teamBId: Long,                  // ID of Team B
    val matchName: String,              // Title of the match
    val venue: String,                  // Where the match is played
    val date: String,                   // Date of the match
    val totalOvers: Int,                // How many overs each team gets to bat
    val battingFirstTeamId: Long,       // Which team is batting in the 1st innings
    val currentInnings: Int = 1,        // Are we currently playing Innings 1 or Innings 2?
    // The 3 starting players for the 1st Innings
    val in1OpeningStrikerId: Long = 0,
    val in1OpeningNonStrikerId: Long = 0,
    val in1OpeningBowlerId: Long = 0,
    // The 3 starting players for the 2nd Innings
    val in2OpeningStrikerId: Long = 0,
    val in2OpeningNonStrikerId: Long = 0,
    val in2OpeningBowlerId: Long = 0,
    val isCompleted: Boolean = false,   // True once the match is completely over
    val resultSummary: String = ""      // Final message like "Karachi Kings won by 3 wickets!"
)

// Table 4: Stores a single ball bowled in the match.
// Why do we save every ball instead of just the final score?
// Because if we have the list of every ball in order, the app can replay them from ball #1
// to figure out the exact score, overs, who is on strike, and every player's stats!
@Entity(tableName = "deliveries")
data class DeliveryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: Long,                        // Which match this ball belongs to
    val inningsNumber: Int,                   // 1 for first innings, 2 for second innings
    val sequenceNumber: Int,                  // Ball order number (1, 2, 3, 4...)
    val strikerId: Long,                      // Who was facing the ball
    val nonStrikerId: Long,                   // Who was standing at the other end
    val bowlerId: Long,                       // Who bowled the ball
    val runsOffBat: Int,                      // Runs hit with the bat (0, 1, 2, 3, 4, or 6)
    val extraType: String,                    // "NONE", "WIDE", "NO_BALL", "BYE", or "LEG_BYE"
    val extraRuns: Int,                       // Extra penalty runs (like 1 for a Wide or No-Ball)
    val isWicket: Boolean = false,            // Did someone get out on this ball?
    val dismissalType: String = "NONE",       // How they got out: "BOWLED", "CAUGHT", "LBW", "STUMPED", "RUN_OUT"
    val dismissedPlayerId: Long = 0,          // Which batter got out
    val nextBatterId: Long = 0,               // Which new batter walked in after the wicket
    val nextOverBowlerId: Long = 0            // If this ball finished an over, who bowls the next over
)

// Holds one batter's scoreboard numbers for an innings
data class BatterInnings(
    val playerId: Long,
    val playerName: String,
    val runs: Int = 0,                        // Total runs scored by this batter
    val ballsFaced: Int = 0,                  // How many balls they faced (Wides don't count!)
    val ones: Int = 0,                        // How many 1s they ran
    val twos: Int = 0,                        // How many 2s they ran
    val threes: Int = 0,                      // How many 3s they ran
    val fours: Int = 0,                       // How many 4s (boundaries) they hit
    val sixes: Int = 0,                       // How many 6s they hit
    val isOut: Boolean = false,               // Are they out?
    val dismissalText: String = "Not Out",    // Shows "Not Out" or how they got out (like "b Naseem")
    val didBat: Boolean = false               // Did they actually come out to bat yet?
)

// Holds one bowler's scoreboard numbers for an innings
data class BowlerSpell(
    val playerId: Long,
    val playerName: String,
    val legalBalls: Int = 0,                  // Good balls bowled (Wides and No-Balls don't count!)
    val runsConceded: Int = 0,                // Runs given away by this bowler
    val wickets: Int = 0                      // Wickets taken by this bowler (Run Outs don't count!)
) {
    // Converts legal balls into cricket overs format.
    // Since 6 legal balls = 1 over, we divide by 6 for full overs and use % 6 for leftover balls.
    // Example: 8 legal balls -> 8 / 6 = 1 over, 8 % 6 = 2 balls -> "1.2"
    val oversNotation: String
        get() = "${legalBalls / 6}.${legalBalls % 6}"
}

// Holds the entire scoreboard for one innings (either Innings 1 or Innings 2)
data class Innings(
    val inningsNumber: Int,
    val battingTeamName: String,
    val bowlingTeamName: String,
    val totalRuns: Int,
    val wickets: Int,
    val legalBalls: Int,
    val maxOvers: Int,
    val targetRuns: Int?,                     // Only used in Innings 2 (Innings 1 score + 1)
    val currentStrikerId: Long,               // Who is on strike right now
    val currentNonStrikerId: Long,            // Who is at the non-striker end right now
    val currentBowlerId: Long,                // Who is bowling right now
    val previousOverBowlerId: Long,           // Who bowled the last over (so they can't bowl 2 in a row)
    val needsNextBowler: Boolean,             // True at the end of an over until a new bowler is picked
    val isCompleted: Boolean,                 // True if overs are done, team is all out, or target is chased
    val completionReason: String,             // Explains why the innings ended
    val extrasWide: Int,
    val extrasNoBall: Int,
    val extrasBye: Int,
    val extrasLegBye: Int,
    val batterStats: List<BatterInnings>,
    val bowlerStats: List<BowlerSpell>,
    val recentBallsDisplay: List<String>      // Short labels for each ball like "1", "4", "Wd", "W"
) {
    // Turns total legal balls into "Overs.Balls" format (like "1.4")
    val oversNotation: String
        get() = "${legalBalls / 6}.${legalBalls % 6}"

    // Adds up all four types of extras
    val totalExtras: Int
        get() = extrasWide + extrasNoBall + extrasBye + extrasLegBye
}

// This is the heart of the app!
// It takes the starting players and the list of balls bowled so far, and loops through
// them one ball at a time to apply all the rules of cricket automatically.
fun reconstructInnings(
    inningsNumber: Int,
    battingTeamName: String,
    bowlingTeamName: String,
    battingPlayers: List<PlayerEntity>,
    bowlingPlayers: List<PlayerEntity>,
    openingStrikerId: Long,
    openingNonStrikerId: Long,
    openingBowlerId: Long,
    maxOvers: Int,
    targetRuns: Int?,
    deliveries: List<DeliveryEntity>
): Innings {
    // Quick lookup table so we can find any player's name from their ID number
    val playerMap = (battingPlayers + bowlingPlayers).associateBy { it.id }

    // Create a blank stat card for every player on the batting team
    val batterMap = linkedMapOf<Long, BatterInnings>()
    for (p in battingPlayers) {
        val isOpening = (p.id == openingStrikerId || p.id == openingNonStrikerId)
        batterMap[p.id] = BatterInnings(
            playerId = p.id,
            playerName = p.name,
            didBat = isOpening
        )
    }

    // Create a blank stat card for every player on the bowling team
    val bowlerMap = linkedMapOf<Long, BowlerSpell>()
    for (p in bowlingPlayers) {
        bowlerMap[p.id] = BowlerSpell(
            playerId = p.id,
            playerName = p.name
        )
    }

    // Start the scoreboard counters at zero
    var totalRuns = 0
    var wickets = 0
    var legalBalls = 0
    var extrasWide = 0
    var extrasNoBall = 0
    var extrasBye = 0
    var extrasLegBye = 0

    // Set the starting striker, non-striker, and bowler
    var strikerId = openingStrikerId
    var nonStrikerId = openingNonStrikerId
    var bowlerId = openingBowlerId
    var previousOverBowlerId = 0L
    var needsNextBowler = false
    val recentBalls = mutableListOf<String>()

    // Make sure we go through the balls in the exact order they were bowled (1, 2, 3...)
    val sortedDeliveries = deliveries.sortedBy { it.sequenceNumber }

    for (d in sortedDeliveries) {
        // Mark the two batters on the field as having batted
        batterMap[d.strikerId]?.let { batterMap[d.strikerId] = it.copy(didBat = true) }
        batterMap[d.nonStrikerId]?.let { batterMap[d.nonStrikerId] = it.copy(didBat = true) }

        val currentBatter = batterMap[d.strikerId] ?: BatterInnings(d.strikerId, "Batter", didBat = true)
        val currentBowler = bowlerMap[d.bowlerId] ?: BowlerSpell(d.bowlerId, "Bowler")

        // STEP 1: Check if this ball counts as a legal ball in the over.
        // Cricket Rule: Wides and No-Balls are illegal deliveries, so they do NOT count toward the 6 balls in an over.
        val isLegalBall = (d.extraType != "WIDE" && d.extraType != "NO_BALL")
        if (isLegalBall) {
            legalBalls += 1
        }

        // STEP 2: Add runs to the team total, the batter, and the bowler.
        val ballTotalRuns = d.runsOffBat + d.extraRuns
        totalRuns += ballTotalRuns

        when (d.extraType) {
            "WIDE" -> {
                // On a Wide, the batter doesn't get a ball faced, and all runs go to Wide extras and hurt the bowler's stats.
                extrasWide += ballTotalRuns
                bowlerMap[d.bowlerId] = currentBowler.copy(
                    runsConceded = currentBowler.runsConceded + ballTotalRuns
                )
            }
            "NO_BALL" -> {
                // On a No-Ball, the 1 penalty run is an extra, but any runs hit with the bat DO count for the batter!
                extrasNoBall += d.extraRuns
                batterMap[d.strikerId] = currentBatter.copy(
                    runs = currentBatter.runs + d.runsOffBat,
                    ballsFaced = currentBatter.ballsFaced + 1,
                    ones = currentBatter.ones + if (d.runsOffBat == 1) 1 else 0,
                    twos = currentBatter.twos + if (d.runsOffBat == 2) 1 else 0,
                    threes = currentBatter.threes + if (d.runsOffBat == 3) 1 else 0,
                    fours = currentBatter.fours + if (d.runsOffBat == 4) 1 else 0,
                    sixes = currentBatter.sixes + if (d.runsOffBat == 6) 1 else 0
                )
                bowlerMap[d.bowlerId] = currentBowler.copy(
                    runsConceded = currentBowler.runsConceded + ballTotalRuns
                )
            }
            "BYE" -> {
                // On a Bye, the ball missed the bat. It counts as a legal ball faced, and runs go to Bye extras (not blamed on the bowler).
                extrasBye += d.extraRuns
                batterMap[d.strikerId] = currentBatter.copy(
                    ballsFaced = currentBatter.ballsFaced + 1
                )
                bowlerMap[d.bowlerId] = currentBowler.copy(
                    legalBalls = currentBowler.legalBalls + 1
                )
            }
            "LEG_BYE" -> {
                // On a Leg-Bye, the ball hit the batter's body/pads. Counts as a legal ball faced, and runs go to Leg-Bye extras.
                extrasLegBye += d.extraRuns
                batterMap[d.strikerId] = currentBatter.copy(
                    ballsFaced = currentBatter.ballsFaced + 1
                )
                bowlerMap[d.bowlerId] = currentBowler.copy(
                    legalBalls = currentBowler.legalBalls + 1
                )
            }
            else -> {
                // Normal ball: add runs and boundaries to the batter, and add legal ball + runs to the bowler.
                batterMap[d.strikerId] = currentBatter.copy(
                    runs = currentBatter.runs + d.runsOffBat,
                    ballsFaced = currentBatter.ballsFaced + 1,
                    ones = currentBatter.ones + if (d.runsOffBat == 1) 1 else 0,
                    twos = currentBatter.twos + if (d.runsOffBat == 2) 1 else 0,
                    threes = currentBatter.threes + if (d.runsOffBat == 3) 1 else 0,
                    fours = currentBatter.fours + if (d.runsOffBat == 4) 1 else 0,
                    sixes = currentBatter.sixes + if (d.runsOffBat == 6) 1 else 0
                )
                bowlerMap[d.bowlerId] = currentBowler.copy(
                    legalBalls = currentBowler.legalBalls + 1,
                    runsConceded = currentBowler.runsConceded + d.runsOffBat
                )
            }
        }

        // STEP 3: Check if a batter got out (Wicket) on this ball.
        // Cricket Rule: Bowled, Caught, LBW, and Stumped give the bowler +1 wicket.
        // Run Out is a team fielding dismissal, so the bowler does NOT get credit for a Run Out wicket.
        if (d.isWicket) {
            wickets += 1
            val bowlerCreditsWicket = d.dismissalType in listOf("BOWLED", "CAUGHT", "LBW", "STUMPED")
            if (bowlerCreditsWicket) {
                val updatedBowler = bowlerMap[d.bowlerId]!!
                bowlerMap[d.bowlerId] = updatedBowler.copy(wickets = updatedBowler.wickets + 1)
            }

            // Write a short note explaining how the batter got out for the scorecard
            val bowlerName = playerMap[d.bowlerId]?.name ?: "Bowler"
            val dismissalDescription = when (d.dismissalType) {
                "BOWLED" -> "b $bowlerName"
                "CAUGHT" -> "c Fielder b $bowlerName"
                "LBW" -> "lbw b $bowlerName"
                "STUMPED" -> "st Keeper b $bowlerName"
                "RUN_OUT" -> "run out"
                else -> "out"
            }

            // Mark the batter as OUT
            val outId = if (d.dismissedPlayerId != 0L) d.dismissedPlayerId else d.strikerId
            batterMap[outId]?.let { outBatter ->
                batterMap[outId] = outBatter.copy(
                    isOut = true,
                    dismissalText = dismissalDescription,
                    didBat = true
                )
            }

            // Send in the next batter to take the place of the batter who got out
            if (d.nextBatterId != 0L) {
                batterMap[d.nextBatterId]?.let { nextB ->
                    batterMap[d.nextBatterId] = nextB.copy(didBat = true)
                }
                if (outId == nonStrikerId) {
                    nonStrikerId = d.nextBatterId
                } else {
                    strikerId = d.nextBatterId
                }
            }
        }

        // STEP 4: Swap striker and non-striker if the batters ran an odd number of runs (1 or 3).
        // Because when batters run 1 or 3 times between the wickets, they end up on opposite sides!
        val runsRanByBatters = when (d.extraType) {
            "WIDE" -> d.runsOffBat
            "BYE", "LEG_BYE" -> d.extraRuns
            else -> d.runsOffBat
        }
        if (runsRanByBatters % 2 == 1) {
            val temp = strikerId
            strikerId = nonStrikerId
            nonStrikerId = temp
        }

        // STEP 5: Check if this ball just finished a 6-ball over.
        // At the end of an over, bowling switches to the other end of the pitch,
        // so the striker and non-striker swap roles, and we ask the scorer to pick the next bowler.
        val isOverEnd = isLegalBall && (legalBalls > 0) && (legalBalls % 6 == 0)
        if (isOverEnd) {
            val temp = strikerId
            strikerId = nonStrikerId
            nonStrikerId = temp

            if (d.nextOverBowlerId != 0L) {
                previousOverBowlerId = d.bowlerId
                bowlerId = d.nextOverBowlerId
                needsNextBowler = false
            } else {
                previousOverBowlerId = d.bowlerId
                needsNextBowler = true
            }
        } else {
            needsNextBowler = false
        }

        // STEP 6: Create a short label for this ball (like "4", "Wd", "Nb", "W") to show in the recent balls bar.
        val badge = buildString {
            if (d.isWicket) append("W")
            when (d.extraType) {
                "WIDE" -> {
                    if (isNotEmpty()) append("+")
                    append(if (d.runsOffBat > 0) "${1 + d.runsOffBat}wd" else "Wd")
                }
                "NO_BALL" -> {
                    if (isNotEmpty()) append("+")
                    append(if (d.runsOffBat > 0) "${d.runsOffBat}+Nb" else "Nb")
                }
                "BYE" -> {
                    if (isNotEmpty()) append("+")
                    append("${d.extraRuns}b")
                }
                "LEG_BYE" -> {
                    if (isNotEmpty()) append("+")
                    append("${d.extraRuns}lb")
                }
                else -> {
                    if (!d.isWicket || d.runsOffBat > 0) {
                        if (isNotEmpty()) append("+")
                        append("${d.runsOffBat}")
                    }
                }
            }
        }
        recentBalls.add(badge)
    }

    // STEP 7: Check if the innings is finished!
    // An innings ends automatically in 3 ways:
    // 1. All overs are bowled (legalBalls == maxOvers * 6)
    // 2. All batters are out except the last lonely partner (wickets == teamSize - 1)
    // 3. In the 2nd innings, the batting team reaches or passes the target score!
    val maxPossibleWickets = (battingPlayers.size - 1).coerceAtLeast(1)
    val allOut = wickets >= maxPossibleWickets
    val oversFinished = legalBalls >= maxOvers * 6
    val targetChased = targetRuns != null && totalRuns >= targetRuns

    val isCompleted = allOut || oversFinished || targetChased
    val completionReason = when {
        targetChased -> "Target of $targetRuns chased successfully!"
        allOut -> "Innings complete (All Out - $wickets wickets)"
        oversFinished -> "Innings complete ($maxOvers overs finished)"
        else -> ""
    }

    // If the innings is already over, we don't need to ask for a next bowler
    val finalNeedsNextBowler = needsNextBowler && !isCompleted

    return Innings(
        inningsNumber = inningsNumber,
        battingTeamName = battingTeamName,
        bowlingTeamName = bowlingTeamName,
        totalRuns = totalRuns,
        wickets = wickets,
        legalBalls = legalBalls,
        maxOvers = maxOvers,
        targetRuns = targetRuns,
        currentStrikerId = strikerId,
        currentNonStrikerId = nonStrikerId,
        currentBowlerId = bowlerId,
        previousOverBowlerId = previousOverBowlerId,
        needsNextBowler = finalNeedsNextBowler,
        isCompleted = isCompleted,
        completionReason = completionReason,
        extrasWide = extrasWide,
        extrasNoBall = extrasNoBall,
        extrasBye = extrasBye,
        extrasLegBye = extrasLegBye,
        batterStats = batterMap.values.toList(),
        bowlerStats = bowlerMap.values.filter {
            it.playerId == bowlerId || it.legalBalls > 0 || it.runsConceded > 0 || it.wickets > 0
        }.ifEmpty { bowlerMap.values.toList() },
        recentBallsDisplay = recentBalls
    )
}
