package com.example.speaksmartbaguio;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WordDetailActivity extends AppCompatActivity {

    private TextView tvIlokano;
    private TextView tvEnglish;
    private TextView tvTagalog;
    private TextView tvPartOfSpeech;
    private TextView tvCategory;
    private TextView tvDefinition;

    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_detail);

        tvIlokano = findViewById(R.id.tvIlokano);
        tvEnglish = findViewById(R.id.tvEnglish);
        tvTagalog = findViewById(R.id.tvTagalog);
        tvPartOfSpeech = findViewById(R.id.tvPartOfSpeech);
        tvCategory = findViewById(R.id.tvCategory);
        tvDefinition = findViewById(R.id.tvDefinition);

        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Receive data from DictionaryAdapter
        String ilokano = getIntent().getStringExtra("ilokano");
        String english = getIntent().getStringExtra("english");
        String tagalog = getIntent().getStringExtra("tagalog");
        String pos = getIntent().getStringExtra("pos");
        String category = getIntent().getStringExtra("category");
        String definition = getIntent().getStringExtra("definition");

        tvIlokano.setText(ilokano);
        tvEnglish.setText(english);
        tvTagalog.setText(tagalog);
        tvPartOfSpeech.setText(pos);
        tvCategory.setText(category);
        tvDefinition.setText(
                definition == null || definition.isEmpty()
                        ? "No definition available."
                        : definition
        );
    }
}