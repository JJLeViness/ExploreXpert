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
import java.util.Random;

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

    private String getRandomFunFact() {
        String[] funFacts = {
                "Did you know? The New York subway system is the largest in the world by number of stations.",
                "Fun fact: Manhattan’s Chinatown is one of the oldest in the United States.",
                "Did you know? Central Park is larger than the country of Monaco.",
                "Fun fact: The Empire State Building has its own zip code – 10118.",
                "Did you know? Times Square is named after The New York Times newspaper.",
                "Fun fact: New York was the first capital of the United States in 1789.",
                "Did you know? The Statue of Liberty was a gift from France in 1886.",
                "Fun fact: The Brooklyn Bridge was the first bridge to use steel wire in its construction.",
                "Did you know? Grand Central Terminal has a hidden tennis court on its top floor.",
                "Fun fact: New York City’s Federal Reserve Bank holds the world’s largest gold storage."
        };

        Random random = new Random();
        return funFacts[random.nextInt(funFacts.length)];
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
                                listener.onResult("No description available. " + getRandomFunFact());
                            }
                        } else {
                            listener.onResult("No description available. " + getRandomFunFact());
                        }
                    } else {
                        listener.onResult("No description available. " + getRandomFunFact());
                    }
                } else {
                    listener.onError("Error fetching data.");
                }
            }

        }.execute();
    }
}

