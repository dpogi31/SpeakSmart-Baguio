
package com.example.speaksmartbaguio;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.speaksmartbaguio.databinding.ActivityHomeBinding;
import com.example.speaksmartbaguio.mapper.EntityMapper;
import com.example.speaksmartbaguio.repository.PhraseRepository;
import com.example.speaksmartbaguio.utils.NetworkUtil;
import com.example.speaksmartbaguio.utils.SyncManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private PhraseRepository phraseRepository;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private boolean hasResumedOnce = false;
    private boolean lastKnownOnline = false;
    private boolean networkCallbackRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        phraseRepository = new PhraseRepository(getApplicationContext());

        setupNavigation();
        setupDailyPhraseCard();
        setupSyncIndicator();

        loadDailyPhrase();

        lastKnownOnline = NetworkUtil.isOnline(this);
        refreshConnectionStatus();

        if (lastKnownOnline) {
            startSync();
        }
    }

    // HOME NAVIGATION

    private void setupNavigation() {
        binding.cardTranslator.setOnClickListener(v ->
                openLearningTool("TRANSLATOR"));

        binding.cardDictionary.setOnClickListener(v ->
                openLearningTool("DICTIONARY"));

        binding.cardPhrasebook.setOnClickListener(v ->
                openLearningTool("PHRASEBOOK"));
    }

    private void openLearningTool(String fragmentName) {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.putExtra("START_FRAGMENT", fragmentName);
        startActivity(intent);
    }

    // DAILY PHRASE CARD

    private void setupDailyPhraseCard() {
        binding.homeDailyPhraseCard.setOnClickListener(v ->
                openLearningTool("PHRASEBOOK"));

        binding.homeDailyPhraseCard.setClickable(true);
        binding.homeDailyPhraseCard.setFocusable(true);
    }

    private void loadDailyPhrase() {
        if (binding == null || phraseRepository == null) {
            return;
        }

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

    private void displayDailyPhrase(List<Phrase> savedPhrases) {
        if (binding == null || savedPhrases == null) {
            return;
        }

        SharedPreferences preferences =
                getSharedPreferences("PhrasePrefs", Context.MODE_PRIVATE);

        String selectedLanguage =
                preferences.getString("selectedLanguage", "English");

        boolean useTagalog =
                "Tagalog".equalsIgnoreCase(selectedLanguage);

        List<Phrase> usablePhrases = new ArrayList<>();

        for (Phrase phrase : savedPhrases) {
            if (phrase == null) {
                continue;
            }

            String ilokanoText = getIlokanoDisplayText(phrase);
            String translation = getTranslation(phrase, useTagalog);

            if (!ilokanoText.trim().isEmpty()
                    && !translation.trim().isEmpty()) {
                usablePhrases.add(phrase);
            }
        }

        if (usablePhrases.isEmpty()) {
            showEmptyDailyPhrase();
            return;
        }

        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

        int dailySeed = year * 1000 + dayOfYear;
        int dailyIndex = Math.floorMod(
                dailySeed,
                usablePhrases.size()
        );

        Phrase selectedPhrase = usablePhrases.get(dailyIndex);

        binding.tvDailyPhrase.setText(
                getIlokanoDisplayText(selectedPhrase)
        );

        binding.tvDailyPhraseTranslation.setText(
                getTranslation(selectedPhrase, useTagalog)
        );

        binding.tvDailyPhrase.setVisibility(View.VISIBLE);
        binding.tvDailyPhraseTranslation.setVisibility(View.VISIBLE);
    }

    private String getIlokanoDisplayText(Phrase phrase) {
        if (phrase == null) {
            return "";
        }

        String sentence = phrase.getIlokanoSentence();

        if (sentence != null && !sentence.trim().isEmpty()) {
            return sentence.trim();
        }

        String word = phrase.getIlokanoWord();

        return word == null ? "" : word.trim();
    }

    private String getTranslation(Phrase phrase, boolean useTagalog) {
        if (phrase == null) {
            return "";
        }

        if (useTagalog) {
            String tagalog = phrase.getTagalogTranslation();

            if (tagalog != null && !tagalog.trim().isEmpty()) {
                return tagalog.trim();
            }
        }

        return getEnglishDisplayText(phrase);
    }

    private String getEnglishDisplayText(Phrase phrase) {
        if (phrase == null) {
            return "";
        }

        String sentence = phrase.getEnglishSentence();

        if (sentence != null && !sentence.trim().isEmpty()) {
            return sentence.trim();
        }

        String translation = phrase.getEnglishTranslation();

        return translation == null ? "" : translation.trim();
    }

    private void showEmptyDailyPhrase() {
        if (binding == null) {
            return;
        }

        binding.tvDailyPhrase.setText(
                "Daily phrase is not available yet."
        );

        binding.tvDailyPhraseTranslation.setText(
                "Open Phrasebook to load saved phrases."
        );

        binding.tvDailyPhrase.setVisibility(View.VISIBLE);
        binding.tvDailyPhraseTranslation.setVisibility(View.VISIBLE);
    }

    // OFFLINE / ONLINE / SYNC INDICATOR

    private void setupSyncIndicator() {
        binding.btnRetrySync.setOnClickListener(v -> {
            if (!NetworkUtil.isOnline(this)) {
                refreshConnectionStatus();
                binding.tvSyncStatus.setText(
                        "No internet connection. Please reconnect and retry."
                );
                return;
            }

            startSync();
        });

        updateLastSyncText();
        binding.progressSync.setVisibility(View.GONE);
    }

    private void startSync() {
        if (binding == null) {
            return;
        }

        if (!NetworkUtil.isOnline(this)) {
            refreshConnectionStatus();

            binding.tvSyncStatus.setText(
                    "Offline mode. Your saved data remains available."
            );

            return;
        }

        binding.btnRetrySync.setEnabled(false);
        binding.progressSync.setVisibility(View.VISIBLE);
        binding.tvSyncStatus.setText("Checking for updates...");

        SyncManager.sync(this, new SyncManager.SyncListener() {
            @Override
            public void onSyncStarted() {
                if (binding == null || isFinishing() || isDestroyed()) {
                    return;
                }

                binding.progressSync.setVisibility(View.VISIBLE);
                binding.btnRetrySync.setEnabled(false);
                binding.tvSyncStatus.setText("Syncing...");
            }

            @Override
            public void onSyncFinished(
                    boolean success,
                    String message
            ) {
                if (binding == null || isFinishing() || isDestroyed()) {
                    return;
                }

                binding.progressSync.setVisibility(View.GONE);
                binding.btnRetrySync.setEnabled(true);

                binding.tvSyncStatus.setText(message);

                if (success) {
                    updateLastSyncText();
                } else {
                    updateLastSyncText();
                }

                refreshConnectionStatus();
            }
        });
    }

    private void refreshConnectionStatus() {
        if (binding == null) {
            return;
        }

        boolean online = NetworkUtil.isOnline(this);

        binding.tvConnectionStatus.setText(
                online ? "Online" : "Offline mode"
        );

        if (!online && !SyncManager.isSyncing()) {
            binding.tvSyncStatus.setText(
                    "Your saved data is available offline."
            );
        }
    }

    private void updateLastSyncText() {
        if (binding == null) {
            return;
        }

        long lastSync = SyncManager.getLastSuccessfulSync(this);

        if (lastSync <= 0) {
            binding.tvLastSync.setText(
                    "Last successful sync: Not yet synced"
            );
            return;
        }

        SimpleDateFormat formatter = new SimpleDateFormat(
                "MMM d, yyyy h:mm a",
                Locale.getDefault()
        );

        binding.tvLastSync.setText(
                "Last successful sync: "
                        + formatter.format(new Date(lastSync))
        );
    }

    // MONITOR CONNECTIVITY CHANGES

    @Override
    protected void onStart() {
        super.onStart();

        connectivityManager =
                (ConnectivityManager) getSystemService(
                        Context.CONNECTIVITY_SERVICE
                );

        if (connectivityManager == null || networkCallbackRegistered) {
            return;
        }

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> handleConnectivityChange());
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> handleConnectivityChange());
            }

            @Override
            public void onCapabilitiesChanged(
                    Network network,
                    NetworkCapabilities capabilities
            ) {
                runOnUiThread(() -> handleConnectivityChange());
            }
        };

        try {
            connectivityManager.registerNetworkCallback(
                    request,
                    networkCallback
            );

            networkCallbackRegistered = true;
        } catch (Exception ignored) {
            // Initial status checks still work if registration fails.
        }
    }

    private void handleConnectivityChange() {
        if (binding == null) {
            return;
        }

        boolean online = NetworkUtil.isOnline(this);
        boolean wasOnline = lastKnownOnline;

        lastKnownOnline = online;
        refreshConnectionStatus();

        if (online && !wasOnline) {
            startSync();
        }
    }

    @Override
    protected void onStop() {
        if (connectivityManager != null
                && networkCallback != null
                && networkCallbackRegistered) {
            try {
                connectivityManager.unregisterNetworkCallback(
                        networkCallback
                );
            } catch (Exception ignored) {
                // Callback may already have been unregistered.
            }
        }

        networkCallbackRegistered = false;
        networkCallback = null;

        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (hasResumedOnce) {
            loadDailyPhrase();
            refreshConnectionStatus();
            updateLastSyncText();
        } else {
            hasResumedOnce = true;
        }
    }

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
