
package com.example.speaksmartbaguio.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.speaksmartbaguio.dao.FavoriteDao;
import com.example.speaksmartbaguio.database.DatabaseClient;
import com.example.speaksmartbaguio.entity.FavoriteEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoriteRepository {

    public static final String TYPE_DICTIONARY = "DICTIONARY";
    public static final String TYPE_PHRASEBOOK = "PHRASEBOOK";

    private final FavoriteDao favoriteDao;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public interface ResultCallback<T> {
        void onComplete(T result);
    }

    public interface ActionCallback {
        void onComplete(boolean success, String message);
    }

    public FavoriteRepository(Context context) {

        Context appContext = context.getApplicationContext();

        favoriteDao = DatabaseClient
                .getInstance(appContext)
                .favoriteDao();

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Save a Dictionary word or Phrasebook phrase.
     *
     * The type should be TYPE_DICTIONARY or TYPE_PHRASEBOOK.
     */
    public void saveFavorite(
            String type,
            String ilokanoText,
            String englishText,
            String tagalogText,
            ActionCallback callback
    ) {
        executorService.execute(() -> {
            try {
                String safeType = clean(type);
                String safeIlokano = clean(ilokanoText);
                String safeEnglish = clean(englishText);
                String safeTagalog = clean(tagalogText);

                if (!TYPE_DICTIONARY.equals(safeType)
                        && !TYPE_PHRASEBOOK.equals(safeType)) {
                    postAction(callback, false, "Invalid favorite type.");
                    return;
                }

                if (safeIlokano.isEmpty()) {
                    postAction(callback, false, "Ilokano text cannot be empty.");
                    return;
                }

                int existing = favoriteDao.isFavorite(
                        safeType,
                        safeIlokano,
                        safeEnglish,
                        safeTagalog
                );

                if (existing > 0) {
                    postAction(callback, true, "This item is already saved.");
                    return;
                }

                FavoriteEntity favorite = new FavoriteEntity(
                        safeType,
                        safeIlokano,
                        safeEnglish,
                        safeTagalog,
                        System.currentTimeMillis()
                );

                long insertedId = favoriteDao.insert(favorite);

                if (insertedId != -1L) {
                    postAction(callback, true, "Added to Favorites.");
                } else {
                    postAction(callback, false, "Could not save this favorite.");
                }

            } catch (Exception error) {
                postAction(
                        callback,
                        false,
                        error.getMessage() == null
                                ? "Could not save this favorite."
                                : error.getMessage()
                );
            }
        });
    }

    /**
     * Load every favorite, newest first.
     */
    public void getAllFavorites(
            ResultCallback<List<FavoriteEntity>> callback
    ) {
        executorService.execute(() -> {
            try {
                List<FavoriteEntity> favorites =
                        favoriteDao.getAllFavorites();

                postResult(callback, favorites);

            } catch (Exception error) {
                postResult(callback, null);
            }
        });
    }

    /**
     * Load favorites of a specific type.
     */
    public void getFavoritesByType(
            String type,
            ResultCallback<List<FavoriteEntity>> callback
    ) {
        executorService.execute(() -> {
            try {
                List<FavoriteEntity> favorites =
                        favoriteDao.getFavoritesByType(clean(type));

                postResult(callback, favorites);

            } catch (Exception error) {
                postResult(callback, null);
            }
        });
    }

    /**
     * Check whether the supplied item is already saved.
     */
    public void isFavorite(
            String type,
            String ilokanoText,
            String englishText,
            String tagalogText,
            ResultCallback<Boolean> callback
    ) {
        executorService.execute(() -> {
            try {
                boolean saved = favoriteDao.isFavorite(
                        clean(type),
                        clean(ilokanoText),
                        clean(englishText),
                        clean(tagalogText)
                ) > 0;

                postResult(callback, saved);

            } catch (Exception error) {
                postResult(callback, false);
            }
        });
    }

    /**
     * Remove a favorite using its database ID.
     */
    public void removeFavorite(
            int favoriteId,
            ActionCallback callback
    ) {
        executorService.execute(() -> {
            try {
                int deleted = favoriteDao.deleteById(favoriteId);

                if (deleted > 0) {
                    postAction(callback, true, "Removed from Favorites.");
                } else {
                    postAction(callback, false, "Favorite was not found.");
                }

            } catch (Exception error) {
                postAction(
                        callback,
                        false,
                        error.getMessage() == null
                                ? "Could not remove this favorite."
                                : error.getMessage()
                );
            }
        });
    }

    /**
     * Remove every favorite.
     */
    public void clearFavorites(ActionCallback callback) {
        executorService.execute(() -> {
            try {
                favoriteDao.deleteAllFavorites();
                postAction(callback, true, "Favorites cleared.");

            } catch (Exception error) {
                postAction(
                        callback,
                        false,
                        error.getMessage() == null
                                ? "Could not clear Favorites."
                                : error.getMessage()
                );
            }
        });
    }

    /**
     * Return the number of saved favorites.
     */
    public void getFavoriteCount(ResultCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                int count = favoriteDao.getFavoriteCount();
                postResult(callback, count);

            } catch (Exception error) {
                postResult(callback, -1);
            }
        });
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private <T> void postResult(
            ResultCallback<T> callback,
            T result
    ) {
        if (callback != null) {
            mainHandler.post(() -> callback.onComplete(result));
        }
    }

    private void postAction(
            ActionCallback callback,
            boolean success,
            String message
    ) {
        if (callback != null) {
            mainHandler.post(
                    () -> callback.onComplete(success, message)
            );
        }
    }

    /**
     * Call when the component using this repository is finished.
     */
    public void close() {
        executorService.shutdown();
    }
}
