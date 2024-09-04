package com.leviness.explorexpert;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;


public class profile_Activity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "profile_Activity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int PICK_IMAGE_REQUEST = 1;

    private GoogleMap mMap;
    private CustomMapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private TextView tvUsername;
    private Uri selectedImageUri;
    private DrawerLayout drawerLayout;
    private String userId;
    private ImageView profileImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        FirebaseApp.initializeApp(this);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        tvUsername = findViewById(R.id.username);
        profileImageView = findViewById(R.id.profileImage);
        profileImageView.setOnClickListener(v -> showEnlargedImage());
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String userId = user.getUid();
            fetchUserProfile(userId);
        } else {
            Toast.makeText(profile_Activity.this, "User not logged in", Toast.LENGTH_SHORT).show();
        }

        Button myPointsButton = findViewById(R.id.myPointsLabel);
        myPointsButton.setOnClickListener(v -> {
            Intent intent = new Intent(profile_Activity.this, point_achievement_Activity.class);
            startActivity(intent);
        });

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
            }else if (id == R.id.nav_login) {
                startActivity(new Intent(profile_Activity.this, login_Activity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            selectedImageUri = data.getData();
                            ImageView profileImageView = findViewById(R.id.profileImage);
                            profileImageView.setImageURI(selectedImageUri);
                        }
                    }
                }
        );

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapView = findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        firestore.collection("users").document("udf2uvZI8NSeDTKiofRWLOCYqZF3").get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                if (profileImageUrl != null) {
                    new DownloadImageTask(profileImageView).execute(profileImageUrl);
                }
            }
        });

        Button editButton = findViewById(R.id.edit_button);
        editButton.setOnClickListener(v -> {
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

                if (user != null) {
                    String userId = user.getUid();
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    DocumentReference userRef = db.collection("users").document(userId);

                    if (!newUsername.isEmpty()) {
                        // Check username availability before updating
                        checkUsernameAvailability(newUsername, available -> {
                            if (available) {
                                tvUsername.setText(newUsername);
                                userRef.update("username", newUsername)
                                        .addOnSuccessListener(aVoid -> Toast.makeText(profile_Activity.this, "Username updated successfully", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> Toast.makeText(profile_Activity.this, "Failed to update username", Toast.LENGTH_SHORT).show());
                            } else {
                                Toast.makeText(profile_Activity.this, "Username is already taken. Please choose another one.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    // Check if a new profile image is selected
                    if (selectedImageUri != null) {
                        FirebaseStorage storage = FirebaseStorage.getInstance();
                        StorageReference storageRef = storage.getReference();
                        StorageReference profileImageRef = storageRef.child("profile_images/" + userId + ".jpg");

                        profileImageRef.putFile(selectedImageUri)
                                .addOnSuccessListener(taskSnapshot -> {
                                    // Get the download URL
                                    profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                        String imageUrl = uri.toString();
                                        userRef.update("profileImageUrl", imageUrl)
                                                .addOnSuccessListener(aVoid -> Toast.makeText(profile_Activity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show())
                                                .addOnFailureListener(e -> Toast.makeText(profile_Activity.this, "Failed to update profile image URL", Toast.LENGTH_SHORT).show());
                                    });
                                })
                                .addOnFailureListener(e -> Toast.makeText(profile_Activity.this, "Failed to upload profile image", Toast.LENGTH_SHORT).show());
                    }

                    dialog.dismiss();
                } else {
                    Toast.makeText(profile_Activity.this, "User not logged in", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.show();
        });
    }

    private void showEnlargedImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_enlarged_image, null);
        builder.setView(dialogView);

        ImageView enlargedImageView = dialogView.findViewById(R.id.enlargedImageView);

        // Set the image to the enlarged view
        enlargedImageView.setImageDrawable(profileImageView.getDrawable());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void checkUsernameAvailability(String username, OnUsernameAvailabilityChecked callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        // If no documents are found or if the username belongs to the current user
                        boolean available = (querySnapshot == null || querySnapshot.isEmpty());
                        callback.onChecked(available);
                    } else {
                        Toast.makeText(profile_Activity.this, "Error checking username availability: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Define an interface for the callback
    interface OnUsernameAvailabilityChecked {
        void onChecked(boolean isAvailable);
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;

        public DownloadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Bitmap bmp = null;
            try {
                InputStream in = new java.net.URL(url).openStream();
                bmp = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bmp;
        }

        protected void onPostExecute(Bitmap result) {
            imageView.setImageBitmap(result);
        }
    }

    private void fetchUserProfile(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String username = document.getString("username");
                    if (username != null) {
                        tvUsername.setText(username);
                    }
                } else {
                    Toast.makeText(profile_Activity.this, "User Document Doesn't Exist", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(profile_Activity.this, "Failed to Retrieve User Data", Toast.LENGTH_SHORT).show();
            }
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        centerOnMyLocation();
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
