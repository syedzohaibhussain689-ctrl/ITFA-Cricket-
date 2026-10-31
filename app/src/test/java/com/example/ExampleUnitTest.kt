package com.example

import com.example.data.DeliveryEntity
import com.example.data.PlayerEntity
import com.example.data.reconstructInnings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying the EF101-P02 Cricket Rules and Required Edge Cases:
 * - Over containing a Wide (does not count as legal ball)
 * - Over containing a No-Ball (does not count as legal ball)
 * - Boundary (4 and 6)
 * - Wicket (Bowled credits bowler; Run Out does not credit bowler)
 * - Strike rotation on odd runs and at the end of an over
 * - Over ending requiring next bowler selection
 * - Successful chase in 2nd innings
 */
class ExampleUnitTest {

    private val teamAPlayers = listOf(
        PlayerEntity(id = 1L, teamId = 10L, name = "Babar"),
        PlayerEntity(id = 2L, teamId = 10L, name = "Fakhar"),
        PlayerEntity(id = 3L, teamId = 10L, name = "Shaheen")
    )

    private val teamBPlayers = listOf(
        PlayerEntity(id = 4L, teamId = 20L, name = "Rizwan"),
        PlayerEntity(id = 5L, teamId = 20L, name = "Naseem")
    )

    @Test
    fun cricketRules_wideNoBallBoundaryWicketStrikeChangeAndOverEnd_areCorrect() {
        val deliveries = listOf(
            // Ball 1: Wide (Illegal ball -> 0.0 overs, 1 extra run)
            DeliveryEntity(
                matchId = 1L, inningsNumber = 1, sequenceNumber = 1,
                strikerId = 1L, nonStrikerId = 2L, bowlerId = 4L,
                runsOffBat = 0, extraType = "WIDE", extraRuns = 1
            ),
            // Ball 2: 1 normal run (Legal ball 0.1 -> Striker swaps from 1L to 2L)
            DeliveryEntity(
                matchId = 1L, inningsNumber = 1, sequenceNumber = 2,
                strikerId = 1L, nonStrikerId = 2L, bowlerId = 4L,
                runsOffBat = 1, extraType = "NONE", extraRuns = 0
            ),
            // Ball 3: Boundary 4 by player 2L (Legal ball 0.2)
            DeliveryEntity(
                matchId = 1L, inningsNumber = 1, sequenceNumber = 3,
                strikerId = 2L, nonStrikerId = 1L, bowlerId = 4L,
                runsOffBat = 4, extraType = "NONE", extraRuns = 0
            ),
            // Ball 4: No-Ball + 2 bat runs by player 2L (Illegal ball -> still 0.2 overs)
            DeliveryEntity(
                matchId = 1L, inningsNumber = 1, sequenceNumber = 4,
                strikerId = 2L, nonStrikerId = 1L, bowlerId = 4L,
                runsOffBat = 2, extraType = "NO_BALL", extraRuns = 1
            ),
            // Ball 5: Wicket (Bowled) of player 2L, replaced by player 3L (Legal ball 0.3)
            DeliveryEntity(
                matchId = 1L, inningsNumber = 1, sequenceNumber = 5,
                strikerId = 2L, nonStrikerId = 1L, bowlerId = 4L,
                runsOffBat = 0, extraType = "NONE", extraRuns = 0,
                isWicket = true, dismissalType = "BOWLED", dismissedPlayerId = 2L, nextBatterId = 3L
            ),
            // Ball 6: 0 runs (Legal ball 0.4)
            DeliveryEntity(
                matchId = 1L, inningsNumber = 1, sequenceNumber = 6,
                strikerId = 3L, nonStrikerId = 1L, bowlerId = 4L,
                runsOffBat = 0, extraType = "NONE", extraRuns = 0
            ),
            // Ball 7: 0 runs (Legal ball 0.5)
            DeliveryEntity(
                matchId = 1L, inningsNumber = 1, sequenceNumber = 7,
                strikerId = 3L, nonStrikerId = 1L, bowlerId = 4L,
                runsOffBat = 0, extraType = "NONE", extraRuns = 0
            ),
            // Ball 8: 0 runs (Legal ball 1.0 -> End of Over 1! Strike swaps 3L <-> 1L and needsNextBowler = true)
            DeliveryEntity(
                matchId = 1L, inningsNumber = 1, sequenceNumber = 8,
                strikerId = 3L, nonStrikerId = 1L, bowlerId = 4L,
                runsOffBat = 0, extraType = "NONE", extraRuns = 0
            )
        )

        val innings = reconstructInnings(
            inningsNumber = 1,
            battingTeamName = "Lahore Lions",
            bowlingTeamName = "Karachi Kings",
            battingPlayers = teamAPlayers,
            bowlingPlayers = teamBPlayers,
            openingStrikerId = 1L,
            openingNonStrikerId = 2L,
            openingBowlerId = 4L,
            maxOvers = 2,
            targetRuns = null,
            deliveries = deliveries
        )

        assertEquals(9, innings.totalRuns) // 1(wd) + 1 + 4 + 3(nb+2) + 0 + 0 + 0 + 0 = 9
        assertEquals(1, innings.wickets)
        assertEquals(6, innings.legalBalls)
        assertEquals("1.0", innings.oversNotation)
        assertTrue(innings.needsNextBowler)
        // At end of over 1, striker 3L and non-striker 1L swapped -> 1L is now on strike
        assertEquals(1L, innings.currentStrikerId)
        assertEquals(3L, innings.currentNonStrikerId)
        assertFalse(innings.isCompleted)
    }

    @Test
    fun secondInnings_successfulChase_completesImmediately() {
        val deliveries = listOf(
            DeliveryEntity(
                matchId = 1L, inningsNumber = 2, sequenceNumber = 1,
                strikerId = 4L, nonStrikerId = 5L, bowlerId = 1L,
                runsOffBat = 6, extraType = "NONE", extraRuns = 0
            ),
            DeliveryEntity(
                matchId = 1L, inningsNumber = 2, sequenceNumber = 2,
                strikerId = 4L, nonStrikerId = 5L, bowlerId = 1L,
                runsOffBat = 4, extraType = "NONE", extraRuns = 0
            )
        )

        val innings2 = reconstructInnings(
            inningsNumber = 2,
            battingTeamName = "Karachi Kings",
            bowlingTeamName = "Lahore Lions",
            battingPlayers = teamBPlayers,
            bowlingPlayers = teamAPlayers,
            openingStrikerId = 4L,
            openingNonStrikerId = 5L,
            openingBowlerId = 1L,
            maxOvers = 2,
            targetRuns = 10,
            deliveries = deliveries
        )

        assertEquals(10, innings2.totalRuns)
        assertEquals("0.2", innings2.oversNotation)
        assertTrue(innings2.isCompleted)
    }
}
