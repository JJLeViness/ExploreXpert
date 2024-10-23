package com.leviness.explorexpert;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
    private FirebaseAuth auth;
    private String userId;
    private TextView usernameTextView;
    private ImageView profileImageView;
    private TextView pointsTextView;
    private ReviewAdapter reviewsAdapter;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Initialize Firebase Auth and Firestore setup
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("profile_pictures");
        user = auth.getCurrentUser();

        // Initialize UI components
        pointsTextView = findViewById(R.id.totalPoints);
        profileImageView = findViewById(R.id.profileImage);
        usernameTextView = findViewById(R.id.username);

        // Initialize ScrollView and ImageView for arrows
        ScrollView scrollView = findViewById(R.id.scrollView);
        ImageView arrowUp = findViewById(R.id.arrow_up);
        ImageView arrowDown = findViewById(R.id.arrow_down);

        // Set a scroll change listener on the ScrollView
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            // Check if the scroll view can scroll up or down
            boolean canScrollUp = scrollView.canScrollVertically(-1); // Check for upward scroll
            boolean canScrollDown = scrollView.canScrollVertically(1); // Check for downward scroll

            // Show/hide arrows based on scroll capability
            arrowUp.setVisibility(canScrollUp ? View.VISIBLE : View.GONE);
            arrowDown.setVisibility(canScrollDown ? View.VISIBLE : View.GONE);
        });

        // Optional: To set initial visibility of arrows on load
        scrollView.post(() -> {
            boolean canScrollUp = scrollView.canScrollVertically(-1);
            boolean canScrollDown = scrollView.canScrollVertically(1);
            arrowUp.setVisibility(canScrollUp ? View.VISIBLE : View.GONE);
            arrowDown.setVisibility(canScrollDown ? View.VISIBLE : View.GONE);
        });

        // Set up RecyclerView for reviews
        RecyclerView recyclerView = findViewById(R.id.reviewsRecyclerView);
        reviewsAdapter = new ReviewAdapter(new ArrayList<>());
        recyclerView.setAdapter(reviewsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set up fusedLocationClient and mapView
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapView = findViewById(R.id.map);
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }

        // Check if the user is logged in
        if (user != null) {
            userId = user.getUid();  // Capture userId for later use
            loadUserProfile();       // Load profile (including username, profile image, points)
            fetchUserPoints();       // Fetch user points
            loadUserReviews();       // Load user reviews for RecyclerView
        } else {
            Log.e(TAG, "User is not logged in.");
            // Handle user not being logged in (e.g., redirect to login)
        }

        // Handle edit profile button click for changing profile picture
        ImageButton editButton = findViewById(R.id.edit_button);
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            // Call the uploadProfileImage method to handle the new image
                            uploadProfileImage(selectedImageUri);
                        }
                    }
                }
        );

        // Other UI and drawer setup
        setupUI();
        setupDrawer();
    }

    // Load user profile method (fetch username, profile image, and points)
    private void loadUserProfile() {
        if (user != null) {
            String userId = user.getUid();

            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                            int points = documentSnapshot.getLong("points").intValue();

                            // Set username and points
                            if (username != null && !username.isEmpty()) {
                                usernameTextView.setText(username);
                            }
                            pointsTextView.setText(String.valueOf(points));

                            // Load and display profile image
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                new DownloadImageTask(profileImageView).execute(profileImageUrl);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error fetching user profile", e));
        }
    }

    private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;

        public DownloadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Bitmap bitmap = null;
            try {
                InputStream in = new URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("DownloadImageTask", e.getMessage());
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            imageView.setImageBitmap(result);
        }
    }

    private void uploadProfileImage(Uri imageUri) {
        if (user != null) {
            String userId = user.getUid();
            StorageReference imageRef = storageReference.child(userId + ".jpg");

            // First delete the old image
            imageRef.delete().addOnSuccessListener(aVoid -> {
                // Upload the new image
                imageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();

                            // Update Firestore with the new profileImageUrl
                            db.collection("users").document(userId)
                                    .update("profileImageUrl", downloadUrl)
                                    .addOnSuccessListener(aVoid1 -> {
                                        Log.d(TAG, "Profile image updated in Firestore");
                                        // Load the new profile image into ImageView
                                        profileImageView.setImageURI(imageUri);
                                    }).addOnFailureListener(e -> {
                                        Log.e(TAG, "Error updating profile image URL in Firestore", e);
                                    });
                        })
                ).addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading new profile image", e);
                });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting previous profile image", e);
            });
        }
    }

    private void loadUserReviews() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            Log.d("ProfileActivity", "Fetching reviews for user ID: " + userId);

            db.collectionGroup("ratings")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(ratingsSnapshot -> {
                        List<Review> reviews = new ArrayList<>();
                        Log.d("ProfileActivity", "Total user reviews fetched: " + ratingsSnapshot.size());

                        if (!ratingsSnapshot.isEmpty()) {
                            for (DocumentSnapshot ratingDoc : ratingsSnapshot.getDocuments()) {
                                Double userRating = ratingDoc.getDouble("rating");
                                String reviewText = ratingDoc.getString("reviewText");

                                // Fetch the parent location document (2 levels up)
                                DocumentReference locationRef = ratingDoc.getReference().getParent().getParent();
                                if (locationRef != null) {
                                    locationRef.get().addOnSuccessListener(locationSnapshot -> {
                                        if (locationSnapshot.exists()) {
                                            String locationName = locationSnapshot.getString("name");

                                            if (locationName != null && userRating != null) {
                                                reviews.add(new Review(locationName, userRating, reviewText != null ? reviewText : "No review provided"));
                                                Log.d("ProfileActivity", "Added review for: " + locationName + " with rating: " + userRating + " and review text: " + reviewText);
                                            } else {
                                                Log.d("ProfileActivity", "Missing data for review: " + ratingDoc.getId());
                                            }

                                            // Update the adapter with collected reviews
                                            if (!reviews.isEmpty()) {
                                                reviewsAdapter.addReviews(reviews);
                                                Log.d("ProfileActivity", "Reviews added to the adapter.");
                                            } else {
                                                Log.d("ProfileActivity", "No valid reviews found for the user.");
                                            }
                                        } else {
                                            Log.d("ProfileActivity", "Location document does not exist.");
                                        }
                                    }).addOnFailureListener(e -> Log.e("ProfileActivity", "Error fetching location document", e));
                                }
                            }
                        } else {
                            Log.d("ProfileActivity", "No reviews found for this user.");
                        }
                    })
                    .addOnFailureListener(e -> Log.e("ProfileActivity", "Error fetching user reviews", e));
        } else {
            Log.d("ProfileActivity", "User is not authenticated.");
        }
    }

    private void fetchUserPoints() {
        // Ensure you have the correct user ID
        DocumentReference userDocRef = db.collection("users").document(userId);

        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Long userPoints = document.getLong("points");  // Ensure 'points' exists in your Firestore
                    if (userPoints != null && pointsTextView != null) {
                        pointsTextView.setText(String.valueOf(userPoints));
                    } else {
                        Log.e("ProfileActivity", "userPoints is null or pointsTextView is null");
                    }
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

        ImageButton editButton = findViewById(R.id.edit_button);
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
            } else if (id == R.id.nav_profile) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    startActivity(new Intent(profile_Activity.this, profile_Activity.class));
                } else {
                    startActivity(new Intent(profile_Activity.this, login_Activity.class));
                }
            } else if (id == R.id.nav_scavenger_hunt) {
                startActivity(new Intent(profile_Activity.this, selectyourhunt_activity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(profile_Activity.this, settings_Activity.class));
            } else if (id == R.id.nav_login) {
                startActivity(new Intent(profile_Activity.this, login_Activity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);

        EditText editUsername = dialogView.findViewById(R.id.edit_username);
        Button changeProfilePic = dialogView.findViewById(R.id.change_profile_pic);
        Button saveChanges = dialogView.findViewById(R.id.save_changes);

        AlertDialog dialog = builder.create();

        changeProfilePic.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        saveChanges.setOnClickListener(view -> {
            String newUsername = editUsername.getText().toString().trim();

            if (!newUsername.isEmpty()) {
                TextView usernameTextView = findViewById(R.id.username);
                usernameTextView.setText(newUsername);
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
                Map_Activity mapActivity = new Map_Activity();
                mapActivity.checkForPointMilestoneAchievement(userId);

                // Update Firestore with the new points
                userRef.update("points", newPoints)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("PointsUpdate", "Points successfully updated!");
                        })
                        .addOnFailureListener(e -> {
                            Log.e("PointsUpdate", "Error updating points", e);
                        });
            }
        }).addOnFailureListener(e -> {
            Log.e("PointsFetch", "Error fetching user data", e);
        });
    }

    public void getUserPoints(String userId, OnPointsRetrievedListener listener) {
        db = FirebaseFirestore.getInstance();
        DocumentReference userDocRef = db.collection("users").document(userId);
        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Long points = documentSnapshot.getLong("points"); // Assuming you have a points field
                listener.onPointsRetrieved(points != null ? points : 0);
            } else {
                listener.onPointsRetrieved(0); // User document doesn't exist
            }
        }).addOnFailureListener(e -> {
            Log.e("ProfileActivity", "Error fetching user points", e);
            listener.onPointsRetrieved(0); // On error, return 0 points
        });
    }

    public interface OnPointsRetrievedListener {
        void onPointsRetrieved(long points);
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
