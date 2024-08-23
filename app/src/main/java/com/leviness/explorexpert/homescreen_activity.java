package com.leviness.explorexpert;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.navigation.NavigationView;

public class homescreen_activity extends AppCompatActivity {

    private EditText fromSearch;
    private EditText toSearch;
    private AppCompatButton navigateButton;
    private ImageView menuButton;
    private DrawerLayout menuNavigation;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homescreen);

        fromSearch = findViewById(R.id.fromSearch);
        toSearch = findViewById(R.id.toSearch);
        navigateButton = findViewById(R.id.navigateButton);
        menuButton = findViewById(R.id.menuButton);
        menuNavigation = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.menu_navigation);

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
                    startActivity(new Intent(homescreen_activity.this, homescreen_activity.class));
                } else if (id == R.id.nav_map) {
                    startActivity(new Intent(homescreen_activity.this, Map_Activity.class));
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(homescreen_activity.this, profile_Activity.class));
                } else if (id == R.id.nav_scavenger_hunt) {
                    startActivity(new Intent(homescreen_activity.this, scavenger_Hunt_Activity.class));
                } else if (id == R.id.nav_settings) {
                    startActivity(new Intent(homescreen_activity.this, settings_Activity.class));
                    } else if (id == R.id.nav_login) {
                    startActivity(new Intent(homescreen_activity.this, login_Activity.class));
                } else if (id == R.id.nav_logout) {
                    // Handle logout logic here, May be removed from menu drawer. Logout Logic in login_Activity.
                }

                menuNavigation.closeDrawer(GravityCompat.END);
                return true;
            }
        });

        // Initialize the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                // Configure the map (e.g., set markers, camera position, etc.)
            }
        });
    }
}
