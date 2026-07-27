package com.example.speaksmartbaguio;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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

        tvIlokano = findViewById(R.id.tvIlokano);
        tvPartOfSpeech = findViewById(R.id.tvPartOfSpeech);
        tvCategory = findViewById(R.id.tvCategory);
        tvEnglish = findViewById(R.id.tvEnglish);
        tvTagalog = findViewById(R.id.tvTagalog);
        tvSentence = findViewById(R.id.tvSentence);
        tvEnglishSentence = findViewById(R.id.tvEnglishSentence);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvIlokano.setText(getIntent().getStringExtra("ilokano"));
        tvPartOfSpeech.setText(getIntent().getStringExtra("pos"));
        tvCategory.setText(getIntent().getStringExtra("category"));
        tvEnglish.setText(getIntent().getStringExtra("english"));
        tvTagalog.setText(getIntent().getStringExtra("tagalog"));
        String sentence = getIntent().getStringExtra("sentence");
        tvEnglishSentence.setText(getIntent().getStringExtra("englishSentence"));

        if (sentence == null || sentence.isEmpty()) {
            tvSentence.setText("No example sentence available.");
        } else {
            tvSentence.setText(sentence);
        }
    }
}