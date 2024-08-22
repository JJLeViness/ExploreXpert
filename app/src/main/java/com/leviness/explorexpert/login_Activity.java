package com.leviness.explorexpert;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class login_Activity extends AppCompatActivity {

    TextView createAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);


        createAccount = findViewById(R.id.createAccount);

        createAccount.setOnClickListener(v -> {
            Intent intent = new Intent(login_Activity.this, registration_Activity.class);
            startActivity(intent);
        });
        }

    }
