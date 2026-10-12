package com.example.speaksmartbaguio;

import android.content.Intent;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.speaksmartbaguio.entity.FavoriteEntity;
import com.example.speaksmartbaguio.repository.FavoriteRepository;
import com.example.speaksmartbaguio.utils.AccessibilityPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PhrasebookAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;

    private static final String HEART_EMPTY = "♡";
    private static final String HEART_FILLED = "♥";

    private List<Phrase> items;

    private final OnPhraseClickListener clickListener;

    private String searchQuery = "";
    private String selectedLanguage = "English";

    private boolean isLoading = false;
    private boolean hasMore = true;

    private Runnable loadMoreListener;

    private FavoriteRepository favoriteRepository;

    public interface OnPhraseClickListener {
        void onPlayClicked(Phrase phrase);
    }

    public PhrasebookAdapter(
            List<Phrase> items,
            OnPhraseClickListener listener,
            String selectedLanguage
    ) {
        this.items = items != null ? items : new ArrayList<>();
        this.clickListener = listener;
        this.selectedLanguage =
                selectedLanguage != null
                        ? selectedLanguage
                        : "English";
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
        searchQuery = query == null
                ? ""
                : query.trim().toLowerCase(Locale.ROOT);

        notifyDataSetChanged();
    }

    public void setSelectedLanguage(String language) {
        selectedLanguage =
                language == null || language.trim().isEmpty()
                        ? "English"
                        : language;

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
        loadMoreListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return position == items.size()
                ? TYPE_FOOTER
                : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        LayoutInflater inflater =
                LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_FOOTER) {
            View view = inflater.inflate(
                    R.layout.item_pagination_footer,
                    parent,
                    false
            );

            return new FooterViewHolder(view);
        }

        View view = inflater.inflate(
                R.layout.item_phrasebook_card,
                parent,
                false
        );

        return new PhraseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {
        if (holder instanceof PhraseViewHolder) {
            bindPhrase((PhraseViewHolder) holder, position);

        } else if (holder instanceof FooterViewHolder) {
            bindFooter((FooterViewHolder) holder);
        }
    }

    // ---------------------------------------------------------
    // PHRASE CARD
    // ---------------------------------------------------------

    private void bindPhrase(
            PhraseViewHolder holder,
            int position
    ) {
        if (position < 0 || position >= items.size()) {
            return;
        }

        Phrase phrase = items.get(position);

        if (phrase == null) {
            return;
        }

        String ilokano = safeText(phrase.getIlokanoWord());
        String english = safeText(phrase.getEnglishTranslation());
        String tagalog = safeText(phrase.getTagalogTranslation());
        String partOfSpeech = safeText(phrase.getPartOfSpeech());

        holder.boundIlokano = ilokano;
        holder.boundEnglish = english;
        holder.boundTagalog = tagalog;

        String translation =
                "Tagalog".equalsIgnoreCase(selectedLanguage)
                        ? tagalog
                        : english;

        holder.textViewIlokano.setText(
                getHighlightedText(ilokano, searchQuery)
        );

        holder.textViewSelected.setText(
                getHighlightedText(translation, searchQuery)
        );

        if (partOfSpeech.trim().isEmpty()) {
            holder.textViewPartOfSpeech.setVisibility(View.GONE);
        } else {
            holder.textViewPartOfSpeech.setVisibility(View.VISIBLE);

            holder.textViewPartOfSpeech.setText(
                    getHighlightedText(partOfSpeech, searchQuery)
            );
        }

        // Open the phrase details screen.
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(
                    v.getContext(),
                    PhraseDetailActivity.class
            );

            intent.putExtra("id", phrase.getId());
            intent.putExtra("ilokano", ilokano);
            intent.putExtra("english", english);
            intent.putExtra("tagalog", tagalog);
            intent.putExtra("pos", partOfSpeech);

            intent.putExtra(
                    "category",
                    safeText(phrase.getCategory())
            );

            intent.putExtra(
                    "englishSentence",
                    safeText(phrase.getEnglishSentence())
            );

            intent.putExtra(
                    "sentence",
                    safeText(phrase.getIlokanoSentence())
            );

            v.getContext().startActivity(intent);
        });

        // Play the phrase pronunciation.
        holder.buttonPlayAudio.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPlayClicked(phrase);
            }
        });

        holder.buttonPlayAudio.setContentDescription(
                "Play pronunciation for " + ilokano
        );

        // Show the default heart while checking the database.
        setHeartState(holder, false, ilokano);

        holder.buttonFavorite.setOnClickListener(v ->
                toggleFavorite(
                        holder,
                        ilokano,
                        english,
                        tagalog
                )
        );

        // Read the saved favorite state from Room.
        getFavoriteRepository(holder.itemView).isFavorite(
                FavoriteRepository.TYPE_PHRASEBOOK,
                ilokano,
                english,
                tagalog,
                isSaved -> {
                    if (!isSameBoundPhrase(
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

        // Apply the saved settings to this dynamically created card.
        AccessibilityPreferences.applyToViewTree(
                holder.itemView.getContext(),
                holder.itemView
        );
    }

    // ---------------------------------------------------------
    // FAVORITES
    // ---------------------------------------------------------

    private void toggleFavorite(
            PhraseViewHolder holder,
            String ilokano,
            String english,
            String tagalog
    ) {
        FavoriteRepository repository =
                getFavoriteRepository(holder.itemView);

        repository.isFavorite(
                FavoriteRepository.TYPE_PHRASEBOOK,
                ilokano,
                english,
                tagalog,
                isSaved -> {
                    if (!isSameBoundPhrase(
                            holder,
                            ilokano,
                            english,
                            tagalog
                    )) {
                        return;
                    }

                    if (isSaved) {
                        removeFavorite(
                                holder,
                                repository,
                                ilokano,
                                english,
                                tagalog
                        );

                    } else {
                        repository.saveFavorite(
                                FavoriteRepository.TYPE_PHRASEBOOK,
                                ilokano,
                                english,
                                tagalog,
                                (success, message) -> {
                                    if (!isSameBoundPhrase(
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

    private void removeFavorite(
            PhraseViewHolder holder,
            FavoriteRepository repository,
            String ilokano,
            String english,
            String tagalog
    ) {
        repository.getFavoritesByType(
                FavoriteRepository.TYPE_PHRASEBOOK,
                favorites -> {
                    if (!isSameBoundPhrase(
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
                                if (!isSameBoundPhrase(
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
            PhraseViewHolder holder,
            boolean isSaved,
            String ilokano
    ) {
        holder.buttonFavorite.setText(
                isSaved ? HEART_FILLED : HEART_EMPTY
        );

        holder.buttonFavorite.setContentDescription(
                isSaved
                        ? "Remove " + ilokano + " from Favorites"
                        : "Add " + ilokano + " to Favorites"
        );
    }

    private boolean isSameBoundPhrase(
            PhraseViewHolder holder,
            String ilokano,
            String english,
            String tagalog
    ) {
        return ilokano.equals(holder.boundIlokano)
                && english.equals(holder.boundEnglish)
                && tagalog.equals(holder.boundTagalog);
    }

    // ---------------------------------------------------------
    // PAGINATION FOOTER
    // ---------------------------------------------------------

    private void bindFooter(FooterViewHolder holder) {
        if (isLoading) {
            holder.footerProgress.setVisibility(View.VISIBLE);
            holder.footerText.setVisibility(View.GONE);
            holder.footerText.setOnClickListener(null);

        } else {
            holder.footerProgress.setVisibility(View.GONE);

            if (hasMore && !items.isEmpty()) {
                holder.footerText.setVisibility(View.VISIBLE);
                holder.footerText.setText("Load more phrases");

                holder.footerText.setOnClickListener(v -> {
                    if (loadMoreListener != null && !isLoading) {
                        loadMoreListener.run();
                    }
                });

            } else {
                holder.footerText.setVisibility(View.GONE);
                holder.footerText.setOnClickListener(null);
            }
        }

        // Apply accessibility to the footer as well.
        AccessibilityPreferences.applyToViewTree(
                holder.itemView.getContext(),
                holder.itemView
        );
    }

    // ---------------------------------------------------------
    // ADAPTER DATA
    // ---------------------------------------------------------

    @Override
    public int getItemCount() {
        return items.size() + 1;
    }

    public void filterList(List<Phrase> filteredList) {
        items = filteredList != null
                ? filteredList
                : new ArrayList<>();

        notifyDataSetChanged();
    }

    public void addItems(List<Phrase> newItems) {
        if (newItems == null || newItems.isEmpty()) {
            return;
        }

        int startPosition = items.size();
        items.addAll(newItems);

        notifyItemRangeInserted(
                startPosition,
                newItems.size()
        );
    }

    // ---------------------------------------------------------
    // SEARCH HIGHLIGHTING
    // ---------------------------------------------------------

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private SpannableString getHighlightedText(
            String text,
            String query
    ) {
        String safeValue = safeText(text);

        SpannableString spannable =
                new SpannableString(safeValue);

        if (query == null || query.trim().isEmpty()
                || safeValue.isEmpty()) {
            return spannable;
        }

        String lowerText = safeValue.toLowerCase(Locale.ROOT);
        String lowerQuery = query.toLowerCase(Locale.ROOT);

        int start = 0;

        int highlightBackground = Color.rgb(42, 157, 147);
        int highlightText = Color.WHITE;

        while (true) {
            int index = lowerText.indexOf(lowerQuery, start);

            if (index < 0) {
                break;
            }

            int end = index + lowerQuery.length();

            spannable.setSpan(
                    new BackgroundColorSpan(highlightBackground),
                    index,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            spannable.setSpan(
                    new ForegroundColorSpan(highlightText),
                    index,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            start = end;
        }

        return spannable;
    }

    // ---------------------------------------------------------
    // VIEW HOLDERS
    // ---------------------------------------------------------

    private static class PhraseViewHolder
            extends RecyclerView.ViewHolder {

        TextView textViewIlokano;
        TextView textViewSelected;
        TextView textViewPartOfSpeech;
        TextView buttonFavorite;
        ImageButton buttonPlayAudio;

        String boundIlokano = "";
        String boundEnglish = "";
        String boundTagalog = "";

        PhraseViewHolder(@NonNull View itemView) {
            super(itemView);

            textViewIlokano = itemView.findViewById(
                    R.id.textViewIlokano
            );

            textViewSelected = itemView.findViewById(
                    R.id.textViewSelectedTranslation
            );

            textViewPartOfSpeech = itemView.findViewById(
                    R.id.textViewPartOfSpeech
            );

            buttonFavorite = itemView.findViewById(
                    R.id.buttonFavorite
            );

            buttonPlayAudio = itemView.findViewById(
                    R.id.buttonPlayAudio
            );
        }
    }

    private static class FooterViewHolder
            extends RecyclerView.ViewHolder {

        ProgressBar footerProgress;
        TextView footerText;

        FooterViewHolder(@NonNull View itemView) {
            super(itemView);

            footerProgress = itemView.findViewById(
                    R.id.footerProgress
            );

            footerText = itemView.findViewById(
                    R.id.footerText
            );
        }
    }
}
