package com.leviness.explorexpert.network;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.leviness.explorexpert.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RoutesTask extends AsyncTask<String, Void, String> {
    private static final String TAG = "RoutesApiTask";
    private GoogleMap mMap;
    private String apiKey;
    private LatLng originLatLng;
    private LatLng destinationLatLng;

    public RoutesTask(Context context, GoogleMap googleMap) {
        this.mMap = googleMap;
        this.apiKey = context.getString(R.string.maps_api_key); // Fetch API key from resources
    }

    @Override
    protected String doInBackground(String... params) {
        String origin = params[0]; // Starting point
        String destination = params[1]; // Ending point
        String mode = "driving"; // Mode of travel

        originLatLng = parseLatLng(origin); // Parse the origin string into a LatLng object
        destinationLatLng = parseLatLng(destination); // Parse the destination string into a LatLng object

        try {
            // Construct the URL for the API request
            String urlString = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin +
                    "&destination=" + destination + "&mode=" + mode + "&key=" + apiKey;

            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the response
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }

            reader.close();
            return responseBuilder.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error in Routes API request", e);
        }

        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        if (result != null) {
            try {
                JSONObject jsonResponse = new JSONObject(result);
                JSONArray routes = jsonResponse.getJSONArray("routes");

                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                    String encodedPolyline = overviewPolyline.getString("points");

                    List<LatLng> points = decodePolyline(encodedPolyline);

                    // Draw polyline on the map
                    if (mMap != null) {
                        mMap.addPolyline(new PolylineOptions()
                                .addAll(points)
                                .width(20)
                                .color(Color.BLUE));

                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(originLatLng, 15)); //snap camera to origin.

                        // Add a marker at the origin
                        mMap.addMarker(new MarkerOptions().position(originLatLng).title("Start"));

                        // Add a marker at the destination
                        mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destination"));

                        // Add markers for each step in the directions for testing purposes
                        JSONArray legs = route.getJSONArray("legs");
                        if (legs.length() > 0) {
                            JSONArray steps = legs.getJSONObject(0).getJSONArray("steps");
                            for (int i = 0; i < steps.length(); i++) {
                                JSONObject step = steps.getJSONObject(i);
                                JSONObject startLocation = step.getJSONObject("start_location");
                                LatLng stepLatLng = new LatLng(
                                        startLocation.getDouble("lat"),
                                        startLocation.getDouble("lng")
                                );
                                String instruction = step.getString("html_instructions"); // Get instruction text

                                mMap.addMarker(new MarkerOptions().position(stepLatLng).title(htmlToString(instruction)));// Add a marker for each step TESTING PURPOSES
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON response", e);
            }
        }
    }

    // Helper method to decode polyline
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    private LatLng parseLatLng(String latLngString) {
        String[] parts = latLngString.split(",");
        double lat = Double.parseDouble(parts[0]);
        double lng = Double.parseDouble(parts[1]);
        return new LatLng(lat, lng);
    }

    private String htmlToString(String html) {
        return android.text.Html.fromHtml(html).toString();
    }
}
