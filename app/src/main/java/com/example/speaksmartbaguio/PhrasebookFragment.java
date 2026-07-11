package com.example.speaksmartbaguio;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.speaksmartbaguio.databinding.FragmentPhrasebookBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PhrasebookFragment extends Fragment {

    private FragmentPhrasebookBinding binding;
    private PhrasebookAdapter adapter;
    private List<Phrase> fullPhraseList = new ArrayList<>();
    private FirebaseFirestore db;
    private TextToSpeech tts;
    private String selectedLanguage = "English";
    private boolean isDropdownVisible = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPhrasebookBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        setupTextToSpeech();
        setupRecyclerView();
        setupLanguageDropdown();
        fetchPhrases();
        setupSearch();
    }

    private void setupTextToSpeech() {
        tts = new TextToSpeech(getContext(), status -> {
            if (status != TextToSpeech.SUCCESS) {
                Toast.makeText(getContext(), "TTS initialization failed", Toast.LENGTH_SHORT).show();
            } else {
                tts.setPitch(0.95f);
                tts.setSpeechRate(0.85f);
                tts.setLanguage(Locale.getDefault());
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new PhrasebookAdapter(fullPhraseList, phrase -> speakPhrase(phrase), selectedLanguage);
        binding.recyclerViewPhrasebook.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewPhrasebook.setAdapter(adapter);
    }

    private void setupLanguageDropdown() {
        String[] languages = {"English", "Tagalog"};

        ArrayAdapter<String> dropdownAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.dropdown_item_bold,
                languages
        );

        binding.languageDropdown.setAdapter(dropdownAdapter);
        binding.languageDropdown.setDropDownBackgroundResource(R.drawable.dropdown_glass_background);

        // Make read-only but clickable
        binding.languageDropdown.setKeyListener(null);
        binding.languageDropdown.setFocusable(false);
        binding.languageDropdown.setInputType(InputType.TYPE_NULL);

        // Set hint on TextInputLayout
        binding.languageDropdownLayout.setHint("Select Target Language");

        // Initially show empty so hint is visible
        binding.languageDropdown.setText("", false);

        // Handle selection
        binding.languageDropdown.setOnItemClickListener((parent, view, position, id) -> {
            selectedLanguage = parent.getItemAtPosition(position).toString();
            binding.languageDropdown.setText(selectedLanguage, false);

            // Update Phrasebook adapter to show only selected language
            if (adapter != null) {
                adapter.setSelectedLanguage(selectedLanguage);
                adapter.notifyDataSetChanged();
            }

            // Save selection in preferences
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences("PhrasePrefs", Context.MODE_PRIVATE);
            prefs.edit().putString("selectedLanguage", selectedLanguage).apply();

            // Filter phrases according to current search
            filterPhrases(binding.searchBar.getText().toString());
        });
    }


    private void fetchPhrases() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textViewEmpty.setVisibility(View.GONE);

        db.collection("phrasebook")
                .get()
                .addOnCompleteListener(task -> {
                    binding.progressBar.setVisibility(View.GONE);
                    fullPhraseList.clear();

                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Phrase phrase = doc.toObject(Phrase.class);
                            fullPhraseList.add(phrase);
                        }
                        filterPhrases("");
                    } else {
                        Toast.makeText(getContext(), "Failed to load phrasebook.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupSearch() {
        binding.searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPhrases(s.toString());
            }
        });
    }

    private void filterPhrases(String query) {
        List<Phrase> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        for (Phrase phrase : fullPhraseList) {
            String targetText = "Tagalog".equals(selectedLanguage) ? phrase.getTagalogTranslation() : phrase.getEnglishTranslation();
            targetText = targetText != null ? targetText.toLowerCase() : "";
            String ilokano = phrase.getIlokanoWord() != null ? phrase.getIlokanoWord().toLowerCase() : "";

            if (ilokano.contains(lowerQuery) || targetText.contains(lowerQuery)) {
                filtered.add(phrase);
            }
        }

        adapter.setSearchQuery(lowerQuery);
        adapter.filterList(filtered);
        binding.textViewEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void speakPhrase(Phrase phrase) {
        if (tts == null || phrase == null) return;

        String ilokano = phrase.getIlokanoWord() != null ? phrase.getIlokanoWord() : "";
        String targetText;
        Locale targetLocale;

        switch (selectedLanguage) {
            case "Tagalog":
                targetText = phrase.getTagalogTranslation();
                targetLocale = new Locale("fil", "PH");
                break;
            case "English":
            default:
                targetText = phrase.getEnglishTranslation();
                targetLocale = Locale.US;
                break;
        }

        if (!ilokano.isEmpty()) {
            tts.setLanguage(new Locale("fil", "PH"));
            tts.speak(ilokano, TextToSpeech.QUEUE_FLUSH, null, "ttsIlokano");
        }

        if (targetText != null && !targetText.isEmpty()) {
            tts.setLanguage(targetLocale);
            tts.speak(targetText, TextToSpeech.QUEUE_ADD, null, "ttsTarget");
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
