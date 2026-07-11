package com.example.speaksmartbaguio;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import android.text.InputType;



import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.speaksmartbaguio.databinding.FragmentTranslatorBinding;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Locale;

public class TranslatorFragment extends Fragment {

    private FragmentTranslatorBinding binding;
    private FirebaseFirestore db;
    private TextToSpeech tts;

    private static final int STT_REQUEST_CODE = 100;

    private String translationMode = "English → Ilokano";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTranslatorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        setupTextToSpeech();
        setupDropdown();
        setupListeners();
    }

    private void setupTextToSpeech() {
        tts = new TextToSpeech(getContext(), status -> {
            if (status != TextToSpeech.SUCCESS) {
                Toast.makeText(getContext(), "TTS initialization failed", Toast.LENGTH_SHORT).show();
            } else {
                tts.setPitch(0.95f);
                tts.setSpeechRate(0.85f);
                setTTSLanguage();
            }
        });
    }

    private void setupDropdown() {
        // Dropdown options
        String[] modes = {"English → Ilokano", "Ilokano → English", "Tagalog → Ilokano", "Ilokano → Tagalog"};

        // Custom ArrayAdapter using bold text layout
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.dropdown_item_bold,
                modes
        );

        binding.languageDropdown.setAdapter(adapter);
        binding.languageDropdown.setDropDownBackgroundResource(R.drawable.dropdown_glass_background);

        // Make AutoCompleteTextView non-editable but clickable
        binding.languageDropdown.setInputType(InputType.TYPE_NULL);
        binding.languageDropdown.setFocusable(false);
        binding.languageDropdown.setKeyListener(null);

        // Set hint (shows when no selection is made)
        binding.languageDropdown.setHint("Select Target Language");

        // Handle item selection
        binding.languageDropdown.setOnItemClickListener((parent, view, position, id) -> {
            translationMode = modes[position];
            binding.languageDropdown.setText(translationMode, false);
            binding.titleText.setText(translationMode);

            // Clear previous translation/input
            binding.editTextInput.setText("");
            binding.originalText.setText("");
            binding.translatedText.setText("");

            setTTSLanguage();
        });
    }

    private void setTTSLanguage() {
        Locale locale;

        switch (translationMode) {
            case "Ilokano → English":
                locale = Locale.US;
                break;

            case "English → Ilokano":
            case "Tagalog → Ilokano":
            case "Ilokano → Tagalog":
            default:
                locale = new Locale("fil", "PH");
                break;
        }

        int result = tts.setLanguage(locale);

        if (result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.LANG_MISSING_DATA) {
            Toast.makeText(getContext(), "TTS language not supported", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        binding.editTextInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s.toString().trim();
                binding.originalText.setText(input);
                if (!input.isEmpty()) translateTextWordByWord(input);
                else binding.translatedText.setText("");
            }
        });

        binding.micButton.setOnClickListener(v -> startSpeechToText());
        binding.ttsButton.setOnClickListener(v -> speakTranslation());
    }

    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        // Choose language for speech input
        switch (translationMode) {
            case "Ilokano → English":
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
                break;

            default:
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fil-PH");
                break;
        }


        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        try {
            startActivityForResult(intent, STT_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Speech recognition not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == STT_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) binding.editTextInput.setText(results.get(0));
        }
    }

    private void speakTranslation() {
        String text = binding.translatedText.getText().toString();
        if (!text.isEmpty() && tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ttsTranslation");
        else Toast.makeText(getContext(), "Nothing to speak", Toast.LENGTH_SHORT).show();
    }

    private String capitalizeSentence(String text) {
        if (text == null || text.trim().isEmpty()) return text;
        return text.trim().substring(0, 1).toUpperCase() + text.trim().substring(1);
    }

    private void translateTextWordByWord(String input) {
        String[] words = input.trim().split("\\s+");
        StringBuilder translatedSentence = new StringBuilder();
        final int totalWords = words.length;
        final int[] completed = {0};

        String fieldSearch, fieldResult;

        switch (translationMode) {
            case "English → Ilokano":
                fieldSearch = "english";
                fieldResult = "ilokano";
                break;
            case "Ilokano → English":
                fieldSearch = "ilokano";
                fieldResult = "english";
                break;
            case "Tagalog → Ilokano":
                fieldSearch = "tagalog";
                fieldResult = "ilokano";
                break;
            case "Ilokano → Tagalog":
                fieldSearch = "ilokano";
                fieldResult = "tagalog";
                break;
            default:
                fieldSearch = "english";
                fieldResult = "ilokano";
        }

        for (String word : words) {
            String cleanedWord = word.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

            if (cleanedWord.isEmpty()) {
                translatedSentence.append(word).append(" ");
                completed[0]++;
                continue;
            }

            db.collection("translations")
                    .whereEqualTo(fieldSearch, cleanedWord)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            String translated = querySnapshot.getDocuments().get(0).getString(fieldResult);
                            translatedSentence.append(translated).append(" ");
                        } else {
                            translatedSentence.append(word).append(" ");
                        }
                        completed[0]++;
                        if (completed[0] == totalWords) {
                            binding.translatedText.setText(capitalizeSentence(translatedSentence.toString().trim()));
                        }
                    })
                    .addOnFailureListener(e -> {
                        translatedSentence.append(word).append(" ");
                        completed[0]++;
                        if (completed[0] == totalWords) {
                            binding.translatedText.setText(capitalizeSentence(translatedSentence.toString().trim()));
                        }
                        e.printStackTrace();
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        binding = null;
    }
}
