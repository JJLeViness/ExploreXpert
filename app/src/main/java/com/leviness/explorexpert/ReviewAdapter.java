package com.leviness.explorexpert;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private List<Review> reviews;

    // Constructor
    public ReviewAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_review_item, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        Log.d("ReviewAdapter", "Binding review: " + review.getLocationName() + " - " + review.getRating());

        holder.bind(review);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    public void addReviews(List<Review> newReviews) {
        reviews.clear();
        reviews.addAll(newReviews);
        notifyDataSetChanged();
    }

    // ViewHolder class
    public class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView locationNameTextView;
        TextView ratingTextView;
        TextView reviewTextTextView;  // New field for text review

        public ReviewViewHolder(View itemView) {
            super(itemView);
            locationNameTextView = itemView.findViewById(R.id.locationName);
            ratingTextView = itemView.findViewById(R.id.rating);
            reviewTextTextView = itemView.findViewById(R.id.reviewText);  // Initialize text review field
        }

        public void bind(Review review) {
            locationNameTextView.setText(review.getLocationName());
            ratingTextView.setText(String.valueOf(review.getRating()));
            reviewTextTextView.setText(review.getReviewText());  // Set the text review
        }
    }
}


