package com.example.downforce;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private CountDownTimer timer;
    private TextView raceTextView;
    private ImageView image;
    private GridLayout racesGrid;
    private ArrayList<Race> races;

    private String[] BannedRaces = {"Pre-Season Testing", "Pre-Season Testing"};


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.calender) {
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
            fetchRacesAPI();
            return true;
        }
        if(item.getItemId() == R.id.stats){
            Intent goGu = new Intent(this, downforce_stats.class);
            startActivity(goGu);
        }
        if(item.getItemId() == R.id.bet_f1){
            Intent goGu = new Intent(this, bet_f1.class);
            startActivity(goGu);
        }
        return super.onOptionsItemSelected(item);
    }

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(this,
                            "Notifications disabled — race reminders won't be shown.",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        NotificationHelper.createChannel(this);
        ensureNotificationPermission();


        raceTextView = findViewById(R.id.text);
        image = findViewById(R.id.image);
        racesGrid = findViewById(R.id.races_grid);

        TextView tv = findViewById(R.id.tvCountdown);

        races = new ArrayList<>();
        fetchRacesAPI();



        try {
            // 1. Set date and get target time in milliseconds all in one line
            long target = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse("2026-12-31 23:59").getTime();

            // 2. Start timer immediately (Target Time - Current Time)
            timer = new CountDownTimer(target - System.currentTimeMillis(), 1000) {
                public void onTick(long ms) {
                    // 3. Do the math directly inside the text formatter
                    tv.setText(String.format(Locale.US, "%02d Days %02d:%02d:%02d",
                            ms / 86400000,          // Days
                            (ms / 3600000) % 24,    // Hours
                            (ms / 60000) % 60,      // Minutes
                            (ms / 1000) % 60));     // Seconds
                }
                public void onFinish() {
                    tv.setText("Event Started!");
                }
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int state = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS);
            if (state != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }


    private void fetchRacesAPI() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://api.openf1.org/v1/meetings?year=2026";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        races.clear();
                        racesGrid.removeAllViews();
                        boolean adjustTime = true;

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);

                            String name = obj.optString("meeting_name", "Unknown Race");
                            if (Arrays.asList(BannedRaces).contains(name)) continue;
                            
                            int id = obj.optInt("meeting_key", i);
                            String location = obj.optString("location", "Unknown Location");
                            String startDate = obj.optString("date_start", "");
                            String endDate = obj.optString("date_end", "");
                            String flag = obj.optString("country_flag", "");
                            String circuit = obj.optString("circuit_image", "");

                            if (obj.optString("is_cancelled", "false").equals("false")) {
                                Race race = new Race(id, name, location, startDate, endDate, circuit, flag);

                                // Stop adjusting if we reached the United States Grand Prix
                                if (name.toLowerCase().contains("united states")) {
                                    adjustTime = false;
                                }

                                if (adjustTime) {
                                    race.startDate = race.startDate.plusHours(1);
                                    race.endDate = race.endDate.plusHours(1);
                                }

                                // Stop adjusting after the Singapore Grand Prix (so Singapore is adjusted, but next ones are not)
                                if (name.toLowerCase().contains("singapore")) {
                                    adjustTime = false;
                                }

                                races.add(race);
                                scheduleRaceNotifications(race);

                                if (i > 0) {
                                    addRaceToGrid(race);
                                }
                            }
                        }

                        if (!races.isEmpty()) {
                            // Find the real next race based on current date
                            Race nextRace = null;
                            long currentTime = System.currentTimeMillis();

                            for (Race race : races) {
                                if (race.endDate.toInstant().toEpochMilli() > currentTime) {
                                    nextRace = race;
                                    break;
                                }
                            }

                            // Fallback to first race if all are in the past
                            if (nextRace == null) nextRace = races.get(0);

                            raceTextView.setText(nextRace.getName() + "\n" + nextRace.getLocation());
                            if (!nextRace.getFlag().isEmpty()) {
                                Picasso.get().load(nextRace.getFlag()).into(image);
                            }

                            final Race finalNextRace = nextRace;
                            LinearLayout nextRaceContainer = findViewById(R.id.next_race_container);
                            nextRaceContainer.setOnClickListener(v -> showRaceDetailDialog(finalNextRace));

                            updateCountdown(nextRace);
                        }
                    } catch (Exception e) {
                        Log.e("F1_DATA", "JSON Error: " + e.getMessage());
                        Toast.makeText(this, "Data Error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("F1_DATA", "Volley Error: " + error.toString());
                    Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show();
                });

        queue.add(stringRequest);
    }

    private void updateCountdown(Race nextRace) {
        TextView tv = findViewById(R.id.tvCountdown);
        if (timer != null) {
            timer.cancel();
        }

        long target = nextRace.endDate.toInstant().toEpochMilli();
        long diff = target - System.currentTimeMillis();

        timer = new CountDownTimer(diff, 1000) {
            public void onTick(long ms) {
                tv.setText(String.format(Locale.US, "%02d Days %02d:%02d:%02d",
                        ms / 86400000,
                        (ms / 3600000) % 24,
                        (ms / 60000) % 60,
                        (ms / 1000) % 60));
            }

            public void onFinish() {
                tv.setText("Race Started!");
            }
        }.start();
    }

    private void scheduleRaceNotifications(Race race) {
        long now = System.currentTimeMillis();

        // 1. Notification 10 minutes before race starts
        long beforeStartTime = race.endDate.toInstant().toEpochMilli() - (10 * 60 * 1000);
        if (beforeStartTime > now) {
            NotificationHelper.scheduleNotification(this, race.getId() * 10, beforeStartTime,
                    "Race Starting Soon!", race.getName() + " starts in 10 minutes.", null);
        }

        // 2. Notification 2 hours after race ends
        long afterEndTime = race.endDate.toInstant().toEpochMilli() + (2 * 60 * 60 * 1000);
        if (afterEndTime > now) {
            int notificationId = race.getId() * 10 + 1;
            
            Intent watched = new Intent(this, RaceActionReceiver.class)
                    .setAction(RaceActionReceiver.ACTION_WATCHED)
                    .putExtra(RaceActionReceiver.EXTRA_RACE_ID, race.getId())
                    .putExtra(RaceActionReceiver.EXTRA_RACE_NAME, race.getName())
                    .putExtra(RaceActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);

            Intent skipped = new Intent(this, RaceActionReceiver.class)
                    .setAction(RaceActionReceiver.ACTION_SKIPPED)
                    .putExtra(RaceActionReceiver.EXTRA_RACE_ID, race.getId())
                    .putExtra(RaceActionReceiver.EXTRA_RACE_NAME, race.getName())
                    .putExtra(RaceActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);

            List<NotificationHelper.Action> actions = Arrays.asList(
                    new NotificationHelper.Action("✓ Watched", watched, true),
                    new NotificationHelper.Action("✗ Skipped", skipped, true)
            );

            NotificationHelper.scheduleNotification(this, notificationId, afterEndTime,
                    "Did you watch?", "The " + race.getName() + " ended 2 hours ago.", actions);
        }
    }

    private void addRaceToGrid(Race race) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_race, racesGrid, false);
        
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(8, 8, 8, 8);
        itemView.setLayoutParams(params);

        TextView name = itemView.findViewById(R.id.item_race_name);
        TextView date = itemView.findViewById(R.id.item_race_date);
        ImageView flag = itemView.findViewById(R.id.item_flag);

        name.setText(race.getName());
        // Use getStartDate() to show the correct local time (handles DST/Winter time)
        date.setText(race.getEndDate());
        if (!race.getFlag().isEmpty()) {
            Picasso.get().load(race.getFlag()).into(flag);
        }

        itemView.setBackgroundResource(R.drawable.rounded_card_bg);
        itemView.setOnClickListener(v -> showRaceDetailDialog(race));

        racesGrid.addView(itemView);
    }

    private void showRaceDetailDialog(Race race) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_race_detail, null);
        builder.setView(dialogView);

        ImageView flagImg = dialogView.findViewById(R.id.dialog_flag);
        TextView nameTxt = dialogView.findViewById(R.id.dialog_race_name);
        TextView locationTxt = dialogView.findViewById(R.id.dialog_location);
        TextView dateTxt = dialogView.findViewById(R.id.dialog_date);
        ImageView circuitImg = dialogView.findViewById(R.id.dialog_circuit);
        Button closeBtn = dialogView.findViewById(R.id.dialog_close_button);

        nameTxt.setText(race.getName());
        locationTxt.setText("📍 " + race.getLocation());
        // Use getStartDate() to show the correct local time
        dateTxt.setText("📅 " + race.getEndDate());
        
        if (!race.getFlag().isEmpty()) {
            Picasso.get().load(race.getFlag()).into(flagImg);
        }
        if (!race.getCircuit().isEmpty()) {
            Picasso.get().load(race.getCircuit()).into(circuitImg);
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.alertdialogbox);
        }
        
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onClick(View v) {
        // ... existing logic
    }
}
