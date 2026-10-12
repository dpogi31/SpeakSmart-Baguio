package com.example.speaksmartbaguio;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.speaksmartbaguio.databinding.ActivityMainBinding;
import com.example.speaksmartbaguio.utils.SyncManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Keep the existing synchronization behavior.
        SyncManager.sync(getApplicationContext());

        BottomNavigationView navView = binding.navView;

        // Find the NavController.
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(
                                R.id.nav_host_fragment_activity_main
                        );

        if (navHostFragment == null) {
            return;
        }

        NavController navController =
                navHostFragment.getNavController();

        // Connect the bottom navigation to the navigation graph.
        NavigationUI.setupWithNavController(
                navView,
                navController
        );

        // Handle requests to open a specific destination.
        Intent intent = getIntent();

        if (intent != null && intent.hasExtra("START_FRAGMENT")) {

            String startFragment =
                    intent.getStringExtra("START_FRAGMENT");

            if ("DICTIONARY".equals(startFragment)) {

                navView.setSelectedItemId(
                        R.id.navigation_dictionary
                );

            } else if ("PHRASEBOOK".equals(startFragment)) {

                navView.setSelectedItemId(
                        R.id.navigation_phrasebook
                );

            } else if ("TRANSLATOR".equals(startFragment)) {

                navView.setSelectedItemId(
                        R.id.navigation_translator
                );

            } else if ("FAVORITES".equals(startFragment)) {

                navView.setSelectedItemId(
                        R.id.navigation_favorites
                );
            }
        }
    }
}