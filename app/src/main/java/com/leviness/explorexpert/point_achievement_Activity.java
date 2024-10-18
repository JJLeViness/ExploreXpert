package com.leviness.explorexpert;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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
                startActivity(new Intent(point_achievement_Activity.this, scavenger_Hunt_Activity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(point_achievement_Activity.this, settings_Activity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(point_achievement_Activity.this, profile_Activity.class));
            }

            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
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
