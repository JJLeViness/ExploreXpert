package com.leviness.explorexpert;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.leviness.explorexpert.network.KnowledgeGraphAPIClient;
import com.leviness.explorexpert.scavengerHunt;
import com.leviness.explorexpert.scavengerHuntTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class scavenger_Hunt_Activity extends AppCompatActivity {

    private List<scavengerHunt> scavengerHuntList = new ArrayList<>();
    private KnowledgeGraphAPIClient knowledgeGraphAPIClient;
    private PlacesClient placesClient;
    private LatLng manhattanLocation = new LatLng(40.7831, -73.9712); // Manhattan  TESTING

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scavenger_hunt);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String apiKey = getString(R.string.maps_api_key);
        knowledgeGraphAPIClient = new KnowledgeGraphAPIClient(apiKey);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(this);

        populateScavengerHunts();
    }

    private void populateScavengerHunts() {
        // Generate multiple scavenger hunts
        createScavengerHunt("Manhattan Landmarks Hunt", "tourist_attraction|museum|landmark", "Discover famous landmarks in Manhattan.");
        createScavengerHunt("Art Installations Hunt", "art_gallery", "Explore stunning art installations across Manhattan.");
        createScavengerHunt("Parks & Nature Hunt", "park", "Visit beautiful parks and green spaces in Manhattan.");
    }

    // Method to create scavenger hunts based on different place types
    private void createScavengerHunt(String huntName, String placeTypes, String huntDescription) {
        String nearbySearchUrl = getNearbySearchUrl(manhattanLocation, placeTypes);
        new NearbyPlacesTask(huntName, huntDescription).execute(nearbySearchUrl);
    }

    private String getNearbySearchUrl(LatLng location, String placeTypes) {
        String apiKey = getString(R.string.maps_api_key);
        return "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + location.latitude + "," + location.longitude
                + "&radius=2000&type=" + placeTypes + "&key=" + apiKey;
    }

    private class NearbyPlacesTask extends AsyncTask<String, Void, String> {
        private String huntName;
        private String huntDescription;

        public NearbyPlacesTask(String huntName, String huntDescription) {
            this.huntName = huntName;
            this.huntDescription = huntDescription;
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
                    List<String> placeNames = new ArrayList<>();
                    List<LatLng> locations = new ArrayList<>();

                    // Parse the places data
                    for (int i = 0; i < Math.min(results.length(), 5); i++) {  // Limiting to 5 places for sample
                        JSONObject place = results.getJSONObject(i);
                        String placeName = place.getString("name");
                        JSONObject geometry = place.getJSONObject("geometry").getJSONObject("location");
                        LatLng placeLocation = new LatLng(geometry.getDouble("lat"), geometry.getDouble("lng"));

                        placeNames.add(placeName);
                        locations.add(placeLocation);
                    }

                    // Fetch detailed info for each place using Knowledge Graph API
                    for (int i = 0; i < placeNames.size(); i++) {
                        final int index = i;
                        knowledgeGraphAPIClient.fetchGeneralInfoForPlace(placeNames.get(i), new KnowledgeGraphAPIClient.OnKnowledgeGraphResultListener() {
                            @Override
                            public void onResult(String description) {
                                scavengerHuntTask task = new scavengerHuntTask(placeNames.get(index), description, locations.get(index));
                                taskList.add(task);

                                // Once all tasks are added, create the scavenger hunt and print to console
                                if (taskList.size() == placeNames.size()) {
                                    scavengerHunt hunt = new scavengerHunt(huntName, huntDescription, taskList);
                                    scavengerHuntList.add(hunt);

                                    // Print the scavenger hunt details to Logcat
                                    printHuntDetails(hunt);
                                }
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Log.e("ScavengerHunt", "Error fetching place info: " + errorMessage);
                                Toast.makeText(scavenger_Hunt_Activity.this, "Error fetching place info", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                } catch (JSONException e) {
                    Log.e("NearbyPlacesTask", "Error parsing JSON", e);
                }
            } else {
                Log.e("NearbyPlacesTask", "No result from Nearby Search");
            }
        }
    }

    // Method to print scavenger hunt details to the console FOR TESTING PURPOSES
    private void printHuntDetails(scavengerHunt hunt) {
        Log.d("ScavengerHunt", "Scavenger Hunt Name: " + hunt.getName());
        Log.d("ScavengerHunt", "Description: " + hunt.getDescription());
        Log.d("ScavengerHunt", "Tasks:");
        for (scavengerHuntTask task : hunt.getTasks()) {
            Log.d("ScavengerHunt", " - Task: " + task.getPlaceName());
            Log.d("ScavengerHunt", "   Description: " + task.getDescription());
            Log.d("ScavengerHunt", "   Location: " + task.getLocation().toString());
        }
    }
}