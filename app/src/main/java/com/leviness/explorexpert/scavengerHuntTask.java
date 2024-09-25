package com.leviness.explorexpert;

import com.google.android.gms.maps.model.LatLng;

public class scavengerHuntTask {
private String placeName;
private LatLng location;
private String description;

    // Constructor
    public scavengerHuntTask(String placeName, String description, LatLng location) {
        this.placeName = placeName;
        this.description = description;
        this.location = location;
    }

    // Getters and Setters
    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }
}