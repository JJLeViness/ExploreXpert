package com.leviness.explorexpert;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
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
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class selectyourhunt_activity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng userLocation;
    private static final String PREFS_NAME = "HuntPreferences";
    private static final String LAST_REFRESH_KEY = "last_refresh_time";
    private static final long REFRESH_INTERVAL = 45 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectyourhunt);

        // Initialize the Google Places API
        String apiKey = getString(R.string.maps_api_key);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }

        // Initialize FusedLocationProviderClient to get user's current location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check if location data is passed from Map_Activity
        Intent intent = getIntent();
        double latitude = intent.getDoubleExtra("LATITUDE", 0);
        double longitude = intent.getDoubleExtra("LONGITUDE", 0);

        if (latitude != 0 && longitude != 0) {
            // Use the passed location data
            userLocation = new LatLng(latitude, longitude);
            Log.d("selectyourhunt_activity", "Location from Map_Activity: " + userLocation.latitude + ", " + userLocation.longitude);

            // Now check if it's time to refresh locations
            if (isTimeToRefresh()) {
                fetchNearbyLocations();
                updateLastRefreshTime();
            } else {
                Toast.makeText(this, "Locations are still valid.", Toast.LENGTH_SHORT).show();

                // Load the last saved locations from SharedPreferences
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                String savedLocations = prefs.getString("SAVED_LOCATIONS", null);

                if (savedLocations != null) {
                    try {
                        JSONArray locationsArray = new JSONArray(savedLocations);
                        for (int i = 0; i < locationsArray.length() && i < 10; i++) {
                            String placeName = locationsArray.getString(i);

                            // Find the corresponding TextView by ID and set the place name
                            int textViewId = getResources().getIdentifier("textView" + (i + 1), "id", getPackageName());
                            TextView placeTextView = findViewById(textViewId);
                            placeTextView.setText(placeName);
                        }
                    } catch (JSONException e) {
                        Log.e("selectyourhunt_activity", "Error parsing saved locations", e);
                    }
                } else {
                    Toast.makeText(this, "No saved locations found.", Toast.LENGTH_SHORT).show();
                }
            }

        } else {
            // If no location is passed, check for location permission and fetch user's current location
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestLocationPermission(); // Call your method here
            } else {
                // Fetch the user's location if permission is already granted
                fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            Log.d("selectyourhunt_activity", "User's location: " + userLocation.latitude + ", " + userLocation.longitude);

                            // Now check if it's time to refresh locations
                            if (isTimeToRefresh()) {
                                fetchNearbyLocations();
                                updateLastRefreshTime();
                            } else {
                                Toast.makeText(selectyourhunt_activity.this, "Locations are still valid.", Toast.LENGTH_SHORT).show();
                                // Optionally load last saved locations
                            }
                        } else {
                            Toast.makeText(selectyourhunt_activity.this, "Unable to get your location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }

    private void saveLocations(JSONArray locationsArray) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString("SAVED_LOCATIONS", locationsArray.toString());
        editor.apply();
    }

    private boolean isTimeToRefresh() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long lastRefreshTime = prefs.getLong(LAST_REFRESH_KEY, 0);
        long currentTime = Calendar.getInstance().getTimeInMillis();
        return (currentTime - lastRefreshTime) >= REFRESH_INTERVAL;
    }

    private void updateLastRefreshTime() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        long currentTime = Calendar.getInstance().getTimeInMillis();
        editor.putLong(LAST_REFRESH_KEY, currentTime);
        editor.apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, fetch location again
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    // Fetch the user's location
                    fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                fetchNearbyLocations();
                            } else {
                                Toast.makeText(selectyourhunt_activity.this, "Unable to get your location", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            } else {
                // Permission denied
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Explain why the permission is needed
            new AlertDialog.Builder(this)
                    .setMessage("Location permission is needed to find nearby places. Please enable it in app settings.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        // Open app settings
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show();
        } else {
            // Request permission if rationale is not needed
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager != null && connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    private void fetchNearbyLocations() {
        if (!isNetworkConnected()) {
            Toast.makeText(selectyourhunt_activity.this, "No internet connection. Please try again.", Toast.LENGTH_LONG).show();
            return;
        }
        int radius = 3200;  // 2 miles in meters
        String placeTypes = "tourist_attraction|museum|park|restaurant";
        String nearbySearchUrl = getNearbySearchUrl(userLocation, placeTypes, radius);

        new NearbyPlacesTask().execute(nearbySearchUrl);
    }

    private String getNearbySearchUrl(LatLng location, String placeTypes, int radius) {
        String apiKey = getString(R.string.maps_api_key);
        return "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + location.latitude + "," + location.longitude
                + "&radius=" + radius + "&type=" + placeTypes + "&key=" + apiKey;
    }

    private class NearbyPlacesTask extends AsyncTask<String, Void, String> {
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
                // Parse the result and update TextViews
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONArray results = jsonObject.getJSONArray("results");

                    // Update TextViews with the names of the locations
                    if (results.length() > 0) {
                        for (int i = 0; i < results.length() && i < 10; i++) {
                            JSONObject place = results.getJSONObject(i);
                            String placeName = place.getString("name");

                            // Find TextView by ID and set the name
                            int textViewId = getResources().getIdentifier("textView" + (i + 1), "id", getPackageName());
                            TextView placeTextView = findViewById(textViewId);
                            placeTextView.setText(placeName);
                        }
                    }
                    if (results.length() == 0) {
                        Toast.makeText(selectyourhunt_activity.this, "No nearby places found.", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    Log.e("NearbyPlacesTask", "Error parsing JSON", e);
                }
            }
            if (result == null) {
                Toast.makeText(selectyourhunt_activity.this, "Failed to fetch nearby places. Please check your connection.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
