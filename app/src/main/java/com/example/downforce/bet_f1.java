package com.example.downforce;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class bet_f1 extends AppCompatActivity {

    private TextView textPoints, textBetCount, textNoRaceBets, textNoChampBets;
    private Spinner spinnerRaceDriver, spinnerRacePosition;
    private Spinner spinnerChampType, spinnerChampWinner;
    private Button btnPlaceRaceBet, btnPlaceChampBet;
    private LinearLayout raceBetsContainer, champBetsContainer;

    private FirebaseFirestore db;
    private String uid;
    private RequestQueue queue;

    private List<String> driverNames = new ArrayList<>();
    private String nextRaceId = "unknown";
    private String nextRaceName = "Next Race";

    private static final String[] POSITIONS = {
            "1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th"
    };
    private static final String[] CHAMP_TYPES = {
            "Drivers' Champion", "Constructors' Champion"
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.calender) {
            startActivity(new Intent(this, MainActivity.class));
        } else if (item.getItemId() == R.id.stats) {
            startActivity(new Intent(this, downforce_stats.class));
        } else if (item.getItemId() == R.id.action_ai_chat) {
            startActivity(new Intent(this, AiChatActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    //do not touch this function!!!!
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bet_f1);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //^^^do not touch this function^^^

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        queue = Volley.newRequestQueue(this);

        textPoints = findViewById(R.id.text_points);
        textBetCount = findViewById(R.id.text_bet_count);
        textNoRaceBets = findViewById(R.id.text_no_race_bets);
        textNoChampBets = findViewById(R.id.text_no_champ_bets);
        spinnerRaceDriver = findViewById(R.id.spinner_race_driver);
        spinnerRacePosition = findViewById(R.id.spinner_race_position);
        spinnerChampType = findViewById(R.id.spinner_championship_type);
        spinnerChampWinner = findViewById(R.id.spinner_championship_winner);
        btnPlaceRaceBet = findViewById(R.id.btn_place_race_bet);
        btnPlaceChampBet = findViewById(R.id.btn_place_champ_bet);
        raceBetsContainer = findViewById(R.id.race_bets_container);
        champBetsContainer = findViewById(R.id.champ_bets_container);

        // Scoring is fixed — hide the "points to bet" amount fields
        findViewById(R.id.edit_race_bet_amount).setVisibility(View.GONE);
        findViewById(R.id.edit_champ_bet_amount).setVisibility(View.GONE);

        setupSpinners();
        loadUserPoints();
        fetchNextRaceAndDrivers();
        loadExistingBets();

        btnPlaceRaceBet.setOnClickListener(v -> placeRaceBet());
        btnPlaceChampBet.setOnClickListener(v -> placeChampBet());
    }

    private void setupSpinners() {
        ArrayAdapter<String> posAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, POSITIONS);
        posAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRacePosition.setAdapter(posAdapter);

        ArrayAdapter<String> champTypeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, CHAMP_TYPES);
        champTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChampType.setAdapter(champTypeAdapter);
    }

    private void loadUserPoints() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        long pts = doc.getLong("points") != null ? doc.getLong("points") : 0;
                        textPoints.setText(pts + " 🪙");
                    }
                });
    }

    private void fetchNextRaceAndDrivers() {
        String url = "https://api.openf1.org/v1/meetings?year=2026";
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray meetings = new JSONArray(response);
                        long now = System.currentTimeMillis();

                        for (int i = 0; i < meetings.length(); i++) {
                            JSONObject obj = meetings.getJSONObject(i);
                            String name = obj.optString("meeting_name", "");
                            if (name.contains("Testing")) continue;

                            Race r = new Race(
                                    obj.optInt("meeting_key", i), name,
                                    obj.optString("location", ""),
                                    obj.optString("date_start", ""),
                                    obj.optString("date_end", ""),
                                    obj.optString("circuit_image", ""),
                                    obj.optString("country_flag", "")
                            );

                            if (r.endDate.toInstant().toEpochMilli() > now) {
                                nextRaceId = String.valueOf(r.getId());
                                nextRaceName = r.getName();
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                    loadDrivers();
                },
                error -> loadDrivers());
        queue.add(request);
    }

    private void loadDrivers() {
        String url = "https://api.openf1.org/v1/drivers?meeting_key=latest";
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    driverNames.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject d = response.getJSONObject(i);
                            String name = d.optString("full_name", d.optString("last_name", ""));
                            if (!name.isEmpty() && !driverNames.contains(name)) {
                                driverNames.add(name);
                            }
                        } catch (JSONException ignored) {}
                    }
                    if (driverNames.isEmpty()) addFallbackDrivers();
                    setDriverSpinners();
                },
                error -> {
                    addFallbackDrivers();
                    setDriverSpinners();
                });
        queue.add(request);
    }

    private void setDriverSpinners() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, driverNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRaceDriver.setAdapter(adapter);
        spinnerChampWinner.setAdapter(adapter);
    }

    private void addFallbackDrivers() {
        String[] names = {
                "Max Verstappen", "Lando Norris", "Charles Leclerc", "Carlos Sainz",
                "Lewis Hamilton", "George Russell", "Fernando Alonso", "Oscar Piastri",
                "Lance Stroll", "Esteban Ocon", "Pierre Gasly", "Valtteri Bottas",
                "Zhou Guanyu", "Alex Albon", "Yuki Tsunoda", "Liam Lawson",
                "Kevin Magnussen", "Nico Hulkenberg", "Oliver Bearman", "Jack Doohan"
        };
        driverNames.addAll(Arrays.asList(names));
    }

    private void placeRaceBet() {
        if (driverNames.isEmpty()) {
            Toast.makeText(this, "Drivers still loading...", Toast.LENGTH_SHORT).show();
            return;
        }

        String driver = spinnerRaceDriver.getSelectedItem().toString();
        int position = spinnerRacePosition.getSelectedItemPosition() + 1;

        Map<String, Object> bet = new HashMap<>();
        bet.put("raceId", nextRaceId);
        bet.put("raceName", nextRaceName);
        bet.put("driverName", driver);
        bet.put("predictedPosition", position);
        bet.put("timestamp", Timestamp.now());
        bet.put("resolved", false);
        bet.put("actualPosition", 0);
        bet.put("pointsAwarded", 0);

        btnPlaceRaceBet.setEnabled(false);
        db.collection("users").document(uid)
                .collection("bets").document("race_" + nextRaceId)
                .set(bet)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this,
                            "Bet placed: " + driver + " → P" + position, Toast.LENGTH_SHORT).show();
                    btnPlaceRaceBet.setEnabled(true);
                    loadExistingBets();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to place bet", Toast.LENGTH_SHORT).show();
                    btnPlaceRaceBet.setEnabled(true);
                });
    }

    private void placeChampBet() {
        if (driverNames.isEmpty()) {
            Toast.makeText(this, "Drivers still loading...", Toast.LENGTH_SHORT).show();
            return;
        }

        String champType = spinnerChampType.getSelectedItem().toString();
        String champion = spinnerChampWinner.getSelectedItem().toString();
        String docId = "champ_" + champType.replace("'", "").replace(" ", "_").toLowerCase();

        Map<String, Object> bet = new HashMap<>();
        bet.put("champType", champType);
        bet.put("predictedWinner", champion);
        bet.put("timestamp", Timestamp.now());
        bet.put("resolved", false);
        bet.put("pointsAwarded", 0);

        btnPlaceChampBet.setEnabled(false);
        db.collection("users").document(uid)
                .collection("bets").document(docId)
                .set(bet)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this,
                            "Championship bet: " + champion + " (" + champType + ")",
                            Toast.LENGTH_SHORT).show();
                    btnPlaceChampBet.setEnabled(true);
                    loadExistingBets();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to place bet", Toast.LENGTH_SHORT).show();
                    btnPlaceChampBet.setEnabled(true);
                });
    }

    private void loadExistingBets() {
        db.collection("users").document(uid)
                .collection("bets")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    raceBetsContainer.removeAllViews();
                    champBetsContainer.removeAllViews();

                    int raceCount = 0, champCount = 0, activeBets = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String docId = doc.getId();
                        Boolean resolved = doc.getBoolean("resolved");
                        Long pts = doc.getLong("pointsAwarded");

                        if (!Boolean.TRUE.equals(resolved)) activeBets++;

                        String status = Boolean.TRUE.equals(resolved)
                                ? (pts != null && pts >= 0 ? "+" + pts + " pts" : pts + " pts")
                                : "Pending";

                        if (docId.startsWith("race_")) {
                            raceCount++;
                            String raceName = doc.getString("raceName");
                            String driverName = doc.getString("driverName");
                            Long pos = doc.getLong("predictedPosition");
                            String label = (pos != null)
                                    ? raceName + " — " + driverName + " P" + pos
                                    : raceName + " — " + driverName;
                            addBetRow(raceBetsContainer, label, status);

                        } else if (docId.startsWith("champ_")) {
                            champCount++;
                            String champType = doc.getString("champType");
                            String winner = doc.getString("predictedWinner");
                            addBetRow(champBetsContainer, champType + ": " + winner, status);
                        }
                    }

                    textNoRaceBets.setVisibility(raceCount == 0 ? View.VISIBLE : View.GONE);
                    textNoChampBets.setVisibility(champCount == 0 ? View.VISIBLE : View.GONE);
                    textBetCount.setText(activeBets + " 🎲");
                });
    }

    private void addBetRow(LinearLayout container, String label, String status) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(24, 20, 24, 20);
        row.setBackgroundResource(R.drawable.rounded_card_bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 8);
        row.setLayoutParams(params);

        TextView tvLabel = new TextView(this);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
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
}
