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

        for (DocumentSnapshot doc : betDocs) {
            String docId = doc.getId();
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
                        race.raceName, driverName != null ? driverName : "", predicted, false));

            } else if (!handledRaceNames.contains(key)) {
                totalDelta -= 3;
                DocumentReference sentinelRef = db.collection("users").document(uid)
                        .collection("bets").document("race_nobet_r" + race.round);
                updates.add(new BetUpdate(sentinelRef, -3, 0,
                        race.raceName, "No bet placed", 0, true));
            }
        }

        if (!updates.isEmpty()) {
            applyChanges(updates, totalDelta);
        }
    }

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

    private void sendNotification(BetUpdate u) {
        String title, message;
        if (u.isSentinel) {
            title = u.raceName;
            message = "No bet placed — -3 pts";
        } else {
            String pts = u.pointsAwarded >= 0 ? "+" + u.pointsAwarded : String.valueOf(u.pointsAwarded);
            title = u.raceName + " resolved";
            message = u.driverName + " finished P" + u.actualPosition + " — " + pts + " pts";
        }
        NotificationHelper.sendImmediateNotification(context, title, message, null);
    }

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

        BetUpdate(DocumentReference ref, int pointsAwarded, int actualPosition,
                  String raceName, String driverName, int predictedPosition, boolean isSentinel) {
            this.ref = ref;
            this.pointsAwarded = pointsAwarded;
            this.actualPosition = actualPosition;
            this.raceName = raceName;
            this.driverName = driverName;
            this.predictedPosition = predictedPosition;
            this.isSentinel = isSentinel;
        }
    }
}
