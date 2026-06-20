package com.example.downforce;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Map;

/**
 * WrappedActivity — a personal "Wrapped"-style summary screen.
 *
 * PURPOSE (why): shows the user their season at a glance: points, races
 * watched/skipped, bets placed/won, win rate, and missed races.
 *
 * HOW (how): combines TWO storage sources. loadWatchHistory() counts
 * watched/skipped from SharedPreferences ("race_history"). loadPointsAndBets()
 * reads Firestore: points from the user doc, and counts bets from the /bets
 * sub-collection (race_ docs = placed, race_nobet_ docs = missed races).
 *
 * Bagrut: SharedPreferences (req 10) + Firestore read (req 7).
 */
public class WrappedActivity extends AppCompatActivity {

    private TextView textPoints, textWatched, textSkipped;
    private TextView textBetsPlaced, textBetsWon, textWinRate, textMissedRaces;

    private FirebaseFirestore db;
    private String uid;

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
        } else if (item.getItemId() == R.id.bet_f1) {
            startActivity(new Intent(this, bet_f1.class));
        } else if (item.getItemId() == R.id.action_ai_chat) {
            startActivity(new Intent(this, AiChatActivity.class));
        } else if (item.getItemId() == R.id.sign_out) {
            FirebaseAuth.getInstance().signOut();
            Intent login = new Intent(this, LoginActivity.class);
            login.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(login);
        }
        return super.onOptionsItemSelected(item);
    }

    //do not touch this function!!!!
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_wrapped);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //^^^do not touch this function^^^

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        textPoints = findViewById(R.id.text_wrapped_points);
        textWatched = findViewById(R.id.text_watched_count);
        textSkipped = findViewById(R.id.text_skipped_count);
        textBetsPlaced = findViewById(R.id.text_bets_placed);
        textBetsWon = findViewById(R.id.text_bets_won);
        textWinRate = findViewById(R.id.text_win_rate);
        textMissedRaces = findViewById(R.id.text_missed_races);

        loadWatchHistory();
        loadPointsAndBets();
    }

    private void loadWatchHistory() {
        SharedPreferences prefs = getSharedPreferences("race_history", Context.MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();
        int watched = 0, skipped = 0;
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if ("watched".equals(entry.getValue())) watched++;
            else if ("skipped".equals(entry.getValue())) skipped++;
        }
        textWatched.setText(String.valueOf(watched));
        textSkipped.setText(String.valueOf(skipped));
    }

    private void loadPointsAndBets() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        long pts = doc.getLong("points") != null ? doc.getLong("points") : 0;
                        textPoints.setText(String.valueOf(pts));
                    }
                });

        db.collection("users").document(uid).collection("bets").get()
                .addOnSuccessListener(snapshots -> {
                    int betsPlaced = 0, betsWon = 0, resolvedBets = 0, missedRaces = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String docId = doc.getId();
                        if (docId.startsWith("race_nobet_")) {
                            missedRaces++;
                        } else if (docId.startsWith("race_")) {
                            betsPlaced++;
                            if (Boolean.TRUE.equals(doc.getBoolean("resolved"))) {
                                resolvedBets++;
                                Long pts = doc.getLong("pointsAwarded");
                                if (pts != null && pts > 0) betsWon++;
                            }
                        }
                    }

                    textBetsPlaced.setText(String.valueOf(betsPlaced));
                    textBetsWon.setText(betsWon + " / " + resolvedBets);
                    textMissedRaces.setText(String.valueOf(missedRaces));

                    if (resolvedBets > 0) {
                        int rate = (int) Math.round((betsWon * 100.0) / resolvedBets);
                        textWinRate.setText(rate + "%");
                    } else {
                        textWinRate.setText("—");
                    }
                });
    }
}
