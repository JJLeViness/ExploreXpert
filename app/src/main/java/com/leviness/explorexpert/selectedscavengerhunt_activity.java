package com.leviness.explorexpert;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

public class selectedscavengerhunt_activity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userId;
    private TextView usernameTextView;
    private TextView pointsTextView;
    private ImageView profileImageView;
    private DrawerLayout menuNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectedscavengerhunt);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : null;

        // Get the hunt details from the intent
        String huntName = getIntent().getStringExtra("huntName");
        String huntDescription = getIntent().getStringExtra("huntDescription");
        scavengerHunt hunt = getIntent().getParcelableExtra("hunt");

        // Set the hunt details in the TextViews
        TextView huntNameView = findViewById(R.id.myPointsLabel);
        TextView huntDescView = findViewById(R.id.huntDescription);
        TextView tasksLabel = findViewById(R.id.tasksLabel);
        usernameTextView = findViewById(R.id.username);
        pointsTextView = findViewById(R.id.totalPoints);
        profileImageView = findViewById(R.id.profileImage);
        Button startHuntButton = findViewById(R.id.startHuntButton);
        TextView pointsAndAchievements = findViewById(R.id.pointsAndAchievmentsLabel);
        TextView checkPoints = findViewById(R.id.checkpointslabel);
        menuNavigation = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.menu_navigation);
        ImageView menuButton = findViewById(R.id.menuButton);

        huntNameView.setText(huntName);
        huntDescView.setText(huntDescription);

        // Load user profile
        loadUserProfile();

        pointsAndAchievements.setOnClickListener(v -> {
            Intent intent = new Intent(selectedscavengerhunt_activity.this, point_achievement_Activity.class);
            startActivity(intent);

        });

        startHuntButton.setOnClickListener(v -> {
            Intent intent = new Intent(selectedscavengerhunt_activity.this, navigator.class);
            intent.putExtra("hunt", hunt);
            startActivity(intent);
        });

        tasksLabel.setOnClickListener(v -> {

            assert hunt != null;
            showTaskLocations(hunt.getTasks());

        });

        checkPoints.setOnClickListener(v -> loadSavedProgress(hunt));
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
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(selectedscavengerhunt_activity.this, homescreen_activity.class));
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(selectedscavengerhunt_activity.this, Map_Activity.class));
            } else if (id == R.id.nav_profile) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    startActivity(new Intent(selectedscavengerhunt_activity.this, profile_Activity.class));
                } else {
                    startActivity(new Intent(selectedscavengerhunt_activity.this, login_Activity.class));
                }
            } else if (id == R.id.nav_scavenger_hunt) {
                startActivity(new Intent(selectedscavengerhunt_activity.this, selectyourhunt_activity.class));
            } else if (id == R.id.nav_login) {
                startActivity(new Intent(selectedscavengerhunt_activity.this, login_Activity.class));
            }
            menuNavigation.closeDrawer(GravityCompat.END);
            return true;
        });
    }

    private void loadUserProfile() {
        if (userId != null) {
            DocumentReference userDocRef = db.collection("users").document(userId);
            userDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String username = documentSnapshot.getString("username");
                    String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                    Long userPoints = documentSnapshot.getLong("points");

                    // Set username and points
                    if (username != null && !username.isEmpty()) {
                        usernameTextView.setText(username);
                    }
                    if (userPoints != null) {
                        pointsTextView.setText(String.valueOf(userPoints));
                    }

                    // Load and display profile image
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        new DownloadImageTask(profileImageView).execute(profileImageUrl);
                    }
                }
            }).addOnFailureListener(e -> Log.e("SelectedHuntActivity", "Error fetching user profile", e));
        }
    }

    private void loadSavedProgress(scavengerHunt hunt) {
        if (userId != null && hunt != null) {
            DocumentReference userDocRef = db.collection("users").document(userId)
                    .collection("huntProgress").document(hunt.getName());

            userDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Long savedTaskIndex = documentSnapshot.getLong("taskIndex");
                    int taskIndex = savedTaskIndex != null ? savedTaskIndex.intValue() : 0;

                    Intent intent = new Intent(selectedscavengerhunt_activity.this, navigator.class);
                    intent.putExtra("hunt", hunt);
                    intent.putExtra("currentTaskIndex", taskIndex); // Pass the saved task index
                    startActivity(intent);
                } else {
                    startNavigatorFromBeginning(hunt);
                }
            }).addOnFailureListener(e -> Log.e("SelectedHuntActivity", "Error fetching progress", e));
        }
    }

    private void startNavigatorFromBeginning(scavengerHunt hunt) {
        Intent intent = new Intent(selectedscavengerhunt_activity.this, navigator.class);
        intent.putExtra("hunt", hunt);
        intent.putExtra("currentTaskIndex", 0);
        startActivity(intent);
    }

    private void startNavigator(scavengerHunt hunt, int startTaskIndex) {
        Intent intent = new Intent(selectedscavengerhunt_activity.this, navigator.class);
        intent.putExtra("hunt", hunt);
        intent.putExtra("currentTaskIndex", startTaskIndex);
        startActivity(intent);
    }


    private void showTaskLocations(List<scavengerHuntTask> tasks) {
        // Create an array of place names
        String[] placeNames = new String[tasks.size()];
        for (int i = 0; i < tasks.size(); i++) {
            placeNames[i] = tasks.get(i).getPlaceName();
        }

        // Create an AlertDialog to display the place names
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Hunt Locations")
                .setItems(placeNames, (dialog, which) -> {

                })
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.create().show();
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
            if (result != null) {
                imageView.setImageBitmap(result);
            }
        }
    }
}
