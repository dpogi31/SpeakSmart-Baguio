package com.example.speaksmartbaguio;

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

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PhrasebookAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;

    // Phrase data displayed in the RecyclerView.
    private List<Phrase> items;

    // Handles the pronunciation button.
    private final OnPhraseClickListener clickListener;

    // Search and language settings.
    private String searchQuery = "";
    private String selectedLanguage = "English";

    // Pagination settings.
    private boolean isLoading = false;
    private boolean hasMore = true;

    private Runnable loadMoreListener;

    // Callback for pronunciation playback.
    public interface OnPhraseClickListener {
        void onPlayClicked(Phrase phrase);
    }

    // Constructor.
    public PhrasebookAdapter(
            List<Phrase> items,
            OnPhraseClickListener listener,
            String selectedLanguage
    ) {
        this.items = items != null ? items : new ArrayList<>();
        this.clickListener = listener;
        this.selectedLanguage =
                selectedLanguage != null ? selectedLanguage : "English";
    }

    // Update the search query used for highlighting.
    public void setSearchQuery(String query) {
        this.searchQuery = query == null
                ? ""
                : query.trim().toLowerCase(Locale.ROOT);

        notifyDataSetChanged();
    }

    // Update the language shown on each phrase card.
    public void setSelectedLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            selectedLanguage = "English";
        } else {
            selectedLanguage = language;
        }

        notifyDataSetChanged();
    }

    // Update the pagination footer.
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

    // Set the action used to load another page.
    public void setOnLoadMoreListener(Runnable listener) {
        this.loadMoreListener = listener;
    }

    // Identify whether the current row is a phrase or footer.
    @Override
    public int getItemViewType(int position) {
        if (position == items.size()) {
            return TYPE_FOOTER;
        }

        return TYPE_ITEM;
    }

    // Create the correct layout for each row.
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

        // Uses the NEW Phrasebook-only card.
        // The Dictionary's item_list_card.xml remains unchanged.
        View view = inflater.inflate(
                R.layout.item_phrasebook_card,
                parent,
                false
        );

        return new PhraseViewHolder(view);
    }

    // Bind data to the appropriate row.
    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {
        if (holder instanceof PhraseViewHolder) {
            bindPhrase(
                    (PhraseViewHolder) holder,
                    position
            );
        } else if (holder instanceof FooterViewHolder) {
            bindFooter((FooterViewHolder) holder);
        }
    }

    // Display phrase information.
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

        String translation;

        if ("Tagalog".equalsIgnoreCase(selectedLanguage)) {
            translation = safeText(
                    phrase.getTagalogTranslation()
            );
        } else {
            translation = safeText(
                    phrase.getEnglishTranslation()
            );
        }

        String partOfSpeech = safeText(
                phrase.getPartOfSpeech()
        );

        // Ilokano phrase.
        holder.textViewIlokano.setText(
                getHighlightedText(ilokano, searchQuery)
        );

        // Translation according to the selected language.
        holder.textViewSelected.setText(
                getHighlightedText(translation, searchQuery)
        );

        // Show part of speech only when available.
        if (partOfSpeech.trim().isEmpty()) {
            holder.textViewPartOfSpeech.setVisibility(View.GONE);
        } else {
            holder.textViewPartOfSpeech.setVisibility(View.VISIBLE);

            holder.textViewPartOfSpeech.setText(
                    getHighlightedText(
                            partOfSpeech,
                            searchQuery
                    )
            );
        }

        // Clicking the card opens the phrase details.
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(
                    v.getContext(),
                    PhraseDetailActivity.class
            );

            intent.putExtra("id", phrase.getId());
            intent.putExtra("ilokano", ilokano);
            intent.putExtra(
                    "english",
                    safeText(phrase.getEnglishTranslation())
            );
            intent.putExtra(
                    "tagalog",
                    safeText(phrase.getTagalogTranslation())
            );
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

        // Speaker button plays the phrase pronunciation.
        holder.buttonPlayAudio.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPlayClicked(phrase);
            }
        });

        holder.buttonPlayAudio.setContentDescription(
                "Play pronunciation for " + ilokano
        );
    }

    // Display pagination information.
    private void bindFooter(FooterViewHolder holder) {
        if (isLoading) {
            holder.footerProgress.setVisibility(View.VISIBLE);
            holder.footerText.setVisibility(View.GONE);

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
    }

    // Number of phrase rows plus one pagination footer.
    @Override
    public int getItemCount() {
        return items.size() + 1;
    }

    // Replace the currently displayed phrase list.
    public void filterList(List<Phrase> filteredList) {
        items = filteredList != null
                ? filteredList
                : new ArrayList<>();

        notifyDataSetChanged();
    }

    // Append phrases received from the next page.
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

    // Safely convert null text into an empty string.
    private String safeText(String value) {
        return value == null ? "" : value;
    }

    // Highlight matching search text with a readable teal background.
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

        // Baguio Frost teal highlight.
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

    // Phrase card holder.
    private static class PhraseViewHolder
            extends RecyclerView.ViewHolder {

        TextView textViewIlokano;
        TextView textViewSelected;
        TextView textViewPartOfSpeech;
        ImageButton buttonPlayAudio;

        PhraseViewHolder(@NonNull View itemView) {
            super(itemView);

            textViewIlokano =
                    itemView.findViewById(R.id.textViewIlokano);

            textViewSelected =
                    itemView.findViewById(
                            R.id.textViewSelectedTranslation
                    );

            textViewPartOfSpeech =
                    itemView.findViewById(
                            R.id.textViewPartOfSpeech
                    );

            buttonPlayAudio =
                    itemView.findViewById(R.id.buttonPlayAudio);
        }
    }

    // Pagination footer holder.
    private static class FooterViewHolder
            extends RecyclerView.ViewHolder {

        ProgressBar footerProgress;
        TextView footerText;

        FooterViewHolder(@NonNull View itemView) {
            super(itemView);

            footerProgress =
                    itemView.findViewById(R.id.footerProgress);

            footerText =
                    itemView.findViewById(R.id.footerText);
        }
    }
}
