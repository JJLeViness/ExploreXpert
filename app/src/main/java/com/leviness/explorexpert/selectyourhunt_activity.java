package com.leviness.explorexpert;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.leviness.explorexpert.network.KnowledgeGraphAPIClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class selectyourhunt_activity extends AppCompatActivity {

    private List<scavengerHunt> scavengerHuntList = new ArrayList<>();
    private KnowledgeGraphAPIClient knowledgeGraphAPIClient;
    private PlacesClient placesClient;
    private LatLng manhattanLocation = new LatLng(40.7831, -73.9712); // TESTING
    private DrawerLayout menuNavigation;
    private NavigationView navigationView;
    private ImageView menuButton;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectyourhunt);
        
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        GridLayout linksGrid = findViewById(R.id.linksGrid);
        menuNavigation = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.menu_navigation);
        menuButton = findViewById(R.id.menuButton);

        // Initialize the Google Places API
        String apiKey = getString(R.string.maps_api_key);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(this);
        knowledgeGraphAPIClient = new KnowledgeGraphAPIClient(apiKey);

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
                    startActivity(new Intent(selectyourhunt_activity.this, homescreen_activity.class));
                } else if (id == R.id.nav_map) {
                    startActivity(new Intent(selectyourhunt_activity.this, Map_Activity.class));
                } else if (id == R.id.nav_profile) {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        startActivity(new Intent(selectyourhunt_activity.this, profile_Activity.class));
                    } else {
                        startActivity(new Intent(selectyourhunt_activity.this, login_Activity.class));
                    }
                } else if (id == R.id.nav_scavenger_hunt) {
                    startActivity(new Intent(selectyourhunt_activity.this, selectyourhunt_activity.class));
                } else if (id == R.id.nav_settings) {
                    startActivity(new Intent(selectyourhunt_activity.this, settings_Activity.class));
                } else if (id == R.id.nav_login) {
                    startActivity(new Intent(selectyourhunt_activity.this, login_Activity.class));
                }
                menuNavigation.closeDrawer(GravityCompat.END);
                return true;
            }
        });
        // Populate scavenger hunts into the GridLayout
        populateScavengerHunts(linksGrid);
    }

    private void populateScavengerHunts(GridLayout linksGrid) {



        // Creating and adding scavenger hunts
        createScavengerHunt("Manhattan Landmarks Hunt", "tourist_attraction|museum|point_of_interest", "Discover famous landmarks in Manhattan.", linksGrid);
        createScavengerHunt("Art Installations Hunt", "art_gallery", "Explore stunning art installations across Manhattan.", linksGrid);
        createScavengerHunt("Parks & Nature Hunt", "park", "Visit beautiful parks and green spaces in Manhattan.", linksGrid);
        createScavengerHunt("Historic Buildings Hunt", "point_of_interest", "Explore the historic buildings in Manhattan.", linksGrid); // 'building' replaced with 'point_of_interest'
        createScavengerHunt("Food & Drink Tour", "restaurant|cafe|bar", "Discover the best food and drink spots in the city.", linksGrid);
        createScavengerHunt("Street Art Walk", "art_gallery|point_of_interest", "Find the hidden street art murals across the city.", linksGrid); // 'landmark' replaced with 'point_of_interest'
        createScavengerHunt("Theater District Hunt", "movie_theater|point_of_interest", "Explore the famous theaters and cultural spots.", linksGrid); // 'theater' replaced with 'point_of_interest'
        createScavengerHunt("Shopping Spree Hunt", "shopping_mall|clothing_store", "Discover the best shopping destinations in Manhattan.", linksGrid);
        createScavengerHunt("Bridges of Manhattan", "point_of_interest", "Take a tour of the iconic bridges of Manhattan.", linksGrid); // 'bridge' replaced with 'point_of_interest'
        createScavengerHunt("Hidden Gems Hunt", "point_of_interest", "Explore the lesser-known hidden gems of the city.", linksGrid);

    }

    private void createScavengerHunt(String huntName, String placeTypes, String huntDescription, GridLayout linksGrid) {
        String nearbySearchUrl = getNearbySearchUrl(manhattanLocation, placeTypes);
        new NearbyPlacesTask(huntName, huntDescription, linksGrid).execute(nearbySearchUrl);
    }

    private String getNearbySearchUrl(LatLng location, String placeTypes) {
        String apiKey = getString(R.string.maps_api_key);
        return "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + location.latitude + "," + location.longitude
                + "&radius=2000&type=" + placeTypes + "&key=" + apiKey;
    }

    // AsyncTask to fetch places using Nearby Search and update the grid
    private class NearbyPlacesTask extends AsyncTask<String, Void, String> {
        private String huntName;
        private String huntDescription;
        private GridLayout linksGrid;

        public NearbyPlacesTask(String huntName, String huntDescription, GridLayout linksGrid) {
            this.huntName = huntName;
            this.huntDescription = huntDescription;
            this.linksGrid = linksGrid;
        }

        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                reader.close();
                return stringBuilder.toString();
            } catch (Exception e) {
                Log.e("NearbyPlacesTask", "Error in getting nearby places", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONArray results = jsonObject.getJSONArray("results");

                    List<scavengerHuntTask> taskList = new ArrayList<>();

                    // Parse the places data and add to scavengerHuntTask list
                    for (int i = 0; i < Math.min(results.length(), 5); i++) {  // Limiting to 5 places for sample
                        JSONObject place = results.getJSONObject(i);
                        String placeName = place.getString("name");
                        JSONObject geometry = place.getJSONObject("geometry").getJSONObject("location");
                        LatLng placeLocation = new LatLng(geometry.getDouble("lat"), geometry.getDouble("lng"));
                        taskList.add(new scavengerHuntTask(placeName, "Description of " + placeName, placeLocation));
                    }

                    // Create the scavengerHunt object
                    scavengerHunt hunt = new scavengerHunt(huntName, huntDescription, taskList);
                    scavengerHuntList.add(hunt);

                    // Update the grid layout with the scavenger hunt titles only
                    updateGridWithHunts(linksGrid);

                } catch (JSONException e) {
                    Log.e("NearbyPlacesTask", "Error parsing JSON", e);
                }
            } else {
                Log.e("NearbyPlacesTask", "No result from Nearby Search");
            }


        }

        // Update GridLayout with just the scavenger hunt names
        private void updateGridWithHunts(GridLayout gridLayout) {

            int childCount = gridLayout.getChildCount();

            // Loop through the scavenger hunts and update the corresponding TextViews
            for (int i = 0; i < scavengerHuntList.size(); i++) {
                if (i < childCount) {
                    // Get the current scavenger hunt
                    scavengerHunt hunt = scavengerHuntList.get(i);

                    // Find the corresponding TextView in the grid
                    TextView textView = (TextView) gridLayout.getChildAt(i);

                    // Update the text of the TextView with the hunt name
                    textView.setText(hunt.getName());

                    textView.setOnClickListener(v -> {
                        Intent intent = new Intent(selectyourhunt_activity.this, selectedscavengerhunt_activity.class);
                        intent.putExtra("huntName", hunt.getName());
                        intent.putExtra("huntDescription", hunt.getDescription());
                        intent.putExtra("hunt", hunt);
                        startActivity(intent);
                    });
                }
            }
        }
    }
}