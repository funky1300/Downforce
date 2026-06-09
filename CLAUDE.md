# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Downforce is an F1 betting & stats Android app written in **Java** (not Kotlin), built as a Bagrut (Israeli high school) project. Package: `com.example.downforce`. GitHub: https://github.com/funky1300/Downforce

## Build Commands

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew build                # Full build (all variants)
./gradlew test                 # Unit tests (src/test — currently stubs only)
./gradlew connectedAndroidTest # Instrumented tests (src/androidTest — stubs only)
./gradlew lint                 # Lint analysis
./gradlew clean                # Clean build outputs
```

SDK: minSdk 30, targetSdk/compileSdk 36, Java 11, AGP 9.0.0.

## Architecture

Single flat package, activity-centric with no fragments and no MVVM/MVP. Firebase and Volley are instantiated directly inside activities/classes — no dependency injection.

**Activities and key classes:**

- `LoginActivity` — launcher; Firebase email/password auth; uses `ActivityResultLauncher` for register flow
- `RegisterActivity` — camera/gallery profile pic → Firebase Auth + Firestore + Storage
- `MainActivity` — race calendar grid (manual `GridLayout` inflation, no RecyclerView), countdown timer to next race, triggers `BetResolver` on `onCreate`
- `bet_f1` — betting UI; reads/writes to Firestore `/users/{uid}/bets/`
- `downforce_stats` — driver/constructor standings + last race results (Jolpica API) + leaderboard (Firestore)
- `AiChatActivity` — Groq API (Llama 3.1) F1 Q&A; injects latest race result as context so the AI has 2026 data
- `MapsActivity` — Google Maps with 24 hardcoded F1 circuit coordinates
- `BetResolver` — called from `MainActivity.onCreate`; fetches completed Jolpica results, scores unresolved bets, creates sentinel docs for missed races, all in one Firestore transaction
- `NotificationHelper` / `NotificationReceiver` — `AlarmManager` scheduling; IDs are `race.getId() * 10` (pre-race) and `race.getId() * 10 + 1` (post-race)
- `RaceActionReceiver` — Watched/Skipped notification buttons → `SharedPreferences` `race_history`
- `BootReceiver` — re-schedules future alarms after reboot using `goAsync()` + Volley

## APIs

| Source | URL | Used for |
|--------|-----|---------|
| OpenF1 | `https://api.openf1.org/v1/meetings?year=2026` | Race calendar |
| Jolpica | `https://api.jolpi.ca/ergast/f1/2026/...` | Standings, results, bet resolution |
| Groq | `https://api.groq.com/openai/v1/chat/completions` | AI chat (model: `llama-3.1-8b-instant`) |

Jolpica responses are deeply nested: `MRData → RaceTable/StandingsTable → Races/StandingsLists[0] → Results/DriverStandings`.
OpenF1 returns plain JSON arrays.
There is a manual +1hr offset in `fetchRacesAPI()` for races before "United States Grand Prix" — do not remove it.

## Firestore Structure

```
/users/{uid}
  displayName, email, photoUrl, points (int), seasonYear (int), createdAt

/users/{uid}/bets/race_{raceId}
  raceId, raceName, driverName, predictedPosition,
  timestamp, resolved (bool), actualPosition, pointsAwarded

/users/{uid}/bets/race_nobet_r{round}   ← BetResolver sentinel for missed races
  raceName, driverName="No bet placed", resolved=true, pointsAwarded=-3

/users/{uid}/bets/champ_{type}
  champType, predictedWinner, timestamp, resolved (bool), pointsAwarded
```

## Points Scoring (BetResolver)

| Outcome | Points |
|---------|--------|
| P1 | +10 |
| P2 | +6 |
| P3 | +3 |
| P4 | 0 |
| P5–P10 | −2 |
| P11+ or DNF | −4 |
| No bet placed | −3 (sentinel doc) |

## Key Rules

- Use plain `EditText`, **not** `TextInputLayout`. Use `android:textColorHint` (not `android:hintTextColor`).
- `fitsSystemWindows="true"` on ScrollViews is required for Android 15 edge-to-edge.
- The `EdgeToEdge` + `WindowInsets` setup block in `MainActivity` is marked `"do not touch this function!!!!"` — never remove it.
- Groq API key is stored locally only (not in version control). Get a free key at console.groq.com.

## Season Auto-Reset (`MainActivity`)

Called from `onCreate` via `checkSeasonReset(uid)`. Fetches the user's `seasonYear` from Firestore:
- If `seasonYear < currentYear` AND month ≥ March → `performSeasonReset`: batch-deletes all `/bets` docs, sets `points = 0`, `seasonYear = currentYear`, shows a toast.
- If `seasonYear` is missing (new user) → just stamps `seasonYear = currentYear`, no reset.

## Personal Wrapped (`WrappedActivity`)

New activity, accessible via **"My Wrapped"** in the overflow menu of every screen.
- Points: from Firestore user doc.
- Watched / Skipped: from SharedPreferences `race_history` (keys `race_{id}`, values `"watched"` / `"skipped"`).
- Bets placed, bets won, win rate, missed races: from Firestore bets subcollection (`race_` docs excluding `race_nobet_*`).

## Championship Bet Resolution (`BetResolver`)

Runs automatically on every launch alongside race bet resolution.
- `processBets` collects unresolved `champ_` docs and calls `tryResolveChampionshipBets`.
- Fetches driver standings → if `round < 24`, exits silently (season not over).
- If `round == 24`, also fetches constructor standings → calls `applyChampionshipResults`.
- Correct champion = **+25 pts**, wrong = **0 pts**.
- Uses a separate Firestore transaction from race bets (safe due to Firestore retry-on-conflict).
- `BetUpdate` has an `isChampBet` boolean; `sendNotification` formats champ notifications differently (no "finished P0").
