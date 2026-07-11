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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.speaksmartbaguio.databinding.FragmentPhrasebookBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PhrasebookFragment extends Fragment {

    private FragmentPhrasebookBinding binding;
    private PhrasebookAdapter adapter;
    private ApiService apiService;
    private TextToSpeech tts;
    private MediaPlayer mediaPlayer;
    private String selectedLanguage = "English";

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private static final int PAGE_SIZE = 20;

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private String pendingSearchQuery = "";

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

        apiService = ApiService.getInstance();
        setupTextToSpeech();
        setupRecyclerView();
        setupLanguageDropdown();
        setupSearch();
        loadPhrases(true);
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
        adapter = new PhrasebookAdapter(new ArrayList<>(), phrase -> speakPhrase(phrase), selectedLanguage);
        adapter.setOnLoadMoreListener(() -> loadPhrases(false));
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        binding.recyclerViewPhrasebook.setLayoutManager(lm);
        binding.recyclerViewPhrasebook.setAdapter(adapter);
        binding.recyclerViewPhrasebook.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

    private void setupLanguageDropdown() {
        String[] languages = {"English", "Tagalog"};

        ArrayAdapter<String> dropdownAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.dropdown_item_bold,
                languages
        );

        binding.languageDropdown.setAdapter(dropdownAdapter);
        binding.languageDropdown.setDropDownBackgroundResource(R.drawable.dropdown_glass_background);
        binding.languageDropdown.setKeyListener(null);
        binding.languageDropdown.setFocusable(false);
        binding.languageDropdown.setInputType(InputType.TYPE_NULL);
        binding.languageDropdownLayout.setHint("Select Target Language");
        binding.languageDropdown.setText("", false);

        binding.languageDropdown.setOnItemClickListener((parent, view, position, id) -> {
            selectedLanguage = parent.getItemAtPosition(position).toString();
            binding.languageDropdown.setText(selectedLanguage, false);
            if (adapter != null) {
                adapter.setSelectedLanguage(selectedLanguage);
                adapter.notifyDataSetChanged();
            }

            SharedPreferences prefs = requireContext()
                    .getSharedPreferences("PhrasePrefs", Context.MODE_PRIVATE);
            prefs.edit().putString("selectedLanguage", selectedLanguage).apply();
        });
    }

    private void setupSearch() {
        binding.searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacksAndMessages(null);
                pendingSearchQuery = s.toString().trim();
                searchHandler.postDelayed(() -> loadPhrases(true), 300);
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

        apiService.getPhrasebook(currentPage, PAGE_SIZE, pendingSearchQuery, new ApiService.ApiCallback<Phrase>() {
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

        if (targetText != null && !targetText.isEmpty()) {
            if (tts != null) {
                tts.setLanguage(targetLocale);
                tts.speak(targetText, TextToSpeech.QUEUE_FLUSH, null, "ttsTarget");
            }
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
        searchHandler.removeCallbacksAndMessages(null);
        binding = null;
    }
}
