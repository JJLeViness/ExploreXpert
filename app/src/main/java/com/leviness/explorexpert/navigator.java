package com.leviness.explorexpert;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.navigation.NavigationView;
import com.leviness.explorexpert.network.DirectionsAdapter;
import com.leviness.explorexpert.network.RoutesTask;

import java.util.ArrayList;
import java.util.List;

public class navigator extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private scavengerHunt hunt;
    private boolean isScavengerHuntActive = false;
    private ImageView menuButton;
    private DrawerLayout menuNavigation;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;

    private List<String> directionsList = new ArrayList<>();
    private List<LatLng> stepLatLngs = new ArrayList<>();
    private DirectionsAdapter directionsAdapter;
    private FusedLocationProviderClient fusedLocationClient;

    private String fromLatLng;
    private String toLatLng;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_navigator);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });


        // Set up RecyclerView for directions
        RecyclerView directionsRecyclerView = findViewById(R.id.directionsRecyclerView);
        directionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the DirectionsAdapter with an empty list
        directionsAdapter = new DirectionsAdapter(directionsList, stepLatLngs, directionsRecyclerView, mMap);
        directionsRecyclerView.setAdapter(directionsAdapter);

        menuButton = findViewById(R.id.navigator_menuButton);
        menuNavigation = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.menu_navigation);


        // Fetch scavenger hunt from intent
        hunt = getIntent().getParcelableExtra("hunt");

        fromLatLng = getIntent().getStringExtra("fromLatLng");
        toLatLng = getIntent().getStringExtra("toLatLng");

         if (hunt != null) {
            isScavengerHuntActive = true;
            Toast.makeText(this, "Starting scavenger hunt: " + hunt.getName(), Toast.LENGTH_SHORT).show();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize fused location provider client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        toggle = new ActionBarDrawerToggle(this, menuNavigation, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        menuNavigation.addDrawerListener(toggle);
        toggle.syncState();
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (menuNavigation.isDrawerOpen(GravityCompat.END)) {
                    menuNavigation.closeDrawer(GravityCompat.END);
                } else {
                    menuNavigation.openDrawer(GravityCompat.END);
                }
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    startActivity(new Intent(navigator.this, homescreen_activity.class));
                } else if (id == R.id.nav_map) {
                    startActivity(new Intent(navigator.this, Map_Activity.class));
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(navigator.this, profile_Activity.class));
                } else if (id == R.id.nav_scavenger_hunt) {
                    startActivity(new Intent(navigator.this, scavenger_Hunt_Activity.class));
                } else if (id == R.id.nav_settings) {
                    startActivity(new Intent(navigator.this, settings_Activity.class));
                } else if (id == R.id.nav_login) {
                    startActivity(new Intent(navigator.this, login_Activity.class));
                }
                menuNavigation.closeDrawer(GravityCompat.END);
                return true;
            }
        });


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        directionsAdapter.setMap(mMap);


        // Check if the location permissions are granted
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Request the permissions if not granted
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }

        mMap.setMyLocationEnabled(true);

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true); // Ensures that the user can scroll/pan the map
        mMap.getUiSettings().setZoomGesturesEnabled(true);   // Allows zooming

        if (fromLatLng != null && toLatLng != null) {
            // Use the locations from the home screen for navigation
            LatLng fromLatLngParsed = parseLatLng(fromLatLng);
            LatLng toLatLngParsed = parseLatLng(toLatLng);

            // Move the camera to the "from" location
            if (mMap != null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fromLatLngParsed, 15));
            }

            // Start RoutesTask to navigate between "from" and "to" places
            new RoutesTask(this, mMap, directionsAdapter, "walking").execute(fromLatLng, toLatLng);

        }
        else if (isScavengerHuntActive && hunt != null && !hunt.getTasks().isEmpty()) {
            // Use the first and second tasks for navigation
            scavengerHuntTask firstTask = hunt.getTasks().get(0);
            LatLng firstTaskLocation = firstTask.getLocation();
            scavengerHuntTask secondTask = hunt.getTasks().get(1);

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstTaskLocation, 15));

            // Get locations as strings (lat, long) for RoutesTask
            String firstPlace = formatLatLng(firstTask.getLocation());
            String secondPlace = formatLatLng(secondTask.getLocation());

            // Start RoutesTask to navigate between first and second places
            new RoutesTask(this, mMap, directionsAdapter, "walking").execute(firstPlace, secondPlace);
        }
    }

    public void onStepClick(LatLng stepLatLng) {
        if (mMap != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stepLatLng, 18));
        }
    }

    // Helper method to format LatLng into the "lat,lng" string format
    private String formatLatLng(LatLng latLng) {
        return latLng.latitude + "," + latLng.longitude;
    }
    private LatLng parseLatLng(String latLngString) {
        String[] parts = latLngString.split(",");
        double lat = Double.parseDouble(parts[0]);
        double lng = Double.parseDouble(parts[1]);
        return new LatLng(lat, lng);
    }
}