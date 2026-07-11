package com.example.speaksmartbaguio;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.speaksmartbaguio.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Translator card
        binding.cardTranslator.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.putExtra("START_FRAGMENT", "TRANSLATOR");
            startActivity(intent);
        });

        // Dictionary card
        binding.cardDictionary.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.putExtra("START_FRAGMENT", "DICTIONARY");
            startActivity(intent);
        });

        // Phrasebook card
        binding.cardPhrasebook.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.putExtra("START_FRAGMENT", "PHRASEBOOK");
            startActivity(intent);
        });
    }
}
