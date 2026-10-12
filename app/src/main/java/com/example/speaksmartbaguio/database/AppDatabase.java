
package com.example.speaksmartbaguio.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.speaksmartbaguio.dao.DictionaryDao;
import com.example.speaksmartbaguio.dao.FavoriteDao;
import com.example.speaksmartbaguio.dao.PhraseDao;

import com.example.speaksmartbaguio.entity.DictionaryEntity;
import com.example.speaksmartbaguio.entity.FavoriteEntity;
import com.example.speaksmartbaguio.entity.PhraseEntity;

@Database(
        entities = {
                DictionaryEntity.class,
                PhraseEntity.class,
                FavoriteEntity.class
        },
        version = 6,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract DictionaryDao dictionaryDao();

    public abstract PhraseDao phraseDao();

    public abstract FavoriteDao favoriteDao();

}
