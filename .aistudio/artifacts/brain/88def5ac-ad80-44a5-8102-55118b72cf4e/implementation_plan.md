# EF101-P02: Cricket Match Scoring Android App

A clean, beginner-friendly **native Android application** built in **Kotlin** with **Jetpack Compose** and **Room SQLite local database** that fulfills every mandatory functional, data/state, and edge-case requirement of the **EF101-P02** course project without needing internet access.

## User Review & Critical Decisions

> [!IMPORTANT]
> Below is the summary of your confirmed preferences from our clarification step, along with the documented cricket dismissal rules required by the project specification.

- **Confirmed Decision — Native Android App**: Built 100% natively in Kotlin using Android Jetpack Compose and Android Room SQLite (not a web app). Code is organized with clear, beginner-friendly comments and straightforward functions so every group member can easily explain it during project demonstration.
- **Confirmed Decision — Minimum Team Size (2 Players)**: A team must have at least **2 players** before it can be selected to start a match. Attempting to start a match with fewer than 2 players on either team shows a clear validation message.
- **Confirmed Decision — Pre-Saved Demo Teams**: On first launch, the local Room database automatically seeds two sample teams (**"Lahore Lions"** and **"Karachi Kings"**, each with 4 players) so you can immediately demonstrate match setup, ball-by-ball scoring, and edge cases, while still allowing you to create new teams and add players anytime.
- **Confirmed Decision — Strictly Mandatory Scope**: Focuses purely on the mandatory requirements and required edge cases from the EF101-P02 specification to keep the code easy to read and understand.
- **Documented Dismissal Rules (Mandatory Group Requirement)**:
  - **Bowled, Caught, LBW, Stumped**: Credited to the bowler (`wickets + 1`). Cannot occur on a No-Ball (except Stumped off a Wide, which credits the bowler). The striker is marked out and replaced by the next available batter.
  - **Run Out**: **Not** credited to the bowler's wicket count. The user selects whether the **Striker** or **Non-Striker** was run out and how many completed runs (0, 1, 2, 3) were scored before the dismissal.

---

## 1. Overview & Core Concept

- **What It Does**: Allows a human scorer to manage local cricket teams and players, configure a two-innings limited-overs match, record every delivery ball-by-ball (normal runs `0, 1, 2, 3, 4, 6`, extras `Wide, No-Ball, Bye, Leg-Bye`, and wickets `Bowled, Caught, LBW, Stumped, Run Out`), and view a live and saved full scorecard.
- **Target Audience / Persona**: EF-101 students demonstrating mobile application state, structured relational/delivery-level storage, and automatic cricket rule enforcement during their project evaluation.
- **Key Value**: Automatically derives innings totals, legal ball & over notation (`0.1` to `0.5` -> `1.0`), automatic strike rotation on odd runs and end of overs, bowler change prompts, innings/chase completion, and full match reconstruction from delivery-level records stored offline in SQLite.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Home & Saved Matches / Teams Tabs**: A simple 3-tab bottom bar (**Teams**, **New Match / Live Scoring**, **Match History**) plus an in-app **Rules & Dismissal Guide** dialog for your project demo.
  2. **Teams & Players Management**: Create a team locally, view saved teams, add players to any team, and inspect the roster count (showing a badge when the minimum 2-player requirement is met).
  3. **Match Setup & Opening Selection**: Pick Team A and Team B (must be two distinct teams with ≥ 2 players each), enter Match Date, Venue/Title, Number of Overs (e.g., 2 overs for quick demo), and Batting First team, then pick the opening **Striker**, **Non-Striker**, and **Opening Bowler**.
  4. **Ball-by-Ball Live Scorer**:
     - Displays the current Innings banner (1st Innings or 2nd Innings with **Target**, **Runs Needed**, and **Balls Remaining**), score (`Runs/Wickets`), overs (`X.Y`), current **Striker*** and **Non-Striker** stats (`R (B), 1s, 2s, 3s, 4s, 6s`), and current **Bowler** spell (`O, Legal Balls, R, W`).
     - Simple tap buttons for Normal Runs (`0`, `1`, `2`, `3`, `4`, `6`), Extras (`Wide`, `No-Ball`, `Bye`, `Leg-Bye`), and `Wicket` (`Bowled`, `Caught`, `LBW`, `Stumped`, `Run Out`), plus an **Undo Last Ball** button that demonstrates why storing individual delivery events makes reconstructing an innings simple.
     - Automatically prompts for the **Next Batter** when a wicket falls (if batters remain) and prompts for the **Next Bowler** at the end of each completed 6-ball over.
  5. **Scorecard & Saved History**: View full 1st and 2nd Innings scorecards (team total, wickets, overs, extras breakdown, batter table with `R, B, 1s, 2s, 3s, 4s, 6s, Status`, and bowler table with `O, Balls, R, W`). Completed matches are saved locally and can be reopened even after closing and restarting the app offline.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Clean, high-contrast "Cricket Pitch & Scoreboard" Material 3 theme designed for effortless readability and straightforward code structure.
  - *Color Palette & Mood*:
    - Primary Turf Green (`#1B5E20`) with crisp White text for scoreboard headers.
    - Secondary Amber Gold (`#F57F17`) for boundary (`4`/`6`) highlights and striker indicators.
    - Error Crimson (`#C62828`) for Wicket buttons and dismissal badges.
    - Light Surface (`#F8FAF8`) and clean elevated white cards (`#FFFFFF`) so all statistics are easy to scan during a classroom presentation.
  - *Typography & Hierarchy*: Standard, clean Material 3 typography using bold tabular numbers for scores and overs, medium labels for buttons, and clear table headers for batter/bowler scorecards.
  - *Component Styling & Layout*: Simple `Scaffold` with a standard Material 3 `NavigationBar`, `Card` containers with `8.dp` / `16.dp` spacing, and minimum `48.dp` touch targets on all scoring buttons.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Reconstructing Innings State from Delivery-Level Records**
  - *Chosen Approach*: Store every ball as a `DeliveryEvent` row in Room SQLite (`inningsNumber`, `overNumber`, `ballInOver`, `strikerId`, `nonStrikerId`, `bowlerId`, `runsOffBat`, `extraType`, `extraRuns`, `isWicket`, `dismissalType`, `dismissedPlayerId`) alongside `Team`, `Player`, and `Match` entities, and compute `BatterInnings` and `BowlerSpell` directly from the delivery list.
  - *Why*: The EF101-P02 prompt specifically states: *"Students should identify entities such as Team, Player, Match, Innings, BatterInnings, BowlerSpell and Delivery/Event. They must understand why a delivery-level record is useful for reconstructing an innings."* Calculating scorecards from the ordered list of deliveries makes rule logic 100% bug-free, crystal clear to explain to your professor, and allows instant **Undo Last Ball** and full match restoration after closing the app.

- **Decision 2: Local Offline Persistence with Android Room SQLite**
  - *Chosen Approach*: Use Jetpack Room (`@Entity`, `@Dao`, `@Database`) stored on the device's internal SQLite storage with zero internet permissions required.
  - *Why*: Meets the mandatory requirement *"works primarily with local storage"* and *"must continue to function without Internet access"* while surviving app restarts.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    UI Layer (Jetpack Compose Screens)                   │
│  ┌──────────────────┐  ┌──────────────────────┐  ┌───────────────────┐  │
│  │   TeamsScreen    │  │   MatchScorerScreen  │  │  History & Full   │  │
│  │ (Create Team &   │  │ (Setup, Ball-by-Ball │  │  ScorecardScreen  │  │
│  │  Add Players)    │  │  Input, Live Rules)  │  │ (Reopen Matches)  │  │
│  └────────▲─────────┘  └──────────▲───────────┘  └─────────▲─────────┘  │
└───────────┼───────────────────────┼────────────────────────┼────────────┘
            │                       │                        │
            └───────────────────────┼────────────────────────┘
                                    │ StateFlow (UI State)
┌───────────────────────────────────▼─────────────────────────────────────┐
│               State & Rule Engine (CricketViewModel)                    │
│  • Validates min team size (>= 2 players)                               │
│  • Applies Cricket Rules per Delivery (Legal balls, Extras, Strike      │
│    rotation on odd runs & over end, Wicket credit, Chase & All-out)     │
│  • Reconstructs Innings, BatterInnings & BowlerSpell from Deliveries    │
└───────────────────────────────────▲─────────────────────────────────────┘
                                    │ Kotlin Coroutines / Flow
┌───────────────────────────────────▼─────────────────────────────────────┐
│               Local Data Layer (Room SQLite Database)                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │  TeamEntity  │  │ PlayerEntity │  │  MatchEntity │  │DeliveryEntity│ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

### Data Model & Entities

1. **`TeamEntity`**: `id`, `name`
2. **`PlayerEntity`**: `id`, `teamId`, `name`
3. **`MatchEntity`**: `id`, `teamAId`, `teamBId`, `date`, `venue`, `totalOvers`, `battingFirstTeamId`, `status` (`IN_PROGRESS`, `COMPLETED`), `resultSummary`
4. **`DeliveryEntity` (`Delivery/Event`)**: `id`, `matchId`, `inningsNumber` (1 or 2), `sequenceNumber`, `strikerId`, `nonStrikerId`, `bowlerId`, `runsOffBat` (0, 1, 2, 3, 4, 6), `extraType` (`NONE`, `WIDE`, `NO_BALL`, `BYE`, `LEG_BYE`), `extraRuns`, `isWicket`, `dismissalType` (`NONE`, `BOWLED`, `CAUGHT`, `LBW`, `STUMPED`, `RUN_OUT`), `dismissedPlayerId`
5. **Derived Domain Models (`Innings`, `BatterInnings`, `BowlerSpell`)**:
   - **`Innings`**: Reconstructed from deliveries for each innings (`totalRuns`, `wickets`, `legalBalls`, `overNotation` e.g. `1.4`, `extrasWide`, `extrasNoBall`, `extrasBye`, `extrasLegBye`, `isCompleted`).
   - **`BatterInnings`**: Tracks each batter's `runs`, `ballsFaced` (excludes Wides), `ones`, `twos`, `threes`, `fours`, `sixes`, `dismissalText`, `isOut`.
   - **`BowlerSpell`**: Tracks each bowler's `legalBalls`, `oversNotation` (`balls / 6` + `.` + `balls % 6`), `runsConceded` (includes bat runs + Wides + No-Balls; excludes Byes and Leg-Byes), and `wickets` (excludes Run Outs).

### Cricket Rules & Interactive State Mapping

- **Legal Ball & Over Counting**:
  - Normal deliveries, Byes, and Leg-Byes increment `legalBalls` by 1.
  - **Wide** and **No-Ball** deliveries add `1 + additional runs` to the score and do **not** count as legal balls in the over.
  - Over notation is computed as `"${legalBalls / 6}.${legalBalls % 6}"`.
- **Automatic Strike Rotation**:
  - Whenever total runs ran on a ball are odd (`1` or `3`), the `strikerId` and `nonStrikerId` automatically swap.
  - When a legal delivery completes an over (`legalBalls > 0 && legalBalls % 6 == 0`), the striker and non-striker automatically swap for the next over, and a modal prompts the scorer to select the **Next Bowler** (preventing the same bowler from bowling two consecutive overs).
- **Wickets & Dismissal Credit**:
  - When a wicket falls, `wickets` increments by 1. If `wickets == teamPlayers.size - 1` (only 1 batter remains without a partner), the innings automatically ends ("All Out"). Otherwise, a dialog prompts the user to select the next incoming batter from the batting team's remaining bench.
- **Innings & Match Completion**:
  - **Innings 1 ends** when `legalBalls == totalOvers * 6` or `wickets == battingTeamSize - 1`. The app displays the **Target (`Innings 1 Runs + 1`)** and prompts to select the opening Striker, Non-Striker, and Bowler for Innings 2.
  - **Innings 2 ends** immediately when `innings2Runs >= target` (Successful Chase!), or when overs are exhausted / all out. The final result (e.g., *"Karachi Kings won by 3 wickets"* or *"Lahore Lions won by 12 runs"* or *"Match Tied"*) is saved to `MatchEntity` and shown on the full Scorecard.
