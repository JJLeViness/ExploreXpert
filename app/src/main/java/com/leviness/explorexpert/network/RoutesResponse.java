package com.leviness.explorexpert.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RoutesResponse {

    @SerializedName("routes")
    public List<Route> routes;

    public static class Route {
        @SerializedName("legs")
        public List<Leg> legs;
    }

    public static class Leg {
        @SerializedName("steps")
        public List<Step> steps;
    }

    public static class Step {
        @SerializedName("start_location")
        public Location startLocation;

        @SerializedName("end_location")
        public Location endLocation;

        @SerializedName("polyline")
        public Polyline polyline;

        @SerializedName("html_instructions")
        public String instructions;
    }

    public static class Location {
        @SerializedName("lat")
        public double latitude;

        @SerializedName("lng")
        public double longitude;
    }

    public static class Polyline {
        @SerializedName("points")
        public String points;
    }
}


