package com.leviness.explorexpert;


import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class forgotpassword_activity extends AppCompatActivity {

    private EditText emailEditText;
    private Button sendButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgotpassword);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();


        emailEditText = findViewById(R.id.email);
        sendButton = findViewById(R.id.send);

        sendButton.setOnClickListener(v -> sendPasswordResetEmail());
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
