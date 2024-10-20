package com.leviness.explorexpert;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class settings_Activity extends AppCompatActivity {

    private EditText newEmailEditText;
    private EditText currentPasswordEditText;
    private Button updateEmailButton;
    private Button forgotPassword;
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView menuButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        newEmailEditText = findViewById(R.id.newEmail);
        currentPasswordEditText = findViewById(R.id.currentPassword);
        updateEmailButton = findViewById(R.id.updateEmail);
        forgotPassword = findViewById(R.id.forgotPasswordButton);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.menu_navigation);
        menuButton = findViewById(R.id.menuButton);

        updateEmailButton.setOnClickListener(v -> reauthenticateAndUpdateEmail());
        forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(settings_Activity.this, forgotpassword_activity.class);
            startActivity(intent);

        });

        // Set up DrawerLayout and NavigationView
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
                startActivity(new Intent(settings_Activity.this, homescreen_activity.class));
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(settings_Activity.this, Map_Activity.class));
            } else if (id == R.id.nav_profile) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    startActivity(new Intent(settings_Activity.this, profile_Activity.class));
                } else {
                    startActivity(new Intent(settings_Activity.this, login_Activity.class));
                }
            } else if (id == R.id.nav_scavenger_hunt) {
                startActivity(new Intent(settings_Activity.this, selectyourhunt_activity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(settings_Activity.this, settings_Activity.class));
            } else if (id == R.id.nav_login) {
                startActivity(new Intent(settings_Activity.this, login_Activity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });
    }

    private void reauthenticateAndUpdateEmail() {
        String newEmail = newEmailEditText.getText().toString().trim();
        String currentPassword = currentPasswordEditText.getText().toString().trim();

        if (newEmail.isEmpty()) {
            newEmailEditText.setError("New email is required!");
            newEmailEditText.requestFocus();
            return;
        }

        if (currentPassword.isEmpty()) {
            currentPasswordEditText.setError("Current password is required!");
            currentPasswordEditText.requestFocus();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

            // Reauthenticate the user with their current credentials
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Reauthentication successful, now send a verification email to the new email
                    user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(emailTask -> {
                        if (emailTask.isSuccessful()) {
                            Toast.makeText(settings_Activity.this, "Verification email sent to " + newEmail + ". Please verify before we can update your email.", Toast.LENGTH_LONG).show();

                            // Listen for changes to email verification status
                            mAuth.addAuthStateListener(authStateListener -> {
                                FirebaseUser updatedUser = mAuth.getCurrentUser();
                                if (updatedUser != null && updatedUser.isEmailVerified()) {
                                    // Update Firestore with the new email
                                    updateEmailInFirestore(updatedUser, newEmail);
                                }
                            });

                        } else {
                            Log.e("FirebaseError", emailTask.getException().getMessage());
                            Toast.makeText(settings_Activity.this, "Error: " + emailTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    Toast.makeText(settings_Activity.this, "Reauthentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(settings_Activity.this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }
    private void updateEmailInFirestore(FirebaseUser user, String newEmail) {
        String userId = user.getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .update("email", newEmail)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreUpdate", "Email successfully updated in Firestore.");
                    Toast.makeText(settings_Activity.this, "Email updated successfully!", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreUpdate", "Error updating email in Firestore: " + e.getMessage());
                    Toast.makeText(settings_Activity.this, "Failed to update email in Firestore.", Toast.LENGTH_LONG).show();
                });
    }

}
