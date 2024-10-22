package com.leviness.explorexpert;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class login_Activity extends AppCompatActivity {

    private EditText username, password;
    private Button loginButton;
    private Button logoutButton;
    private TextView forgotPassword;
    private TextView createAccount;
    private FirebaseAuth mAuth;
    private DrawerLayout menuNavigation;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private ImageView menuButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        logoutButton = findViewById(R.id.logoutButton);
        createAccount = findViewById(R.id.createAccount);
        forgotPassword = findViewById(R.id.forgotPassword);
        menuNavigation = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.menu_navigation);
        menuButton = findViewById(R.id.menuButton);

        //Test to see if user is logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // User is logged in
            Log.d("LoginActivity", "User is logged in: " + currentUser.getEmail());
        } else {
            // No user is logged in
            Log.d("LoginActivity", "No user is logged in.");
        }

        createAccount.setOnClickListener(v -> {
            Intent intent = new Intent(login_Activity.this, registration_Activity.class);
            startActivity(intent);
        });

        loginButton.setOnClickListener(v -> {
            loginUser();
        });

        logoutButton.setOnClickListener(v -> {

            mAuth.signOut(); // Sign out the user
            Toast.makeText(login_Activity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

            // Redirect to homescreen
            Intent intent = new Intent(login_Activity.this, homescreen_activity.class);
            startActivity(intent);
            finish(); // Prevents returning to the previous activity on back press

        });

        forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(login_Activity.this, forgotpassword_activity.class);
            startActivity(intent);

        });
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
                    startActivity(new Intent(login_Activity.this, homescreen_activity.class));
                } else if (id == R.id.nav_map) {
                    startActivity(new Intent(login_Activity.this, Map_Activity.class));
                } else if (id == R.id.nav_profile) {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        startActivity(new Intent(login_Activity.this, profile_Activity.class));
                    } else {
                        startActivity(new Intent(login_Activity.this, login_Activity.class));
                    }
                } else if (id == R.id.nav_scavenger_hunt) {
                    startActivity(new Intent(login_Activity.this, selectyourhunt_activity.class));
                } else if (id == R.id.nav_settings) {
                    startActivity(new Intent(login_Activity.this, settings_Activity.class));
                } else if (id == R.id.nav_login) {
                    startActivity(new Intent(login_Activity.this, login_Activity.class));
                }

                menuNavigation.closeDrawer(GravityCompat.END);
                return true;
            }
        });

        }

       private void loginUser(){
        String user = username.getText().toString();
        String pass = password.getText().toString();

        //Set error if either field is empty
           if (user.isEmpty()) {
               username.setError("Username is required!");
               username.requestFocus();
               return;
           }

           if (pass.isEmpty()) {
               password.setError("Password is required!");
               password.requestFocus();
               return;
           }

           FirebaseFirestore db = FirebaseFirestore.getInstance();

           db.collection("users")
                   .whereEqualTo("username", user)
                   .get()
                   .addOnCompleteListener(task -> {
                       if (task.isSuccessful() && !task.getResult().isEmpty()) {
                           // Username found, get the associated email
                           String email = task.getResult().getDocuments().get(0).getString("email");

                           mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(authTask -> {
                               if (authTask.isSuccessful()) {
                                   Toast.makeText(login_Activity.this, "Login successful", Toast.LENGTH_SHORT).show();
                                   Intent intent = new Intent(login_Activity.this, homescreen_activity.class);
                                   startActivity(intent);
                                   finish(); // Prevents returning to login activity on back press
                               } else {
                                   Toast.makeText(login_Activity.this, "Login failed: " + authTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                               }
                           });
                       } else {
                           // Username not found
                           Toast.makeText(login_Activity.this, "Username not found!", Toast.LENGTH_LONG).show();
                       }
                   });
       }

    }


