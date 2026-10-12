package com.example.speaksmartbaguio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.speaksmartbaguio.entity.FavoriteEntity;
import com.example.speaksmartbaguio.repository.FavoriteRepository;
import com.example.speaksmartbaguio.utils.AccessibilityPreferences;

import java.util.ArrayList;
import java.util.List;

public class FavoritesAdapter
        extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {

    public interface OnFavoriteRemovedListener {
        void onFavoriteRemoved();
    }

    private final List<FavoriteEntity> favorites = new ArrayList<>();
    private final FavoriteRepository repository;
    private final OnFavoriteRemovedListener removeListener;

    public FavoritesAdapter(
            FavoriteRepository repository,
            OnFavoriteRemovedListener removeListener
    ) {
        this.repository = repository;
        this.removeListener = removeListener;
    }

    public void setFavorites(List<FavoriteEntity> newFavorites) {
        favorites.clear();

        if (newFavorites != null) {
            favorites.addAll(newFavorites);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_favorite_card,
                parent,
                false
        );

        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull FavoriteViewHolder holder,
            int position
    ) {
        FavoriteEntity favorite = favorites.get(position);

        String ilokano = safeText(favorite.getIlokanoText());
        String english = safeText(favorite.getEnglishText());
        String tagalog = safeText(favorite.getTagalogText());

        boolean isDictionary =
                FavoriteRepository.TYPE_DICTIONARY.equals(
                        favorite.getType()
                );

        holder.textFavoriteType.setText(
                isDictionary ? "DICTIONARY" : "PHRASEBOOK"
        );

        holder.textFavoriteIlokano.setText(ilokano);

        holder.textFavoriteEnglish.setText(
                "English: " + (english.isEmpty() ? "—" : english)
        );

        holder.textFavoriteTagalog.setText(
                "Tagalog: " + (tagalog.isEmpty() ? "—" : tagalog)
        );

        holder.buttonRemoveFavorite.setContentDescription(
                "Remove " + ilokano + " from Favorites"
        );

        holder.buttonRemoveFavorite.setOnClickListener(v -> {
            holder.buttonRemoveFavorite.setEnabled(false);

            repository.removeFavorite(
                    favorite.getId(),
                    (success, message) -> {
                        // The repository callback may run asynchronously.
                        // Update the view only while its holder is attached.
                        holder.buttonRemoveFavorite.setEnabled(true);

                        Toast.makeText(
                                v.getContext(),
                                message,
                                Toast.LENGTH_SHORT
                        ).show();

                        if (success && removeListener != null) {
                            removeListener.onFavoriteRemoved();
                        }
                    }
            );
        });

        // Apply saved font-size, contrast, and layout preferences
        // to this dynamically created favorite card.
        AccessibilityPreferences.applyToViewTree(
                holder.itemView.getContext(),
                holder.itemView
        );
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {

        TextView textFavoriteType;
        TextView textFavoriteIlokano;
        TextView textFavoriteEnglish;
        TextView textFavoriteTagalog;
        TextView buttonRemoveFavorite;

        FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);

            textFavoriteType = itemView.findViewById(
                    R.id.textFavoriteType
            );

            textFavoriteIlokano = itemView.findViewById(
                    R.id.textFavoriteIlokano
            );

            textFavoriteEnglish = itemView.findViewById(
                    R.id.textFavoriteEnglish
            );

            textFavoriteTagalog = itemView.findViewById(
                    R.id.textFavoriteTagalog
            );

            buttonRemoveFavorite = itemView.findViewById(
                    R.id.buttonRemoveFavorite
            );
        }
    }
}
