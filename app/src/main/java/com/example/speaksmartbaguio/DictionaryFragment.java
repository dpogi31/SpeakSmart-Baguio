package com.example.speaksmartbaguio;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import com.example.speaksmartbaguio.entity.DictionaryEntity;
import com.example.speaksmartbaguio.mapper.EntityMapper;
import com.example.speaksmartbaguio.repository.DictionaryRepository;
import com.example.speaksmartbaguio.utils.NetworkUtil;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import java.util.concurrent.Executors;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.speaksmartbaguio.databinding.FragmentDictionaryBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.util.Log;
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

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private String pendingSearchQuery = "";

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

        apiService = ApiService.getInstance();
        repository = new DictionaryRepository(requireContext());

        setupTextToSpeech();
        setupRecyclerView();
        setupLanguageDropdown();
        setupSearch();

        loadOfflineWords();

        if (NetworkUtil.isOnline(requireContext())) {
            syncAllDictionaryPages();
        }
    }
    private void syncAllDictionaryPages() {

        currentPage = 1;


        downloadNextPage();

    }


    private void downloadNextPage() {

        apiService.getDictionary(
                currentPage,
                PAGE_SIZE,
                "",
                new ApiService.ApiCallback<Word>() {

                    @Override
                    public void onSuccess(List<Word> items, boolean more) {


                        repository.insertAll(
                                EntityMapper.toEntityList(items)
                        );


                        Log.d(
                                "SYNC_TEST",
                                "Saved page "
                                        + currentPage
                                        + " : "
                                        + items.size()
                                        + " words"
                        );


                        if(more){

                            currentPage++;

                            downloadNextPage();

                        }
                        else {

                            Log.d("SYNC_TEST", "SYNC COMPLETE");

                            requireActivity().runOnUiThread(() -> {
                                loadOfflineWords();
                            });

                        }

                    }


                    @Override
                    public void onError(String error) {

                        Log.e(
                                "SYNC_TEST",
                                error
                        );

                    }

                }
        );
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
        dictionaryAdapter.setOnLoadMoreListener(() -> loadWords(false));
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        binding.recyclerViewDictionary.setLayoutManager(lm);
        binding.recyclerViewDictionary.setAdapter(dictionaryAdapter);
        binding.recyclerViewDictionary.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0 || isLoading || !hasMore) return;
                int totalCount = dictionaryAdapter.getItemCount();
                int lastVisible = lm.findLastVisibleItemPosition();
                if (lastVisible >= totalCount - 3) {
                    loadWords(false);
                }
            }
        });
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
            if (dictionaryAdapter != null) {
                dictionaryAdapter.setSelectedLanguage(selectedLanguage);
            }
        });

        binding.languageDropdown.setText("", false);
    }

    private void setupSearch() {
        binding.searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacksAndMessages(null);
                pendingSearchQuery = s.toString().trim();
                searchHandler.postDelayed(() -> loadWords(true), 300);
            }
        });
    }

    private void loadWords(boolean reset) {

        if (!NetworkUtil.isOnline(requireContext())) {

            if (!pendingSearchQuery.isEmpty()) {
                searchOffline(pendingSearchQuery);
            } else {
                loadOfflineWords();
            }

            return;
        }

        if (isLoading) return;
        if (!reset && !hasMore) return;

        isLoading = true;

        if (reset) {
            currentPage = 1;
            hasMore = true;
            binding.progressBar.setVisibility(View.VISIBLE);
            dictionaryAdapter.setPaginationState(false, false);
        } else {
            dictionaryAdapter.setPaginationState(true, true);
        }

        apiService.getDictionary(currentPage, PAGE_SIZE, pendingSearchQuery, new ApiService.ApiCallback<Word>() {
            @Override
            public void onSuccess(List<Word> items, boolean more) {
                isLoading = false;
                binding.progressBar.setVisibility(View.GONE);
                binding.textViewEmpty.setVisibility(items.isEmpty() && reset ? View.VISIBLE : View.GONE);
                Log.d("SYNC_TEST", "Downloaded words: " + items.size());
                Log.d("SYNC_TEST", "Has more: " + more);
                if (reset) {
                    dictionaryAdapter.filterList(new ArrayList<>(items));
                } else {
                    dictionaryAdapter.addItems(items);
                }

                hasMore = more;
                currentPage++;
                dictionaryAdapter.setPaginationState(false, hasMore);
            }

            @Override
            public void onError(String error) {
                isLoading = false;
                binding.progressBar.setVisibility(View.GONE);
                dictionaryAdapter.setPaginationState(false, hasMore);
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void speakWord(Word word) {
        if (word == null) return;

        String ttsUrl = word.getTtsUrl();
        if (ttsUrl != null && !ttsUrl.isEmpty()) {
            playAudio(ttsUrl);
        }

        String english = word.getEnglishTranslation() != null ? word.getEnglishTranslation() : "";
        String tagalog = word.getTagalogTranslation() != null ? word.getTagalogTranslation() : "";

        if (selectedLanguage.equals("English") && !english.isEmpty()) {
            if (tts != null) {
                tts.setLanguage(Locale.US);
                tts.speak(english, TextToSpeech.QUEUE_FLUSH, null, "ttsEnglish");
            }
        } else if (selectedLanguage.equals("Tagalog") && !tagalog.isEmpty()) {
            if (tts != null) {
                tts.setLanguage(new Locale("fil", "PH"));
                tts.speak(tagalog, TextToSpeech.QUEUE_FLUSH, null, "ttsTagalog");
            }
        }
    }
    private void saveWordsToRoom(List<Word> words) {

        repository.replaceAll(
                EntityMapper.toEntityList(words)
        );

    }
    private void loadOfflineWords() {

        repository.getAllWords(result -> {

            requireActivity().runOnUiThread(() -> {

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
    private void searchOffline(String query) {

        repository.searchWords(query, result -> {

            requireActivity().runOnUiThread(() -> {

                dictionaryAdapter.filterList(
                        EntityMapper.toWordList(result)
                );

            });

        });

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
