package com.example.downforce;

import android.os.Bundle;
import android.widget.Toast;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String raceName;
    private String raceLocation;

    // Hardcoded F1 circuit coordinates (lat, lng)
    private static Map<String, double[]> CIRCUIT_COORDS = new HashMap<>();
    static {
        CIRCUIT_COORDS.put("bahrain",        new double[]{26.0325,  50.5106});
        CIRCUIT_COORDS.put("saudi",          new double[]{21.6319,  39.1044});
        CIRCUIT_COORDS.put("jeddah",         new double[]{21.6319,  39.1044});
        CIRCUIT_COORDS.put("australia",      new double[]{-37.8497, 144.9680});
        CIRCUIT_COORDS.put("melbourne",      new double[]{-37.8497, 144.9680});
        CIRCUIT_COORDS.put("japan",          new double[]{34.8431,  136.5410});
        CIRCUIT_COORDS.put("suzuka",         new double[]{34.8431,  136.5410});
        CIRCUIT_COORDS.put("china",          new double[]{31.3389,  121.2197});
        CIRCUIT_COORDS.put("shanghai",       new double[]{31.3389,  121.2197});
        CIRCUIT_COORDS.put("miami",          new double[]{25.9581,  -80.2389});
        CIRCUIT_COORDS.put("imola",          new double[]{44.3439,   11.7167});
        CIRCUIT_COORDS.put("monaco",         new double[]{43.7347,    7.4205});
        CIRCUIT_COORDS.put("canada",         new double[]{45.5048,  -73.5222});
        CIRCUIT_COORDS.put("montreal",       new double[]{45.5048,  -73.5222});
        CIRCUIT_COORDS.put("spain",          new double[]{41.5700,    2.2611});
        CIRCUIT_COORDS.put("barcelona",      new double[]{41.5700,    2.2611});
        CIRCUIT_COORDS.put("austria",        new double[]{47.2197,   14.7647});
        CIRCUIT_COORDS.put("spielberg",      new double[]{47.2197,   14.7647});
        CIRCUIT_COORDS.put("great britain",  new double[]{52.0786,   -1.0169});
        CIRCUIT_COORDS.put("silverstone",    new double[]{52.0786,   -1.0169});
        CIRCUIT_COORDS.put("hungary",        new double[]{47.5830,   19.2526});
        CIRCUIT_COORDS.put("budapest",       new double[]{47.5830,   19.2526});
        CIRCUIT_COORDS.put("belgium",        new double[]{50.4372,    5.9714});
        CIRCUIT_COORDS.put("spa",            new double[]{50.4372,    5.9714});
        CIRCUIT_COORDS.put("netherlands",    new double[]{52.3888,    4.5408});
        CIRCUIT_COORDS.put("zandvoort",      new double[]{52.3888,    4.5408});
        CIRCUIT_COORDS.put("italy",          new double[]{45.6156,    9.2811});
        CIRCUIT_COORDS.put("monza",          new double[]{45.6156,    9.2811});
        CIRCUIT_COORDS.put("azerbaijan",     new double[]{40.3725,   49.8533});
        CIRCUIT_COORDS.put("baku",           new double[]{40.3725,   49.8533});
        CIRCUIT_COORDS.put("singapore",      new double[]{1.2914,   103.8640});
        CIRCUIT_COORDS.put("united states",  new double[]{30.1328,  -97.6411});
        CIRCUIT_COORDS.put("austin",         new double[]{30.1328,  -97.6411});
        CIRCUIT_COORDS.put("mexico",         new double[]{19.4042,  -99.0907});
        CIRCUIT_COORDS.put("brazil",         new double[]{-23.7036, -46.6997});
        CIRCUIT_COORDS.put("são paulo",      new double[]{-23.7036, -46.6997});
        CIRCUIT_COORDS.put("sao paulo",      new double[]{-23.7036, -46.6997});
        CIRCUIT_COORDS.put("las vegas",      new double[]{36.1147, -115.1728});
        CIRCUIT_COORDS.put("qatar",          new double[]{25.4900,   51.4542});
        CIRCUIT_COORDS.put("lusail",         new double[]{25.4900,   51.4542});
        CIRCUIT_COORDS.put("abu dhabi",      new double[]{24.4672,   54.6031});
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        raceName = getIntent().getStringExtra("race_name");
        raceLocation = getIntent().getStringExtra("race_location");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng latLng = findCircuitCoords();

        if (latLng != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(raceName));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
        } else {
            Toast.makeText(this, "Circuit location not found", Toast.LENGTH_SHORT).show();
        }
    }

    private LatLng findCircuitCoords() {
        String searchName = (raceName + " " + raceLocation).toLowerCase();

        for (Map.Entry<String, double[]> entry : CIRCUIT_COORDS.entrySet()) {
            if (searchName.contains(entry.getKey())) {
                double[] coords = entry.getValue();
                return new LatLng(coords[0], coords[1]);
            }
        }
        return null;
    }
}
