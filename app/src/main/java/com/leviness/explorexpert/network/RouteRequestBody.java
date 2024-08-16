package com.leviness.explorexpert.network;

public class RouteRequestBody {
    public Location origin;
    public Location destination;
    public String travelMode = "DRIVE";
    public String routingPreference = "TRAFFIC_AWARE";
    public boolean computeAlternativeRoutes = false;
    public RouteModifiers routeModifiers = new RouteModifiers();
    public String languageCode = "en-US";
    public String units = "IMPERIAL";

    public static class Location {
        public LatLng latLng;

        public Location(double latitude, double longitude) {
            this.latLng = new LatLng(latitude, longitude);
        }

        public static class LatLng {
            public double latitude;
            public double longitude;

            public LatLng(double latitude, double longitude) {
                this.latitude = latitude;
                this.longitude = longitude;
            }
        }
    }

    public static class RouteModifiers {
        public boolean avoidTolls = false;
        public boolean avoidHighways = false;
        public boolean avoidFerries = false;
    }
}

