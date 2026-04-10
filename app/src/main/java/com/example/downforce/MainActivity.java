package com.example.downforce;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
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
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList; // Keep this import



public class MainActivity extends AppCompatActivity {

    public TextView raceTextView;
    public ImageView image;
    // CHANGE 1: Explicitly initialize as a new ArrayList
    public ArrayList<Race> races ;
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


        // View setup...
        this.raceTextView = findViewById(R.id.text);
        this.image = findViewById(R.id.image);
        this.races = new ArrayList<>();

        // Just start the request. Don't try to use 'races' here!
        fetchRacesAPI();



    }


    private void fetchRacesAPI() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://api.openf1.org/v1/meetings?year=2026";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray jsonArray = new JSONArray(response);

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);

                            String name = obj.getString("meeting_name");
                            String location = obj.getString("location");
                            String date = obj.getString("date_end");
                            String flag = obj.getString("country_flag");

                            this.races.add(new Race(name, location, date, flag));
                        }

                        // CHANGE 2: Move the UI update INSIDE the success block
                        // This prevents the "Keep Stopping" crash because data is now ready
                        if (!races.isEmpty()) {
                            this.raceTextView.setText(races.get(6).getName() + "\n" +
                                    races.get(6).getLocation() + "\n" +
                                    races.get(6).getDate());
                            //this.image.setImageBitmap(this.races.get(6).getAverageFlagColor());
                        }



                    } catch (Exception e) {
                        Log.e("F1_DATA", "JSON Error: " + e.getMessage());
                    }
                },
                error -> Toast.makeText(this, "Internet Error", Toast.LENGTH_SHORT).show());

        queue.add(stringRequest);
    }
}
