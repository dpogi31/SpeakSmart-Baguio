package com.example.speaksmartbaguio;

import android.content.Context;
import android.media.MediaPlayer;
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
import androidx.recyclerview.widget.RecyclerView;

import com.example.speaksmartbaguio.databinding.FragmentPhraseListBinding;

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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPhraseListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        apiService = ApiService.getInstance();
        setupTextToSpeech();
        setupLanguageDropdown();
        setupRecyclerView();
        loadPhrases(true);

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
            if (adapter != null) {
                adapter.setSelectedLanguage(selectedLanguage);
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new PhraseAdapter(new ArrayList<>(), this::speakPhrase, selectedLanguage);

        adapter.setOnLoadMoreListener(() -> loadPhrases(false));
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        binding.recyclerViewPhrases.setLayoutManager(lm);
        binding.recyclerViewPhrases.setAdapter(adapter);
        binding.recyclerViewPhrases.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0 || isLoading || !hasMore) return;
                int totalCount = adapter.getItemCount();
                int lastVisible = lm.findLastVisibleItemPosition();
                if (lastVisible >= totalCount - 3) {
                    loadPhrases(false);
                }
            }
        });
    }

    private void loadPhrases(boolean reset) {
        if (isLoading) return;
        if (!reset && !hasMore) return;

        isLoading = true;

        if (reset) {
            currentPage = 1;
            hasMore = true;
            binding.progressBar.setVisibility(View.VISIBLE);
            adapter.setPaginationState(false, false);
        } else {
            adapter.setPaginationState(true, true);
        }

        apiService.getPhrasebook(currentPage, PAGE_SIZE, null, new ApiService.ApiCallback<Phrase>() {
            @Override
            public void onSuccess(List<Phrase> items, boolean more) {
                isLoading = false;
                binding.progressBar.setVisibility(View.GONE);
                binding.textViewEmpty.setVisibility(items.isEmpty() && reset ? View.VISIBLE : View.GONE);

                if (reset) {
                    adapter.filterList(new ArrayList<>(items));
                } else {
                    adapter.addItems(items);
                }

                hasMore = more;
                currentPage++;
                adapter.setPaginationState(false, hasMore);
            }

            @Override
            public void onError(String error) {
                isLoading = false;
                binding.progressBar.setVisibility(View.GONE);
                adapter.setPaginationState(false, hasMore);
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void speakPhrase(Phrase phrase) {
        if (phrase == null) return;

        String ttsUrl = phrase.getTtsUrl();
        if (ttsUrl != null && !ttsUrl.isEmpty()) {
            playAudio(ttsUrl);
        }

        String targetText = "Tagalog".equals(selectedLanguage)
                ? phrase.getTagalogTranslation()
                : phrase.getEnglishTranslation();
        targetText = targetText != null ? targetText : "";

        if (!targetText.isEmpty() && tts != null) {
            Locale locale = "Tagalog".equals(selectedLanguage)
                    ? new Locale("fil", "PH")
                    : Locale.US;
            tts.setLanguage(locale);
            tts.speak(targetText, TextToSpeech.QUEUE_FLUSH, null, "ttsTarget");
        }
    }

    private void playAudio(String url) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                mp.release();
                mediaPlayer = null;
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        binding = null;
    }
}
