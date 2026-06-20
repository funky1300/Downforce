package com.example.downforce;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * downforce_stats — the statistics screen.
 *
 * PURPOSE (why): shows driver standings, constructor standings, the last race
 * results + fastest lap, and a leaderboard comparing all users in the app.
 *
 * HOW (how): three Jolpica API calls (driverstandings, constructorstandings,
 * last/results), each parsing the nested JSON and building table rows in code.
 * loadLeaderboard() READS Firestore: all users ordered by points descending;
 * tapping a row opens a dialog with that user's bets.
 *
 * Bagrut: API (req 6), Firestore read (req 7), dialogs (req 13).
 */
public class downforce_stats extends AppCompatActivity {

    private LinearLayout driverStandingsContainer, constructorStandingsContainer;
    private LinearLayout raceResultsContainer, lapTimesContainer, leaderboardContainer;
    private TextView textLastRaceName, textLastRaceDate;
    private TextView textFastestLapDriver, textFastestLapTime;
    private RequestQueue queue;
    private FirebaseFirestore db;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.calender) {
            startActivity(new Intent(this, MainActivity.class));
        } else if (item.getItemId() == R.id.bet_f1) {
            startActivity(new Intent(this, bet_f1.class));
        } else if (item.getItemId() == R.id.action_ai_chat) {
            startActivity(new Intent(this, AiChatActivity.class));
        } else if (item.getItemId() == R.id.wrapped) {
            startActivity(new Intent(this, WrappedActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    //do not touch this function!!!!
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_downforce_stats);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //^^^do not touch this function^^^

        queue = Volley.newRequestQueue(this);
        db = FirebaseFirestore.getInstance();

        driverStandingsContainer = findViewById(R.id.driver_standings_container);
        constructorStandingsContainer = findViewById(R.id.constructor_standings_container);
        raceResultsContainer = findViewById(R.id.race_results_container);
        lapTimesContainer = findViewById(R.id.lap_times_container);
        leaderboardContainer = findViewById(R.id.leaderboard_container);
        textLastRaceName = findViewById(R.id.text_last_race_name);
        textLastRaceDate = findViewById(R.id.text_last_race_date);
        textFastestLapDriver = findViewById(R.id.text_fastest_lap_driver);
        textFastestLapTime = findViewById(R.id.text_fastest_lap_time);

        loadDriverStandings();
        loadConstructorStandings();
        loadRaceResults();
        loadLeaderboard();
    }

    private void loadDriverStandings() {
        String url = "https://api.jolpi.ca/ergast/f1/2026/driverstandings.json";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray standings = response
                                .getJSONObject("MRData")
                                .getJSONObject("StandingsTable")
                                .getJSONArray("StandingsLists")
                                .getJSONObject(0)
                                .getJSONArray("DriverStandings");

                        driverStandingsContainer.removeAllViews();
                        for (int i = 0; i < standings.length(); i++) {
                            JSONObject entry = standings.getJSONObject(i);
                            String pos = entry.optString("position");
                            String pts = entry.optString("points");
                            JSONObject driver = entry.getJSONObject("Driver");
                            String name = driver.optString("givenName") + " " + driver.optString("familyName");
                            String team = entry.getJSONArray("Constructors")
                                    .getJSONObject(0).optString("name");
                            addDriverRow(pos, name, team, pts);
                        }
                    } catch (JSONException e) {
                        showError(driverStandingsContainer, "Could not load driver standings");
                    }
                },
                error -> showError(driverStandingsContainer, "Network error"));
        queue.add(request);
    }

    private void loadConstructorStandings() {
        String url = "https://api.jolpi.ca/ergast/f1/2026/constructorstandings.json";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray standings = response
                                .getJSONObject("MRData")
                                .getJSONObject("StandingsTable")
                                .getJSONArray("StandingsLists")
                                .getJSONObject(0)
                                .getJSONArray("ConstructorStandings");

                        constructorStandingsContainer.removeAllViews();
                        for (int i = 0; i < standings.length(); i++) {
                            JSONObject entry = standings.getJSONObject(i);
                            String pos = entry.optString("position");
                            String pts = entry.optString("points");
                            String team = entry.getJSONObject("Constructor").optString("name");
                            addConstructorRow(pos, team, pts);
                        }
                    } catch (JSONException e) {
                        showError(constructorStandingsContainer, "Could not load constructor standings");
                    }
                },
                error -> showError(constructorStandingsContainer, "Network error"));
        queue.add(request);
    }

    private void loadRaceResults() {
        String url = "https://api.jolpi.ca/ergast/f1/2026/last/results.json";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray races = response
                                .getJSONObject("MRData")
                                .getJSONObject("RaceTable")
                                .getJSONArray("Races");

                        if (races.length() == 0) {
                            textLastRaceName.setText("Season not started yet");
                            return;
                        }

                        JSONObject race = races.getJSONObject(0);
                        textLastRaceName.setText(race.optString("raceName"));
                        textLastRaceDate.setText(race.optString("date"));

                        JSONArray results = race.getJSONArray("Results");
                        raceResultsContainer.removeAllViews();
                        lapTimesContainer.removeAllViews();

                        String fastestDriver = "—", fastestTime = "—";
                        int fastestRank = Integer.MAX_VALUE;

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject result = results.getJSONObject(i);
                            String pos = result.optString("position");
                            JSONObject driver = result.getJSONObject("Driver");
                            String name = driver.optString("givenName") + " " + driver.optString("familyName");

                            // Top 10 in the results section
                            if (i < 10) {
                                String time = "—";
                                if (result.has("Time")) {
                                    time = result.getJSONObject("Time").optString("time", "—");
                                } else if (result.has("status")) {
                                    time = result.optString("status");
                                }
                                addResultRow(pos, name, time);
                            }

                            // Fastest lap for all drivers
                            if (result.has("FastestLap")) {
                                JSONObject fl = result.getJSONObject("FastestLap");
                                String lapTime = fl.getJSONObject("Time").optString("time", "—");
                                addLapRow(driver.optString("familyName"), lapTime);

                                int rank = fl.optInt("rank", Integer.MAX_VALUE);
                                if (rank < fastestRank) {
                                    fastestRank = rank;
                                    fastestDriver = name;
                                    fastestTime = lapTime;
                                }
                            }
                        }

                        textFastestLapDriver.setText(fastestDriver);
                        textFastestLapTime.setText(fastestTime);

                    } catch (JSONException e) {
                        textLastRaceName.setText("Error loading results");
                    }
                },
                error -> textLastRaceName.setText("Network error"));
        queue.add(request);
    }

    private void addDriverRow(String pos, String name, String team, String pts) {
        LinearLayout row = makeRow();
        row.addView(makeCell(pos, 0xFFDBE2EF, 14f, true, dp(32)));
        row.addView(makeCell(name, 0xFFF9F7F7, 14f, true, 0));     // weight=1
        row.addView(makeCell(team, 0xFFDBE2EF, 12f, false, -1));
        TextView tvPts = makeCell(pts, 0xFFFFFFFF, 14f, true, dp(48));
        tvPts.setGravity(Gravity.END);
        row.addView(tvPts);
        driverStandingsContainer.addView(row);
    }

    private void addConstructorRow(String pos, String team, String pts) {
        LinearLayout row = makeRow();
        row.addView(makeCell(pos, 0xFFDBE2EF, 14f, true, dp(32)));
        row.addView(makeCell(team, 0xFFF9F7F7, 14f, true, 0));     // weight=1
        TextView tvPts = makeCell(pts, 0xFFFFFFFF, 14f, true, dp(48));
        tvPts.setGravity(Gravity.END);
        row.addView(tvPts);
        constructorStandingsContainer.addView(row);
    }

    private void addResultRow(String pos, String driver, String time) {
        LinearLayout row = makeRow();
        row.addView(makeCell("P" + pos, 0xFFDBE2EF, 14f, true, dp(32)));
        row.addView(makeCell(driver, 0xFFF9F7F7, 14f, true, 0));   // weight=1
        row.addView(makeCell(time, 0xFFFFFFFF, 12f, false, -1));
        raceResultsContainer.addView(row);
    }

    private void addLapRow(String driver, String lapTime) {
        LinearLayout row = makeRow();
        row.addView(makeCell(driver, 0xFFF9F7F7, 14f, true, 0));   // weight=1
        row.addView(makeCell(lapTime, 0xFFDBE2EF, 14f, true, -1));
        lapTimesContainer.addView(row);
    }

    private void showError(LinearLayout container, String message) {
        container.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextColor(0xFFFF5252);
        tv.setTextSize(13f);
        tv.setPadding(24, 16, 24, 16);
        container.addView(tv);
    }

    private void loadLeaderboard() {
        db.collection("users")
                .orderBy("points", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    leaderboardContainer.removeAllViews();
                    int rank = 1;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String name = doc.getString("displayName");
                        Long pts = doc.getLong("points");
                        String uid = doc.getId();
                        if (name == null) name = "Unknown";
                        if (pts == null) pts = 0L;
                        addLeaderboardRow(rank++, name, pts, uid);
                    }
                    if (snapshots.isEmpty()) {
                        showError(leaderboardContainer, "No players yet");
                    }
                })
                .addOnFailureListener(e -> showError(leaderboardContainer, "Could not load leaderboard"));
    }

    private void addLeaderboardRow(int rank, String name, long pts, String uid) {
        LinearLayout row = makeRow();
        row.addView(makeCell(String.valueOf(rank), 0xFFDBE2EF, 14f, true, dp(32)));
        row.addView(makeCell(name, 0xFFF9F7F7, 14f, true, 0));
        TextView tvPts = makeCell(pts + " 🪙", 0xFFFFFFFF, 14f, true, dp(48));
        tvPts.setGravity(Gravity.END);
        row.addView(tvPts);
        row.setOnClickListener(v -> showUserBetsDialog(name, uid));
        leaderboardContainer.addView(row);
    }

    private void showUserBetsDialog(String displayName, String uid) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout betsLayout = new LinearLayout(this);
        betsLayout.setOrientation(LinearLayout.VERTICAL);
        betsLayout.setPadding(32, 16, 32, 16);
        scrollView.addView(betsLayout);

        TextView loading = new TextView(this);
        loading.setText("Loading...");
        loading.setTextColor(0xFFDBE2EF);
        loading.setPadding(0, 16, 0, 16);
        betsLayout.addView(loading);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(displayName + "'s Bets")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.alertdialogbox);
        }
        dialog.show();

        db.collection("users").document(uid)
                .collection("bets")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    betsLayout.removeAllViews();
                    if (snapshots.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText("No bets placed yet");
                        empty.setTextColor(0xFFDBE2EF);
                        empty.setPadding(0, 16, 0, 16);
                        betsLayout.addView(empty);
                        return;
                    }
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String docId = doc.getId();
                        Boolean resolved = doc.getBoolean("resolved");
                        Long pts = doc.getLong("pointsAwarded");
                        String status = Boolean.TRUE.equals(resolved)
                                ? (pts != null && pts >= 0 ? "+" + pts + " pts" : pts + " pts")
                                : "Pending";
                        String label;
                        if (docId.startsWith("race_")) {
                            String raceName = doc.getString("raceName");
                            String driverName = doc.getString("driverName");
                            Long pos = doc.getLong("predictedPosition");
                            label = (pos != null)
                                    ? raceName + "\n" + driverName + " P" + pos
                                    : raceName + "\n" + driverName;
                        } else {
                            String champType = doc.getString("champType");
                            String winner = doc.getString("predictedWinner");
                            label = champType + "\n" + winner;
                        }
                        addBetDialogRow(betsLayout, label, status);
                    }
                });
    }

    private void addBetDialogRow(LinearLayout container, String label, String status) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(24, 20, 24, 20);
        row.setBackgroundResource(R.drawable.rounded_card_bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 8);
        row.setLayoutParams(params);

        TextView tvLabel = new TextView(this);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvLabel.setText(label);
        tvLabel.setTextColor(0xFFF9F7F7);
        tvLabel.setTextSize(13f);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(status);
        tvStatus.setTextSize(13f);
        if (status.startsWith("+")) {
            tvStatus.setTextColor(0xFF4CAF50);
        } else if (status.startsWith("-")) {
            tvStatus.setTextColor(0xFFFF5252);
        } else {
            tvStatus.setTextColor(0xFFDBE2EF);
        }

        row.addView(tvLabel);
        row.addView(tvStatus);
        container.addView(row);
    }

    // ----- layout helpers -----

    private LinearLayout makeRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(24, 20, 24, 20);
        row.setBackgroundResource(R.drawable.rounded_card_bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 12);
        row.setLayoutParams(params);
        return row;
    }

    // width=0 means weight=1 (flexible), width=-1 means wrap_content
    private TextView makeCell(String text, int color, float size, boolean bold, int width) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(size);
        if (bold) tv.setTypeface(tv.getTypeface(), Typeface.BOLD);

        if (width == 0) {
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        } else if (width == -1) {
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        } else {
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    width, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        return tv;
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
