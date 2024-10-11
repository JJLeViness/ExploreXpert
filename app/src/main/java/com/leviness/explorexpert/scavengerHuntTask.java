package com.leviness.explorexpert;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

public class scavengerHuntTask implements Parcelable {
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

    // Parcelable implementation
    protected scavengerHuntTask(Parcel in) {
        placeName = in.readString();
        description = in.readString();
        double latitude = in.readDouble();
        double longitude = in.readDouble();
        location = new LatLng(latitude, longitude);
    }

    public static final Creator<scavengerHuntTask> CREATOR = new Creator<scavengerHuntTask>() {
        @Override
        public scavengerHuntTask createFromParcel(Parcel in) {
            return new scavengerHuntTask(in);
        }

        @Override
        public scavengerHuntTask[] newArray(int size) {
            return new scavengerHuntTask[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(placeName);
        parcel.writeString(description);
        parcel.writeDouble(location.latitude);
        parcel.writeDouble(location.longitude);
    }
}
