package com.example.speaksmartbaguio;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DictionaryAdapter extends RecyclerView.Adapter<DictionaryAdapter.WordViewHolder> {

    private List<Word> wordList;
    private OnPlayClickListener playClickListener;
    private String searchQuery = "";
    private String selectedLanguage = "English";

    public interface OnPlayClickListener {
        void onPlayClick(Word word);
    }

    public DictionaryAdapter(List<Word> wordList, OnPlayClickListener listener, String selectedLanguage) {
        this.wordList = wordList;
        this.playClickListener = listener;
        this.selectedLanguage = selectedLanguage;
    }

    /** Pass the current search query for highlighting */
    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.toLowerCase() : "";
    }

    /** Update selected language dynamically */
    public void setSelectedLanguage(String language) {
        this.selectedLanguage = language;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_word, parent, false);
        return new WordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
        Word currentWord = wordList.get(position);

        String ilokano = currentWord.getIlokanoWord() != null ? currentWord.getIlokanoWord() : "";
        String english = currentWord.getEnglishTranslation() != null ? currentWord.getEnglishTranslation() : "";
        String tagalog = currentWord.getTagalogTranslation() != null ? currentWord.getTagalogTranslation() : "";
        String pos = currentWord.getPartOfSpeech() != null ? currentWord.getPartOfSpeech() : "";

        // Highlight search query in Ilokano
        holder.ilokanoWordText.setText(getHighlightedText(ilokano, searchQuery));

        // Show Part of Speech
        holder.partOfSpeechText.setText(getHighlightedText(pos, searchQuery));

        // Show selected language translation
        String selectedTranslation = selectedLanguage.equals("Tagalog") ? tagalog : english;
        holder.selectedTranslationText.setText(getHighlightedText(selectedTranslation, searchQuery));

        // Play audio click
        holder.playAudioButton.setOnClickListener(v -> {
            if (playClickListener != null) {
                playClickListener.onPlayClick(currentWord);
            }
        });
    }

    @Override
    public int getItemCount() {
        return wordList != null ? wordList.size() : 0;
    }

    /** Update the list after filtering */
    public void filterList(List<Word> filteredList) {
        wordList = filteredList;
        notifyDataSetChanged();
    }

    /** Highlight occurrences of search query */
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
}
