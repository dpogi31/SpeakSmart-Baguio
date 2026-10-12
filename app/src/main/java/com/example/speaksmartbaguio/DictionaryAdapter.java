
package com.example.speaksmartbaguio;

import android.content.Intent;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.speaksmartbaguio.entity.FavoriteEntity;
import com.example.speaksmartbaguio.repository.FavoriteRepository;
import com.example.speaksmartbaguio.utils.AccessibilityPreferences;

import java.util.List;

public class DictionaryAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;

    private static final String HEART_EMPTY = "♡";
    private static final String HEART_FILLED = "♥";

    private List<Word> wordList;
    private OnPlayClickListener playClickListener;

    private String searchQuery = "";
    private String selectedLanguage = "English";

    private boolean isLoading = false;
    private boolean hasMore = true;

    private Runnable loadMoreListener;

    private FavoriteRepository favoriteRepository;

    public interface OnPlayClickListener {
        void onPlayClick(Word word);
    }

    public DictionaryAdapter(
            List<Word> wordList,
            OnPlayClickListener listener,
            String selectedLanguage
    ) {
        this.wordList = wordList;
        this.playClickListener = listener;
        this.selectedLanguage = selectedLanguage;
    }

    private FavoriteRepository getFavoriteRepository(View view) {
        if (favoriteRepository == null) {
            favoriteRepository = new FavoriteRepository(
                    view.getContext().getApplicationContext()
            );
        }

        return favoriteRepository;
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query != null
                ? query.toLowerCase()
                : "";
    }

    public void setSelectedLanguage(String language) {
        this.selectedLanguage = language;
        notifyDataSetChanged();
    }

    public void setPaginationState(
            boolean isLoading,
            boolean hasMore
    ) {
        this.isLoading = isLoading;
        this.hasMore = hasMore;

        int footerPosition = getItemCount() - 1;

        if (footerPosition >= 0) {
            notifyItemChanged(footerPosition);
        }
    }

    public void setOnLoadMoreListener(Runnable listener) {
        this.loadMoreListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (wordList != null && position == wordList.size()) {
            return TYPE_FOOTER;
        }

        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        if (viewType == TYPE_FOOTER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(
                            R.layout.item_pagination_footer,
                            parent,
                            false
                    );

            return new FooterViewHolder(view);
        }

        View view = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_list_card,
                        parent,
                        false
                );

        return new WordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {
        if (holder instanceof WordViewHolder) {
            bindWord((WordViewHolder) holder, position);

        } else if (holder instanceof FooterViewHolder) {
            bindFooter((FooterViewHolder) holder);
        }
    }

    private void bindWord(
            WordViewHolder holder,
            int position
    ) {
        if (wordList == null || position >= wordList.size()) {
            return;
        }

        Word currentWord = wordList.get(position);

        String id = currentWord.getId() != null
                ? currentWord.getId()
                : "";

        Log.d("DICTIONARY DEBUG", "DICTIONARY ID: " + id);

        String ilokano = safeText(currentWord.getIlokanoWord());
        String english = safeText(currentWord.getEnglishTranslation());
        String tagalog = safeText(currentWord.getTagalogTranslation());
        String pos = safeText(currentWord.getPartOfSpeech());
        String category = safeText(currentWord.getCategory());

        holder.boundIlokano = ilokano;
        holder.boundEnglish = english;
        holder.boundTagalog = tagalog;

        // Ilokano word and highlighted search matches.
        holder.ilokanoWordText.setText(
                getHighlightedText(ilokano, searchQuery)
        );

        // Part of speech.
        if (!pos.isEmpty()) {
            holder.partOfSpeechText.setVisibility(View.VISIBLE);
            holder.partOfSpeechText.setText(
                    getHighlightedText(pos, searchQuery)
            );
        } else {
            holder.partOfSpeechText.setVisibility(View.GONE);
        }

        // Category.
        if (!category.isEmpty()) {
            holder.categoryText.setVisibility(View.VISIBLE);
            holder.categoryText.setText(
                    getHighlightedText(category, searchQuery)
            );
        } else {
            holder.categoryText.setVisibility(View.GONE);
        }

        // Translation selected by the user.
        String selectedTranslation =
                "Tagalog".equals(selectedLanguage)
                        ? tagalog
                        : english;

        holder.selectedTranslationText.setText(
                getHighlightedText(
                        selectedTranslation,
                        searchQuery
                )
        );

        // Pronunciation button.
        holder.playAudioButton.setOnClickListener(v -> {
            if (playClickListener != null) {
                playClickListener.onPlayClick(currentWord);
            }
        });

        // Favorites button.
        holder.favoriteButton.setText(HEART_EMPTY);
        holder.favoriteButton.setContentDescription(
                "Add " + ilokano + " to Favorites"
        );

        holder.favoriteButton.setOnClickListener(v ->
                toggleFavorite(holder, currentWord)
        );

        // Load the saved favorite state from the local Room database.
        getFavoriteRepository(holder.itemView).isFavorite(
                FavoriteRepository.TYPE_DICTIONARY,
                ilokano,
                english,
                tagalog,
                isSaved -> {
                    if (!isSameBoundWord(
                            holder,
                            ilokano,
                            english,
                            tagalog
                    )) {
                        return;
                    }

                    setHeartState(holder, isSaved, ilokano);
                }
        );

        // Tap the card to open the word details screen.
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(
                    v.getContext(),
                    WordDetailActivity.class
            );

            intent.putExtra("id", currentWord.getId());
            intent.putExtra("ilokano", currentWord.getIlokanoWord());
            intent.putExtra("english", currentWord.getEnglishTranslation());
            intent.putExtra("tagalog", currentWord.getTagalogTranslation());
            intent.putExtra("pos", currentWord.getPartOfSpeech());
            intent.putExtra("category", currentWord.getCategory());
            intent.putExtra("definition", currentWord.getDefinition());
            intent.putExtra("ttsUrl", currentWord.getTtsUrl());

            v.getContext().startActivity(intent);
        });

        // Apply saved accessibility settings to this card.
        // This also covers newly loaded and filtered dictionary entries.
        AccessibilityPreferences.applyToViewTree(
                holder.itemView.getContext(),
                holder.itemView
        );
    }

    private void toggleFavorite(
            WordViewHolder holder,
            Word word
    ) {
        String ilokano = safeText(word.getIlokanoWord());
        String english = safeText(word.getEnglishTranslation());
        String tagalog = safeText(word.getTagalogTranslation());

        FavoriteRepository repository =
                getFavoriteRepository(holder.itemView);

        repository.isFavorite(
                FavoriteRepository.TYPE_DICTIONARY,
                ilokano,
                english,
                tagalog,
                isSaved -> {
                    if (!isSameBoundWord(
                            holder,
                            ilokano,
                            english,
                            tagalog
                    )) {
                        return;
                    }

                    if (isSaved) {
                        removeDictionaryFavorite(
                                holder,
                                repository,
                                ilokano,
                                english,
                                tagalog
                        );
                    } else {
                        repository.saveFavorite(
                                FavoriteRepository.TYPE_DICTIONARY,
                                ilokano,
                                english,
                                tagalog,
                                (success, message) -> {
                                    if (!isSameBoundWord(
                                            holder,
                                            ilokano,
                                            english,
                                            tagalog
                                    )) {
                                        return;
                                    }

                                    if (success) {
                                        setHeartState(
                                                holder,
                                                true,
                                                ilokano
                                        );
                                    }

                                    Toast.makeText(
                                            holder.itemView.getContext(),
                                            message,
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                        );
                    }
                }
        );
    }

    private void removeDictionaryFavorite(
            WordViewHolder holder,
            FavoriteRepository repository,
            String ilokano,
            String english,
            String tagalog
    ) {
        repository.getFavoritesByType(
                FavoriteRepository.TYPE_DICTIONARY,
                favorites -> {
                    if (!isSameBoundWord(
                            holder,
                            ilokano,
                            english,
                            tagalog
                    )) {
                        return;
                    }

                    if (favorites == null) {
                        Toast.makeText(
                                holder.itemView.getContext(),
                                "Could not load Favorites.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    FavoriteEntity match = null;

                    for (FavoriteEntity favorite : favorites) {
                        if (ilokano.equals(favorite.getIlokanoText())
                                && english.equals(favorite.getEnglishText())
                                && tagalog.equals(favorite.getTagalogText())) {
                            match = favorite;
                            break;
                        }
                    }

                    if (match == null) {
                        setHeartState(holder, false, ilokano);

                        Toast.makeText(
                                holder.itemView.getContext(),
                                "Favorite was not found.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    repository.removeFavorite(
                            match.getId(),
                            (success, message) -> {
                                if (!isSameBoundWord(
                                        holder,
                                        ilokano,
                                        english,
                                        tagalog
                                )) {
                                    return;
                                }

                                if (success) {
                                    setHeartState(
                                            holder,
                                            false,
                                            ilokano
                                    );
                                }

                                Toast.makeText(
                                        holder.itemView.getContext(),
                                        message,
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                    );
                }
        );
    }

    private void setHeartState(
            WordViewHolder holder,
            boolean isSaved,
            String ilokano
    ) {
        holder.favoriteButton.setText(
                isSaved ? HEART_FILLED : HEART_EMPTY
        );

        holder.favoriteButton.setContentDescription(
                isSaved
                        ? "Remove " + ilokano + " from Favorites"
                        : "Add " + ilokano + " to Favorites"
        );
    }

    private boolean isSameBoundWord(
            WordViewHolder holder,
            String ilokano,
            String english,
            String tagalog
    ) {
        return ilokano.equals(holder.boundIlokano)
                && english.equals(holder.boundEnglish)
                && tagalog.equals(holder.boundTagalog);
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private void bindFooter(FooterViewHolder holder) {
        if (isLoading) {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.footerText.setVisibility(View.GONE);

        } else if (hasMore && wordList != null && !wordList.isEmpty()) {
            holder.progressBar.setVisibility(View.GONE);
            holder.footerText.setVisibility(View.VISIBLE);
            holder.footerText.setText("Load more");

            holder.footerText.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, 0, 0
            );

            holder.itemView.setOnClickListener(v -> {
                if (loadMoreListener != null) {
                    loadMoreListener.run();
                }
            });

        } else {
            holder.progressBar.setVisibility(View.GONE);
            holder.footerText.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
        }

        // Apply saved accessibility settings to the pagination footer too.
        AccessibilityPreferences.applyToViewTree(
                holder.itemView.getContext(),
                holder.itemView
        );
    }

    @Override
    public int getItemCount() {
        int count = wordList != null ? wordList.size() : 0;
        return count + 1;
    }

    public void filterList(List<Word> filteredList) {
        wordList = filteredList;
        notifyDataSetChanged();
    }

    public void addItems(List<Word> newItems) {
        if (wordList == null || newItems == null || newItems.isEmpty()) {
            return;
        }

        int start = wordList.size();
        wordList.addAll(newItems);

        notifyItemRangeInserted(start, newItems.size());
    }

    private Spannable getHighlightedText(
            String text,
            String query
    ) {
        Spannable spannable = new SpannableString(text);

        if (query != null && !query.isEmpty()) {
            String lowerText = text.toLowerCase();
            int start = lowerText.indexOf(query);

            while (start >= 0) {
                int end = start + query.length();

                spannable.setSpan(
                        new BackgroundColorSpan(Color.YELLOW),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                start = lowerText.indexOf(query, end);
            }
        }

        return spannable;
    }

    public static class WordViewHolder
            extends RecyclerView.ViewHolder {

        public TextView ilokanoWordText;
        public TextView partOfSpeechText;
        public TextView selectedTranslationText;
        public TextView categoryText;
        public TextView favoriteButton;
        public ImageView playAudioButton;

        String boundIlokano = "";
        String boundEnglish = "";
        String boundTagalog = "";

        public WordViewHolder(@NonNull View itemView) {
            super(itemView);

            ilokanoWordText = itemView.findViewById(
                    R.id.textViewIlokano
            );

            partOfSpeechText = itemView.findViewById(
                    R.id.textViewPartOfSpeech
            );

            categoryText = itemView.findViewById(
                    R.id.textViewCategory
            );

            selectedTranslationText = itemView.findViewById(
                    R.id.textViewSelectedTranslation
            );

            favoriteButton = itemView.findViewById(
                    R.id.buttonFavorite
            );

            playAudioButton = itemView.findViewById(
                    R.id.buttonPlayAudio
            );
        }
    }

    static class FooterViewHolder
            extends RecyclerView.ViewHolder {

        ProgressBar progressBar;
        TextView footerText;

        FooterViewHolder(@NonNull View itemView) {
            super(itemView);

            progressBar = itemView.findViewById(
                    R.id.footerProgress
            );

            footerText = itemView.findViewById(
                    R.id.footerText
            );
        }
    }
}
