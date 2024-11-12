package com.leviness.explorexpert;


import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
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

public class forgotpassword_activity extends AppCompatActivity {

    private EditText emailEditText;
    private Button sendButton;
    private FirebaseAuth mAuth;
    private DrawerLayout menuNavigation;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private ImageView menuButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgotpassword);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();


        emailEditText = findViewById(R.id.email);
        sendButton = findViewById(R.id.send);

        menuNavigation = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.menu_navigation);
        menuButton = findViewById(R.id.menuButton);

        sendButton.setOnClickListener(v -> sendPasswordResetEmail());

        toggle = new ActionBarDrawerToggle(this, menuNavigation, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        menuNavigation.addDrawerListener(toggle);
        toggle.syncState();

        // Menu button click listener to open/close the menu
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

        // Set navigation item selected listener
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    startActivity(new Intent(forgotpassword_activity.this, homescreen_activity.class));
                } else if (id == R.id.nav_map) {
                    startActivity(new Intent(forgotpassword_activity.this, Map_Activity.class));
                } else if (id == R.id.nav_profile) {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        startActivity(new Intent(forgotpassword_activity.this, profile_Activity.class));
                    } else {
                        startActivity(new Intent(forgotpassword_activity.this, login_Activity.class));
                    }
                } else if (id == R.id.nav_scavenger_hunt) {
                    startActivity(new Intent(forgotpassword_activity.this, selectyourhunt_activity.class));
                }else if (id == R.id.nav_login) {
                    startActivity(new Intent(forgotpassword_activity.this, login_Activity.class));
                }

                menuNavigation.closeDrawer(GravityCompat.END);
                return true;
            }
        });
    }

    private void sendPasswordResetEmail() {
        String email = emailEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError("Email is required!");
            emailEditText.requestFocus();
            return;
        }

        // Send password reset email
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(forgotpassword_activity.this, "Password reset email sent!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(forgotpassword_activity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}
