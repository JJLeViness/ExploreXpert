package com.leviness.explorexpert.network;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class DirectionsAdapter extends RecyclerView.Adapter<DirectionsAdapter.DirectionsViewHolder> {

    private List<String> directionsList;
    private List<LatLng> stepLatLngs;
    private RecyclerView recyclerView;
    private GoogleMap mMap;

    public DirectionsAdapter(List<String> directionsList, List<LatLng> stepLatLngs, RecyclerView recyclerView, GoogleMap mMap) {
        this.directionsList = directionsList;
        this.stepLatLngs = stepLatLngs;
        this.recyclerView = recyclerView;
        this.mMap = mMap;
    }

    public void updateDirections(List<String> newDirections, List<LatLng> newStepLatLngs) {
        this.directionsList.clear();
        this.directionsList.addAll(newDirections);
        this.stepLatLngs.clear();
        this.stepLatLngs.addAll(newStepLatLngs);
        notifyDataSetChanged();

        if (directionsList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @NonNull
    @Override
    public DirectionsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new DirectionsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DirectionsViewHolder holder, int position) {
        String stepText = "Step " + (position + 1) + ": " + directionsList.get(position);
        holder.directionsTextView.setText(stepText);

        holder.itemView.setOnClickListener(v -> {
            // Move the map's camera to the step's LatLng position when the item is clicked
            LatLng stepLatLng = stepLatLngs.get(position);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stepLatLng, 20));
        });
    }

    @Override
    public int getItemCount() {
        return directionsList.size();
    }

    public static class DirectionsViewHolder extends RecyclerView.ViewHolder {
        TextView directionsTextView;

        public DirectionsViewHolder(@NonNull View itemView) {
            super(itemView);
            directionsTextView = itemView.findViewById(android.R.id.text1);
        }
    }
}
