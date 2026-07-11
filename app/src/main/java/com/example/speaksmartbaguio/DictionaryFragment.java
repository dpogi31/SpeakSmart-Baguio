package com.example.speaksmartbaguio;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.speaksmartbaguio.databinding.FragmentDictionaryBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DictionaryFragment extends Fragment {

    private FragmentDictionaryBinding binding;
    private DictionaryAdapter dictionaryAdapter;
    private List<Word> fullWordList = new ArrayList<>();
    private FirebaseFirestore db;
    private TextToSpeech tts;
    private String selectedLanguage = "English";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDictionaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        setupTextToSpeech();
        setupRecyclerView();
        setupLanguageDropdown();
        fetchWords();
        setupSearch();
    }

    private void setupTextToSpeech() {
        tts = new TextToSpeech(getContext(), status -> {
            if (status != TextToSpeech.SUCCESS) {
                Toast.makeText(getContext(), "TTS initialization failed", Toast.LENGTH_SHORT).show();
            } else {
                tts.setPitch(0.95f);
                tts.setSpeechRate(0.85f);
                tts.setLanguage(new Locale("fil", "PH"));
            }
        });
    }

    private void setupRecyclerView() {
        dictionaryAdapter = new DictionaryAdapter(new ArrayList<>(), this::speakWord, selectedLanguage);
        binding.recyclerViewDictionary.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewDictionary.setAdapter(dictionaryAdapter);
    }

    private void setupLanguageDropdown() {
        String[] languages = {"English", "Tagalog"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.dropdown_item_bold,
                languages
        );

        binding.languageDropdown.setAdapter(adapter);
        binding.languageDropdown.setDropDownBackgroundResource(R.drawable.dropdown_glass_background);
        binding.languageDropdown.setKeyListener(null);
        binding.languageDropdown.setFocusable(false);
        binding.languageDropdown.setInputType(InputType.TYPE_NULL);
        binding.languageDropdownLayout.setHint("Select Target Language");

        binding.languageDropdown.setOnItemClickListener((parent, view, position, id) -> {
            selectedLanguage = parent.getItemAtPosition(position).toString();
            binding.languageDropdown.setText(selectedLanguage, false);

            // Update adapter with selected language dynamically
            if (dictionaryAdapter != null) {
                dictionaryAdapter.setSelectedLanguage(selectedLanguage);
            }
        });

        binding.languageDropdown.setText("", false);
    }

    private void fetchWords() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textViewEmpty.setVisibility(View.GONE);

        db.collection("dictionary")
                .get()
                .addOnCompleteListener(task -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        fullWordList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Word word = doc.toObject(Word.class);
                            if (word.getIlokanoWord() == null) word.setIlokanoWord("");
                            if (word.getEnglishTranslation() == null) word.setEnglishTranslation("");
                            if (word.getTagalogTranslation() == null) word.setTagalogTranslation("");
                            if (word.getPartOfSpeech() == null) word.setPartOfSpeech("");
                            fullWordList.add(word);
                        }

                        dictionaryAdapter.setSearchQuery("");
                        dictionaryAdapter.filterList(fullWordList);
                        binding.textViewEmpty.setVisibility(fullWordList.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        Toast.makeText(getContext(), "Failed to load dictionary.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupSearch() {
        binding.searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterWords(s.toString());
            }
        });
    }

    private void filterWords(String query) {
        List<Word> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        for (Word word : fullWordList) {
            String ilokano = word.getIlokanoWord().toLowerCase();
            String english = word.getEnglishTranslation().toLowerCase();
            String tagalog = word.getTagalogTranslation() != null ? word.getTagalogTranslation().toLowerCase() : "";

            if (ilokano.contains(lowerQuery) || english.contains(lowerQuery) || tagalog.contains(lowerQuery)) {
                filtered.add(word);
            }
        }

        binding.textViewEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        dictionaryAdapter.setSearchQuery(lowerQuery);
        dictionaryAdapter.filterList(filtered);
    }

    private void speakWord(Word word) {
        if (word == null || tts == null) return;

        tts.stop();

        String ilokano = word.getIlokanoWord() != null ? word.getIlokanoWord() : "";
        String english = word.getEnglishTranslation() != null ? word.getEnglishTranslation() : "";
        String tagalog = word.getTagalogTranslation() != null ? word.getTagalogTranslation() : "";

        if (!ilokano.isEmpty()) {
            tts.setLanguage(new Locale("fil", "PH"));
            tts.speak(ilokano, TextToSpeech.QUEUE_FLUSH, null, "ttsIlokano");
        }

        if (selectedLanguage.equals("English") && !english.isEmpty()) {
            tts.setLanguage(Locale.US);
            tts.speak(english, TextToSpeech.QUEUE_ADD, null, "ttsEnglish");
        } else if (selectedLanguage.equals("Tagalog") && !tagalog.isEmpty()) {
            tts.setLanguage(new Locale("fil", "PH"));
            tts.speak(tagalog, TextToSpeech.QUEUE_ADD, null, "ttsTagalog");
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
