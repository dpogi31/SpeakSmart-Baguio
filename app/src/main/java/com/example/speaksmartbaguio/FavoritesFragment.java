package com.example.speaksmartbaguio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.speaksmartbaguio.entity.FavoriteEntity;
import com.example.speaksmartbaguio.repository.FavoriteRepository;
import com.example.speaksmartbaguio.utils.AccessibilityPreferences;
import com.example.speaksmartbaguio.utils.SettingsMenuHelper;

import java.util.List;

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerViewFavorites;
    private LinearLayout layoutFavoritesEmpty;
    private ProgressBar progressFavorites;
    private TextView textFavoritesCount;

    private FavoriteRepository favoriteRepository;
    private FavoritesAdapter favoritesAdapter;

    private boolean isLoadingFavorites = false;

    public FavoritesFragment() {
        // Required empty public constructor.
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_favorites,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        recyclerViewFavorites = view.findViewById(
                R.id.recyclerViewFavorites
        );

        layoutFavoritesEmpty = view.findViewById(
                R.id.layoutFavoritesEmpty
        );

        progressFavorites = view.findViewById(
                R.id.progressFavorites
        );

        textFavoritesCount = view.findViewById(
                R.id.textFavoritesCount
        );

        // Connect the shared Settings popup to the gear button.
        View settingsButton = view.findViewById(
                R.id.buttonSettingsMenu
        );

        if (settingsButton != null) {
            settingsButton.setOnClickListener(
                    v -> SettingsMenuHelper.show(v)
            );
        }

        // Apply saved accessibility preferences to this screen.
        AccessibilityPreferences.applyToViewTree(
                requireContext(),
                view
        );

        favoriteRepository = new FavoriteRepository(
                requireContext().getApplicationContext()
        );

        favoritesAdapter = new FavoritesAdapter(
                favoriteRepository,
                this::loadFavorites
        );

        recyclerViewFavorites.setAdapter(favoritesAdapter);

        loadFavorites();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh whenever the user returns to Favorites.
        if (favoriteRepository != null) {
            loadFavorites();
        }
    }

    // ---------------------------------------------------------
    // LOAD FAVORITES
    // ---------------------------------------------------------

    private void loadFavorites() {
        if (favoriteRepository == null || isLoadingFavorites) {
            return;
        }

        isLoadingFavorites = true;

        if (progressFavorites != null) {
            progressFavorites.setVisibility(View.VISIBLE);
        }

        favoriteRepository.getAllFavorites(favorites -> {

            // Do not update a fragment whose view has been destroyed.
            if (!isAdded() || getView() == null) {
                isLoadingFavorites = false;
                return;
            }

            isLoadingFavorites = false;

            if (progressFavorites != null) {
                progressFavorites.setVisibility(View.GONE);
            }

            if (favorites == null) {
                Toast.makeText(
                        requireContext(),
                        "Unable to load Favorites. Please try again.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            displayFavorites(favorites);
        });
    }

    // ---------------------------------------------------------
    // DISPLAY FAVORITES
    // ---------------------------------------------------------

    private void displayFavorites(
            List<FavoriteEntity> favorites
    ) {
        if (favoritesAdapter == null
                || textFavoritesCount == null
                || layoutFavoritesEmpty == null
                || recyclerViewFavorites == null
                || favorites == null) {
            return;
        }

        favoritesAdapter.setFavorites(favorites);

        int count = favorites.size();

        textFavoritesCount.setText(
                count == 1
                        ? "1 saved item"
                        : count + " saved items"
        );

        boolean isEmpty = favorites.isEmpty();

        layoutFavoritesEmpty.setVisibility(
                isEmpty ? View.VISIBLE : View.GONE
        );

        recyclerViewFavorites.setVisibility(
                isEmpty ? View.GONE : View.VISIBLE
        );

        // Reapply accessibility preferences after updating the list.
        View currentView = getView();

        if (currentView != null && isAdded()) {
            AccessibilityPreferences.applyToViewTree(
                    requireContext(),
                    currentView
            );
        }
    }

    // ---------------------------------------------------------
    // CLEANUP
    // ---------------------------------------------------------

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        recyclerViewFavorites = null;
        layoutFavoritesEmpty = null;
        progressFavorites = null;
        textFavoritesCount = null;
        favoritesAdapter = null;

        isLoadingFavorites = false;
    }
}
