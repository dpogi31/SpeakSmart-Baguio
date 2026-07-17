package com.example.speaksmartbaguio.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.speaksmartbaguio.entity.DictionaryEntity;

import java.util.List;

@Dao
public interface DictionaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DictionaryEntity word);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DictionaryEntity> words);

    @Query("SELECT * FROM dictionary ORDER BY ilokanoWord ASC")
    List<DictionaryEntity> getAllWords();

    @Query("SELECT * FROM dictionary WHERE id = :id LIMIT 1")
    DictionaryEntity getWordById(String id);

    @Query("SELECT * FROM dictionary WHERE " +
            "ilokanoWord LIKE '%' || :query || '%' OR " +
            "englishTranslation LIKE '%' || :query || '%' OR " +
            "tagalogTranslation LIKE '%' || :query || '%'")
    List<DictionaryEntity> searchWords(String query);

    @Query("DELETE FROM dictionary")
    void deleteAll();
}