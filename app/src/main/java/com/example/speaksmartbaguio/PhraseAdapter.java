package com.example.speaksmartbaguio;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PhraseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;

    private List<Phrase> items;
    private OnPhraseClickListener clickListener;
    private String searchQuery = "";
    private String selectedLanguage = "English";

    private boolean isLoading = false;
    private boolean hasMore = true;
    private Runnable loadMoreListener;

    public interface OnPhraseClickListener {
        void onPlayClicked(Phrase phrase);
    }

    public PhraseAdapter(List<Phrase> items, OnPhraseClickListener listener, String selectedLanguage) {
        this.items = items;
        this.clickListener = listener;
        this.selectedLanguage = selectedLanguage != null ? selectedLanguage : "English";
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.toLowerCase() : "";
    }

    public void setSelectedLanguage(String language) {
        this.selectedLanguage = language != null ? language : "English";
        notifyDataSetChanged();
    }

    public void setPaginationState(boolean isLoading, boolean hasMore) {
        this.isLoading = isLoading;
        this.hasMore = hasMore;
        notifyItemChanged(getItemCount() - 1);
    }

    public void setOnLoadMoreListener(Runnable listener) {
        this.loadMoreListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == items.size()) return TYPE_FOOTER;
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pagination_footer, parent, false);
            return new FooterViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_card, parent, false);
        return new PhraseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PhraseViewHolder) {
            bindPhrase((PhraseViewHolder) holder, position);
        } else if (holder instanceof FooterViewHolder) {
            bindFooter((FooterViewHolder) holder);
        }
    }

    private void bindPhrase(PhraseViewHolder holder, int position) {
        Phrase phrase = items.get(position);

        String ilokano = phrase.getIlokanoWord() != null ? phrase.getIlokanoWord() : "";
        String targetText = "Tagalog".equals(selectedLanguage)
                ? phrase.getTagalogTranslation()
                : phrase.getEnglishTranslation();
        targetText = targetText != null ? targetText : "";

        holder.textViewIlokano.setText(getHighlightedText(ilokano, searchQuery));
        holder.textViewSelectedTranslation.setText(getHighlightedText(targetText, searchQuery));

        holder.buttonPlay.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPlayClicked(phrase);
            }
        });
    }

    private void bindFooter(FooterViewHolder holder) {
        if (isLoading) {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.footerText.setVisibility(View.GONE);
        } else if (hasMore && items.size() > 0) {
            holder.progressBar.setVisibility(View.GONE);
            holder.footerText.setVisibility(View.VISIBLE);
            holder.footerText.setText("Load more");
            holder.footerText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            holder.itemView.setOnClickListener(v -> {
                if (loadMoreListener != null) loadMoreListener.run();
            });
        } else {
            holder.progressBar.setVisibility(View.GONE);
            holder.footerText.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        int count = items != null ? items.size() : 0;
        return count + 1;
    }

    public void filterList(List<Phrase> filteredList) {
        items = filteredList;
        notifyDataSetChanged();
    }

    public void addItems(List<Phrase> newItems) {
        int start = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
    }

    private Spannable getHighlightedText(String text, String query) {
        Spannable spannable = new SpannableString(text);
        if (query != null && !query.isEmpty()) {
            String lowerText = text.toLowerCase();
            int start = lowerText.indexOf(query);
            while (start >= 0) {
                int end = start + query.length();
                spannable.setSpan(new BackgroundColorSpan(Color.YELLOW),
                        start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                start = lowerText.indexOf(query, end);
            }
        }
        return spannable;
    }

    static class PhraseViewHolder extends RecyclerView.ViewHolder {
        TextView textViewIlokano, textViewSelectedTranslation;
        ImageView buttonPlay;

        PhraseViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewIlokano = itemView.findViewById(R.id.textViewIlokano);
            textViewSelectedTranslation = itemView.findViewById(R.id.textViewSelectedTranslation);
            buttonPlay = itemView.findViewById(R.id.buttonPlayAudio);
        }
    }

    static class FooterViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;
        TextView footerText;

        FooterViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.footerProgress);
            footerText = itemView.findViewById(R.id.footerText);
        }
    }
}
