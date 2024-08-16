package com.leviness.explorexpert;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.Manifest;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.navigation.NavigationView;
import com.leviness.explorexpert.network.RetrofitClient;
import com.leviness.explorexpert.network.RoutesApiService;
import com.leviness.explorexpert.network.RoutesResponse;
import com.leviness.explorexpert.network.RouteRequestBody;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Map_Activity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private RoutesApiService routesApiService;
    private FusedLocationProviderClient fusedLocationClient;
    private ImageView menuButton;
    private DrawerLayout menuNavigation;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_map);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        menuButton = findViewById(R.id.map_menuButton);
        menuNavigation = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.menu_navigation);
        routesApiService = RetrofitClient.getClient().create(RoutesApiService.class); //For RoutesAPI

        //Menu drawer
        toggle = new ActionBarDrawerToggle(this, menuNavigation, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        menuNavigation.addDrawerListener(toggle);
        toggle.syncState();
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (menuNavigation.isDrawerOpen(GravityCompat.END)) {
                    menuNavigation.closeDrawer(GravityCompat.END);
                } else {
                    menuNavigation.openDrawer(GravityCompat.END);
                }
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    }

//Map Logic and snap to current location
    @Override
    public void onMapReady(GoogleMap googleMap) {



        mMap = googleMap;
        // Check if the location permissions are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Request the permissions if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }




        mMap.setMyLocationEnabled(true);

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true); // Ensures that the user can scroll/pan the map
        mMap.getUiSettings().setZoomGesturesEnabled(true);   // Allows zooming

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {

                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());


                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));


                        mMap.addMarker(new MarkerOptions().position(currentLocation).title("You are here"));

                       /* String origin = location.getLatitude() + "," + location.getLongitude();
                        String destination = "37.7749,-122.4194"; // Example destination (San Francisco coordinates)
                        String travelMode = "DRIVE";
                        String apiKey = getString(R.string.maps_api_key);
                        fetchDirections(origin, destination);
                        routesApiService.getRoute(origin, destination, travelMode, apiKey).enqueue(new Callback<RoutesResponse>() {
                            @Override
                            public void onResponse(Call<RoutesResponse> call, Response<RoutesResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    Log.d("API Success", "Routes found: " + response.body().routes.size());


                                } else {
                                    Log.e("API Error", "API response was not successful.");
                                    Log.e("API Error", "Response: " + response.errorBody());
                                }
                            }

                            @Override
                            public void onFailure(Call<RoutesResponse> call, Throwable t) {
                                Log.e("API Error", "Request failed: " + t.getMessage());
                            }
                        });*/

                    }
                });
//Menu Drawer logic
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    startActivity(new Intent(Map_Activity.this, homescreen_activity.class));
                } else if (id == R.id.nav_map) {
                    startActivity(new Intent(Map_Activity.this, Map_Activity.class));
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(Map_Activity.this, profile_Activity.class));
                } else if (id == R.id.nav_scavenger_hunt) {
                    startActivity(new Intent(Map_Activity.this, scavenger_Hunt_Activity.class));
                } else if (id == R.id.nav_settings) {
                    startActivity(new Intent(Map_Activity.this, settings_Activity.class));
                } else if (id == R.id.nav_login) {
                    startActivity(new Intent(Map_Activity.this, login_Activity.class));
                } else if (id == R.id.nav_logout) {
                    // Handle logout logic here
                }

                menuNavigation.closeDrawer(GravityCompat.END);
                return true;
            }
        });





    }

    private void fetchDirections(double originLat, double originLng, double destinationLat, double destinationLng) {
        RouteRequestBody requestBody = new RouteRequestBody();
        requestBody.origin = new RouteRequestBody.Location(originLat, originLng);
        requestBody.destination = new RouteRequestBody.Location(destinationLat, destinationLng);

        Call<RoutesResponse> call = routesApiService.getRoute(requestBody);
        call.enqueue(new Callback<RoutesResponse>() {
            @Override
            public void onResponse(Call<RoutesResponse> call, Response<RoutesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RoutesResponse.Route route = response.body().routes.get(0);
                    if (route != null) {
                        drawRouteOnMap(route);
                        showStepByStepInstructions(route.legs.get(0).steps);
                    } else {
                        Log.d("MapActivity", "No route found in response.");
                    }
                } else {
                    Log.e("API Error", "API response was not successful: " + response.errorBody());
                }
            }

            @Override
            public void onFailure(Call<RoutesResponse> call, Throwable t) {
                Log.e("API Error", "Request failed: " + t.getMessage());
            }
        });
    }






    private void drawRoute(RoutesResponse routesResponse) {
        if (routesResponse != null && routesResponse.routes != null) {
            for (RoutesResponse.Route route : routesResponse.routes) {
                PolylineOptions polylineOptions = new PolylineOptions();
                for (RoutesResponse.Leg leg : route.legs) {
                    for (RoutesResponse.Step step : leg.steps) {
                        polylineOptions.add(new LatLng(step.startLocation.latitude, step.startLocation.longitude));
                        polylineOptions.add(new LatLng(step.endLocation.latitude, step.endLocation.longitude));
                    }
                }
                mMap.addPolyline(polylineOptions);
            }
        }


    }

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

    private void drawRouteOnMap(RoutesResponse.Route route) {
        for (RoutesResponse.Leg leg : route.legs) {
            for (RoutesResponse.Step step : leg.steps) {
                List<LatLng> decodedPath = decodePolyline(step.polyline.points);

                // Add the polyline to the map
                mMap.addPolyline(new PolylineOptions().addAll(decodedPath).color(Color.BLUE).width(10f));
            }
        }
    }
    private void showStepByStepInstructions(List<RoutesResponse.Step> steps) {
        for (RoutesResponse.Step step : steps) {
            LatLng position = new LatLng(step.startLocation.latitude, step.startLocation.longitude);
            String instruction = android.text.Html.fromHtml(step.instructions).toString();

            // Add a marker for each step
            mMap.addMarker(new MarkerOptions().position(position).title(instruction));


        }
    }

}