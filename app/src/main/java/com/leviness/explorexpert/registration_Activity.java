package com.leviness.explorexpert;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class registration_Activity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText emailEditText, passwordEditText, nameEditText, dobEditText, usernameEditText;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();



        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        nameEditText = findViewById(R.id.name);
        dobEditText = findViewById(R.id.dob);
        usernameEditText = findViewById(R.id.username);
        registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            String name = nameEditText.getText().toString();
            String dob = dobEditText.getText().toString();
            String username = usernameEditText.getText().toString();
            registerUser(email, password, name, dob, username);

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
                        }
                        Toast.makeText(registration_Activity.this, "Registration successful!", Toast.LENGTH_SHORT).show();

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
}
