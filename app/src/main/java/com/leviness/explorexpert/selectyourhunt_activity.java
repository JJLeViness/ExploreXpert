package com.leviness.explorexpert;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
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
    private LatLng manhattanLocation = new LatLng(40.7831, -73.9712); // Manhattan

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectyourhunt);

        GridLayout linksGrid = findViewById(R.id.linksGrid);

        // Initialize the Google Places API
        String apiKey = getString(R.string.maps_api_key);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(this);
        knowledgeGraphAPIClient = new KnowledgeGraphAPIClient(apiKey);

        // Populate scavenger hunts into the GridLayout
        populateScavengerHunts(linksGrid);
    }

    private void populateScavengerHunts(GridLayout linksGrid) {
        // Creating and adding scavenger hunts
        createScavengerHunt("Manhattan Landmarks Hunt", "tourist_attraction|museum|landmark", "Discover famous landmarks in Manhattan.", linksGrid);
        createScavengerHunt("Art Installations Hunt", "art_gallery", "Explore stunning art installations across Manhattan.", linksGrid);
        createScavengerHunt("Parks & Nature Hunt", "park", "Visit beautiful parks and green spaces in Manhattan.", linksGrid);
        createScavengerHunt("Historic Buildings Hunt", "building", "Explore the historic buildings in Manhattan.", linksGrid);
        createScavengerHunt("Food & Drink Tour", "restaurant|cafe|bar", "Discover the best food and drink spots in the city.", linksGrid);
        createScavengerHunt("Street Art Walk", "art_gallery|landmark", "Find the hidden street art murals across the city.", linksGrid);
        createScavengerHunt("Theater District Hunt", "movie_theater|theater", "Explore the famous theaters and cultural spots.", linksGrid);
        createScavengerHunt("Shopping Spree Hunt", "shopping_mall|clothing_store", "Discover the best shopping destinations in Manhattan.", linksGrid);
        createScavengerHunt("Bridges of Manhattan", "bridge", "Take a tour of the iconic bridges of Manhattan.", linksGrid);
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
                }
            }
        }
    }
}