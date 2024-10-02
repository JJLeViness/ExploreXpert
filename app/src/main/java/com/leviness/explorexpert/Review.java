package com.leviness.explorexpert;

public class Review {
    private String locationName;
    private double rating;
    private String reviewText;  // New field for text review

    // Constructor
    public Review(String locationName, double rating, String reviewText) {
        this.locationName = locationName;
        this.rating = rating;
        this.reviewText = reviewText;  // Initialize the review text
    }

    // Getters and setters
    public String getLocationName() {
        return locationName;
    }

    public double getRating() {
        return rating;
    }

    public String getReviewText() {
        return reviewText;  // Getter for review text
    }
}
