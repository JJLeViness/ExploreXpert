package com.leviness.explorexpert;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class registration_Activity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText emailEditText, passwordEditText, nameEditText, dobEditText, usernameEditText;
    private Button registerButton;
    private DrawerLayout menuNavigation;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;
    private ImageView menuButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();



        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        nameEditText = findViewById(R.id.name);
        dobEditText = findViewById(R.id.dob);
        usernameEditText = findViewById(R.id.username);
        registerButton = findViewById(R.id.registerButton);
        menuButton = findViewById(R.id.menuButton);
        menuNavigation = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.menu_navigation);

        toggle = new ActionBarDrawerToggle(this, menuNavigation, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        menuNavigation.addDrawerListener(toggle);
        toggle.syncState();

        registerButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            String name = nameEditText.getText().toString();
            String dob = dobEditText.getText().toString();
            String username = usernameEditText.getText().toString();
            checkUsernameAvailability(email, password, name, dob, username);


        });

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
                    startActivity(new Intent(registration_Activity.this, homescreen_activity.class));
                } else if (id == R.id.nav_map) {
                    startActivity(new Intent(registration_Activity.this, Map_Activity.class));
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(registration_Activity.this, profile_Activity.class));
                } else if (id == R.id.nav_scavenger_hunt) {
                    startActivity(new Intent(registration_Activity.this, scavenger_Hunt_Activity.class));
                } else if (id == R.id.nav_settings) {
                    startActivity(new Intent(registration_Activity.this, settings_Activity.class));
                } else if (id == R.id.nav_login) {
                    startActivity(new Intent(registration_Activity.this, login_Activity.class));
                }

                menuNavigation.closeDrawer(GravityCompat.END);
                return true;
            }
        });
    }

    private void registerUser(String email, String password, String name, String dob, String username) {
        if (email.isEmpty() || password.isEmpty() || name.isEmpty() || dob.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registration Successful
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid(); // Get the user's ID
                            storeAdditionalUserData(userId, name, dob, username);
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileUpdateTask -> {
                                        if (profileUpdateTask.isSuccessful()) {
                                            Toast.makeText(registration_Activity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                            // Proceed to next activity or main screen
                                        } else {
                                            Toast.makeText(registration_Activity.this, "Profile update failed: " + profileUpdateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });

                        }
                        Toast.makeText(registration_Activity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(registration_Activity.this, login_Activity.class);
                        startActivity(intent);

                    } else {
                        // registration fails, display message to user.
                        Toast.makeText(registration_Activity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void storeAdditionalUserData(String userId, String name, String dob, String username) {
        // Create a map for the additional user data
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("dob", dob);
        userData.put("username", username);
        userData.put("email", emailEditText.getText().toString());

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    //storage successful
                    Toast.makeText(registration_Activity.this, "User data saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Error
                    Toast.makeText(registration_Activity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void checkUsernameAvailability(String email, String password, String name, String dob, String username) {
        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            // Username is already taken
                            Toast.makeText(registration_Activity.this, "Username is already taken. Please choose another one.", Toast.LENGTH_SHORT).show();
                        } else {
                            // Username is available, proceed with registration
                            registerUser(email, password, name, dob, username);
                        }
                    } else {
                        Toast.makeText(registration_Activity.this, "Error checking username availability: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        }
    }
