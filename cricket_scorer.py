"""
EF101-P02: Cricket Match Scoring Application (Python + Local SQLite Edition)
----------------------------------------------------------------------------
This Python program implements every mandatory requirement of EF101-P02 using
simple, easy-to-understand Python code and Python's built-in 'sqlite3' local
database (works 100% offline without internet).

Mandatory Requirements Covered:
1. Create a new team and save it locally for later reuse.
2. Add players to a team and retrieve previously saved players.
3. Prevent an incomplete team (< 2 players) from starting a match.
4. Create a match by selecting two teams and entering date, venue/name, overs, and batting team.
5. Select striker, non-striker, and bowler before each innings begins.
6. Ball-by-ball scoring input for normal runs (0, 1, 2, 3, 4, 6).
7. Support extras: Wide, No-Ball, Bye, and Leg-Bye.
8. Wide and No-Ball deliveries do NOT count as legal balls in the over.
9. Automatically count legal balls and convert them into cricket over notation (0.1 -> 0.5 -> 1.0).
10. Automatically change the striker on odd runs (1 or 3) and at the end of an over.
11. Require selection of the next bowler at the beginning of a new over.
12. Track each batter's runs, balls faced, 1s, 2s, 3s, 4s, and 6s.
13. Track each bowler's overs, legal balls, runs conceded, and wickets.
14. Support wickets: Bowled, Caught, LBW, Stumped (credit bowler +1 wicket) and Run Out (no bowler credit).
15. Automatically determine innings completion through overs, wickets, or chase completion.
16. Display target and final result for a two-innings limited-overs match.
17. Provide a full scorecard (team total, wickets, overs, batter stats, bowler stats, and extras).
18. Save completed matches locally and allow previous match summaries/scorecards to be reopened.
"""

import sqlite3
import sys

# File name for our local offline SQLite database
DB_FILE = "cricket_scorer.db"

# Minimum number of players required per team to start a match
MIN_PLAYERS_PER_TEAM = 2


# =====================================================================
# PART 1: LOCAL DATABASE SETUP (SQLite)
# =====================================================================

def get_connection():
    """Opens a connection to our local SQLite database file on disk."""
    conn = sqlite3.connect(DB_FILE)
    conn.row_factory = sqlite3.Row
    return conn


def setup_database():
    """Creates the 4 database tables (teams, players, matches, deliveries) if they don't exist yet."""
    conn = get_connection()
    cursor = conn.cursor()

    # Table 1: Teams
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS teams (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL
        )
    """)

    # Table 2: Players (linked to a team by team_id)
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS players (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            team_id INTEGER NOT NULL,
            name TEXT NOT NULL
        )
    """)

    # Table 3: Matches
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS matches (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            team_a_id INTEGER NOT NULL,
            team_b_id INTEGER NOT NULL,
            match_name TEXT NOT NULL,
            venue TEXT NOT NULL,
            date TEXT NOT NULL,
            total_overs INTEGER NOT NULL,
            batting_first_team_id INTEGER NOT NULL,
            current_innings INTEGER DEFAULT 1,
            in1_striker_id INTEGER DEFAULT 0,
            in1_non_striker_id INTEGER DEFAULT 0,
            in1_bowler_id INTEGER DEFAULT 0,
            in2_striker_id INTEGER DEFAULT 0,
            in2_non_striker_id INTEGER DEFAULT 0,
            in2_bowler_id INTEGER DEFAULT 0,
            is_completed INTEGER DEFAULT 0,
            result_summary TEXT DEFAULT ''
        )
    """)

    # Table 4: Deliveries (Every ball bowled in the match)
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS deliveries (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            match_id INTEGER NOT NULL,
            innings_number INTEGER NOT NULL,
            sequence_number INTEGER NOT NULL,
            striker_id INTEGER NOT NULL,
            non_striker_id INTEGER NOT NULL,
            bowler_id INTEGER NOT NULL,
            runs_off_bat INTEGER NOT NULL,
            extra_type TEXT NOT NULL,
            extra_runs INTEGER NOT NULL,
            is_wicket INTEGER DEFAULT 0,
            dismissal_type TEXT DEFAULT 'NONE',
            dismissed_player_id INTEGER DEFAULT 0,
            next_batter_id INTEGER DEFAULT 0,
            next_over_bowler_id INTEGER DEFAULT 0
        )
    """)

    # Add 2 starter teams with 4 players each if the database is brand new
    cursor.execute("SELECT COUNT(*) FROM teams")
    team_count = cursor.fetchone()[0]
    if team_count == 0:
        cursor.execute("INSERT INTO teams (name) VALUES (?)", ("Lahore Lions",))
        team_a_id = cursor.lastrowid
        for player_name in ["Babar Azam", "Shaheen Afridi", "Fakhar Zaman", "Haris Rauf"]:
            cursor.execute("INSERT INTO players (team_id, name) VALUES (?, ?)", (team_a_id, player_name))

        cursor.execute("INSERT INTO teams (name) VALUES (?)", ("Karachi Kings",))
        team_b_id = cursor.lastrowid
        for player_name in ["Mohammad Rizwan", "Naseem Shah", "Shadab Khan", "Iftikhar Ahmed"]:
            cursor.execute("INSERT INTO players (team_id, name) VALUES (?, ?)", (team_b_id, player_name))

    conn.commit()
    conn.close()


# =====================================================================
# PART 2: CRICKET RULE ENGINE (Reconstructs an Innings from Deliveries)
# =====================================================================

def format_overs(legal_balls):
    """
    Turns total legal balls into cricket over notation.
    6 legal balls = 1 over.
    Example: 8 legal balls -> 8 // 6 = 1 over, 8 % 6 = 2 balls -> '1.2'
    """
    overs = legal_balls // 6
    balls = legal_balls % 6
    return f"{overs}.{balls}"


def reconstruct_innings(
    innings_number,
    batting_team_name,
    bowling_team_name,
    batting_players,
    bowling_players,
    opening_striker_id,
    opening_non_striker_id,
    opening_bowler_id,
    max_overs,
    target_runs,
    deliveries
):
    """
    Loops through every ball bowled in the innings one by one and calculates:
    - Team total runs, wickets, legal balls, and overs
    - Each batter's runs, balls faced, 1s, 2s, 3s, 4s, 6s, and out/not-out status
    - Each bowler's overs, legal balls, runs conceded, and wickets
    - Automatic strike changes on odd runs (1, 3) and at the end of every over
    - Automatic innings completion (overs finished, all out, or target chased)
    """
    # Map player ID -> player name for quick lookup
    player_names = {}
    for p in batting_players + bowling_players:
        player_names[p["id"]] = p["name"]

    # Create a blank stat dictionary for each batter
    batter_stats = {}
    for p in batting_players:
        is_opening = (p["id"] == opening_striker_id or p["id"] == opening_non_striker_id)
        batter_stats[p["id"]] = {
            "id": p["id"],
            "name": p["name"],
            "runs": 0,
            "balls": 0,
            "ones": 0,
            "twos": 0,
            "threes": 0,
            "fours": 0,
            "sixes": 0,
            "is_out": False,
            "status": "not out" if is_opening else "did not bat"
        }

    # Create a blank stat dictionary for each bowler
    bowler_stats = {}
    for p in bowling_players:
        bowler_stats[p["id"]] = {
            "id": p["id"],
            "name": p["name"],
            "legal_balls": 0,
            "runs_conceded": 0,
            "wickets": 0
        }

    # Scoreboard counters start at 0
    total_runs = 0
    wickets = 0
    legal_balls = 0
    extras_wide = 0
    extras_no_ball = 0
    extras_bye = 0
    extras_leg_bye = 0

    striker_id = opening_striker_id
    non_striker_id = opening_non_striker_id
    bowler_id = opening_bowler_id
    previous_over_bowler_id = 0
    needs_next_bowler = False
    ball_log = []

    # Process every delivery in order
    for d in deliveries:
        s_id = d["striker_id"]
        ns_id = d["non_striker_id"]
        bw_id = d["bowler_id"]
        runs_bat = d["runs_off_bat"]
        extra_type = d["extra_type"]
        extra_runs = d["extra_runs"]

        if batter_stats[s_id]["status"] == "did not bat":
            batter_stats[s_id]["status"] = "not out"
        if batter_stats[ns_id]["status"] == "did not bat":
            batter_stats[ns_id]["status"] = "not out"

        # 1. Check if the ball is a legal delivery.
        # Rule: Wide and No-Ball do NOT count as legal balls in the over.
        is_legal_ball = extra_type not in ("WIDE", "NO_BALL")
        if is_legal_ball:
            legal_balls += 1

        # 2. Add runs to the team, batter, and bowler
        ball_total = runs_bat + extra_runs
        total_runs += ball_total

        if extra_type == "WIDE":
            # Wide: not a ball faced by batter; all runs go to Wide extras and bowler's runs conceded
            extras_wide += ball_total
            bowler_stats[bw_id]["runs_conceded"] += ball_total

        elif extra_type == "NO_BALL":
            # No-Ball: 1 penalty extra run, plus any runs hit off the bat count for the batter
            extras_no_ball += extra_runs
            batter_stats[s_id]["runs"] += runs_bat
            batter_stats[s_id]["balls"] += 1
            if runs_bat == 1:
                batter_stats[s_id]["ones"] += 1
            elif runs_bat == 2:
                batter_stats[s_id]["twos"] += 1
            elif runs_bat == 3:
                batter_stats[s_id]["threes"] += 1
            elif runs_bat == 4:
                batter_stats[s_id]["fours"] += 1
            elif runs_bat == 6:
                batter_stats[s_id]["sixes"] += 1
            bowler_stats[bw_id]["runs_conceded"] += ball_total

        elif extra_type == "BYE":
            # Bye: legal ball faced by batter, runs added to Bye extras (not blamed on bowler)
            extras_bye += extra_runs
            batter_stats[s_id]["balls"] += 1
            bowler_stats[bw_id]["legal_balls"] += 1

        elif extra_type == "LEG_BYE":
            # Leg-Bye: legal ball faced by batter, runs added to Leg-Bye extras
            extras_leg_bye += extra_runs
            batter_stats[s_id]["balls"] += 1
            bowler_stats[bw_id]["legal_balls"] += 1

        else:
            # Normal legal ball
            batter_stats[s_id]["runs"] += runs_bat
            batter_stats[s_id]["balls"] += 1
            if runs_bat == 1:
                batter_stats[s_id]["ones"] += 1
            elif runs_bat == 2:
                batter_stats[s_id]["twos"] += 1
            elif runs_bat == 3:
                batter_stats[s_id]["threes"] += 1
            elif runs_bat == 4:
                batter_stats[s_id]["fours"] += 1
            elif runs_bat == 6:
                batter_stats[s_id]["sixes"] += 1
            bowler_stats[bw_id]["legal_balls"] += 1
            bowler_stats[bw_id]["runs_conceded"] += runs_bat

        # 3. Check for Wickets
        # Documented Rule: Bowled, Caught, LBW, Stumped credit the bowler (+1 wicket).
        # Run Out does NOT credit the bowler.
        if d["is_wicket"]:
            wickets += 1
            d_type = d["dismissal_type"]
            if d_type in ("BOWLED", "CAUGHT", "LBW", "STUMPED"):
                bowler_stats[bw_id]["wickets"] += 1

            bowler_name = player_names.get(bw_id, "Bowler")
            out_id = d["dismissed_player_id"] if d["dismissed_player_id"] != 0 else s_id
            batter_stats[out_id]["is_out"] = True

            if d_type == "BOWLED":
                batter_stats[out_id]["status"] = f"b {bowler_name}"
            elif d_type == "CAUGHT":
                batter_stats[out_id]["status"] = f"c Fielder b {bowler_name}"
            elif d_type == "LBW":
                batter_stats[out_id]["status"] = f"lbw b {bowler_name}"
            elif d_type == "STUMPED":
                batter_stats[out_id]["status"] = f"st Keeper b {bowler_name}"
            elif d_type == "RUN_OUT":
                batter_stats[out_id]["status"] = "run out"

            # Send in the replacement batter if one was selected
            next_b_id = d["next_batter_id"]
            if next_b_id != 0 and next_b_id in batter_stats:
                batter_stats[next_b_id]["status"] = "not out"
                if out_id == non_striker_id:
                    non_striker_id = next_b_id
                else:
                    striker_id = next_b_id

        # 4. Strike Rotation Rule: Swap batters on odd runs (1 or 3)
        runs_ran = extra_runs if extra_type in ("BYE", "LEG_BYE") else runs_bat
        if runs_ran % 2 == 1:
            striker_id, non_striker_id = non_striker_id, striker_id

        # 5. End of Over Rule: Every 6 legal balls, swap ends and ask for the next bowler
        if is_legal_ball and legal_balls > 0 and (legal_balls % 6 == 0):
            striker_id, non_striker_id = non_striker_id, striker_id
            previous_over_bowler_id = bw_id
            if d["next_over_bowler_id"] != 0:
                bowler_id = d["next_over_bowler_id"]
                needs_next_bowler = False
            else:
                needs_next_bowler = True
        else:
            needs_next_bowler = False

        # 6. Record a short badge for the ball log
        if d["is_wicket"]:
            ball_log.append(f"W({d['dismissal_type']})")
        elif extra_type != "NONE":
            ball_log.append(f"{extra_type}+{ball_total}")
        else:
            ball_log.append(str(runs_bat))

    # 7. Check if the innings is complete
    max_wickets = max(1, len(batting_players) - 1)
    all_out = wickets >= max_wickets
    overs_finished = legal_balls >= (max_overs * 6)
    target_chased = (target_runs is not None) and (total_runs >= target_runs)

    is_completed = all_out or overs_finished or target_chased
    if target_chased:
        completion_reason = f"Target of {target_runs} chased successfully!"
    elif all_out:
        completion_reason = f"All Out ({wickets} wickets)"
    elif overs_finished:
        completion_reason = f"Overs completed ({max_overs} overs)"
    else:
        completion_reason = ""

    return {
        "innings_number": innings_number,
        "batting_team_name": batting_team_name,
        "bowling_team_name": bowling_team_name,
        "total_runs": total_runs,
        "wickets": wickets,
        "legal_balls": legal_balls,
        "overs_notation": format_overs(legal_balls),
        "max_overs": max_overs,
        "target_runs": target_runs,
        "striker_id": striker_id,
        "non_striker_id": non_striker_id,
        "bowler_id": bowler_id,
        "previous_over_bowler_id": previous_over_bowler_id,
        "needs_next_bowler": needs_next_bowler and not is_completed,
        "is_completed": is_completed,
        "completion_reason": completion_reason,
        "extras_wide": extras_wide,
        "extras_no_ball": extras_no_ball,
        "extras_bye": extras_bye,
        "extras_leg_bye": extras_leg_bye,
        "total_extras": extras_wide + extras_no_ball + extras_bye + extras_leg_bye,
        "batter_stats": list(batter_stats.values()),
        "bowler_stats": list(bowler_stats.values()),
        "ball_log": ball_log
    }


# =====================================================================
# PART 3: SCORECARD PRINTER & SAVED MATCH LOADER
# =====================================================================

def print_innings_scorecard(innings):
    """Prints a clean, easy-to-read cricket scorecard for an innings."""
    print("\n" + "=" * 68)
    print(f" {innings['batting_team_name']} Innings (Innings {innings['innings_number']})")
    print(f" Score: {innings['total_runs']}/{innings['wickets']} in {innings['overs_notation']} / {innings['max_overs']} overs")
    if innings["target_runs"] is not None:
        print(f" Target: {innings['target_runs']} runs")
    print(f" Extras: {innings['total_extras']} (Wd {innings['extras_wide']}, Nb {innings['extras_no_ball']}, B {innings['extras_bye']}, Lb {innings['extras_leg_bye']})")
    print("-" * 68)
    print(f" {'Batter':<20} {'Status':<18} {'R':>3} {'B':>3} {'1s':>3} {'2s':>3} {'3s':>3} {'4s':>3} {'6s':>3}")
    print("-" * 68)
    for b in innings["batter_stats"]:
        star = "*" if (not b["is_out"] and b["id"] == innings["striker_id"]) else ""
        name_display = f"{b['name']}{star}"
        print(f" {name_display:<20} {b['status']:<18} {b['runs']:>3} {b['balls']:>3} {b['ones']:>3} {b['twos']:>3} {b['threes']:>3} {b['fours']:>3} {b['sixes']:>3}")

    print("-" * 68)
    print(f" {'Bowler':<22} {'Overs':>7} {'Legal Balls':>13} {'Runs':>8} {'Wickets':>9}")
    print("-" * 68)
    for bw in innings["bowler_stats"]:
        if bw["legal_balls"] > 0 or bw["runs_conceded"] > 0 or bw["wickets"] > 0 or bw["id"] == innings["bowler_id"]:
            ov = format_overs(bw["legal_balls"])
            print(f" {bw['name']:<22} {ov:>7} {bw['legal_balls']:>13} {bw['runs_conceded']:>8} {bw['wickets']:>9}")
    print("-" * 68)
    print(" Ball-by-Ball Log:", " -> ".join(innings["ball_log"]) if innings["ball_log"] else "No balls yet")
    print("=" * 68)


def load_and_print_match_scorecard(match_id):
    """Loads a saved match from SQLite and prints its full 1st and 2nd Innings scorecards."""
    conn = get_connection()
    cur = conn.cursor()
    match_row = cur.execute("SELECT * FROM matches WHERE id = ?", (match_id,)).fetchone()
    if not match_row:
        print(f"No match found with ID {match_id}.")
        conn.close()
        return

    match = dict(match_row)
    team_a = dict(cur.execute("SELECT * FROM teams WHERE id = ?", (match["team_a_id"],)).fetchone())
    team_b = dict(cur.execute("SELECT * FROM teams WHERE id = ?", (match["team_b_id"],)).fetchone())

    bat_first = team_a if match["batting_first_team_id"] == team_a["id"] else team_b
    bat_second = team_b if match["batting_first_team_id"] == team_a["id"] else team_a

    players_first = [dict(r) for r in cur.execute("SELECT * FROM players WHERE team_id = ?", (bat_first["id"],)).fetchall()]
    players_second = [dict(r) for r in cur.execute("SELECT * FROM players WHERE team_id = ?", (bat_second["id"],)).fetchall()]

    deliv_1 = [dict(r) for r in cur.execute("SELECT * FROM deliveries WHERE match_id = ? AND innings_number = 1 ORDER BY sequence_number", (match_id,)).fetchall()]
    deliv_2 = [dict(r) for r in cur.execute("SELECT * FROM deliveries WHERE match_id = ? AND innings_number = 2 ORDER BY sequence_number", (match_id,)).fetchall()]
    conn.close()

    print(f"\nMATCH: {match['match_name']} | Venue: {match['venue']} | Date: {match['date']}")
    innings_1 = reconstruct_innings(
        1, bat_first["name"], bat_second["name"], players_first, players_second,
        match["in1_striker_id"], match["in1_non_striker_id"], match["in1_bowler_id"],
        match["total_overs"], None, deliv_1
    )
    print_innings_scorecard(innings_1)

    if match["current_innings"] == 2 and match["in2_striker_id"] != 0:
        target = innings_1["total_runs"] + 1
        innings_2 = reconstruct_innings(
            2, bat_second["name"], bat_first["name"], players_second, players_first,
            match["in2_striker_id"], match["in2_non_striker_id"], match["in2_bowler_id"],
            match["total_overs"], target, deliv_2
        )
        print_innings_scorecard(innings_2)

    if match["result_summary"]:
        print(f"\nRESULT: {match['result_summary']}\n")


# =====================================================================
# PART 4: INTERACTIVE BALL-BY-BALL MATCH SCORER
# =====================================================================

def choose_player(prompt_text, players, exclude_ids=None):
    """Helper to display a list of players and let the user pick one by ID."""
    exclude_ids = exclude_ids or []
    available = [p for p in players if p["id"] not in exclude_ids]
    print(prompt_text)
    for p in available:
        print(f"  [{p['id']}] {p['name']}")
    valid_ids = {p["id"] for p in available}
    while True:
        choice = input("Enter Player ID: ").strip()
        if choice.isdigit() and int(choice) in valid_ids:
            return int(choice)
        print("Invalid choice. Please pick one of the listed Player IDs.")


def score_innings_interactively(conn, match_id, innings_num, bat_team, bowl_team, bat_players, bowl_players, s_id, ns_id, bw_id, max_overs, target):
    """Runs the ball-by-ball interactive loop for one innings until completion."""
    cur = conn.cursor()
    while True:
        deliveries = [dict(r) for r in cur.execute(
            "SELECT * FROM deliveries WHERE match_id = ? AND innings_number = ? ORDER BY sequence_number",
            (match_id, innings_num)
        ).fetchall()]

        state = reconstruct_innings(
            innings_num, bat_team["name"], bowl_team["name"], bat_players, bowl_players,
            s_id, ns_id, bw_id, max_overs, target, deliveries
        )
        print_innings_scorecard(state)

        if state["is_completed"]:
            print(f"\n*** Innings {innings_num} Finished: {state['completion_reason']} ***")
            return state

        # If an over just ended, require selecting the next bowler
        if state["needs_next_bowler"]:
            print(f"\n--- End of Over {state['legal_balls'] // 6}! Select Next Bowler ---")
            next_bw = choose_player(
                "Choose bowler for the next over:",
                bowl_players,
                exclude_ids=[state["previous_over_bowler_id"]] if len(bowl_players) > 1 else []
            )
            last_deliv_id = deliveries[-1]["id"]
            cur.execute("UPDATE deliveries SET next_over_bowler_id = ? WHERE id = ?", (next_bw, last_deliv_id))
            conn.commit()
            continue

        print("\nBall Input: [0, 1, 2, 3, 4, 6] Normal Runs | [W] Wide | [N] No-Ball | [B] Bye | [L] Leg-Bye | [O] Out/Wicket | [U] Undo")
        action = input("Enter ball event: ").strip().upper()
        next_seq = (deliveries[-1]["sequence_number"] + 1) if deliveries else 1

        if action in ("0", "1", "2", "3", "4", "6"):
            cur.execute("""
                INSERT INTO deliveries (match_id, innings_number, sequence_number, striker_id, non_striker_id, bowler_id, runs_off_bat, extra_type, extra_runs)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'NONE', 0)
            """, (match_id, innings_num, next_seq, state["striker_id"], state["non_striker_id"], state["bowler_id"], int(action)))
            conn.commit()

        elif action == "W":
            add_runs = input("Additional runs ran on Wide (0-4, default 0): ").strip()
            r_bat = int(add_runs) if add_runs.isdigit() else 0
            cur.execute("""
                INSERT INTO deliveries (match_id, innings_number, sequence_number, striker_id, non_striker_id, bowler_id, runs_off_bat, extra_type, extra_runs)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'WIDE', 1)
            """, (match_id, innings_num, next_seq, state["striker_id"], state["non_striker_id"], state["bowler_id"], r_bat))
            conn.commit()

        elif action == "N":
            bat_hit = input("Runs hit off bat on No-Ball (0, 1, 2, 3, 4, 6, default 0): ").strip()
            r_bat = int(bat_hit) if bat_hit in ("0", "1", "2", "3", "4", "6") else 0
            cur.execute("""
                INSERT INTO deliveries (match_id, innings_number, sequence_number, striker_id, non_striker_id, bowler_id, runs_off_bat, extra_type, extra_runs)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'NO_BALL', 1)
            """, (match_id, innings_num, next_seq, state["striker_id"], state["non_striker_id"], state["bowler_id"], r_bat))
            conn.commit()

        elif action in ("B", "L"):
            ex_type = "BYE" if action == "B" else "LEG_BYE"
            ex_r = input(f"Number of {ex_type} runs (1-4, default 1): ").strip()
            runs_val = int(ex_r) if ex_r in ("1", "2", "3", "4") else 1
            cur.execute("""
                INSERT INTO deliveries (match_id, innings_number, sequence_number, striker_id, non_striker_id, bowler_id, runs_off_bat, extra_type, extra_runs)
                VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?)
            """, (match_id, innings_num, next_seq, state["striker_id"], state["non_striker_id"], state["bowler_id"], ex_type, runs_val))
            conn.commit()

        elif action == "O":
            print("Dismissal Types: [1] BOWLED  [2] CAUGHT  [3] LBW  [4] STUMPED  [5] RUN_OUT")
            d_choice = input("Pick dismissal type (1-5): ").strip()
            d_map = {"1": "BOWLED", "2": "CAUGHT", "3": "LBW", "4": "STUMPED", "5": "RUN_OUT"}
            d_type = d_map.get(d_choice, "BOWLED")

            dismissed_id = state["striker_id"]
            completed_runs = 0
            if d_type == "RUN_OUT":
                who = input("Who was Run Out? [1] Striker  [2] Non-Striker: ").strip()
                if who == "2":
                    dismissed_id = state["non_striker_id"]
                cr = input("Completed runs before Run Out (0-3, default 0): ").strip()
                completed_runs = int(cr) if cr in ("0", "1", "2", "3") else 0

            # Check if there is a next batter available
            out_ids = {b["id"] for b in state["batter_stats"] if b["is_out"]} | {state["striker_id"], state["non_striker_id"]}
            bench = [p for p in bat_players if p["id"] not in out_ids]
            next_b_id = 0
            if bench:
                next_b_id = choose_player("Select next incoming batter:", bench)

            cur.execute("""
                INSERT INTO deliveries (
                    match_id, innings_number, sequence_number, striker_id, non_striker_id, bowler_id,
                    runs_off_bat, extra_type, extra_runs, is_wicket, dismissal_type, dismissed_player_id, next_batter_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'NONE', 0, 1, ?, ?, ?)
            """, (match_id, innings_num, next_seq, state["striker_id"], state["non_striker_id"], state["bowler_id"], completed_runs, d_type, dismissed_id, next_b_id))
            conn.commit()

        elif action == "U" and deliveries:
            cur.execute("DELETE FROM deliveries WHERE id = ?", (deliveries[-1]["id"],))
            conn.commit()
            print("Last ball undone.")


def start_interactive_match():
    """Walks the user through setting up and scoring a full 2-innings match."""
    conn = get_connection()
    cur = conn.cursor()
    teams = [dict(r) for r in cur.execute("SELECT * FROM teams").fetchall()]
    if len(teams) < 2:
        print("Need at least 2 teams to start a match.")
        conn.close()
        return

    print("\nAvailable Teams:")
    for t in teams:
        cnt = cur.execute("SELECT COUNT(*) FROM players WHERE team_id = ?", (t["id"],)).fetchone()[0]
        print(f"  [{t['id']}] {t['name']} ({cnt} players)")

    team_a_id = int(input("Select Team A ID: ").strip())
    team_b_id = int(input("Select Team B ID: ").strip())
    if team_a_id == team_b_id:
        print("Error: Please select two different teams.")
        conn.close()
        return

    players_a = [dict(r) for r in cur.execute("SELECT * FROM players WHERE team_id = ?", (team_a_id,)).fetchall()]
    players_b = [dict(r) for r in cur.execute("SELECT * FROM players WHERE team_id = ?", (team_b_id,)).fetchall()]

    # Enforce minimum team size rule
    if len(players_a) < MIN_PLAYERS_PER_TEAM or len(players_b) < MIN_PLAYERS_PER_TEAM:
        print(f"Error: Incomplete team! Each team must have at least {MIN_PLAYERS_PER_TEAM} players.")
        conn.close()
        return

    match_name = input("Enter Match Title (default 'EF101 Match'): ").strip() or "EF101 Match"
    venue = input("Enter Venue (default 'University Ground'): ").strip() or "University Ground"
    date = input("Enter Date (default '2026-11-09'): ").strip() or "2026-11-09"
    overs_str = input("Enter Overs per Innings (default 2): ").strip()
    total_overs = int(overs_str) if overs_str.isdigit() and int(overs_str) > 0 else 2

    bat_first_str = input(f"Which team bats first? [{team_a_id}] or [{team_b_id}]: ").strip()
    bat_first_id = team_b_id if bat_first_str == str(team_b_id) else team_a_id

    team_a = next(t for t in teams if t["id"] == team_a_id)
    team_b = next(t for t in teams if t["id"] == team_b_id)
    bat_1_team = team_a if bat_first_id == team_a_id else team_b
    bowl_1_team = team_b if bat_first_id == team_a_id else team_a
    bat_1_players = players_a if bat_first_id == team_a_id else players_b
    bowl_1_players = players_b if bat_first_id == team_a_id else players_a

    print(f"\n--- 1st Innings Opening Selection ({bat_1_team['name']} batting) ---")
    in1_s = choose_player("Select Opening Striker:", bat_1_players)
    in1_ns = choose_player("Select Opening Non-Striker:", bat_1_players, exclude_ids=[in1_s])
    in1_bw = choose_player("Select Opening Bowler:", bowl_1_players)

    cur.execute("""
        INSERT INTO matches (
            team_a_id, team_b_id, match_name, venue, date, total_overs,
            batting_first_team_id, current_innings, in1_striker_id, in1_non_striker_id, in1_bowler_id
        ) VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?)
    """, (team_a_id, team_b_id, match_name, venue, date, total_overs, bat_first_id, in1_s, in1_ns, in1_bw))
    match_id = cur.lastrowid
    conn.commit()

    in1_state = score_innings_interactively(conn, match_id, 1, bat_1_team, bowl_1_team, bat_1_players, bowl_1_players, in1_s, in1_ns, in1_bw, total_overs, None)

    target = in1_state["total_runs"] + 1
    print(f"\n--- 2nd Innings Setup: {bowl_1_team['name']} needs {target} runs to win! ---")
    in2_s = choose_player("Select 2nd Innings Opening Striker:", bowl_1_players)
    in2_ns = choose_player("Select 2nd Innings Opening Non-Striker:", bowl_1_players, exclude_ids=[in2_s])
    in2_bw = choose_player("Select 2nd Innings Opening Bowler:", bat_1_players)

    cur.execute("""
        UPDATE matches SET current_innings = 2, in2_striker_id = ?, in2_non_striker_id = ?, in2_bowler_id = ?
        WHERE id = ?
    """, (in2_s, in2_ns, in2_bw, match_id))
    conn.commit()

    in2_state = score_innings_interactively(conn, match_id, 2, bowl_1_team, bat_1_team, bowl_1_players, bat_1_players, in2_s, in2_ns, in2_bw, total_overs, target)

    if in2_state["total_runs"] >= target:
        wkts_left = max(1, len(bowl_1_players) - 1 - in2_state["wickets"])
        result_text = f"{bowl_1_team['name']} won by {wkts_left} wicket(s)!"
    elif in2_state["total_runs"] == in1_state["total_runs"]:
        result_text = "Match Tied!"
    else:
        diff = in1_state["total_runs"] - in2_state["total_runs"]
        result_text = f"{bat_1_team['name']} won by {diff} run(s)!"

    cur.execute("UPDATE matches SET is_completed = 1, result_summary = ? WHERE id = ?", (result_text, match_id))
    conn.commit()
    conn.close()
    print(f"\n========================================\n MATCH RESULT: {result_text}\n========================================\n")


# =====================================================================
# PART 5: AUTOMATED DEMO OF ALL REQUIRED EF101-P02 EDGE CASES
# =====================================================================

def run_ef101_edge_case_demo():
    """
    Demonstrates all required EF101-P02 features and edge cases automatically:
    - Saved teams & players in SQLite
    - Over containing a Wide and a No-Ball (not counting as legal balls)
    - Boundary (4 and 6)
    - Wicket (Bowled) and replacement batter
    - Strike change on odd runs and at the end of an over
    - Over ending (1.0) and selecting the next bowler
    - Successful chase in the 2nd innings and saving the completed match locally
    """
    setup_database()
    conn = get_connection()
    cur = conn.cursor()

    print("\n--- EF101-P02 PYTHON + SQLITE CRICKET SCORER DEMO ---")
    teams = [dict(row) for row in cur.execute("SELECT * FROM teams").fetchall()]
    team_a, team_b = teams[0], teams[1]

    players_a = [dict(row) for row in cur.execute("SELECT * FROM players WHERE team_id = ?", (team_a["id"],)).fetchall()]
    players_b = [dict(row) for row in cur.execute("SELECT * FROM players WHERE team_id = ?", (team_b["id"],)).fetchall()]

    print(f"Loaded Team A from SQLite: {team_a['name']} ({len(players_a)} players)")
    print(f"Loaded Team B from SQLite: {team_b['name']} ({len(players_b)} players)")

    # Create a 2-over match
    cur.execute("""
        INSERT INTO matches (
            team_a_id, team_b_id, match_name, venue, date, total_overs,
            batting_first_team_id, current_innings,
            in1_striker_id, in1_non_striker_id, in1_bowler_id,
            in2_striker_id, in2_non_striker_id, in2_bowler_id
        ) VALUES (?, ?, ?, ?, ?, ?, ?, 2, ?, ?, ?, ?, ?, ?)
    """, (
        team_a["id"], team_b["id"], "EF101 Demo Match", "University Oval", "2026-11-09", 2,
        team_a["id"],
        players_a[0]["id"], players_a[1]["id"], players_b[0]["id"],
        players_b[0]["id"], players_b[1]["id"], players_a[1]["id"]
    ))
    match_id = cur.lastrowid

    # Record Innings 1 deliveries demonstrating Wide, No-Ball, 1 run (strike change), 4 (boundary), Wicket, and Over End
    in1_balls = [
        (match_id, 1, 1, players_a[0]["id"], players_a[1]["id"], players_b[0]["id"], 0, "WIDE", 1, 0, "NONE", 0, 0, 0),
        (match_id, 1, 2, players_a[0]["id"], players_a[1]["id"], players_b[0]["id"], 1, "NONE", 0, 0, "NONE", 0, 0, 0),
        (match_id, 1, 3, players_a[1]["id"], players_a[0]["id"], players_b[0]["id"], 4, "NONE", 0, 0, "NONE", 0, 0, 0),
        (match_id, 1, 4, players_a[1]["id"], players_a[0]["id"], players_b[0]["id"], 2, "NO_BALL", 1, 0, "NONE", 0, 0, 0),
        (match_id, 1, 5, players_a[1]["id"], players_a[0]["id"], players_b[0]["id"], 0, "NONE", 0, 1, "BOWLED", players_a[1]["id"], players_a[2]["id"], 0),
        (match_id, 1, 6, players_a[2]["id"], players_a[0]["id"], players_b[0]["id"], 0, "NONE", 0, 0, "NONE", 0, 0, 0),
        (match_id, 1, 7, players_a[2]["id"], players_a[0]["id"], players_b[0]["id"], 0, "NONE", 0, 0, "NONE", 0, 0, 0),
        (match_id, 1, 8, players_a[2]["id"], players_a[0]["id"], players_b[0]["id"], 2, "NONE", 0, 0, "NONE", 0, 0, players_b[1]["id"]),
    ]
    cur.executemany("""
        INSERT INTO deliveries (
            match_id, innings_number, sequence_number, striker_id, non_striker_id, bowler_id,
            runs_off_bat, extra_type, extra_runs, is_wicket, dismissal_type,
            dismissed_player_id, next_batter_id, next_over_bowler_id
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, in1_balls)

    # Record Innings 2 deliveries demonstrating a Successful Chase of Target = 12 runs (6 + 1 + 6 = 13)
    in2_balls = [
        (match_id, 2, 1, players_b[0]["id"], players_b[1]["id"], players_a[1]["id"], 6, "NONE", 0, 0, "NONE", 0, 0, 0),
        (match_id, 2, 2, players_b[0]["id"], players_b[1]["id"], players_a[1]["id"], 1, "NONE", 0, 0, "NONE", 0, 0, 0),
        (match_id, 2, 3, players_b[1]["id"], players_b[0]["id"], players_a[1]["id"], 6, "NONE", 0, 0, "NONE", 0, 0, 0),
    ]
    cur.executemany("""
        INSERT INTO deliveries (
            match_id, innings_number, sequence_number, striker_id, non_striker_id, bowler_id,
            runs_off_bat, extra_type, extra_runs, is_wicket, dismissal_type,
            dismissed_player_id, next_batter_id, next_over_bowler_id
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, in2_balls)

    result_text = f"{team_b['name']} won by 3 wickets (Target 12 chased in 0.3 overs)!"
    cur.execute("UPDATE matches SET is_completed = 1, result_summary = ? WHERE id = ?", (result_text, match_id))
    conn.commit()
    conn.close()

    load_and_print_match_scorecard(match_id)


# =====================================================================
# PART 6: MAIN MENU
# =====================================================================

def interactive_menu():
    """Simple interactive console menu for creating teams, adding players, and scoring matches."""
    setup_database()
    while True:
        print("\n=== EF101-P02 CRICKET SCORER (PYTHON + SQLITE) ===")
        print("1. View Saved Teams & Players")
        print("2. Create New Team")
        print("3. Add Player to Team")
        print("4. Start & Score a New Match (Ball-by-Ball)")
        print("5. View & Reopen Saved Match Scorecards")
        print("6. Run Automated EF101-P02 Edge-Case Demo")
        print("0. Exit")
        choice = input("Choose an option: ").strip()

        if choice == "1":
            conn = get_connection()
            teams = conn.execute("SELECT * FROM teams").fetchall()
            for t in teams:
                players = conn.execute("SELECT * FROM players WHERE team_id = ?", (t["id"],)).fetchall()
                p_names = ", ".join(p["name"] for p in players)
                status = "READY" if len(players) >= MIN_PLAYERS_PER_TEAM else "INCOMPLETE"
                print(f"  Team [{t['id']}] {t['name']} ({len(players)} players - {status}): {p_names}")
            conn.close()

        elif choice == "2":
            name = input("Enter new team name: ").strip()
            if name:
                conn = get_connection()
                conn.execute("INSERT INTO teams (name) VALUES (?)", (name,))
                conn.commit()
                conn.close()
                print(f"Saved team '{name}' locally.")

        elif choice == "3":
            team_id = input("Enter Team ID: ").strip()
            player_name = input("Enter Player Name: ").strip()
            if team_id.isdigit() and player_name:
                conn = get_connection()
                conn.execute("INSERT INTO players (team_id, name) VALUES (?, ?)", (int(team_id), player_name))
                conn.commit()
                conn.close()
                print(f"Added player '{player_name}' to Team {team_id}.")

        elif choice == "4":
            start_interactive_match()

        elif choice == "5":
            conn = get_connection()
            matches = conn.execute("SELECT * FROM matches ORDER BY id DESC").fetchall()
            conn.close()
            if not matches:
                print("No saved matches yet.")
            else:
                for m in matches:
                    print(f"  Match [{m['id']}] {m['match_name']} ({m['venue']}, {m['date']}) - Result: {m['result_summary'] or 'In Progress'}")
                m_id = input("Enter Match ID to reopen full scorecard (or Enter to cancel): ").strip()
                if m_id.isdigit():
                    load_and_print_match_scorecard(int(m_id))

        elif choice == "6":
            run_ef101_edge_case_demo()

        elif choice == "0":
            print("Goodbye!")
            break


if __name__ == "__main__":
    if "--demo" in sys.argv:
        run_ef101_edge_case_demo()
    else:
        interactive_menu()
