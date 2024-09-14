package com.leviness.explorexpert.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class WikipediaAPI {
    private static final String WIKIPEDIA_API_URL = "https://en.wikipedia.org/api/rest_v1/page/summary/";

    public String getLocationSummary(String locationName) {
        try {
            // Encode the location name to replace spaces with underscores for the Wikipedia API.
            String encodedLocationName = locationName.replace(" ", "_");
            URL url = new URL(WIKIPEDIA_API_URL + encodedLocationName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            // Read the API response
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            // Close connections
            in.close();
            connection.disconnect();

            // Parse the JSON response
            JSONObject json = new JSONObject(content.toString());
            if (json.has("extract")) {
                return json.getString("extract");
            } else {
                return "No brief information available for this location.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error fetching information about the location.";
        }
    }
}

