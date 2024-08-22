package com.leviness.explorexpert;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class login_Activity extends AppCompatActivity {

    private EditText username, password;
    private Button loginButton;
    private Button logoutButton;
    private TextView createAccount;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        logoutButton = findViewById(R.id.logoutButton);
        createAccount = findViewById(R.id.createAccount);

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


        }

       private void loginUser(){
        String user = username.getText().toString();
        String pass = password.getText().toString();

        //Set error if either field is empty
           if (user.isEmpty()) {
               username.setError("Email is required!");
               username.requestFocus();
               return;
           }

           if (pass.isEmpty()) {
               password.setError("Password is required!");
               password.requestFocus();
               return;
           }

           mAuth.signInWithEmailAndPassword(user, pass).addOnCompleteListener(task -> {
               if (task.isSuccessful()) {
                   // Sign in success
                   Toast.makeText(login_Activity.this, "Login successful", Toast.LENGTH_SHORT).show();
                   //Send user to homescreen, may change to profile activity.
                   Intent intent = new Intent(login_Activity.this, homescreen_activity.class);
                   startActivity(intent);
                   finish(); // Prevents returning to login activity on back press
               } else {
                   // If sign in fails, display a message to the user.
                   Toast.makeText(login_Activity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
               }
           });
       }

    }


