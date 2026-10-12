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

import com.example.speaksmartbaguio.databinding.FragmentPhrasebookBinding;
import com.example.speaksmartbaguio.mapper.EntityMapper;
import com.example.speaksmartbaguio.repository.PhraseRepository;
import com.example.speaksmartbaguio.utils.AccessibilityPreferences;
import com.example.speaksmartbaguio.utils.NetworkUtil;
import com.example.speaksmartbaguio.utils.SettingsMenuHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PhrasebookFragment extends Fragment {

    private FragmentPhrasebookBinding binding;
    private PhrasebookAdapter adapter;
    private ApiService apiService;
    private PhraseRepository repository;

    private TextToSpeech tts;
    private MediaPlayer mediaPlayer;

    private String selectedLanguage = "English";
    private String pendingSearchQuery = "";

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;

    private static final int PAGE_SIZE = 100;

    private final Handler searchHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable searchRunnable = () -> {
        if (binding != null && isAdded()) {
            loadPhrases(true);
        }
    };

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentPhrasebookBinding.inflate(
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
        repository = new PhraseRepository(requireContext());

        setupTextToSpeech();
        setupRecyclerView();
        setupLanguageDropdown();
        setupSearch();
        setupSettingsMenuButton();

        // Apply saved accessibility settings to the Phrasebook screen.
        AccessibilityPreferences.applyToViewTree(
                requireContext(),
                view
        );

        // Display saved phrases immediately.
        loadOfflinePhrases();

        // Refresh phrases from the server when online.
        if (NetworkUtil.isOnline(requireContext())) {
            loadPhrases(true);
        }
    }

    // ---------------------------------------------------------
    // SHARED SETTINGS MENU
    // ---------------------------------------------------------

    private void setupSettingsMenuButton() {
        if (binding == null) {
            return;
        }

        binding.buttonSettingsMenu.setOnClickListener(
                v -> SettingsMenuHelper.show(v)
        );
    }

    // ---------------------------------------------------------
    // TEXT-TO-SPEECH
    // ---------------------------------------------------------

    private void setupTextToSpeech() {
        Context context = getContext();

        if (context == null) {
            return;
        }

        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                if (tts != null) {
                    tts.setPitch(0.95f);
                    tts.setSpeechRate(0.85f);
                    tts.setLanguage(Locale.US);
                }
            } else if (isAdded()) {
                Context currentContext = getContext();

                if (currentContext != null) {
                    Toast.makeText(
                            currentContext,
                            "Text-to-speech initialization failed.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });
    }

    // ---------------------------------------------------------
    // RECYCLER VIEW
    // ---------------------------------------------------------

    private void setupRecyclerView() {
        adapter = new PhrasebookAdapter(
                new ArrayList<>(),
                this::speakPhrase,
                selectedLanguage
        );

        adapter.setSearchQuery(pendingSearchQuery);

        adapter.setOnLoadMoreListener(() -> {
            if (!isLoading && hasMore) {
                loadPhrases(false);
            }
        });

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(requireContext());

        binding.recyclerViewPhrasebook.setLayoutManager(
                layoutManager
        );

        binding.recyclerViewPhrasebook.setAdapter(adapter);

        binding.recyclerViewPhrasebook.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(
                            @NonNull RecyclerView recyclerView,
                            int dx,
                            int dy
                    ) {
                        super.onScrolled(recyclerView, dx, dy);

                        if (dy <= 0 || isLoading || !hasMore) {
                            return;
                        }

                        int totalCount = adapter.getItemCount();

                        int lastVisible =
                                layoutManager.findLastVisibleItemPosition();

                        if (lastVisible >= totalCount - 3) {
                            loadPhrases(false);
                        }
                    }
                }
        );
    }

// ---------------------------------------------------------
// LANGUAGE DROPDOWN
// Keeps all languages visible and toggles open/close on tap.
// ---------------------------------------------------------

    private void setupLanguageDropdown() {

        // Keep the complete list of available languages.
        final String[] languages = {
                "English",
                "Tagalog"
        };

        // Load the saved language preference.
        SharedPreferences prefs =
                requireContext().getSharedPreferences(
                        "PhrasePrefs",
                        Context.MODE_PRIVATE
                );

        selectedLanguage = prefs.getString(
                "selectedLanguage",
                "English"
        );

        // Validate the saved language.
        if (!"English".equals(selectedLanguage)
                && !"Tagalog".equals(selectedLanguage)) {

            selectedLanguage = "English";
        }

        // Create an adapter that always returns every language.
        ArrayAdapter<String> dropdownAdapter =
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

        // Attach the dropdown adapter.
        binding.languageDropdown.setAdapter(dropdownAdapter);

        // Preserve the existing dropdown appearance.
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

        // Preserve the existing hint.
        binding.languageDropdownLayout.setHint(
                "Select Target Language"
        );

        // Restore the saved selection.
        binding.languageDropdown.setText(
                selectedLanguage,
                false
        );

        // Update the Phrasebook adapter's selected language.
        if (adapter != null) {
            adapter.setSelectedLanguage(selectedLanguage);
            adapter.notifyDataSetChanged();
        }

        // Track the dropdown's open/closed state.
        final boolean[] isDropdownOpen = {false};

        // Toggle the dropdown when the field or arrow is tapped.
        View.OnClickListener toggleDropdown = v -> {

            if (binding == null) {
                return;
            }

            // Close it if it is already open.
            if (isDropdownOpen[0]) {
                binding.languageDropdown.dismissDropDown();
                isDropdownOpen[0] = false;
                return;
            }

            // Otherwise, restore all languages and open it.
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
                                selectedLanguage,
                                false
                        );

                        binding.languageDropdown.showDropDown();
                        isDropdownOpen[0] = true;
                    }
            );
        };

        // Tapping the field opens or closes the dropdown.
        binding.languageDropdown.setOnClickListener(
                toggleDropdown
        );

        // Tapping the dropdown arrow does the same.
        binding.languageDropdownLayout.setEndIconOnClickListener(
                toggleDropdown
        );

        // Handle language selection.
        binding.languageDropdown.setOnItemClickListener(
                (parent, itemView, position, id) -> {

                    Object selectedItem =
                            parent.getItemAtPosition(position);

                    if (selectedItem == null) {
                        return;
                    }

                    selectedLanguage = selectedItem.toString();

                    // Save the language preference.
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

                    // Mark the dropdown as closed.
                    isDropdownOpen[0] = false;

                    // Update the existing Phrasebook adapter.
                    if (adapter != null) {
                        adapter.setSelectedLanguage(
                                selectedLanguage
                        );

                        adapter.notifyDataSetChanged();
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
                        pendingSearchQuery = s.toString().trim();

                        // Update search highlighting immediately.
                        if (adapter != null) {
                            adapter.setSearchQuery(
                                    pendingSearchQuery
                            );
                        }

                        // Debounce search requests.
                        searchHandler.removeCallbacks(
                                searchRunnable
                        );

                        searchHandler.postDelayed(
                                searchRunnable,
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
    // LOAD ONLINE PHRASES
    // ---------------------------------------------------------

    private void loadPhrases(boolean reset) {
        if (binding == null || !isAdded()) {
            return;
        }

        Context context = getContext();

        if (context == null) {
            return;
        }

        // Offline mode uses the local Room database.
        if (!NetworkUtil.isOnline(context)) {
            if (pendingSearchQuery.isEmpty()) {
                loadOfflinePhrases();
            } else {
                searchOfflinePhrases(pendingSearchQuery);
            }

            Toast.makeText(
                    context,
                    "Offline Mode — showing saved phrases",
                    Toast.LENGTH_SHORT
            ).show();

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

            adapter.setPaginationState(false, false);
        } else {
            adapter.setPaginationState(true, true);
        }

        // Capture the query and page for this request.
        final String requestedQuery = pendingSearchQuery;
        final int requestedPage = currentPage;

        apiService.getPhrasebook(
                requestedPage,
                PAGE_SIZE,
                requestedQuery,
                new ApiService.ApiCallback<Phrase>() {

                    @Override
                    public void onSuccess(
                            List<Phrase> items,
                            boolean more
                    ) {
                        if (binding == null || !isAdded()) {
                            isLoading = false;
                            return;
                        }

                        // Ignore results for an older search.
                        if (!requestedQuery.equals(
                                pendingSearchQuery
                        )) {
                            isLoading = false;

                            adapter.setPaginationState(
                                    false,
                                    hasMore
                            );

                            loadPhrases(true);
                            return;
                        }

                        isLoading = false;

                        binding.progressBar.setVisibility(
                                View.GONE
                        );

                        List<Phrase> safeItems =
                                items != null
                                        ? items
                                        : new ArrayList<>();

                        if (reset) {
                            adapter.filterList(
                                    new ArrayList<>(safeItems)
                            );
                        } else {
                            adapter.addItems(safeItems);
                        }

                        // Save returned phrases for offline use.
                        if (!safeItems.isEmpty()) {
                            repository.insertAll(
                                    EntityMapper.toPhraseEntityList(
                                            safeItems
                                    )
                            );
                        }

                        hasMore = more;
                        currentPage = requestedPage + 1;

                        adapter.setPaginationState(
                                false,
                                hasMore
                        );

                        updateEmptyState(
                                adapterPhraseCount(),
                                requestedQuery
                        );
                    }

                    @Override
                    public void onError(String error) {
                        isLoading = false;

                        if (binding == null || !isAdded()) {
                            return;
                        }

                        binding.progressBar.setVisibility(
                                View.GONE
                        );

                        adapter.setPaginationState(
                                false,
                                hasMore
                        );

                        // Fall back to locally saved phrases.
                        if (requestedQuery.isEmpty()) {
                            loadOfflinePhrases();
                        } else {
                            searchOfflinePhrases(
                                    requestedQuery
                            );
                        }

                        Context currentContext = getContext();

                        if (currentContext != null) {
                            Toast.makeText(
                                    currentContext,
                                    "Unable to refresh phrases. "
                                            + "Showing saved results if available.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                }
        );
    }

    // ---------------------------------------------------------
    // LOAD OFFLINE PHRASES
    // ---------------------------------------------------------

    private void loadOfflinePhrases() {
        if (repository == null) {
            return;
        }

        repository.getAllPhrases(result -> {
            if (!isAdded()) {
                return;
            }

            requireActivity().runOnUiThread(() -> {
                if (binding == null || adapter == null) {
                    return;
                }

                List<Phrase> phrases =
                        EntityMapper.toPhraseList(result);

                if (phrases == null) {
                    phrases = new ArrayList<>();
                }

                adapter.filterList(phrases);

                updateEmptyState(
                        phrases.size(),
                        pendingSearchQuery
                );

                binding.progressBar.setVisibility(View.GONE);
            });
        });
    }

    // ---------------------------------------------------------
    // SEARCH OFFLINE PHRASES
    // ---------------------------------------------------------

    private void searchOfflinePhrases(String query) {
        if (repository == null) {
            return;
        }

        repository.searchPhrases(query, result -> {
            if (!isAdded()) {
                return;
            }

            requireActivity().runOnUiThread(() -> {
                if (binding == null || adapter == null) {
                    return;
                }

                List<Phrase> phrases =
                        EntityMapper.toPhraseList(result);

                if (phrases == null) {
                    phrases = new ArrayList<>();
                }

                adapter.filterList(phrases);

                updateEmptyState(
                        phrases.size(),
                        query
                );

                binding.progressBar.setVisibility(View.GONE);
            });
        });
    }

    // ---------------------------------------------------------
    // EMPTY STATE
    // ---------------------------------------------------------

    private int adapterPhraseCount() {
        if (adapter == null) {
            return 0;
        }

        // The adapter includes one footer row.
        return Math.max(0, adapter.getItemCount() - 1);
    }

    private void updateEmptyState(
            int phraseCount,
            String query
    ) {
        if (binding == null) {
            return;
        }

        if (phraseCount == 0) {
            binding.textViewEmpty.setVisibility(View.VISIBLE);

            if (query != null && !query.trim().isEmpty()) {
                binding.textViewEmpty.setText(
                        "No phrases found. Try another search."
                );
            } else {
                binding.textViewEmpty.setText(
                        "No phrases available yet. "
                                + "Connect to the internet to refresh."
                );
            }

            binding.recyclerViewPhrasebook.setVisibility(
                    View.GONE
            );

        } else {
            binding.textViewEmpty.setVisibility(View.GONE);

            binding.recyclerViewPhrasebook.setVisibility(
                    View.VISIBLE
            );
        }
    }

    // ---------------------------------------------------------
    // PRONUNCIATION
    // ---------------------------------------------------------

    private void speakPhrase(Phrase phrase) {
        if (phrase == null || !isAdded()) {
            return;
        }

        String audioUrl = phrase.getTtsUrl();

        // Prefer recorded Ilokano audio when available.
        if (audioUrl != null && !audioUrl.trim().isEmpty()) {
            stopTextToSpeech();
            playAudio(audioUrl);
            return;
        }

        stopAudio();

        String targetText;
        Locale targetLocale;

        if ("Tagalog".equalsIgnoreCase(selectedLanguage)) {
            targetText = phrase.getTagalogTranslation();
            targetLocale = new Locale("fil", "PH");
        } else {
            targetText = phrase.getEnglishTranslation();
            targetLocale = Locale.US;
        }

        if (targetText == null || targetText.trim().isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    "No translation available to play.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (tts != null) {
            int languageStatus = tts.setLanguage(targetLocale);

            if (languageStatus == TextToSpeech.LANG_MISSING_DATA
                    || languageStatus == TextToSpeech.LANG_NOT_SUPPORTED) {

                Toast.makeText(
                        requireContext(),
                        "The selected voice is not available "
                                + "on this device.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            tts.speak(
                    targetText,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "phrase_translation"
            );
        }
    }

    // ---------------------------------------------------------
    // RECORDED AUDIO PLAYER
    // ---------------------------------------------------------

    private void playAudio(String url) {
        stopAudio();

        try {
            MediaPlayer player = new MediaPlayer();
            mediaPlayer = player;

            player.setDataSource(url);

            player.setOnPreparedListener(mp -> {
                if (mediaPlayer == mp && binding != null) {
                    mp.start();
                } else {
                    mp.release();
                }
            });

            player.setOnCompletionListener(mp -> {
                mp.release();

                if (mediaPlayer == mp) {
                    mediaPlayer = null;
                }
            });

            player.setOnErrorListener((mp, what, extra) -> {
                mp.release();

                if (mediaPlayer == mp) {
                    mediaPlayer = null;
                }

                if (isAdded()) {
                    Context context = getContext();

                    if (context != null) {
                        Toast.makeText(
                                context,
                                "Unable to play the recording.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }

                return true;
            });

            player.prepareAsync();

        } catch (Exception e) {
            stopAudio();

            if (isAdded()) {
                Context context = getContext();

                if (context != null) {
                    Toast.makeText(
                            context,
                            "Unable to play the recording.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        }
    }

    private void stopAudio() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.setOnPreparedListener(null);
                mediaPlayer.setOnCompletionListener(null);
                mediaPlayer.setOnErrorListener(null);
                mediaPlayer.release();
            } catch (Exception ignored) {
                // Player may already have been released.
            }

            mediaPlayer = null;
        }
    }

    private void stopTextToSpeech() {
        if (tts != null) {
            try {
                tts.stop();
            } catch (Exception ignored) {
                // TTS may already be stopped.
            }
        }
    }

    // ---------------------------------------------------------
    // CLEANUP
    // ---------------------------------------------------------

    @Override
    public void onDestroyView() {
        searchHandler.removeCallbacksAndMessages(null);

        stopAudio();

        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {
                // TTS may already have been shut down.
            }

            tts = null;
        }

        binding = null;

        super.onDestroyView();
    }
}
