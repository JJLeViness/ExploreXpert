package com.leviness.explorexpert;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;



import androidx.appcompat.app.AppCompatActivity;

public class selectedscavengerhunt_activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectedscavengerhunt);

        // Get the hunt details from the intent
        String huntName = getIntent().getStringExtra("huntName");
        String huntDescription = getIntent().getStringExtra("huntDescription");
        scavengerHunt hunt = getIntent().getParcelableExtra("hunt");

        // Set the hunt details in the TextViews
        TextView huntNameView = findViewById(R.id.myPointsLabel);
        TextView huntDescView = findViewById(R.id.huntDescription);

        TextView tasksLabel = findViewById(R.id.tasksLabel);


        huntNameView.setText(huntName);
        huntDescView.setText(huntDescription);

        tasksLabel.setOnClickListener(v -> { //FOR TESTING PURPOSES ONLY - REMOVE LATER

            Intent intent = new Intent(selectedscavengerhunt_activity.this, navigator.class);
            intent.putExtra("hunt", hunt);
            startActivity(intent);
        });
    }
}
