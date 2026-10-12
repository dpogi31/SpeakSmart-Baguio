
package com.example.speaksmartbaguio.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.speaksmartbaguio.entity.FavoriteEntity;

import java.util.List;

@Dao
public interface FavoriteDao {

    // Save a favorite item.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(FavoriteEntity favorite);

    // Retrieve all favorites, newest first.
    @Query("SELECT * FROM favorites ORDER BY savedAt DESC")
    List<FavoriteEntity> getAllFavorites();

    // Get favorites of one type: DICTIONARY or PHRASEBOOK.
    @Query("SELECT * FROM favorites WHERE type = :type ORDER BY savedAt DESC")
    List<FavoriteEntity> getFavoritesByType(String type);

    // Check whether an item is already saved.
    @Query(
            "SELECT COUNT(*) FROM favorites " +
                    "WHERE type = :type " +
                    "AND ilokanoText = :ilokanoText " +
                    "AND englishText = :englishText " +
                    "AND tagalogText = :tagalogText"
    )
    int isFavorite(
            String type,
            String ilokanoText,
            String englishText,
            String tagalogText
    );

    // Remove a favorite using its database ID.
    @Query("DELETE FROM favorites WHERE id = :favoriteId")
    int deleteById(int favoriteId);

    // Remove all favorites.
    @Query("DELETE FROM favorites")
    int deleteAllFavorites();

    // Count saved favorites.
    @Query("SELECT COUNT(*) FROM favorites")
    int getFavoriteCount();
}
