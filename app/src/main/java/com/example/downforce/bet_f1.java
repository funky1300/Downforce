package com.example.downforce;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class bet_f1 extends AppCompatActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.calender) {
            Intent goGu = new Intent(this, MainActivity.class);
            startActivity(goGu);
        }
        if(item.getItemId() == R.id.stats){
            Intent goGu = new Intent(this, downforce_stats.class);
            startActivity(goGu);
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
            // Inside bet_f1.java onCreate, after the insets listener:
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new BettingFragment())
                    .commit();
        });
        //^^^do not touch this function^^^


    }
}