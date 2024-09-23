package com.leviness.explorexpert;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.navigation.NavigationView;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


public class profile_Activity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "profile_Activity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int PICK_IMAGE_REQUEST = 1;

    private GoogleMap mMap;
    private CustomMapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private DrawerLayout drawerLayout;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private String userId;
    private TextView pointsTextView;
    private TextView totalPointsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

//        RecyclerView reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView);
//        ReviewAdapter reviewsAdapter = new ReviewAdapter();
//        reviewsRecyclerView.setAdapter(reviewsAdapter);
//        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        totalPointsTextView = findViewById(R.id.totalPoints);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapView = findViewById(R.id.map);  // Replace 'customMapView' with the correct ID from your XML

        // Ensure the map is properly set up
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this); // Assuming you are implementing OnMapReadyCallback
        }

        pointsTextView = findViewById(R.id.totalPoints);

        // Initialize Firebase components
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();

            // Retrieve or initialize points when screen opens
            db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    if (!documentSnapshot.contains("points")) {
                        // Initialize points if not present
                        Map<String, Object> userPoints = new HashMap<>();
                        userPoints.put("points", 0);  // Start with 0 points if it doesn't exist
                        db.collection("users").document(userId).update(userPoints)
                                .addOnSuccessListener(aVoid -> pointsTextView.setText("0"))
                                .addOnFailureListener(e -> Log.e("PointsInit", "Error initializing points", e));
                    } else {
                        long currentPoints = documentSnapshot.getLong("points");
                        pointsTextView.setText(String.valueOf(currentPoints));
                    }
                }
            }).addOnFailureListener(e -> Log.e("PointsFetch", "Error fetching user points", e));
        }

        // Other UI and map setup code
        setupUI();
        setupDrawer();
        setupPickImageLauncher();
        fetchUserPoints();
    }

    private void fetchUserPoints() {
        // Ensure you have the correct user ID
        DocumentReference userDocRef = db.collection("users").document(userId);

        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    long userPoints = document.getLong("points");  // Ensure 'points' exists in your Firestore
                    totalPointsTextView.setText(String.valueOf(userPoints));
                } else {
                    Log.d("Profile", "No such document");
                }
            } else {
                Log.d("Profile", "get failed with ", task.getException());
            }
        });
    }

    private void setupUI() {
        Button myPointsButton = findViewById(R.id.myPointsLabel);
        myPointsButton.setOnClickListener(v -> {
            Intent intent = new Intent(profile_Activity.this, point_achievement_Activity.class);
            startActivity(intent);
        });

        Button editButton = findViewById(R.id.edit_button);
        editButton.setOnClickListener(v -> showEditProfileDialog());
    }

    private void setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.menu_navigation);
        ImageView menuButton = findViewById(R.id.menuButton);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        menuButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(profile_Activity.this, homescreen_activity.class));
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(profile_Activity.this, Map_Activity.class));
            } else if (id == R.id.nav_scavenger_hunt) {
                startActivity(new Intent(profile_Activity.this, scavenger_Hunt_Activity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(profile_Activity.this, settings_Activity.class));
            } else if (id == R.id.nav_login) {
                startActivity(new Intent(profile_Activity.this, login_Activity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });
    }

    private void setupPickImageLauncher() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri selectedImageUri = data.getData();
                            ImageView profileImageView = findViewById(R.id.profileImage);
                            profileImageView.setImageURI(selectedImageUri);
                        }
                    }
                }
        );
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);

        EditText editUsername = dialogView.findViewById(R.id.edit_username);
        EditText editEmail = dialogView.findViewById(R.id.edit_email);
        Button changeProfilePic = dialogView.findViewById(R.id.change_profile_pic);
        Button saveChanges = dialogView.findViewById(R.id.save_changes);

        AlertDialog dialog = builder.create();

        changeProfilePic.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        saveChanges.setOnClickListener(view -> {
            String newUsername = editUsername.getText().toString().trim();
            String newEmail = editEmail.getText().toString().trim();

            if (!newUsername.isEmpty()) {
                TextView usernameTextView = findViewById(R.id.username);
                usernameTextView.setText(newUsername);
            }

            if (!newEmail.isEmpty()) {
                TextView emailTextView = findViewById(R.id.email);
                emailTextView.setText(newEmail);
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    public void updateUserPoints(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(userId);

        // Fetch the current points and add 100
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Long currentPoints = documentSnapshot.getLong("points");
                if (currentPoints == null) {
                    currentPoints = 0L; // Initialize to 0 if null
                }
                long newPoints = currentPoints + 100; // Add 100 points

                // Update Firestore with the new points
                userRef.update("points", newPoints)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("PointsUpdate", "Points successfully updated!");
                            // Update the points TextView in the UI
                            TextView pointsTextView = findViewById(R.id.totalPoints);
                            pointsTextView.setText(String.valueOf(newPoints));
                        })
                        .addOnFailureListener(e -> {
                            Log.e("PointsUpdate", "Error updating points", e);
                        });
            }
        }).addOnFailureListener(e -> {
            Log.e("PointsFetch", "Error fetching user data", e);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                CircleImageView profileImageView = findViewById(R.id.profileImage);
                profileImageView.setImageURI(imageUri);
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Check if location permissions are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        mMap.setMyLocationEnabled(true);

        // Ensure fusedLocationClient is initialized before using it
        if (fusedLocationClient != null) {
            // Get the last known location
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            // Move camera to user's current location
                            LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15));

                            // Fetch and display nearby places
                            fetchNearbyPlaces(myLocation);
                        } else {
                            Log.e(TAG, "Location is null");
                        }
                    });
        } else {
            Log.e("MapReady", "FusedLocationProviderClient is null");
        }
    }

    private void fetchNearbyPlaces(LatLng location) {
        // Define the type of places you want to show, e.g., restaurants, cafes, etc.
        String placeType = "restaurant";  // Change as needed
        String apiKey = getString(R.string.maps_api_key);

        // Construct the API URL for nearby places
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="
                + location.latitude + "," + location.longitude
                + "&radius=1500&type=" + placeType
                + "&key=" + apiKey;

        // Execute the task to fetch and display places
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
                        JSONObject geometry = place.getJSONObject("geometry");
                        JSONObject location = geometry.getJSONObject("location");

                        LatLng latLng = new LatLng(location.getDouble("lat"), location.getDouble("lng"));

                        // Add marker for each place
                        mMap.addMarker(new MarkerOptions().position(latLng).title(placeName));
                    }

                } catch (JSONException e) {
                    Log.e("GetNearbyPlacesTask", "Error parsing JSON", e);
                }
            }
        }
    }

    private void centerOnMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.addMarker(new MarkerOptions().position(myLocation).title("You are here"));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15));
                        } else {
                            Log.e(TAG, "Location is null");
                        }
                    });
        } else {
            Log.e(TAG, "Location permission not granted");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                centerOnMyLocation();
            } else {
                Log.e(TAG, "Location permission denied");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
