package com.example.speaksmartbaguio;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.speaksmartbaguio.databinding.ActivityHomeBinding;
import com.example.speaksmartbaguio.mapper.EntityMapper;
import com.example.speaksmartbaguio.repository.PhraseRepository;
import com.example.speaksmartbaguio.entity.PhraseEntity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private PhraseRepository phraseRepository;

    private boolean hasResumedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        phraseRepository = new PhraseRepository(getApplicationContext());

        setupNavigation();
        setupDailyPhraseCard();
        loadDailyPhrase();
    }

    // ---------------------------------------------------------
    // HOME NAVIGATION
    // ---------------------------------------------------------

    private void setupNavigation() {

        binding.cardTranslator.setOnClickListener(v -> {
            openLearningTool("TRANSLATOR");
        });

        binding.cardDictionary.setOnClickListener(v -> {
            openLearningTool("DICTIONARY");
        });

        binding.cardPhrasebook.setOnClickListener(v -> {
            openLearningTool("PHRASEBOOK");
        });
    }

    private void openLearningTool(String fragmentName) {

        Intent intent = new Intent(
                HomeActivity.this,
                MainActivity.class
        );

        intent.putExtra("START_FRAGMENT", fragmentName);

        startActivity(intent);
    }

    // ---------------------------------------------------------
    // DAILY PHRASE CARD
    // ---------------------------------------------------------

    private void setupDailyPhraseCard() {

        // Keep the existing behavior: tapping the Daily Phrase
        // card opens the Phrasebook screen.

        binding.homeDailyPhraseCard.setOnClickListener(v -> {
            openLearningTool("PHRASEBOOK");
        });

        binding.homeDailyPhraseCard.setClickable(true);
        binding.homeDailyPhraseCard.setFocusable(true);
    }

    // ---------------------------------------------------------
    // LOAD DAILY PHRASE FROM THE LOCAL ROOM DATABASE
    // ---------------------------------------------------------

    private void loadDailyPhrase() {

        if (binding == null || phraseRepository == null) {
            return;
        }

        // Read the saved phrases asynchronously.
        // This prevents database work from blocking the UI.

        phraseRepository.getAllPhrases(result -> {

            if (isFinishing() || isDestroyed()) {
                return;
            }

            runOnUiThread(() -> {

                if (binding == null) {
                    return;
                }

                List<Phrase> savedPhrases =
                        EntityMapper.toPhraseList(result);

                if (savedPhrases == null || savedPhrases.isEmpty()) {
                    showEmptyDailyPhrase();
                    return;
                }

                displayDailyPhrase(savedPhrases);
            });
        });
    }

    // ---------------------------------------------------------
    // DISPLAY A CONSISTENT PHRASE FOR THE CURRENT DAY
    // ---------------------------------------------------------

    private void displayDailyPhrase(List<Phrase> savedPhrases) {

        if (binding == null || savedPhrases == null) {
            return;
        }

        // Use the language previously selected in Phrasebook.
        SharedPreferences preferences =
                getSharedPreferences(
                        "PhrasePrefs",
                        Context.MODE_PRIVATE
                );

        String selectedLanguage = preferences.getString(
                "selectedLanguage",
                "English"
        );

        boolean useTagalog =
                "Tagalog".equalsIgnoreCase(selectedLanguage);

        // Keep only phrases that have an Ilokano text and
        // an available translation in the selected language.

        List<Phrase> usablePhrases = new ArrayList<>();

        for (Phrase phrase : savedPhrases) {

            if (phrase == null) {
                continue;
            }

            String ilokanoText = getIlokanoDisplayText(phrase);

            String translation = getTranslation(
                    phrase,
                    useTagalog
            );

            if (!ilokanoText.trim().isEmpty()
                    && !translation.trim().isEmpty()) {

                usablePhrases.add(phrase);
            }
        }

        if (usablePhrases.isEmpty()) {
            showEmptyDailyPhrase();
            return;
        }

        // Select a repeatable index using today's date.
        // With the same saved list, the selected phrase stays
        // the same throughout the day.

        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

        int dailySeed = year * 1000 + dayOfYear;

        int dailyIndex = Math.floorMod(
                dailySeed,
                usablePhrases.size()
        );

        Phrase selectedPhrase = usablePhrases.get(dailyIndex);

        String ilokanoText =
                getIlokanoDisplayText(selectedPhrase);

        String translation = getTranslation(
                selectedPhrase,
                useTagalog
        );

        binding.tvDailyPhrase.setText(ilokanoText);
        binding.tvDailyPhraseTranslation.setText(translation);

        binding.tvDailyPhrase.setVisibility(View.VISIBLE);
        binding.tvDailyPhraseTranslation.setVisibility(View.VISIBLE);
    }

    // ---------------------------------------------------------
    // GET THE BEST AVAILABLE ILOKANO TEXT
    // ---------------------------------------------------------

    private String getIlokanoDisplayText(Phrase phrase) {

        if (phrase == null) {
            return "";
        }

        // Prefer the complete Ilokano sentence when available.
        String sentence = phrase.getIlokanoSentence();

        if (sentence != null && !sentence.trim().isEmpty()) {
            return sentence.trim();
        }

        // Otherwise, use the saved Ilokano word or phrase.
        String word = phrase.getIlokanoWord();

        if (word != null) {
            return word.trim();
        }

        return "";
    }

    // ---------------------------------------------------------
    // GET THE TRANSLATION FOR THE SELECTED LANGUAGE
    // ---------------------------------------------------------

    private String getTranslation(
            Phrase phrase,
            boolean useTagalog
    ) {

        if (phrase == null) {
            return "";
        }

        if (useTagalog) {

            String tagalog = phrase.getTagalogTranslation();

            if (tagalog != null && !tagalog.trim().isEmpty()) {
                return tagalog.trim();
            }

            // If Tagalog is unavailable, try the English
            // translation rather than leaving the card blank.
            return getEnglishDisplayText(phrase);
        }

        return getEnglishDisplayText(phrase);
    }

    private String getEnglishDisplayText(Phrase phrase) {

        if (phrase == null) {
            return "";
        }

        // Prefer the full English sentence when available.
        String sentence = phrase.getEnglishSentence();

        if (sentence != null && !sentence.trim().isEmpty()) {
            return sentence.trim();
        }

        // Otherwise, use the saved English translation.
        String translation = phrase.getEnglishTranslation();

        if (translation != null) {
            return translation.trim();
        }

        return "";
    }

    // ---------------------------------------------------------
    // EMPTY DATABASE / NO USABLE PHRASES
    // ---------------------------------------------------------

    private void showEmptyDailyPhrase() {

        if (binding == null) {
            return;
        }

        // These are status messages, not hardcoded daily phrases.
        // The actual phrase comes from the saved Phrasebook data.

        binding.tvDailyPhrase.setText(
                "Daily phrase is not available yet."
        );

        binding.tvDailyPhraseTranslation.setText(
                "Open Phrasebook to load saved phrases."
        );

        binding.tvDailyPhrase.setVisibility(View.VISIBLE);
        binding.tvDailyPhraseTranslation.setVisibility(View.VISIBLE);
    }

    // ---------------------------------------------------------
    // REFRESH WHEN RETURNING FROM PHRASEBOOK
    // ---------------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();

        // onCreate() already loads the phrase the first time.
        // Reload only when returning to Home afterward.

        if (hasResumedOnce) {
            loadDailyPhrase();
        } else {
            hasResumedOnce = true;
        }
    }

    // ---------------------------------------------------------
    // CLEANUP
    // ---------------------------------------------------------

    @Override
    protected void onDestroy() {

        if (phraseRepository != null) {
            phraseRepository.close();
            phraseRepository = null;
        }

        binding = null;

        super.onDestroy();
    }
}
