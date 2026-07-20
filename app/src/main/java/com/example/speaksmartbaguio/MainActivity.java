package com.example.speaksmartbaguio;

import android.content.Intent;
import android.os.Bundle;
import com.example.speaksmartbaguio.utils.SyncManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.speaksmartbaguio.utils.SyncManager;
import com.example.speaksmartbaguio.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SyncManager.sync(getApplicationContext());
        BottomNavigationView navView = binding.navView;

        // Find NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);
        NavController navController = navHostFragment.getNavController();

        // Link BottomNavigationView with NavController
        NavigationUI.setupWithNavController(navView, navController);

        // Handle starting fragment from Home
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("START_FRAGMENT")) {
            String startFragment = intent.getStringExtra("START_FRAGMENT");

            // Use setSelectedItemId to avoid NavController mismatch
            if ("DICTIONARY".equals(startFragment)) {
                navView.setSelectedItemId(R.id.navigation_dictionary);
            } else if ("PHRASEBOOK".equals(startFragment)) {
                navView.setSelectedItemId(R.id.navigation_phrasebook);
            } else if ("TRANSLATOR".equals(startFragment)) {
                navView.setSelectedItemId(R.id.navigation_translator);
            }
        }
    }
}
