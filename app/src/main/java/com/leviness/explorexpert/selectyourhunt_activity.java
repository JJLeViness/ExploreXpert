package com.leviness.explorexpert;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.leviness.explorexpert.network.KnowledgeGraphAPIClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class selectyourhunt_activity extends AppCompatActivity {

    private List<scavengerHunt> scavengerHuntList = new ArrayList<>();
    private KnowledgeGraphAPIClient knowledgeGraphAPIClient;
    private LatLng manhattanLocation = new LatLng(40.7831, -73.9712); // TESTING
    private DrawerLayout menuNavigation;
    private GridLayout linksGrid;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectyourhunt);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        db = FirebaseFirestore.getInstance();  // Initialize Firestore
        TextView viewSavedHunts = findViewById(R.id.view_saved_hunts);

        viewSavedHunts.setOnClickListener(v -> showSavedHunts());

       linksGrid = findViewById(R.id.linksGrid);
        menuNavigation = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.menu_navigation);
        ImageView menuButton = findViewById(R.id.menuButton);

        // Initialize the Google Places API
        String apiKey = getString(R.string.maps_api_key);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
        PlacesClient placesClient = Places.createClient(this);
        knowledgeGraphAPIClient = new KnowledgeGraphAPIClient(apiKey);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, menuNavigation, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
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

    private void showSavedHunts() {
        db.collection("scavengerHunts").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<String> huntNames = new ArrayList<>();

                // Retrieve each hunt's document ID, which serves as the hunt name
                for (DocumentSnapshot document : task.getResult()) {
                    String huntName = document.getId();  // Document ID is the hunt name
                    huntNames.add(huntName);
                    Log.d("Firestore", "Fetched Hunt Name: " + huntName);
                }

                if (!huntNames.isEmpty()) {
                    List<String> displayedHunts = getDisplayedHunts();
                    List<String> availableHunts = new ArrayList<>(huntNames);
                    availableHunts.removeAll(displayedHunts);
                    showHuntDialog(availableHunts);   // Show dialog with hunt names
                } else {
                    Log.d("Firestore", "No hunts found in scavengerHunts collection.");
                    Toast.makeText(this, "No hunts found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Exception e = task.getException();
                Log.e("Firestore", "Error fetching hunts: ", e);
                Toast.makeText(this, "Failed to load hunts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void showHuntDialog(List<String> huntNames) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a Scavenger Hunt");

        // Use an ArrayAdapter to display the hunt names in the dialog
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, huntNames);

        builder.setAdapter(adapter, (dialog, which) -> {
            String selectedHunt = huntNames.get(which);
            openHuntDetails(selectedHunt);  // Handle selected hunt
        });

        builder.setNegativeButton("CLOSE", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }


    private void openHuntDetails(String huntName) {
        DocumentReference huntDocRef = db.collection("scavengerHunts").document(huntName);

        huntDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String huntDescription = documentSnapshot.getString("description");

                // Fetch tasks associated with the hunt
                huntDocRef.collection("tasks").get().addOnSuccessListener(taskSnapshots -> {
                    List<scavengerHuntTask> taskList = new ArrayList<>();

                    for (DocumentSnapshot taskDoc : taskSnapshots) {
                        String placeName = taskDoc.getString("placeName");
                        String description = taskDoc.getString("description");
                        Map<String, Double> locationMap = (Map<String, Double>) taskDoc.get("location");

                        if (locationMap != null) {
                            double latitude = locationMap.get("latitude");
                            double longitude = locationMap.get("longitude");
                            LatLng location = new LatLng(latitude, longitude);

                            scavengerHuntTask task = new scavengerHuntTask(placeName, description, location);
                            taskList.add(task);
                        } else {
                            Log.e("Firestore", "Location data missing for task: " + placeName);
                        }
                    }

                    // Reconstruct the scavengerHunt object
                    scavengerHunt hunt = new scavengerHunt(huntName, huntDescription, taskList);



                    // Pass the scavengerHunt object to selectedscavengerhunt_activity
                    Intent intent = new Intent(selectyourhunt_activity.this, selectedscavengerhunt_activity.class);
                    intent.putExtra("hunt", hunt);
                    intent.putExtra("huntName", huntName);
                    intent.putExtra("huntDescription", huntDescription);
                    startActivity(intent);

                }).addOnFailureListener(e -> Log.e("Firestore", "Error fetching tasks", e));
            } else {
                Log.e("Firestore", "Hunt document does not exist");
                Toast.makeText(this, "Hunt not found", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Log.e("Firestore", "Error fetching hunt document", e));
    }







    private void populateScavengerHunts(GridLayout linksGrid) {



        // Creating and adding scavenger hunts
        /*createScavengerHunt("Manhattan Landmarks Hunt", "tourist_attraction|museum|art_gallery", "Discover famous landmarks in Manhattan.", linksGrid);
        createScavengerHunt("Art Installations Hunt", "art_gallery|museum|tourist_attraction", "Explore stunning art installations across Manhattan.", linksGrid);
        createScavengerHunt("Parks & Nature Hunt", "park|zoo|campground", "Visit beautiful parks and green spaces in Manhattan.", linksGrid);
        createScavengerHunt("Historic Buildings Hunt", "museum|city_hall|embassy", "Explore the historic buildings in Manhattan.", linksGrid);
        createScavengerHunt("Food & Drink Tour", "restaurant|cafe|bar|bakery", "Discover the best food and drink spots in the city.", linksGrid);
        createScavengerHunt("Street Art Walk", "art_gallery|tourist_attraction|point_of_interest", "Find the hidden street art murals across the city.", linksGrid);
        createScavengerHunt("Theater District Hunt", "movie_theater|night_club|tourist_attraction", "Explore the famous theaters and cultural spots.", linksGrid);
        createScavengerHunt("Shopping Spree Hunt", "shopping_mall|clothing_store|shoe_store|book_store", "Discover the best shopping destinations in Manhattan.", linksGrid);
        createScavengerHunt("Bridges of Manhattan", "tourist_attraction|point_of_interest|museum", "Take a tour of the iconic bridges of Manhattan.", linksGrid);
        createScavengerHunt("Hidden Gems Hunt", "point_of_interest|bakery|book_store|pet_store|library", "Explore the lesser-known hidden gems of the city.", linksGrid);
        */

        createScavengerHunt("Riverside Retreats", "park|river|tourist_attraction", "Enjoy scenic riverside spots and tranquil parks along Manhattan's waterfront.", linksGrid);
        createScavengerHunt("Cultural Hotspots Tour", "museum|art_gallery|cultural_center", "Discover diverse cultural landmarks showcasing art, history, and heritage.", linksGrid);
        createScavengerHunt("Library & Literary Tour", "library|book_store|point_of_interest", "Visit famous libraries, bookstores, and literary landmarks in Manhattan.", linksGrid);
        createScavengerHunt("Architectural Wonders", "architectural_building|landmark|tourist_attraction", "Explore Manhattan’s iconic and modern architectural marvels.", linksGrid);
        createScavengerHunt("Outdoor Sculptures Hunt", "sculpture|public_art|point_of_interest", "Seek out incredible outdoor sculptures scattered across the city.", linksGrid);
        createScavengerHunt("Historic Hotels & Inns", "hotel|historical_building|landmark", "Tour historic hotels and inns that have stood the test of time.", linksGrid);
        createScavengerHunt("Underground NYC", "subway_station|speakeasy|museum", "Delve into New York’s underground scene with subway art and hidden gems.", linksGrid);
        createScavengerHunt("Farmers Markets & Gardens", "market|botanical_garden|park", "Visit farmers markets and lush gardens for a fresh perspective of NYC.", linksGrid);
        createScavengerHunt("Famous Film Locations", "movie_location|tourist_attraction|point_of_interest", "Discover the filming locations of iconic movies set in Manhattan.", linksGrid);
        createScavengerHunt("Music & Jazz Clubs", "music_venue|jazz_club|night_club", "Explore legendary music venues and jazz clubs that shaped NYC’s sound.", linksGrid);


    }

    private void createScavengerHunt(String huntName, String placeTypes, String huntDescription, GridLayout linksGrid) {
        DocumentReference huntDocRef = db.collection("scavengerHunts").document(huntName);

        huntDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // The hunt exists in Firestore; retrieve tasks and construct scavengerHunt object
                fetchTasksForExistingHunt(huntDocRef, huntName, huntDescription, linksGrid);
            } else {
                // The hunt does not exist; create and save it
                Map<String, Object> huntData = new HashMap<>();
                huntData.put("name", huntName);
                huntData.put("description", huntDescription);

                huntDocRef.set(huntData).addOnSuccessListener(aVoid -> {
                    // Only create a new hunt if it doesn't exist
                    String nearbySearchUrl = getNearbySearchUrl(manhattanLocation, placeTypes);
                    new NearbyPlacesTask(huntName, huntDescription, linksGrid).execute(nearbySearchUrl);
                }).addOnFailureListener(e -> Log.e("Firestore", "Error saving scavenger hunt", e));
            }
        }).addOnFailureListener(e -> Log.e("Firestore", "Error checking for existing hunt", e));
    }

    private void fetchTasksForExistingHunt(DocumentReference huntDocRef, String huntName, String huntDescription, GridLayout linksGrid) {
        huntDocRef.collection("tasks").get().addOnSuccessListener(taskSnapshots -> {
            List<scavengerHuntTask> taskList = new ArrayList<>();

            for (DocumentSnapshot taskDoc : taskSnapshots) {
                String placeName = taskDoc.getString("placeName");
                String description = taskDoc.getString("description");
                Map<String, Double> locationMap = (Map<String, Double>) taskDoc.get("location");

                if (locationMap != null) {
                    double latitude = locationMap.get("latitude");
                    double longitude = locationMap.get("longitude");
                    LatLng location = new LatLng(latitude, longitude);

                    scavengerHuntTask task = new scavengerHuntTask(placeName, description, location);
                    taskList.add(task);
                } else {
                    Log.e("Firestore", "Location data missing for task: " + placeName);
                }
            }

            // Construct the scavengerHunt object
            scavengerHunt hunt = new scavengerHunt(huntName, huntDescription, taskList);
            scavengerHuntList.add(hunt);

            // Update the grid layout with the scavenger hunt titles only
            updateGridWithHunts(linksGrid);

        }).addOnFailureListener(e -> Log.e("Firestore", "Error fetching tasks", e));
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
                    DocumentReference huntDocRef = db.collection("scavengerHunts").document(huntName);

                    // Parse the places data and add to scavengerHuntTask list
                    for (int i = 0; i < Math.min(results.length(), 5); i++) {  // Limiting to 5 places for sample
                        JSONObject place = results.getJSONObject(i);
                        String placeName = place.getString("name");
                        JSONObject geometry = place.getJSONObject("geometry").getJSONObject("location");
                        LatLng placeLocation = new LatLng(geometry.getDouble("lat"), geometry.getDouble("lng"));
                        scavengerHuntTask task = new scavengerHuntTask(placeName," ", placeLocation);
                        fetchAndSaveTaskDescription(huntDocRef, task, taskList);

                       // fetchDescriptionForTask(task); //commented out functionality in new function to fetch description

                        taskList.add(task);


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

        private void fetchDescriptionForTask(scavengerHuntTask task) {
            knowledgeGraphAPIClient.fetchGeneralInfoForPlace(task.getPlaceName(), new KnowledgeGraphAPIClient.OnKnowledgeGraphResultListener() {
                @Override
                public void onResult(String description) {
                    task.setDescription(description);  // Update task with description

                    Log.d("selectyourhunt_activity", "Description for " + task.getPlaceName() + ": " + description);
                }

                @Override
                public void onError(String errorMessage) {
                    String description = "No description available. " + getRandomFunFact();
                    task.setDescription(description);

                    Log.e("selectyourhunt_activity", "Error fetching description: " + errorMessage);
                }
            });
        }

        private String getRandomFunFact() {
            String[] funFacts = {
                    "Did you know? The New York subway system is the largest in the world by number of stations.",
                    "Fun fact: Manhattan’s Chinatown is one of the oldest in the United States.",
                    "Did you know? Central Park is larger than the country of Monaco.",
                    "Fun fact: The Empire State Building has its own zip code – 10118.",
                    "Did you know? Times Square is named after The New York Times newspaper.",
                    "Fun fact: New York was the first capital of the United States in 1789.",
                    "Did you know? The Statue of Liberty was a gift from France in 1886.",
                    "Fun fact: The Brooklyn Bridge was the first bridge to use steel wire in its construction.",
                    "Did you know? Grand Central Terminal has a hidden tennis court on its top floor.",
                    "Fun fact: New York City’s Federal Reserve Bank holds the world’s largest gold storage."
            };

            Random random = new Random();
            return funFacts[random.nextInt(funFacts.length)];
        }




    private void fetchAndSaveTaskDescription(DocumentReference huntDocRef, scavengerHuntTask task, List<scavengerHuntTask> taskList) {
        knowledgeGraphAPIClient.fetchGeneralInfoForPlace(task.getPlaceName(), new KnowledgeGraphAPIClient.OnKnowledgeGraphResultListener() {
            @Override
            public void onResult(String description) {
                task.setDescription(description); // Set the fetched description

                // Save task to Firestore within the specific hunt document
                Map<String, Object> taskData = new HashMap<>();
                taskData.put("placeName", task.getPlaceName());
                taskData.put("description", task.getDescription());

                // Convert LatLng to a map
                Map<String, Double> locationMap = new HashMap<>();
                locationMap.put("latitude", task.getLocation().latitude);
                locationMap.put("longitude", task.getLocation().longitude);
                taskData.put("location", locationMap);

                // Add the task to the hunt's tasks subcollection
                huntDocRef.collection("tasks").add(taskData)
                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "Task saved successfully!"))
                        .addOnFailureListener(e -> Log.e("Firestore", "Error saving task", e));

                // Add to task list after setting description
                taskList.add(task);
            }

            @Override
            public void onError(String errorMessage) {
                String description = "No description available. " + getRandomFunFact();
                task.setDescription(description);

                Log.e("selectyourhunt_activity", "Error fetching description: " + errorMessage);
            }
        });
    }


}

    private List<String> getDisplayedHunts() {
        List<String> displayedHunts = new ArrayList<>();
        for (int i = 0; i < linksGrid.getChildCount(); i++) {
            TextView huntTextView = (TextView) linksGrid.getChildAt(i);
            displayedHunts.add(huntTextView.getText().toString());
        }
        return displayedHunts;
    }

    public void updateGridWithHunts(GridLayout gridLayout) {

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