package com.leviness.explorexpert;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class point_achievement_Activity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private FirebaseFirestore db;
    private String userId;
    private TextView totalPointsTextView;
    private FirebaseUser user;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point_achievement);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Check if the user is logged in
        if (user != null) {
            userId = user.getUid(); // Get actual user ID

            // Initialize the TextView
            totalPointsTextView = findViewById(R.id.totalPoints);

            // Fetch user points
            fetchUserPoints();

            // Fetch user achievements with the desired achievement name
            String achievementName = "Rating Master"; // You can define the achievement name as needed
            fetchUserAchievements(userId, achievementName);

            // Call displayAchievements to show the achievements for the user
            displayAchievements(userId);
        } else {
            Log.e("PointAchievementActivity", "User is not logged in");
            startActivity(new Intent(point_achievement_Activity.this, login_Activity.class));
            finish();
        }


        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.menu_navigation);
        ImageView menuButton = findViewById(R.id.menuButton);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Set the menu button to open/close the drawer
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
                startActivity(new Intent(point_achievement_Activity.this, homescreen_activity.class));
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(point_achievement_Activity.this, Map_Activity.class));
            } else if (id == R.id.nav_scavenger_hunt) {
                startActivity(new Intent(point_achievement_Activity.this, selectyourhunt_activity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(point_achievement_Activity.this, settings_Activity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(point_achievement_Activity.this, profile_Activity.class));
            }

            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });
    }

    private Map<String, Boolean> userAchievements = new HashMap<>();

    private void fetchUserAchievements(String userId,String achievementName) {
        Log.d("AchievementUnlocked", "Attempting to unlock: " + achievementName + " for user: " + userId);

        if (userId == null || achievementName == null) {
            Log.e("AchievementUnlock", "User ID or Achievement Name is null.");
            return; // Exit the method if any of the parameters are null
        }

        DocumentReference userDocRef = db.collection("users").document(userId);
        Log.d("AchievementUnlock", "Document Reference: " + userDocRef.getPath());

        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Assuming achievements are stored as boolean values
                userAchievements.put("Rating Master", documentSnapshot.getBoolean("Rating Master"));
                userAchievements.put("Top Explorer", documentSnapshot.getBoolean("Top Explorer"));
                // Add more achievements as needed
            }
        }).addOnFailureListener(e -> {
            Log.e("AchievementFetch", "Error fetching achievements", e);
        });
    }

    public void unlockAchievement(String userId, String achievementName) {
        if (userId == null || achievementName == null) {
            Log.e("AchievementUnlock", "User ID or Achievement Name is null.");
            return; // Exit if parameters are null
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance(); // Initialize Firestore
        DocumentReference userRef = db.collection("users").document(userId);

        // Update the achievement status
        userRef.update(achievementName, true)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Achievement", achievementName + " unlocked for user: " + userId);
                    // Update the UI or notify the user here
                })
                .addOnFailureListener(e -> {
                    Log.e("Achievement", "Error unlocking achievement", e);
                });
    }

    private void displayAchievements(String userId) {
        LinearLayout achievementsContainer = findViewById(R.id.achievements_container);
        achievementsContainer.removeAllViews();  // Clear previous views

        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Boolean> achievementsMap = new HashMap<>();
                achievementsMap.put("Top Explorer", documentSnapshot.getBoolean("Top Explorer"));
                achievementsMap.put("Rating Master", documentSnapshot.getBoolean("Rating Master"));
                achievementsMap.put("First Scavenger Hunt Completed", documentSnapshot.getBoolean("First Scavenger Hunt Completed"));
                achievementsMap.put("First Review", documentSnapshot.getBoolean("First Review"));
                achievementsMap.put("5-Star Rater", documentSnapshot.getBoolean("5-Star Rater"));
                achievementsMap.put("500 Points Milestone", documentSnapshot.getBoolean("500 Points Milestone"));
                achievementsMap.put("1000 Points Milestone", documentSnapshot.getBoolean("1000 Points Milestone"));

                for (Map.Entry<String, Boolean> entry : achievementsMap.entrySet()) {
                    String achievementName = entry.getKey();
                    Boolean isUnlocked = entry.getValue();

                    if (isUnlocked != null) {
                        TextView achievementView = new TextView(this);
                        achievementView.setText(isUnlocked ? achievementName + " ✔" : achievementName);
                        achievementView.setTextColor(isUnlocked ? Color.parseColor("#528f8f") : Color.parseColor("#888888"));
                        achievementView.setTextSize(18); // Increase text size

                        // Set custom font
                        Typeface typeface = ResourcesCompat.getFont(this, R.font.bungeeregular);
                        achievementView.setTypeface(typeface);

                        achievementsContainer.addView(achievementView);  // Add to LinearLayout
                    }
                }
            }
        }).addOnFailureListener(e -> {
            Log.e("FirestoreError", "Error fetching user achievements", e);
        });
    }

    private void fetchUserPoints() {
        // Ensure you have the correct user ID
        DocumentReference userDocRef = db.collection("users").document(userId);

        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Long userPoints = document.getLong("points");  // Ensure 'points' exists in your Firestore
                    if (userPoints != null && totalPointsTextView != null) {
                        totalPointsTextView.setText(String.valueOf(userPoints));
                    } else {
                        Log.e("PointAchievementActivity", "userPoints is null or totalPointsTextView is null");
                    }
                } else {
                    Log.d("PointAchievementActivity", "No such document");
                }
            } else {
                Log.d("PointAchievementActivity", "get failed with ", task.getException());
            }
        });
    }
}
