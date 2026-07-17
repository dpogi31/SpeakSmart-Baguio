package com.example.speaksmartbaguio.database;

import android.content.Context;

import androidx.room.Room;

public class DatabaseClient {

    private static AppDatabase database;

    public static AppDatabase getInstance(Context context) {

        if (database == null) {
            database = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "SpeakSmartDatabase"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
        }

        return database;
    }
}