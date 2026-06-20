package com.example.downforce;

import android.content.Context;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BetResolver — the scoring engine (the most complex class). Not an Activity.
 *
 * PURPOSE (why): automatically settles bets that haven't been scored yet. It
 * compares each prediction to the real race result, awards points, and penalises
 * races the user didn't bet on. Runs on every launch from MainActivity.onCreate.
 *
 * HOW (how):
 *   resolve()       -> fetch all 2026 results from Jolpica (Volley).
 *   parseResults()  -> build a Map<driverName, finishPosition> per race.
 *   loadUserBets()  -> read the user's /bets sub-collection from Firestore.
 *   processBets()   -> for each finished race: if there is an unresolved bet,
 *                      score it; if there is none, create a -3 "sentinel" doc.
 *   applyChanges()  -> commit every update in ONE Firestore transaction (atomic:
 *                      read current points -> update each bet -> add total delta).
 *   tryResolveChampionshipBets() -> only at round 24 (season end): +25 if correct.
 *
 * Data structures: HashMap (driver->pos), List/ArrayList (bets/updates),
 * HashSet (handled races), inner classes CompletedRace and BetUpdate.
 *
 * Bagrut: remote DB read+write (req 7), API (req 6), advanced logic/transaction,
 * data structures (req 15).
 */
public class BetResolver {

    private final Context context;
    private final String uid;
    private final FirebaseFirestore db;
    private final RequestQueue queue;

    public BetResolver(Context context, String uid) {
        this.context = context;
        this.uid = uid;
        this.db = FirebaseFirestore.getInstance();
        this.queue = Volley.newRequestQueue(context);
    }

    public void resolve() {
        String url = "https://api.jolpi.ca/ergast/f1/2026/results.json?limit=100";
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        List<CompletedRace> completedRaces = parseResults(response);
                        if (!completedRaces.isEmpty()) {
                            loadUserBets(completedRaces);
                        }
                    } catch (Exception e) {
                        showError();
                    }
                },
                error -> showError());
        queue.add(request);
    }

    private List<CompletedRace> parseResults(String response) throws Exception {
        List<CompletedRace> races = new ArrayList<>();
        JSONArray raceArray = new JSONObject(response)
                .getJSONObject("MRData")
                .getJSONObject("RaceTable")
                .getJSONArray("Races");

        for (int i = 0; i < raceArray.length(); i++) {
            JSONObject raceObj = raceArray.getJSONObject(i);
            String raceName = raceObj.getString("raceName");
            int round = raceObj.getInt("round");

            Map<String, Integer> positions = new HashMap<>();
            JSONArray results = raceObj.getJSONArray("Results");
            for (int j = 0; j < results.length(); j++) {
                JSONObject res = results.getJSONObject(j);
                int pos = res.optInt("position", 99);
                JSONObject driver = res.getJSONObject("Driver");
                String fullName = driver.getString("givenName") + " " + driver.getString("familyName");
                positions.put(fullName.trim().toLowerCase(), pos);
            }

            races.add(new CompletedRace(raceName, round, positions));
        }
        return races;
    }

    private void loadUserBets(List<CompletedRace> completedRaces) {
        db.collection("users").document(uid)
                .collection("bets")
                .get()
                .addOnSuccessListener(snapshots -> processBets(completedRaces, snapshots.getDocuments()))
                .addOnFailureListener(e -> showError());
    }

    private void processBets(List<CompletedRace> completedRaces, List<DocumentSnapshot> betDocs) {
        Map<String, DocumentSnapshot> unresolvedByRaceName = new HashMap<>();
        Set<String> handledRaceNames = new HashSet<>();
        List<DocumentSnapshot> unresolvedChampBets = new ArrayList<>();

        for (DocumentSnapshot doc : betDocs) {
            String docId = doc.getId();
            if (docId.startsWith("champ_")) {
                if (!Boolean.TRUE.equals(doc.getBoolean("resolved"))) {
                    unresolvedChampBets.add(doc);
                }
                continue;
            }
            if (!docId.startsWith("race_")) continue;

            String raceName = doc.getString("raceName");
            if (raceName == null) continue;
            String key = raceName.trim().toLowerCase();

            if (Boolean.TRUE.equals(doc.getBoolean("resolved"))) {
                handledRaceNames.add(key);
            } else {
                unresolvedByRaceName.put(key, doc);
            }
        }

        List<BetUpdate> updates = new ArrayList<>();
        int totalDelta = 0;

        for (CompletedRace race : completedRaces) {
            String key = race.raceName.trim().toLowerCase();
            DocumentSnapshot betDoc = unresolvedByRaceName.get(key);

            if (betDoc != null) {
                String driverName = betDoc.getString("driverName");
                Long predictedPos = betDoc.getLong("predictedPosition");
                int predicted = predictedPos != null ? predictedPos.intValue() : 0;

                int actual = 99;
                if (driverName != null) {
                    Integer found = race.positions.get(driverName.trim().toLowerCase());
                    if (found != null) actual = found;
                }

                int points = calculatePoints(actual);
                totalDelta += points;
                updates.add(new BetUpdate(betDoc.getReference(), points, actual,
                        race.raceName, driverName != null ? driverName : "", predicted, false, false));

            } else if (!handledRaceNames.contains(key)) {
                totalDelta -= 3;
                DocumentReference sentinelRef = db.collection("users").document(uid)
                        .collection("bets").document("race_nobet_r" + race.round);
                updates.add(new BetUpdate(sentinelRef, -3, 0,
                        race.raceName, "No bet placed", 0, true, false));
            }
        }

        if (!updates.isEmpty()) {
            applyChanges(updates, totalDelta);
        }

        if (!unresolvedChampBets.isEmpty()) {
            tryResolveChampionshipBets(unresolvedChampBets);
        }
    }

    /**
     * Commits all bet updates atomically in one Firestore transaction.
     * A transaction is used (not a plain update) because we READ the current
     * points and then WRITE a new value — the transaction guarantees no other
     * write slips in between, so points can't be double-counted or overwritten.
     */
    private void applyChanges(List<BetUpdate> updates, int totalDelta) {
        DocumentReference userRef = db.collection("users").document(uid);

        db.runTransaction(transaction -> {
            DocumentSnapshot userDoc = transaction.get(userRef);
            long currentPoints = userDoc.getLong("points") != null ? userDoc.getLong("points") : 0;

            for (BetUpdate u : updates) {
                if (u.isSentinel) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("raceName", u.raceName);
                    data.put("driverName", u.driverName);
                    data.put("resolved", true);
                    data.put("pointsAwarded", u.pointsAwarded);
                    data.put("timestamp", Timestamp.now());
                    transaction.set(u.ref, data);
                } else {
                    transaction.update(u.ref,
                            "resolved", true,
                            "actualPosition", u.actualPosition,
                            "pointsAwarded", u.pointsAwarded);
                }
            }

            transaction.update(userRef, "points", currentPoints + totalDelta);
            return null;
        }).addOnSuccessListener(v -> {
            for (BetUpdate u : updates) {
                sendNotification(u);
            }
        }).addOnFailureListener(e -> showError());
    }

    private void tryResolveChampionshipBets(List<DocumentSnapshot> champBets) {
        String url = "https://api.jolpi.ca/ergast/f1/2026/driverstandings.json";
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject mrData = new JSONObject(response).getJSONObject("MRData");
                        JSONArray standingsLists = mrData.getJSONObject("StandingsTable")
                                .getJSONArray("StandingsLists");
                        if (standingsLists.length() == 0) return;

                        JSONObject standingsList = standingsLists.getJSONObject(0);
                        int round = standingsList.optInt("round", 0);
                        // Only resolve at end of season (all 24 rounds complete)
                        if (round < 24) return;

                        JSONObject champEntry = standingsList.getJSONArray("DriverStandings").getJSONObject(0);
                        JSONObject champDriver = champEntry.getJSONObject("Driver");
                        String driverChampion = (champDriver.optString("givenName") + " "
                                + champDriver.optString("familyName")).trim();

                        fetchConstructorChampionAndResolve(champBets, driverChampion);
                    } catch (Exception ignored) {}
                },
                error -> {});
        queue.add(request);
    }

    private void fetchConstructorChampionAndResolve(List<DocumentSnapshot> champBets, String driverChampion) {
        String url = "https://api.jolpi.ca/ergast/f1/2026/constructorstandings.json";
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject mrData = new JSONObject(response).getJSONObject("MRData");
                        JSONArray standingsLists = mrData.getJSONObject("StandingsTable")
                                .getJSONArray("StandingsLists");
                        if (standingsLists.length() == 0) return;

                        String constructorChampion = standingsLists.getJSONObject(0)
                                .getJSONArray("ConstructorStandings")
                                .getJSONObject(0)
                                .getJSONObject("Constructor")
                                .optString("name", "").trim();

                        applyChampionshipResults(champBets, driverChampion, constructorChampion);
                    } catch (Exception ignored) {}
                },
                error -> {});
        queue.add(request);
    }

    private void applyChampionshipResults(List<DocumentSnapshot> champBets, String driverChampion, String constructorChampion) {
        List<BetUpdate> updates = new ArrayList<>();
        int totalDelta = 0;

        for (DocumentSnapshot doc : champBets) {
            String champType = doc.getString("champType");
            String predictedWinner = doc.getString("predictedWinner");
            if (champType == null || predictedWinner == null) continue;

            String actual = champType.toLowerCase().contains("driver") ? driverChampion : constructorChampion;
            int points = predictedWinner.trim().equalsIgnoreCase(actual) ? 25 : 0;
            totalDelta += points;
            updates.add(new BetUpdate(doc.getReference(), points, 0,
                    champType, predictedWinner, 0, false, true));
        }

        if (updates.isEmpty()) return;

        final int finalDelta = totalDelta;
        DocumentReference userRef = db.collection("users").document(uid);
        db.runTransaction(transaction -> {
            DocumentSnapshot userDoc = transaction.get(userRef);
            long currentPoints = userDoc.getLong("points") != null ? userDoc.getLong("points") : 0;
            for (BetUpdate u : updates) {
                transaction.update(u.ref, "resolved", true, "pointsAwarded", u.pointsAwarded);
            }
            transaction.update(userRef, "points", currentPoints + finalDelta);
            return null;
        }).addOnSuccessListener(v -> {
            for (BetUpdate u : updates) {
                sendNotification(u);
            }
        }).addOnFailureListener(e -> showError());
    }

    private void sendNotification(BetUpdate u) {
        String title, message;
        if (u.isSentinel) {
            title = u.raceName;
            message = "No bet placed — -3 pts";
        } else if (u.isChampBet) {
            String pts = u.pointsAwarded > 0 ? "+" + u.pointsAwarded : "0";
            title = u.raceName + " resolved";
            message = u.driverName + " — " + pts + " pts";
        } else {
            String pts = u.pointsAwarded >= 0 ? "+" + u.pointsAwarded : String.valueOf(u.pointsAwarded);
            title = u.raceName + " resolved";
            message = u.driverName + " finished P" + u.actualPosition + " — " + pts + " pts";
        }
        NotificationHelper.sendImmediateNotification(context, title, message, null);
    }

    /** Scoring table: P1=+10, P2=+6, P3=+3, P4=0, P5-P10=-2, P11+/DNF=-4. */
    private int calculatePoints(int actualPos) {
        if (actualPos == 1) return 10;
        if (actualPos == 2) return 6;
        if (actualPos == 3) return 3;
        if (actualPos == 4) return 0;
        if (actualPos >= 5 && actualPos <= 10) return -2;
        return -4;
    }

    private void showError() {
        Toast.makeText(context, "Could not resolve bets — check your connection", Toast.LENGTH_SHORT).show();
    }

    private static class CompletedRace {
        final String raceName;
        final int round;
        final Map<String, Integer> positions;

        CompletedRace(String raceName, int round, Map<String, Integer> positions) {
            this.raceName = raceName;
            this.round = round;
            this.positions = positions;
        }
    }

    private static class BetUpdate {
        final DocumentReference ref;
        final int pointsAwarded;
        final int actualPosition;
        final String raceName;
        final String driverName;
        final int predictedPosition;
        final boolean isSentinel;
        final boolean isChampBet;

        BetUpdate(DocumentReference ref, int pointsAwarded, int actualPosition,
                  String raceName, String driverName, int predictedPosition,
                  boolean isSentinel, boolean isChampBet) {
            this.ref = ref;
            this.pointsAwarded = pointsAwarded;
            this.actualPosition = actualPosition;
            this.raceName = raceName;
            this.driverName = driverName;
            this.predictedPosition = predictedPosition;
            this.isSentinel = isSentinel;
            this.isChampBet = isChampBet;
        }
    }
}
