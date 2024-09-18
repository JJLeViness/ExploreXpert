package com.leviness.explorexpert;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.Manifest;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.leviness.explorexpert.network.RoutesTask;
import com.leviness.explorexpert.network.DirectionsAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Map_Activity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private ImageView menuButton;
    private DrawerLayout menuNavigation;
    private ActionBarDrawerToggle toggle;
    private Spinner filterSpinner;
    private NavigationView navigationView;
    private PlacesClient placesClient;
    private LatLng currentLocation;
    private String[] placeTypes = {"RESTAURANT", "CAFE", "BAR", "STORE", "SHOPPING_MALL", "MUSEUM", "AMUSEMENT_PARK", "PARK", "MOVIE_THEATER", "THINGS_TO_DO", "HOTEL", "TOURIST_ATTRACTION", "POINT_OF_INTEREST", "LOCAL_LANDMARK", "HISTORIC_SITE"};
    private List<String> directionsList = new ArrayList<>();

    private Map<Marker, Bitmap> markerImages = new HashMap<>();
    private Map<Marker, Float> markerRatings = new HashMap<>();
    private  Map<Marker, String> markerFunFacts = new HashMap<>();
    private List<LatLng> stepLatLngs = new ArrayList<>();


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
        RecyclerView directionsRecyclerView = findViewById(R.id.directionsRecyclerView);
        directionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DirectionsAdapter adapter = new DirectionsAdapter(directionsList, stepLatLngs, directionsRecyclerView, mMap);
        directionsRecyclerView.setAdapter(adapter);


        filterSpinner = findViewById(R.id.filterSpinner);

//Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.maps_api_key));
        }
        placesClient = Places.createClient(this);


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

    private void setupFilterSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, placeTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(adapter);
        filterSpinner.setVisibility(View.VISIBLE);
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedPlaceType = placeTypes[position];
                if (currentLocation != null) {
                    markNearbyPOIs(currentLocation, selectedPlaceType);
                } else {
                    Log.w("Spinner", "currentLocation is null. Cannot mark nearby POIs.");
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });
    }

    //Map Logic and snap to current location
    @Override
    public void onMapReady(GoogleMap googleMap) {


        mMap = googleMap;


        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null; // Use default InfoWindow frame
            }

            @Override
            public View getInfoContents(Marker marker) {
                View infoWindowView = getLayoutInflater().inflate(R.layout.info_window, null);

                ImageView placePhoto = infoWindowView.findViewById(R.id.place_photo);
                TextView title = infoWindowView.findViewById(R.id.title);
                RatingBar placeRating = infoWindowView.findViewById(R.id.place_rating);
                TextView funFactTextView = infoWindowView.findViewById(R.id.fun_fact);

                title.setText(marker.getTitle());

                // Use preloaded rating
                if (markerRatings.containsKey(marker)) {
                    placeRating.setRating(markerRatings.get(marker));
                } else {
                    placeRating.setRating(0f);
                }

                // Use preloaded image
                if (markerImages.containsKey(marker)) {
                    placePhoto.setImageBitmap(markerImages.get(marker));
                } else {
                    placePhoto.setImageResource(R.drawable.default_profile_image);
                }

                if (markerFunFacts.containsKey(marker)) {
                    Log.d("InfoWindow", "Fun fact for marker " + marker.getTitle() + ": " + markerFunFacts.get(marker));
                    funFactTextView.setText(markerFunFacts.get(marker));
                } else {
                    funFactTextView.setText("Loading fun fact...");

                }

                return infoWindowView;
            }

        });

        mMap.setOnInfoWindowClickListener(marker -> {

            String placeId = marker.getSnippet();
            showRatingDialog(placeId, marker.getTitle());

        });

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

        String fromLatLng = getIntent().getStringExtra("fromLatLng");
        String toLatLng = getIntent().getStringExtra("toLatLng");  //Retrieve to and from location from home screen, for testing purposes

        RecyclerView directionsRecyclerView = findViewById(R.id.directionsRecyclerView);
        directionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DirectionsAdapter adapter = new DirectionsAdapter(directionsList, stepLatLngs, directionsRecyclerView, mMap);
        directionsRecyclerView.setAdapter(adapter);

        if (fromLatLng != null && toLatLng != null) {
            new RoutesTask(this, mMap, adapter, "walking").execute(fromLatLng, toLatLng);

        } else {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {

                            //LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

                            //For testing
                            LatLng currentLocation = new LatLng(40.7870, -73.9754);


                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 20));


                            mMap.addMarker(new MarkerOptions().position(currentLocation).title("You are here"));


                            this.currentLocation = currentLocation;


                            setupFilterSpinner();


                        }

                    });
        }

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
                }
                menuNavigation.closeDrawer(GravityCompat.END);
                return true;
            }
        });


    }

    private void markNearbyPOIs(LatLng location, String placeType) {
        // Clear existing markers
        mMap.clear();
        markerImages.clear();

        // Add user's current location marker
        mMap.addMarker(new MarkerOptions().position(location).title("You are here"));

        // Nearby Places API request
        String apiKey = this.getString(R.string.maps_api_key);
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="
                + location.latitude + "," + location.longitude
                + "&radius=1500&type=" + placeType
                + "&key=" + apiKey;

        new GetNearbyPlacesTask().execute(url);
    }

    private class GetNearbyPlacesTask extends AsyncTask<String, Void, String> {
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
                Log.e("GetNearbyPlacesTask", "Error in getting nearby places", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONArray results = jsonObject.getJSONArray("results");

                    for (int i = 0; i < results.length(); i++) {
                        JSONObject place = results.getJSONObject(i);
                        String placeName = place.getString("name");
                        String placeId = place.getString("place_id");
                        JSONObject geometry = place.getJSONObject("geometry");
                        JSONObject location = geometry.getJSONObject("location");

                        LatLng latLng = new LatLng(location.getDouble("lat"), location.getDouble("lng"));

                        // Add marker for each place
                        Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(placeName).snippet(placeId).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                        preloadImageForMarker(marker, placeId);
                        fetchGeneralInfoForPlace(marker, placeName);
                    }

                } catch (JSONException e) {
                    Log.e("GetNearbyPlacesTask", "Error parsing JSON", e);
                }
            }
        }


    }

    private void showRatingDialog(String placeId, String placeName) {
        // Create a new dialog to let the user input a new rating
        AlertDialog.Builder builder = new AlertDialog.Builder(Map_Activity.this);
        builder.setTitle("Rate " + placeName);

        final RatingBar ratingBar = new RatingBar(Map_Activity.this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        ratingBar.setLayoutParams(layoutParams);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(0.5f);

        LinearLayout layout = new LinearLayout(Map_Activity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(ratingBar);

        builder.setView(layout);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            float newRating = ratingBar.getRating();

            // Get the currently logged-in user
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser != null) {
                String userId = currentUser.getUid();
                String userName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Anonymous";

                // Update the rating in Firestore
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Map<String, Object> ratingData = new HashMap<>();
                ratingData.put("rating", newRating);
                ratingData.put("userId", userId);
                ratingData.put("userName", userName);
                ratingData.put("timestamp", System.currentTimeMillis());

                db.collection("locations").document(placeId).collection("ratings")
                        .add(ratingData)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(Map_Activity.this, "Rating submitted!", Toast.LENGTH_SHORT).show();
                            // After adding the new rating, update the average rating
                            updateAverageRating(db, placeId);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("RatingUpdate", "Error submitting rating", e);
                            Toast.makeText(Map_Activity.this, "Failed to submit rating", Toast.LENGTH_SHORT).show();
                        });

            } else {
                Toast.makeText(Map_Activity.this, "You must be logged in to submit a rating.", Toast.LENGTH_SHORT).show();
            }

        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void updateAverageRating(FirebaseFirestore db, String placeId) {
        db.collection("locations").document(placeId).collection("ratings")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    double totalRating = 0;
                    int ratingCount = querySnapshot.size();

                    for (DocumentSnapshot document : querySnapshot) {
                        Double rating = document.getDouble("rating");
                        if (rating != null) {
                            totalRating += rating;
                        }
                    }

                    double averageRating = totalRating / ratingCount;

                    // Update the average rating and rating count in the main location document
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("rating", averageRating);
                    updateData.put("ratingCount", ratingCount);

                    db.collection("locations").document(placeId)
                            .update(updateData)
                            .addOnSuccessListener(aVoid -> Log.d("RatingUpdate", "Average rating updated!"))
                            .addOnFailureListener(e -> Log.e("RatingUpdate", "Error updating average rating", e));
                })
                .addOnFailureListener(e -> {
                    Log.e("RatingUpdate", "Error fetching ratings", e);
                });
    }

    private void preloadImageForMarker(Marker marker, String placeId) {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.PHOTO_METADATAS);
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);

        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place place = response.getPlace();
            String placeName = place.getName();

            List<PhotoMetadata> photoMetadataList = place.getPhotoMetadatas();
            Log.d("PreloadImage", "Photo Metadata List Size: " + photoMetadataList.size());

            if (photoMetadataList != null && !photoMetadataList.isEmpty()) {
                PhotoMetadata photoMetadata = photoMetadataList.get(0);
                FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(photoMetadata)
                        .setMaxWidth(500)
                        .setMaxHeight(300)
                        .build();

                placesClient.fetchPhoto(photoRequest).addOnSuccessListener((fetchPhotoResponse) -> {
                    Bitmap bitmap = fetchPhotoResponse.getBitmap();
                    markerImages.put(marker, bitmap);
                }).addOnFailureListener((exception) -> {
                    Log.e("PreloadImage", "Failed to fetch photo for marker", exception);
                });
            } else {
                Log.e("PreloadImage", "No photo metadata available for: " + placeName);
            }


        }).addOnFailureListener((exception) -> {
            Log.e("PreloadImage", "Failed to fetch place details for marker", exception);
        });


        // Preload rating
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("locations").document(placeId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Double rating = documentSnapshot.getDouble("rating");
                if (rating != null) {
                    markerRatings.put(marker, rating.floatValue());
                } else {
                    markerRatings.put(marker, 0f);
                }
            } else {
                markerRatings.put(marker, 0f);
                Map<String, Object> locationData = new HashMap<>();
                locationData.put("name", marker.getTitle());
                locationData.put("rating", 0.0);

                db.collection("locations").document(placeId)
                        .set(locationData)
                        .addOnSuccessListener(aVoid -> Log.d("PreloadRating", "Document successfully created with default rating."))
                        .addOnFailureListener(e -> Log.e("PreloadRating", "Error creating document", e));
            }
        }).addOnFailureListener(e -> {
            markerRatings.put(marker, 0f);
            Log.e("PreloadRating", "Error fetching rating from Firestore: " + e.getMessage());
        });

    }

    private void fetchGeneralInfoForPlace(Marker marker, String placeName) {
        new AsyncTask<Void, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(Void... voids) {
                try {
                    String encodedPlaceName = URLEncoder.encode(placeName, "UTF-8");
                    String apiKey = getString(R.string.maps_api_key);
                    String urlString = "https://kgsearch.googleapis.com/v1/entities:search" +
                            "?query=" + encodedPlaceName +
                            "&key=" + apiKey +
                            "&limit=1&indent=true";

                    URL url = new URL(urlString);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        StringBuilder responseBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                        return new JSONObject(responseBuilder.toString());
                    }
                } catch (Exception e) {
                    Log.e("KnowledgeGraphAPI", "Error fetching data: " + e.getMessage(), e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(JSONObject entity) {
                if (entity != null) {
                    JSONArray itemList = entity.optJSONArray("itemListElement");
                    if (itemList != null && itemList.length() > 0) {
                        JSONObject result = itemList.optJSONObject(0).optJSONObject("result");
                        if (result != null) {
                            // Extracting the detailed description
                            JSONObject detailedDescription = result.optJSONObject("detailedDescription");
                            if (detailedDescription != null) {
                                // Only fetch the articleBody part
                                String articleBody = detailedDescription.optString("articleBody", "No description available.");
                                markerFunFacts.put(marker, articleBody);
                            } else {
                                markerFunFacts.put(marker, "No description available.");
                            }
                        } else {
                            markerFunFacts.put(marker, "No description available.");
                        }
                    } else {
                        markerFunFacts.put(marker, "No general information available.");
                    }
                } else {
                    markerFunFacts.put(marker, "Error fetching data.");
                }
            }

        }.execute();
    }








}