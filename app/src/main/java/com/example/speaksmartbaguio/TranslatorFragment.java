package com.example.speaksmartbaguio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.widget.Filter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.speaksmartbaguio.databinding.FragmentTranslatorBinding;
import com.example.speaksmartbaguio.mapper.EntityMapper;
import com.example.speaksmartbaguio.repository.DictionaryRepository;
import com.example.speaksmartbaguio.utils.AccessibilityPreferences;
import com.example.speaksmartbaguio.utils.NetworkUtil;
import com.example.speaksmartbaguio.utils.SettingsMenuHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TranslatorFragment extends Fragment {

    private FragmentTranslatorBinding binding;
    private ApiService apiService;
    private DictionaryRepository repository;
    private TextToSpeech tts;

    private final Handler searchHandler =
            new Handler(Looper.getMainLooper());

    private Runnable searchRunnable;

    private String latestInput = "";

    private static final int STT_REQUEST_CODE = 100;

    private String translationMode = "English → Ilokano";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentTranslatorBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        apiService = ApiService.getInstance();

        repository = new DictionaryRepository(requireContext());

        setupTextToSpeech();
        setupDropdown();
        setupListeners();
        setupSettingsMenuButton();

        // Apply the user's saved accessibility preferences.
        AccessibilityPreferences.applyToViewTree(
                requireContext(),
                view
        );
    }

    // ---------------------------------------------------------
    // SHARED SETTINGS MENU
    // ---------------------------------------------------------

    private void setupSettingsMenuButton() {
        binding.buttonSettingsMenu.setOnClickListener(
                v -> SettingsMenuHelper.show(v)
        );
    }

    // ---------------------------------------------------------
    // TEXT-TO-SPEECH
    // ---------------------------------------------------------

    private void setupTextToSpeech() {

        tts = new TextToSpeech(
                requireContext(),
                status -> {

                    if (status != TextToSpeech.SUCCESS) {

                        if (getContext() != null) {
                            Toast.makeText(
                                    requireContext(),
                                    "TTS initialization failed",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                    } else if (tts != null) {

                        tts.setPitch(0.95f);
                        tts.setSpeechRate(0.85f);

                        setTTSLanguage();
                    }
                }
        );
    }


// ---------------------------------------------------------
// TRANSLATION MODE DROPDOWN
// Keeps all modes visible and toggles open/close on tap.
// ---------------------------------------------------------

    private void setupDropdown() {

        final String[] modes = {
                "English → Ilokano",
                "Ilokano → English",
                "Tagalog → Ilokano",
                "Ilokano → Tagalog"
        };

        SharedPreferences prefs =
                requireContext().getSharedPreferences(
                        "TranslatorPrefs",
                        Context.MODE_PRIVATE
                );

        // Restore the last selected translation mode.
        translationMode = prefs.getString(
                "translationMode",
                "English → Ilokano"
        );

        // Validate the saved mode.
        boolean validMode = false;

        for (String mode : modes) {
            if (mode.equals(translationMode)) {
                validMode = true;
                break;
            }
        }

        if (!validMode) {
            translationMode = "English → Ilokano";
        }

        // Create an adapter that always returns all four modes.
        ArrayAdapter<String> dropdownAdapter =
                new ArrayAdapter<String>(
                        requireContext(),
                        R.layout.dropdown_item_bold,
                        new ArrayList<>(
                                java.util.Arrays.asList(modes)
                        )
                ) {

                    private final Filter showAllModesFilter =
                            new Filter() {

                                @Override
                                protected FilterResults performFiltering(
                                        CharSequence constraint
                                ) {
                                    FilterResults results =
                                            new FilterResults();

                                    ArrayList<String> allModes =
                                            new ArrayList<>(
                                                    java.util.Arrays.asList(modes)
                                            );

                                    results.values = allModes;
                                    results.count = allModes.size();

                                    return results;
                                }

                                @Override
                                protected void publishResults(
                                        CharSequence constraint,
                                        FilterResults results
                                ) {
                                    if (results == null
                                            || results.values == null) {
                                        return;
                                    }

                                    @SuppressWarnings("unchecked")
                                    List<String> allModes =
                                            (List<String>) results.values;

                                    clear();
                                    addAll(allModes);
                                    notifyDataSetChanged();
                                }
                            };

                    @NonNull
                    @Override
                    public Filter getFilter() {
                        return showAllModesFilter;
                    }
                };

        // Attach the adapter and preserve the existing appearance.
        binding.languageDropdown.setAdapter(dropdownAdapter);

        binding.languageDropdown.setDropDownBackgroundResource(
                R.drawable.dropdown_glass_background
        );

        // Prevent manual typing.
        binding.languageDropdown.setKeyListener(null);
        binding.languageDropdown.setFocusable(false);
        binding.languageDropdown.setInputType(
                InputType.TYPE_NULL
        );
        binding.languageDropdown.setThreshold(0);

        binding.languageDropdown.setHint(
                "Select Translation Mode"
        );

        // Restore the saved mode without filtering the options.
        binding.languageDropdown.setText(
                translationMode,
                false
        );

        binding.titleText.setText(translationMode);

        // Track whether the dropdown is open.
        final boolean[] isDropdownOpen = {false};

        // Toggle the dropdown when the field or arrow is tapped.
        View.OnClickListener toggleDropdown = v -> {

            if (binding == null) {
                return;
            }

            // Close the dropdown if it is already open.
            if (isDropdownOpen[0]) {
                binding.languageDropdown.dismissDropDown();
                isDropdownOpen[0] = false;
                return;
            }

            // Restore every translation mode before opening.
            dropdownAdapter.getFilter().filter(
                    "",
                    count -> {

                        if (binding == null) {
                            return;
                        }

                        binding.languageDropdown.setAdapter(
                                dropdownAdapter
                        );

                        // Keep the current selection unchanged.
                        binding.languageDropdown.setText(
                                translationMode,
                                false
                        );

                        binding.languageDropdown.showDropDown();
                        isDropdownOpen[0] = true;
                    }
            );
        };

        // Tapping the field toggles the dropdown.
        binding.languageDropdown.setOnClickListener(
                toggleDropdown
        );

        // Tapping the Material dropdown arrow does the same.
        binding.languageDropdownLayout.setEndIconOnClickListener(
                toggleDropdown
        );

        // Save and apply the selected translation mode.
        binding.languageDropdown.setOnItemClickListener(
                (parent, selectedView, position, id) -> {

                    Object selectedItem =
                            parent.getItemAtPosition(position);

                    if (selectedItem == null) {
                        return;
                    }

                    translationMode = selectedItem.toString();

                    // Save the selected mode.
                    prefs.edit()
                            .putString(
                                    "translationMode",
                                    translationMode
                            )
                            .apply();

                    // Update the displayed selection.
                    binding.languageDropdown.setText(
                            translationMode,
                            false
                    );

                    binding.titleText.setText(
                            translationMode
                    );

                    // The selection closes the dropdown.
                    isDropdownOpen[0] = false;

                    // Clear results from the previous mode.
                    binding.editTextInput.setText("");
                    binding.originalText.setText("");
                    binding.translatedText.setText("");

                    latestInput = "";

                    searchHandler.removeCallbacksAndMessages(null);

                    // Apply the correct speech language.
                    setTTSLanguage();
                }
        );

        // Apply the correct speech language for the restored mode.
        setTTSLanguage();
    }



    // ---------------------------------------------------------
    // TTS LANGUAGE
    // ---------------------------------------------------------

    private void setTTSLanguage() {

        if (tts == null) {
            return;
        }

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

        if (result == TextToSpeech.LANG_NOT_SUPPORTED
                || result == TextToSpeech.LANG_MISSING_DATA) {

            if (getContext() != null) {
                Toast.makeText(
                        requireContext(),
                        "TTS language not supported",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    // ---------------------------------------------------------
    // TRANSLATOR LISTENERS
    // ---------------------------------------------------------

    private void setupListeners() {

        binding.editTextInput.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                        // No action needed.
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        String input = s.toString().trim();

                        binding.originalText.setText(input);

                        latestInput = input;

                        searchHandler.removeCallbacksAndMessages(null);

                        if (input.isEmpty()) {
                            binding.translatedText.setText("");
                            return;
                        }

                        searchRunnable =
                                () -> translateTextWordByWord(input);

                        searchHandler.postDelayed(
                                searchRunnable,
                                400
                        );
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        // No action needed.
                    }
                }
        );

        binding.micButton.setOnClickListener(
                v -> startSpeechToText()
        );

        binding.ttsButton.setOnClickListener(
                v -> speakTranslation()
        );
    }

    // ---------------------------------------------------------
    // SPEECH-TO-TEXT
    // ---------------------------------------------------------

    private void startSpeechToText() {

        Intent intent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        switch (translationMode) {

            case "Ilokano → English":
                intent.putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE,
                        "en-US"
                );
                break;

            default:
                intent.putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE,
                        "fil-PH"
                );
                break;
        }

        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Speak now..."
        );

        try {

            startActivityForResult(
                    intent,
                    STT_REQUEST_CODE
            );

        } catch (Exception e) {

            Toast.makeText(
                    requireContext(),
                    "Speech recognition not supported",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == STT_REQUEST_CODE
                && resultCode == Activity.RESULT_OK
                && data != null
                && binding != null) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (results != null && !results.isEmpty()) {
                binding.editTextInput.setText(results.get(0));
            }
        }
    }

    // ---------------------------------------------------------
    // PLAY TRANSLATED TEXT
    // ---------------------------------------------------------

    private void speakTranslation() {

        if (binding == null) {
            return;
        }

        String text = binding.translatedText
                .getText()
                .toString();

        if (!text.isEmpty() && tts != null) {

            tts.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "ttsTranslation"
            );

        } else {

            Toast.makeText(
                    requireContext(),
                    "Nothing to speak",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ---------------------------------------------------------
    // SENTENCE CAPITALIZATION
    // ---------------------------------------------------------

    private String capitalizeSentence(String text) {

        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String trimmed = text.trim();

        return trimmed.substring(0, 1).toUpperCase()
                + trimmed.substring(1);
    }

    // ---------------------------------------------------------
    // ONLINE WORD-BY-WORD TRANSLATION
    // ---------------------------------------------------------

    private void translateTextWordByWord(String input) {

        if (!isAdded() || binding == null) {
            return;
        }

        if (!NetworkUtil.isOnline(requireContext())) {
            translateOffline(input);
            return;
        }

        String[] words = input.trim().split("\\s+");

        StringBuilder translatedSentence = new StringBuilder();

        final int totalWords = words.length;
        final int[] completed = {0};

        for (String word : words) {

            String cleanedWord = word
                    .replaceAll("[^a-zA-Z0-9]", "")
                    .toLowerCase(Locale.ROOT);

            if (cleanedWord.isEmpty()) {

                translatedSentence.append(word).append(" ");
                completed[0]++;

                if (completed[0] == totalWords) {
                    showTranslation(
                            capitalizeSentence(
                                    translatedSentence.toString().trim()
                            )
                    );
                }

                continue;
            }

            String fieldSearch;

            Log.d("TYPED WORD", "WORD " + cleanedWord);
            Log.d("WORD GET", "WORD " + word);

            switch (translationMode) {

                case "English → Ilokano":
                    fieldSearch = "englishTranslation";
                    break;

                case "Ilokano → English":
                    fieldSearch = "ilokanoWord";
                    break;

                case "Tagalog → Ilokano":
                    fieldSearch = "tagalogTranslation";
                    break;

                case "Ilokano → Tagalog":
                    fieldSearch = "ilokanoWord";
                    break;

                default:
                    fieldSearch = "englishTranslation";
                    break;
            }

            apiService.searchDictionaryWord(
                    fieldSearch,
                    cleanedWord,
                    new ApiService.ApiCallback<Word>() {

                        @Override
                        public void onSuccess(
                                List<Word> results,
                                boolean hasMore
                        ) {

                            if (binding == null
                                    || !isAdded()
                                    || !latestInput.equalsIgnoreCase(input)) {
                                return;
                            }

                            String translated = word;

                            if (results != null && !results.isEmpty()) {

                                Word item = results.get(0);

                                switch (translationMode) {

                                    case "English → Ilokano":
                                        translated = item.getIlokanoWord();
                                        break;

                                    case "Ilokano → English":
                                        translated = item.getEnglishTranslation();
                                        break;

                                    case "Tagalog → Ilokano":
                                        translated = item.getIlokanoWord();
                                        break;

                                    case "Ilokano → Tagalog":
                                        translated = item.getTagalogTranslation();
                                        break;
                                }
                            }

                            translatedSentence.append(translated).append(" ");
                            completed[0]++;

                            if (completed[0] == totalWords) {

                                showTranslation(
                                        capitalizeSentence(
                                                translatedSentence.toString().trim()
                                        )
                                );
                            }
                        }

                        @Override
                        public void onError(String error) {

                            if (binding == null
                                    || !isAdded()
                                    || !latestInput.equalsIgnoreCase(input)) {
                                return;
                            }

                            translatedSentence.append(word).append(" ");
                            completed[0]++;

                            if (completed[0] == totalWords) {

                                showTranslation(
                                        capitalizeSentence(
                                                translatedSentence.toString().trim()
                                        )
                                );
                            }
                        }
                    }
            );
        }
    }

    // ---------------------------------------------------------
    // OFFLINE TRANSLATION
    // ---------------------------------------------------------

    private void translateOffline(String input) {

        if (!isAdded() || binding == null) {
            return;
        }

        String[] words = input.trim().split("\\s+");

        repository.getAllWords(entities -> {

            if (!isAdded() || binding == null) {
                return;
            }

            List<Word> dictionary =
                    EntityMapper.toWordList(entities);

            StringBuilder translated = new StringBuilder();

            for (String word : words) {

                String clean = word
                        .replaceAll("[^a-zA-Z0-9]", "")
                        .toLowerCase(Locale.ROOT);

                String output = word;

                for (Word item : dictionary) {

                    if (item == null) {
                        continue;
                    }

                    switch (translationMode) {

                        case "English → Ilokano":

                            if (item.getEnglishTranslation() != null
                                    && item.getEnglishTranslation()
                                    .equalsIgnoreCase(clean)) {

                                output = item.getIlokanoWord();
                            }

                            break;

                        case "Ilokano → English":

                            if (item.getIlokanoWord() != null
                                    && item.getIlokanoWord()
                                    .equalsIgnoreCase(clean)) {

                                output = item.getEnglishTranslation();
                            }

                            break;

                        case "Tagalog → Ilokano":

                            if (item.getTagalogTranslation() != null
                                    && item.getTagalogTranslation()
                                    .equalsIgnoreCase(clean)) {

                                output = item.getIlokanoWord();
                            }

                            break;

                        case "Ilokano → Tagalog":

                            if (item.getIlokanoWord() != null
                                    && item.getIlokanoWord()
                                    .equalsIgnoreCase(clean)) {

                                output = item.getTagalogTranslation();
                            }

                            break;
                    }
                }

                translated.append(output).append(" ");
            }

            String finalTranslation = capitalizeSentence(
                    translated.toString().trim()
            );

            if (isAdded()) {
                requireActivity().runOnUiThread(
                        () -> showTranslation(finalTranslation)
                );
            }
        });
    }

    // ---------------------------------------------------------
    // SAFELY DISPLAY TRANSLATION RESULTS
    // ---------------------------------------------------------

    private void showTranslation(String translation) {

        if (binding == null || !isAdded()) {
            return;
        }

        binding.translatedText.setText(translation);
    }

    // ---------------------------------------------------------
    // CLEANUP
    // ---------------------------------------------------------

    @Override
    public void onDestroyView() {

        searchHandler.removeCallbacksAndMessages(null);

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }

        binding = null;

        super.onDestroyView();
    }
}
