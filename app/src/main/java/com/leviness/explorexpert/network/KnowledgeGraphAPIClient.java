package com.leviness.explorexpert.network;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class KnowledgeGraphAPIClient {

    private static final String BASE_URL = "https://kgsearch.googleapis.com/v1/entities:search";
    private String apiKey;

    public KnowledgeGraphAPIClient(String apiKey) {
        this.apiKey = apiKey;
    }

    // Callback interface for delivering results
    public interface OnKnowledgeGraphResultListener {
        void onResult(String description);
        void onError(String errorMessage);
    }

    // Fetch general information about a place using its name
    public void fetchGeneralInfoForPlace(String placeName, OnKnowledgeGraphResultListener listener) {
        new AsyncTask<Void, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(Void... voids) {
                try {
                    String encodedPlaceName = URLEncoder.encode(placeName, "UTF-8");
                    String urlString = BASE_URL + "?query=" + encodedPlaceName + "&key=" + apiKey + "&limit=1&indent=true";

                    URL url = new URL(urlString);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        StringBuilder responseBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                        return new JSONObject(responseBuilder.toString());
                    }
                } catch (Exception e) {
                    listener.onError("Error fetching data: " + e.getMessage());
                    Log.e("KnowledgeGraphAPI", "Error fetching data: " + e.getMessage(), e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(JSONObject entity) {
                if (entity != null) {
                    JSONArray itemList = entity.optJSONArray("itemListElement");
                    if (itemList != null && itemList.length() > 0) {
                        JSONObject result = itemList.optJSONObject(0).optJSONObject("result");
                        if (result != null) {
                            // Extracting the detailed description
                            JSONObject detailedDescription = result.optJSONObject("detailedDescription");
                            if (detailedDescription != null) {
                                String articleBody = detailedDescription.optString("articleBody", "No description available.");
                                listener.onResult(articleBody);
                            } else {
                                listener.onResult("No description available.");
                            }
                        } else {
                            listener.onResult("No description available.");
                        }
                    } else {
                        listener.onResult("No general information available.");
                    }
                } else {
                    listener.onError("Error fetching data.");
                }
            }

        }.execute();
    }
}

