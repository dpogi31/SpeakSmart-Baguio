
package com.example.speaksmartbaguio.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class DatabaseClient {

    private static volatile AppDatabase database;

    /**
     * Migration from database version 5 to version 6.
     *
     * Creates the Favorites table without deleting existing
     * Dictionary or Phrasebook data.
     */
    public static final Migration MIGRATION_5_6 =
            new Migration(5, 6) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase db) {

                    db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `favorites` (" +
                                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                    "`type` TEXT NOT NULL, " +
                                    "`ilokanoText` TEXT NOT NULL, " +
                                    "`englishText` TEXT NOT NULL, " +
                                    "`tagalogText` TEXT NOT NULL, " +
                                    "`savedAt` INTEGER NOT NULL" +
                                    ")"
                    );
                }
            };

    public static AppDatabase getInstance(Context context) {

        if (database == null) {
            synchronized (DatabaseClient.class) {

                if (database == null) {
                    database = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "SpeakSmartDatabase"
                            )
                            .addMigrations(MIGRATION_5_6)
                            .build();
                }
            }
        }

        return database;
    }
}
