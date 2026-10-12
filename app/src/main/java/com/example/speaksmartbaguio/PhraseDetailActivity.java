package com.example.speaksmartbaguio;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.speaksmartbaguio.utils.AccessibilityPreferences;

public class PhraseDetailActivity extends AppCompatActivity {

    private TextView tvIlokano;
    private TextView tvPartOfSpeech;
    private TextView tvCategory;
    private TextView tvEnglish;
    private TextView tvTagalog;
    private TextView tvSentence;
    private TextView tvEnglishSentence;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phrase_detail);

        // Connect the layout views.
        tvIlokano = findViewById(R.id.tvIlokano);
        tvPartOfSpeech = findViewById(R.id.tvPartOfSpeech);
        tvCategory = findViewById(R.id.tvCategory);
        tvEnglish = findViewById(R.id.tvEnglish);
        tvTagalog = findViewById(R.id.tvTagalog);
        tvSentence = findViewById(R.id.tvSentence);
        tvEnglishSentence = findViewById(R.id.tvEnglishSentence);

        // Back button.
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Retrieve phrase information from the Phrasebook card.
        String ilokano = getIntent().getStringExtra("ilokano");
        String partOfSpeech = getIntent().getStringExtra("pos");
        String category = getIntent().getStringExtra("category");
        String english = getIntent().getStringExtra("english");
        String tagalog = getIntent().getStringExtra("tagalog");
        String sentence = getIntent().getStringExtra("sentence");
        String englishSentence =
                getIntent().getStringExtra("englishSentence");

        // Main Ilokano phrase.
        tvIlokano.setText(
                displayOrFallback(ilokano, "Phrase unavailable.")
        );

        // Optional part of speech.
        if (isEmpty(partOfSpeech)) {
            tvPartOfSpeech.setVisibility(View.GONE);
        } else {
            tvPartOfSpeech.setVisibility(View.VISIBLE);
            tvPartOfSpeech.setText(partOfSpeech.trim());
        }

        // Optional category.
        if (isEmpty(category)) {
            tvCategory.setVisibility(View.GONE);
        } else {
            tvCategory.setVisibility(View.VISIBLE);
            tvCategory.setText(category.trim());
        }

        // English translation.
        tvEnglish.setText(
                displayOrFallback(
                        english,
                        "No English translation available."
                )
        );

        // Tagalog translation.
        tvTagalog.setText(
                displayOrFallback(
                        tagalog,
                        "Walang available na salin sa Tagalog."
                )
        );

        // Ilokano example sentence.
        tvSentence.setText(
                displayOrFallback(
                        sentence,
                        "No Ilokano example sentence available."
                )
        );

        // English translation of the example sentence.
        tvEnglishSentence.setText(
                displayOrFallback(
                        englishSentence,
                        "No English example translation available."
                )
        );

        // Apply saved font-size, high-contrast, and simplified-layout settings.
        AccessibilityPreferences.applyToViewTree(
                this,
                getWindow().getDecorView()
        );
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String displayOrFallback(
            String value,
            String fallback
    ) {
        return isEmpty(value) ? fallback : value.trim();
    }
}
