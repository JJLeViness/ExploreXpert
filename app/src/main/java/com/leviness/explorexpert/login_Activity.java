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
import com.google.firebase.firestore.FirebaseFirestore;

public class login_Activity extends AppCompatActivity {

    private EditText username, password;
    private Button loginButton;
    private Button logoutButton;
    private TextView forgotPassword;
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
        forgotPassword = findViewById(R.id.forgotPassword);

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


