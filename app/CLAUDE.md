# Downforce — Session Summary

## Project Overview
- **Name:** Downforce — F1 betting & stats Android app (bagrut project)
- **Student:** Israeli high school student, not very experienced with Android Studio
- **Language:** Java (NOT Kotlin)
- **Local path (Linux):** `/home/evyatar/AndroidStudioProjects/Downforce`
- **Local path (Windows):** `C:\Users\evyat\AndroidStudioProjects\Downforce`
- **GitHub:** https://github.com/funky1300/Downforce
- **Package:** `com.example.downforce`
- **SDK:** minSdk 30, targetSdk 36, compileSdk 36, AGP 8.13.2, Java 11

---

## Tech Stack / Dependencies
- Firebase BOM 33.14.0 → Auth, Firestore, Storage
- Google Play Services Maps 19.2.0
- Volley 1.2.1 (all API calls)
- Picasso 2.71828 (image loading)
- Palette 1.0.0 (imported, not yet used)
- MaterialComponents theme (`Theme.Downforce`)
- google-services plugin 4.4.2

## Color Palette
- `primary_dark` = #112D4E (background)
- `secondary_dark` = #3F72AF (header bars)
- `accent_teal` = #DBE2EF (accent text)
- `light_gray` = #F9F7F7 (body text)
- Buttons: `@drawable/button_design`, Cards: `@drawable/rounded_card_bg`
- EditTexts: `@drawable/edit_text_bg`

---

## Firebase Setup
- **Auth:** Email/Password
- **Firestore** `users` collection:
  ```
  uid, displayName, email, photoUrl, points (int), seasonYear (int), createdAt
  ```
- **Storage:** profile pictures at `profile_pictures/{uid}.jpg`
- **Maps API Key** in AndroidManifest.xml: `AIzaSyBDAEX0DFeJ5UiUnhPPiY-q0xcfHsemrKk`

---

## What's Built ✅

### Core UI
- Race calendar (MainActivity) — OpenF1 API `https://api.openf1.org/v1/meetings?year=2026`
- Race detail dialog (flag, circuit image, location, date, MAP button)
- Countdown timer to next race
- Google Maps — MapsActivity with hardcoded coordinates for all 24 F1 circuits
- Profile header (displayName + avatar from Firebase)
- Menu: calendar (refresh), Stats, Bet F1, AI Chat, Sign Out

### Authentication
- LoginActivity (launcher) — Firebase email/password, ActivityResultLauncher for register
- RegisterActivity — camera/gallery profile pic, saves to Firestore + Storage + Auth profile
- Auto-navigate to MainActivity if already signed in

### Notifications
- NotificationHelper — scheduleNotification() + sendImmediateNotification()
- NotificationReceiver — fires scheduled alarms
- RaceActionReceiver — handles Watched/Skipped buttons, saves to SharedPreferences `race_history`
- BootReceiver — re-schedules all future alarms after device reboot (uses goAsync() + Volley)
- Auto-scheduled per race: 10min before (endDate - 10min) and 2hr after (endDate + 2hr)

### Betting (bet_f1.java) ✅
- Loads user points from Firestore
- Fetches next race from OpenF1, fetches drivers from `meeting_key=latest`
- Fallback driver list if API fails
- Position spinner 1st–10th, Championship type spinner
- Hides the "points to bet" EditTexts (scoring is fixed)
- Saves race bet: `/users/{uid}/bets/race_{raceId}`
- Saves champ bet: `/users/{uid}/bets/champ_{type}`
- Loads + displays all bets with Pending/+pts/-pts status

### Stats (downforce_stats.java) ✅
- Driver standings: `https://api.jolpi.ca/ergast/f1/2026/driverstandings.json`
- Constructor standings: `https://api.jolpi.ca/ergast/f1/2026/constructorstandings.json`
- Last race results + fastest laps: `https://api.jolpi.ca/ergast/f1/2026/last/results.json`
- **Leaderboard** (Section 5): queries `/users` ordered by `points` desc, tap any row to see that user's bets in a dialog

### AI Chat (AiChatActivity.java) ✅ — Bagrut Section 9
- Uses **Groq API** (free, no billing needed)
- Groq key: stored locally only — get a free key at console.groq.com
- URL: `https://api.groq.com/openai/v1/chat/completions`
- Model: `llama-3.1-8b-instant`
- Auth: `Authorization: Bearer {key}` header
- Fetches last race result from Jolpica first → injects as context into system prompt
  so AI answers with current 2026 data (not its 2023 training cutoff)
- Layout: `activity_ai_chat.xml`
- Shows "AI-generated — may not be accurate" disclaimer
- Gemini was tried and abandoned (required billing); Groq is the replacement

### Bet Resolution (BetResolver.java) ✅
- Called automatically from `MainActivity.onCreate`
- Fetches all completed 2026 race results from Jolpica (`results.json?limit=100`)
- Queries user's `/bets` subcollection from Firestore
- For each completed race with an unresolved bet → scores it, updates `resolved=true`, `actualPosition`, `pointsAwarded`
- For each completed race with NO bet → creates sentinel doc `race_nobet_r{round}` with `pointsAwarded=-3`
- All writes in one Firestore transaction (atomic), then fires one notification per resolved item
- On error → Toast "Could not resolve bets — check your connection"
- `BetUpdate` inner class has `isChampBet` boolean; `sendNotification` checks it to avoid "finished P0" in champ notifications

### Championship Bet Resolution (BetResolver.java) ✅
- Runs automatically alongside race bet resolution (parallel Firestore transactions)
- `processBets` collects unresolved `champ_` docs → calls `tryResolveChampionshipBets`
- Fetches driver standings; if `round < 24` exits silently (season not over yet)
- If `round == 24`, fetches constructor standings → `applyChampionshipResults`
- Correct champion prediction = **+25 pts**, wrong = **0 pts**
- Fires a notification per resolved championship bet

### Season Auto-Reset (MainActivity.java) ✅
- `checkSeasonReset(uid)` called in `onCreate` before `BetResolver`
- Reads `seasonYear` from user's Firestore doc (null-safe: treats missing as 0)
- If `seasonYear < currentYear` AND `Calendar.MONTH >= Calendar.MARCH` → `performSeasonReset`
  - Batch-deletes all `/bets` subcollection docs
  - Sets `points = 0`, `seasonYear = currentYear` in the same batch
  - Shows Toast "New F1 season! Points reset for {year}"
- If `seasonYear` is missing (new user) → just stamps `seasonYear = currentYear`, no reset

### Personal Wrapped (WrappedActivity.java) ✅
- New activity, accessible via **"My Wrapped"** overflow menu item in all screens
- Layout: `activity_wrapped.xml`
- Data sources:
  - Total points: Firestore user doc `points`
  - Watched / Skipped counts: SharedPreferences `"race_history"` (keys `race_{id}`, values `"watched"` / `"skipped"`)
  - Bets placed: count `race_{id}` docs excluding `race_nobet_*`
  - Bets won: resolved race bets with `pointsAwarded > 0`
  - Win rate: bets won / resolved race bets × 100%
  - Missed races: count `race_nobet_*` docs

---

## Firestore Structure
```
/users/{uid}
  displayName, email, photoUrl, points (int), seasonYear (int), createdAt

/users/{uid}/bets/race_{raceId}
  raceId, raceName, driverName, predictedPosition,
  timestamp, resolved (bool), actualPosition, pointsAwarded

/users/{uid}/bets/race_nobet_r{round}    ← created by BetResolver for missed races
  raceName, driverName="No bet placed", resolved=true, pointsAwarded=-3, timestamp

/users/{uid}/bets/champ_{type}
  champType, predictedWinner, timestamp, resolved (bool), pointsAwarded
```

---

## Points Scoring System ✅ (auto-resolved by BetResolver.java)
```
Driver finished P1  → +10 points
Driver finished P2  → +6 points
Driver finished P3  → +3 points
Driver finished P4  → 0 points
Driver finished P5–P10  → -2 points
Driver finished P11+ or DNF → -4 points
No bet placed for race  → -3 points (sentinel doc created automatically)
```

---

## All TODOs Complete ✅

All three originally planned features are now implemented:
- Season auto-reset ✅
- Personal Wrapped stats screen ✅
- Championship bet resolution ✅

---

## Key Gotchas / Rules

### User preferences
- Use plain `EditText`, NOT `TextInputLayout`
- Keep code simple and readable
- `fitsSystemWindows="true"` on ScrollViews for Android 15 edge-to-edge
- **"do not touch this function!!!!"** comment = EdgeToEdge + WindowInsets setup — NEVER remove it

### XML attributes
- Use `android:textColorHint` NOT `android:hintTextColor` on EditText (hintTextColor is TextInputLayout only)

### API quirks
- Manual +1hr offset in `fetchRacesAPI()` for races before "United States Grand Prix" — don't touch
- Jolpica responses are wrapped: `MRData → StandingsTable → StandingsLists[0] → DriverStandings`
- Jolpica results: `MRData → RaceTable → Races → [array] → Results → [array] → position, Driver.givenName/familyName`
- OpenF1 returns JSON arrays

### Notification ID scheme
- `race.getId() * 10` = pre-race reminder (10 min before endDate)
- `race.getId() * 10 + 1` = post-race "Did you watch?" (2 hrs after endDate)

### Dead code
- `RaceAdapter.java` exists but is unused — MainActivity inflates race cards manually into GridLayout

---

## Bagrut Section Mapping
| Section | Requirement | Status |
|---------|------------|--------|
| Section 6 | AlarmManager + Notifications | ✅ Done |
| Section 6 | API (OpenF1) | ✅ Done |
| Section 7 | ActivityResultLauncher | ✅ Done (login→register) |
| Section 9 | Google Maps | ✅ Done |
| Section 9 | AI API call | ✅ Done (Groq/Llama) |
| Section 10 | BroadcastReceiver | ✅ Done (3 receivers) |
| Section 10 | Camera & Gallery | ✅ Done |
| Firebase | Auth + Firestore + Storage | ✅ Done |

---

## Git / GitHub
- Synced between Windows PC and Linux laptop
- `.idea/` files are NOT committed
- Always Commit and Push after changes, then Git Pull on other machine
