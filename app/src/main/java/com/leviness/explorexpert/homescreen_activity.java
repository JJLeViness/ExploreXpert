package com.leviness.explorexpert;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.libraries.places.api.model.Place;

import com.google.android.material.navigation.NavigationView;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class homescreen_activity extends AppCompatActivity {

    private EditText fromSearch;
    private EditText toSearch;
    private AppCompatButton navigateButton;
    private ImageView map;
    private ImageView menuButton;
    private DrawerLayout menuNavigation;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;

    private static final int AUTOCOMPLETE_REQUEST_CODE_FROM = 1;
    private static final int AUTOCOMPLETE_REQUEST_CODE_TO = 2;

    private String fromLatLng = null;
    private String toLatLng = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homescreen);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.maps_api_key));
        }

        PlacesClient placesClient = Places.createClient(this);


        fromSearch = findViewById(R.id.fromSearch);
        toSearch = findViewById(R.id.toSearch);
        navigateButton = findViewById(R.id.navigateButton);
        map = findViewById(R.id.map);
        menuButton = findViewById(R.id.menuButton);
        menuNavigation = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.menu_navigation);

        fromSearch.setOnClickListener(v -> openAutocomplete(AUTOCOMPLETE_REQUEST_CODE_FROM));
        toSearch.setOnClickListener(v -> openAutocomplete(AUTOCOMPLETE_REQUEST_CODE_TO));

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

        navigateButton.setOnClickListener(v -> {  //Currently moves the user  to map Activity with entered info from to and from text
            //For testing purposes of places and routes API. Function will change later.
            Intent intent = new Intent(homescreen_activity.this, Map_Activity.class);
            intent.putExtra("fromLatLng", fromLatLng);
            intent.putExtra("toLatLng", toLatLng);
            startActivity(intent);
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
                    // Handle logout logic here
                }

                menuNavigation.closeDrawer(GravityCompat.END);
                return true;
            }
        });



    }

    private void openAutocomplete(int requestCode) {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(this);
        startActivityForResult(intent, requestCode);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE_FROM || requestCode == AUTOCOMPLETE_REQUEST_CODE_TO) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                if (requestCode == AUTOCOMPLETE_REQUEST_CODE_FROM) {
                    fromSearch.setText(place.getName());
                    fromLatLng = Objects.requireNonNull(place.getLatLng()).latitude + "," + place.getLatLng().longitude;
                } else if (requestCode == AUTOCOMPLETE_REQUEST_CODE_TO) {
                    toSearch.setText(place.getName());
                    toLatLng = Objects.requireNonNull(place.getLatLng()).latitude + "," + place.getLatLng().longitude;
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.e("AutocompleteError", status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {

            }
        }
    }
}


