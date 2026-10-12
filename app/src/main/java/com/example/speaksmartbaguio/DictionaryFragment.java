package com.example.speaksmartbaguio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.speaksmartbaguio.databinding.FragmentDictionaryBinding;
import com.example.speaksmartbaguio.mapper.EntityMapper;
import com.example.speaksmartbaguio.repository.DictionaryRepository;
import com.example.speaksmartbaguio.utils.AccessibilityPreferences;
import com.example.speaksmartbaguio.utils.NetworkUtil;
import com.example.speaksmartbaguio.utils.SettingsMenuHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DictionaryFragment extends Fragment {

    private FragmentDictionaryBinding binding;
    private DictionaryAdapter dictionaryAdapter;
    private ApiService apiService;
    private TextToSpeech tts;
    private MediaPlayer mediaPlayer;

    private String selectedLanguage = "English";
    private DictionaryRepository repository;

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;

    private static final int PAGE_SIZE = 100;

    private final Handler searchHandler =
            new Handler(Looper.getMainLooper());

    private String pendingSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentDictionaryBinding.inflate(
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

        setupSettingsMenuButton();
        setupTextToSpeech();
        setupRecyclerView();
        setupLanguageDropdown();
        setupSearch();

        // Apply the user's saved accessibility preferences.
        AccessibilityPreferences.applyToViewTree(
                requireContext(),
                view
        );

        loadOfflineWords();

        if (NetworkUtil.isOnline(requireContext())) {
            loadWords(true);
        }
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
                        if (isAdded()) {
                            Toast.makeText(
                                    requireContext(),
                                    "TTS initialization failed",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    } else if (tts != null) {
                        tts.setPitch(0.95f);
                        tts.setSpeechRate(0.85f);
                        tts.setLanguage(new Locale("fil", "PH"));
                    }
                }
        );
    }

    // ---------------------------------------------------------
    // RECYCLER VIEW AND PAGINATION
    // ---------------------------------------------------------

    private void setupRecyclerView() {
        dictionaryAdapter = new DictionaryAdapter(
                new ArrayList<>(),
                this::speakWord,
                selectedLanguage
        );

        dictionaryAdapter.setOnLoadMoreListener(
                () -> loadWords(false)
        );

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(requireContext());

        binding.recyclerViewDictionary.setLayoutManager(
                layoutManager
        );

        binding.recyclerViewDictionary.setAdapter(
                dictionaryAdapter
        );

        binding.recyclerViewDictionary.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(
                            @NonNull RecyclerView recyclerView,
                            int dx,
                            int dy
                    ) {
                        if (dy <= 0 || isLoading || !hasMore) {
                            return;
                        }

                        int totalCount =
                                dictionaryAdapter.getItemCount();

                        int lastVisible =
                                layoutManager.findLastVisibleItemPosition();

                        if (lastVisible >= totalCount - 3) {
                            loadWords(false);
                        }
                    }
                }
        );
    }


// ---------------------------------------------------------
// LANGUAGE DROPDOWN
// Always show all languages and toggle open/close on tap.
// ---------------------------------------------------------

    private void setupLanguageDropdown() {

        final String[] languages = {
                "English",
                "Tagalog"
        };

        SharedPreferences prefs =
                requireContext().getSharedPreferences(
                        "DictionaryPrefs",
                        Context.MODE_PRIVATE
                );

        // Restore the saved language selection.
        selectedLanguage = prefs.getString(
                "selectedLanguage",
                "English"
        );

        // Validate the saved language.
        if (!"English".equals(selectedLanguage)
                && !"Tagalog".equals(selectedLanguage)) {
            selectedLanguage = "English";
        }

        // Create an adapter that always returns both languages.
        ArrayAdapter<String> languageAdapter =
                new ArrayAdapter<String>(
                        requireContext(),
                        R.layout.dropdown_item_bold,
                        new ArrayList<>(
                                java.util.Arrays.asList(languages)
                        )
                ) {

                    private final Filter showAllLanguagesFilter =
                            new Filter() {

                                @Override
                                protected FilterResults performFiltering(
                                        CharSequence constraint
                                ) {
                                    FilterResults results =
                                            new FilterResults();

                                    ArrayList<String> allLanguages =
                                            new ArrayList<>(
                                                    java.util.Arrays.asList(languages)
                                            );

                                    results.values = allLanguages;
                                    results.count = allLanguages.size();

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
                                    List<String> allLanguages =
                                            (List<String>) results.values;

                                    clear();
                                    addAll(allLanguages);
                                    notifyDataSetChanged();
                                }
                            };

                    @NonNull
                    @Override
                    public Filter getFilter() {
                        return showAllLanguagesFilter;
                    }
                };

        // Attach the adapter and preserve the dropdown appearance.
        binding.languageDropdown.setAdapter(languageAdapter);

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

        binding.languageDropdownLayout.setHint(
                "Select Target Language"
        );

        // Restore the selected language without filtering the list.
        binding.languageDropdown.setText(
                selectedLanguage,
                false
        );

        // Update the dictionary adapter.
        if (dictionaryAdapter != null) {
            dictionaryAdapter.setSelectedLanguage(selectedLanguage);
        }

        // Track whether the dropdown is currently open.
        final boolean[] isDropdownOpen = {false};

        // Toggle the dropdown when the field is tapped.
        View.OnClickListener toggleDropdown = v -> {

            if (binding == null) {
                return;
            }

            // If already open, close it without changing selection.
            if (isDropdownOpen[0]) {
                binding.languageDropdown.dismissDropDown();
                isDropdownOpen[0] = false;
                return;
            }

            // Restore all options before opening the dropdown.
            languageAdapter.getFilter().filter("", count -> {

                if (binding == null) {
                    return;
                }

                binding.languageDropdown.setAdapter(languageAdapter);

                // Preserve the current selection.
                binding.languageDropdown.setText(
                        selectedLanguage,
                        false
                );

                binding.languageDropdown.showDropDown();
                isDropdownOpen[0] = true;
            });
        };

        // Tapping the field opens or closes the dropdown.
        binding.languageDropdown.setOnClickListener(toggleDropdown);

        // Tapping the dropdown arrow does the same.
        binding.languageDropdownLayout.setEndIconOnClickListener(
                toggleDropdown
        );

        // Handle language selection.
        binding.languageDropdown.setOnItemClickListener(
                (parent, itemView, position, id) -> {

                    Object item = parent.getItemAtPosition(position);

                    if (item == null) {
                        return;
                    }

                    selectedLanguage = item.toString();

                    // Save the selected language.
                    prefs.edit()
                            .putString(
                                    "selectedLanguage",
                                    selectedLanguage
                            )
                            .apply();

                    // Display the selected language.
                    binding.languageDropdown.setText(
                            selectedLanguage,
                            false
                    );

                    // The selection closes the dropdown.
                    isDropdownOpen[0] = false;

                    // Update the dictionary adapter.
                    if (dictionaryAdapter != null) {
                        dictionaryAdapter.setSelectedLanguage(
                                selectedLanguage
                        );
                    }
                }
        );
    }




    // ---------------------------------------------------------
    // SEARCH
    // ---------------------------------------------------------

    private void setupSearch() {
        binding.searchBar.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                        // Not needed.
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {
                        searchHandler.removeCallbacksAndMessages(null);

                        pendingSearchQuery = s.toString().trim();

                        searchHandler.postDelayed(
                                () -> {
                                    if (isAdded() && binding != null) {
                                        loadWords(true);
                                    }
                                },
                                300
                        );
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        // Not needed.
                    }
                }
        );
    }

    // ---------------------------------------------------------
    // LOAD ONLINE WORDS
    // ---------------------------------------------------------

    private void loadWords(boolean reset) {
        if (!isAdded() || binding == null) {
            return;
        }

        if (!NetworkUtil.isOnline(requireContext())) {
            if (!pendingSearchQuery.isEmpty()) {
                searchOffline(pendingSearchQuery);
            } else {
                loadOfflineWords();
            }

            return;
        }

        if (isLoading) {
            return;
        }

        if (!reset && !hasMore) {
            return;
        }

        isLoading = true;

        if (reset) {
            currentPage = 1;
            hasMore = true;

            binding.progressBar.setVisibility(View.VISIBLE);

            dictionaryAdapter.setPaginationState(
                    false,
                    false
            );
        } else {
            dictionaryAdapter.setPaginationState(
                    true,
                    true
            );
        }

        apiService.getDictionary(
                currentPage,
                PAGE_SIZE,
                pendingSearchQuery,
                new ApiService.ApiCallback<Word>() {
                    @Override
                    public void onSuccess(
                            List<Word> items,
                            boolean more
                    ) {
                        isLoading = false;

                        if (!isAdded() || binding == null) {
                            return;
                        }

                        binding.progressBar.setVisibility(View.GONE);

                        binding.textViewEmpty.setVisibility(
                                items.isEmpty() && reset
                                        ? View.VISIBLE
                                        : View.GONE
                        );

                        if (reset) {
                            dictionaryAdapter.filterList(
                                    new ArrayList<>(items)
                            );

                            // Cache results for offline use.
                            repository.insertAll(
                                    EntityMapper.toEntityList(items)
                            );
                        } else {
                            dictionaryAdapter.addItems(items);

                            repository.insertAll(
                                    EntityMapper.toEntityList(items)
                            );
                        }

                        hasMore = more;
                        currentPage++;

                        dictionaryAdapter.setPaginationState(
                                false,
                                hasMore
                        );
                    }

                    @Override
                    public void onError(String error) {
                        isLoading = false;

                        if (!isAdded() || binding == null) {
                            return;
                        }

                        binding.progressBar.setVisibility(View.GONE);

                        dictionaryAdapter.setPaginationState(
                                false,
                                hasMore
                        );

                        Toast.makeText(
                                requireContext(),
                                error,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    // ---------------------------------------------------------
    // WORD PRONUNCIATION
    // ---------------------------------------------------------

    private void speakWord(Word word) {
        if (word == null) {
            return;
        }

        String ttsUrl = word.getTtsUrl();

        if (ttsUrl != null && !ttsUrl.isEmpty()) {
            playAudio(ttsUrl);
        }

        String english =
                word.getEnglishTranslation() != null
                        ? word.getEnglishTranslation()
                        : "";

        String tagalog =
                word.getTagalogTranslation() != null
                        ? word.getTagalogTranslation()
                        : "";

        if ("English".equals(selectedLanguage)
                && !english.isEmpty()) {

            if (tts != null) {
                tts.setLanguage(Locale.US);

                tts.speak(
                        english,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsEnglish"
                );
            }

        } else if ("Tagalog".equals(selectedLanguage)
                && !tagalog.isEmpty()) {

            if (tts != null) {
                tts.setLanguage(new Locale("fil", "PH"));

                tts.speak(
                        tagalog,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsTagalog"
                );
            }
        }
    }

    // ---------------------------------------------------------
    // LOAD OFFLINE WORDS
    // ---------------------------------------------------------

    private void loadOfflineWords() {
        if (!isAdded() || binding == null || repository == null) {
            return;
        }

        repository.getAllWords(result -> {
            if (!isAdded()) {
                return;
            }

            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || binding == null) {
                    return;
                }

                List<Word> words =
                        EntityMapper.toWordList(result);

                binding.progressBar.setVisibility(View.GONE);

                binding.textViewEmpty.setVisibility(
                        words.isEmpty()
                                ? View.VISIBLE
                                : View.GONE
                );

                dictionaryAdapter.filterList(words);
            });
        });
    }

    // ---------------------------------------------------------
    // SEARCH OFFLINE WORDS
    // ---------------------------------------------------------

    private void searchOffline(String query) {
        if (!isAdded() || binding == null || repository == null) {
            return;
        }

        repository.searchWords(query, result -> {
            if (!isAdded()) {
                return;
            }

            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || binding == null) {
                    return;
                }

                List<Word> words =
                        EntityMapper.toWordList(result);

                binding.textViewEmpty.setVisibility(
                        words.isEmpty()
                                ? View.VISIBLE
                                : View.GONE
                );

                dictionaryAdapter.filterList(words);
            });
        });
    }

    // ---------------------------------------------------------
    // PLAY RECORDED AUDIO
    // ---------------------------------------------------------

    private void playAudio(String url) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(url);

            mediaPlayer.setOnPreparedListener(
                    player -> player.start()
            );

            mediaPlayer.setOnErrorListener(
                    (player, what, extra) -> {
                        player.release();

                        if (mediaPlayer == player) {
                            mediaPlayer = null;
                        }

                        return true;
                    }
            );

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            Log.e(
                    "DictionaryFragment",
                    "Unable to play dictionary audio",
                    e
            );

            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
    }

    // ---------------------------------------------------------
    // CLEANUP
    // ---------------------------------------------------------

    @Override
    public void onDestroyView() {
        searchHandler.removeCallbacksAndMessages(null);

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }

        binding = null;

        super.onDestroyView();
    }
}
