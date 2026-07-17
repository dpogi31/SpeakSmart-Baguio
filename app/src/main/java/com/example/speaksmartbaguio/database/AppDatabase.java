package com.example.speaksmartbaguio.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.speaksmartbaguio.dao.DictionaryDao;
import com.example.speaksmartbaguio.entity.DictionaryEntity;

@Database(
        entities = {
                DictionaryEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract DictionaryDao dictionaryDao();

}