package com.example.speaksmartbaguio;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.speaksmartbaguio.utils.AccessibilityPreferences;

import java.io.IOException;
import java.util.Locale;

public class WordDetailActivity extends AppCompatActivity {

    private TextView tvIlokano;
    private TextView tvEnglish;
    private TextView tvTagalog;
    private TextView tvPartOfSpeech;
    private TextView tvCategory;
    private TextView tvDefinition;

    private ImageButton btnBack;
    private ImageButton btnSpeakWord;

    private TextToSpeech tts;
    private MediaPlayer mediaPlayer;

    private String ilokanoWord = "";
    private String ttsUrl = "";

    private boolean ttsReady = false;
    private boolean activityDestroyed = false;

    private String pendingSpeech = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_detail);

        // Connect the existing layout views.
        tvIlokano = findViewById(R.id.tvIlokano);
        tvEnglish = findViewById(R.id.tvEnglish);
        tvTagalog = findViewById(R.id.tvTagalog);
        tvPartOfSpeech = findViewById(R.id.tvPartOfSpeech);
        tvCategory = findViewById(R.id.tvCategory);
        tvDefinition = findViewById(R.id.tvDefinition);

        btnBack = findViewById(R.id.btnBack);
        btnSpeakWord = findViewById(R.id.btnSpeakWord);

        btnBack.setOnClickListener(v -> finish());

        // Receive the selected word from DictionaryAdapter.
        ilokanoWord = getIntent().getStringExtra("ilokano");
        String english = getIntent().getStringExtra("english");
        String tagalog = getIntent().getStringExtra("tagalog");
        String pos = getIntent().getStringExtra("pos");
        String category = getIntent().getStringExtra("category");
        String definition = getIntent().getStringExtra("definition");
        ttsUrl = getIntent().getStringExtra("ttsUrl");

        // Prevent null values from appearing in the interface.
        if (ilokanoWord == null) {
            ilokanoWord = "";
        }

        if (english == null || english.trim().isEmpty()) {
            english = "No English translation available.";
        }

        if (tagalog == null || tagalog.trim().isEmpty()) {
            tagalog = "No Tagalog translation available.";
        }

        if (pos == null || pos.trim().isEmpty()) {
            pos = "Part of speech not specified";
        }

        if (category == null || category.trim().isEmpty()) {
            category = "Category not specified";
        }

        if (definition == null || definition.trim().isEmpty()) {
            definition = "No definition available.";
        }

        if (ttsUrl == null) {
            ttsUrl = "";
        }

        tvIlokano.setText(ilokanoWord);
        tvEnglish.setText(english);
        tvTagalog.setText(tagalog);
        tvPartOfSpeech.setText(pos);
        tvCategory.setText(category);
        tvDefinition.setText(definition);

        // Apply saved font-size, high-contrast, and simplified-layout settings.
        AccessibilityPreferences.applyToViewTree(
                this,
                getWindow().getDecorView()
        );

        // Initialize Android Text-to-Speech.
        initializeTextToSpeech();

        // Play the word when the speaker button is tapped.
        btnSpeakWord.setOnClickListener(v -> playPronunciation());
    }

    private void initializeTextToSpeech() {
        tts = new TextToSpeech(
                getApplicationContext(),
                status -> {
                    if (activityDestroyed || isFinishing()) {
                        if (tts != null) {
                            tts.shutdown();
                            tts = null;
                        }
                        return;
                    }

                    if (status == TextToSpeech.SUCCESS && tts != null) {
                        ttsReady = true;

                        // Prefer Ilokano if the installed engine supports it.
                        Locale ilokanoLocale = new Locale("ilo", "PH");
                        int result = tts.setLanguage(ilokanoLocale);

                        // Filipino is the next-best available fallback.
                        if (result == TextToSpeech.LANG_MISSING_DATA
                                || result == TextToSpeech.LANG_NOT_SUPPORTED) {

                            Locale filipinoLocale = new Locale("fil", "PH");
                            result = tts.setLanguage(filipinoLocale);
                        }

                        // Keep the engine's default language if neither is supported.
                        if (result == TextToSpeech.LANG_MISSING_DATA
                                || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            tts.setLanguage(Locale.getDefault());
                        }

                        tts.setSpeechRate(0.85f);
                        tts.setPitch(1.0f);

                        // Speak if the user tapped before TTS finished initializing.
                        if (pendingSpeech != null) {
                            String wordToSpeak = pendingSpeech;
                            pendingSpeech = null;
                            speakWord(wordToSpeak);
                        }
                    }
                }
        );
    }

    private void playPronunciation() {
        if (ilokanoWord == null || ilokanoWord.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "No word is available to pronounce.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // Prefer the recorded audio when a URL exists.
        if (!ttsUrl.trim().isEmpty()) {
            playRemoteAudio(ttsUrl);
        } else {
            speakWord(ilokanoWord);
        }
    }

    private void playRemoteAudio(String audioUrl) {
        releaseMediaPlayer();

        try {
            MediaPlayer player = new MediaPlayer();
            mediaPlayer = player;

            player.setOnPreparedListener(preparedPlayer -> {
                if (activityDestroyed || mediaPlayer != preparedPlayer) {
                    preparedPlayer.release();
                    return;
                }

                preparedPlayer.start();
            });

            player.setOnCompletionListener(completedPlayer -> {
                if (mediaPlayer == completedPlayer) {
                    releaseMediaPlayer();
                }
            });

            player.setOnErrorListener((failedPlayer, what, extra) -> {
                if (mediaPlayer == failedPlayer) {
                    releaseMediaPlayer();

                    if (!activityDestroyed) {
                        // Try Text-to-Speech if the recording cannot play.
                        speakWord(ilokanoWord);
                    }
                }

                return true;
            });

            player.setDataSource(audioUrl);
            player.prepareAsync();

        } catch (IOException | IllegalArgumentException
                 | IllegalStateException e) {

            releaseMediaPlayer();

            // Audio URL failed; try the device's speech engine.
            speakWord(ilokanoWord);
        }
    }

    private void speakWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }

        if (tts == null || !ttsReady) {
            pendingSpeech = word;

            if (tts == null) {
                Toast.makeText(
                        this,
                        "Pronunciation is initializing. Please try again.",
                        Toast.LENGTH_SHORT
                ).show();
            }

            return;
        }

        tts.speak(
                word,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "ILOKANO_WORD_PRONUNCIATION"
        );
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException ignored) {
                // The player may already have stopped or failed.
            }

            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Stop pronunciation when this screen is no longer visible.
        releaseMediaPlayer();

        if (tts != null) {
            tts.stop();
        }
    }

    @Override
    protected void onDestroy() {
        activityDestroyed = true;
        pendingSpeech = null;

        releaseMediaPlayer();

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }

        ttsReady = false;

        super.onDestroy();
    }
}
