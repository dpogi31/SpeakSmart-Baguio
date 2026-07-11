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

public class DictionaryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;

    private List<Word> wordList;
    private OnPlayClickListener playClickListener;
    private String searchQuery = "";
    private String selectedLanguage = "English";

    private boolean isLoading = false;
    private boolean hasMore = true;
    private Runnable loadMoreListener;

    public interface OnPlayClickListener {
        void onPlayClick(Word word);
    }

    public DictionaryAdapter(List<Word> wordList, OnPlayClickListener listener, String selectedLanguage) {
        this.wordList = wordList;
        this.playClickListener = listener;
        this.selectedLanguage = selectedLanguage;
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.toLowerCase() : "";
    }

    public void setSelectedLanguage(String language) {
        this.selectedLanguage = language;
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
        if (position == wordList.size()) return TYPE_FOOTER;
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
        return new WordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof WordViewHolder) {
            bindWord((WordViewHolder) holder, position);
        } else if (holder instanceof FooterViewHolder) {
            bindFooter((FooterViewHolder) holder);
        }
    }

    private void bindWord(WordViewHolder holder, int position) {
        Word currentWord = wordList.get(position);

        String ilokano = currentWord.getIlokanoWord() != null ? currentWord.getIlokanoWord() : "";
        String english = currentWord.getEnglishTranslation() != null ? currentWord.getEnglishTranslation() : "";
        String tagalog = currentWord.getTagalogTranslation() != null ? currentWord.getTagalogTranslation() : "";
        String pos = currentWord.getPartOfSpeech() != null ? currentWord.getPartOfSpeech() : "";

        holder.ilokanoWordText.setText(getHighlightedText(ilokano, searchQuery));
        if (!pos.isEmpty()) {
            holder.partOfSpeechText.setVisibility(View.VISIBLE);
            holder.partOfSpeechText.setText(getHighlightedText(pos, searchQuery));
        } else {
            holder.partOfSpeechText.setVisibility(View.GONE);
        }
        String selectedTranslation = selectedLanguage.equals("Tagalog") ? tagalog : english;
        holder.selectedTranslationText.setText(getHighlightedText(selectedTranslation, searchQuery));

        holder.playAudioButton.setOnClickListener(v -> {
            if (playClickListener != null) {
                playClickListener.onPlayClick(currentWord);
            }
        });
    }

    private void bindFooter(FooterViewHolder holder) {
        if (isLoading) {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.footerText.setVisibility(View.GONE);
        } else if (hasMore && wordList.size() > 0) {
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
        int count = wordList != null ? wordList.size() : 0;
        return count + 1;
    }

    public void filterList(List<Word> filteredList) {
        wordList = filteredList;
        notifyDataSetChanged();
    }

    public void addItems(List<Word> newItems) {
        int start = wordList.size();
        wordList.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
    }

    private Spannable getHighlightedText(String text, String query) {
        Spannable spannable = new SpannableString(text);
        if (query != null && !query.isEmpty()) {
            String lowerText = text.toLowerCase();
            int start = lowerText.indexOf(query);
            while (start >= 0) {
                int end = start + query.length();
                spannable.setSpan(
                        new BackgroundColorSpan(Color.YELLOW),
                        start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                start = lowerText.indexOf(query, end);
            }
        }
        return spannable;
    }

    public static class WordViewHolder extends RecyclerView.ViewHolder {
        public TextView ilokanoWordText;
        public TextView partOfSpeechText;
        public TextView selectedTranslationText;
        public ImageView playAudioButton;

        public WordViewHolder(@NonNull View itemView) {
            super(itemView);
            ilokanoWordText = itemView.findViewById(R.id.textViewIlokano);
            partOfSpeechText = itemView.findViewById(R.id.textViewPartOfSpeech);
            selectedTranslationText = itemView.findViewById(R.id.textViewSelectedTranslation);
            playAudioButton = itemView.findViewById(R.id.buttonPlayAudio);
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
