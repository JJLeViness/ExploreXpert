package com.leviness.explorexpert;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class splashscreen_activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

        ImageView logoImageView = findViewById(R.id.logoImage);

        logoImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(splashscreen_activity.this, homescreen_activity.class);
                startActivity(intent);
                finish(); // Finish the splash screen activity
            }
        });
    }
}


