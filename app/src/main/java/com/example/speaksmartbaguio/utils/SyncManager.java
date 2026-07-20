package com.example.speaksmartbaguio.utils;
import com.example.speaksmartbaguio.utils.VersionChecker;

import android.content.Context;
import android.util.Log;

import com.example.speaksmartbaguio.repository.DictionaryRepository;
import com.example.speaksmartbaguio.repository.PhraseRepository;

public class SyncManager {

    private static final String TAG = "SyncManager";

    public static void sync(Context context) {

        VersionChecker.checkVersions(
                context,
                (
                        dictionaryNeedsUpdate,
                        phrasebookNeedsUpdate,
                        dictionaryVersion,
                        phrasebookVersion
                ) -> {

                    Log.d(TAG, "Dictionary update: " + dictionaryNeedsUpdate);
                    Log.d(TAG, "Phrasebook update: " + phrasebookNeedsUpdate);

                    if (dictionaryNeedsUpdate) {
                        Log.d(TAG, "Updating Dictionary...");
                        new DictionaryRepository(context)
                                .syncFromServer(
                                        context,
                                        dictionaryVersion,
                                        () -> Log.d(TAG, "Dictionary synced.")
                                );

                    }

                    if (phrasebookNeedsUpdate) {
                        Log.d(TAG, "Updating Phrasebook...");
                        new PhraseRepository(context)
                                .syncFromServer(
                                        context,
                                        phrasebookVersion,
                                        () -> Log.d(TAG, "Phrasebook synced.")
                                );

                    }
                    if (!dictionaryNeedsUpdate && !phrasebookNeedsUpdate) {
                        Log.d(TAG, "Local database is already up to date.");
                    }
                });

    }

}