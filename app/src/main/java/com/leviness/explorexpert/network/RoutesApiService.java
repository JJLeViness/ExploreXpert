package com.leviness.explorexpert.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface RoutesApiService {

    @Headers({
            "Content-Type: application/json",
            "X-Goog-FieldMask: routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline"
    })
    @POST("directions/v2:computeRoutes")
    Call<RoutesResponse> getRoute(
            @Body RouteRequestBody requestBody
    );
}

