package com.leviness.explorexpert;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.Manifest;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

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

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.navigation.NavigationView;
import com.leviness.explorexpert.network.RoutesApiTask;

public class Map_Activity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private ImageView menuButton;
    private DrawerLayout menuNavigation;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;
    private LatLng currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_map);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        menuButton = findViewById(R.id.map_menuButton);
        menuNavigation = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.menu_navigation);


        //Menu drawer
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

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    }

//Map Logic and snap to current location
    @Override
    public void onMapReady(GoogleMap googleMap) {



        mMap = googleMap;
        // Check if the location permissions are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Request the permissions if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }




        mMap.setMyLocationEnabled(true);

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true); // Ensures that the user can scroll/pan the map
        mMap.getUiSettings().setZoomGesturesEnabled(true);   // Allows zooming

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {

                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());


                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));


                        mMap.addMarker(new MarkerOptions().position(currentLocation).title("You are here"));



                        String origin = currentLocation.latitude + "," + currentLocation.longitude;
                        String destination = "37.7749,-122.4194"; // Example destination (San Francisco coordinates)
                        new RoutesApiTask(this, mMap).execute(origin, destination);


                    }
                });
//Menu Drawer logic
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    startActivity(new Intent(Map_Activity.this, homescreen_activity.class));
                } else if (id == R.id.nav_map) {
                    startActivity(new Intent(Map_Activity.this, Map_Activity.class));
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(Map_Activity.this, profile_Activity.class));
                } else if (id == R.id.nav_scavenger_hunt) {
                    startActivity(new Intent(Map_Activity.this, scavenger_Hunt_Activity.class));
                } else if (id == R.id.nav_settings) {
                    startActivity(new Intent(Map_Activity.this, settings_Activity.class));
                } else if (id == R.id.nav_login) {
                    startActivity(new Intent(Map_Activity.this, login_Activity.class));
                } else if (id == R.id.nav_logout) {
                    // Handle logout logic here
                }

                menuNavigation.closeDrawer(GravityCompat.END);
                return true;
            }
        });





    }




}