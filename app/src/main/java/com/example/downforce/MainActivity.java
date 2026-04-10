package com.example.downforce;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

public class MainActivity extends AppCompatActivity {

    private TextView raceTextView;
    private ImageView image;
    private GridLayout racesGrid;
    private ArrayList<Race> races;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.calender) {
            Intent calender = new Intent(this, MainActivity.class);
            startActivity(calender);
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

        // Initialize Views
        raceTextView = findViewById(R.id.text);
        image = findViewById(R.id.image);
        racesGrid = findViewById(R.id.races_grid);

        races = new ArrayList<>();
        fetchRacesAPI();
    }

    private void fetchRacesAPI() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://api.openf1.org/v1/meetings?year=2024";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        races.clear();
                        racesGrid.removeAllViews();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            String name = obj.getString("meeting_name");
                            String location = obj.getString("location");
                            String date = obj.getString("date_end");
                            String flag = obj.optString("country_flag", "");

                            Race race = new Race(name, location, date, flag);
                            races.add(race);
                            
                            // Add to Grid after the first one (which is featured)
                            if (i > 0) {
                                addRaceToGrid(race);
                            }
                        }

                        // Update Featured Race (First one)
                        if (!races.isEmpty()) {
                            Race nextRace = races.get(0);
                            raceTextView.setText(nextRace.getName() + "\n" + nextRace.getLocation());
                            if (!nextRace.getFlag().isEmpty()) {
                                Picasso.get().load(nextRace.getFlag()).into(image);
                            }
                        }

                    } catch (Exception e) {
                        Log.e("F1_DATA", "JSON Error: " + e.getMessage());
                    }
                },
                error -> Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show());

        queue.add(stringRequest);
    }

    private void addRaceToGrid(Race race) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_race, racesGrid, false);
        
        // Adjust layout params for 2-column grid
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

        racesGrid.addView(itemView);
    }
}
