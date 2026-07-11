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

public class PhrasebookAdapter extends RecyclerView.Adapter<PhrasebookAdapter.PhraseViewHolder> {

    private List<Phrase> items;
    private OnPhraseClickListener clickListener;
    private String searchQuery = "";
    private String selectedLanguage = "English";

    public interface OnPhraseClickListener {
        void onPlayClicked(Phrase phrase);
    }

    public PhrasebookAdapter(List<Phrase> items, OnPhraseClickListener listener, String selectedLanguage) {
        this.items = items;
        this.clickListener = listener;
        this.selectedLanguage = selectedLanguage;
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.toLowerCase() : "";
    }

    public void setSelectedLanguage(String language) {
        this.selectedLanguage = language;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhraseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_phrase, parent, false);
        return new PhraseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhraseViewHolder holder, int position) {
        Phrase phrase = items.get(position);

        String ilokano = phrase.getIlokanoWord() != null ? phrase.getIlokanoWord() : "";
        String translation = "Tagalog".equals(selectedLanguage)
                ? phrase.getTagalogTranslation() : phrase.getEnglishTranslation();
        translation = translation != null ? translation : "";

        // Highlight search query
        holder.textViewIlokano.setText(getHighlightedText(ilokano, searchQuery));
        holder.textViewSelected.setText(getHighlightedText(translation, searchQuery));

        holder.buttonPlay.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPlayClicked(phrase);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void filterList(List<Phrase> filteredList) {
        items = filteredList;
        notifyDataSetChanged();
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
        TextView textViewIlokano, textViewSelected;
        ImageView buttonPlay;

        PhraseViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewIlokano = itemView.findViewById(R.id.textViewIlokano);
            textViewSelected = itemView.findViewById(R.id.textViewSelectedTranslation);
            buttonPlay = itemView.findViewById(R.id.buttonPlayAudio);
        }
    }
}
