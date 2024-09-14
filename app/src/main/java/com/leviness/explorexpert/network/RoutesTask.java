package com.leviness.explorexpert.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.Log;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.leviness.explorexpert.Map_Activity;
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
    private Map_Activity.DirectionsAdapter adapter;
    private String travelMode;
    private RecyclerView recyclerView;

    private List<String> directionsList = new ArrayList<>();

    public RoutesTask(Context context, GoogleMap googleMap, Map_Activity.DirectionsAdapter adapter, String travelMode) {
        this.mMap = googleMap;
        this.apiKey = context.getString(R.string.maps_api_key); // Fetch API key from resources
        this.travelMode = travelMode;
        this.recyclerView = recyclerView;
        this.adapter = adapter;
    }

    @Override
    protected String doInBackground(String... params) {
        String origin = params[0]; // Starting point
        String destination = params[1]; // Ending point
        String mode = travelMode; // Mode of travel

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

                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(originLatLng, 20)); //snap camera to origin.

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

                                String stepNumberText = "Step " + (i + 1);
                                Marker stepMarker= mMap.addMarker(new MarkerOptions()
                                        .position(stepLatLng)
                                        .icon(createTextIcon(stepNumberText))
                                        .anchor(0.5f, 0.5f));
                                stepMarker.setTag("non-clickable");

                                directionsList.add(htmlToString(instruction));

                                adapter.updateDirections(directionsList);


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

    private BitmapDescriptor createTextIcon(String text) {

        int iconSize = 400; //
        int textSize = 60;

        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setTextSize(textSize);
        paint.setAntiAlias(true);
        paint.setFakeBoldText(true);

        Bitmap bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        int xPos = canvas.getWidth() / 2;
        int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2));
        canvas.drawText(text, xPos, yPos, paint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
