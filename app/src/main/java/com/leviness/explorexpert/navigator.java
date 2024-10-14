package com.leviness.explorexpert;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
    private int currentTaskIndex = 0;
    private TextView destinationNameTextView;
    private Button nextTaskButton;

    private Location currentLocation; // User's current location
    private boolean isTaskCompleted = false; // To avoid multiple triggers for the same task

    private static final float PROXIMITY_THRESHOLD = 10f;



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
        destinationNameTextView = findViewById(R.id.destination_name);
        nextTaskButton = findViewById(R.id.next_task_button);


        // Fetch scavenger hunt from intent
        hunt = getIntent().getParcelableExtra("hunt");

        fromLatLng = getIntent().getStringExtra("fromLatLng");
        toLatLng = getIntent().getStringExtra("toLatLng");

         if (hunt != null) {
            isScavengerHuntActive = true;
            Toast.makeText(this, "Starting scavenger hunt: " + hunt.getName(), Toast.LENGTH_SHORT).show();
             nextTaskButton.setVisibility(View.VISIBLE);
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

        nextTaskButton.setOnClickListener(v -> moveToNextTask());  //for emulator testing
        //uncomment for non emulator testing
        //startLocationUpdates();

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


            // Move the camera to the "from" location
            if (mMap != null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fromLatLngParsed, 15));
            }

            // Start RoutesTask to navigate between "from" and "to" places
            new RoutesTask(this, mMap, directionsAdapter, "walking").execute(fromLatLng, toLatLng);

        }
        else if (isScavengerHuntActive && hunt != null && !hunt.getTasks().isEmpty()) {
            // Fetch current location
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    //uncomment current location for non emulator use
                    //LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    LatLng nycLocation = new LatLng(40.7870, -73.9754);
                    scavengerHuntTask firstTask = hunt.getTasks().get(0);


                    // Move camera to starting location, change to current location for non emulator use.
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nycLocation, 15));

                    destinationNameTextView.setText("Destination: " + firstTask.getPlaceName());


                    // Start RoutesTask to navigate between current location and first task, CHANGE TO CURRENT LOCATION FOR NON EMULATOR USE
                    navigateToTask(nycLocation, hunt.getTasks().get(currentTaskIndex).getLocation());
                } else {
                    Toast.makeText(navigator.this, "Unable to fetch current location.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void startLocationUpdates() {
        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // Create a LocationRequest
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Create a LocationCallback
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                // Get the most recent location
                Location currentLocation = locationResult.getLastLocation();

                // If scavenger hunt is active, check if the user has reached the destination
                if (isScavengerHuntActive && !isTaskCompleted && currentTaskIndex < hunt.getTasks().size()) {
                    LatLng taskLocation = hunt.getTasks().get(currentTaskIndex).getLocation();
                    Location taskLoc = new Location("");
                    taskLoc.setLatitude(taskLocation.latitude);
                    taskLoc.setLongitude(taskLocation.longitude);

                    // Check if user is within the proximity threshold of the destination
                    float distanceToDestination = currentLocation.distanceTo(taskLoc);
                    if (distanceToDestination < PROXIMITY_THRESHOLD) {
                        isTaskCompleted = true;
                        Toast.makeText(navigator.this, "You have reached the destination!", Toast.LENGTH_SHORT).show();

                        // Move to the next task or finish the hunt
                        moveToNextTask();
                    }
                }
            }
        };

        // Request location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }


    private void moveToNextTask() {
        if (isScavengerHuntActive && hunt != null && currentTaskIndex < hunt.getTasks().size() - 1) {
            currentTaskIndex++;
            scavengerHuntTask nextTask = hunt.getTasks().get(currentTaskIndex);
            LatLng previousTaskLocation = hunt.getTasks().get(currentTaskIndex - 1).getLocation();

            // Navigate from the previous task to the next task
            navigateToTask(previousTaskLocation, nextTask.getLocation());

            // Update the destination name and UI
            destinationNameTextView.setText("Destination: " + nextTask.getPlaceName());
        } else {
            // All tasks completed, update UI accordingly
            Toast.makeText(this, "All tasks completed!", Toast.LENGTH_SHORT).show();

            // Hide the "Next Task" button
            nextTaskButton.setVisibility(View.GONE);

            // Update the destination name to show "Scavenger Hunt Complete"
            destinationNameTextView.setText("Scavenger Hunt Complete!");
            scavengerHuntTask finalTask = hunt.getTasks().get(currentTaskIndex); // Get the final task
            LatLng finalTaskLocation = finalTask.getLocation(); // Get the final task location
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(finalTaskLocation, 15));
        }
    }

    private void navigateToTask(LatLng from, LatLng to) {
        if (mMap != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(from, 15));

            // Start RoutesTask to navigate between two tasks
            String fromLocation = formatLatLng(from);
            String toLocation = formatLatLng(to);
            new RoutesTask(this, mMap, directionsAdapter, "walking").execute(fromLocation, toLocation);
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