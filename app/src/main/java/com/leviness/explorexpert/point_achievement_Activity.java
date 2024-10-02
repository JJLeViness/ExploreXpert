package com.leviness.explorexpert;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

public class point_achievement_Activity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point_achievement);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.menu_navigation);
        ImageView menuButton = findViewById(R.id.menuButton);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Set the menu button to open/close the drawer
        menuButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(point_achievement_Activity.this, homescreen_activity.class));
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(point_achievement_Activity.this, Map_Activity.class));
            } else if (id == R.id.nav_scavenger_hunt) {
                startActivity(new Intent(point_achievement_Activity.this, scavenger_Hunt_Activity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(point_achievement_Activity.this, settings_Activity.class));
            } else if (id == R.id.nav_profile) {
            startActivity(new Intent(point_achievement_Activity.this, profile_Activity.class));
            }

            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });
    }
}
