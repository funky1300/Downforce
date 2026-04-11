package com.example.downforce;

import android.os.Bundle;
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
import androidx.appcompat.widget.Toolbar;
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
import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    private TextView raceTextView;
    private ImageView image;
    private GridLayout racesGrid;
    private ArrayList<Race> races;

    private String[] BannedRaces = {"Bahrain Grand Prix", "Saudi Arabian Grand Prix"};


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
        return super.onOptionsItemSelected(item);
    }

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



        raceTextView = findViewById(R.id.text);
        image = findViewById(R.id.image);
        racesGrid = findViewById(R.id.races_grid);

        races = new ArrayList<>();
        fetchRacesAPI();
    }

    private void fetchRacesAPI() {
        RequestQueue queue = Volley.newRequestQueue(this);
        // Changed back to 2024 because 2026 might not have schedule data yet
        String url = "https://api.openf1.org/v1/meetings?year=2026";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        races.clear();
                        racesGrid.removeAllViews();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);

                            String name = obj.optString("meeting_name", "Unknown Race");
                            if (Arrays.asList(BannedRaces).contains(name)) continue;
                            String location = obj.optString("location", "Unknown Location");
                            String date = obj.optString("date_end", "");
                            String flag = obj.optString("country_flag", "");
                            String circuit = obj.optString("circuit_image", "");

                            if (!date.isEmpty()) {
                                // Important: Match the constructor order in Race.java
                                Race race = new Race(name, location, date, circuit, flag);
                                races.add(race);
                                
                                if (i > 0) {
                                    addRaceToGrid(race);
                                }
                            }
                        }

                        if (!races.isEmpty()) {
                            Race nextRace = races.get(0);
                            raceTextView.setText(nextRace.getName() + "\n" + nextRace.getLocation());
                            if (!nextRace.getFlag().isEmpty()) {
                                Picasso.get().load(nextRace.getFlag()).into(image);
                            }
                            
                            LinearLayout nextRaceContainer = findViewById(R.id.next_race_container);
                            nextRaceContainer.setOnClickListener(v -> showRaceDetailDialog(nextRace));
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
        date.setText(race.getDate());
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
        Button closeBtn = dialogView.findViewById(R.id.dialog_close_button);

        nameTxt.setText(race.getName());
        locationTxt.setText("📍 " + race.getLocation());
        dateTxt.setText("📅 " + race.getDate());
        
        if (!race.getFlag().isEmpty()) {
            Picasso.get().load(race.getFlag()).into(flagImg);
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
