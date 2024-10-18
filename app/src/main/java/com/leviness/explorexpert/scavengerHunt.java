package com.leviness.explorexpert;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class scavengerHunt implements Parcelable {
    private String name;
    private List<scavengerHuntTask> tasks;
    private String description;

    // Constructor
    public scavengerHunt(String name, String description, List<scavengerHuntTask> tasks) {
        this.name = name;
        this.description = description;
        this.tasks = tasks;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<scavengerHuntTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<scavengerHuntTask> tasks) {
        this.tasks = tasks;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Parcelable implementation
    protected scavengerHunt(Parcel in) {
        name = in.readString();
        description = in.readString();
        tasks = in.createTypedArrayList(scavengerHuntTask.CREATOR);
    }

    public static final Creator<scavengerHunt> CREATOR = new Creator<scavengerHunt>() {
        @Override
        public scavengerHunt createFromParcel(Parcel in) {
            return new scavengerHunt(in);
        }

        @Override
        public scavengerHunt[] newArray(int size) {
            return new scavengerHunt[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(name);
        parcel.writeString(description);
        parcel.writeTypedList(tasks);
    }
}
