package com.example.speaksmartbaguio.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.speaksmartbaguio.entity.PhraseEntity;

import java.util.List;

@Dao
public interface PhraseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PhraseEntity phrase);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PhraseEntity> phrases);

    @Query("SELECT * FROM phrasebook ORDER BY category ASC, ilokanoWord ASC")
    List<PhraseEntity> getAllPhrases();

    @Query("SELECT * FROM phrasebook WHERE id = :id LIMIT 1")
    PhraseEntity getPhraseById(String id);

    @Query("SELECT * FROM phrasebook WHERE " +
            "ilokanoWord LIKE '%' || :query || '%' OR " +
            "englishTranslation LIKE '%' || :query || '%' OR " +
            "tagalogTranslation LIKE '%' || :query || '%'")
    List<PhraseEntity> searchPhrases(String query);
    @Query("SELECT COUNT(*) FROM phrasebook")
    int getCount();
    @Query("DELETE FROM phrasebook")
    void deleteAll();
}