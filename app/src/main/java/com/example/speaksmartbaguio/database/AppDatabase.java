package com.example.speaksmartbaguio.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.speaksmartbaguio.dao.DictionaryDao;
import com.example.speaksmartbaguio.dao.PhraseDao;
import com.example.speaksmartbaguio.entity.DictionaryEntity;
import com.example.speaksmartbaguio.entity.PhraseEntity;

@Database(
        entities = {
                DictionaryEntity.class,
                PhraseEntity.class
        },
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract DictionaryDao dictionaryDao();

    public abstract PhraseDao phraseDao();

}