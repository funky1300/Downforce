# Downforce — Session Summary (copy this into new session)

## Project Overview
- **Name:** Downforce — F1 betting & stats Android app (bagrut project)
- **Student:** Israeli high school student, not very experienced with Android Studio
- **Language:** Java (not Kotlin)
- **Location:** `C:\Users\evyat\AndroidStudioProjects\Downforce`
- **Package:** `com.example.downforce`
- **SDK:** minSdk 30, targetSdk 36, compileSdk 36, AGP 8.13.2

---

## Tech Stack / Dependencies
- Firebase BOM 33.14.0 → Auth, Firestore, Storage
- Google Play Services Maps 19.2.0
- Volley 1.2.1 (API calls)
- Picasso 2.71828 (image loading)
- MaterialComponents theme (`Theme.MaterialComponents.DayNight.DarkActionBar.Bridge`)
- Version catalog at `gradle/libs.versions.toml`
- google-services plugin 4.4.2

## Color Palette
- `primary_dark` = #112D4E (background)
- `secondary_dark` = #3F72AF
- `accent_teal` = #DBE2EF
- `light_gray` = #F9F7F7
- Buttons use `@drawable/button_design` (dark gradient, rounded corners)
- EditTexts use `@drawable/edit_text_bg` (dark rounded box)

---

## Firebase Setup
- **Auth:** Email/Password enabled
- **Firestore** `users` collection — document per user:
  ```
  uid, displayName, email, photoUrl, points (int, starts 0), createdAt (Timestamp)
  ```
- **Storage:** profile pictures at `profile_pictures/{uid}.jpg`
- **Maps API Key** already in `AndroidManifest.xml`

---

## What's Already Built ✅

### Core UI
- Race calendar (main screen) — fetches 2026 F1 races from OpenF1 API (`https://api.openf1.org/v1/meetings?year=2026`)
- Race detail dialog (flag, circuit image, location, date, MAP button)
- Countdown timer to next race
- Google Maps — tapping 📍 MAP opens `MapsActivity` with hardcoded circuit coordinates (pin on map)

### Auth
- `LoginActivity` (launcher) — Firebase email/password, uses `ActivityResultLauncher` for register
- `RegisterActivity` — display name, email, password, profile pic (camera/gallery), saves to Firestore
- Profile pic + display name shown in main screen header bar (top right)
- Sign Out option in the overflow menu → clears back stack → back to login

### Notifications
- `NotificationHelper` — schedules race reminders (10 min before) and post-race "Did you watch?" (2hr after)
- `RaceActionReceiver` — handles Watched / Skipped action buttons
- `NotificationReceiver` — fires scheduled notifications

---

## Key Files
| File | Purpose |
|------|---------|
| `MainActivity.java` | Race calendar, countdown, profile display, menu |
| `LoginActivity.java` | Firebase login |
| `RegisterActivity.java` | Registration + camera/gallery |
| `MapsActivity.java` | Google Maps with hardcoded F1 coords |
| `Race.java` | Race model (id, name, location, startDate, endDate, circuit, flag) |
| `NotificationHelper.java` | Schedule notifications & alarms |
| `RaceActionReceiver.java` | Watched/Skipped broadcast receiver |
| `bet_f1.java` | Betting screen — EXISTS but EMPTY, needs to be built |
| `downforce_stats.java` | Stats screen — EXISTS but EMPTY, needs to be built |
| `activity_main.xml` | Main layout |
| `dialog_race_detail.xml` | Race popup dialog |
| `activity_login.xml` / `activity_register.xml` | Auth screens |
| `activity_maps.xml` | SupportMapFragment layout |
| `AndroidManifest.xml` | Permissions, activities, Maps API key, FileProvider |

---

## Still TODO (from ToDo.md)

### Betting System (next priority)
- [ ] Bet placement screen (`bet_f1` activity) — pick race, predict top 3 drivers
- [ ] Automatic bet resolution via OpenF1 API
- [ ] Points scoring: +10 exact / +7 podium / +5 right driver wrong pos / 0 / −3
- [ ] Season auto-reset on March 1st

### Stats & Leaderboard
- [ ] Personal "Wrapped" stats screen (`downforce_stats` activity)
- [ ] Global leaderboard (all users ranked by points from Firestore)
- [ ] Tap user → view their stats

### Other
- [ ] BootReceiver — reschedule alarms after reboot
- [ ] Post-race notification — trigger bet resolution
- [ ] F1 AI chat screen

---

## Important Notes for Next Session
- User prefers **simple code** — use plain `EditText`, not `TextInputLayout`
- User is doing this for **bagrut** (Israeli matriculation exam) so must follow school requirements
- The project uses `fitsSystemWindows="true"` on ScrollViews to handle Android 15 edge-to-edge enforcement
- `LoginActivity` is the launcher (not `MainActivity`)
- `MainActivity` is not exported (only reachable after login)
- The OpenF1 API returns `meeting_name`, `location`, `date_start`, `date_end`, `country_flag`, `circuit_image`, `circuit_key`
