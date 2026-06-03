# Bet Resolution — Design Spec
Date: 2026-06-03

## Overview
Automatically resolve user race bets when the app opens. For each completed F1 race, score the user's prediction against the actual result and update their points in Firestore. If the user placed no bet for a completed race, deduct 3 points. Send a notification per resolved item.

Championship bets are deferred — they resolve at season end only (not in scope here).

---

## Architecture

One new file: `BetResolver.java`  
One change to existing code: one line added to `MainActivity.onCreate` after user is confirmed signed in.

```java
new BetResolver(this, uid).resolve();
```

No other existing files are modified.

---

## Data Flow

Three chained async steps:

1. **Fetch Jolpica results** — `GET https://api.jolpi.ca/ergast/f1/2026/results.json?limit=100`  
   Returns all completed 2026 races with full finishing order. Only completed races appear here, so this is the authoritative source for "has this race ended?"

2. **Query Firestore bets** — `/users/{uid}/bets`  
   Load all user bet documents into memory.

3. **Process + write** — score each bet, create no-bet penalty records, write everything in one Firestore transaction, fire one notification per resolved item.

---

## Matching

**Race matching:** Bet documents store `raceName` from OpenF1's `meeting_name` (e.g. `"Monaco Grand Prix"`). Jolpica's `raceName` field is the same string. Match case-insensitively with trim.

**Driver matching:** Bet stores `driverName` from OpenF1's `full_name` (e.g. `"Max Verstappen"`). Jolpica returns `givenName + " " + familyName`. Match case-insensitively with trim.

---

## Scoring

| Situation | Points |
|---|---|
| Driver finished P1 | +10 |
| Driver finished P2 | +6 |
| Driver finished P3 | +3 |
| Driver finished P4 | 0 |
| Driver finished P5–P10 | −2 |
| Driver finished P11+ or DNF/not found | −4 |
| No bet placed for race | −3 |

---

## Avoiding Double-Resolution

- **Existing bets:** `resolved=true` flag prevents re-scoring.
- **No-bet records:** A sentinel document named `race_nobet_r{round}` (e.g. `race_nobet_r7`) is created in `/users/{uid}/bets` with `resolved=true, pointsAwarded=-3, driverName="No bet placed"`. Presence of this document means the penalty has already been applied. `loadExistingBets()` in `bet_f1.java` will display it with the red `−3 pts` label automatically (it starts with `race_`).

---

## Firestore Writes

All writes happen in a single `db.runTransaction()`:
- Each resolved bet: update `resolved=true`, `actualPosition`, `pointsAwarded`
- Each no-bet penalty: `set()` the sentinel document
- User doc: `points` incremented by total delta

Atomicity means points and bet statuses are always consistent.

---

## Notifications

One notification per resolved item, fired after the transaction succeeds using `NotificationHelper.sendImmediateNotification()`.

Examples:
- `"Monaco GP resolved — Verstappen finished P1. +10 pts! 🏆"`
- `"Spanish GP — no bet placed. −3 pts"`

Notification IDs: use a hash of the bet document ID to keep them unique.

---

## Error Handling

- API failure or Firestore write failure: show `Toast.makeText(..., "Could not resolve bets — check your connection", Toast.LENGTH_SHORT)`. Bets stay `resolved=false` and are retried next app open.
- Individual bet parse errors (missing field, unexpected JSON): skip that bet silently, continue processing others.

---

## Class Structure

```
BetResolver(Context context, String uid)
  └── resolve()
        ├── fetchJolpikaResults()
        │     └── loadUserBets(races)
        │           └── processBets(races, bets)
        │                 ├── scoreExistingBets()
        │                 ├── findMissingBets()
        │                 └── applyChanges()  // transaction + notify
        └── (on any error) → Toast

  private int calculatePoints(int predicted, int actual)
```

---

## Out of Scope

- Championship bet resolution (season-end only, future task)
- Season auto-reset
- Resolving bets when app is fully closed (WorkManager)
