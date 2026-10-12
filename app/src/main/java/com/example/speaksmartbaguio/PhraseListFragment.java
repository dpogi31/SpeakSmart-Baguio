package com.example.speaksmartbaguio;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.speaksmartbaguio.databinding.FragmentPhraseListBinding;
import com.example.speaksmartbaguio.utils.AccessibilityPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PhraseListFragment extends Fragment {

    private FragmentPhraseListBinding binding;
    private PhraseAdapter adapter;
    private ApiService apiService;
    private TextToSpeech tts;
    private MediaPlayer mediaPlayer;

    private String selectedLanguage = "English";

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;

    private static final int PAGE_SIZE = 20;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentPhraseListBinding.inflate(
                inflater,
                container,
                false
        );

        View root = binding.getRoot();

        // Apply saved accessibility preferences to this screen.
        AccessibilityPreferences.applyToViewTree(
                requireContext(),
                root
        );

        apiService = ApiService.getInstance();

        setupTextToSpeech();
        setupLanguageDropdown();
        setupRecyclerView();
        loadPhrases(true);

        return root;
    }

    private void setupTextToSpeech() {
        tts = new TextToSpeech(
                requireContext().getApplicationContext(),
                status -> {
                    if (!isAdded() || binding == null) {
                        if (tts != null) {
                            tts.shutdown();
                            tts = null;
                        }
                        return;
                    }

                    if (status != TextToSpeech.SUCCESS) {
                        Toast.makeText(
                                requireContext(),
                                "TTS initialization failed",
                                Toast.LENGTH_SHORT
                        ).show();
                    } else if (tts != null) {
                        tts.setLanguage(Locale.getDefault());
                    }
                }
        );
    }

    private void setupLanguageDropdown() {
        String[] languages = {"English", "Tagalog"};

        ArrayAdapter<String> adapterDropdown = new ArrayAdapter<>(
                requireContext(),
                R.layout.dropdown_item_bold,
                languages
        );

        binding.languageDropdown.setAdapter(adapterDropdown);

        binding.languageDropdown.setDropDownBackgroundResource(
                R.drawable.dropdown_glass_background
        );

        binding.languageDropdown.setKeyListener(null);
        binding.languageDropdown.setFocusable(false);
        binding.languageDropdown.setInputType(InputType.TYPE_NULL);
        binding.languageDropdown.setText(selectedLanguage, false);

        binding.languageDropdown.setOnItemClickListener(
                (parent, view, position, id) -> {
                    selectedLanguage =
                            parent.getItemAtPosition(position).toString();

                    binding.languageDropdown.setText(
                            selectedLanguage,
                            false
                    );

                    if (adapter != null) {
                        adapter.setSelectedLanguage(selectedLanguage);
                    }
                }
        );
    }

    private void setupRecyclerView() {
        adapter = new PhraseAdapter(
                new ArrayList<>(),
                this::speakPhrase,
                selectedLanguage
        );

        adapter.setOnLoadMoreListener(
                () -> loadPhrases(false)
        );

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(requireContext());

        binding.recyclerViewPhrases.setLayoutManager(layoutManager);
        binding.recyclerViewPhrases.setAdapter(adapter);

        binding.recyclerViewPhrases.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(
                            @NonNull RecyclerView recyclerView,
                            int dx,
                            int dy
                    ) {
                        if (dy <= 0 || isLoading || !hasMore
                                || adapter == null) {
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

    private void loadPhrases(boolean reset) {
        if (binding == null || isLoading) {
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

        apiService.getPhrasebook(
                currentPage,
                PAGE_SIZE,
                null,
                new ApiService.ApiCallback<Phrase>() {
                    @Override
                    public void onSuccess(
                            List<Phrase> items,
                            boolean more
                    ) {
                        if (!isAdded() || binding == null
                                || adapter == null) {
                            isLoading = false;
                            return;
                        }

                        isLoading = false;

                        binding.progressBar.setVisibility(View.GONE);

                        boolean isEmpty =
                                items == null || items.isEmpty();

                        binding.textViewEmpty.setVisibility(
                                isEmpty && reset
                                        ? View.VISIBLE
                                        : View.GONE
                        );

                        if (items == null) {
                            adapter.setPaginationState(false, hasMore);
                            return;
                        }

                        if (reset) {
                            adapter.filterList(
                                    new ArrayList<>(items)
                            );
                        } else {
                            adapter.addItems(items);
                        }

                        hasMore = more;
                        currentPage++;

                        adapter.setPaginationState(false, hasMore);

                        // Apply preferences to the refreshed screen.
                        AccessibilityPreferences.applyToViewTree(
                                requireContext(),
                                binding.getRoot()
                        );
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded() || binding == null
                                || adapter == null) {
                            isLoading = false;
                            return;
                        }

                        isLoading = false;

                        binding.progressBar.setVisibility(View.GONE);

                        adapter.setPaginationState(false, hasMore);

                        Toast.makeText(
                                requireContext(),
                                error,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void speakPhrase(Phrase phrase) {
        if (phrase == null || !isAdded()) {
            return;
        }

        String audioUrl = phrase.getTtsUrl();

        if (audioUrl != null && !audioUrl.trim().isEmpty()) {
            playAudio(audioUrl);
        }

        String targetText = "Tagalog".equals(selectedLanguage)
                ? phrase.getTagalogTranslation()
                : phrase.getEnglishTranslation();

        targetText = targetText != null ? targetText : "";

        if (!targetText.trim().isEmpty() && tts != null) {
            Locale locale = "Tagalog".equals(selectedLanguage)
                    ? new Locale("fil", "PH")
                    : Locale.US;

            int result = tts.setLanguage(locale);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.getDefault());
            }

            tts.speak(
                    targetText,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "ttsTarget"
            );
        }
    }

    private void playAudio(String url) {
        releaseMediaPlayer();

        try {
            MediaPlayer player = new MediaPlayer();
            mediaPlayer = player;

            player.setOnPreparedListener(preparedPlayer -> {
                if (!isAdded() || mediaPlayer != preparedPlayer) {
                    preparedPlayer.release();
                    return;
                }

                preparedPlayer.start();
            });

            player.setOnCompletionListener(completedPlayer -> {
                if (mediaPlayer == completedPlayer) {
                    releaseMediaPlayer();
                }
            });

            player.setOnErrorListener((mp, what, extra) -> {
                if (mediaPlayer == mp) {
                    releaseMediaPlayer();
                }
                return true;
            });

            player.setDataSource(url);
            player.prepareAsync();

        } catch (IOException | IllegalArgumentException
                 | IllegalStateException e) {
            releaseMediaPlayer();
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException ignored) {
                // The player may already have stopped or failed.
            }

            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onDestroyView() {
        releaseMediaPlayer();

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }

        adapter = null;
        binding = null;
        isLoading = false;

        super.onDestroyView();
    }
}
