package com.leviness.explorexpert;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.libraries.places.api.model.Review;
import com.leviness.explorexpert.R;

import java.util.ArrayList;
import java.util.List;

//public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
//    private List<Review> reviews = new ArrayList<>();
//
//    public void setReviews(List<Review> reviews) {
//        this.reviews = reviews;
//        notifyDataSetChanged();
//    }
//
//    @NonNull
//    @Override
//    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_review_item, parent, false);
//        return new ReviewViewHolder(view);
//    }
//
//    /*@Override
//    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
//        Review review = reviews.get(position);
//        holder.locationNameTextView.setText(review.getLocationName());
//        holder.ratingTextView.setText(String.valueOf(review.getRating()));
//    }*/
//
//    @Override
//    public int getItemCount() {
//        return reviews.size();
//    }
//
//    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
//        TextView locationNameTextView;
//        TextView ratingTextView;
//        TextView reviewTextView;
//
//        public ReviewViewHolder(@NonNull View itemView) {
//            super(itemView);
//            locationNameTextView = itemView.findViewById(R.id.locationName);
//            ratingTextView = itemView.findViewById(R.id.rating);
//        }
//    }
//}
