package com.example.speaksmartbaguio;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.speaksmartbaguio.databinding.FragmentPhraseListBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PhraseListFragment extends Fragment {

    private FragmentPhraseListBinding binding;
    private PhraseAdapter adapter;
    private List<Phrase> phraseList = new ArrayList<>();
    private FirebaseFirestore db;
    private TextToSpeech tts;
    private String selectedLanguage = "English";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPhraseListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = FirebaseFirestore.getInstance();

        setupTextToSpeech();
        setupLanguageDropdown();
        setupRecyclerView();
        fetchPhrasesFromFirestore();

        return root;
    }

    private void setupTextToSpeech() {
        tts = new TextToSpeech(getContext(), status -> {
            if (status != TextToSpeech.SUCCESS) {
                Toast.makeText(getContext(), "TTS initialization failed", Toast.LENGTH_SHORT).show();
            } else {
                tts.setLanguage(Locale.getDefault());
            }
        });
    }

    private void setupLanguageDropdown() {
        String[] languages = {"English", "Tagalog"};
        ArrayAdapter<String> adapterDropdown = new ArrayAdapter<>(
                requireContext(),
                R.layout.dropdown_item_bold,
                languages
        );

        binding.languageDropdown.setAdapter(adapterDropdown);
        binding.languageDropdown.setDropDownBackgroundResource(R.drawable.dropdown_glass_background);
        binding.languageDropdown.setKeyListener(null);
        binding.languageDropdown.setFocusable(false);
        binding.languageDropdown.setInputType(InputType.TYPE_NULL);
        binding.languageDropdown.setText(selectedLanguage, false);

        binding.languageDropdown.setOnItemClickListener((parent, view, position, id) -> {
            selectedLanguage = parent.getItemAtPosition(position).toString();
            binding.languageDropdown.setText(selectedLanguage, false);

            // Update adapter language
            if (adapter != null) {
                adapter.setSelectedLanguage(selectedLanguage);
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new PhraseAdapter(phraseList, phrase -> {
            if (tts != null && phrase != null) {
                String ilokano = phrase.getIlokanoWord() != null ? phrase.getIlokanoWord() : "";
                String targetText = "Tagalog".equals(selectedLanguage)
                        ? phrase.getTagalogTranslation()
                        : phrase.getEnglishTranslation();
                targetText = targetText != null ? targetText : "";

                // Speak Ilokano first
                if (!ilokano.isEmpty()) {
                    tts.setLanguage(new Locale("fil", "PH"));
                    tts.speak(ilokano, TextToSpeech.QUEUE_FLUSH, null, "ttsIlokano");
                }

                // Then speak selected language
                if (!targetText.isEmpty()) {
                    Locale locale = "Tagalog".equals(selectedLanguage)
                            ? new Locale("fil", "PH")
                            : Locale.US;
                    tts.setLanguage(locale);
                    tts.speak(targetText, TextToSpeech.QUEUE_ADD, null, "ttsTarget");
                }
            }
        }, selectedLanguage);

        binding.recyclerViewPhrases.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewPhrases.setAdapter(adapter);
    }

    private void fetchPhrasesFromFirestore() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textViewEmpty.setVisibility(View.GONE);

        db.collection("phrasebook")
                .get()
                .addOnCompleteListener(task -> {
                    binding.progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        phraseList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Phrase phrase = document.toObject(Phrase.class);
                            phraseList.add(phrase);
                        }

                        adapter.notifyDataSetChanged();
                        binding.textViewEmpty.setVisibility(phraseList.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        Toast.makeText(getContext(), "Failed to load phrases.", Toast.LENGTH_SHORT).show();
                        binding.textViewEmpty.setVisibility(View.VISIBLE);
                    }
                });
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
