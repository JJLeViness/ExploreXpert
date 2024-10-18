package com.leviness.explorexpert;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.net.URL;

public class selectedscavengerhunt_activity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser user;
    private String userId;
    private TextView huntNameView;
    private TextView huntDescView;
    private TextView tasksLabel;
    private TextView usernameTextView;
    private TextView pointsTextView;
    private ImageView profileImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectedscavengerhunt);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : null;

        // Get the hunt details from the intent
        String huntName = getIntent().getStringExtra("huntName");
        String huntDescription = getIntent().getStringExtra("huntDescription");
        scavengerHunt hunt = getIntent().getParcelableExtra("hunt");

        // Set the hunt details in the TextViews
        huntNameView = findViewById(R.id.myPointsLabel);
        huntDescView = findViewById(R.id.huntDescription);
        tasksLabel = findViewById(R.id.tasksLabel);
        usernameTextView = findViewById(R.id.username);
        pointsTextView = findViewById(R.id.totalPoints);
        profileImageView = findViewById(R.id.profileImage);

        huntNameView.setText(huntName);
        huntDescView.setText(huntDescription);

        // Load user profile
        loadUserProfile();

        tasksLabel.setOnClickListener(v -> { // FOR TESTING PURPOSES ONLY - REMOVE LATER
            Intent intent = new Intent(selectedscavengerhunt_activity.this, navigator.class);
            intent.putExtra("hunt", hunt);
            startActivity(intent);
        });
    }

    private void loadUserProfile() {
        if (userId != null) {
            DocumentReference userDocRef = db.collection("users").document(userId);
            userDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String username = documentSnapshot.getString("username");
                    String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                    Long userPoints = documentSnapshot.getLong("points");

                    // Set username and points
                    if (username != null && !username.isEmpty()) {
                        usernameTextView.setText(username);
                    }
                    if (userPoints != null) {
                        pointsTextView.setText(String.valueOf(userPoints));
                    }

                    // Load and display profile image
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        new DownloadImageTask(profileImageView).execute(profileImageUrl);
                    }
                }
            }).addOnFailureListener(e -> Log.e("SelectedHuntActivity", "Error fetching user profile", e));
        }
    }

    private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;

        public DownloadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Bitmap bitmap = null;
            try {
                InputStream in = new URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("DownloadImageTask", e.getMessage());
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                imageView.setImageBitmap(result);
            }
        }
    }
}
